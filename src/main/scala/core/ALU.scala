package core

import chisel3._

class Adder extends RawModule {
  val a   = IO(Input(UInt(32.W)))
  val b   = IO(Input(UInt(32.W)))
  val sum = IO(Output(UInt(32.W)))

  sum := a + b
}
