package core

import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import RV32IReference._

class CPUSpec extends AnyFunSpec with ChiselSim with RV32ITestHarness {
  describe("CPU (R/I-type 싱글사이클)") {
    it("addi로 값을 채우고 add/sub로 조합한다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 5),
        I("addi", rd = 2, rs1 = 0, imm = 3),
        R("add", rd = 3, rs1 = 1, rs2 = 2),
        R("sub", rd = 4, rs1 = 1, rs2 = 2),
      ))
    }
    it("논리(and/or/xor) + 시프트(slli/srli)") {
      run(Seq(
        I("addi", 1, 0, 0xf0),
        I("addi", 2, 0, 0x0f),
        R("and", 3, 1, 2),
        R("or", 4, 1, 2),
        R("xor", 5, 1, 2),
        I("slli", 6, 1, 4),
        I("srli", 7, 1, 2),
      ))
    }
    it("음수 immediate가 부호 확장돼 signed/unsigned 비교가 갈린다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = -1), // x1 = 0xFFFFFFFF
        I("slti", rd = 2, rs1 = 1, imm = 0),  // -1 < 0 (signed)  -> 1
        I("sltiu", rd = 3, rs1 = 1, imm = 0), // 0xFFFFFFFF < 0 (unsigned) -> 0
      ))
    }
    it("레지스터 피연산자 시프트양은 하위 5비트만 쓴다 (sra 부호 유지)") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = -8), // x1 = 0xFFFFFFF8 (-8)
        I("addi", rd = 2, rs1 = 0, imm = 1),  // x2 = 1
        R("sra", rd = 3, rs1 = 1, rs2 = 2),   // -8 >> 1 = -4, 부호 비트 유지
        R("srl", rd = 4, rs1 = 1, rs2 = 2),   // 논리 시프트는 0으로 채움 -> 큰 양수
      ))
    }
    it("rd=x0을 겨냥해도 x0은 0으로 고정된다") {
      run(Seq(I("addi", rd = 0, rs1 = 0, imm = 123)))
    }
  }

  describe("CPU (load/store)") {
    it("sw로 저장한 word를 lw로 그대로 읽는다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 0),          // x1 = base addr 0
        I("addi", rd = 2, rs1 = 0, imm = -100),       // x2 = 저장할 값
        S("sw", rs1 = 1, rs2 = 2, imm = 0),
        L("lw", rd = 3, rs1 = 1, imm = 0),
      ))
    }
    it("sb/lb, lbu: 부호 있는 바이트를 저장/로드하며 부호·무부호 확장이 갈린다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 8),   // base
        I("addi", rd = 2, rs1 = 0, imm = -1),  // 0xFFFFFFFF, 하위 바이트 = 0xFF
        S("sb", rs1 = 1, rs2 = 2, imm = 0),
        L("lb", rd = 3, rs1 = 1, imm = 0),     // sign-extend -> 0xFFFFFFFF
        L("lbu", rd = 4, rs1 = 1, imm = 0),    // zero-extend -> 0x000000FF
      ))
    }
    it("sh/lh는 word 안 상위 하프에도 정확히 배치된다 (offset != 0)") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 16),
        I("addi", rd = 2, rs1 = 0, imm = 2000), // I-type immediate는 signed 12비트(-2048~2047) 안이어야 sign-extend가 안 걸린다
        // Mem은 RegInit처럼 0으로 초기화되지 않는다(uninitialized) -- 건드리지 않은 자리를
        // 0으로 가정하면 안 되므로, 검사 전에 word 전체를 SW로 명시적으로 0을 찍어둔다.
        S("sw", rs1 = 1, rs2 = 0, imm = 0),
        S("sh", rs1 = 1, rs2 = 2, imm = 2),  // addr 18 = word 16의 상위 하프
        L("lh", rd = 3, rs1 = 1, imm = 2),
        L("lw", rd = 4, rs1 = 1, imm = 0),   // 하위 하프는 (SW로 찍어둔) 0이어야 함
      ))
    }
    it("byte store가 인접 바이트를 안 건드린다 (byte-enable 확인)") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 20),
        I("addi", rd = 2, rs1 = 0, imm = -1),
        S("sw", rs1 = 1, rs2 = 2, imm = 0),   // word 전체를 0xFFFFFFFF로
        I("addi", rd = 3, rs1 = 0, imm = 0),
        S("sb", rs1 = 1, rs2 = 3, imm = 1),   // byte lane 1만 0x00
        L("lw", rd = 4, rs1 = 1, imm = 0),    // 0xFFFF00FF 기대
      ))
    }
  }

  describe("CPU (branch/jump)") {
    it("beq taken이면 다음 명령을 건너뛴다") {
      run(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 5),
          I("addi", rd = 2, rs1 = 0, imm = 5),
          B("beq", rs1 = 1, rs2 = 2, imm = 8),   // pc=8, 같으니 taken -> pc=16으로
          I("addi", rd = 3, rs1 = 0, imm = 999), // pc=12, 건너뛰어짐
          I("addi", rd = 4, rs1 = 0, imm = 42),  // pc=16, 여기부터 재개
        ),
        cycles = 4, // pc=0,4,8,16 (12는 안 밟음)
      )
    }
    it("bne not-taken이면 그냥 다음 명령으로 진행한다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 5),
        I("addi", rd = 2, rs1 = 0, imm = 5),
        B("bne", rs1 = 1, rs2 = 2, imm = 8), // 같으니 bne는 not-taken
        I("addi", rd = 3, rs1 = 0, imm = 77),
        I("addi", rd = 4, rs1 = 0, imm = 88),
      )) // cycles 생략 -> prog.length(5), 분기 없이 순서대로
    }
    it("blt(signed)와 bltu(unsigned)가 같은 비트패턴을 다르게 판정한다") {
      run(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = -1), // x1 = 0xFFFFFFFF
          I("addi", rd = 2, rs1 = 0, imm = 0),  // x2 = 0
          B("blt", rs1 = 1, rs2 = 2, imm = 8),  // signed: -1 < 0 -> taken
          I("addi", rd = 3, rs1 = 0, imm = 111),
          I("addi", rd = 4, rs1 = 0, imm = 222),
        ),
        cycles = 4,
      )
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = -1),
        I("addi", rd = 2, rs1 = 0, imm = 0),
        B("bltu", rs1 = 1, rs2 = 2, imm = 8), // unsigned: 0xFFFFFFFF < 0 -> not taken
        I("addi", rd = 3, rs1 = 0, imm = 111),
        I("addi", rd = 4, rs1 = 0, imm = 222),
      )) // not taken -> 5 사이클 다 실행
    }
    it("jal: rd에 복귀주소(pc+4)를 쓰고 pc-relative로 건너뛴다") {
      run(
        Seq(
          Jal(rd = 1, imm = 8),                  // pc=0 -> pc=8, x1 = 0+4 = 4
          I("addi", rd = 2, rs1 = 0, imm = 999), // pc=4, 건너뛰어짐
          I("addi", rd = 3, rs1 = 0, imm = 55),  // pc=8
        ),
        cycles = 2,
      )
    }
    it("jalr: rs1+imm의 LSB를 0으로 자른 주소로 뛴다") {
      run(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 13),   // x1 = 13 (홀수, 일부러)
          Jalr(rd = 2, rs1 = 1, imm = 0),         // target = 13 & ~1 = 12, x2 = pc+4 = 8
          I("addi", rd = 3, rs1 = 0, imm = 999),  // pc=8, 건너뛰어짐
          I("addi", rd = 4, rs1 = 0, imm = 77),   // pc=12
        ),
        cycles = 3,
      )
    }
    it("lui/auipc: 상위 20비트 배치 + auipc는 자기 pc를 더한다") {
      run(Seq(
        U("lui", rd = 1, imm20 = 0x12345),
        U("auipc", rd = 2, imm20 = 1), // pc=4 -> x2 = 4 + (1<<12) = 4100
      ))
    }
  }
}
