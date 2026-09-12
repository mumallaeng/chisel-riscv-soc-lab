package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

class RegisterFileSpec extends AnyFunSpec with ChiselSim {
  describe("RegisterFile") {
    it("x0 always reads 0, even after a write") {
      simulate(new RegisterFile) { dut =>
        dut.io.rd.poke(0.U); dut.io.writeData.poke(123.U); dut.io.writeEnable.poke(true.B)
        dut.clock.step(1)
        dut.io.rs1.poke(0.U)
        dut.io.readData1.expect(0.U)
      }
    }
    it("write then read back") {
      simulate(new RegisterFile) { dut =>
        dut.io.rd.poke(5.U); dut.io.writeData.poke(42.U); dut.io.writeEnable.poke(true.B)
        dut.clock.step(1)
        dut.io.writeEnable.poke(false.B)
        dut.io.rs1.poke(5.U)
        dut.io.readData1.expect(42.U)
      }
    }
    it("two read ports work simultaneously") {
      simulate(new RegisterFile) { dut =>
        dut.io.rd.poke(1.U); dut.io.writeData.poke(10.U); dut.io.writeEnable.poke(true.B)
        dut.clock.step(1)
        dut.io.rd.poke(2.U); dut.io.writeData.poke(20.U)
        dut.clock.step(1)
        dut.io.writeEnable.poke(false.B)
        dut.io.rs1.poke(1.U); dut.io.rs2.poke(2.U)
        dut.io.readData1.expect(10.U)
        dut.io.readData2.expect(20.U)
      }
    }
    it("writeEnable = false does not write") {
      simulate(new RegisterFile) { dut =>
        dut.io.rd.poke(3.U); dut.io.writeData.poke(99.U); dut.io.writeEnable.poke(false.B)
        dut.clock.step(1)
        dut.io.rs1.poke(3.U)
        dut.io.readData1.expect(0.U)
      }
    }
  }
}
