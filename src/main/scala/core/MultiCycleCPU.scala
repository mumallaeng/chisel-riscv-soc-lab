package core

import chisel3._
import chisel3.util._

object State extends ChiselEnum {
  val fetch, decode, execute, memory, writeback = Value
}

// RV32I multi-cycle datapath. All base instructions; undefined opcodes are NOPs.
//   R/I-type ALU, LUI, AUIPC : Fetch -> Decode -> Execute -> Writeback            (CPI 4)
//   JAL, JALR                : Fetch -> Decode -> Execute -> Writeback            (CPI 4, rd = return address)
//   Load                     : Fetch -> Decode -> Execute -> Memory -> Writeback  (CPI 5)
//   Store                    : Fetch -> Decode -> Execute -> Memory               (CPI 4)
//   Branch                   : Fetch -> Decode -> Execute                         (CPI 3, no Writeback)
//   Undefined opcode         : Fetch -> Decode                                    (CPI 2, NOP)
// State registers, in the step that first needed each: IR/A/B (11), ALUOut (12), MDR (13), oldPC (14).
class MultiCycleCPU(program: Seq[UInt], regInit: Map[Int, BigInt] = Map.empty) extends Module {
  val io = IO(new Bundle {
    val state     = Output(State())
    val pc        = Output(UInt(32.W))
    val oldPc     = Output(UInt(32.W))
    val ir        = Output(UInt(32.W))
    val a         = Output(UInt(32.W))
    val b         = Output(UInt(32.W))
    val aluOut    = Output(UInt(32.W))
    val mdr       = Output(UInt(32.W))
    val memOut    = Output(UInt(32.W)) // test probe: raw data memory output (valid only while memRead is high)
    val debugRegs = Output(Vec(32, UInt(32.W)))
  })

  val state  = RegInit(State.fetch)
  val pc     = RegInit(0.U(32.W))
  val oldPC  = RegInit(0.U(32.W)) // PC of the instruction in flight (pc before Fetch added 4)
  val ir     = RegInit(0.U(32.W)) // holds the current instruction for all its cycles
  val a      = RegInit(0.U(32.W)) // rs1 value
  val b      = RegInit(0.U(32.W)) // rs2 value: R-type operand / store data / branch compare operand
  val aluOut = RegInit(0.U(32.W)) // ALU result / load-store address / branch-JAL target / link address
  val mdr    = RegInit(0.U(32.W)) // load data captured in Memory

  // Instruction memory: same Vec ROM as single-cycle (Harvard split kept, see Step 11 point 2).
  val imem = VecInit(program)
  val fetched = if (program.length <= 1) imem(0.U) else {
    val idxWidth = log2Ceil(program.length)
    imem(pc(idxWidth + 1, 2))
  }

  // Decoder/ImmGen are the single-cycle modules. Their input is IR (a register), not the imem output,
  // so control signals stay stable from Decode to the last state. Side-effecting signals
  // (memWrite, regWrite) therefore must be gated by state.
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
  regFile.io.writeEnable := state === State.writeback // only rd-writing instructions reach this state

  // One ALU, time-shared by state:
  //   Fetch   : pc + 4                        next PC
  //   Decode  : oldPC + imm                   speculative branch/JAL target (ALU is idle this cycle)
  //   Execute : (A | 0 | oldPC) op (B | imm)  ALU op, load/store address, branch compare, JALR target
  val inExecute = state === State.execute
  val inDecode  = state === State.decode
  val execA = MuxLookup(decoder.ctrl.aluSrcA, a)(Seq(
    AluASrc.zero -> 0.U,   // lui: 0 + imm
    AluASrc.pc   -> oldPC, // auipc: own PC + imm (pc already advanced by 4)
  ))
  val alu = Module(new ALU)
  alu.a  := Mux(inExecute, execA, Mux(inDecode, oldPC, pc))
  alu.b  := Mux(inExecute, Mux(decoder.ctrl.aluSrc, immGen.imm, b), Mux(inDecode, immGen.imm, 4.U))
  alu.op := Mux(inExecute, decoder.ctrl.aluOp, ALUOp.add)

  // Data memory is accessed only in Memory. IR is stable for several cycles, so the decoder's
  // memWrite/memRead are already high in Decode/Execute/Writeback; an ungated write there would
  // clobber memory with a stale aluOut/B. Gating the read too makes the memory output valid
  // only in Memory (0 in Writeback), so MDR must hold the load data.
  val dmem = Module(new DataMemory)
  dmem.io.addr      := aluOut
  dmem.io.writeData := b
  dmem.io.funct3    := ir(14, 12) // byte/half/word width, straight from IR as in single-cycle
  dmem.io.memWrite  := state === State.memory && decoder.ctrl.memWrite
  dmem.io.memRead   := state === State.memory && decoder.ctrl.memRead

  // Branch decision, same rule as Step 9: aluOp sub -> "is zero" (beq condition),
  // slt/sltu -> "is one" (blt/bltu condition). funct3[0] = 1 (bne/bge/bgeu) inverts it.
  // Evaluated in Execute on the ALU's A-vs-B compare.
  val branchRawCond = Mux(decoder.ctrl.aluOp === ALUOp.sub, alu.out === 0.U, alu.out === 1.U)
  val branchTaken   = decoder.ctrl.branch && (branchRawCond =/= ir(12))
  val jalrTarget    = Cat(alu.out(31, 1), 0.U(1.W)) // (rs1 + imm) with LSB cleared, per spec

  // Instruction class comes from decoder signals, not the opcode. An undefined opcode hits the
  // all-zero default row, matches none of these, and goes Decode -> Fetch.
  val isMemInst = decoder.ctrl.memRead || decoder.ctrl.memWrite
  val executes  = decoder.ctrl.regWrite || decoder.ctrl.memWrite || decoder.ctrl.branch

  switch(state) {
    is(State.fetch) {
      ir    := fetched
      oldPC := pc
      pc    := alu.out
      state := State.decode
    }
    is(State.decode) {
      a      := regFile.io.readData1
      b      := regFile.io.readData2
      aluOut := alu.out // branch/JAL target; Execute overwrites it for every other instruction
      state  := Mux(executes, State.execute, State.fetch)
    }
    is(State.execute) {
      when(decoder.ctrl.branch) {
        when(branchTaken) { pc := aluOut } // target was latched in Decode; aluOut is left untouched
      }.elsewhen(decoder.ctrl.jump) {
        // Simultaneous assignment on one clock edge (a swap, no temp): pc gets the target,
        // aluOut gets the link address (old pc = oldPC + 4). Writeback then writes aluOut to rd.
        pc     := Mux(decoder.ctrl.aluSrc, jalrTarget, aluOut) // jalr (aluSrc set): rs1 + imm; jal: Decode target
        aluOut := pc
      }.otherwise {
        aluOut := alu.out
      }
      state := Mux(isMemInst, State.memory, Mux(decoder.ctrl.branch, State.fetch, State.writeback))
    }
    is(State.memory) {
      mdr   := dmem.io.readData // store loads 0 here, nobody reads it
      state := Mux(decoder.ctrl.memRead, State.writeback, State.fetch) // store ends here
    }
    is(State.writeback) {
      state := State.fetch
    }
  }

  io.state     := state
  io.pc        := pc
  io.oldPc     := oldPC
  io.ir        := ir
  io.a         := a
  io.b         := b
  io.aluOut    := aluOut
  io.mdr       := mdr
  io.memOut    := dmem.io.readData
  io.debugRegs := regFile.io.debugRegs
}
