package core

import chisel3._
import chisel3.util._

object State extends ChiselEnum {
  val fetch, decode, execute, memory, writeback = Value
}

// RV32I multi-cycle datapath — R/I-type ALU + load/store까지.
//   R/I-type ALU : Fetch→Decode→Execute→Writeback           (CPI=4)
//   Load         : Fetch→Decode→Execute→Memory→Writeback    (CPI=5)
//   Store        : Fetch→Decode→Execute→Memory              (CPI=4)
// branch/jump/lui/auipc는 아직 구현 전이라 Decode에서 곧장 Fetch로 돌아간다(사실상 NOP).
// 상태 레지스터는 처음 쓰이는 스텝에서 추가한다: IR/A/B(11), ALUOut(12), MDR(13), oldPC(14).
class MultiCycleCPU(program: Seq[UInt], regInit: Map[Int, BigInt] = Map.empty) extends Module {
  val io = IO(new Bundle {
    val state     = Output(State())
    val pc        = Output(UInt(32.W))
    val ir        = Output(UInt(32.W))
    val a         = Output(UInt(32.W))
    val b         = Output(UInt(32.W))
    val aluOut    = Output(UInt(32.W))
    val mdr       = Output(UInt(32.W))
    val debugRegs = Output(Vec(32, UInt(32.W)))
  })

  val state  = RegInit(State.fetch)
  val pc     = RegInit(0.U(32.W))
  val ir     = RegInit(0.U(32.W)) // 실행 중인 명령어를 여러 사이클 동안 붙잡아 둔다
  val a      = RegInit(0.U(32.W)) // rs1 값 래치
  val b      = RegInit(0.U(32.W)) // rs2 값 래치 (R-type 피연산자 / store 데이터)
  val aluOut = RegInit(0.U(32.W)) // Execute 결과(연산 결과 또는 load/store 주소)를 다음 상태로 넘기는 다리
  val mdr    = RegInit(0.U(32.W)) // Memory 상태에서 읽어온 load 데이터

  // 명령어 메모리는 single-cycle과 같은 Vec ROM(Harvard 유지) — Step 11 "요점 2" 참고
  val imem = VecInit(program)
  val fetched = if (program.length <= 1) imem(0.U) else {
    val idxWidth = log2Ceil(program.length)
    imem(pc(idxWidth + 1, 2))
  }

  // Decoder/ImmGen은 single-cycle 것을 그대로 쓴다. 입력이 imem 출력이 아니라 IR 레지스터라서
  // 제어 신호가 Decode~마지막 상태까지 안정적으로 유지된다 — 그래서 부작용이 있는 신호
  // (memWrite, regWrite)는 반드시 상태로 게이트해야 한다.
  val decoder = Module(new Decoder)
  decoder.inst := ir

  val immGen = Module(new ImmGen)
  immGen.inst   := ir
  immGen.immSel := decoder.ctrl.immSel

  val regFile = Module(new RegisterFile(regInit))
  regFile.io.rs1         := ir(19, 15)
  regFile.io.rs2         := ir(24, 20)
  regFile.io.rd          := ir(11, 7)
  regFile.io.writeData   := Mux(decoder.ctrl.memToReg, mdr, aluOut)
  regFile.io.writeEnable := state === State.writeback // rd에 쓰는 명령어만 이 상태에 온다

  // ALU 하나를 상태별로 나눠 쓴다: Fetch에선 PC+4, Execute에선 A op (B 또는 imm).
  // load/store의 주소 계산(A + imm)도 Execute의 같은 ALU가 한다.
  val inExecute = state === State.execute
  val alu = Module(new ALU)
  alu.a  := Mux(inExecute, a, pc)
  alu.b  := Mux(inExecute, Mux(decoder.ctrl.aluSrc, immGen.imm, b), 4.U)
  alu.op := Mux(inExecute, decoder.ctrl.aluOp, ALUOp.add)

  // data memory는 Memory 상태에서만 쓴다. IR이 여러 사이클 유지되므로 decoder의 memWrite는
  // Decode/Execute/Writeback에서도 1인데, 그때 쓰면 옛 aluOut 주소를 옛 B 값으로 덮어쓴다.
  val dmem = Module(new DataMemory)
  dmem.io.addr      := aluOut
  dmem.io.writeData := b
  dmem.io.funct3    := ir(14, 12) // byte/half/word 폭 선택은 single-cycle과 같이 IR에서 직접
  dmem.io.memWrite  := state === State.memory && decoder.ctrl.memWrite

  // R/I-type ALU 명령어 = rd에 쓰면서 load/jump/lui/auipc가 아닌 것. opcode를 직접 비교하지
  // 않고 Decoder가 이미 내는 제어 신호로 가른다.
  val isAluInst = decoder.ctrl.regWrite && !decoder.ctrl.memToReg && !decoder.ctrl.jump &&
    decoder.ctrl.aluSrcA === AluASrc.rs1
  val isMemInst = decoder.ctrl.memRead || decoder.ctrl.memWrite

  switch(state) {
    is(State.fetch) {
      ir    := fetched
      pc    := alu.out
      state := State.decode
    }
    is(State.decode) {
      a     := regFile.io.readData1
      b     := regFile.io.readData2
      state := Mux(isAluInst || isMemInst, State.execute, State.fetch) // 미구현 명령어는 NOP처럼 건너뜀
    }
    is(State.execute) {
      aluOut := alu.out
      state  := Mux(isMemInst, State.memory, State.writeback)
    }
    is(State.memory) {
      when(decoder.ctrl.memRead) { mdr := dmem.io.readData }
      state := Mux(decoder.ctrl.memRead, State.writeback, State.fetch) // store는 여기서 끝
    }
    is(State.writeback) {
      state := State.fetch
    }
  }

  io.state     := state
  io.pc        := pc
  io.ir        := ir
  io.a         := a
  io.b         := b
  io.aluOut    := aluOut
  io.mdr       := mdr
  io.debugRegs := regFile.io.debugRegs
}
