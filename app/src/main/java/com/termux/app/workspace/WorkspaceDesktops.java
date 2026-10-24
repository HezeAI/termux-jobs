package com.termux.app.workspace;
import android.view.View;
import com.termux.app.TermuxActivity;
import com.termux.x11.DesktopPrograms;
import java.util.*;
import java.util.function.*;
/** One UI channel/server per environment, one visible surface in the Activity. */
final class WorkspaceDesktops {
 private final Map<String,WorkspaceDesktop> channels=new LinkedHashMap<>();
 private final TermuxActivity activity;private final Supplier<WorkspaceStore.Session> selected;
 private final BooleanSupplier visible;private final View input;private final Runnable changed,restore;
 private Runnable keyboardChanged=()->{};private boolean closed;
 WorkspaceDesktops(TermuxActivity a,Supplier<WorkspaceStore.Session> s,BooleanSupplier v,View i,Runnable c,Runnable r){activity=a;selected=s;visible=v;input=i;changed=c;restore=r;}
 private WorkspaceDesktop current(boolean create){
  WorkspaceStore.Session p=selected.get();String target=WorkspaceDesktop.target(p);
  if(closed||!DesktopPrograms.supportsTarget(target))return null;
  WorkspaceDesktop existing=channels.get(target);
  if(existing==null&&create){
   existing=new WorkspaceDesktop(activity,()->{WorkspaceStore.Session now=selected.get();return target.equals(WorkspaceDesktop.target(now))?now:null;},()->visible.getAsBoolean()&&target.equals(WorkspaceDesktop.target(selected.get())),input,changed,restore);
   existing.onKeyboardChanged(()->{if(target.equals(WorkspaceDesktop.target(selected.get())))keyboardChanged.run();});channels.put(target,existing);
  }
  return existing;
 }
 static boolean diagnosticsEligible(WorkspaceStore.Session p){return WorkspaceDesktop.eligible(p)&&!WorkspaceDesktop.target(p).equals("ubuntu-ai");}
 String diagnostics(WorkspaceStore.Session p){
  if(!diagnosticsEligible(p))return "";
  String target=WorkspaceDesktop.target(p);boolean ru=WorkspaceLanguage.code(activity).equals("ru");
  if(!DesktopPrograms.supportsTarget(target))return ru?"X11 · Рабочий стол пока не поддерживается":"X11 · Desktop is not yet supported";
  WorkspaceDesktop c=channels.get(target);
  return c==null?(ru?"X11 · Нет сведений о запуске в этом процессе приложения":"X11 · No startup snapshot in this app process"):c.sessionDiagnostic(p);
 }
 void prepareDiagnostics(WorkspaceStore.Session p,Runnable done){WorkspaceDesktop c=diagnosticsEligible(p)?channels.get(WorkspaceDesktop.target(p)):null;if(c==null)done.run();else c.refreshDiagnostic(done);}
 void onKeyboardChanged(Runnable r){keyboardChanged=r;}
 boolean hasSurface(){WorkspaceDesktop c=current(false);return c!=null&&c.hasSurface();}
 boolean showing(){WorkspaceDesktop c=current(false);return c!=null&&c.showing();}
 boolean keyboardEnabled(){WorkspaceDesktop c=current(false);return c!=null&&c.keyboardEnabled();}
 boolean extraKeysVisible(){WorkspaceDesktop c=current(false);return c!=null&&c.extraKeysVisible();}
 void extraKeys(){WorkspaceDesktop c=current(false);if(c!=null)c.extraKeys();}
 void keyboard(){WorkspaceDesktop c=current(false);if(c!=null)c.keyboard();}
 boolean fullscreenBack(){WorkspaceDesktop c=current(false);return c!=null&&c.fullscreenBack();}
 String label(){WorkspaceDesktop c=current(true);return c==null?(WorkspaceLanguage.code(activity).equals("ru")?"Терминал":"Terminal"):c.label();}
 void openPrograms(){WorkspaceDesktop c=current(true);if(c!=null)c.openPrograms();else activity.showToast(WorkspaceLanguage.code(activity).equals("ru")?"X11 пока не поддерживается этим окружением.":"X11 is not yet supported in this environment.",true);}
 boolean programsInstalling(){WorkspaceDesktop c=current(false);return c!=null&&c.programsInstalling()&&!c.programsVisible();}
 void menu(){WorkspaceDesktop c=current(true);if(c!=null)c.menu();else activity.showToast(WorkspaceLanguage.code(activity).equals("ru")?"X11 пока не поддерживается этим окружением.":"X11 is not yet supported in this environment.",true);}
 boolean owns(WorkspaceStore.Session p){WorkspaceDesktop c=channels.get(WorkspaceDesktop.target(p));return c!=null&&c.owns(p);}
 void sessionClosed(WorkspaceStore.Session p){WorkspaceDesktop c=channels.get(WorkspaceDesktop.target(p));if(c!=null)c.sessionClosed(p);}
 void sync(){if(closed)return;WorkspaceDesktop active=current(false);for(WorkspaceDesktop c:new ArrayList<>(channels.values()))if(c!=active)c.sync();if(active!=null)active.sync();}
 void close(){if(closed)return;closed=true;for(WorkspaceDesktop c:channels.values())c.close();channels.clear();}
}
