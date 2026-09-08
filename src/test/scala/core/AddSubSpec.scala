package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

class AddSubSpec extends AnyFunSpec with ChiselSim {
  describe("AddSub") {
    it("ads when sub = 0") {
      simulateRaw(new AddSub) { dut =>
        dut.a.poke(10.U)
        dut.b.poke(3.U)
        dut.sub.poke(false.B)
        dut.out.expect(13.U)
      }
    }
    it("subtracts when sub = 1") {
      simulateRaw(new AddSub) { dut =>
        dut.a.poke(10.U)
        dut.b.poke(3.U)
        dut.sub.poke(true.B)
        dut.out.expect(7.U)
      }
    }
    it("adds with mod-2^32 wrap") {
      simulateRaw(new AddSub) { dut =>
        dut.a.poke("hFFFFFFFF".U)
        dut.b.poke(1.U)
        dut.sub.poke(false.B)
        dut.out.expect(0.U)
      }
    }
    it("0 - 1 underflows to 0xFFFFFFFF") {
      simulateRaw(new AddSub) { dut =>
        dut.a.poke(0.U)
        dut.b.poke(1.U)
        dut.sub.poke(true.B)
        dut.out.expect("hFFFFFFFF".U)
      }
    }
  }
}
