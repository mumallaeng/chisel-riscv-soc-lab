package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

class CPUSpec extends AnyFunSpec with ChiselSim {
  // --- 명령어를 하나의 ADT로 기술하고, encode(하드웨어에 넣을 32비트 워드)와
  //     interpret(기대 레지스터 값)를 각각 독립적으로 이 ADT에서 유도한다.
  //     같은 실수를 encode/interpret 양쪽에 동시에 하면 못 걸러내므로,
  //     interpret은 ALU.scala/Decoder.scala 코드를 절대 참조하지 않고
  //     RV32I 스펙 그대로(Scala 네이티브 Int 연산)로 직접 계산한다.
  sealed trait Instr
  case class R(op: String, rd: Int, rs1: Int, rs2: Int) extends Instr
  case class I(op: String, rd: Int, rs1: Int, imm: Int) extends Instr
  case class L(op: String, rd: Int, rs1: Int, imm: Int) extends Instr // load: lb/lh/lw/lbu/lhu
  case class S(op: String, rs1: Int, rs2: Int, imm: Int) extends Instr // store: sb/sh/sw, rs2를 (rs1+imm)에 저장
  case class B(op: String, rs1: Int, rs2: Int, imm: Int) extends Instr // branch, imm = 이 명령어 자신의 pc 기준 byte offset(짝수)
  case class U(op: String, rd: Int, imm20: Int) extends Instr         // lui/auipc, imm20 = inst[31:12] (실제 값은 imm20<<12)
  case class Jal(rd: Int, imm: Int) extends Instr                     // pc 기준 byte offset(짝수)
  case class Jalr(rd: Int, rs1: Int, imm: Int) extends Instr          // target = (rs1+imm) & ~1

  def rFields(op: String): (Int, Int) = op match { // (funct7, funct3)
    case "add"  => (0x00, 0)
    case "sub"  => (0x20, 0)
    case "sll"  => (0x00, 1)
    case "slt"  => (0x00, 2)
    case "sltu" => (0x00, 3)
    case "xor"  => (0x00, 4)
    case "srl"  => (0x00, 5)
    case "sra"  => (0x20, 5)
    case "or"   => (0x00, 6)
    case "and"  => (0x00, 7)
  }
  val shiftOps = Set("slli", "srli", "srai")
  def iFields(op: String): (Int, Int) = op match { // (shift일 때 funct7, funct3)
    case "addi"  => (0x00, 0)
    case "slli"  => (0x00, 1)
    case "slti"  => (0x00, 2)
    case "sltiu" => (0x00, 3)
    case "xori"  => (0x00, 4)
    case "srli"  => (0x00, 5)
    case "srai"  => (0x20, 5)
    case "ori"   => (0x00, 6)
    case "andi"  => (0x00, 7)
  }
  def lFunct3(op: String): Int = op match {
    case "lb" => 0; case "lh" => 1; case "lw" => 2; case "lbu" => 4; case "lhu" => 5
  }
  def sFunct3(op: String): Int = op match {
    case "sb" => 0; case "sh" => 1; case "sw" => 2
  }
  def bFunct3(op: String): Int = op match {
    case "beq" => 0; case "bne" => 1; case "blt" => 4; case "bge" => 5; case "bltu" => 6; case "bgeu" => 7
  }

  def encode(instr: Instr): BigInt = instr match {
    case R(op, rd, rs1, rs2) =>
      val (f7, f3) = rFields(op)
      (BigInt(f7) << 25) | (BigInt(rs2) << 20) | (BigInt(rs1) << 15) |
        (BigInt(f3) << 12) | (BigInt(rd) << 7) | BigInt(0x33)
    case I(op, rd, rs1, imm) if shiftOps(op) =>
      val (f7, f3) = iFields(op)
      val shamt    = imm & 0x1f
      (BigInt(f7) << 25) | (BigInt(shamt) << 20) | (BigInt(rs1) << 15) |
        (BigInt(f3) << 12) | (BigInt(rd) << 7) | BigInt(0x13)
    case I(op, rd, rs1, imm) =>
      val (_, f3) = iFields(op)
      val imm12   = imm & 0xfff
      (BigInt(imm12) << 20) | (BigInt(rs1) << 15) | (BigInt(f3) << 12) |
        (BigInt(rd) << 7) | BigInt(0x13)
    case L(op, rd, rs1, imm) =>
      val f3    = lFunct3(op)
      val imm12 = imm & 0xfff
      (BigInt(imm12) << 20) | (BigInt(rs1) << 15) | (BigInt(f3) << 12) |
        (BigInt(rd) << 7) | BigInt(0x03)
    case S(op, rs1, rs2, imm) =>
      val f3      = sFunct3(op)
      val v       = imm & 0xfff
      val immHi   = (v >> 5) & 0x7f
      val immLo   = v & 0x1f
      (BigInt(immHi) << 25) | (BigInt(rs2) << 20) | (BigInt(rs1) << 15) |
        (BigInt(f3) << 12) | (BigInt(immLo) << 7) | BigInt(0x23)
    case B(op, rs1, rs2, imm) =>
      val f3 = bFunct3(op)
      val v  = imm & 0x1ffe // bit0은 항상 0
      (BigInt((v >> 12) & 0x1) << 31) | (BigInt((v >> 5) & 0x3f) << 25) | (BigInt(rs2) << 20) |
        (BigInt(rs1) << 15) | (BigInt(f3) << 12) | (BigInt((v >> 1) & 0xf) << 8) |
        (BigInt((v >> 11) & 0x1) << 7) | BigInt(0x63)
    case U(op, rd, imm20) =>
      val opc = if (op == "lui") 0x37 else 0x17 // auipc
      (BigInt(imm20 & 0xfffff) << 12) | (BigInt(rd) << 7) | BigInt(opc)
    case Jal(rd, imm) =>
      val v = imm & 0x1ffffe // bit0은 항상 0
      (BigInt((v >> 20) & 0x1) << 31) | (BigInt((v >> 12) & 0xff) << 12) |
        (BigInt((v >> 11) & 0x1) << 20) | (BigInt((v >> 1) & 0x3ff) << 21) |
        (BigInt(rd) << 7) | BigInt(0x6f)
    case Jalr(rd, rs1, imm) =>
      val imm12 = imm & 0xfff
      (BigInt(imm12) << 20) | (BigInt(rs1) << 15) | (BigInt(rd) << 7) | BigInt(0x67) // funct3=000
  }

  // cycles만큼, pc를 따라가며 한 사이클에 한 명령을 실행한다(분기/점프로 순서가 바뀔 수 있어
  // 이제 prog를 순서대로 foreach 못 돌고, 하드웨어처럼 주소로 다음 명령을 찾아야 한다).
  def interpret(prog: Seq[Instr], cycles: Int): Array[Int] = {
    val regs   = Array.fill(32)(0)
    val mem    = Array.fill(1024)(0.toByte) // DataMemory 기본 크기(256 word)와 맞춘 byte 배열
    val byAddr = prog.zipWithIndex.map { case (instr, i) => (i * 4, instr) }.toMap
    def w(rd: Int, v: Int): Unit = if (rd != 0) regs(rd) = v

    var pc = 0
    for (_ <- 0 until cycles) {
      var nextPc = pc + 4
      byAddr(pc) match {
      case R("add", rd, rs1, rs2)  => w(rd, regs(rs1) + regs(rs2))
      case R("sub", rd, rs1, rs2)  => w(rd, regs(rs1) - regs(rs2))
      case R("sll", rd, rs1, rs2)  => w(rd, regs(rs1) << (regs(rs2) & 0x1f))
      case R("srl", rd, rs1, rs2)  => w(rd, regs(rs1) >>> (regs(rs2) & 0x1f))
      case R("sra", rd, rs1, rs2)  => w(rd, regs(rs1) >> (regs(rs2) & 0x1f))
      case R("slt", rd, rs1, rs2)  => w(rd, if (regs(rs1) < regs(rs2)) 1 else 0)
      case R("sltu", rd, rs1, rs2) => w(rd, if (java.lang.Integer.compareUnsigned(regs(rs1), regs(rs2)) < 0) 1 else 0)
      case R("xor", rd, rs1, rs2)  => w(rd, regs(rs1) ^ regs(rs2))
      case R("or", rd, rs1, rs2)   => w(rd, regs(rs1) | regs(rs2))
      case R("and", rd, rs1, rs2)  => w(rd, regs(rs1) & regs(rs2))
      case I("addi", rd, rs1, imm)  => w(rd, regs(rs1) + imm)
      case I("slti", rd, rs1, imm)  => w(rd, if (regs(rs1) < imm) 1 else 0)
      case I("sltiu", rd, rs1, imm) => w(rd, if (java.lang.Integer.compareUnsigned(regs(rs1), imm) < 0) 1 else 0)
      case I("xori", rd, rs1, imm)  => w(rd, regs(rs1) ^ imm)
      case I("ori", rd, rs1, imm)   => w(rd, regs(rs1) | imm)
      case I("andi", rd, rs1, imm)  => w(rd, regs(rs1) & imm)
      case I("slli", rd, rs1, imm)  => w(rd, regs(rs1) << (imm & 0x1f))
      case I("srli", rd, rs1, imm)  => w(rd, regs(rs1) >>> (imm & 0x1f))
      case I("srai", rd, rs1, imm)  => w(rd, regs(rs1) >> (imm & 0x1f))
      case L("lb", rd, rs1, imm)  => w(rd, mem(regs(rs1) + imm).toInt) // Byte->Int: 자동 부호 확장
      case L("lbu", rd, rs1, imm) => w(rd, mem(regs(rs1) + imm) & 0xff)
      case L("lh", rd, rs1, imm) =>
        val a = regs(rs1) + imm
        w(rd, (mem(a + 1).toInt << 8) | (mem(a) & 0xff)) // hi.toInt 부호 확장이 그대로 위로 퍼짐
      case L("lhu", rd, rs1, imm) =>
        val a = regs(rs1) + imm
        w(rd, ((mem(a + 1) & 0xff) << 8) | (mem(a) & 0xff))
      case L("lw", rd, rs1, imm) =>
        val a = regs(rs1) + imm
        val b = (0 to 3).map(i => mem(a + i) & 0xff)
        w(rd, (b(3) << 24) | (b(2) << 16) | (b(1) << 8) | b(0))
      case S("sb", rs1, rs2, imm) =>
        mem(regs(rs1) + imm) = regs(rs2).toByte
      case S("sh", rs1, rs2, imm) =>
        val a = regs(rs1) + imm
        mem(a)     = regs(rs2).toByte
        mem(a + 1) = (regs(rs2) >>> 8).toByte
      case S("sw", rs1, rs2, imm) =>
        val a = regs(rs1) + imm
        for (i <- 0 to 3) mem(a + i) = (regs(rs2) >>> (8 * i)).toByte
      case B("beq", rs1, rs2, imm)  => if (regs(rs1) == regs(rs2)) nextPc = pc + imm
      case B("bne", rs1, rs2, imm)  => if (regs(rs1) != regs(rs2)) nextPc = pc + imm
      case B("blt", rs1, rs2, imm)  => if (regs(rs1) < regs(rs2)) nextPc = pc + imm
      case B("bge", rs1, rs2, imm)  => if (regs(rs1) >= regs(rs2)) nextPc = pc + imm
      case B("bltu", rs1, rs2, imm) => if (java.lang.Integer.compareUnsigned(regs(rs1), regs(rs2)) < 0) nextPc = pc + imm
      case B("bgeu", rs1, rs2, imm) => if (java.lang.Integer.compareUnsigned(regs(rs1), regs(rs2)) >= 0) nextPc = pc + imm
      case U("lui", rd, imm20)   => w(rd, imm20 << 12)
      case U("auipc", rd, imm20) => w(rd, pc + (imm20 << 12))
      case Jal(rd, imm) =>
        w(rd, pc + 4); nextPc = pc + imm
      case Jalr(rd, rs1, imm) =>
        w(rd, pc + 4); nextPc = (regs(rs1) + imm) & ~1
      case other => sys.error(s"golden model: unsupported instr $other")
      }
      pc = nextPc
    }
    regs
  }

  def run(prog: Seq[Instr], cycles: Int = -1): Unit = {
    val n        = if (cycles < 0) prog.length else cycles
    val words    = prog.map(i => encode(i).U(32.W))
    val expected = interpret(prog, n)
    simulate(new CPU(words)) { dut =>
      dut.clock.step(n)
      for (r <- 0 until 32) {
        dut.io.debugRegs(r).expect(expected(r).S(32.W).asUInt)
      }
    }
  }

  describe("CPU (R/I-type 싱글사이클)") {
    it("addi로 값을 채우고 add/sub로 조합한다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 5),
        I("addi", rd = 2, rs1 = 0, imm = 3),
        R("add", rd = 3, rs1 = 1, rs2 = 2),
        R("sub", rd = 4, rs1 = 1, rs2 = 2),
      ))
    }
    it("논리(and/or/xor) + 시프트(slli/srli)") {
      run(Seq(
        I("addi", 1, 0, 0xf0),
        I("addi", 2, 0, 0x0f),
        R("and", 3, 1, 2),
        R("or", 4, 1, 2),
        R("xor", 5, 1, 2),
        I("slli", 6, 1, 4),
        I("srli", 7, 1, 2),
      ))
    }
    it("음수 immediate가 부호 확장돼 signed/unsigned 비교가 갈린다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = -1), // x1 = 0xFFFFFFFF
        I("slti", rd = 2, rs1 = 1, imm = 0),  // -1 < 0 (signed)  -> 1
        I("sltiu", rd = 3, rs1 = 1, imm = 0), // 0xFFFFFFFF < 0 (unsigned) -> 0
      ))
    }
    it("레지스터 피연산자 시프트양은 하위 5비트만 쓴다 (sra 부호 유지)") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = -8), // x1 = 0xFFFFFFF8 (-8)
        I("addi", rd = 2, rs1 = 0, imm = 1),  // x2 = 1
        R("sra", rd = 3, rs1 = 1, rs2 = 2),   // -8 >> 1 = -4, 부호 비트 유지
        R("srl", rd = 4, rs1 = 1, rs2 = 2),   // 논리 시프트는 0으로 채움 -> 큰 양수
      ))
    }
    it("rd=x0을 겨냥해도 x0은 0으로 고정된다") {
      run(Seq(I("addi", rd = 0, rs1 = 0, imm = 123)))
    }
  }

  describe("CPU (load/store)") {
    it("sw로 저장한 word를 lw로 그대로 읽는다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 0),          // x1 = base addr 0
        I("addi", rd = 2, rs1 = 0, imm = -100),       // x2 = 저장할 값
        S("sw", rs1 = 1, rs2 = 2, imm = 0),
        L("lw", rd = 3, rs1 = 1, imm = 0),
      ))
    }
    it("sb/lb, lbu: 부호 있는 바이트를 저장/로드하며 부호·무부호 확장이 갈린다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 8),   // base
        I("addi", rd = 2, rs1 = 0, imm = -1),  // 0xFFFFFFFF, 하위 바이트 = 0xFF
        S("sb", rs1 = 1, rs2 = 2, imm = 0),
        L("lb", rd = 3, rs1 = 1, imm = 0),     // sign-extend -> 0xFFFFFFFF
        L("lbu", rd = 4, rs1 = 1, imm = 0),    // zero-extend -> 0x000000FF
      ))
    }
    it("sh/lh는 word 안 상위 하프에도 정확히 배치된다 (offset != 0)") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 16),
        I("addi", rd = 2, rs1 = 0, imm = 2000), // I-type immediate는 signed 12비트(-2048~2047) 안이어야 sign-extend가 안 걸린다
        // Mem은 RegInit처럼 0으로 초기화되지 않는다(uninitialized) -- 건드리지 않은 자리를
        // 0으로 가정하면 안 되므로, 검사 전에 word 전체를 SW로 명시적으로 0을 찍어둔다.
        S("sw", rs1 = 1, rs2 = 0, imm = 0),
        S("sh", rs1 = 1, rs2 = 2, imm = 2),  // addr 18 = word 16의 상위 하프
        L("lh", rd = 3, rs1 = 1, imm = 2),
        L("lw", rd = 4, rs1 = 1, imm = 0),   // 하위 하프는 (SW로 찍어둔) 0이어야 함
      ))
    }
    it("byte store가 인접 바이트를 안 건드린다 (byte-enable 확인)") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 20),
        I("addi", rd = 2, rs1 = 0, imm = -1),
        S("sw", rs1 = 1, rs2 = 2, imm = 0),   // word 전체를 0xFFFFFFFF로
        I("addi", rd = 3, rs1 = 0, imm = 0),
        S("sb", rs1 = 1, rs2 = 3, imm = 1),   // byte lane 1만 0x00
        L("lw", rd = 4, rs1 = 1, imm = 0),    // 0xFFFF00FF 기대
      ))
    }
  }

  describe("CPU (branch/jump)") {
    it("beq taken이면 다음 명령을 건너뛴다") {
      run(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 5),
          I("addi", rd = 2, rs1 = 0, imm = 5),
          B("beq", rs1 = 1, rs2 = 2, imm = 8),   // pc=8, 같으니 taken -> pc=16으로
          I("addi", rd = 3, rs1 = 0, imm = 999), // pc=12, 건너뛰어짐
          I("addi", rd = 4, rs1 = 0, imm = 42),  // pc=16, 여기부터 재개
        ),
        cycles = 4, // pc=0,4,8,16 (12는 안 밟음)
      )
    }
    it("bne not-taken이면 그냥 다음 명령으로 진행한다") {
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = 5),
        I("addi", rd = 2, rs1 = 0, imm = 5),
        B("bne", rs1 = 1, rs2 = 2, imm = 8), // 같으니 bne는 not-taken
        I("addi", rd = 3, rs1 = 0, imm = 77),
        I("addi", rd = 4, rs1 = 0, imm = 88),
      )) // cycles 생략 -> prog.length(5), 분기 없이 순서대로
    }
    it("blt(signed)와 bltu(unsigned)가 같은 비트패턴을 다르게 판정한다") {
      run(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = -1), // x1 = 0xFFFFFFFF
          I("addi", rd = 2, rs1 = 0, imm = 0),  // x2 = 0
          B("blt", rs1 = 1, rs2 = 2, imm = 8),  // signed: -1 < 0 -> taken
          I("addi", rd = 3, rs1 = 0, imm = 111),
          I("addi", rd = 4, rs1 = 0, imm = 222),
        ),
        cycles = 4,
      )
      run(Seq(
        I("addi", rd = 1, rs1 = 0, imm = -1),
        I("addi", rd = 2, rs1 = 0, imm = 0),
        B("bltu", rs1 = 1, rs2 = 2, imm = 8), // unsigned: 0xFFFFFFFF < 0 -> not taken
        I("addi", rd = 3, rs1 = 0, imm = 111),
        I("addi", rd = 4, rs1 = 0, imm = 222),
      )) // not taken -> 5 사이클 다 실행
    }
    it("jal: rd에 복귀주소(pc+4)를 쓰고 pc-relative로 건너뛴다") {
      run(
        Seq(
          Jal(rd = 1, imm = 8),                  // pc=0 -> pc=8, x1 = 0+4 = 4
          I("addi", rd = 2, rs1 = 0, imm = 999), // pc=4, 건너뛰어짐
          I("addi", rd = 3, rs1 = 0, imm = 55),  // pc=8
        ),
        cycles = 2,
      )
    }
    it("jalr: rs1+imm의 LSB를 0으로 자른 주소로 뛴다") {
      run(
        Seq(
          I("addi", rd = 1, rs1 = 0, imm = 13),   // x1 = 13 (홀수, 일부러)
          Jalr(rd = 2, rs1 = 1, imm = 0),         // target = 13 & ~1 = 12, x2 = pc+4 = 8
          I("addi", rd = 3, rs1 = 0, imm = 999),  // pc=8, 건너뛰어짐
          I("addi", rd = 4, rs1 = 0, imm = 77),   // pc=12
        ),
        cycles = 3,
      )
    }
    it("lui/auipc: 상위 20비트 배치 + auipc는 자기 pc를 더한다") {
      run(Seq(
        U("lui", rd = 1, imm20 = 0x12345),
        U("auipc", rd = 2, imm20 = 1), // pc=4 -> x2 = 4 + (1<<12) = 4100
      ))
    }
  }
}
