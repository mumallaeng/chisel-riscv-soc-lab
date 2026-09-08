// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See VsvsimTestbench.h for the primary calling header

#ifndef VERILATED_VSVSIMTESTBENCH___024ROOT_H_
#define VERILATED_VSVSIMTESTBENCH___024ROOT_H_  // guard

#include "verilated.h"


class VsvsimTestbench__Syms;

class alignas(VL_CACHE_LINE_BYTES) VsvsimTestbench___024root final {
  public:

    // DESIGN SPECIFIC STATE
    CData/*0:0*/ svsimTestbench__DOT__sub;
    CData/*0:0*/ __Vdpi_export_trigger;
    CData/*0:0*/ __VactDidInit;
    IData/*31:0*/ svsimTestbench__DOT__a;
    IData/*31:0*/ svsimTestbench__DOT__b;
    IData/*31:0*/ svsimTestbench__DOT__simulationState;
    IData/*31:0*/ __Vtrigprevexpr___TOP__svsimTestbench__DOT__simulationState__0;
    VlUnpacked<QData/*63:0*/, 1> __VstlTriggered;
    VlUnpacked<QData/*63:0*/, 1> __VicoTriggered;
    VlUnpacked<QData/*63:0*/, 2> __VactTriggered;
    VlUnpacked<QData/*63:0*/, 2> __VnbaTriggered;

    // INTERNAL VARIABLES
    VsvsimTestbench__Syms* vlSymsp;
    const char* vlNamep;

    // CONSTRUCTORS
    VsvsimTestbench___024root(VsvsimTestbench__Syms* symsp, const char* namep);
    ~VsvsimTestbench___024root();
    VL_UNCOPYABLE(VsvsimTestbench___024root);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
};


#endif  // guard
