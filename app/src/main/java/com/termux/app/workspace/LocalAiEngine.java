package com.termux.app.workspace;

import android.content.Context;
import android.os.Build;
import org.json.*;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns only its local inference resources. No shell, network endpoint or tool execution. */
final class LocalAiEngine implements AutoCloseable {
    interface Output {void text(String text);default void context(String state){}default void stage(String stage){}default void metrics(int input,int output){}default void performance(String json){}}
    private static final String PKG="com.google.ai.edge.litertlm.";
    private Object engine,conversation;
    private Context runtimeContext;private JSONObject loadedModel;private File loadedFile;private String loadedRequested;private int loadedThreads,loadedContext;private boolean vision,audio;
    JSONArray nativeSchemas=new JSONArray();
    private volatile LocalAiProcess external;String location="android",protocol="json",compute="cpu";
    private long llama;private boolean fastCpu;String cpuMode="auto";
    private final AtomicBoolean cancel=new AtomicBoolean();
    String cacheScope="";String backend="",finishReason="unknown";
    String system="You are a helpful assistant. Reply in the user\'s language. Be concise.";
    static String soc(){return Build.VERSION.SDK_INT>=31?Build.SOC_MODEL.toUpperCase(Locale.ROOT):"";}
    static boolean npuLibraries(Context c){return LocalAiNpu.available(new File(c.getApplicationInfo().nativeLibraryDir));}
    static void validateBackend(JSONObject model,String requested,Context c) throws Exception {
        boolean npu=LocalAiNpu.model(model);
        if(npu){
            if(!requested.equals("npu")&&!requested.equals("auto"))throw new IllegalArgumentException("NPU model requires its matching accelerator");
            if(!soc().equals("SM8750")||!LocalAiNpu.verifiedModel(model))throw new IllegalArgumentException("NPU model/chip pairing is not verified");
            if(!npuLibraries(c))throw new IllegalStateException("Qualcomm NPU runtime is incomplete; select another model");
        }else if(requested.equals("npu"))throw new IllegalArgumentException("Select the dedicated NPU model");
        if(model.getString("engine").equals("llama")&&!(requested.equals("auto")||requested.equals("cpu")))throw new IllegalArgumentException("This llama.cpp build supports CPU; GPU support is not enabled");
    }
    void load(Context c,JSONObject model,File file,String requested,int threads,int context) throws Exception {
        cancel.set(false);runtimeContext=c.getApplicationContext();loadedModel=model;loadedFile=file;loadedRequested=requested;loadedThreads=threads;loadedContext=context;initialize(c,model,file,requested,threads,context,false,false);
    }
    private void initialize(Context c,JSONObject model,File file,String requested,int threads,int context,boolean images,boolean sound)throws Exception{
        validateBackend(model,requested,c);validateCompute(compute);if(compute.equals("gpu")&&!gpuModel(model))throw new IllegalArgumentException("OpenCL testing supports catalog Qwen3.5 2B only");LocalAiQwenProtocol.validate(protocol);if(protocol.equals(LocalAiLiteRt.NATIVE)&&!LocalAiLiteRt.supported(model))throw new IllegalArgumentException("Native LiteRT format requires a LiteRT model");if(protocol.equals("qwen-native")&&!LocalAiQwenProtocol.supported(model.getString("id"),model.getString("engine")))throw new IllegalArgumentException("Native Qwen format currently supports catalog Qwen3.5 2B llama only");LocalAiProcess.validate(location);if(!model.getString("engine").equals("llama")&&!location.equals("android"))throw new IllegalArgumentException("External locations support llama.cpp only");vision=false;audio=false;
        if(model.getString("engine").equals("llama")){
            System.loadLibrary("jobs_llama");
            if(compute.equals("gpu")){
                if(!soc().equals("SM8750")||!nativeFastCpuSupported())throw new IllegalStateException("OpenCL testing requires Snapdragon 8 Elite SM8750 with supported ARM instructions; select CPU");
                external=new LocalAiProcess(c,"termux",false,file,true);if(cancel.get()){external.close();external=null;throw new CancellationException();}external.load(file,"termux",6,context);backend="GPU / OpenCL (testing) / batch=1024/512 / location=Android runner / CPU assistance=6";return;
            }
            validateCpuMode(cpuMode);fastCpu=false;String variant="compatible";
            if(cpuMode.equals("auto")&&nativeFastCpuSupported())try{System.loadLibrary("jobs_llama_fast");fastCpu=true;variant="dotprod+fp16+i8mm";}catch(UnsatisfiedLinkError error){variant="compatible (optimized library unavailable)";}
            if(!location.equals("android")){external=new LocalAiProcess(c,location,fastCpu,file);if(cancel.get()){external.close();external=null;throw new CancellationException();}external.load(file,location,threads,context);backend="CPU / "+variant+" / threads="+threads+" / location="+location;return;}
            llama=fastCpu?FastCpu.nativeLoad(file.getAbsolutePath(),threads,context):nativeLoad(file.getAbsolutePath(),threads,context);if(llama==0)throw new IllegalStateException("Cannot load GGUF architecture or model");backend="CPU / "+variant+" / threads="+Math.max(1,Math.min(8,threads));return;
        }
        List<String> attempts=LocalAiNpu.model(model)?Collections.singletonList("npu"):requested.equals("auto")?Arrays.asList("gpu","cpu"):Collections.singletonList(requested);
        if(LocalAiNpu.model(model))LocalAiNpu.prepare(c);
        Throwable failure=null;
        for(String target:attempts){
            Object candidate=null;
            try{
                Object device=target.equals("gpu")?Class.forName(PKG+"Backend$GPU").getConstructor().newInstance():target.equals("npu")?Class.forName(PKG+"Backend$NPU").getConstructor(String.class).newInstance(c.getApplicationInfo().nativeLibraryDir):Class.forName(PKG+"Backend$CPU").getConstructor(Integer.class,Integer.class).newInstance(threads,null);
                Class<?> configType=Class.forName(PKG+"EngineConfig");
                File cache=new File(c.getCacheDir(),"local-ai/"+LocalAiFiles.safeId(model.getString("id"))+"/"+target);cache.mkdirs();
                Object config=configType.getConstructor(String.class,Class.forName(PKG+"Backend"),Class.forName(PKG+"Backend"),Class.forName(PKG+"Backend"),Integer.class,Integer.class,String.class).newInstance(file.getAbsolutePath(),device,images?Class.forName(PKG+"Backend$GPU").getConstructor().newInstance():null,sound?Class.forName(PKG+"Backend$CPU").getConstructor(Integer.class,Integer.class).newInstance(threads,null):null,context,images?3:null,cache.getAbsolutePath());
                candidate=Class.forName(PKG+"Engine").getConstructor(configType).newInstance(config);
                invoke(candidate,"initialize");engine=candidate;backend=target.toUpperCase(Locale.ROOT);vision=images;audio=sound;return;
            }catch(Throwable t){failure=t;if(candidate!=null)try{invoke(candidate,"close");}catch(Exception ignored){}if(cancel.get())throw new CancellationException();}
        }
        throw new Exception("LiteRT-LM initialization failed: "+message(failure),failure);
    }
    private void ensureMedia(boolean images,boolean sound)throws Exception{
        if((!images||vision)&&(!sound||audio))return;
        if(images&&!LocalAiMedia.supported(loadedModel)||sound&&!LocalAiMedia.audioSupported(loadedModel))throw new IllegalStateException("UNSUPPORTED_MEDIA: selected model has no verified media input");
        if(cancel.get())throw new CancellationException();Object old=engine;engine=null;if(old!=null)invoke(old,"close");
        boolean wantImages=images||vision,wantSound=sound||audio;
        try{initialize(runtimeContext,loadedModel,loadedFile,loadedRequested,loadedThreads,loadedContext,wantImages,wantSound);}catch(Exception error){if(cancel.get())throw new CancellationException();initialize(runtimeContext,loadedModel,loadedFile,loadedRequested,loadedThreads,loadedContext,false,false);throw new IllegalStateException("MEDIA_UNAVAILABLE: media backend failed; text engine restored. "+message(error),error);}
    }
    private Object contents(JSONObject message)throws Exception{
        Object companion=Class.forName(PKG+"Contents").getField("Companion").get(null);List<Object> parts=new ArrayList<>();parts.add(Class.forName(PKG+"Content$Text").getConstructor(String.class).newInstance(message.getString("text")));JSONArray images=message.optJSONArray("media");if(images!=null)for(int i=0;i<images.length();i++){JSONObject item=images.getJSONObject(i);boolean sound=item.optString("kind").equals("audio");File path=sound?LocalAiMedia.checkedAudio(runtimeContext,item.getString("path")):LocalAiMedia.checkedImage(runtimeContext,item.getString("path"));parts.add(Class.forName(PKG+(sound?"Content$AudioFile":"Content$ImageFile")).getConstructor(String.class).newInstance(path.getPath()));}return companion.getClass().getMethod("of",List.class).invoke(companion,parts);
    }
    static boolean contextOverflow(String error){String text=error.toLowerCase(Locale.ROOT);return !text.contains("context_too_small")&&(text.contains("context")&&text.matches("(?s).*(exceed|too long|overflow|capacity|token limit).*" )||text.contains("input")&&text.contains("too long"));}
    void generate(JSONArray messages,int maxTokens,Output output)throws Exception{
        try{generateOnce(messages,maxTokens,output);}catch(Exception error){
            if(isLlama()||cancel.get()||!contextOverflow(message(error)))throw error;
            // LiteRT does not expose its tokenizer here. One bounded retry with more headroom.
            output.context("begin");int estimate=LocalAiContext.estimate(messages,system);int tighter=Math.min((loadedContext>0?loadedContext:4096)*3/4,Math.max(1,estimate*2/3)+Math.min(maxTokens,2048)+64);
            JSONArray shortened=LocalAiContext.fit(messages,system,tighter,Math.min(maxTokens,2048),LocalAiContext::estimate,output::context);
            if(shortened.toString().equals(messages.toString())){output.context("failed");throw error;}
            output.context("done");output.text("");generateOnce(shortened,maxTokens,output);
        }
    }
    private void generateOnce(JSONArray messages,int maxTokens,Output output) throws Exception {
        if(cancel.get())throw new CancellationException();finishReason="unknown";output.stage("context_preparation");
        boolean images=false,sound=false;
        for(int i=0;i<messages.length();i++){JSONArray media=messages.getJSONObject(i).optJSONArray("media");if(media!=null)for(int j=0;j<media.length();j++)if(media.getJSONObject(j).optString("kind").equals("audio"))sound=true;else images=true;}
        if(images||sound){output.stage("media_preparation");if(isLlama())throw new IllegalStateException("UNSUPPORTED_VISION: llama.cpp projector/audio support is not bundled");ensureMedia(images,sound);}
        boolean nativeTools=!isLlama()&&protocol.equals(LocalAiLiteRt.NATIVE)&&LocalAiLiteRt.supported(loadedModel);
        String budgetSystem=system+(nativeTools?"\n"+nativeSchemas.toString():"");
        int capacity=loadedContext>0?loadedContext:4096;if(isLlama())capacity=Math.max(512,Math.min(8192,capacity));
        int[] inputTokens={-1};LocalAiContext.Counter counter=isLlama()?(items,instructions)->{inputTokens[0]=countPrompt(items,instructions);return inputTokens[0];}:LocalAiContext::estimate;
        messages=LocalAiContext.fit(messages,budgetSystem,capacity,Math.min(maxTokens,2048),counter,output::context);
        LocalAiContext.Counter budgetCounter=isLlama()?this::countPrompt:LocalAiContext::estimate;
        output.performance(LocalAiInputBudget.safeMeasure(messages,budgetSystem,budgetCounter,isLlama()).toString());
        JSONArray rendered=renderMessages(messages);
        if(external!=null){if(cancel.get())throw new CancellationException();output.stage("prefill");JSONObject reply=external.request(LocalAiProcess.prompt(rendered,system).put("op","generate").put("scope",cacheScope).put("maxTokens",maxTokens),output).getJSONObject("value");finishReason=reply.getString("finish");output.metrics(reply.getInt("input"),reply.getInt("generated"));output.stage("postprocessing");return;}
        if(llama!=0){
            String[] roles=roles(rendered),texts=texts(rendered);
            output.stage("prefill");int outcome=fastCpu?FastCpu.nativeGenerate(llama,roles,texts,maxTokens,output,cancel,system,cacheScope):nativeGenerate(llama,roles,texts,maxTokens,output,cancel,system,cacheScope);finishReason=(outcome&1)==1?"token_limit":"eos";output.metrics(inputTokens[0],outcome>>>1);output.stage("postprocessing");return;
        }
        if(cancel.get())throw new CancellationException();if(engine==null)throw new IllegalStateException("Model is stopped");
        Object companion=Class.forName(PKG+"Contents").getField("Companion").get(null);
        Object system=companion.getClass().getMethod("of",String.class).invoke(companion,this.system);
        Object msgCompanion=Class.forName(PKG+"Message").getField("Companion").get(null);List<Object> history=new ArrayList<>();
        List<Object> runtimeMessages=new ArrayList<>();
        for(int i=0;i<messages.length();i++){
            JSONObject m=messages.getJSONObject(i);JSONObject call=m.optJSONObject("toolRequest");
            if(nativeTools&&call!=null){runtimeMessages.add(LocalAiLiteRt.callMessage(call));continue;}
            if(nativeTools&&m.has("toolResult")&&!m.optString("toolResult").equals("file_selection")){
                runtimeMessages.add(LocalAiLiteRt.response(m.getString("toolResult"),m.getString("text")));
                if(m.has("media"))runtimeMessages.add(LocalAiLiteRt.message("user",contents(m)));
            }else runtimeMessages.add(LocalAiLiteRt.message(m.getString("role").equals("assistant")?"model":"user",contents(m)));
        }
        if(runtimeMessages.isEmpty())throw new IllegalArgumentException("Empty conversation");
        Object lastMessage=runtimeMessages.remove(runtimeMessages.size()-1);history.addAll(runtimeMessages);
        Class<?> configType=Class.forName(PKG+"ConversationConfig");
        Object sampler=Class.forName(PKG+"SamplerConfig").getConstructor(int.class,double.class,double.class,int.class).newInstance(40,.9,.65,0);
        Object thinking=Class.forName(PKG+"ThinkingConfig").getConstructor(boolean.class,int.class).newInstance(false,0);
        Object cfg=configType.getConstructor(Class.forName(PKG+"Contents"),List.class,List.class,Class.forName(PKG+"SamplerConfig"),boolean.class,List.class,java.util.Map.class,Class.forName(PKG+"LoraConfig"),boolean.class,Integer.class,Class.forName(PKG+"ThinkingConfig"),boolean.class)
            .newInstance(system,history,nativeTools?LocalAiLiteRt.providers(nativeSchemas):Collections.emptyList(),sampler,false,Collections.emptyList(),Collections.emptyMap(),null,true,Math.min(maxTokens,2048),thinking,false);
        output.stage("prefill");conversation=engine.getClass().getMethod("createConversation",configType).invoke(engine,cfg);
        CountDownLatch done=new CountDownLatch(1);Throwable[] error={null};StringBuilder response=new StringBuilder();String[] nativeCall={""};
        Class<?> callback=Class.forName(PKG+"MessageCallback");
        Object listener=Proxy.newProxyInstance(callback.getClassLoader(),new Class<?>[]{callback},(proxy,method,args)->{
            switch(method.getName()){
                case "onMessage": if(!cancel.get()){try{String call=nativeTools?LocalAiLiteRt.call(args[0]):"";synchronized(response){if(!call.isEmpty()){if(!nativeCall[0].isEmpty()&&!nativeCall[0].equals(call))throw new IllegalArgumentException("Multiple structured calls in one response are not supported; no calls executed");nativeCall[0]=call;}String part=invoke(args[0],"getContents").toString();response.append(part);if(nativeCall[0].isEmpty())output.text(response.toString());}}catch(Throwable failure){error[0]=failure;}}return null;
                case "onError":error[0]=(Throwable)args[0];done.countDown();return null;
                case "onDone":done.countDown();return null;
                case "toString":return "LocalAiCallback";
                case "hashCode":return System.identityHashCode(proxy);
                case "equals":return proxy==args[0];default:return null;
            }
        });
        try{
            conversation.getClass().getMethod("sendMessageAsync",Class.forName(PKG+"Message"),callback).invoke(conversation,lastMessage,listener);
            if(!done.await(15,TimeUnit.MINUTES)){cancel();done.await();throw new TimeoutException("Local response timed out");}
            finishReason=nativeCall[0].isEmpty()?"sdk_done":"tool";if(cancel.get())throw new CancellationException();if(error[0]!=null)throw new Exception(message(error[0]),error[0]);if(!nativeCall[0].isEmpty())output.text(nativeCall[0]);output.metrics(-1,-1);output.stage("postprocessing");
        }finally{Object conv=conversation;conversation=null;if(conv!=null)invoke(conv,"close");}
    }
    private boolean isLlama(){return llama!=0||external!=null;}
    JSONArray renderMessages(JSONArray items)throws Exception{return protocol.equals("qwen-native")?LocalAiQwenProtocol.render(items):items;}
    private int countPrompt(JSONArray items,String instructions)throws Exception{items=renderMessages(items);if(cancel.get())throw new CancellationException();return external!=null?external.request(LocalAiProcess.prompt(items,instructions).put("op","count"),null).getInt("value"):fastCpu?FastCpu.nativePromptTokens(llama,roles(items),texts(items),instructions):nativePromptTokens(llama,roles(items),texts(items),instructions);}
    void resetBenchmarkCancel(){cancel.set(false);}
    void benchmark(int mode,Output output)throws Exception{if(compute.equals("gpu"))throw new IllegalStateException("CPU benchmark unavailable in GPU mode; select CPU");if(!isLlama())throw new IllegalStateException("Benchmark requires a loaded llama.cpp model");if(mode<0||mode>2)throw new IllegalArgumentException("Invalid benchmark mode");if(cancel.get())throw new CancellationException();try{if(external!=null){external.request(new JSONObject().put("op","benchmark").put("mode",mode),output);}else if(fastCpu)FastCpu.nativeBenchmark(llama,mode,loadedThreads,output,cancel);else nativeBenchmark(llama,mode,loadedThreads,output,cancel);}finally{cancel.set(false);}}
    void cancel(){cancel.set(true);LocalAiProcess process=external;if(process!=null)process.cancel();Object conv=conversation;if(conv!=null)try{invoke(conv,"cancelProcess");}catch(Exception ignored){}}
    public void close(){cancel();LocalAiProcess process=external;external=null;if(process!=null)process.close();if(llama!=0){if(fastCpu)FastCpu.nativeFree(llama);else nativeFree(llama);llama=0;}fastCpu=false;if(engine!=null){try{invoke(engine,"close");}catch(Exception ignored){}engine=null;}vision=false;audio=false;backend="";}
    static Object invoke(Object target,String name) throws Exception{return target.getClass().getMethod(name).invoke(target);}
    static String message(Throwable t){if(t==null)return "Unknown error";while(t instanceof InvocationTargetException&&t.getCause()!=null)t=t.getCause();String s=t.getMessage();return t.getClass().getSimpleName()+(s==null?"":": "+s.substring(0,Math.min(s.length(),500)));}
    static String[] roles(JSONArray messages)throws JSONException{String[] out=new String[messages.length()];for(int i=0;i<out.length;i++)out[i]=messages.getJSONObject(i).getString("role");return out;}
    static String[] texts(JSONArray messages)throws JSONException{String[] out=new String[messages.length()];for(int i=0;i<out.length;i++)out[i]=messages.getJSONObject(i).getString("text");return out;}
    static void validateCompute(String value){if(!"cpu".equals(value)&&!"gpu".equals(value))throw new IllegalArgumentException("Invalid compute mode");}
    static boolean gpuModel(JSONObject model){return "qwen35-2b".equals(model.optString("id"))&&"llama".equals(model.optString("engine"));}
    static void validateCpuMode(String mode){if(!"auto".equals(mode)&&!"compatible".equals(mode))throw new IllegalArgumentException("Invalid CPU engine mode");}
    private static native boolean nativeFastCpuSupported();
    private static final class FastCpu {
    private static native int nativePromptTokens(long ptr,String[] roles,String[] texts,String system);
    private static native long nativeLoad(String path,int threads,int context);
    private static native int nativeGenerate(long handle,String[] roles,String[] texts,int maxTokens,Output output,AtomicBoolean cancel,String system,String scope);
    private static native void nativeBenchmark(long handle,int mode,int threads,Output output,AtomicBoolean cancel);
    private static native void nativeFree(long handle);
    }
    private static native int nativePromptTokens(long ptr,String[] roles,String[] texts,String system);
    private static native long nativeLoad(String path,int threads,int context);
    private static native int nativeGenerate(long handle,String[] roles,String[] texts,int maxTokens,Output output,AtomicBoolean cancel,String system,String scope);
    private static native void nativeBenchmark(long handle,int mode,int threads,Output output,AtomicBoolean cancel);
    private static native void nativeFree(long handle);
}
