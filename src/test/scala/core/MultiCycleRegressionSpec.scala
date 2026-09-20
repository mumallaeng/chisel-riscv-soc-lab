package core

import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import RV32IReference._

class MultiCycleRegressionSpec extends AnyFunSpec with ChiselSim with RV32ITestHarness {
  describe("self-checking directed tests (RegressionSpec 프로그램을 multi-cycle에서 재검증)") {
    it("ALU: add 결과가 기대값과 같은지 프로그램 스스로 확인한다") {
      runMC(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 7),
          I("addi", rd = 2, rs1 = 0, imm = 5),
          R("add", rd = 3, rs1 = 1, rs2 = 2),
          I("addi", rd = 4, rs1 = 0, imm = 12),
          B("beq", rs1 = 3, rs2 = 4, imm = 8),
          B("beq", rs1 = 0, rs2 = 0, imm = 0),
          I("addi", rd = 31, rs1 = 0, imm = 1),
        ),
        instrs = 6,
        expectPass = true,
      )
    }
    it("load/store: sw로 쓴 값을 lw로 읽어 기대값과 비교한다") {
      runMC(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 0),
          I("addi", rd = 2, rs1 = 0, imm = -77),
          S("sw", rs1 = 1, rs2 = 2, imm = 0),
          L("lw", rd = 3, rs1 = 1, imm = 0),
          I("addi", rd = 4, rs1 = 0, imm = -77),
          B("beq", rs1 = 3, rs2 = 4, imm = 8),
          B("beq", rs1 = 0, rs2 = 0, imm = 0),
          I("addi", rd = 31, rs1 = 0, imm = 1),
        ),
        instrs = 7,
        expectPass = true,
      )
    }
    it("jump: jal의 복귀주소(pc+4)가 기대값과 같은지 확인한다") {
      runMC(
        Seq(
          Jal(rd = 1, imm = 8),
          I("addi", rd = 2, rs1 = 0, imm = 999),
          I("addi", rd = 3, rs1 = 0, imm = 4),
          B("beq", rs1 = 1, rs2 = 3, imm = 8),
          B("beq", rs1 = 0, rs2 = 0, imm = 0),
          I("addi", rd = 31, rs1 = 0, imm = 1),
        ),
        instrs = 4,
        expectPass = true,
      )
    }
  }

  describe("통합 프로그램") {
    it("역방향 분기로 1부터 5까지 더하는 loop") {
      runMC(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 1),
          I("addi", rd = 2, rs1 = 0, imm = 0),
          I("addi", rd = 5, rs1 = 0, imm = 6),
          R("add", rd = 2, rs1 = 2, rs2 = 1),
          I("addi", rd = 1, rs1 = 1, imm = 1),
          B("blt", rs1 = 1, rs2 = 5, imm = -8),
          I("addi", rd = 6, rs1 = 0, imm = 15),
          B("beq", rs1 = 2, rs2 = 6, imm = 8),
          B("beq", rs1 = 0, rs2 = 0, imm = 0),
          I("addi", rd = 31, rs1 = 0, imm = 1),
        ),
        instrs = 21,
        expectPass = true,
      )
    }
  }
}
