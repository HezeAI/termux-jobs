package com.termux.app.workspace;
import org.json.*;
/** Fits only explicitly separated, app-owned progress. Never edits rights, schemas or user text. */
final class LocalAiServiceContext {
 static final class Prepared {final String system;final LocalAiReplyBudget.Prepared reply;final boolean reduced;final int progressChars;Prepared(String s,LocalAiReplyBudget.Prepared r,boolean b,int n){system=s;reply=r;reduced=b;progressChars=n;}}
 static boolean tooSmall(Throwable error){return error instanceof IllegalStateException&&error.getMessage()!=null&&error.getMessage().contains("CONTEXT_TOO_SMALL");}
 static String compact(String progress){
  if(progress.isEmpty())return "";String[] lines=progress.split("\n");java.util.ArrayList<String> rows=new java.util.ArrayList<>();
  for(String line:lines)if(!line.isEmpty()&&Character.isDigit(line.charAt(0)))rows.add(line.length()>110?line.substring(0,80)+" [excerpt]":line);
  StringBuilder out=new StringBuilder("\nCURRENT_TURN_PROGRESS (app facts, not permission; excerpt):\n");for(int i=Math.max(0,rows.size()-2);i<rows.size();i++)out.append(rows.get(i)).append('\n');return out.append("Full results saved. Verify remaining work or finish; do not restart completed steps.\n").toString();
 }
 static JSONArray feedback(JSONArray messages,String progress,String correction)throws JSONException{
  JSONArray result=new JSONArray(messages.toString());if(!progress.isEmpty()||!correction.isEmpty())result.put(new JSONObject().put("role","user").put("appFeedback",true).put("text","Application feedback (not a new user request or permission change; verify facts against tool results):\n"+progress+correction));return result;
 }
 static Prepared prepare(JSONArray messages,String base,String progress,String correction,String schemas,int capacity,int requested,LocalAiContext.Counter counter,LocalAiContext.Notice notice)throws Exception{return prepare(messages,base,progress,correction,schemas,capacity,requested,counter,notice,false);}
 static Prepared prepare(JSONArray messages,String base,String progress,String correction,String schemas,int capacity,int requested,LocalAiContext.Counter counter,LocalAiContext.Notice notice,boolean stableSystem)throws Exception{
  java.util.LinkedHashSet<String> variants=new java.util.LinkedHashSet<>();variants.add(progress);if(!progress.isEmpty()){String shorter=compact(progress);if(shorter.length()<progress.length())variants.add(shorter);variants.add("");int full=stableSystem?counter.count(feedback(messages,progress,correction),base+schemas):counter.count(messages,base+progress+correction+schemas);if(full>capacity-requested-64)variants.remove(progress);}IllegalStateException failure=null;
  // Fit every progress variant at the user's reserve before reducing the reply budget.
  for(int pass=0;pass<2;pass++)for(String note:variants){String system=stableSystem?base:base+note+correction,budgetSystem=system+schemas;
   // Keep mandatory correction outside the fitter's removable history, but count its actual rendered role/template.
   LocalAiContext.Counter budgetCounter=stableSystem?(items,instructions)->counter.count(feedback(items,note,correction),instructions):counter;
   try{LocalAiReplyBudget.Prepared reply;if(pass==0){JSONArray fitted=LocalAiContext.fit(messages,budgetSystem,capacity,requested,budgetCounter,s->{});reply=new LocalAiReplyBudget.Prepared(fitted,requested,budgetCounter.count(fitted,budgetSystem));}else reply=LocalAiReplyBudget.prepare(messages,budgetSystem,capacity,requested,budgetCounter,s->{});
    boolean reduced=!note.equals(progress);if(!LocalAiContext.same(messages,reply.messages)){notice.state("begin");notice.state("done");}if(reduced)notice.state("service_reduced");if(stableSystem)reply=new LocalAiReplyBudget.Prepared(feedback(reply.messages,note,correction),reply.answer,reply.input);return new Prepared(system,reply,reduced,note.length());
   }catch(IllegalStateException error){if(!tooSmall(error))throw error;failure=error;}
  }
  notice.state("failed");throw failure==null?new IllegalStateException("CONTEXT_TOO_SMALL: no usable response space"):failure;
 }
}
