package com.termux.app.workspace;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import org.json.*;
import java.util.concurrent.*;

/** Non-exported inference process. Native crashes must not kill TermuxService. */
public final class LocalAiService extends Service {
    static final int LOAD=1,GENERATE=2,CANCEL=3,CLOSE=4,BENCHMARK=5,CANCEL_BENCHMARK=6,REPLY=10;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final LocalAiEngine engine=new LocalAiEngine();
    private volatile String owner="";private final LocalAiBenchmarkGate benchmarkGate=new LocalAiBenchmarkGate();
    private final Messenger messenger=new Messenger(new Handler(Looper.getMainLooper()){
        public void handleMessage(Message message){
            final int what=message.what,id=message.arg1;final Bundle data=message.getData();final Messenger reply=message.replyTo;final String client=data.getString("client","");
            if(!client.matches("[A-Za-z0-9_-]{1,90}"))return;
            if(what==CANCEL_BENCHMARK){String token=data.getString("benchmark","");if(client.equals(owner)&&token.matches("[A-Za-z0-9_-]{1,90}"))benchmarkGate.cancel(token,engine::cancel);return;}
            if(what==CANCEL){if(client.equals(owner))engine.cancel();return;}
            if(what!=LOAD&&what!=GENERATE&&what!=CLOSE&&what!=BENCHMARK)return;
            worker.execute(()->{
                try{
                    if(!com.termux.BuildConfig.LOCAL_AI_ENABLED)throw new IllegalStateException("Local models are unavailable in this public build");
                    if(what==LOAD){
                        engine.close();owner=client;String cpu=data.getString("cpu","auto");LocalAiEngine.validateCpuMode(cpu);engine.cpuMode=cpu;engine.compute=data.getString("compute","cpu");LocalAiEngine.validateCompute(engine.compute);engine.protocol=data.getString("protocol","json");LocalAiQwenProtocol.validate(engine.protocol);engine.location=data.getString("location","android");LocalAiProcess.validate(engine.location);LocalAiFiles files=new LocalAiFiles(LocalAiService.this);JSONObject model=files.find(data.getString("model",""));
                        String lora=data.getString("lora","");LocalAiLora.id(lora);engine.loraFile=lora.isEmpty()?null:LocalAiLora.checked(files,lora,model);
                        int context=data.getInt("context",4096);LocalAiLimits.engine(model.getString("engine"),context,768);engine.load(LocalAiService.this,model,files.modelFile(model),data.getString("backend","auto"),LocalAiCpu.resolve(data.getInt("threads",0),Runtime.getRuntime().availableProcessors()),context);
                        send(reply,id,"loaded",engine.backend);
                    }else if(what==BENCHMARK){
                        if(!client.equals(owner))throw new IllegalStateException("This model is owned by another local session");String token=data.getString("benchmark","");if(!token.matches("[A-Za-z0-9_-]{1,90}"))throw new IllegalArgumentException("Invalid benchmark ID");if(!benchmarkGate.begin(token,engine::resetBenchmarkCancel))throw new CancellationException();try{engine.benchmark(data.getInt("mode",-1),new LocalAiEngine.Output(){public void text(String text){}public void performance(String json){send(reply,id,"performance",json);}});send(reply,id,"done","");}finally{benchmarkGate.end(token,engine::resetBenchmarkCancel);}
                    }else if(what==GENERATE){
                        if(!client.equals(owner))throw new IllegalStateException("This model was stopped by another local session");
                        JSONArray history=new JSONArray(data.getString("history","[]"));long[] last={0};String[] result={""};
                        String scope=data.getString("scope","");if(!scope.isEmpty())LocalAiFiles.safeId(scope);engine.cacheScope=scope.isEmpty()?"":client+":"+scope;
                        engine.nativeSchemas=new JSONArray(data.getString("nativeSchemas","[]"));
                        engine.probeThinking=data.getBoolean("probeThinking",false);engine.probeGreedy=data.getBoolean("probeGreedy",false);
                        engine.system=data.getString("system","You are a helpful assistant. No tools are available.");engine.turnProgress=data.getString("turnProgress","");engine.turnCorrection=data.getString("turnCorrection","");
                        int tokens=data.getInt("tokens",768);LocalAiLimits.validate(4096,tokens);engine.generate(history,tokens,new LocalAiEngine.Output(){public void text(String text){result[0]=text;long now=android.os.SystemClock.uptimeMillis();if(now-last[0]>=65){last[0]=now;send(reply,id,"text",text);}}public void context(String state){send(reply,id,"context",state);}public void performance(String json){send(reply,id,"performance",json);}public void stage(String stage){send(reply,id,"stage",stage);}public void metrics(int input,int output){send(reply,id,"metrics","{\"input\":"+input+",\"output\":"+output+"}");}});
                        send(reply,id,"performance",new JSONObject().put("kind","finish_reason").put("value",engine.finishReason).toString());send(reply,id,"done",result[0],engine.finishReason);
                    }else{if(client.equals(owner)){engine.close();owner="";}send(reply,id,"closed","");}
                }catch(Throwable e){send(reply,id,"error",LocalAiEngine.message(e));}
            });
        }
    });
    private static void send(Messenger to,int id,String type,String text){send(to,id,type,text,"");}
    private static void send(Messenger to,int id,String type,String text,String finish){if(to==null)return;try{Message m=Message.obtain(null,REPLY,id,0);Bundle b=new Bundle();b.putString("type",type);b.putString("text",text);b.putString("finish",finish);m.setData(b);to.send(m);}catch(RemoteException ignored){}}
    @Override public IBinder onBind(Intent intent){return messenger.getBinder();}
    @Override public void onDestroy(){engine.cancel();worker.execute(engine::close);worker.shutdown();super.onDestroy();}
}
