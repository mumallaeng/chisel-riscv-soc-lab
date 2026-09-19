package core

import chisel3._

// init: 검증용 초기 레지스터 값(index -> 값). 기본은 전부 0이라 기존 사용처엔 영향 없음.
class RegisterFile(init: Map[Int, BigInt] = Map.empty) extends Module {
  require(init.getOrElse(0, BigInt(0)) == 0, "x0 is hardwired to zero")

  val io = IO(new Bundle {
    val rs1         = Input(UInt(5.W))
    val rs2         = Input(UInt(5.W))
    val rd          = Input(UInt(5.W))
    val writeData   = Input(UInt(32.W))
    val writeEnable = Input(Bool())
    val readData1   = Output(UInt(32.W))
    val readData2   = Output(UInt(32.W))
    val debugRegs   = Output(Vec(32, UInt(32.W))) // 검증용 전체 덤프, datapath 배선엔 안 씀
  })

  val regs = RegInit(VecInit(Seq.tabulate(32)(i => init.getOrElse(i, BigInt(0)).U(32.W))))

  io.readData1  := regs(io.rs1)
  io.readData2  := regs(io.rs2)
  io.debugRegs  := regs

  when(io.writeEnable && io.rd =/= 0.U) {
    regs(io.rd) := io.writeData
  }
}
