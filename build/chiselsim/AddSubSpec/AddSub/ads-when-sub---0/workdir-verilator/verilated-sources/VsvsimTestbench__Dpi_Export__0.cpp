// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Implementation of DPI export functions.

#include "VsvsimTestbench.h"
#include "VsvsimTestbench__Syms.h"
#include "verilated_dpi.h"


void VsvsimTestbench::getBitWidthImpl_a(int* value) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::getBitWidthImpl_a\n"); );
    // Locals
    IData/*31:0*/ value__Vcvt;
    value__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("getBitWidthImpl_a");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_getBitWidthImpl_a_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_getBitWidthImpl_a_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value__Vcvt);
    *value = value__Vcvt;
}

void VsvsimTestbench::setBitsImpl_a(const svBitVecVal* value_a) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::setBitsImpl_a\n"); );
    // Locals
    IData/*31:0*/ value_a__Vcvt;
    value_a__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("setBitsImpl_a");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_setBitsImpl_a_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_setBitsImpl_a_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    VL_SET_I_SVBV(32, value_a__Vcvt, value_a + 0);
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value_a__Vcvt);
}

void VsvsimTestbench::getBitsImpl_a(svBitVecVal* value_a) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::getBitsImpl_a\n"); );
    // Locals
    IData/*31:0*/ value_a__Vcvt;
    value_a__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("getBitsImpl_a");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_getBitsImpl_a_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_getBitsImpl_a_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value_a__Vcvt);
    VL_SET_SVBV_I(32, value_a, value_a__Vcvt);
}

void VsvsimTestbench::getBitWidthImpl_b(int* value) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::getBitWidthImpl_b\n"); );
    // Locals
    IData/*31:0*/ value__Vcvt;
    value__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("getBitWidthImpl_b");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_getBitWidthImpl_b_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_getBitWidthImpl_b_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value__Vcvt);
    *value = value__Vcvt;
}

void VsvsimTestbench::setBitsImpl_b(const svBitVecVal* value_b) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::setBitsImpl_b\n"); );
    // Locals
    IData/*31:0*/ value_b__Vcvt;
    value_b__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("setBitsImpl_b");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_setBitsImpl_b_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_setBitsImpl_b_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    VL_SET_I_SVBV(32, value_b__Vcvt, value_b + 0);
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value_b__Vcvt);
}

void VsvsimTestbench::getBitsImpl_b(svBitVecVal* value_b) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::getBitsImpl_b\n"); );
    // Locals
    IData/*31:0*/ value_b__Vcvt;
    value_b__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("getBitsImpl_b");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_getBitsImpl_b_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_getBitsImpl_b_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value_b__Vcvt);
    VL_SET_SVBV_I(32, value_b, value_b__Vcvt);
}

void VsvsimTestbench::getBitWidthImpl_sub(int* value) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::getBitWidthImpl_sub\n"); );
    // Locals
    IData/*31:0*/ value__Vcvt;
    value__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("getBitWidthImpl_sub");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_getBitWidthImpl_sub_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_getBitWidthImpl_sub_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value__Vcvt);
    *value = value__Vcvt;
}

void VsvsimTestbench::setBitsImpl_sub(const svBitVecVal* value_sub) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::setBitsImpl_sub\n"); );
    // Locals
    CData/*0:0*/ value_sub__Vcvt;
    value_sub__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("setBitsImpl_sub");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_setBitsImpl_sub_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_setBitsImpl_sub_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    VL_SET_C_SVBV(1, value_sub__Vcvt, value_sub + 0);
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value_sub__Vcvt);
}

void VsvsimTestbench::getBitsImpl_sub(svBitVecVal* value_sub) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::getBitsImpl_sub\n"); );
    // Locals
    CData/*0:0*/ value_sub__Vcvt;
    value_sub__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("getBitsImpl_sub");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_getBitsImpl_sub_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_getBitsImpl_sub_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value_sub__Vcvt);
    VL_SET_SVBV_I(1, value_sub, value_sub__Vcvt);
}

void VsvsimTestbench::getBitWidthImpl_out(int* value) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::getBitWidthImpl_out\n"); );
    // Locals
    IData/*31:0*/ value__Vcvt;
    value__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("getBitWidthImpl_out");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_getBitWidthImpl_out_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_getBitWidthImpl_out_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value__Vcvt);
    *value = value__Vcvt;
}

void VsvsimTestbench::getBitsImpl_out(svBitVecVal* value_out) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::getBitsImpl_out\n"); );
    // Locals
    IData/*31:0*/ value_out__Vcvt;
    value_out__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("getBitsImpl_out");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_getBitsImpl_out_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_getBitsImpl_out_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), value_out__Vcvt);
    VL_SET_SVBV_I(32, value_out, value_out__Vcvt);
}

void VsvsimTestbench::simulation_initializeTrace(const char* traceFilePath) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::simulation_initializeTrace\n"); );
    // Locals
    static thread_local std::string traceFilePath__Vcvt;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("simulation_initializeTrace");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_simulation_initializeTrace_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_simulation_initializeTrace_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    traceFilePath__Vcvt = VL_CVT_N_CSTR(traceFilePath);
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), traceFilePath__Vcvt);
}

void VsvsimTestbench::simulation_enableTrace(int* success) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::simulation_enableTrace\n"); );
    // Locals
    IData/*31:0*/ success__Vcvt;
    success__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("simulation_enableTrace");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_simulation_enableTrace_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_simulation_enableTrace_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), success__Vcvt);
    *success = success__Vcvt;
}

void VsvsimTestbench::simulation_disableTrace(int* success) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VsvsimTestbench___024root::simulation_disableTrace\n"); );
    // Locals
    IData/*31:0*/ success__Vcvt;
    success__Vcvt = 0;
    // Body
    static int __Vfuncnum = -1;
    if (VL_UNLIKELY(__Vfuncnum == -1)) {
        __Vfuncnum = Verilated::exportFuncNum("simulation_disableTrace");
    }
    const VerilatedScope* const __Vscopep = Verilated::dpiScope();
    VsvsimTestbench__Vcb_simulation_disableTrace_t __Vcb = reinterpret_cast<VsvsimTestbench__Vcb_simulation_disableTrace_t>(VerilatedScope::exportFind(__Vscopep, __Vfuncnum));
    (*__Vcb)((VsvsimTestbench__Syms*)(__Vscopep->symsp()), success__Vcvt);
    *success = success__Vcvt;
}
