package core

import chisel3._
import chisel3.util._

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

object ALUOp extends ChiselEnum {
  val add = Value(0.U)
  val sub = Value(1.U)
  val and = Value(2.U)
  val or  = Value(3.U)
  val xor = Value(4.U)
}

class ALU extends RawModule {
  val a   = IO(Input(UInt(32.W)))
  val b   = IO(Input(UInt(32.W)))
  val op  = IO(Input(ALUOp()))
  val out = IO(Output(UInt(32.W)))

  // out := 0.U // default
  // switch(op) {
  //   is(ALUOp.add)(out := a + b)
  //   is(ALUOp.sub)(out := a - b)
  //   is(ALUOp.and)(out := a & b)
  //   is(ALUOp.or)(out := a | b)
  //   is(ALUOp.xor)(out := a ^ b)
  // }

  out := MuxLookup(op, 0.U)(Seq(
    ALUOp.add -> (a + b),
    ALUOp.sub -> (a - b),
    ALUOp.and -> (a & b),
    ALUOp.or  -> (a | b),
    ALUOp.xor -> (a ^ b),
  ))
}
