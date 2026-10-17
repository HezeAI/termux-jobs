#pragma once
#include <asm/hwcap.h>
inline bool jobs_fast_cpu_supported(unsigned long hw,unsigned long hw2){
 const unsigned long required=HWCAP_ASIMDDP|HWCAP_FPHP|HWCAP_ASIMDHP|HWCAP_ATOMICS|HWCAP_ASIMDRDM|HWCAP_CRC32;
 return (hw&required)==required&&(hw2&HWCAP2_I8MM)!=0;
}
