package com.termux.x11;
public final class X11Bootstrap {
 public static void main(String[] args)throws Exception{
  String nativePath=System.getenv("JOBS_X11_NATIVE");
  dalvik.system.PathClassLoader loader=new dalvik.system.PathClassLoader(System.getenv("CLASSPATH"),new java.io.File(nativePath).getParent(),ClassLoader.getSystemClassLoader().getParent());
  Class.forName("com.termux.x11.X11Harness",true,loader).getMethod("main",String[].class).invoke(null,(Object)args);
 }
}
