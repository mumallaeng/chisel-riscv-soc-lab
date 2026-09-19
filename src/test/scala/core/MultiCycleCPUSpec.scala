package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import RV32IReference._

class MultiCycleCPUSpec extends AnyFunSpec with ChiselSim with RV32ITestHarness {
  val regInit = Map(1 -> BigInt(7), 2 -> BigInt(5), 4 -> BigInt(100), 5 -> BigInt(30), 6 -> BigInt(77))
  val pad: Instr = S("sw", rs1 = 0, rs2 = 0, imm = 0) // 미구현 명령어 — 마지막 fetch가 imem 범위 밖을 안 읽게 하는 패딩

  def words(prog: Seq[Instr]): Seq[UInt] = prog.map(i => encode(i).U(32.W))
  def sim(prog: Seq[Instr])(body: MultiCycleCPU => Unit): Unit =
    simulate(new MultiCycleCPU(words(prog), regInit))(body)
  def ir(prog: Seq[Instr], i: Int): UInt = encode(prog(i)).U(32.W)

  // Step 11에서 만든 Fetch/Decode 경로. Step 12부터는 ALU 명령어가 Execute로 넘어가므로,
  // 이 경로를 그대로 타는 건 "아직 구현 안 된" 명령어(store/load)뿐이다 — 2사이클 만에 다음 명령어로 넘어간다.
  describe("MultiCycleCPU (Fetch/Decode 경로 — 미구현 명령어는 여기서 바로 Fetch로 돌아온다)") {
    val skel: Seq[Instr] = Seq(
      S("sw", rs1 = 1, rs2 = 2, imm = 0),  // A=x1, B=x2
      S("sw", rs1 = 4, rs2 = 5, imm = 0),  // A=x4, B=x5
      L("lw", rd = 1, rs1 = 0, imm = 6),   // inst[24:20]=6은 rs2가 아니라 immediate 비트
      pad,
    )

    it("리셋 직후엔 fetch 상태, PC=0, 상태 레지스터는 전부 0") {
      sim(skel) { dut =>
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(0.U)
        dut.io.ir.expect(0.U)
        dut.io.a.expect(0.U)
        dut.io.b.expect(0.U)
        dut.io.aluOut.expect(0.U)
      }
    }
    it("fetch 한 사이클: IR에 명령어를 담고 PC를 +4 하고 decode로 넘어간다") {
      sim(skel) { dut =>
        dut.clock.step(1)
        dut.io.state.expect(State.decode)
        dut.io.ir.expect(ir(skel, 0))
        dut.io.pc.expect(4.U)
        dut.io.a.expect(0.U) // fetch는 A/B를 안 건드린다
        dut.io.b.expect(0.U)
      }
    }
    it("decode 한 사이클: rs1/rs2가 가리키는 레지스터 값을 A/B에 래치하고 IR/PC는 그대로") {
      sim(skel) { dut =>
        dut.clock.step(2)
        dut.io.state.expect(State.fetch) // 미구현 명령어 -> Execute 없이 바로 Fetch
        dut.io.ir.expect(ir(skel, 0))
        dut.io.pc.expect(4.U)
        dut.io.a.expect(7.U)
        dut.io.b.expect(5.U)
      }
    }
    it("fetch-decode를 반복하며 명령어를 차례로 훑는다 (A/B는 다음 decode 전까지 이전 값 유지)") {
      sim(skel) { dut =>
        dut.clock.step(3)
        dut.io.state.expect(State.decode)
        dut.io.ir.expect(ir(skel, 1))
        dut.io.pc.expect(8.U)
        dut.io.a.expect(7.U) // 아직 첫 명령어의 A/B
        dut.io.b.expect(5.U)
        dut.clock.step(1)
        dut.io.state.expect(State.fetch)
        dut.io.a.expect(100.U)
        dut.io.b.expect(30.U)
      }
    }
    it("I-type의 rs2 자리는 immediate 비트인데도 decode는 무조건 읽어 B에 넣는다(garbage read)") {
      sim(skel) { dut =>
        dut.clock.step(6)
        dut.io.ir.expect(ir(skel, 2))
        dut.io.a.expect(0.U)  // rs1 = x0
        dut.io.b.expect(77.U) // inst[24:20] = 6 -> x6의 값
      }
    }
    it("미구현 명령어는 레지스터 파일을 안 바꾼다(NOP처럼 건너뜀)") {
      sim(skel) { dut =>
        dut.clock.step(8)
        dut.io.pc.expect(16.U)
        for (r <- 0 until 32) {
          dut.io.debugRegs(r).expect(regInit.getOrElse(r, BigInt(0)).U(32.W))
        }
      }
    }
  }

  describe("MultiCycleCPU (R/I-type ALU 명령: Fetch→Decode→Execute→Writeback)") {
    it("add 한 명령어는 4사이클: ALUOut에 결과가 담기는 건 Execute 뒤, 레지스터에 쓰이는 건 Writeback 뒤") {
      val prog: Seq[Instr] = Seq(R("add", rd = 3, rs1 = 1, rs2 = 2), pad)
      sim(prog) { dut =>
        dut.clock.step(2) // Fetch, Decode
        dut.io.state.expect(State.execute)
        dut.io.a.expect(7.U)
        dut.io.b.expect(5.U)
        dut.io.aluOut.expect(0.U) // 아직 계산 전
        dut.clock.step(1) // Execute
        dut.io.state.expect(State.writeback)
        dut.io.aluOut.expect(12.U)
        dut.io.debugRegs(3).expect(0.U) // 아직 안 씀
        dut.clock.step(1) // Writeback
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(4.U)
        dut.io.debugRegs(3).expect(12.U)
      }
    }
    it("addi는 ALU의 B 입력으로 B 레지스터가 아니라 immediate를 고른다") {
      val prog: Seq[Instr] = Seq(I("addi", rd = 1, rs1 = 0, imm = 6), pad)
      sim(prog) { dut =>
        dut.clock.step(2)
        dut.io.b.expect(77.U) // decode가 garbage(x6)를 B에 래치해 뒀지만
        dut.clock.step(1)
        dut.io.aluOut.expect(6.U) // ALU는 0 + imm(6)을 계산 — B(77)는 안 씀
        dut.clock.step(1)
        dut.io.debugRegs(1).expect(6.U) // x1: 7 -> 6
      }
    }
    it("sub: funct7이 다른 R-type도 Decoder의 aluOp대로 계산된다") {
      val prog: Seq[Instr] = Seq(R("sub", rd = 6, rs1 = 4, rs2 = 5), pad)
      sim(prog) { dut =>
        dut.clock.step(4)
        dut.io.debugRegs(6).expect(70.U) // 100 - 30 (x6: 77 -> 70)
        dut.io.debugRegs(4).expect(100.U)
        dut.io.debugRegs(5).expect(30.U)
      }
    }
    it("미구현 명령어(lw)는 2사이클 만에 지나가고, 이어지는 ALU 명령어는 정상 실행된다") {
      val prog: Seq[Instr] = Seq(
        L("lw", rd = 3, rs1 = 1, imm = 0),
        R("add", rd = 4, rs1 = 1, rs2 = 2),
        pad,
      )
      sim(prog) { dut =>
        dut.clock.step(2)
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(4.U)
        dut.io.debugRegs(3).expect(0.U) // lw는 아무것도 안 함
        dut.clock.step(4)
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(8.U)
        dut.io.debugRegs(4).expect(12.U) // add x4,x1,x2 = 7+5
        dut.io.debugRegs(3).expect(0.U)
      }
    }
    it("FSM 상태 전이: ALU 명령어는 4사이클, 미구현 명령어는 2사이클") {
      val prog: Seq[Instr] = Seq(
        R("add", rd = 3, rs1 = 1, rs2 = 2), // 4사이클
        L("lw", rd = 5, rs1 = 1, imm = 0),  // 2사이클(건너뜀)
        R("sub", rd = 6, rs1 = 4, rs2 = 5), // 4사이클
        pad,
      )
      val (f, d, e, w) = (State.fetch, State.decode, State.execute, State.writeback)
      val trace = Seq(f, d, e, w, // t=0..3   add
                      f, d,       // t=4,5    lw: decode에서 바로 fetch
                      f, d, e, w, // t=6..9   sub
                      f)          // t=10
      sim(prog) { dut =>
        for ((expected, t) <- trace.zipWithIndex) {
          dut.io.state.expect(expected) // t=t
          if (t < trace.length - 1) dut.clock.step(1)
        }
      }
    }
  }

  // Step 12부터 golden model(RV32IReference) 대조를 시작한다. 하드웨어 사이클 수는 ALU 명령어 4 × 개수.
  describe("MultiCycleCPU vs golden model (R/I-type)") {
    it("R-type 10개 연산을 전부 지난다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = -8),
        I("addi", rd = 2, rs1 = 0, imm = 3),
        R("add", rd = 3, rs1 = 1, rs2 = 2),
        R("sub", rd = 4, rs1 = 1, rs2 = 2),
        R("and", rd = 5, rs1 = 1, rs2 = 2),
        R("or", rd = 6, rs1 = 1, rs2 = 2),
        R("xor", rd = 7, rs1 = 1, rs2 = 2),
        R("sll", rd = 8, rs1 = 1, rs2 = 2),
        R("srl", rd = 9, rs1 = 1, rs2 = 2),
        R("sra", rd = 10, rs1 = 1, rs2 = 2),
        R("slt", rd = 11, rs1 = 1, rs2 = 2),
        R("sltu", rd = 12, rs1 = 1, rs2 = 2),
      )
      runMultiCycle(prog, hwCycles = 4 * prog.length)
    }
    it("I-type 9개 연산(음수 immediate, 시프트 포함)을 전부 지난다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 100),
        I("slti", rd = 2, rs1 = 1, imm = 200),
        I("sltiu", rd = 3, rs1 = 1, imm = 50),
        I("xori", rd = 4, rs1 = 1, imm = 0x55),
        I("ori", rd = 5, rs1 = 1, imm = 0x0f),
        I("andi", rd = 6, rs1 = 1, imm = 0x3c),
        I("slli", rd = 7, rs1 = 1, imm = 3),
        I("srli", rd = 8, rs1 = 1, imm = 2),
        I("addi", rd = 9, rs1 = 0, imm = -1),
        I("srai", rd = 10, rs1 = 9, imm = 4),
      )
      runMultiCycle(prog, hwCycles = 4 * prog.length)
    }
    it("앞 명령어의 결과를 바로 다음 명령어가 읽어도 맞다(명령어가 끝까지 끝난 뒤 다음 Fetch)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 1),
        R("add", rd = 1, rs1 = 1, rs2 = 1),
        R("add", rd = 1, rs1 = 1, rs2 = 1),
        R("add", rd = 1, rs1 = 1, rs2 = 1), // x1 = 8
        R("add", rd = 2, rs1 = 1, rs2 = 1), // x2 = 16
      )
      runMultiCycle(prog, hwCycles = 4 * prog.length)
    }
    it("rd=x0으로 쓰려는 시도는 무시되고 x0은 0으로 남는다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 0, rs1 = 0, imm = 123),
        I("addi", rd = 1, rs1 = 0, imm = 5),
        R("add", rd = 0, rs1 = 1, rs2 = 1),
        R("add", rd = 2, rs1 = 0, rs2 = 1), // x0(=0) + x1
      )
      runMultiCycle(prog, hwCycles = 4 * prog.length)
    }
  }
}
