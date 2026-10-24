/* Build-only ARM64 glibc baseline shim, MIT licensed.
 * glibc 2.43 adds a new default atan2f symbol version. Keep this private Mesa
 * runtime usable with glibc >= 2.38 by calling the existing ARM64 baseline ABI.
 * Link the object with --wrap=atan2f; no libc or installed files are replaced.
 */
extern float jobs_baseline_atan2f(float y, float x);
__asm__(".symver jobs_baseline_atan2f,atan2f@GLIBC_2.17");
float __wrap_atan2f(float y, float x) { return jobs_baseline_atan2f(y, x); }
