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
  }

  def interpret(prog: Seq[Instr]): Array[Int] = {
    val regs = Array.fill(32)(0)
    def w(rd: Int, v: Int): Unit = if (rd != 0) regs(rd) = v
    prog.foreach {
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
      case other                    => sys.error(s"golden model: unsupported instr $other")
    }
    regs
  }

  def run(prog: Seq[Instr]): Unit = {
    val words = prog.map(i => encode(i).U(32.W))
    val expected = interpret(prog)
    simulate(new CPU(words)) { dut =>
      dut.clock.step(prog.length)
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
}
