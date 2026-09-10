package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

class ALUSpec extends AnyFunSpec with ChiselSim {
  describe("ALU") {
    it("add / sub") {
      simulateRaw(new ALU) { dut =>
        dut.a.poke(10.U); dut.b.poke(3.U)
        dut.op.poke(ALUOp.add); dut.out.expect(13.U)
        dut.op.poke(ALUOp.sub); dut.out.expect(7.U)
      }
    }
    it("and / or / xor") {
      simulateRaw(new ALU) { dut =>
        dut.a.poke("hF0".U); dut.b.poke("h0F".U)
        dut.op.poke(ALUOp.and); dut.out.expect(0.U)
        dut.op.poke(ALUOp.or); dut.out.expect("hFF".U)
        dut.op.poke(ALUOp.xor); dut.out.expect("hFF".U)
      }
    }
    it("sll / srl / sra") {
      simulateRaw(new ALU) { dut =>
        dut.a.poke(1.U); dut.b.poke(4.U)
        dut.op.poke(ALUOp.sll); dut.out.expect(16.U)
        dut.b.poke(36.U)
        dut.op.poke(ALUOp.sll); dut.out.expect(16.U) // 36 & 0x1F == 4
        dut.a.poke("h80000000".U); dut.b.poke(4.U)
        dut.op.poke(ALUOp.srl); dut.out.expect("h08000000".U) // 왼쪽 0으로 채움
        dut.op.poke(ALUOp.sra); dut.out.expect("hF8000000".U) // 왼쪽 부호 비트 채움
      }
    }
  }
}
