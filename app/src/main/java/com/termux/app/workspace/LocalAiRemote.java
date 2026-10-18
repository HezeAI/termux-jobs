package com.termux.app.workspace;

import android.content.*;
import android.os.*;
import org.json.*;
import java.util.UUID;
import java.util.concurrent.*;

/** Blocking methods are called only by LocalAiUi's worker. Main looper delivers Binder replies. */
final class LocalAiRemote implements AutoCloseable {
    private final Context context;
    private final String client=UUID.randomUUID().toString();
    private volatile Messenger service;
    private final CountDownLatch connected=new CountDownLatch(1);
    private volatile CountDownLatch pending;
    private volatile String failure="",result="";
    private volatile LocalAiEngine.Output output;
    private volatile int request;
    private boolean bound,detached;
    volatile String benchmarkToken="";volatile boolean benchmarkCancelled;volatile String cacheScope="";String lora="",cpuMode="auto",location="android",protocol="json",compute="cpu";
    String nativeSchemas="[]";
    volatile boolean contextCompacted;volatile String contextState="";
    volatile String backend="",finishReason="unknown";
    private final Messenger callback=new Messenger(new Handler(Looper.getMainLooper()){
        public void handleMessage(Message m){if(m.what!=LocalAiService.REPLY||m.arg1!=request)return;String type=m.getData().getString("type",""),value=m.getData().getString("text","");
            if(type.equals("performance")){LocalAiEngine.Output o=output;if(o!=null)o.performance(value);return;}
            if(type.equals("stage")){LocalAiEngine.Output o=output;if(o!=null)o.stage(value);return;}
            if(type.equals("metrics")){try{JSONObject v=new JSONObject(value);LocalAiEngine.Output o=output;if(o!=null)o.metrics(v.getInt("input"),v.getInt("output"));}catch(Exception ignored){}return;}
            if(type.equals("context")){if(!value.equals("service_reduced")){contextCompacted=true;contextState=value;}LocalAiEngine.Output o=output;if(o!=null)o.context(value);return;}
            if(type.equals("text")){LocalAiEngine.Output o=output;if(o!=null)o.text(value);return;}
            if(type.equals("error"))failure=value;else result=value;if(type.equals("done"))finishReason=m.getData().getString("finish","unknown");
            CountDownLatch latch=pending;if(latch!=null)latch.countDown();
        }
    });
    private final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder binder){if(binder==null){onNullBinding(name);return;}service=new Messenger(binder);connected.countDown();}
        public void onServiceDisconnected(ComponentName name){service=null;backend="";failure="Local inference process exited. Other terminals are not stopped. Reopen the local session to retry.";CountDownLatch latch=pending;if(latch!=null)latch.countDown();}
        public void onNullBinding(ComponentName name){failure="Local inference service unavailable";connected.countDown();}
        public void onBindingDied(ComponentName name){onServiceDisconnected(name);connected.countDown();}
    };
    LocalAiRemote(Context c){context=c.getApplicationContext();bound=context.bindService(new Intent(context,LocalAiService.class),connection,Context.BIND_AUTO_CREATE);if(!bound)connected.countDown();}
    private synchronized String call(int operation,Bundle data) throws Exception {
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Inference on UI thread is forbidden");
        if(!connected.await(20,TimeUnit.SECONDS)||service==null)throw new IllegalStateException(failure.isEmpty()?"Cannot connect local inference process":failure);
        data.putString("client",client);failure="";result="";CountDownLatch latch=new CountDownLatch(1);pending=latch;int id=++request;
        Message m=Message.obtain(null,operation,id,0);m.replyTo=callback;m.setData(data);
        try{service.send(m);if(!latch.await(20,TimeUnit.MINUTES)){if(operation==LocalAiService.BENCHMARK)cancelBenchmark();else cancel();throw new TimeoutException("Local inference timed out");}if(!failure.isEmpty())throw new IllegalStateException(failure);return result;}
        finally{pending=null;output=null;}
    }
    void load(Context c,JSONObject model,java.io.File file,String acceleration,int threads,int contextSize) throws Exception {Bundle b=new Bundle();b.putString("model",model.getString("id"));b.putString("backend",acceleration);b.putString("cpu",cpuMode);LocalAiLora.id(lora);b.putString("lora",lora);LocalAiEngine.validateCompute(compute);b.putString("compute",compute);LocalAiQwenProtocol.validate(protocol);b.putString("protocol",protocol);LocalAiProcess.validate(location);b.putString("location",location);b.putInt("context",contextSize);LocalAiCpu.validate(threads);b.putInt("threads",threads);backend=call(LocalAiService.LOAD,b);}
    boolean probeThinking,probeGreedy;
    void generate(JSONArray history,int tokens,LocalAiEngine.Output listener) throws Exception {generate(history,tokens,"You are a helpful assistant. No tools are available.",listener);}
    void generate(JSONArray history,int tokens,String system,LocalAiEngine.Output listener) throws Exception {generate(history,tokens,system,"","",listener);}
    void generate(JSONArray history,int tokens,String system,String progress,String correction,LocalAiEngine.Output listener) throws Exception {
        contextCompacted=false;contextState="";
        history=LocalAiContext.fit(history,system,400000,0,(items,instructions)->{int n=instructions.length()*3;for(int i=0;i<items.length();i++)n+=items.getJSONObject(i).toString().length()*3;return n;},state->{contextCompacted=true;contextState=state;listener.context(state);});
        JSONArray bounded=new JSONArray();int start=0,chars=0;
        int imageBudget=3;for(int i=history.length()-1;i>=start;i--){JSONObject source=history.getJSONObject(i),copy=new JSONObject(source.toString());JSONArray media=copy.optJSONArray("media");if(media!=null){JSONArray keep=new JSONArray();for(int n=0;n<media.length()&&imageBudget>0;n++,imageBudget--)keep.put(media.getJSONObject(n));if(keep.length()==0){copy.remove("media");copy.put("text",copy.getString("text")+"\n[Older media omitted; only the latest three images/frames are attached.]");}else copy.put("media",keep);}bounded.put(copy);}JSONArray ordered=new JSONArray();for(int i=bounded.length()-1;i>=0;i--)ordered.put(bounded.getJSONObject(i));bounded=ordered;
        finishReason="unknown";output=listener;Bundle b=new Bundle();b.putString("nativeSchemas",nativeSchemas);b.putBoolean("probeThinking",probeThinking);b.putBoolean("probeGreedy",probeGreedy);b.putString("history",bounded.toString());b.putString("system",system);b.putString("turnProgress",progress);b.putString("turnCorrection",correction);b.putString("scope",cacheScope);b.putInt("tokens",tokens);String answer=call(LocalAiService.GENERATE,b);listener.text(answer);
    }
    void prepareBenchmark(){benchmarkCancelled=false;benchmarkToken=UUID.randomUUID().toString();}
    void benchmark(int mode,LocalAiEngine.Output listener)throws Exception{if(benchmarkCancelled)throw new CancellationException();Bundle b=new Bundle();b.putInt("mode",mode);b.putString("benchmark",benchmarkToken);output=listener;call(LocalAiService.BENCHMARK,b);}
    void cancelBenchmark(){benchmarkCancelled=true;Messenger s=service;if(s!=null)try{Message m=Message.obtain(null,LocalAiService.CANCEL_BENCHMARK);Bundle b=new Bundle();b.putString("client",client);b.putString("benchmark",benchmarkToken);m.setData(b);s.send(m);}catch(RemoteException ignored){}}
    void cancel(){Messenger s=service;if(s!=null)try{Message m=Message.obtain(null,LocalAiService.CANCEL);Bundle b=new Bundle();b.putString("client",client);m.setData(b);s.send(m);}catch(RemoteException ignored){}}
    void releaseModel(){try{if(service!=null)call(LocalAiService.CLOSE,new Bundle());}catch(Exception ignored){}backend="";}
    public void close(){cancel();releaseModel();if(bound&&!detached){detached=true;context.unbindService(connection);}}
}
