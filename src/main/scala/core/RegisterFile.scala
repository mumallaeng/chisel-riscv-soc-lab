package core

import chisel3._

class RegisterFile extends Module {
  val io = IO(new Bundle {
    val rs1         = Input(UInt(5.W))
    val rs2         = Input(UInt(5.W))
    val rd          = Input(UInt(5.W))
    val writeData   = Input(UInt(32.W))
    val writeEnable = Input(Bool())
    val readData1   = Output(UInt(32.W))
    val readData2   = Output(UInt(32.W))
  })

  val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  io.readData1 := regs(io.rs1)
  io.readData2 := regs(io.rs2)

  when(io.writeEnable && io.rd =/= 0.U) {
    regs(io.rd) := io.writeData
  }
}
