package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

class DecoderSpec extends AnyFunSpec with ChiselSim {
  // rs1/rs2/rd는 control 신호 디코드에 안 쓰이므로 0으로 고정
  def encode(opcode: Int, funct3: Int = 0, funct7: Int = 0): UInt =
    ((BigInt(funct7) << 25) | (BigInt(funct3) << 12) | BigInt(opcode)).U(32.W)

  describe("Decoder") {
    it("add (R-type): regWrite, aluSrc=0, aluOp=add") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x33, funct3 = 0, funct7 = 0x00))
        dut.ctrl.aluOp.expect(ALUOp.add)
        dut.ctrl.aluSrc.expect(false.B)
        dut.ctrl.regWrite.expect(true.B)
        dut.ctrl.memRead.expect(false.B)
        dut.ctrl.memWrite.expect(false.B)
        dut.ctrl.branch.expect(false.B)
        dut.ctrl.jump.expect(false.B)
      }
    }
    it("sub (R-type): funct7 bit30이 add와 구분") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x33, funct3 = 0, funct7 = 0x20))
        dut.ctrl.aluOp.expect(ALUOp.sub)
      }
    }
    it("srai vs srli (I-type shift): funct7으로 산술/논리 구분") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x13, funct3 = 5, funct7 = 0x00))
        dut.ctrl.aluOp.expect(ALUOp.srl)
        dut.ctrl.aluSrc.expect(true.B)
        dut.inst.poke(encode(0x13, funct3 = 5, funct7 = 0x20))
        dut.ctrl.aluOp.expect(ALUOp.sra)
      }
    }
    it("addi (I-type ALU): aluSrc=1, immSel=itype, regWrite=1") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x13, funct3 = 0))
        dut.ctrl.aluOp.expect(ALUOp.add)
        dut.ctrl.aluSrc.expect(true.B)
        dut.ctrl.immSel.expect(ImmType.itype)
        dut.ctrl.regWrite.expect(true.B)
      }
    }
    it("lw: memRead=1, memToReg=1, regWrite=1, immSel=itype") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x03, funct3 = 2))
        dut.ctrl.memRead.expect(true.B)
        dut.ctrl.memToReg.expect(true.B)
        dut.ctrl.regWrite.expect(true.B)
        dut.ctrl.immSel.expect(ImmType.itype)
        dut.ctrl.aluOp.expect(ALUOp.add)
      }
    }
    it("sw: memWrite=1, regWrite=0, immSel=stype") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x23, funct3 = 2))
        dut.ctrl.memWrite.expect(true.B)
        dut.ctrl.regWrite.expect(false.B)
        dut.ctrl.immSel.expect(ImmType.stype)
      }
    }
    it("beq/bne -> aluOp=sub, blt/bge -> slt, bltu/bgeu -> sltu, 모두 branch=1") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x63, funct3 = 0)) // beq
        dut.ctrl.aluOp.expect(ALUOp.sub); dut.ctrl.branch.expect(true.B)
        dut.inst.poke(encode(0x63, funct3 = 1)) // bne
        dut.ctrl.aluOp.expect(ALUOp.sub); dut.ctrl.branch.expect(true.B)
        dut.inst.poke(encode(0x63, funct3 = 4)) // blt
        dut.ctrl.aluOp.expect(ALUOp.slt)
        dut.inst.poke(encode(0x63, funct3 = 6)) // bltu
        dut.ctrl.aluOp.expect(ALUOp.sltu)
        dut.ctrl.regWrite.expect(false.B)
        dut.ctrl.immSel.expect(ImmType.btype)
      }
    }
    it("lui/auipc: regWrite=1, aluSrc=1, immSel=utype, funct3 무관 — aluSrcA만 다르다(0 vs pc)") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x37, funct3 = 5)) // lui, funct3 아무 값
        dut.ctrl.regWrite.expect(true.B)
        dut.ctrl.aluSrc.expect(true.B)
        dut.ctrl.immSel.expect(ImmType.utype)
        dut.ctrl.aluSrcA.expect(AluASrc.zero)
        dut.inst.poke(encode(0x17, funct3 = 2)) // auipc
        dut.ctrl.regWrite.expect(true.B)
        dut.ctrl.immSel.expect(ImmType.utype)
        dut.ctrl.aluSrcA.expect(AluASrc.pc)
      }
    }
    it("jal: jump=1, regWrite=1, immSel=jtype, aluSrc=0(jalr과 구분용)") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x6f))
        dut.ctrl.jump.expect(true.B)
        dut.ctrl.regWrite.expect(true.B)
        dut.ctrl.immSel.expect(ImmType.jtype)
        dut.ctrl.aluSrc.expect(false.B)
      }
    }
    it("jalr: jump=1, aluSrc=1, immSel=itype") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x67, funct3 = 0))
        dut.ctrl.jump.expect(true.B)
        dut.ctrl.aluSrc.expect(true.B)
        dut.ctrl.immSel.expect(ImmType.itype)
      }
    }
    it("정의되지 않은 opcode는 전부 0(NOP)으로 떨어진다") {
      simulateRaw(new Decoder) { dut =>
        dut.inst.poke(encode(0x00, funct3 = 0, funct7 = 0))
        dut.ctrl.regWrite.expect(false.B)
        dut.ctrl.memRead.expect(false.B)
        dut.ctrl.memWrite.expect(false.B)
        dut.ctrl.branch.expect(false.B)
        dut.ctrl.jump.expect(false.B)
      }
    }
  }
}
