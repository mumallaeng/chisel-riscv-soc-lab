// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Prototypes for DPI import and export functions.
//
// Verilator includes this file in all generated .cpp files that use DPI functions.
// Manually include this file where DPI .c import functions are declared to ensure
// the C functions match the expectations of the DPI imports.

#ifndef VERILATED_VSVSIMTESTBENCH__DPI_H_
#define VERILATED_VSVSIMTESTBENCH__DPI_H_  // guard

#include "svdpi.h"

#ifdef __cplusplus
extern "C" {
#endif


    // DPI EXPORTS
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:19:17
    extern void getBitWidthImpl_a(int* value);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:36:17
    extern void getBitWidthImpl_b(int* value);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:70:17
    extern void getBitWidthImpl_out(int* value);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:53:17
    extern void getBitWidthImpl_sub(int* value);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:29:17
    extern void getBitsImpl_a(svBitVecVal* value_a);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:46:17
    extern void getBitsImpl_b(svBitVecVal* value_b);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:75:17
    extern void getBitsImpl_out(svBitVecVal* value_out);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:63:17
    extern void getBitsImpl_sub(svBitVecVal* value_sub);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:24:17
    extern void setBitsImpl_a(const svBitVecVal* value_a);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:41:17
    extern void setBitsImpl_b(const svBitVecVal* value_b);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:58:17
    extern void setBitsImpl_sub(const svBitVecVal* value_sub);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:148:17
    extern void simulation_disableTrace(int* success);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:133:17
    extern void simulation_enableTrace(int* success);
    // DPI export at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:109:17
    extern void simulation_initializeTrace(const char* traceFilePath);

    // DPI IMPORTS
    // DPI import at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:14:40
    extern void initTestBenchScope();
    // DPI import at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:103:32
    extern void run_simulation(int timesteps, int* done);
    // DPI import at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:81:31
    extern int simulation_body();
    // DPI import at /Users/mumallaeng/repo/mini_project_repo/chisel-riscv-soc-lab/build/chiselsim/AddSubSpec/AddSub/adds-with-mod-2-32-wrap/workdir-verilator/../generated-sources/testbench.sv:91:31
    extern int simulation_final();

#ifdef __cplusplus
}
#endif

#endif  // guard
