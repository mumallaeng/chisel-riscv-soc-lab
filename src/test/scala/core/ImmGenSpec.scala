package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

class ImmGenSpec extends AnyFunSpec with ChiselSim {
  def iType(imm12: Int): BigInt = (BigInt(imm12 & 0xfff) << 20) | BigInt(0x13) // opcode만, rs1/rd=0

  def sType(imm12: Int): BigInt = {
    val v = imm12 & 0xfff
    (BigInt(v >> 5) << 25) | (BigInt(v & 0x1f) << 7) | BigInt(0x23)
  }

  def bType(immVal: Int): BigInt = {
    val v = immVal & 0x1ffe // bit0 항상 0
    (BigInt((v >> 12) & 0x1) << 31) | (BigInt((v >> 5) & 0x3f) << 25) |
      (BigInt((v >> 1) & 0xf) << 8) | (BigInt((v >> 11) & 0x1) << 7) | BigInt(0x63)
  }

  def uType(imm20: Int): BigInt = (BigInt(imm20 & 0xfffff) << 12) | BigInt(0x37)

  def jType(immVal: Int): BigInt = {
    val v = immVal & 0x1ffffe
    (BigInt((v >> 20) & 0x1) << 31) | (BigInt((v >> 12) & 0xff) << 12) |
      (BigInt((v >> 11) & 0x1) << 20) | (BigInt((v >> 1) & 0x3ff) << 21) | BigInt(0x6f)
  }

  describe("ImmGen") {
    it("I-type sign-extends a negative immediate") {
      simulateRaw(new ImmGen) { dut =>
        dut.inst.poke(iType(-5).U(32.W))
        dut.immSel.poke(ImmType.itype)
        dut.imm.expect("hFFFFFFFB".U)
      }
    }
    it("I-type keeps a positive immediate") {
      simulateRaw(new ImmGen) { dut =>
        dut.inst.poke(iType(100).U(32.W))
        dut.immSel.poke(ImmType.itype)
        dut.imm.expect(100.U)
      }
    }
    it("S-type sign-extends") {
      simulateRaw(new ImmGen) { dut =>
        dut.inst.poke(sType(-8).U(32.W))
        dut.immSel.poke(ImmType.stype)
        dut.imm.expect("hFFFFFFF8".U)
      }
    }
    it("B-type: offset LSB is always 0") {
      simulateRaw(new ImmGen) { dut =>
        dut.inst.poke(bType(16).U(32.W))
        dut.immSel.poke(ImmType.btype)
        dut.imm.expect(16.U)
      }
    }
    it("U-type: upper 20 bits, lower 12 zero") {
      simulateRaw(new ImmGen) { dut =>
        dut.inst.poke(uType(0x12345).U(32.W))
        dut.immSel.poke(ImmType.utype)
        dut.imm.expect("h12345000".U)
      }
    }
    it("J-type: offset LSB is always 0") {
      simulateRaw(new ImmGen) { dut =>
        dut.inst.poke(jType(2048).U(32.W))
        dut.immSel.poke(ImmType.jtype)
        dut.imm.expect(2048.U)
      }
    }
  }
}
