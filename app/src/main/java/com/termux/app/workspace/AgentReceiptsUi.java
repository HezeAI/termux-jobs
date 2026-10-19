package com.termux.app.workspace;

import android.os.*;
import android.graphics.Typeface;
import android.widget.*;
import com.termux.app.TermuxActivity;
import org.json.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;

/** OpenCode pending requests and native transcript receipt details; never executes terminal text. */
final class AgentReceiptsUi {
    private final TermuxActivity activity;
    private final Supplier<WorkspaceStore.Session> current;
    private final BooleanSupplier visible;
    interface Transport {JSONObject request(WorkspaceStore.Session p,JSONObject q)throws Exception;}
    private final Transport transport;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService reader=Executors.newSingleThreadExecutor();
    private final GeminiPermissionUi openCode;
    private boolean closed;
    private android.app.AlertDialog details;
    private String detailsOwner="",lastOwner="";
    private long previewSerial;
    private static final Pattern TAG=Pattern.compile("\\[([GO]):([a-f0-9]{8})\\]");
    AgentReceiptsUi(TermuxActivity a,Supplier<WorkspaceStore.Session> current,BooleanSupplier visible){
        this(a,current,visible,(p,q)->p.geminiAgent?GeminiLiveControl.request(a.getFilesDir(),p.geminiSocketId,q):AiAgents.liveRequest(a,p,q));
    }
    AgentReceiptsUi(TermuxActivity a,Supplier<WorkspaceStore.Session> current,BooleanSupplier visible,Transport transport){
        activity=a;this.current=current;this.visible=visible;this.transport=transport;
        openCode=new GeminiPermissionUi(a,()->{WorkspaceStore.Session p=current.get();return p!=null&&p.aiAgent.equals("opencode")?p:null;},visible,(run,q)->{
            WorkspaceStore.Session p=current.get();if(p==null||!run.equals(owner(p)))throw new java.io.IOException("Changed session");
            WorkspaceStore.Session snapshot=snapshot(p);return transport.request(snapshot,q);
        },true);handler.post(poll);
    }
    private static WorkspaceStore.Session snapshot(WorkspaceStore.Session p){WorkspaceStore.Session s=new WorkspaceStore.Session();s.id=p.id;s.aiAgent=p.aiAgent;s.aiThread=p.aiThread;s.aiConnection=p.aiConnection;s.geminiAgent=p.geminiAgent;s.geminiSocketId=p.geminiSocketId;return s;}
    private String owner(WorkspaceStore.Session p){return p.aiAgent.equals("opencode")?p.id+":"+p.aiThread:p.geminiSocketId;}
    private final Runnable poll=new Runnable(){public void run(){if(closed)return;syncVisibility();WorkspaceStore.Session p=current.get();if(visible.getAsBoolean()&&p!=null&&p.aiAgent.equals("opencode"))openCode.refresh(p);handler.postDelayed(this,1500);}};
    void syncVisibility(){openCode.syncVisibility();WorkspaceStore.Session p=current.get();String active=visible.getAsBoolean()&&p!=null?owner(p):"";if(!active.equals(lastOwner)){lastOwner=active;previewSerial++;}if(details!=null&&(!visible.getAsBoolean()||p==null||!owner(p).equals(detailsOwner))){details.dismiss();details=null;}}
    boolean tap(String line){
        syncVisibility();WorkspaceStore.Session p=current.get();Matcher m=TAG.matcher(line);if(closed||!visible.getAsBoolean()||p==null||!m.find())return false;
        boolean gemini=m.group(1).equals("G");if(gemini?!p.geminiAgent:!p.aiAgent.equals("opencode"))return false;
        final long serial=++previewSerial;final String token=m.group(2),captured=owner(p);final WorkspaceStore.Session snapshot=snapshot(p);
        reader.execute(()->{JSONObject result=null;try{JSONObject q=new JSONObject().put("op","receipt").put("token",token);result=transport.request(snapshot,q);}catch(Exception ignored){}
            final JSONObject response=result;handler.post(()->{WorkspaceStore.Session now=current.get();if(closed||serial!=previewSerial||!visible.getAsBoolean()||now==null||!captured.equals(owner(now)))return;
                JSONObject r=response==null?null:response.optJSONObject("receipt");if(r==null||!token.equals(r.optString("token"))){Toast.makeText(activity,WorkspaceLanguage.code(activity).equals("ru")?"Подробности этой записи недоступны в текущем чате.":"Receipt details are unavailable in this chat.",Toast.LENGTH_SHORT).show();return;}
                TextView text=new TextView(activity);text.setTextColor(0xffd9e6eb);text.setTextSize(13);text.setTypeface(Typeface.MONOSPACE);text.setTextIsSelectable(true);int pad=Math.round(16*activity.getResources().getDisplayMetrics().density);text.setPadding(pad,pad,pad,pad);
                boolean ru=WorkspaceLanguage.code(activity).equals("ru");String note=ru?"Это запись о разрешении, а не подтверждение успешного выполнения.":"This records permission, not successful execution.";
                if(gemini&&r.optString("outcome").equals("proceed_always"))note+="\n"+(ru?"Сессионное разрешение относилось к тому запуску Gemini. Эта запись не выдаёт его заново.":"Session permission belonged to that Gemini launch. This receipt does not grant it again.");
                text.setText(note+"\n\n"+r.optString("body"));ScrollView scroll=new ScrollView(activity);scroll.addView(text);if(details!=null)details.dismiss();detailsOwner=captured;
                details=WorkspaceDialogs.builder(activity).setTitle(r.optString("title")).setView(scroll).setPositiveButton(ru?"Назад":"Back",null).create();details.show();
            });});return true;
    }
    void close(){closed=true;handler.removeCallbacks(poll);openCode.close();reader.shutdownNow();if(details!=null)details.dismiss();}
}
