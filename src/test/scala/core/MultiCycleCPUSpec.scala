package core

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import RV32IReference._

class MultiCycleCPUSpec extends AnyFunSpec with ChiselSim {
  val prog: Seq[Instr] = Seq(
    R("add", rd = 3, rs1 = 1, rs2 = 2),
    R("sub", rd = 6, rs1 = 4, rs2 = 5),
    I("addi", rd = 1, rs1 = 0, imm = 6), // inst[24:20]=6은 rs2가 아니라 immediate 비트
    I("addi", rd = 0, rs1 = 0, imm = 0), // nop — 마지막 fetch가 imem 범위 밖을 안 읽게 하는 패딩
  )
  val words   = prog.map(i => encode(i).U(32.W))
  val regInit = Map(1 -> BigInt(7), 2 -> BigInt(5), 4 -> BigInt(100), 5 -> BigInt(30), 6 -> BigInt(77))

  def ir(i: Int): UInt = encode(prog(i)).U(32.W)

  describe("MultiCycleCPU (Fetch/Decode 골격)") {
    it("리셋 직후엔 fetch 상태, PC=0, 상태 레지스터는 전부 0") {
      simulate(new MultiCycleCPU(words, regInit)) { dut =>
        dut.io.state.expect(State.fetch)
        dut.io.pc.expect(0.U)
        dut.io.ir.expect(0.U)
        dut.io.a.expect(0.U)
        dut.io.b.expect(0.U)
      }
    }
    it("fetch 한 사이클: IR에 명령어를 담고 PC를 +4 하고 decode로 넘어간다") {
      simulate(new MultiCycleCPU(words, regInit)) { dut =>
        dut.clock.step(1)
        dut.io.state.expect(State.decode)
        dut.io.ir.expect(ir(0))
        dut.io.pc.expect(4.U)
        dut.io.a.expect(0.U) // fetch는 A/B를 안 건드린다
        dut.io.b.expect(0.U)
      }
    }
    it("decode 한 사이클: rs1/rs2가 가리키는 레지스터 값을 A/B에 래치하고 IR/PC는 그대로") {
      simulate(new MultiCycleCPU(words, regInit)) { dut =>
        dut.clock.step(2)
        dut.io.state.expect(State.fetch)
        dut.io.ir.expect(ir(0))
        dut.io.pc.expect(4.U)
        dut.io.a.expect(7.U) // add x3,x1,x2 -> x1
        dut.io.b.expect(5.U) //               -> x2
      }
    }
    it("fetch-decode를 반복하며 명령어를 차례로 훑는다 (A/B는 다음 decode 전까지 이전 값 유지)") {
      simulate(new MultiCycleCPU(words, regInit)) { dut =>
        dut.clock.step(3) // 두 번째 명령어 fetch
        dut.io.state.expect(State.decode)
        dut.io.ir.expect(ir(1))
        dut.io.pc.expect(8.U)
        dut.io.a.expect(7.U) // 아직 첫 명령어의 A/B
        dut.io.b.expect(5.U)
        dut.clock.step(1) // 두 번째 명령어 decode
        dut.io.state.expect(State.fetch)
        dut.io.a.expect(100.U) // sub x6,x4,x5
        dut.io.b.expect(30.U)
      }
    }
    it("I-type의 rs2 자리는 immediate 비트인데도 decode는 무조건 읽어 B에 넣는다(garbage read)") {
      simulate(new MultiCycleCPU(words, regInit)) { dut =>
        dut.clock.step(6) // 세 번째 명령어 addi x1,x0,6 까지 decode 끝
        dut.io.ir.expect(ir(2))
        dut.io.a.expect(0.U)  // rs1 = x0
        dut.io.b.expect(77.U) // inst[24:20] = 6 -> x6의 값
      }
    }
    it("Writeback 상태가 아직 없어서 레지스터 파일은 초기값 그대로다") {
      simulate(new MultiCycleCPU(words, regInit)) { dut =>
        dut.clock.step(8) // 명령어 4개를 fetch+decode
        dut.io.pc.expect(16.U)
        for (r <- 0 until 32) {
          dut.io.debugRegs(r).expect(regInit.getOrElse(r, BigInt(0)).U(32.W))
        }
      }
    }
  }
}
