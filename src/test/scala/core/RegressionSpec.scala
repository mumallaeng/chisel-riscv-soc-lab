package core

import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import RV32IReference._

// Step 10 — 회귀 통합. CPUSpec이 기능별로 쪼개서 확인한 것과 달리, 여기는 riscv-tests
// 스타일의 "self-checking" 프로그램(기대값을 프로그램 자기 안에서 비교해서, 맞으면
// x31=1을 쓰고 틀리면 fail 라벨에서 무한루프)과, 여러 기능을 실제로 엮어 쓰는
// 통합 프로그램(반복문)으로 v0.2.0 RV32I single-cycle 전체를 마무리 검증한다.
class RegressionSpec extends AnyFunSpec with ChiselSim with RV32ITestHarness {
  describe("self-checking directed tests (riscv-tests 관례: x31=1이면 pass, 아니면 fail 라벨에서 무한루프)") {
    it("ALU: add 결과가 기대값과 같은지 프로그램 스스로 확인한다") {
      run(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 7),
          I("addi", rd = 2, rs1 = 0, imm = 5),
          R("add", rd = 3, rs1 = 1, rs2 = 2),   // x3 = 12
          I("addi", rd = 4, rs1 = 0, imm = 12), // expected, idx3
          B("beq", rs1 = 3, rs2 = 4, imm = 8),  // idx4, 맞으면 idx6(pass)로 -> offset=(6-4)*4=8
          B("beq", rs1 = 0, rs2 = 0, imm = 0),  // idx5 = fail: 자기 자신으로 무한루프(offset 0)
          I("addi", rd = 31, rs1 = 0, imm = 1), // idx6 = pass
        ),
        cycles = 6, // idx0,1,2,3,4(taken)->idx6 실행, idx5(fail)는 안 밟음
        expectPass = true,
      )
    }
    it("load/store: sw로 쓴 값을 lw로 읽어 기대값과 비교한다") {
      run(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 0),   // base
          I("addi", rd = 2, rs1 = 0, imm = -77), // 저장할 값
          S("sw", rs1 = 1, rs2 = 2, imm = 0),
          L("lw", rd = 3, rs1 = 1, imm = 0),
          I("addi", rd = 4, rs1 = 0, imm = -77), // expected, idx4
          B("beq", rs1 = 3, rs2 = 4, imm = 8),   // idx5, 맞으면 idx7로 -> offset=(7-5)*4=8
          B("beq", rs1 = 0, rs2 = 0, imm = 0),   // idx6 = fail
          I("addi", rd = 31, rs1 = 0, imm = 1),  // idx7 = pass
        ),
        cycles = 7,
        expectPass = true,
      )
    }
    it("jump: jal의 복귀주소(pc+4)가 기대값과 같은지 확인한다") {
      run(
        Seq(
          Jal(rd = 1, imm = 8),                 // idx0, pc=0 -> idx2, x1 = 4
          I("addi", rd = 2, rs1 = 0, imm = 999), // idx1, 건너뛰어짐
          I("addi", rd = 3, rs1 = 0, imm = 4),   // idx2 = expected
          B("beq", rs1 = 1, rs2 = 3, imm = 8),   // idx3, 맞으면 idx5로 -> offset=(5-3)*4=8
          B("beq", rs1 = 0, rs2 = 0, imm = 0),   // idx4 = fail
          I("addi", rd = 31, rs1 = 0, imm = 1),  // idx5 = pass
        ),
        cycles = 4, // idx0,2,3(taken)->idx5
        expectPass = true,
      )
    }
  }

  describe("통합 프로그램") {
    it("역방향 분기로 1부터 5까지 더하는 loop (최초의 진짜 반복문)") {
      run(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 1), // idx0: i = 1
          I("addi", rd = 2, rs1 = 0, imm = 0), // idx1: sum = 0
          I("addi", rd = 5, rs1 = 0, imm = 6), // idx2: limit = 6
          R("add", rd = 2, rs1 = 2, rs2 = 1),  // idx3(loop): sum += i
          I("addi", rd = 1, rs1 = 1, imm = 1), // idx4: i++
          B("blt", rs1 = 1, rs2 = 5, imm = -8), // idx5: i<6이면 idx3(loop)로 -> offset=(3-5)*4=-8
          I("addi", rd = 6, rs1 = 0, imm = 15), // idx6: expected sum = 1+2+3+4+5
          B("beq", rs1 = 2, rs2 = 6, imm = 8),  // idx7, 맞으면 idx9로 -> offset=(9-7)*4=8
          B("beq", rs1 = 0, rs2 = 0, imm = 0),  // idx8 = fail
          I("addi", rd = 31, rs1 = 0, imm = 1), // idx9 = pass
        ),
        // 사이클 수 = 준비 3 + loop 5회 * 3 + 검사 2 + pass 1 = 21
        cycles = 21,
        expectPass = true,
      )
    }
  }
}
