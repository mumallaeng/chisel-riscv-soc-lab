package core

import chisel3._
import chisel3.util._

class CPU(program: Seq[UInt]) extends Module {
  val io = IO(new Bundle {
    val pc        = Output(UInt(32.W))
    val debugRegs = Output(Vec(32, UInt(32.W)))
  })

  val imem = VecInit(program)
  val pc   = RegInit(0.U(32.W))
  io.pc := pc

  // clamp the index width so an unbounded PC doesn't force a full 32-bit dynamic index (W004)
  val inst = if (program.length <= 1) imem(0.U) else {
    val idxWidth = log2Ceil(program.length)
    imem(pc(idxWidth + 1, 2))
  }

  val decoder = Module(new Decoder)
  decoder.inst := inst

  val immGen = Module(new ImmGen)
  immGen.inst   := inst
  immGen.immSel := decoder.ctrl.immSel

  val funct3 = inst(14, 12)

  val regFile = Module(new RegisterFile)
  regFile.io.rs1 := inst(19, 15)
  regFile.io.rs2 := inst(24, 20)
  regFile.io.rd  := inst(11, 7)

  val alu = Module(new ALU)
  alu.a := MuxLookup(decoder.ctrl.aluSrcA, regFile.io.readData1)(Seq(
    AluASrc.zero -> 0.U,
    AluASrc.pc   -> pc,
  ))
  alu.b  := Mux(decoder.ctrl.aluSrc, immGen.imm, regFile.io.readData2)
  alu.op := decoder.ctrl.aluOp

  val dmem = Module(new DataMemory)
  dmem.io.addr      := alu.out
  dmem.io.writeData := regFile.io.readData2
  dmem.io.memWrite  := decoder.ctrl.memWrite
  dmem.io.memRead   := decoder.ctrl.memRead
  dmem.io.funct3    := funct3

  val branchRawCond = Mux(decoder.ctrl.aluOp === ALUOp.sub, alu.out === 0.U, alu.out === 1.U)
  val branchTaken   = decoder.ctrl.branch && (branchRawCond =/= funct3(0).asBool)

  val pcPlus4     = pc + 4.U
  val pcRelTarget = pc + immGen.imm
  val jalrTarget  = Cat(alu.out(31, 1), 0.U(1.W))

  val nextPC = Mux(
    decoder.ctrl.jump,
    Mux(decoder.ctrl.aluSrc, jalrTarget, pcRelTarget),
    Mux(branchTaken, pcRelTarget, pcPlus4),
  )

  regFile.io.writeData := Mux(
    decoder.ctrl.jump,
    pcPlus4,
    Mux(decoder.ctrl.memToReg, dmem.io.readData, alu.out),
  )
  regFile.io.writeEnable := decoder.ctrl.regWrite

  io.debugRegs := regFile.io.debugRegs

  pc := nextPC
}
