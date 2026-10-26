/* Build-only ARM64 glibc baseline shim, MIT licensed.
 * glibc 2.43 adds new default libm symbol versions. Keep this private Mesa
 * runtime usable with glibc >= 2.38 by calling the existing ARM64 baseline ABI.
 * Link with --wrap=atan2f, --wrap=asinf and --wrap=acosf. No libc or installed
 * files are replaced; the original ARM64 float ABI and math functions remain.
 */
extern float jobs_baseline_atan2f(float y, float x);
__asm__(".symver jobs_baseline_atan2f,atan2f@GLIBC_2.17");
float __wrap_atan2f(float y, float x) { return jobs_baseline_atan2f(y, x); }
extern float jobs_baseline_asinf(float x);
extern float jobs_baseline_acosf(float x);
__asm__(".symver jobs_baseline_asinf,asinf@GLIBC_2.17");
__asm__(".symver jobs_baseline_acosf,acosf@GLIBC_2.17");
float __wrap_asinf(float x) { return jobs_baseline_asinf(x); }
float __wrap_acosf(float x) { return jobs_baseline_acosf(x); }
