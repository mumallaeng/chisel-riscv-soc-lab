package core

import chisel3._

class Adder extends RawModule {
  val a   = IO(Input(UInt(32.W)))
  val b   = IO(Input(UInt(32.W)))
  val sum = IO(Output(UInt(32.W)))

  sum := a + b
}

class AddSub extends RawModule {
  val a   = IO(Input(UInt(32.W)))
  val b   = IO(Input(UInt(32.W)))
  val sub = IO(Input(Bool())) // 0 = add, 1 = subtract
  val out = IO(Output(UInt(32.W)))

  out := Mux(sub, a - b, a + b)
}
