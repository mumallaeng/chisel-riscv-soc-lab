package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import RV32IReference._

class MultiCycleCPUSpec extends AnyFunSpec with ChiselSim with RV32ITestHarness {
  val regInit = Map(1 -> BigInt(7), 2 -> BigInt(5), 4 -> BigInt(100), 5 -> BigInt(30), 6 -> BigInt(77))
  // Raw word with an undefined opcode (custom-0 = 0x0b); rs1/rs2 fields can be filled in.
  // Hits the all-zero Decoder default, so it goes Decode -> Fetch (NOP, 2 cycles).
  // Also used as padding so the last fetch never runs past the end of imem.
  def unknown(rs1: Int = 0, rs2: Int = 0): Instr = Raw((BigInt(rs2) << 20) | (BigInt(rs1) << 15) | BigInt(0x0b))
  val pad: Instr = unknown()

  def words(prog: Seq[Instr]): Seq[UInt] = prog.map(i => encode(i).U(32.W))
  def sim(prog: Seq[Instr], init: Map[Int, BigInt] = regInit)(body: MultiCycleCPU => Unit): Unit =
    simulate(new MultiCycleCPU(words(prog), init))(body)
  def ir(prog: Seq[Instr], i: Int): UInt = encode(prog(i)).U(32.W)

  // Independent CPI model, written separately from the FSM:
  // R/I/LUI/AUIPC/JAL/JALR 4, load 5, store 4, branch 3, undefined opcode 2.
  def cpi(i: Instr): Int = i match {
    case _: L                                  => 5
    case _: S                                  => 4
    case _: B                                  => 3
    case _: R | _: I | _: U | _: Jal | _: Jalr => 4
    case _: Raw                                => 2
  }

  // Expected hardware cycles = CPI sum over the instruction trace the golden model actually executed
  // (following branches/jumps). With control flow the executed count differs from the program
  // length, so the caller passes it as `instrs`.
  def runMC(prog: Seq[Instr], instrs: Int = -1): Unit = {
    val n = if (instrs < 0) prog.length else instrs
    runMultiCycle(prog, hwCycles = executedInstrs(prog, n).map(cpi).sum, instrs = n)
  }

  // Fetch/Decode path from Step 11. Every RV32I instruction now continues to Execute,
  // so only an undefined opcode stays on this path (2 cycles, NOP).
  describe("MultiCycleCPU (Fetch/Decode 경로 — 정의되지 않은 opcode는 여기서 바로 Fetch로 돌아온다)") {
    val skel: Seq[Instr] = Seq(
      unknown(rs1 = 1, rs2 = 2),                // A=x1, B=x2
      unknown(rs1 = 4, rs2 = 5),                // A=x4, B=x5
      Raw((BigInt(6) << 20) | BigInt(0x0b)),    // inst[24:20]=6: immediate bits in an I-type, not rs2
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
        dut.io.a.expect(0.U) // Fetch leaves A/B alone
        dut.io.b.expect(0.U)
      }
    }
    it("decode 한 사이클: rs1/rs2가 가리키는 레지스터 값을 A/B에 래치하고 IR/PC는 그대로") {
      sim(skel) { dut =>
        dut.clock.step(2)
        dut.io.state.expect(State.fetch) // undefined opcode: straight back to Fetch, no Execute
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
        dut.io.a.expect(7.U) // still the first instruction's A/B
        dut.io.b.expect(5.U)
        dut.clock.step(1)
        dut.io.state.expect(State.fetch)
        dut.io.a.expect(100.U)
        dut.io.b.expect(30.U)
      }
    }
    it("decode는 opcode를 안 보고 inst[24:20]을 rs2로 무조건 읽어 B에 넣는다(I-type이면 immediate 비트인 garbage read)") {
      sim(skel) { dut =>
        dut.clock.step(6)
        dut.io.ir.expect(ir(skel, 2))
        dut.io.a.expect(0.U)  // rs1 = x0
        dut.io.b.expect(77.U) // inst[24:20] = 6 -> value of x6
      }
    }
    it("정의되지 않은 opcode는 레지스터 파일을 안 바꾼다(NOP처럼 건너뜀)") {
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
        // aluOut holds Decode's speculative branch/JAL target (oldPC + imm): meaningless for add, not checked
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
    it("정의되지 않은 opcode는 2사이클 만에 지나가고, 이어지는 ALU 명령어는 정상 실행된다") {
      val prog: Seq[Instr] = Seq(
        unknown(),
        R("add", rd = 4, rs1 = 1, rs2 = 2),
        pad,
      )
      sim(prog) { dut =>
        dut.clock.step(2)
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(4.U)
        dut.io.debugRegs(3).expect(0.U) // 아무것도 안 함
        dut.clock.step(4)
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(8.U)
        dut.io.debugRegs(4).expect(12.U) // add x4,x1,x2 = 7+5
        dut.io.debugRegs(3).expect(0.U)
      }
    }
    it("FSM 상태 전이: ALU 명령어는 4사이클, 정의되지 않은 opcode는 2사이클") {
      val prog: Seq[Instr] = Seq(
        R("add", rd = 3, rs1 = 1, rs2 = 2), // 4사이클
        unknown(),                          // 2사이클(건너뜀)
        R("sub", rd = 6, rs1 = 4, rs2 = 5), // 4사이클
        pad,
      )
      val (f, d, e, w) = (State.fetch, State.decode, State.execute, State.writeback)
      val trace = Seq(f, d, e, w, // t=0..3   add
                      f, d,       // t=4,5    정의되지 않은 opcode: decode에서 바로 fetch
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
    it("load는 5사이클: 메모리 출력은 Memory 상태에서만 유효하고, MDR이 붙잡아 Writeback에 넘긴다") {
      val prog: Seq[Instr] = Seq(
        S("sw", rs1 = 1, rs2 = 2, imm = 4), // 4사이클, t=0..3
        L("lw", rd = 5, rs1 = 1, imm = 4),  // t=4..8 (Fetch=4, Decode=5, Execute=6, Memory=7, Writeback=8)
        pad,
      )
      sim(prog, memInit) { dut =>
        dut.clock.step(7)
        dut.io.state.expect(State.memory)
        dut.io.aluOut.expect(20.U) // 주소
        dut.io.memOut.expect("hDEADBEEF".U) // memRead가 켜진 Memory 상태: 메모리 출력이 유효
        dut.io.mdr.expect(0.U)     // MDR은 아직 못 잡음 (이 사이클 끝에 잡는다)
        dut.clock.step(1)
        dut.io.state.expect(State.writeback)
        dut.io.memOut.expect(0.U)        // Writeback: memRead가 꺼져 메모리 출력은 0 — 메모리는 값을 안 붙잡는다
        dut.io.mdr.expect("hDEADBEEF".U) // 그래서 MDR만이 값을 들고 있다
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
      runMC(prog)
    }
  }

  // Golden-model comparison (RV32IReference) since Step 12; expected hardware cycles = CPI sum.
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
      runMC(prog)
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
      runMC(prog)
    }
    it("앞 명령어의 결과를 바로 다음 명령어가 읽어도 맞다(명령어가 끝까지 끝난 뒤 다음 Fetch)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 1),
        R("add", rd = 1, rs1 = 1, rs2 = 1),
        R("add", rd = 1, rs1 = 1, rs2 = 1),
        R("add", rd = 1, rs1 = 1, rs2 = 1), // x1 = 8
        R("add", rd = 2, rs1 = 1, rs2 = 1), // x2 = 16
      )
      runMC(prog)
    }
    it("rd=x0으로 쓰려는 시도는 무시되고 x0은 0으로 남는다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 0, rs1 = 0, imm = 123),
        I("addi", rd = 1, rs1 = 0, imm = 5),
        R("add", rd = 0, rs1 = 1, rs2 = 1),
        R("add", rd = 2, rs1 = 0, rs2 = 1), // x0(=0) + x1
      )
      runMC(prog)
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
      runMC(prog)
    }
    it("sb/lb, lbu: 부호 있는 바이트를 저장/로드하며 부호·무부호 확장이 갈린다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 8),
        I("addi", rd = 2, rs1 = 0, imm = -1),
        S("sb", rs1 = 1, rs2 = 2, imm = 0),
        L("lb", rd = 3, rs1 = 1, imm = 0),
        L("lbu", rd = 4, rs1 = 1, imm = 0),
      )
      runMC(prog)
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
      runMC(prog)
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
      runMC(prog)
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
      runMC(prog) // 4x4 + 3x4 + 3x5 + 2x4 = 51
    }
  }

  // Step 14: branch/JAL/JALR/LUI/AUIPC. Branch decision in Execute, target precomputed in Decode as oldPC + imm.
  describe("MultiCycleCPU (Branch/Jump/LUI/AUIPC 타임라인)") {
    it("Fetch가 oldPC를 잡는다: 두 번째 명령어(pc=4)의 fetch 뒤엔 pc=8, oldPc=4") {
      val prog: Seq[Instr] = Seq(I("addi", rd = 3, rs1 = 0, imm = 1), I("addi", rd = 4, rs1 = 0, imm = 1), pad)
      sim(prog) { dut =>
        dut.clock.step(1)
        dut.io.pc.expect(4.U)
        dut.io.oldPc.expect(0.U)
        dut.clock.step(4) // rest of the first instruction (3 cycles) + second Fetch
        dut.io.state.expect(State.decode)
        dut.io.pc.expect(8.U)
        dut.io.oldPc.expect(4.U)
      }
    }
    it("taken beq: Decode에서 aluOut에 타깃(oldPC+imm)을 잡고, 3사이클 만에 pc가 타깃으로 바뀐다") {
      val prog: Seq[Instr] = Seq(
        B("beq", rs1 = 1, rs2 = 1, imm = 12),   // pc 0 -> 12
        I("addi", rd = 3, rs1 = 0, imm = 1),   // skipped
        I("addi", rd = 3, rs1 = 0, imm = 2),   // skipped
        I("addi", rd = 7, rs1 = 0, imm = 9),   // target
        pad,
      )
      sim(prog) { dut =>
        dut.clock.step(2) // fetch, decode
        dut.io.state.expect(State.execute)
        dut.io.pc.expect(4.U)
        dut.io.aluOut.expect(12.U) // precomputed target
        dut.clock.step(1)
        dut.io.state.expect(State.fetch) // no Writeback
        dut.io.pc.expect(12.U)
        dut.io.aluOut.expect(12.U)   // Execute must not overwrite aluOut
        dut.clock.step(4)
        dut.io.debugRegs(7).expect(9.U)
        dut.io.debugRegs(3).expect(0.U)
      }
    }
    it("not-taken beq: pc는 fetch에서 이미 +4 된 값 그대로, 레지스터는 안 바뀐다") {
      val prog: Seq[Instr] = Seq(
        B("beq", rs1 = 1, rs2 = 2, imm = 12),
        I("addi", rd = 3, rs1 = 0, imm = 1),
        pad,
      )
      sim(prog) { dut =>
        dut.clock.step(3)
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(4.U)
        dut.clock.step(4)
        dut.io.debugRegs(3).expect(1.U)
      }
    }
    it("jal: 타깃은 pc, 복귀주소(oldPC+4)는 aluOut으로 같은 엣지에 들어가고 Writeback이 rd에 쓴다") {
      val prog: Seq[Instr] = Seq(
        Jal(rd = 3, imm = 12),
        I("addi", rd = 9, rs1 = 0, imm = 1), // skipped
        I("addi", rd = 9, rs1 = 0, imm = 2), // skipped
        pad,
      )
      sim(prog) { dut =>
        dut.clock.step(2)
        dut.io.aluOut.expect(12.U) // precomputed target
        dut.clock.step(1)
        dut.io.state.expect(State.writeback)
        dut.io.pc.expect(12.U)
        dut.io.aluOut.expect(4.U) // link address
        dut.io.debugRegs(3).expect(0.U)
        dut.clock.step(1)
        dut.io.state.expect(State.fetch)
        dut.io.debugRegs(3).expect(4.U)
        dut.io.debugRegs(9).expect(0.U)
      }
    }
    it("jalr: 타깃은 (rs1+imm)의 LSB를 0으로 만든 값, rd에는 복귀주소") {
      val prog: Seq[Instr] = Seq(Jalr(rd = 3, rs1 = 4, imm = 9), pad) // x4=100 -> 109 -> 108
      sim(prog) { dut =>
        dut.clock.step(3)
        dut.io.state.expect(State.writeback)
        dut.io.pc.expect(108.U)
        dut.io.aluOut.expect(4.U)
        dut.clock.step(1)
        dut.io.debugRegs(3).expect(4.U)
      }
    }
    it("lui: ALU A 입력이 0 -> rd = imm20 << 12, 4사이클") {
      val prog: Seq[Instr] = Seq(U("lui", rd = 3, imm20 = 0x12345), pad)
      sim(prog) { dut =>
        dut.clock.step(3)
        dut.io.aluOut.expect(0x12345000L.U)
        dut.io.debugRegs(3).expect(0.U)
        dut.clock.step(1)
        dut.io.debugRegs(3).expect(0x12345000L.U)
        dut.io.state.expect(State.fetch)
      }
    }
    it("auipc: ALU A 입력이 oldPC — 자기 PC(8) + imm20<<12 (pc는 이미 12)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 3, rs1 = 0, imm = 1),
        I("addi", rd = 3, rs1 = 0, imm = 2),
        U("auipc", rd = 8, imm20 = 1),
        pad,
      )
      sim(prog) { dut =>
        dut.clock.step(8 + 1)
        dut.io.pc.expect(12.U)
        dut.io.oldPc.expect(8.U)
        dut.clock.step(3)
        dut.io.debugRegs(8).expect((8 + 0x1000).U)
      }
    }
  }

  describe("MultiCycleCPU vs golden model (Branch/Jump/LUI/AUIPC)") {
    it("6가지 분기를 taken/not-taken 모두 지난다 (blt/bge는 부호, bltu/bgeu는 무부호)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = -5),
        I("addi", rd = 2, rs1 = 0, imm = 3),
        B("blt", rs1 = 1, rs2 = 2, imm = 8),   // taken
        I("addi", rd = 10, rs1 = 0, imm = 1),
        B("bge", rs1 = 1, rs2 = 2, imm = 8),   // not taken
        I("addi", rd = 11, rs1 = 0, imm = 1),
        B("bltu", rs1 = 1, rs2 = 2, imm = 8),  // not taken (-5 is huge unsigned)
        I("addi", rd = 12, rs1 = 0, imm = 1),
        B("bgeu", rs1 = 1, rs2 = 2, imm = 8),  // taken
        I("addi", rd = 13, rs1 = 0, imm = 1),
        B("beq", rs1 = 1, rs2 = 2, imm = 8),   // not taken
        I("addi", rd = 14, rs1 = 0, imm = 1),
        B("bne", rs1 = 1, rs2 = 2, imm = 8),   // taken
        I("addi", rd = 15, rs1 = 0, imm = 1),
        pad,
      )
      runMC(prog, instrs = 11)
    }
    it("음수 offset 루프: 1+2+3+4+5 = 15 (bne로 뒤로 분기)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 0),
        I("addi", rd = 2, rs1 = 0, imm = 5),
        R("add", rd = 1, rs1 = 1, rs2 = 2),
        I("addi", rd = 2, rs1 = 2, imm = -1),
        B("bne", rs1 = 2, rs2 = 0, imm = -8),
        pad,
      )
      runMC(prog, instrs = 2 + 5 * 3)
    }
    it("jal/jalr로 함수 호출과 복귀: 호출 뒤 복귀 지점에서 이어 실행한다") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 10, rs1 = 0, imm = 3),
        Jal(rd = 1, imm = 16),                // -> 20 (function), x1 = 8
        I("addi", rd = 11, rs1 = 10, imm = 1), // return point
        Jal(rd = 0, imm = 16),                // -> 28 (end)
        pad,
        I("slli", rd = 10, rs1 = 10, imm = 2), // function body (pc 20)
        Jalr(rd = 0, rs1 = 1, imm = 0),        // ret
        pad,
      )
      runMC(prog, instrs = 6)
    }
    it("jalr rd==rs1 (jalr x1,x1,0): 옛 x1로 점프하고 x1엔 복귀주소") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 12),
        Jalr(rd = 1, rs1 = 1, imm = 0),
        pad,
        I("addi", rd = 2, rs1 = 1, imm = 0),
      )
      runMC(prog, instrs = 3)
    }
    it("jalr는 타깃의 LSB를 0으로 만든다 (13 -> 12)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 13),
        Jalr(rd = 2, rs1 = 1, imm = 0),
        pad,
        I("addi", rd = 3, rs1 = 0, imm = 1),
      )
      runMC(prog, instrs = 3)
    }
    it("lui + addi로 32비트 상수를 만든다 (0x12345678), auipc 두 개, 음수 방향 lui") {
      val prog: Seq[Instr] = Seq(
        U("lui", rd = 1, imm20 = 0x12345),
        I("addi", rd = 1, rs1 = 1, imm = 0x678),
        U("auipc", rd = 2, imm20 = 0),
        U("auipc", rd = 3, imm20 = 1),
        U("lui", rd = 4, imm20 = 0xFFFFF),
        pad,
      )
      runMC(prog, instrs = 5)
    }
    it("루프 안에 load/store가 섞여도 맞다 (메모리 카운트다운)") {
      val prog: Seq[Instr] = Seq(
        I("addi", rd = 1, rs1 = 0, imm = 64),
        I("addi", rd = 2, rs1 = 0, imm = 4),
        S("sw", rs1 = 1, rs2 = 2, imm = 0),
        L("lw", rd = 3, rs1 = 1, imm = 0),      // loop start (pc 12)
        I("addi", rd = 3, rs1 = 3, imm = -1),
        S("sw", rs1 = 1, rs2 = 3, imm = 0),
        B("bne", rs1 = 3, rs2 = 0, imm = -12),
        pad,
      )
      runMC(prog, instrs = 3 + 4 * 4)
    }
  }
}
