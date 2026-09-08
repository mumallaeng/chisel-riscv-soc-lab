module svsimTestbench;
  reg  [$bits(dut.a)-1:0] a = '0;
  reg  [$bits(dut.b)-1:0] b = '0;
  reg  [$bits(dut.sub)-1:0] sub = '0;
  wire [$bits(dut.out)-1:0] out;

AddSub dut (
    .a(a),
    .b(b),
    .sub(sub),
    .out(out)
);

  import "DPI-C" context function void initTestBenchScope();
  initial
    initTestBenchScope();
  // Port 0: a
  export "DPI-C" function getBitWidthImpl_a;
  function void getBitWidthImpl_a;
    output int value;
    value = $bits(dut.a);
  endfunction
  export "DPI-C" function setBitsImpl_a;
  function void setBitsImpl_a;
    input bit [$bits(dut.a)-1:0] value_a;
    a = value_a;
  endfunction
  export "DPI-C" function getBitsImpl_a;
  function void getBitsImpl_a;
    output bit [$bits(dut.a)-1:0] value_a;
    value_a = a;
  endfunction

  // Port 1: b
  export "DPI-C" function getBitWidthImpl_b;
  function void getBitWidthImpl_b;
    output int value;
    value = $bits(dut.b);
  endfunction
  export "DPI-C" function setBitsImpl_b;
  function void setBitsImpl_b;
    input bit [$bits(dut.b)-1:0] value_b;
    b = value_b;
  endfunction
  export "DPI-C" function getBitsImpl_b;
  function void getBitsImpl_b;
    output bit [$bits(dut.b)-1:0] value_b;
    value_b = b;
  endfunction

  // Port 2: sub
  export "DPI-C" function getBitWidthImpl_sub;
  function void getBitWidthImpl_sub;
    output int value;
    value = $bits(dut.sub);
  endfunction
  export "DPI-C" function setBitsImpl_sub;
  function void setBitsImpl_sub;
    input bit [$bits(dut.sub)-1:0] value_sub;
    sub = value_sub;
  endfunction
  export "DPI-C" function getBitsImpl_sub;
  function void getBitsImpl_sub;
    output bit [$bits(dut.sub)-1:0] value_sub;
    value_sub = sub;
  endfunction

  // Port 3: out
  export "DPI-C" function getBitWidthImpl_out;
  function void getBitWidthImpl_out;
    output int value;
    value = $bits(dut.out);
  endfunction
  export "DPI-C" function getBitsImpl_out;
  function void getBitsImpl_out;
    output bit [$bits(dut.out)-1:0] value_out;
    value_out = out;
  endfunction

  // Simulation
  import "DPI-C" context task simulation_body();
  enum {INIT, RUN, DONE} simulationState = INIT;
  initial
    simulationState = RUN;
  always @(simulationState) begin
    if (simulationState == RUN) begin
      simulation_body();
      simulationState = DONE;
    end
  end
  import "DPI-C" context task simulation_final();
  final
    simulation_final();
  `ifdef SVSIM_BACKEND_SUPPORTS_DELAY_IN_PUBLIC_FUNCTIONS
  export "DPI-C" task run_simulation;
  task run_simulation;
    input int timesteps;
    output int finish;
    #(timesteps*0.1);
    finish = 0;
  endtask
  `else
  import "DPI-C" function void run_simulation(input int timesteps, output int done);
  `endif

  // Tracing
  int traceSupported = 0;
  export "DPI-C" function simulation_initializeTrace;
  function void simulation_initializeTrace;
    input string traceFilePath;
    `ifdef SVSIM_ENABLE_FST_TRACING_SUPPORT
      $dumpfile({traceFilePath,".fst"});
      $dumpvars(0, dut);
      traceSupported = 1;
    `elsif SVSIM_ENABLE_VCD_TRACING_SUPPORT
      $dumpfile({traceFilePath,".vcd"});
      $dumpvars(0, dut);
      traceSupported = 1;
    `endif
    `ifdef SVSIM_ENABLE_VPD_TRACING_SUPPORT
      $vcdplusfile({traceFilePath,".vpd"});
      $dumpvars(0, dut);
      $vcdpluson(0, dut);
      traceSupported = 1;
    `endif
    `ifdef SVSIM_ENABLE_FSDB_TRACING_SUPPORT
      $fsdbDumpfile({traceFilePath,".fsdb"});
      $fsdbDumpvars(0, dut, "+all");
      traceSupported = 1;
    `endif
  endfunction
  export "DPI-C" function simulation_enableTrace;
  function void simulation_enableTrace;
    output int success;
    success = traceSupported;
    `ifdef SVSIM_ENABLE_VCD_TRACING_SUPPORT
    $dumpon;
    `elsif SVSIM_ENABLE_FST_TRACING_SUPPORT
    $dumpon;
    `elsif SVSIM_ENABLE_VPD_TRACING_SUPPORT
    $dumpon;
    `endif
    `ifdef SVSIM_ENABLE_FSDB_TRACING_SUPPORT
    $fsdbDumpon;
    `endif
  endfunction
  export "DPI-C" function simulation_disableTrace;
  function void simulation_disableTrace;
    output int success;
    success = traceSupported;
    `ifdef SVSIM_ENABLE_VCD_TRACING_SUPPORT
    $dumpoff;
    `elsif SVSIM_ENABLE_FST_TRACING_SUPPORT
    $dumpoff;
    `elsif SVSIM_ENABLE_VPD_TRACING_SUPPORT
    $dumpoff;
    `endif
    `ifdef SVSIM_ENABLE_FSDB_TRACING_SUPPORT
    $fsdbDumpoff;
    `endif
  endfunction

endmodule
