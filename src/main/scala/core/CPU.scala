package core

import chisel3._
import chisel3.util._

// RV32I 싱글사이클 datapath — R/I-type ALU, load/store, branch/jump까지 전부 대상.
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

  val funct3 = inst(14, 12)

  val regFile = Module(new RegisterFile)
  regFile.io.rs1 := inst(19, 15)
  regFile.io.rs2 := inst(24, 20)
  regFile.io.rd  := inst(11, 7)

  val alu = Module(new ALU)
  alu.a := MuxLookup(decoder.ctrl.aluSrcA, regFile.io.readData1)(Seq(
    AluASrc.zero -> 0.U, // lui: 0 + imm = imm 그대로
    AluASrc.pc   -> pc,  // auipc: pc + imm
  ))
  alu.b  := Mux(decoder.ctrl.aluSrc, immGen.imm, regFile.io.readData2)
  alu.op := decoder.ctrl.aluOp

  val dmem = Module(new DataMemory)
  dmem.io.addr      := alu.out              // load/store 주소 = rs1 + imm (ALU가 계산)
  dmem.io.writeData := regFile.io.readData2 // 저장할 값은 aluSrc 무관하게 rs2 원본
  dmem.io.memWrite  := decoder.ctrl.memWrite
  dmem.io.memRead   := decoder.ctrl.memRead
  dmem.io.funct3    := funct3               // byte/half/word 폭 선택은 decoder를 안 거치고 직접

  // 분기 판정: aluOp가 sub면 "0인가"(beq 조건), slt/sltu면 "1인가"(blt/bltu 조건)를 본다.
  // funct3 최하위 비트가 1인 명령(bne/bge/bgeu)은 그 조건을 뒤집는다.
  val branchRawCond = Mux(decoder.ctrl.aluOp === ALUOp.sub, alu.out === 0.U, alu.out === 1.U)
  val branchTaken   = decoder.ctrl.branch && (branchRawCond =/= funct3(0).asBool)

  // JAL/JALR/branch taken이 모두 "PC 기준 상대 이동" 아니면 "레지스터 기준 절대 이동" 둘 중 하나.
  // JAL과 branch는 imm이 immSel로 이미 갈라져 있어서 같은 pc+imm 덧셈기를 공유한다.
  val pcPlus4     = pc + 4.U
  val pcRelTarget = pc + immGen.imm
  val jalrTarget  = Cat(alu.out(31, 1), 0.U(1.W)) // rs1+imm의 LSB를 0으로 (스펙 요구사항)

  val nextPC = Mux(
    decoder.ctrl.jump,
    Mux(decoder.ctrl.aluSrc, jalrTarget, pcRelTarget), // jalr(aluSrc=Y) vs jal(aluSrc=N)
    Mux(branchTaken, pcRelTarget, pcPlus4),
  )

  // JAL/JALR는 rd에 복귀주소(PC+4)를 쓴다 — load/ALU 결과와는 다른 세 번째 write-back 소스.
  regFile.io.writeData := Mux(
    decoder.ctrl.jump,
    pcPlus4,
    Mux(decoder.ctrl.memToReg, dmem.io.readData, alu.out),
  )
  regFile.io.writeEnable := decoder.ctrl.regWrite

  io.debugRegs := regFile.io.debugRegs

  pc := nextPC
}
