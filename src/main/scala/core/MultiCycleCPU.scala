package core

import chisel3._
import chisel3.util._

object State extends ChiselEnum {
  val fetch, decode, execute, memory, writeback = Value
}

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
    val memOut    = Output(UInt(32.W))
    val debugRegs = Output(Vec(32, UInt(32.W)))
  })

  val state  = RegInit(State.fetch)
  val pc     = RegInit(0.U(32.W))
  val oldPC  = RegInit(0.U(32.W))
  val ir     = RegInit(0.U(32.W))
  val a      = RegInit(0.U(32.W))
  val b      = RegInit(0.U(32.W))
  val aluOut = RegInit(0.U(32.W))
  val mdr    = RegInit(0.U(32.W))

  val imem = VecInit(program)
  val fetched = if (program.length <= 1) imem(0.U) else {
    val idxWidth = log2Ceil(program.length)
    imem(pc(idxWidth + 1, 2))
  }

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
  regFile.io.writeEnable := state === State.writeback

  val inExecute = state === State.execute
  val inDecode  = state === State.decode
  val execA = MuxLookup(decoder.ctrl.aluSrcA, a)(Seq(
    AluASrc.zero -> 0.U,
    AluASrc.pc   -> oldPC,
  ))
  val alu = Module(new ALU)
  alu.a  := Mux(inExecute, execA, Mux(inDecode, oldPC, pc))
  alu.b  := Mux(inExecute, Mux(decoder.ctrl.aluSrc, immGen.imm, b), Mux(inDecode, immGen.imm, 4.U))
  alu.op := Mux(inExecute, decoder.ctrl.aluOp, ALUOp.add)

  val dmem = Module(new DataMemory)
  dmem.io.addr      := aluOut
  dmem.io.writeData := b
  dmem.io.funct3    := ir(14, 12)
  dmem.io.memWrite  := state === State.memory && decoder.ctrl.memWrite
  dmem.io.memRead   := state === State.memory && decoder.ctrl.memRead

  val branchRawCond = Mux(decoder.ctrl.aluOp === ALUOp.sub, alu.out === 0.U, alu.out === 1.U)
  val branchTaken   = decoder.ctrl.branch && (branchRawCond =/= ir(12))
  val jalrTarget    = Cat(alu.out(31, 1), 0.U(1.W))

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
      aluOut := alu.out
      state  := Mux(executes, State.execute, State.fetch)
    }
    is(State.execute) {
      when(decoder.ctrl.branch) {
        when(branchTaken) { pc := aluOut }
      }.elsewhen(decoder.ctrl.jump) {
        pc     := Mux(decoder.ctrl.aluSrc, jalrTarget, aluOut)
        aluOut := pc
      }.otherwise {
        aluOut := alu.out
      }
      state := Mux(isMemInst, State.memory, Mux(decoder.ctrl.branch, State.fetch, State.writeback))
    }
    is(State.memory) {
      mdr   := dmem.io.readData
      state := Mux(decoder.ctrl.memRead, State.writeback, State.fetch)
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
