package core

import chisel3._
import chisel3.util._

object State extends ChiselEnum {
  val fetch, decode = Value
}

// RV32I multi-cycle datapath — 이번 스텝은 FSM 골격(Fetch/Decode)까지만.
// 명령어 하나를 여러 사이클에 나눠 처리하므로 사이클 사이에 값을 붙잡아 둘 상태 레지스터
// (IR/A/B)가 생긴다. ALUOut/MDR/oldPC는 처음 쓰이는 스텝(12/13/14)에서 추가한다.
class MultiCycleCPU(program: Seq[UInt], regInit: Map[Int, BigInt] = Map.empty) extends Module {
  val io = IO(new Bundle {
    val state     = Output(State())
    val pc        = Output(UInt(32.W))
    val ir        = Output(UInt(32.W))
    val a         = Output(UInt(32.W))
    val b         = Output(UInt(32.W))
    val debugRegs = Output(Vec(32, UInt(32.W)))
  })

  val state = RegInit(State.fetch)
  val pc    = RegInit(0.U(32.W))
  val ir    = RegInit(0.U(32.W)) // 실행 중인 명령어를 여러 사이클 동안 붙잡아 둔다
  val a     = RegInit(0.U(32.W)) // rs1 값 래치
  val b     = RegInit(0.U(32.W)) // rs2 값 래치

  // 명령어 메모리는 single-cycle과 같은 Vec ROM(Harvard 유지) — Step 11 "요점 1" 참고
  val imem = VecInit(program)
  val fetched = if (program.length <= 1) imem(0.U) else {
    val idxWidth = log2Ceil(program.length)
    imem(pc(idxWidth + 1, 2))
  }

  val regFile = Module(new RegisterFile(regInit))
  regFile.io.rs1         := ir(19, 15)
  regFile.io.rs2         := ir(24, 20)
  regFile.io.rd          := 0.U
  regFile.io.writeData   := 0.U
  regFile.io.writeEnable := false.B // Writeback 상태가 아직 없다(Step 12)

  // PC+4는 전용 덧셈기 없이 ALU를 빌려 쓴다. 지금은 Fetch만 ALU를 쓰므로 입력이 상수이고,
  // Step 12부터 상태별로 입력을 고르는 mux가 된다.
  val alu = Module(new ALU)
  alu.a  := pc
  alu.b  := 4.U
  alu.op := ALUOp.add

  switch(state) {
    is(State.fetch) {
      ir    := fetched
      pc    := alu.out
      state := State.decode
    }
    is(State.decode) {
      a     := regFile.io.readData1
      b     := regFile.io.readData2
      state := State.fetch // 임시: Step 12에서 opcode에 따라 execute로 분기
    }
  }

  io.state     := state
  io.pc        := pc
  io.ir        := ir
  io.a         := a
  io.b         := b
  io.debugRegs := regFile.io.debugRegs
}
