package com.termux.app.workspace;
import java.util.LinkedHashSet;
/** Benchmark-only cancellation. Never turns a late stop into cancellation of a chat. */
final class LocalAiBenchmarkGate {
 private String active="";private final LinkedHashSet<String> cancelled=new LinkedHashSet<>();
 synchronized boolean begin(String token,Runnable reset){if(!active.isEmpty())throw new IllegalStateException("Benchmark already running");if(cancelled.remove(token))return false;reset.run();active=token;return true;}
 synchronized void cancel(String token,Runnable cancel){cancelled.add(token);if(cancelled.size()>64)cancelled.remove(cancelled.iterator().next());if(active.equals(token))cancel.run();}
 synchronized void end(String token,Runnable reset){if(active.equals(token)){active="";reset.run();}}
}
