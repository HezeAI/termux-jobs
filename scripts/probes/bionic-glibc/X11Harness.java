package com.termux.x11;
import android.os.Looper;
import android.system.*;
import java.io.FileDescriptor;
import java.net.InetAddress;
public final class X11Harness {
 private static FileDescriptor guard;
 public static void main(String[] args)throws Exception{
  Looper.prepareMainLooper();
  int display=Integer.parseInt(args[0].substring(1));
  guard=Os.socket(OsConstants.AF_INET,OsConstants.SOCK_STREAM,0);
  Os.bind(guard,InetAddress.getByName("127.0.0.1"),7900+display);
  System.load(System.getenv("JOBS_X11_NATIVE"));
  CmdEntryPoint entry=new CmdEntryPoint(()->System.out.println("READY: private X11 test server"));
  if(!entry.start(args))throw new IllegalStateException("Private core startup rejected");
  Looper.loop();
 }
}
