// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Model implementation (design independent parts)

#include "VsvsimTestbench__pch.h"

//============================================================
// Constructors

VsvsimTestbench::VsvsimTestbench(VerilatedContext* _vcontextp__, const char* _vcname__)
    : VerilatedModel{*_vcontextp__}
    , vlSymsp{new VsvsimTestbench__Syms(contextp(), _vcname__, this)}
    , m_evalLoop{*this, /*convergeLimit:*/ 10000}
    , rootp{&(vlSymsp->TOP)}
{
    // Register model with the context
    contextp()->addModel(this);
}

VsvsimTestbench::VsvsimTestbench(const char* _vcname__)
    : VsvsimTestbench(Verilated::threadContextp(), _vcname__)
{
}

//============================================================
// Destructor

VsvsimTestbench::~VsvsimTestbench() {
    delete vlSymsp;
}

//============================================================
// Evaluation function

#ifdef VL_DEBUG
void VsvsimTestbench___024root___eval_debug_assertions(VsvsimTestbench___024root* vlSelf);
#endif  // VL_DEBUG
VL_ATTR_COLD void VsvsimTestbench___024root___eval_static(VsvsimTestbench___024root* vlSelf);
VL_ATTR_COLD void VsvsimTestbench___024root___eval_initial(VsvsimTestbench___024root* vlSelf);
VL_ATTR_COLD bool VsvsimTestbench___024root___eval_stl(VsvsimTestbench___024root* vlSelf, CData/*0:0*/ firstIteration);
void VsvsimTestbench___024root___eval_sample(VsvsimTestbench___024root* vlSelf);
bool VsvsimTestbench___024root___eval_ico(VsvsimTestbench___024root* vlSelf, CData/*0:0*/ firstIteration);
bool VsvsimTestbench___024root___eval_act(VsvsimTestbench___024root* vlSelf);
bool VsvsimTestbench___024root___eval_inact(VsvsimTestbench___024root* vlSelf);
bool VsvsimTestbench___024root___eval_nba(VsvsimTestbench___024root* vlSelf);
bool VsvsimTestbench___024root___eval_obs(VsvsimTestbench___024root* vlSelf);
bool VsvsimTestbench___024root___eval_react(VsvsimTestbench___024root* vlSelf);
void VsvsimTestbench___024root___eval_postponed(VsvsimTestbench___024root* vlSelf);
VL_ATTR_COLD void VsvsimTestbench___024root___eval_final(VsvsimTestbench___024root* vlSelf);
VL_ATTR_COLD void VsvsimTestbench___024root___eval_dump_triggers__stl(VsvsimTestbench___024root* vlSelf);
VL_ATTR_COLD void VsvsimTestbench___024root___eval_dump_triggers__ico(VsvsimTestbench___024root* vlSelf);
VL_ATTR_COLD void VsvsimTestbench___024root___eval_dump_triggers__act(VsvsimTestbench___024root* vlSelf);
VL_ATTR_COLD void VsvsimTestbench___024root___eval_dump_triggers__nba(VsvsimTestbench___024root* vlSelf);
VL_ATTR_COLD void VsvsimTestbench___024root___eval_dump_triggers__obs(VsvsimTestbench___024root* vlSelf);
VL_ATTR_COLD void VsvsimTestbench___024root___eval_dump_triggers__react(VsvsimTestbench___024root* vlSelf);

void VsvsimTestbench::eval_step() {
    VL_DEBUG_IF(VL_DBG_MSGF("+++++TOP Evaluate VsvsimTestbench::eval_step\n"); );
    m_evalLoop.eval();
}

void VsvsimTestbench::evalBegin() {
#ifdef VL_DEBUG
    // Debug assertions
    VsvsimTestbench___024root___eval_debug_assertions(&(vlSymsp->TOP));
#endif  // VL_DEBUG
    vlSymsp->__Vm_deleter.deleteAll();
}

void VsvsimTestbench::evalEnd() {
    // Evaluate cleanup
    Verilated::endOfEval(vlSymsp->__Vm_evalMsgQp);
}

void VsvsimTestbench::evalStatic() {
    VsvsimTestbench___024root___eval_static(&(vlSymsp->TOP));
}

void VsvsimTestbench::evalInitial() {
    VsvsimTestbench___024root___eval_initial(&(vlSymsp->TOP));
}

bool VsvsimTestbench::evalStl(bool firstIteration) {
    return VsvsimTestbench___024root___eval_stl(&(vlSymsp->TOP), firstIteration);
}

void VsvsimTestbench::evalSample() {
    VsvsimTestbench___024root___eval_sample(&(vlSymsp->TOP));
}

bool VsvsimTestbench::evalIco(bool firstIteration) {
    return VsvsimTestbench___024root___eval_ico(&(vlSymsp->TOP), firstIteration);
}

bool VsvsimTestbench::evalAct() {
    return VsvsimTestbench___024root___eval_act(&(vlSymsp->TOP));
}

bool VsvsimTestbench::evalInact() {
    return VsvsimTestbench___024root___eval_inact(&(vlSymsp->TOP));
}

bool VsvsimTestbench::evalNba() {
    return VsvsimTestbench___024root___eval_nba(&(vlSymsp->TOP));
}

bool VsvsimTestbench::evalObs() {
    return VsvsimTestbench___024root___eval_obs(&(vlSymsp->TOP));
}

bool VsvsimTestbench::evalReact() {
    return VsvsimTestbench___024root___eval_react(&(vlSymsp->TOP));
}

void VsvsimTestbench::evalPostponed() {
    VsvsimTestbench___024root___eval_postponed(&(vlSymsp->TOP));
}

void VsvsimTestbench::evalFinal() {
    VsvsimTestbench___024root___eval_final(&(vlSymsp->TOP));
}

VL_ATTR_COLD void VsvsimTestbench::dumpTriggersStl() {
    VsvsimTestbench___024root___eval_dump_triggers__stl(&(vlSymsp->TOP));
}

VL_ATTR_COLD void VsvsimTestbench::dumpTriggersIco() {
    VsvsimTestbench___024root___eval_dump_triggers__ico(&(vlSymsp->TOP));
}

VL_ATTR_COLD void VsvsimTestbench::dumpTriggersAct() {
    VsvsimTestbench___024root___eval_dump_triggers__act(&(vlSymsp->TOP));
}

VL_ATTR_COLD void VsvsimTestbench::dumpTriggersNba() {
    VsvsimTestbench___024root___eval_dump_triggers__nba(&(vlSymsp->TOP));
}

VL_ATTR_COLD void VsvsimTestbench::dumpTriggersObs() {
    VsvsimTestbench___024root___eval_dump_triggers__obs(&(vlSymsp->TOP));
}

VL_ATTR_COLD void VsvsimTestbench::dumpTriggersReact() {
    VsvsimTestbench___024root___eval_dump_triggers__react(&(vlSymsp->TOP));
}

//============================================================
// Events and timing
bool VsvsimTestbench::eventsPending() { return false; }

uint64_t VsvsimTestbench::nextTimeSlot() {
    VL_FATAL_MT(__FILE__, __LINE__, "", "No delays in the design");
    return 0;
}

//============================================================
// Utilities

const char* VsvsimTestbench::name() const {
    return vlSymsp->name();
}

//============================================================
// Invoke final blocks

VL_ATTR_COLD void VsvsimTestbench::final() {
    contextp()->executingFinal(true);
    evalFinal();
    contextp()->executingFinal(false);
}

//============================================================
// Implementations of abstract methods from VerilatedModel

const char* VsvsimTestbench::hierName() const { return vlSymsp->name(); }
const char* VsvsimTestbench::modelName() const { return "VsvsimTestbench"; }
unsigned VsvsimTestbench::threads() const { return 1; }
void VsvsimTestbench::prepareClone() const { contextp()->prepareClone(); }
void VsvsimTestbench::atClone() const {
    contextp()->threadPoolpOnClone();
}
