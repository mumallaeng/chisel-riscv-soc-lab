package core

import chisel3._
import chisel3.util._

// RV32I 싱글사이클 datapath — R/I-type ALU + load/store까지 대상.
// branch/jump용 control 신호(branch/jump)는 Decoder가 이미 내지만
// 여기선 아직 안 물린다(Step 9에서 배선).
class CPU(program: Seq[UInt]) extends Module {
  val io = IO(new Bundle {
    val pc        = Output(UInt(32.W))
    val debugRegs = Output(Vec(32, UInt(32.W)))
  })

  val imem = VecInit(program) // 명령어 ROM, PC로 조합 read
  val pc   = RegInit(0.U(32.W))
  io.pc := pc

  // word address = byte address / 4. 인덱스 폭을 imem 크기에 맞춰 잘라내지 않으면
  // 32비트 전체가 dynamic index로 들어가 불필요하게 큰 mux가 생긴다(W004 경고).
  // 명령어가 1개뿐이면 애초에 인덱싱할 필요가 없어 상수 0으로 고정한다.
  val inst = if (program.length <= 1) imem(0.U) else {
    val idxWidth = log2Ceil(program.length)
    imem(pc(idxWidth + 1, 2))
  }

  val decoder = Module(new Decoder)
  decoder.inst := inst

  val immGen = Module(new ImmGen)
  immGen.inst   := inst
  immGen.immSel := decoder.ctrl.immSel

  val regFile = Module(new RegisterFile)
  regFile.io.rs1 := inst(19, 15)
  regFile.io.rs2 := inst(24, 20)
  regFile.io.rd  := inst(11, 7)

  val alu = Module(new ALU)
  alu.a  := regFile.io.readData1
  alu.b  := Mux(decoder.ctrl.aluSrc, immGen.imm, regFile.io.readData2)
  alu.op := decoder.ctrl.aluOp

  val dmem = Module(new DataMemory)
  dmem.io.addr      := alu.out              // load/store 주소 = rs1 + imm (ALU가 계산)
  dmem.io.writeData := regFile.io.readData2 // 저장할 값은 aluSrc 무관하게 rs2 원본
  dmem.io.memWrite  := decoder.ctrl.memWrite
  dmem.io.funct3    := inst(14, 12)         // byte/half/word 폭 선택은 decoder를 안 거치고 직접

  regFile.io.writeData   := Mux(decoder.ctrl.memToReg, dmem.io.readData, alu.out)
  regFile.io.writeEnable := decoder.ctrl.regWrite

  io.debugRegs := regFile.io.debugRegs

  pc := pc + 4.U // 분기/점프 없음 -> 항상 다음 명령어
}
