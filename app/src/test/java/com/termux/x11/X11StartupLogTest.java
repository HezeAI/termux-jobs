package com.termux.x11;
import org.junit.Test;
import static org.junit.Assert.*;
public class X11StartupLogTest {
 @Test public void virglLoaderCategoryRejectsArbitraryPayload(){
  X11StartupLog log=new X11StartupLog();log.feed("JOBS_GPU_GLX=drisw-virpipe\nJOBS_GPU_GLX=private/socket token=secret\n");String out=log.snapshot();assertTrue(out.contains("JOBS_GPU_GLX=drisw-virpipe"));assertFalse(out.contains("private"));assertFalse(out.contains("secret"));
 }
 @Test public void archRootRepairUsesFixedCategoriesOnly(){
  X11StartupLog log=new X11StartupLog();log.feed("MISSING:root-permissions\nJOBS_X11_ROOT_PERMISSIONS=owner-access-restored\nERROR:ARCH_ROOTFS_OWNER_ACCESS\n");String out=log.snapshot();assertTrue(out.contains("ROOTFS_OWNER_ACCESS_REQUIRED"));assertTrue(out.contains("ROOTFS_OWNER_ACCESS_RESTORED"));assertTrue(out.contains("ERROR:ARCH_ROOTFS_OWNER_ACCESS"));assertFalse(out.contains("UNRECOGNIZED"));
 }
 @Test public void missingDesktopPropertyIsNotAPackageDownloadFailure(){
  X11StartupLog log=new X11StartupLog();log.feed("Wnck-Message: Failed to fetch _NET_CURRENT_DESKTOP; assuming 0\n");assertFalse(log.snapshot().contains("PACKAGE_DOWNLOAD_FAILED"));
 }
 @Test public void glycinAndIconCrashAreClassifiedWithoutExportingPaths(){
  X11StartupLog log=new X11StartupLog();log.feed("Failed to load /usr/share/icons/private/image-missing.svg: Loader process exited early with status '1' Command: bwrap /usr/lib/glycin-loaders/2+/glycin-svg\nBail out! Gtk:ERROR:gtkiconhelper.c:495:ensure_surface_for_gicon\nWnck:ERROR:xutils.c:1510:default_icon_at_size: assertion failed\n");
  String out=log.snapshot();assertTrue(out.contains("GLYCIN_IMAGE_SANDBOX_FAILED"));assertTrue(out.contains("DESKTOP_ICON_LOAD_FAILED"));assertFalse(out.contains("private"));assertFalse(out.contains("/usr/"));
 }
 @Test public void pacmanRetrievingAndCommitFailuresAreClassifiedAsDownloadFailure(){
  X11StartupLog log=new X11StartupLog();log.feed("error: failed retrieving file 'private.pkg' from mirror.example : Operation too slow\nerror: failed to commit transaction (download library error)\n");
  String out=log.snapshot();assertTrue(out.contains("PACKAGE_DOWNLOAD_FAILED"));assertFalse(out.contains("private.pkg"));assertFalse(out.contains("mirror.example"));
 }
 @Test public void chunkedMarkersAndGraphicsErrorsSurviveWithoutRawPrivateText(){X11StartupLog log=new X11StartupLog();log.feed("JOBS_X11_COMPOSITOR=off\nJOBS_GPU_REN");log.feed("DERER=turnip-zink\nMesa: ZINK: failed EGL_NOT_INITIALIZED at /private/key token=secret\nJOBS_X11_FOCUS_V1 private-token FocusIn\nmy personal document\nERROR:PRIVATE_SECRET_VALUE\n");String result=log.snapshot();assertTrue(result.contains("JOBS_GPU_RENDERER=turnip-zink"));assertTrue(result.contains("JOBS_X11_COMPOSITOR=off"));assertTrue(result.contains("EGL_NOT_INITIALIZED"));assertTrue(result.contains("ZINK_FAILURE"));assertFalse(result.contains("secret"));assertFalse(result.contains("private"));assertFalse(result.contains("document"));assertFalse(result.contains("PRIVATE_SECRET_VALUE"));}
 @Test public void stageTimesAreRelativeAndStopAtDesktopReadiness(){long[] now={1000000000L};X11StartupLog log=new X11StartupLog(()->now[0]);log.feed("JOBS_X11_STARTUP=prepare\n");now[0]+=250000000L;log.feed("JOBS_X11_STARTUP=desktop-ready\n");assertEquals(250,log.elapsedMs());now[0]+=9000000000L;assertEquals(250,log.elapsedMs());assertTrue(log.snapshot().contains("+250ms JOBS_X11_STARTUP=desktop-ready"));}
 @Test public void oversizedLinesAreDroppedAndLaterStagesRetained(){X11StartupLog log=new X11StartupLog();log.feed(new String(new char[3000]).replace('\0','x')+"EGL_BAD_ALLOC\nJOBS_X11_STARTUP=xfce-session\n");assertFalse(log.snapshot().contains("EGL_BAD_ALLOC"));assertTrue(log.snapshot().contains("xfce-session"));for(int i=0;i<200;i++)log.feed("ERROR:TEST_"+i+"\n");assertTrue(log.snapshot().length()<5000);}
 @Test public void processAndQuerySnapshotsKeepFirstAndLastWithoutCrowdingStages(){X11StartupLog log=new X11StartupLog();String base="JOBS_X11_PROC xfdesktop pid=123 event=sample state=S age_ms=";log.feed(base+"1 cpu_ms=2 wait=poll sm=1 binary=proot\n");for(int n=2;n<100;n++)log.feed(base+n+" cpu_ms=2 wait=poll sm=1 binary=proot\n");log.feed("JOBS_X11_QUERY session count=5 total_ms=40 max_ms=20 failed=4\nJOBS_X11_STARTUP=desktop-ready\nJOBS_X11_PROC private pid=1 secret\n");String out=log.snapshot();assertTrue(out.contains("age_ms=1 cpu_ms"));assertTrue(out.contains("age_ms=99 cpu_ms"));assertFalse(out.contains("age_ms=50 cpu_ms"));assertTrue(out.contains("desktop-ready"));assertTrue(out.contains("failed=4"));assertFalse(out.contains("private"));assertTrue(out.length()<2000);}

 @Test public void androidLinkerErrorsBecomeFixedCategoriesWithoutPathsOrSecrets(){
  X11StartupLog log=new X11StartupLog();log.feed("CANNOT LINK EXECUTABLE \"fcitx5\": cannot locate sym");log.feed("bol \"_ZNSt6__ndk113__hash_memoryEPKvm\" referenced by /data/data/com.termux/files/usr/bin/fcitx5 token=secret\nCANNOT LINK EXECUTABLE \"gpgconf\": cannot locate symbol gpgrt_nvc_parse at /data/data/com.termux/files/usr/bin/gpgconf\ngpg-agent: Fatal: libgcrypt is too old (need 1.12.0, have 1.11.0)\ntumbler: dlopen failed: library libpoppler-glib.so not found private-file.pdf\nWARNING:X11_INPUT_COMPONENT_UNAVAILABLE\n");
  String out=log.snapshot();for(String expected:new String[]{"LIBRARY_SYMBOL_MISSING","EXECUTABLE_LINK_FAILED","LIBRARY_ABI_MISMATCH=libcxx-hash-memory","LIBRARY_ABI_MISMATCH=libgpg-error-nvc","LIBRARY_VERSION_MISMATCH=libgcrypt","SHARED_LIBRARY_MISSING","OPTIONAL_COMPONENT_MISSING=pdf-thumbnails","TERMUX_EXECUTABLE=gpgconf","AUTOMATIC_INPUT_COMPONENT_UNAVAILABLE"})assertTrue(expected,out.contains(expected));for(String secret:new String[]{"/data/","token=","secret","private-file"})assertFalse(out.contains(secret));
 }
 @Test public void childStatusSurvivesEventLimitAndIsNotReplacedByGuardianCleanup(){
  X11StartupLog log=new X11StartupLog();assertEquals(-1,log.childExit());for(int i=0;i<80;i++)log.feed("DESKTOP_CHILD_EXIT="+i+"\n");log.feed("DESKTOP_CHILD_EXIT=7\n");assertEquals(7,log.childExit());log.feed("DESKTOP_CHILD_EXIT=999\n");assertEquals(7,log.childExit());
 }


 @Test public void packageSignatureDownloadKeepsNetworkCauseWithoutEndpointOrCredentials(){
  X11StartupLog log=new X11StartupLog();log.feed("ERROR: [trans] failed to down");log.feed("load 'libICE-1.1.2_1' signature from https://private:secret@example.test/path?token=secret: HTTP/2 503 Service Unavailable\nCould not resolve host: private.example\nConnection timed out\nDESKTOP_CHILD_EXIT=19");log.feed("\n");
  String out=log.snapshot();for(String expected:new String[]{"PACKAGE_DOWNLOAD_FAILED","PACKAGE_SIGNATURE_DOWNLOAD_FAILED","HTTP_ERROR=503","NETWORK_DNS_FAILED","NETWORK_TIMEOUT"})assertTrue(expected,out.contains(expected));
  assertEquals(19,log.childExit());for(String secret:new String[]{"private","secret","example","libICE","https://","token="})assertFalse(secret,out.contains(secret));
 }
 @Test public void signatureDownloadFailureIsNotReportedAsInvalidSignature(){
  X11StartupLog log=new X11StartupLog();log.feed("ERROR: failed to download signature: Connection reset by peer\n");
  assertTrue(log.snapshot().contains("NETWORK_CONNECTION_FAILED"));assertFalse(log.snapshot().contains("PACKAGE_INTEGRITY_CHECK_FAILED"));
  log.feed("checksum mismatch\ncertificate verification failed\nno space left on device\n");
  for(String expected:new String[]{"PACKAGE_INTEGRITY_CHECK_FAILED","NETWORK_TLS_FAILED","STORAGE_FULL"})assertTrue(log.snapshot().contains(expected));
 }
 @Test public void missingRouteBecomesStructuredCategory(){X11StartupLog log=new X11StartupLog();log.feed("ERROR: failed to fetch private-url: No route to host\n");assertTrue(log.snapshot().contains("NETWORK_ROUTE_UNAVAILABLE"));assertFalse(log.snapshot().contains("private-url"));}
 @Test public void virglErrorsAndBackendAreCategorizedWithoutRawDetails(){
  X11StartupLog log=new X11StartupLog();log.feed("JOBS_GPU_BACKEND=vulkan\nJOBS_GPU_BACKEND=private-secret\nvtest_protocol_version: protocol version too low private/path\nlost connection to rendering server on 8 read -1 104 token=secret\nERROR:VIRGL_GUEST_DRIVER_MISSING\nvrend: Error switching context: EGL_BAD_ACCESS\n");String out=log.snapshot();
  for(String text:new String[]{"JOBS_GPU_BACKEND=vulkan","VIRGL_PROTOCOL_TOO_OLD","VIRGL_CLIENT_CONNECTION_LOST","VIRGL_GUEST_DRIVER_MISSING","EGL_BAD_ACCESS"})assertTrue(text,out.contains(text));
  assertFalse(out.contains("private"));assertFalse(out.contains("secret"));assertFalse(out.contains("token="));
 }


}
