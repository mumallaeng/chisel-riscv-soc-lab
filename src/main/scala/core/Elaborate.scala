package core

import circt.stage.ChiselStage

object Elaborate extends App {
  println(ChiselStage.emitSystemVerilog(new Adder)) // print teminal
  ChiselStage.emitSystemVerilogFile( // save file
    new Adder,
    Array("--target-dir", "generated"),
  )
}
