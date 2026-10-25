package com.termux.x11;

import java.util.*;
import java.util.regex.*;

/** Bounded startup categories only: never exports raw program text, paths or focus tokens. */
public final class X11StartupLog {
 private final StringBuilder pending=new StringBuilder();
 private final LinkedHashSet<String> events=new LinkedHashSet<>();
 private boolean oversized;private int childExit=-1;
 public synchronized int childExit(){return childExit;}
 private final LinkedHashMap<String,String> traceFirst=new LinkedHashMap<>(),traceLast=new LinkedHashMap<>();
 private static final Pattern PROC=Pattern.compile("^JOBS_X11_PROC (xfce4-session|xfwm4|xfsettingsd|xfce4-panel|Thunar|thunar|xfdesktop|fcitx5|dbus-daemon) pid=([0-9]{1,10}) event=(?:first|sample) state=[RSDTZtIXWP] age_ms=(?:[0-9]{1,15}|unknown) cpu_ms=[0-9]{1,15} wait=(?:futex|poll|sleep|io|other) sm=[01] binary=(?:unknown|proot|termux|guest)$");
 private static final Pattern QUERY=Pattern.compile("^JOBS_X11_QUERY (session|map|tree|root|window) count=[0-9]{1,10} total_ms=[0-9]{1,15} max_ms=[0-9]{1,10} failed=[0-9]{1,10}$");
 private static final Pattern SESSION=Pattern.compile("^JOBS_X11_SESSION state=(startup|idle|other|unavailable) clients=(?:[0-9]{1,4}|unknown)$");
 private static final Pattern BUS=Pattern.compile("^JOBS_X11_BUS guest_uid=[0-9]{1,10} kernel_uid=[0-9]{1,10} guest_auth=(?:ok|rejected|other|unavailable) kernel_auth=(?:ok|rejected|other|unavailable)$");
 private void trace(String key,String text){if(!traceFirst.containsKey(key)&&traceFirst.size()>=40)return;String line="+"+elapsedMs()+"ms "+text;if(!traceFirst.containsKey(key))traceFirst.put(key,line);traceLast.put(key,line);}
 private final java.util.function.LongSupplier clock;private final long began;private long finished=-1;
 public X11StartupLog(){this(System::nanoTime);}
 X11StartupLog(java.util.function.LongSupplier clock){this.clock=clock;began=clock.getAsLong();}
 public synchronized long elapsedMs(){return finished>=0?finished:Math.max(0,(clock.getAsLong()-began)/1000000);}
 private static final Pattern MARKER=Pattern.compile("^(?:JOBS_GPU_RENDERER|JOBS_X11_WINDOW_MANAGER|JOBS_X11_COMPOSITOR|JOBS_X11_STARTUP|DESKTOP_CHILD_EXIT|ERROR)[:=]([a-zA-Z0-9_-]{1,80})$");
 private static final Pattern GL=Pattern.compile("\\b(?:EGL_(?:NOT_INITIALIZED|BAD_[A-Z_]+)|GLXBad[A-Za-z]+|VK_ERROR_[A-Z_]+)\\b");
 public synchronized void feed(String text){for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='\n'){if(!oversized)line(pending.toString().trim());pending.setLength(0);oversized=false;}else if(!oversized){if(pending.length()<2048)pending.append(c);else{pending.setLength(0);oversized=true;}}}}
 private void add(String text){if(events.size()<48){for(String event:events)if(event.endsWith(" "+text))return;events.add("+"+elapsedMs()+"ms "+text);}}
 private void line(String text){
  if(text.matches("DESKTOP_CHILD_EXIT=[0-9]{1,3}")){int code=Integer.parseInt(text.substring(text.indexOf('=')+1));if(code<=255)childExit=code;}

  if(text.startsWith("JOBS_GPU_BACKEND=")){if(text.equals("JOBS_GPU_BACKEND=gl")||text.equals("JOBS_GPU_BACKEND=vulkan"))add(text);return;}
  if(text.startsWith("JOBS_GPU_GLX=")){if(text.equals("JOBS_GPU_GLX=drisw-virpipe"))add(text);return;}
  if(text.equals("MISSING:root-permissions"))add("ROOTFS_OWNER_ACCESS_REQUIRED");
  if(text.equals("JOBS_X11_ROOT_PERMISSIONS=owner-access-restored"))add("ROOTFS_OWNER_ACCESS_RESTORED");
  if(text.startsWith("JOBS_X11_WRAPPER_DRIVER=")){if(text.matches("JOBS_X11_WRAPPER_DRIVER=[a-f0-9]{16}"))add(text);return;}
  Matcher process=PROC.matcher(text);if(process.matches()){trace("process:"+process.group(1)+":"+process.group(2),text);return;}
  Matcher query=QUERY.matcher(text);if(query.matches()){trace("query:"+query.group(1),text);return;}
  if(SESSION.matcher(text).matches()){trace("session",text);return;}
  if(BUS.matcher(text).matches()){trace("bus",text);return;}
  Matcher marker=MARKER.matcher(text);if(marker.matches()){if(text.startsWith("ERROR:")&&!text.matches("ERROR:(?:X11_SCALE(?:_MISSING)?|GPU_[A-Z0-9_]+|VIRGL_[A-Z0-9_]+|WRAPPER_[A-Z0-9_]+|BIONIC_[A-Z0-9_]+|ARCH_ROOTFS_OWNER_ACCESS|COMPONENT_OPERATION_EXCEPTION|ARM64_REQUIRED|UBUNTU_26_04_LIBVULKAN1_LIBEGL1_REQUIRED|CHECKSUM_MISMATCH|CACHE_INCOMPLETE|CACHE_PATH|CACHE_IS_A_LINK|CONTROL_PATH|UNSUPPORTED_DESKTOP|INVALID_RENDERER|FOCUS_TOKEN|ARM64_REQUIRED|DOWNLOAD_LIMIT|INSECURE_REDIRECT|INSECURE_RESPONSE)")){add("UNRECOGNIZED_STARTUP_ERROR");return;}add(text);if(text.equals("JOBS_X11_STARTUP=desktop-ready")||text.equals("JOBS_X11_STARTUP=ended"))finished=elapsedMs();return;}
  Matcher gl=GL.matcher(text);while(gl.find())add("GRAPHICS_ERROR="+gl.group());
  String s=text.toLowerCase(Locale.ROOT);
  if(s.contains("protocol version too low"))add("VIRGL_PROTOCOL_TOO_OLD");
  if(s.contains("lost connection to rendering server"))add("VIRGL_CLIENT_CONNECTION_LOST");
  if(s.contains("failed to load driver")||s.contains("failed to open")&&s.contains(".so"))add("DRIVER_LIBRARY_LOAD_FAILED");
  if(s.contains("cannot open shared object")||s.contains("error while loading shared libraries"))add("SHARED_LIBRARY_MISSING");
  if(s.contains("undefined symbol")||s.contains("cannot locate symbol")||s.contains("symbol lookup error"))add("LIBRARY_SYMBOL_MISSING");
  if(s.contains("cannot link executable"))add("EXECUTABLE_LINK_FAILED");
  if(s.contains("dlopen failed")&&(s.contains("not found")||s.contains("cannot locate")))add("SHARED_LIBRARY_MISSING");
  if(s.contains("libgcrypt is too old"))add("LIBRARY_VERSION_MISMATCH=libgcrypt");
  if(s.contains("__hash_memory")&&(s.contains("symbol")||s.contains("undefined")))add("LIBRARY_ABI_MISMATCH=libcxx-hash-memory");
  if(s.contains("gpgrt_nvc_parse")&&(s.contains("symbol")||s.contains("undefined")))add("LIBRARY_ABI_MISMATCH=libgpg-error-nvc");
  if(s.contains("libpoppler-glib.so")&&(s.contains("not found")||s.contains("cannot open")))add("OPTIONAL_COMPONENT_MISSING=pdf-thumbnails");
  if(s.contains("warning:x11_input_component_unavailable"))add("AUTOMATIC_INPUT_COMPONENT_UNAVAILABLE");
  if(s.contains("/data/data/com.termux/files/usr/bin/"))for(String name:new String[]{"fcitx5","gpgconf","gpg-agent","ssh-agent","dbus-daemon"})if(s.contains("/data/data/com.termux/files/usr/bin/"+name))add("TERMUX_EXECUTABLE="+name);

  if(s.contains("glycin")&&(s.contains("bwrap")||s.contains("loader process exited early")))add("GLYCIN_IMAGE_SANDBOX_FAILED");
  if(s.contains("gtkiconhelper")||s.contains("default_icon_at_size")||s.contains("failed to load")&&s.contains("image-missing"))add("DESKTOP_ICON_LOAD_FAILED");
  if(s.contains("zink")&&(s.contains("failed")||s.contains("error")))add("ZINK_FAILURE");
  if(s.contains("failed to create dri")||s.contains("dri3")&&(s.contains("failed")||s.contains("error")))add("DRI3_FAILURE");
  if(s.contains("composit")&&(s.contains("failed")||s.contains("error")||s.contains("unsupported")))add("COMPOSITOR_FAILURE");
  if(s.contains("cannot open display")||s.contains("can't open display"))add("DISPLAY_OPEN_FAILED");
  if(s.contains("authorization required")||s.contains("no protocol specified"))add("X11_AUTH_FAILED");
  if(s.contains("failed to connect to the session manager")||s.contains("failed to connect to session manager"))add("SESSION_MANAGER_CONNECT_FAILED");
  if(s.contains("failed to register in time"))add("SESSION_CLIENT_REGISTRATION_TIMEOUT");
  if(s.contains("ice")&&(s.contains("authentication")||s.contains("i/o error")||s.contains("connection rejected")))add("ICE_CONNECTION_OR_AUTH_FAILURE");
  if(s.contains("dbus")||s.contains("d-bus")){if(s.contains("did not receive a reply")||s.contains("timed out"))add("DBUS_REPLY_TIMEOUT");if(s.contains("failed to connect")||s.contains("failed to register"))add("DBUS_CONNECTION_FAILED");}
  if(!s.contains("failed to fetch _net_current_desktop")&&(s.contains("failed to download")||s.contains("cannot download")||s.contains("failed to fetch")||s.contains("failed retrieving file")||s.contains("download library error")))add("PACKAGE_DOWNLOAD_FAILED");
  if(s.contains("signature")&&(s.contains("failed to download")||s.contains("cannot download")))add("PACKAGE_SIGNATURE_DOWNLOAD_FAILED");
  if((s.contains("signature")||s.contains("checksum"))&&(s.contains("invalid")||s.contains("mismatch")||s.contains("verification failed")))add("PACKAGE_INTEGRITY_CHECK_FAILED");
  if(s.contains("could not resolve")||s.contains("couldn't resolve")||s.contains("name or service not known")||s.contains("temporary failure in name resolution"))add("NETWORK_DNS_FAILED");
  if(s.contains("timed out")||s.contains("timeout was reached"))add("NETWORK_TIMEOUT");
  if(s.contains("no route to host")||s.contains("network is unreachable"))add("NETWORK_ROUTE_UNAVAILABLE");
  if(s.contains("connection reset")||s.contains("connection refused")||s.contains("failed to connect")||s.contains("couldn't connect"))add("NETWORK_CONNECTION_FAILED");
  if((s.contains("certificate")&&(s.contains("failed")||s.contains("expired")||s.contains("unable to")))||s.contains("ssl connect error")||s.contains("tls handshake"))add("NETWORK_TLS_FAILED");
  if(s.contains("no space left on device"))add("STORAGE_FULL");
  if(s.contains("unable to acquire")&&s.contains("lock")||s.contains("could not get lock")||s.contains("failed to lock"))add("PACKAGE_MANAGER_LOCKED");
  if(s.contains("unable to locate package")||s.contains("no match for argument")||s.contains("no installation candidate")||s.contains("package not found"))add("PACKAGE_UNAVAILABLE");
  Matcher http=Pattern.compile("(?i)(?:http(?:/[0-9.]+)?(?: error| status)?[: ]+|returned error[: ]+|status code[: ]+)([45][0-9]{2})\\b").matcher(text);
  while(http.find())add("HTTP_ERROR="+http.group(1));
  if(s.contains("segmentation fault"))add("SEGMENTATION_FAULT");
  if(s.contains("permission denied"))add("PERMISSION_DENIED");
 }
 public synchronized String snapshot(){StringBuilder text=new StringBuilder(String.join("\n",events));for(String key:traceFirst.keySet()){text.append('\n').append(traceFirst.get(key));if(!traceFirst.get(key).equals(traceLast.get(key)))text.append('\n').append(traceLast.get(key));}return text.toString();}
}
