package core

import circt.stage.ChiselStage

object Elaborate extends App {
  println(ChiselStage.emitSystemVerilog(new ALU)) // print teminal
  ChiselStage.emitSystemVerilogFile(              // save file
    new ALU,
    Array("--target-dir", "generated"),
  )
}
