package core

import chisel3._
import chisel3.util._

object ImmType extends ChiselEnum {
  val itype, stype, btype, utype, jtype = Value
}

class ImmGen extends RawModule {
  val inst   = IO(Input(UInt(32.W)))
  val immSel = IO(Input(ImmType()))
  val imm    = IO(Output(UInt(32.W)))

  val iImm = Cat(Fill(20, inst(31)), inst(31, 20))
  val sImm = Cat(Fill(20, inst(31)), inst(31, 25), inst(11, 7))
  val bImm = Cat(Fill(19, inst(31)), inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
  val uImm = Cat(inst(31, 12), 0.U(12.W))
  val jImm = Cat(Fill(11, inst(31)), inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))

  imm := MuxLookup(immSel, 0.U)(Seq(
    ImmType.itype -> iImm,
    ImmType.stype -> sImm,
    ImmType.btype -> bImm,
    ImmType.utype -> uImm,
    ImmType.jtype -> jImm,
  ))
}
