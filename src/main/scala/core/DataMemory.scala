package core

import chisel3._
import chisel3.util._

// funct3 인코딩(RV32I 표준, load/store 공용): LB/SB=000 LH/SH=001 LW/SW=010 LBU=100 LHU=101
class DataMemory(depthWords: Int = 256) extends Module {
  val io = IO(new Bundle {
    val addr      = Input(UInt(32.W))
    val writeData = Input(UInt(32.W))
    val memWrite  = Input(Bool())
    val funct3    = Input(UInt(3.W))
    val readData  = Output(UInt(32.W))
  })

  val mem = Mem(depthWords, Vec(4, UInt(8.W))) // 4바이트 lane을 가진 word 단위 메모리

  val wordAddr  = io.addr(log2Ceil(depthWords) + 1, 2)
  val byteOff   = io.addr(1, 0)
  val shiftBits = byteOff << 3 // byteOff * 8

  // ---- 읽기: word를 읽고, 대상 바이트/하프워드가 LSB에 오도록 오른쪽으로 밀어 잘라낸다 ----
  val word    = mem.read(wordAddr)
  val wordU   = Cat(word(3), word(2), word(1), word(0)) // 리틀엔디안: lane 0 = LSB
  val aligned = wordU >> shiftBits

  val loadByte = aligned(7, 0)
  val loadHalf = aligned(15, 0)

  io.readData := MuxLookup(io.funct3, wordU)(Seq(
    "b000".U -> Cat(Fill(24, loadByte(7)), loadByte),  // LB
    "b001".U -> Cat(Fill(16, loadHalf(15)), loadHalf), // LH
    "b010".U -> wordU,                                 // LW
    "b100".U -> Cat(0.U(24.W), loadByte),              // LBU
    "b101".U -> Cat(0.U(16.W), loadHalf),              // LHU
  ))

  // ---- 쓰기: 반대로 source data를 왼쪽으로 밀어 대상 lane에 맞춘 뒤, mask로 그 lane만 쓴다 ----
  val shiftedWrite = (io.writeData << shiftBits)(31, 0)
  val storeBytes   = VecInit(Seq.tabulate(4)(i => shiftedWrite(8 * i + 7, 8 * i)))

  val byteMask = VecInit(Seq.tabulate(4)(i => byteOff === i.U))
  val halfMask = Mux(byteOff(1), VecInit(false.B, false.B, true.B, true.B), VecInit(true.B, true.B, false.B, false.B))
  val wordMask = VecInit(Seq.fill(4)(true.B))

  val mask = MuxLookup(io.funct3, wordMask)(Seq(
    "b000".U -> byteMask, // SB
    "b001".U -> halfMask, // SH
    "b010".U -> wordMask, // SW
  ))

  when(io.memWrite) {
    mem.write(wordAddr, storeBytes, mask)
  }
}
