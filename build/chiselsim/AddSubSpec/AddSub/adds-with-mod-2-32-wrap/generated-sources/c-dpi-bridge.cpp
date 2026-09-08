#include <stdint.h>

#ifdef SVSIM_ENABLE_VERILATOR_SUPPORT
#include "verilated-sources/VsvsimTestbench__Dpi.h"
#endif
#ifdef SVSIM_ENABLE_VCS_SUPPORT
#include "vc_hdrs.h"
#endif

extern "C" {
 svScope setScopeToTestBench();
void getBitWidth_a(int* result) {
           svScope prev = setScopeToTestBench();
           getBitWidthImpl_a(result);
           svSetScope(prev);
        }
void getBits_a(svBitVecVal* result) {
           svScope prev = setScopeToTestBench();
           getBitsImpl_a(result);
           svSetScope(prev);
        }
void setBits_a(const svBitVecVal* data) {
           svScope prev = setScopeToTestBench();
           setBitsImpl_a(data);
           svSetScope(prev);
        }
void getBitWidth_b(int* result) {
           svScope prev = setScopeToTestBench();
           getBitWidthImpl_b(result);
           svSetScope(prev);
        }
void getBits_b(svBitVecVal* result) {
           svScope prev = setScopeToTestBench();
           getBitsImpl_b(result);
           svSetScope(prev);
        }
void setBits_b(const svBitVecVal* data) {
           svScope prev = setScopeToTestBench();
           setBitsImpl_b(data);
           svSetScope(prev);
        }
void getBitWidth_sub(int* result) {
           svScope prev = setScopeToTestBench();
           getBitWidthImpl_sub(result);
           svSetScope(prev);
        }
void getBits_sub(svBitVecVal* result) {
           svScope prev = setScopeToTestBench();
           getBitsImpl_sub(result);
           svSetScope(prev);
        }
void setBits_sub(const svBitVecVal* data) {
           svScope prev = setScopeToTestBench();
           setBitsImpl_sub(data);
           svSetScope(prev);
        }
void getBitWidth_out(int* result) {
           svScope prev = setScopeToTestBench();
           getBitWidthImpl_out(result);
           svSetScope(prev);
        }
void getBits_out(svBitVecVal* result) {
           svScope prev = setScopeToTestBench();
           getBitsImpl_out(result);
           svSetScope(prev);
        }

int port_getter(int id, int *bitWidth, void (**getter)(uint8_t*)) {
  switch (id) {
    case 0: // a
      getBitWidth_a(bitWidth);
      *getter = (void(*)(uint8_t*))getBits_a;
      return 0;
    case 1: // b
      getBitWidth_b(bitWidth);
      *getter = (void(*)(uint8_t*))getBits_b;
      return 0;
    case 2: // sub
      getBitWidth_sub(bitWidth);
      *getter = (void(*)(uint8_t*))getBits_sub;
      return 0;
    case 3: // out
      getBitWidth_out(bitWidth);
      *getter = (void(*)(uint8_t*))getBits_out;
      return 0;
    default:
      return -1;
  }
}

int port_setter(int id, int *bitWidth, void (**setter)(const uint8_t*)) {
  switch (id) {
    case 0: // a
      getBitWidth_a(bitWidth);
      *setter = (void(*)(const uint8_t*))setBits_a;
      return 0;
    case 1: // b
      getBitWidth_b(bitWidth);
      *setter = (void(*)(const uint8_t*))setBits_b;
      return 0;
    case 2: // sub
      getBitWidth_sub(bitWidth);
      *setter = (void(*)(const uint8_t*))setBits_sub;
      return 0;
    default:
      return -1;
  }
}

} // extern "C"

