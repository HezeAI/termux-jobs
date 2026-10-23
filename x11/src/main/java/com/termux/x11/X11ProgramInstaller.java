package com.termux.x11;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A separate pipe-owned job, so checking/installing apps never blocks the running desktop. */
final class X11ProgramInstaller implements AutoCloseable {
    private final Context context;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile Bundle state=new Bundle();
    private volatile int generation;
    private volatile DesktopProcess child;
    private volatile boolean closed;
    private final Map<String,DesktopCatalog> catalogs=new ConcurrentHashMap<>(),installedCatalogs=new ConcurrentHashMap<>();
    private final DesktopCatalogCache cache;
    private final DesktopProgramResume resume;
    private volatile int catalogRevision;
    X11ProgramInstaller(Context context){this.context=context;cache=new DesktopCatalogCache(new File(context.getFilesDir(),"workspace-x11-catalog"));resume=new DesktopProgramResume(new File(context.getFilesDir(),"workspace-x11-program-resume"));}
    Bundle snapshot(){Bundle b=new Bundle(state);b.putBundle("resumePrograms",resume.read(target()));b.putBoolean("catalogReady",catalogs.containsKey(target()));b.putInt("catalogRevision",catalogRevision);b.putBoolean("installedReady",installedCatalogs.containsKey(target()));return b;}
    Bundle page(String target,String query,int offset,boolean installed){DesktopCatalog data=(installed?installedCatalogs:catalogs).get(target);if(data==null)throw new IllegalStateException("Load the desktop catalog first");Bundle page=data.page(query,offset);page.putBoolean("installed",installed);return page;}
    boolean busy(){String p=state.getString("phase","");return p.equals("checking")||p.equals("installing")||p.equals("cancelling")||p.equals("cataloguing")||p.equals("removing");}
    void invalidate(String target){catalogs.remove(target);installedCatalogs.remove(target);cache.invalidate(target,false);cache.invalidate(target,true);cache.saveItems(target,null);catalogRevision++;}
    String target(){return state.getString("target","");}
    void scope(String target){if(!DesktopProcess.supported(target))throw new IllegalArgumentException("Invalid program scope");if(state.isEmpty()){Bundle b=new Bundle();b.putString("target",target);b.putString("phase","idle");state=b;}}
    void start(String op,String target,String id){start(op,target,id,false);}
    void start(String op,String target,String id,boolean refresh){
        if(closed||busy())throw new IllegalStateException("Desktop program operation is busy");
        if(!DesktopProcess.supported(target)||!(op.equals("check")||op.equals("install")||op.equals("catalog")||op.equals("catalog-install")||op.equals("installed")||op.equals("remove")||op.equals("firefox-compat-on")||op.equals("firefox-compat-off")||op.equals("firefox-dns-on")||op.equals("firefox-dns-off"))||(!id.isEmpty()&&!DesktopPrograms.requestId(id))||(op.equals("install")&&id.isEmpty()))throw new IllegalArgumentException("Unsupported desktop program");
        if(op.startsWith("firefox-compat-")&&!DesktopPrograms.supportsFirefox(target))throw new IllegalArgumentException("Compatibility requires a supported desktop environment");
        if(op.startsWith("firefox-dns-")&&!DesktopPrograms.supportsFirefox(target))throw new IllegalArgumentException("HTTPS DNS requires a supported Firefox environment");
        Bundle pending=resume.read(target);
        DesktopCatalog catalog=catalogs.get(target),installedCatalog=installedCatalogs.get(target);
        if(op.equals("remove")&&(!id.startsWith("pkg:")||!DesktopPrograms.removable(id.substring(4))||(installedCatalog==null||!installedCatalog.containsInstalled(id.substring(4)))&&!(target.equals(target())&&DesktopPrograms.knownRecommendedPackage(id.substring(4),target,state.getBundle("items")))))throw new IllegalArgumentException("Not a removable installed application");
        if(!op.equals("remove")&&id.startsWith("pkg:")&&!(op.equals("install")&&pending.containsKey(id))&&(catalog==null||!catalog.allows(id.substring(4))))throw new IllegalArgumentException("Package is not in the current installable catalog");
        if(op.equals("remove")||op.equals("install")||op.equals("catalog-install")){catalogs.remove(target);installedCatalogs.remove(target);catalogRevision++;}
        if(op.equals("install"))resume.put(target,id,pending.getBoolean(id,false));
        int epoch=++generation;Bundle initial=new Bundle();if(target.equals(target())){Bundle known=state.getBundle("items");if(known!=null){Bundle retained=new Bundle(known);if(op.equals("install")||op.equals("remove")){for(String group:DesktopPrograms.IDS)if(group.equals(id)||(id.startsWith("pkg:")&&DesktopPrograms.mainPackage(group,target).equals(id.substring(4))))retained.remove(group);}initial.putBundle("items",retained);}if(state.getBoolean("firefoxDnsKnown",false)){initial.putBoolean("firefoxDnsKnown",true);initial.putBoolean("firefoxDns",state.getBoolean("firefoxDns",false));}}initial.putInt("job",epoch);initial.putString("target",target);initial.putString("program",id);initial.putString("phase",op.equals("catalog")?"cataloguing":(op.equals("check")||op.equals("installed"))?"checking":op.equals("remove")?"removing":"installing");initial.putString("action",op);initial.putInt("percent",-1);initial.putLong("eta",-1);state=initial;
        worker.execute(()->run(epoch,op,target,id,initial,refresh));
    }
    private void run(int epoch,String op,String target,String id,Bundle seed,boolean refresh){
        Bundle initial=new Bundle(seed);int compat=FirefoxCompatibilityState.read(target);initial.putBoolean("firefoxCompatKnown",compat>=0);initial.putBoolean("firefoxCompat",compat==1);
        DesktopProcess owned=null;StringBuilder log=new StringBuilder();X11InstallProgress progress=new X11InstallProgress();
        ProgramOutput output=new ProgramOutput();java.util.concurrent.atomic.AtomicBoolean prepared=new java.util.concurrent.atomic.AtomicBoolean();DesktopCatalog.Parser catalogOutput=new DesktopCatalog.Parser(op.equals("installed"));
        try{
            if(closed||epoch!=generation)return;
            if(op.equals("check")&&!refresh){
                Bundle saved=cache.loadItems(target);
                if(saved!=null){if(epoch==generation&&!closed){Bundle b=new Bundle(initial);b.putBundle("items",saved);b.putString("phase","complete");b.putBoolean("cacheHit",true);b.putInt("percent",100);state=b;}return;}
            }
            boolean reading=op.equals("catalog")||op.equals("installed");
            if(reading&&!refresh){
                Map<String,DesktopCatalog> memory=op.equals("installed")?installedCatalogs:catalogs;
                DesktopCatalog saved=memory.get(target);if(saved==null)saved=cache.load(target,op.equals("installed"));
                if(saved!=null){if(epoch==generation&&!closed){memory.put(target,saved);catalogRevision++;Bundle b=new Bundle(initial);b.putString("phase","complete");b.putBoolean("cacheHit",true);b.putInt("percent",100);state=b;}return;}
            }
            if(closed||epoch!=generation)return;
            if(op.equals("remove")||op.equals("install")||op.equals("catalog-install")){cache.invalidate(target,false);cache.invalidate(target,true);cache.saveItems(target,null);}
            String childOp=op.equals("install")&&resume.read(target).getBoolean(id,false)?"resume":op;
            owned=DesktopProcess.programs(context,X11Runtime.controlDirectory(context),childOp,target,id);child=owned;
            if(closed||epoch!=generation)owned.close();
            owned.read(chunk->{
                output.feed(chunk);if(op.equals("install")&&output.indexReady&&epoch==generation&&!closed&&prepared.compareAndSet(false,true))resume.put(target,id,true);catalogOutput.feed(chunk);progress.feed(chunk,SystemClock.elapsedRealtime());log.append(chunk);if(log.length()>8192)log.delete(0,log.length()-8192);
                if(epoch==generation&&!closed){Bundle b=new Bundle(initial);b.putString("detail",log.toString());b.putInt("percent",progress.percent());b.putLong("eta",progress.etaSeconds(SystemClock.elapsedRealtime()));b.putLong("sampleTime",SystemClock.elapsedRealtime());b.putString("stage",progress.stage());b.putString("item",progress.currentItem());b.putLong("downloadBytes",progress.downloadedBytes());b.putLong("downloadTotal",progress.totalBytes());state=b;}
            });
            int exit=owned.waitFor();
            if(epoch==generation&&!closed){
                Bundle b=new Bundle(state);boolean installedOp=op.equals("installed"),catalogOp=op.equals("catalog")||op.equals("catalog-install");DesktopCatalog loaded=catalogOutput.result();
                boolean ok=(exit==0||exit==137)&&((catalogOp||installedOp)?loaded!=null:output.success());
                if(ok&&op.equals("install"))resume.put(target,id,null);
                if(ok&&op.equals("remove")){for(String pending:resume.read(target).keySet())if(pending.equals(id)||DesktopPrograms.mainPackage(pending,target).equals(id.substring(4)))resume.put(target,pending,null);}
                if(ok&&installedOp){installedCatalogs.put(target,loaded);cache.save(target,true,loaded);catalogRevision++;}
                if(ok&&catalogOp){catalogs.put(target,loaded);cache.save(target,false,loaded);catalogRevision++;}
                if(op.equals("remove")){catalogs.remove(target);installedCatalogs.remove(target);catalogRevision++;}
                if(!ok&&installedOp){installedCatalogs.remove(target);}
                if(!ok&&catalogOp){catalogs.remove(target);catalogRevision++;}
                if(op.startsWith("firefox-compat-")){int current=FirefoxCompatibilityState.read(target);b.putBoolean("firefoxCompatKnown",current>=0);b.putBoolean("firefoxCompat",current==1);}
                if(output.dnsSeen){b.putBoolean("firefoxDnsKnown",output.dnsKnown);b.putBoolean("firefoxDns",output.dnsEnabled);}
                b.putString("phase",ok?"complete":catalogOp&&(catalogOutput.missingTools||catalogOutput.empty)?"needs-tools":"error");b.putString("detail",log.toString());b.putInt("percent",ok?100:progress.percent());b.putLong("eta",-1);
                if(ok&&!catalogOp&&!installedOp){Bundle items=output.items();b.putBundle("items",items);cache.saveItems(target,items);}
                if(ok&&(catalogOp||installedOp))b.putString("detail","");
                // Finish only after the owned guardian/group has exited.
                state=b;
            }
        }catch(Exception e){if(epoch==generation&&!closed){Bundle b=new Bundle(state);b.putString("phase","error");String message=e.getMessage();b.putString("detail",log+"\n"+(message==null?e.getClass().getSimpleName():message.substring(0,Math.min(240,message.length()))));state=b;}}
        finally{
            if(owned!=null){
                owned.close();boolean interrupted=false;
                // Even a journal/output error must finish this owned group before
                // the single worker can launch a retry. Never signal borrowed PIDs.
                while(true){try{owned.waitFor();break;}catch(InterruptedException error){interrupted=true;}}
                if(interrupted)Thread.currentThread().interrupt();
            }
            child=null;
            if(epoch!=generation&&!closed&&state.getInt("job",0)==epoch){Bundle b=new Bundle(state);b.putString("phase","cancelled");b.putString("detail",log.toString());b.putLong("eta",-1);state=b;}
        }
    }
    void cancel(){if(!busy())return;generation++;Bundle b=new Bundle(state);b.putString("phase","cancelling");state=b;DesktopProcess owned=child;if(owned!=null)owned.close();}
    @Override public void close(){closed=true;generation++;DesktopProcess owned=child;if(owned!=null)owned.close();worker.shutdownNow();}

    /** Results are accepted only from a complete successful child, never partial APT output. */
    static final class ProgramOutput {
        private final StringBuilder line=new StringBuilder();private boolean overflow,ready,exited;
        boolean dnsSeen,dnsKnown,dnsEnabled,indexReady;
        private final Bundle items=new Bundle();
        void feed(String chunk){for(int i=0;i<chunk.length();i++){char c=chunk.charAt(i);if(c=='\n'||c=='\r'){if(!overflow)parse(line.toString());line.setLength(0);overflow=false;}else if(!overflow){if(line.length()<512)line.append(c);else{line.setLength(0);overflow=true;}}}}
        private void parse(String value){if(value.equals("JOBS_PROGRAM_INDEX_READY"))indexReady=true;if(value.equals("FIREFOX_DNS:unknown")){dnsSeen=true;dnsKnown=false;}if(value.equals("FIREFOX_DNS:on")||value.equals("FIREFOX_DNS:off")){dnsSeen=true;dnsKnown=true;dnsEnabled=value.endsWith(":on");}if(value.equals("READY:programs"))ready=true;if(value.equals("DESKTOP_CHILD_EXIT=0"))exited=true;
            String[] p=value.split(":",-1);if(p.length==3&&p[0].equals("PROGRAM")&&DesktopPrograms.valid(p[1])&&(p[2].equals("installed")||p[2].equals("missing")))items.putBoolean(p[1],p[2].equals("installed"));}
        boolean success(){return ready&&exited&&items.size()==DesktopPrograms.IDS.length;}
        Bundle items(){return new Bundle(items);}
    }
}
