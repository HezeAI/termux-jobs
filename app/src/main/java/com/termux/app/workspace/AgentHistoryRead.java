package com.termux.app.workspace;
import java.io.*;import java.util.*;
/** Cancels only sockets opened by this read on its worker thread, never the agent. */
final class AgentHistoryRead implements AutoCloseable {
 private static final ThreadLocal<AgentHistoryRead> active=new ThreadLocal<>();private final List<Closeable> owned=new ArrayList<>();private boolean cancelled;
 static void track(Closeable socket)throws IOException{AgentHistoryRead scope=active.get();if(scope!=null)scope.add(socket);}
 private synchronized void add(Closeable socket)throws IOException{if(cancelled){socket.close();throw new InterruptedIOException("History read cancelled");}owned.add(socket);}
 AutoCloseable enter(){AgentHistoryRead previous=active.get();active.set(this);return ()->{if(previous==null)active.remove();else active.set(previous);};}
 synchronized boolean cancelled(){return cancelled;}
 public synchronized void close(){cancelled=true;for(Closeable socket:owned)try{socket.close();}catch(IOException ignored){}owned.clear();}
}
