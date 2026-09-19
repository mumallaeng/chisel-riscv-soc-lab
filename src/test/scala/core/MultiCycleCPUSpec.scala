package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import RV32IReference._

class MultiCycleCPUSpec extends AnyFunSpec with ChiselSim with RV32ITestHarness {
  val regInit = Map(1 -> BigInt(7), 2 -> BigInt(5), 4 -> BigInt(100), 5 -> BigInt(30), 6 -> BigInt(77))
  // 아직 구현 안 된 명령어(branch/jump 계열)는 Decode에서 곧장 Fetch로 돌아온다 — 마지막 fetch가
  // imem 범위 밖을 안 읽게 하는 패딩으로도 쓴다.
  val pad: Instr = B("beq", rs1 = 0, rs2 = 0, imm = 0)

  def words(prog: Seq[Instr]): Seq[UInt] = prog.map(i => encode(i).U(32.W))
  def sim(prog: Seq[Instr], init: Map[Int, BigInt] = regInit)(body: MultiCycleCPU => Unit): Unit =
    simulate(new MultiCycleCPU(words(prog), init))(body)
  def ir(prog: Seq[Instr], i: Int): UInt = encode(prog(i)).U(32.W)

  // 명령어별 사이클 수 모델: R/I-type 4, load 5, store 4. 하드웨어 FSM과 따로 한 번 더 적어둔 기대값이다.
  def mcCycles(prog: Seq[Instr]): Int = prog.map {
    case _: L            => 5
    case _: S            => 4
    case _: R | _: I     => 4
    case other           => sys.error(s"mcCycles: 아직 구현 전인 명령어 $other")
  }.sum

  // Step 11에서 만든 Fetch/Decode 경로. ALU/메모리 명령어는 이제 Execute로 넘어가므로, 이 경로를
  // 그대로 타는 건 "아직 구현 안 된" 명령어(branch/jump)뿐이다 — 2사이클 만에 다음 명령어로 넘어간다.
  describe("MultiCycleCPU (Fetch/Decode 경로 — 미구현 명령어는 여기서 바로 Fetch로 돌아온다)") {
    val skel: Seq[Instr] = Seq(
      B("beq", rs1 = 1, rs2 = 2, imm = 8),  // A=x1, B=x2
      B("bne", rs1 = 4, rs2 = 5, imm = 8),  // A=x4, B=x5
      Jalr(rd = 1, rs1 = 0, imm = 6),       // inst[24:20]=6은 rs2가 아니라 immediate 비트
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
        dut.io.mdr.expect(0.U)
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
    it("미구현 명령어(jal)는 2사이클 만에 지나가고, 이어지는 ALU 명령어는 정상 실행된다") {
      val prog: Seq[Instr] = Seq(
        Jal(rd = 3, imm = 8),
        R("add", rd = 4, rs1 = 1, rs2 = 2),
        pad,
      )
      sim(prog) { dut =>
        dut.clock.step(2)
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(4.U)
        dut.io.debugRegs(3).expect(0.U) // jal은 아무것도 안 함
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
        Jal(rd = 5, imm = 8),               // 2사이클(건너뜀)
        R("sub", rd = 6, rs1 = 4, rs2 = 5), // 4사이클
        pad,
      )
      val (f, d, e, w) = (State.fetch, State.decode, State.execute, State.writeback)
      val trace = Seq(f, d, e, w, // t=0..3   add
                      f, d,       // t=4,5    jal: decode에서 바로 fetch
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

  // Step 13: Memory 상태와 MDR. 주소 계산(A+imm)은 Execute의 ALU가 그대로 한다.
  describe("MultiCycleCPU (load/store: Execute에서 주소 계산, Memory 상태에서 접근)") {
    val memInit = Map(1 -> BigInt(16), 2 -> BigInt("DEADBEEF", 16), 3 -> BigInt(64), 4 -> BigInt(100), 6 -> BigInt(17))

    it("store는 4사이클: Memory 상태에서 끝나고 rd에 쓰지 않는다 (inst[11:7]은 imm[4:0]일 뿐)") {
      val prog: Seq[Instr] = Seq(S("sw", rs1 = 1, rs2 = 2, imm = 4), pad) // imm[4:0]=4 -> inst[11:7]=4=x4
      sim(prog, memInit) { dut =>
        dut.clock.step(2) // Fetch, Decode
        dut.io.b.expect("hDEADBEEF".U) // B는 이번엔 store 데이터(rs2)
        dut.clock.step(1) // Execute
        dut.io.state.expect(State.memory)
        dut.io.aluOut.expect(20.U) // 주소 = x1(16) + 4
        dut.clock.step(1) // Memory
        dut.io.state.expect(State.fetch) // Writeback 없이 바로 다음 명령어로
        dut.io.pc.expect(4.U)
        for (r <- 0 until 32) {
          dut.io.debugRegs(r).expect(memInit.getOrElse(r, BigInt(0)).U(32.W)) // x4(rd처럼 보이는 자리)도 그대로
        }
      }
    }
    it("load는 5사이클: MDR은 Memory 상태 뒤에 채워지고, 레지스터는 Writeback 뒤에 바뀐다") {
      val prog: Seq[Instr] = Seq(
        S("sw", rs1 = 1, rs2 = 2, imm = 4), // 4사이클, t=0..3
        L("lw", rd = 5, rs1 = 1, imm = 4),  // t=4..8 (Fetch=4, Decode=5, Execute=6, Memory=7, Writeback=8)
        pad,
      )
      sim(prog, memInit) { dut =>
        dut.clock.step(7)
        dut.io.state.expect(State.memory)
        dut.io.aluOut.expect(20.U) // 주소
        dut.io.mdr.expect(0.U)     // 아직 안 읽음
        dut.clock.step(1)
        dut.io.state.expect(State.writeback)
        dut.io.mdr.expect("hDEADBEEF".U) // Memory 상태가 끝나며 MDR에 담김
        dut.io.debugRegs(5).expect(0.U)  // 레지스터는 아직 (x5 초기값 0)
        dut.clock.step(1)
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(8.U)
        dut.io.debugRegs(5).expect("hDEADBEEF".U)
      }
    }
    it("FSM 상태 전이: store는 4사이클(Memory에서 종료), load는 5사이클(Memory 뒤 Writeback)") {
      val prog: Seq[Instr] = Seq(S("sw", rs1 = 1, rs2 = 2, imm = 4), L("lw", rd = 5, rs1 = 1, imm = 4), pad)
      val (f, d, e, m, w) = (State.fetch, State.decode, State.execute, State.memory, State.writeback)
      val trace = Seq(f, d, e, m,    // t=0..3  sw
                      f, d, e, m, w, // t=4..8  lw
                      f)             // t=9
      sim(prog, memInit) { dut =>
        for ((expected, t) <- trace.zipWithIndex) {
          dut.io.state.expect(expected)
          if (t < trace.length - 1) dut.clock.step(1)
        }
      }
    }
    // IR이 여러 사이클 유지돼서 decoder의 memWrite는 store 명령어의 Decode/Execute 사이클에도 1이다.
    // memWrite를 상태로 게이트하지 않으면 그 사이클에 "옛 aluOut 주소 ← 옛 B 값"이 써진다.
    it("store는 Memory 상태에서만 쓴다 — 다른 사이클엔 직전 명령어의 잔여 aluOut/B로 메모리를 덮지 않는다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 3, rs1 = 0, imm = 64),
        I("addi", rd = 6, rs1 = 0, imm = 17),
        I("addi", rd = 1, rs1 = 0, imm = 16),
        I("addi", rd = 2, rs1 = 0, imm = 1234),
        S("sw", rs1 = 3, rs2 = 6, imm = 0),  // mem[64] = 17
        I("addi", rd = 7, rs1 = 0, imm = 64), // aluOut=64로 남고, B는 x0(=0)이 래치됨
        S("sw", rs1 = 1, rs2 = 2, imm = 0),  // mem[16] = 1234 — 게이트가 없으면 Decode 사이클에 mem[64] <- 0
        L("lw", rd = 8, rs1 = 3, imm = 0),   // mem[64]는 여전히 17이어야 함
        L("lw", rd = 9, rs1 = 1, imm = 0),   // mem[16] = 1234
      )
      runMultiCycle(prog, hwCycles = mcCycles(prog))
    }
  }

  // Step 12부터 golden model(RV32IReference) 대조를 시작했다. 하드웨어 사이클 수는 mcCycles(명령어 종류별 합)로 센다.
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
      runMultiCycle(prog, hwCycles = mcCycles(prog))
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
      runMultiCycle(prog, hwCycles = mcCycles(prog))
    }
    it("앞 명령어의 결과를 바로 다음 명령어가 읽어도 맞다(명령어가 끝까지 끝난 뒤 다음 Fetch)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 1),
        R("add", rd = 1, rs1 = 1, rs2 = 1),
        R("add", rd = 1, rs1 = 1, rs2 = 1),
        R("add", rd = 1, rs1 = 1, rs2 = 1), // x1 = 8
        R("add", rd = 2, rs1 = 1, rs2 = 1), // x2 = 16
      )
      runMultiCycle(prog, hwCycles = mcCycles(prog))
    }
    it("rd=x0으로 쓰려는 시도는 무시되고 x0은 0으로 남는다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 0, rs1 = 0, imm = 123),
        I("addi", rd = 1, rs1 = 0, imm = 5),
        R("add", rd = 0, rs1 = 1, rs2 = 1),
        R("add", rd = 2, rs1 = 0, rs2 = 1), // x0(=0) + x1
      )
      runMultiCycle(prog, hwCycles = mcCycles(prog))
    }
  }

  describe("MultiCycleCPU vs golden model (load/store)") {
    it("sw로 저장한 word를 lw로 그대로 읽는다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 0),
        I("addi", rd = 2, rs1 = 0, imm = -100),
        S("sw", rs1 = 1, rs2 = 2, imm = 0),
        L("lw", rd = 3, rs1 = 1, imm = 0),
      )
      runMultiCycle(prog, hwCycles = mcCycles(prog))
    }
    it("sb/lb, lbu: 부호 있는 바이트를 저장/로드하며 부호·무부호 확장이 갈린다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 8),
        I("addi", rd = 2, rs1 = 0, imm = -1),
        S("sb", rs1 = 1, rs2 = 2, imm = 0),
        L("lb", rd = 3, rs1 = 1, imm = 0),
        L("lbu", rd = 4, rs1 = 1, imm = 0),
      )
      runMultiCycle(prog, hwCycles = mcCycles(prog))
    }
    it("sh/lh는 word 안 상위 하프에도 정확히 배치된다 (offset != 0, Mem은 비초기화라 먼저 0으로 찍어둠)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 16),
        I("addi", rd = 2, rs1 = 0, imm = 2000),
        S("sw", rs1 = 1, rs2 = 0, imm = 0),
        S("sh", rs1 = 1, rs2 = 2, imm = 2),
        L("lh", rd = 3, rs1 = 1, imm = 2),
        L("lhu", rd = 5, rs1 = 1, imm = 2),
        L("lw", rd = 4, rs1 = 1, imm = 0),
      )
      runMultiCycle(prog, hwCycles = mcCycles(prog))
    }
    it("byte store가 인접 바이트를 안 건드린다 (byte-enable)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 20),
        I("addi", rd = 2, rs1 = 0, imm = -1),
        S("sw", rs1 = 1, rs2 = 2, imm = 0),
        I("addi", rd = 3, rs1 = 0, imm = 0),
        S("sb", rs1 = 1, rs2 = 3, imm = 1),
        L("lw", rd = 4, rs1 = 1, imm = 0),
      )
      runMultiCycle(prog, hwCycles = mcCycles(prog))
    }
    it("store 3개 + load 3개 + add로 배열 합 60을 만든다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 32),
        I("addi", rd = 2, rs1 = 0, imm = 10),
        I("addi", rd = 3, rs1 = 0, imm = 20),
        I("addi", rd = 4, rs1 = 0, imm = 30),
        S("sw", rs1 = 1, rs2 = 2, imm = 0),
        S("sw", rs1 = 1, rs2 = 3, imm = 4),
        S("sw", rs1 = 1, rs2 = 4, imm = 8),
        L("lw", rd = 5, rs1 = 1, imm = 0),
        L("lw", rd = 6, rs1 = 1, imm = 4),
        L("lw", rd = 7, rs1 = 1, imm = 8),
        R("add", rd = 8, rs1 = 5, rs2 = 6),
        R("add", rd = 8, rs1 = 8, rs2 = 7), // x8 = 60
      )
      runMultiCycle(prog, hwCycles = mcCycles(prog)) // 4x4 + 3x4 + 3x5 + 2x4 = 51
    }
  }
}
