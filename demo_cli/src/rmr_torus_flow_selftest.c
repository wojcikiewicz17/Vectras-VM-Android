#include "rmr_torus_flow.h"

#include <stdio.h>

int main(void) {
  RmR_TorusFlowState a;
  RmR_TorusFlowState b;

  RmR_TorusFlow_Init(&a, 0x963u);
  RmR_TorusFlow_Init(&b, 0x963u);

  for (u32 step = 0; step < 12u; ++step) {
    RmR_TorusFlow_InjectGrammar(&a, step + 1u);
    RmR_TorusFlow_InjectGrammar(&b, step + 1u);
    RmR_TorusFlow_Step(&a);
    RmR_TorusFlow_Step(&b);
  }

  u32 c1 = RmR_TorusFlow_Checksum(&a);
  u32 c2 = RmR_TorusFlow_Checksum(&b);
  u32 c_api = RmR_TorusFlow_RunDeterministic(0x963u, 12u);
  if (c1 == 0u || c1 != c2) {
    printf("FAIL torus_flow deterministic mismatch c1=%u c2=%u\n", c1, c2);
    return 1;
  }
  if (c_api != c1) {
    printf("FAIL torus_flow api mismatch c1=%u c_api=%u\n", c1, c_api);
    return 1;
  }

  RmR_TorusFlow_InjectGrammar(&a, 99u);
  RmR_TorusFlow_Step(&a);
  u32 c3 = RmR_TorusFlow_Checksum(&a);
  if (c3 == c1) {
    printf("FAIL torus_flow progression stalled c1=%u c3=%u\n", c1, c3);
    return 1;
  }

  printf("OK torus_flow_selftest checksum=%u next=%u steps=%u\n", c1, c3, a.steps_done);
  return 0;
}
