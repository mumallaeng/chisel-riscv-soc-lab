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
  }
}
