package com.termux.x11;
import android.content.Context;
/** Fixed private service slots. Profiles never supply process names, ports or paths. */
public final class X11Environment {
 public final String target,process,directory;public final int display,port;public final Class<? extends X11Service> service;
 private X11Environment(String t,String p,String d,int n,Class<? extends X11Service> s){target=t;process=p;directory=d;display=n;port=7900+n;service=s;}
 public static X11Environment forTarget(String target){
  switch(target){
   case "termux":return new X11Environment(target,":jobs_x11","workspace-x11",97,X11Service.class);
   case "ubuntu":return new X11Environment(target,":jobs_x11_ubuntu","workspace-x11-ubuntu",98,X11Service.Ubuntu.class);
   case "jobs-x11-ubuntu":return new X11Environment(target,":jobs_x11_test","workspace-x11-jobs-x11-ubuntu",99,X11Service.TestUbuntu.class);
   case "debian":return new X11Environment(target,":jobs_x11_debian","workspace-x11-debian",100,X11Service.Debian.class);
   case "alpine":return new X11Environment(target,":jobs_x11_alpine","workspace-x11-alpine",101,X11Service.Alpine.class);
   case "fedora":return new X11Environment(target,":jobs_x11_fedora","workspace-x11-fedora",102,X11Service.Fedora.class);
   case "opensuse":return new X11Environment(target,":jobs_x11_opensuse","workspace-x11-opensuse",103,X11Service.OpenSuse.class);
   case "void":return new X11Environment(target,":jobs_x11_void","workspace-x11-void",104,X11Service.VoidLinux.class);
   case "archlinux":return new X11Environment(target,":jobs_x11_archlinux","workspace-x11-archlinux",105,X11Service.ArchLinux.class);
   default:throw new IllegalArgumentException("Unsupported desktop environment");
  }
 }
 static X11Environment context(Context context){
  Class<?> c=context.getClass();for(String t:new String[]{"termux","ubuntu","jobs-x11-ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){X11Environment e=forTarget(t);if(c==e.service)return e;}
  return forTarget("termux"); // JVM fixtures / old private test activity only.
 }
 boolean owns(String name,String pkg){return (pkg+process).equals(name);}
}
