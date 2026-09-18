package core

import chisel3._
import chisel3.util._

// ALU의 A 입력원. R/I/Load/Store/Branch는 rs1, LUI는 0(imm을 그대로 통과), AUIPC는 PC.
object AluASrc extends ChiselEnum {
  val rs1, zero, pc = Value
}

class ControlSignals extends Bundle {
  val aluOp    = ALUOp()
  val aluSrc   = Bool() // ALU B 입력: 0 = rs2, 1 = immediate. JAL/JALR 구분에도 재사용(JALR만 Y)
  val aluSrcA  = AluASrc()
  val immSel   = ImmType()
  val regWrite = Bool()
  val memRead  = Bool()
  val memWrite = Bool()
  val memToReg = Bool() // 0 = ALU 결과, 1 = 메모리 read data
  val branch   = Bool()
  val jump     = Bool()
}

class Decoder extends RawModule {
  val inst = IO(Input(UInt(32.W)))
  val ctrl = IO(Output(new ControlSignals))

  val opcode = inst(6, 0)
  val funct3 = inst(14, 12)
  val funct7 = inst(31, 25)
  val key    = Cat(funct7, funct3, opcode) // 7+3+7 = 17비트, 테이블 주소

  def pat(f7: String, f3: String, op: String): BitPat = BitPat("b" + f7 + f3 + op)

  def row(
      aluOp: ALUOp.Type,
      aluSrc: Bool,
      immSel: ImmType.Type,
      regWrite: Bool,
      memRead: Bool,
      memWrite: Bool,
      memToReg: Bool,
      branch: Bool,
      jump: Bool,
      aluSrcA: AluASrc.Type = AluASrc.rs1,
  ): List[UInt] = List(
    aluOp.asUInt,
    aluSrc.asUInt,
    immSel.asUInt,
    regWrite.asUInt,
    memRead.asUInt,
    memWrite.asUInt,
    memToReg.asUInt,
    branch.asUInt,
    jump.asUInt,
    aluSrcA.asUInt,
  )

  val N = false.B
  val Y = true.B

  // 잘못된 opcode(default) = NOP 취급: 아무것도 쓰지 않음
  val default = row(ALUOp.add, N, ImmType.itype, N, N, N, N, N, N)

  val table = Array(
    // R-type (immSel은 안 씀 — don't-care, 관례상 itype) ---------------------
    pat("0000000", "000", "0110011") -> row(ALUOp.add, N, ImmType.itype, Y, N, N, N, N, N),  // add
    pat("0100000", "000", "0110011") -> row(ALUOp.sub, N, ImmType.itype, Y, N, N, N, N, N),  // sub
    pat("0000000", "001", "0110011") -> row(ALUOp.sll, N, ImmType.itype, Y, N, N, N, N, N),  // sll
    pat("0000000", "010", "0110011") -> row(ALUOp.slt, N, ImmType.itype, Y, N, N, N, N, N),  // slt
    pat("0000000", "011", "0110011") -> row(ALUOp.sltu, N, ImmType.itype, Y, N, N, N, N, N), // sltu
    pat("0000000", "100", "0110011") -> row(ALUOp.xor, N, ImmType.itype, Y, N, N, N, N, N),  // xor
    pat("0000000", "101", "0110011") -> row(ALUOp.srl, N, ImmType.itype, Y, N, N, N, N, N),  // srl
    pat("0100000", "101", "0110011") -> row(ALUOp.sra, N, ImmType.itype, Y, N, N, N, N, N),  // sra
    pat("0000000", "110", "0110011") -> row(ALUOp.or, N, ImmType.itype, Y, N, N, N, N, N),   // or
    pat("0000000", "111", "0110011") -> row(ALUOp.and, N, ImmType.itype, Y, N, N, N, N, N),  // and

    // I-type ALU (aluSrc=1, immSel=itype) ------------------------------------
    pat("???????", "000", "0010011") -> row(ALUOp.add, Y, ImmType.itype, Y, N, N, N, N, N),  // addi
    pat("???????", "010", "0010011") -> row(ALUOp.slt, Y, ImmType.itype, Y, N, N, N, N, N),  // slti
    pat("???????", "011", "0010011") -> row(ALUOp.sltu, Y, ImmType.itype, Y, N, N, N, N, N), // sltiu
    pat("???????", "100", "0010011") -> row(ALUOp.xor, Y, ImmType.itype, Y, N, N, N, N, N),  // xori
    pat("???????", "110", "0010011") -> row(ALUOp.or, Y, ImmType.itype, Y, N, N, N, N, N),   // ori
    pat("???????", "111", "0010011") -> row(ALUOp.and, Y, ImmType.itype, Y, N, N, N, N, N),  // andi
    pat("0000000", "001", "0010011") -> row(ALUOp.sll, Y, ImmType.itype, Y, N, N, N, N, N),  // slli
    pat("0000000", "101", "0010011") -> row(ALUOp.srl, Y, ImmType.itype, Y, N, N, N, N, N),  // srli
    pat("0100000", "101", "0010011") -> row(ALUOp.sra, Y, ImmType.itype, Y, N, N, N, N, N),  // srai

    // Load (byte폭 선택은 funct3을 datapath가 직접 읽음 — control 신호는 공통) ---
    pat("???????", "???", "0000011") -> row(ALUOp.add, Y, ImmType.itype, Y, Y, N, Y, N, N),

    // Store -------------------------------------------------------------------
    pat("???????", "???", "0100011") -> row(ALUOp.add, Y, ImmType.stype, N, N, Y, N, N, N),

    // Branch (aluOp는 datapath가 taken 여부를 판단할 비교 종류를 고른 것) --------
    pat("???????", "000", "1100011") -> row(ALUOp.sub, N, ImmType.btype, N, N, N, N, Y, N),  // beq
    pat("???????", "001", "1100011") -> row(ALUOp.sub, N, ImmType.btype, N, N, N, N, Y, N),  // bne
    pat("???????", "100", "1100011") -> row(ALUOp.slt, N, ImmType.btype, N, N, N, N, Y, N),  // blt
    pat("???????", "101", "1100011") -> row(ALUOp.slt, N, ImmType.btype, N, N, N, N, Y, N),  // bge
    pat("???????", "110", "1100011") -> row(ALUOp.sltu, N, ImmType.btype, N, N, N, N, Y, N), // bltu
    pat("???????", "111", "1100011") -> row(ALUOp.sltu, N, ImmType.btype, N, N, N, N, Y, N), // bgeu

    // LUI / AUIPC (funct3 무관, opcode만으로 결정). aluSrcA만 다르고 나머진 동일 --
    pat("???????", "???", "0110111") -> row(ALUOp.add, Y, ImmType.utype, Y, N, N, N, N, N, AluASrc.zero), // lui: 0 + imm
    pat("???????", "???", "0010111") -> row(ALUOp.add, Y, ImmType.utype, Y, N, N, N, N, N, AluASrc.pc),   // auipc: pc + imm

    // JAL / JALR. aluSrc로 둘을 구분(jal=N, jalr=Y) — datapath의 next-PC mux가 이걸로 target을 고른다.
    pat("???????", "???", "1101111") -> row(ALUOp.add, N, ImmType.jtype, Y, N, N, N, N, Y), // jal
    pat("???????", "000", "1100111") -> row(ALUOp.add, Y, ImmType.itype, Y, N, N, N, N, Y), // jalr
  )

  val decoded = ListLookup(key, default, table)

  // ChiselEnum은 하드웨어 신호(비-리터럴)를 asTypeOf로 바로 캐스팅하면 경고
  // .safe는 (값, 유효성 Bool) 쌍을 주는데, 테이블이 모든 opcode 조합을 커버하므로 유효성은 버림
  ctrl.aluOp := ALUOp.safe(decoded(0))._1
  ctrl.aluSrc := decoded(1).asBool
  ctrl.immSel := ImmType.safe(decoded(2))._1
  ctrl.regWrite := decoded(3).asBool
  ctrl.memRead := decoded(4).asBool
  ctrl.memWrite := decoded(5).asBool
  ctrl.memToReg := decoded(6).asBool
  ctrl.branch := decoded(7).asBool
  ctrl.jump := decoded(8).asBool
  ctrl.aluSrcA := AluASrc.safe(decoded(9))._1
}
