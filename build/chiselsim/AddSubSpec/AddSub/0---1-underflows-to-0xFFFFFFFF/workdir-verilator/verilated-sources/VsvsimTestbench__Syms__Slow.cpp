// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Symbol table implementation internals

#include "VsvsimTestbench__pch.h"

void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_a_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ &value);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_b_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ &value);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_out_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ &value);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_sub_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ &value);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_a_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ &value_a);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_b_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ &value_b);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_out_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ &value_out);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_sub_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, CData/*0:0*/ &value_sub);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__setBitsImpl_a_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ value_a);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__setBitsImpl_b_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ value_b);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__setBitsImpl_sub_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, CData/*0:0*/ value_sub);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__simulation_disableTrace_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ &success);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__simulation_enableTrace_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, IData/*31:0*/ &success);
void VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__simulation_initializeTrace_TOP(VsvsimTestbench__Syms* __restrict vlSymsp, std::string traceFilePath);

extern const VlScopeTableEntry VsvsimTestbench__Syms__VpiScopeTable[];


// VPI VARIABLE/SCOPE TABLES
#if defined(__GNUC__)
# pragma GCC diagnostic push
# pragma GCC diagnostic ignored "-Winvalid-offsetof"
#endif
extern const VlScopeTableEntry VsvsimTestbench__Syms__VpiScopeTable[] = {
    {offsetof(VsvsimTestbench__Syms, __Vscopep_svsimTestbench), "svsimTestbench", "svsimTestbench", "<null>", -9, VerilatedScope::SCOPE_OTHER},
};
#if defined(__GNUC__)
# pragma GCC diagnostic pop
#endif
VsvsimTestbench__Syms::VsvsimTestbench__Syms(VerilatedContext* contextp, const char* namep, VsvsimTestbench* modelp)
    : VerilatedSyms{contextp}
    // Setup internal state of the Syms class
    , __Vm_modelp{modelp}
    , __Vm_didInit{modelp->m_didInit}
    // Setup top module instance
    , TOP{this, namep}
{
    // Check resources
    Verilated::stackCheck(416);
    // Setup sub module instances
    // Configure time unit / time precision
    _vm_contextp__->timeunit(-9);
    _vm_contextp__->timeprecision(-10);
    // Setup each module's pointers to their submodules
    // Setup each module's pointer back to symbol table (for public functions)
    TOP.__Vconfigure(true);
    // Setup scopes
    VerilatedScope::scopesConstructFromTable(VsvsimTestbench__Syms__VpiScopeTable, 1, this);
    // Setup export functions - final: 0
    __Vscopep_svsimTestbench->exportInsert(0, "getBitWidthImpl_a", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_a_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "getBitWidthImpl_b", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_b_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "getBitWidthImpl_out", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_out_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "getBitWidthImpl_sub", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_sub_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "getBitsImpl_a", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_a_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "getBitsImpl_b", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_b_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "getBitsImpl_out", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_out_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "getBitsImpl_sub", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_sub_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "setBitsImpl_a", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__setBitsImpl_a_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "setBitsImpl_b", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__setBitsImpl_b_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "setBitsImpl_sub", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__setBitsImpl_sub_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "simulation_disableTrace", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__simulation_disableTrace_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "simulation_enableTrace", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__simulation_enableTrace_TOP));
    __Vscopep_svsimTestbench->exportInsert(0, "simulation_initializeTrace", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__simulation_initializeTrace_TOP));
    // Setup export functions - final: 1
    __Vscopep_svsimTestbench->exportInsert(1, "getBitWidthImpl_a", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_a_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "getBitWidthImpl_b", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_b_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "getBitWidthImpl_out", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_out_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "getBitWidthImpl_sub", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitWidthImpl_sub_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "getBitsImpl_a", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_a_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "getBitsImpl_b", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_b_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "getBitsImpl_out", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_out_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "getBitsImpl_sub", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__getBitsImpl_sub_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "setBitsImpl_a", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__setBitsImpl_a_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "setBitsImpl_b", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__setBitsImpl_b_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "setBitsImpl_sub", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__setBitsImpl_sub_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "simulation_disableTrace", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__simulation_disableTrace_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "simulation_enableTrace", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__simulation_enableTrace_TOP));
    __Vscopep_svsimTestbench->exportInsert(1, "simulation_initializeTrace", (void*)(&VsvsimTestbench___024root____Vdpiexp_svsimTestbench__DOT__simulation_initializeTrace_TOP));
}

VsvsimTestbench__Syms::~VsvsimTestbench__Syms() {
    // Tear down scopes
    VL_DO_CLEAR(delete __Vscopep_svsimTestbench, __Vscopep_svsimTestbench = nullptr);
    // Tear down sub module instances
}
