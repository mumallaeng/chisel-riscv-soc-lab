package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

class DataMemorySpec extends AnyFunSpec with ChiselSim {
  val LB = 0.U(3.W); val LH = 1.U(3.W); val LW = 2.U(3.W); val LBU = 4.U(3.W); val LHU = 5.U(3.W)
  val SB = 0.U(3.W); val SH = 1.U(3.W); val SW = 2.U(3.W)

  def store(dut: DataMemory, addr: Int, data: BigInt, funct3: UInt): Unit = {
    dut.io.addr.poke(addr.U); dut.io.writeData.poke(data.U(32.W))
    dut.io.funct3.poke(funct3); dut.io.memWrite.poke(true.B)
    dut.clock.step(1)
    dut.io.memWrite.poke(false.B)
  }

  // 읽기 검증은 memRead를 켜 둔 채로 한다(꺼져 있으면 readData는 0 — 아래 마지막 테스트)
  def sim(body: DataMemory => Unit): Unit =
    simulate(new DataMemory) { dut => dut.io.memRead.poke(true.B); body(dut) }

  describe("DataMemory") {
    it("SW 저장 후 LW로 그대로 읽힌다") {
      sim { dut =>
        store(dut, 0, BigInt("DEADBEEF", 16), SW)
        dut.io.addr.poke(0.U); dut.io.funct3.poke(LW)
        dut.io.readData.expect("hDEADBEEF".U)
      }
    }
    it("SB는 addr 하위 2비트로 고른 lane 하나만 바꾸고 나머지는 그대로 둔다") {
      sim { dut =>
        store(dut, 0, BigInt("FFFFFFFF", 16), SW) // 워드 전체를 0xFF로 채움
        store(dut, 1, 0x00, SB)                   // byte lane 1만 0x00으로 덮어씀
        dut.io.addr.poke(0.U); dut.io.funct3.poke(LW)
        dut.io.readData.expect("hFFFF00FF".U)     // lane1만 00, 나머지 FF 유지
      }
    }
    it("LB/LBU: 같은 바이트를 부호/무부호로 다르게 확장한다") {
      sim { dut =>
        store(dut, 4, 0xff, SB) // 바이트 0xFF = signed -1
        dut.io.addr.poke(4.U); dut.io.funct3.poke(LB)
        dut.io.readData.expect("hFFFFFFFF".U) // sign-extend
        dut.io.funct3.poke(LBU)
        dut.io.readData.expect(0xff.U) // zero-extend
      }
    }
    it("LH/LHU: word 안 상위/하위 하프워드 둘 다, 부호 확장 여부로 갈린다") {
      sim { dut =>
        store(dut, 8, BigInt("8001", 16), SH)     // addr 8 (하위 하프): 0x8001 = signed -32767
        store(dut, 10, BigInt("1234", 16), SH)    // addr 10 (상위 하프, 같은 word)
        dut.io.addr.poke(8.U); dut.io.funct3.poke(LH)
        dut.io.readData.expect("hFFFF8001".U)
        dut.io.addr.poke(8.U); dut.io.funct3.poke(LHU)
        dut.io.readData.expect("h00008001".U)
        dut.io.addr.poke(10.U); dut.io.funct3.poke(LH)
        dut.io.readData.expect("h00001234".U)
        // 두 하프가 같은 word에 잘 나눠 들어갔는지 LW로도 확인
        dut.io.addr.poke(8.U); dut.io.funct3.poke(LW)
        dut.io.readData.expect("h12348001".U)
      }
    }
    it("memWrite=false면 아무 것도 안 바뀐다") {
      sim { dut =>
        store(dut, 12, BigInt("11111111", 16), SW)
        dut.io.addr.poke(12.U); dut.io.writeData.poke(BigInt("22222222", 16).U(32.W))
        dut.io.funct3.poke(SW); dut.io.memWrite.poke(false.B)
        dut.clock.step(1)
        dut.io.funct3.poke(LW)
        dut.io.readData.expect("h11111111".U)
      }
    }
    it("memRead=false면 readData는 0 — 메모리는 읽기 사이클 밖에서 출력을 붙잡지 않는다") {
      sim { dut =>
        store(dut, 16, BigInt("CAFEF00D", 16), SW)
        dut.io.addr.poke(16.U); dut.io.funct3.poke(LW)
        dut.io.readData.expect("hCAFEF00D".U) // memRead=1: 유효
        dut.io.memRead.poke(false.B)
        dut.io.readData.expect(0.U)           // memRead=0: 같은 주소인데도 0
        dut.io.memRead.poke(true.B)
        dut.io.readData.expect("hCAFEF00D".U) // 데이터 자체는 그대로 저장돼 있음
      }
    }
  }
}
