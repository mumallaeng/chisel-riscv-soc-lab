package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim

// RV32I 골든 모델 — Step 7~9까지 CPUSpec 안에 있던 것을 별도 파일로 정리했다(Step 10).
// encode(하드웨어에 넣을 32비트 워드)와 interpret(기대 레지스터 값)를 명령어 ADT에서
// 각각 독립적으로 유도한다. interpret은 ALU.scala/Decoder.scala/CPU.scala를 절대
// 참조하지 않고 RV32I 스펙 그대로(Scala 네이티브 Int 연산)로 계산한다 — 같은 코드
// 실수를 encode/interpret 양쪽에 동시에 저지르면 못 걸러내므로 오라클을 분리해 둔다.
object RV32IReference {
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
  // prog를 순서대로 foreach 못 돌고, 하드웨어처럼 주소로 다음 명령을 찾아야 한다).
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
}

// ChiselSim으로 (prog, golden model)을 같이 돌리는 테스트 하네스. CPUSpec과
// RegressionSpec이 공유한다. ChiselSim은 TestSuite와 섞여야 하는 self-type
// 제약이 있어서, 여기서 직접 extends 하지 않고 self-type으로만 요구한다 —
// 실제 mixing은 이 trait를 쓰는 스펙 클래스가 `with ChiselSim`을 같이 붙여서 한다.
trait RV32ITestHarness { self: ChiselSim =>
  import RV32IReference._

  // expectPass=true면 riscv-tests 식 "self-checking" 관례를 쓴 프로그램이라는 뜻 —
  // x31을 pass 플래그로 약속하고, golden model 스스로도 x31=1에 도달했는지 먼저
  // sanity-check한다(프로그램 자체의 오프셋 계산이 틀려 golden model에서마저
  // fail-loop에 갇히는 실수를 미리 잡기 위함). 최종 검증은 항상 전체 레지스터
  // 32개를 golden model과 대조하는 쪽이고, 이 어서션은 그 위에 얹는 문서화용 확인이다.
  def run(prog: Seq[Instr], cycles: Int = -1, expectPass: Boolean = false): Unit = {
    val n        = if (cycles < 0) prog.length else cycles
    val words    = prog.map(i => encode(i).U(32.W))
    val expected = interpret(prog, n)
    if (expectPass) {
      assert(expected(31) == 1, "self-checking 프로그램의 golden model이 pass 마커(x31=1)에 도달하지 못함")
    }
    simulate(new CPU(words)) { dut =>
      dut.clock.step(n)
      for (r <- 0 until 32) {
        dut.io.debugRegs(r).expect(expected(r).S(32.W).asUInt)
      }
    }
  }

  // multi-cycle CPU용. golden model은 "명령어 instrs개를 실행한 뒤"의 레지스터를, 하드웨어는
  // hwCycles 사이클 뒤의 레지스터를 본다 — 명령어당 사이클 수(CPI)가 명령어마다 달라서 둘은 다른 값이고,
  // 호출하는 쪽이 hwCycles를 직접 세어 넘긴다(instrs를 생략하면 prog.length).
  def runMultiCycle(prog: Seq[Instr], hwCycles: Int, instrs: Int = -1): Unit = {
    val n        = if (instrs < 0) prog.length else instrs
    val words    = prog.map(i => encode(i).U(32.W))
    val expected = interpret(prog, n)
    simulate(new MultiCycleCPU(words)) { dut =>
      dut.clock.step(hwCycles)
      for (r <- 0 until 32) {
        dut.io.debugRegs(r).expect(expected(r).S(32.W).asUInt)
      }
    }
  }
}
