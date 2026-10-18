#include <jni.h>
#include "llama.h"
#include "jobs-chat-policy.h"
#include "jobs-token-budget.h"
#include "jobs-prefix-cache.h"
#include "jobs-benchmark.h"
#include <chrono>
#include <sys/auxv.h>
#include "jobs-cpu-policy.h"
#ifdef JOBS_FAST_CPU
#define JOBS_JNI(name) Java_com_termux_app_workspace_LocalAiEngine_00024FastCpu_##name
#else
#define JOBS_JNI(name) Java_com_termux_app_workspace_LocalAiEngine_##name
extern "C" JNIEXPORT jboolean JNICALL Java_com_termux_app_workspace_LocalAiEngine_nativeFastCpuSupported(JNIEnv*,jclass){
 const auto h=getauxval(AT_HWCAP),h2=getauxval(AT_HWCAP2);
 return jobs_fast_cpu_supported(h,h2);
}
#endif
#include <algorithm>
#include <string>
#include <vector>
#include <mutex>
#include <stdexcept>

struct JobsModel { llama_model * model; llama_context * ctx; jobs_prefix_cache cache; llama_adapter_lora * adapter=nullptr; };
static void fail(JNIEnv *e,const char *message){e->ThrowNew(e->FindClass("java/lang/IllegalStateException"),message);}
static std::string utf8(JNIEnv *e,jstring s){
    jclass cls=e->FindClass("java/lang/String");jmethodID get=e->GetMethodID(cls,"getBytes","(Ljava/lang/String;)[B");jstring charset=e->NewStringUTF("UTF-8");
    auto a=(jbyteArray)e->CallObjectMethod(s,get,charset);std::string out(e->GetArrayLength(a),'\0');e->GetByteArrayRegion(a,0,out.size(),(jbyte*)out.data());e->DeleteLocalRef(a);e->DeleteLocalRef(charset);e->DeleteLocalRef(cls);return out;
}
static jstring string(JNIEnv *e,const std::string &s){
    jbyteArray a=e->NewByteArray(s.size());e->SetByteArrayRegion(a,0,s.size(),(const jbyte*)s.data());jclass cls=e->FindClass("java/lang/String");jstring charset=e->NewStringUTF("UTF-8");
    auto out=(jstring)e->NewObject(cls,e->GetMethodID(cls,"<init>","([BLjava/lang/String;)V"),a,charset);e->DeleteLocalRef(a);e->DeleteLocalRef(charset);e->DeleteLocalRef(cls);return out;
}
extern "C" JNIEXPORT jlong JNICALL JOBS_JNI(nativeLoad)(JNIEnv *e,jclass,jstring path,jint threads,jint context){
    static std::once_flag once;std::call_once(once,[]{llama_backend_init();});
    llama_model_params mp=llama_model_default_params();mp.n_gpu_layers=0;
    auto model=llama_model_load_from_file(utf8(e,path).c_str(),mp);if(!model){fail(e,"GGUF model is invalid or its architecture is unsupported");return 0;}
    llama_context_params cp=llama_context_default_params();cp.n_ctx=std::clamp((int)context,512,8192);cp.n_batch=256;cp.n_ubatch=128;cp.n_threads=std::clamp((int)threads,1,8);cp.n_threads_batch=cp.n_threads;
    auto ctx=llama_init_from_model(model,cp);if(!ctx){llama_model_free(model);fail(e,"Cannot allocate model context");return 0;}
    return (jlong)new JobsModel{model,ctx};
}
extern "C" JNIEXPORT void JNICALL JOBS_JNI(nativeAdapter)(JNIEnv *e,jclass,jlong ptr,jstring path){
 auto h=(JobsModel*)ptr;try{if(!h||h->adapter)throw std::runtime_error("Invalid adapter state");auto adapter=llama_adapter_lora_init(h->model,utf8(e,path).c_str());if(!adapter)throw std::runtime_error("LoRA load/base compatibility failed");float scale=1.0f;if(llama_set_adapters_lora(h->ctx,&adapter,1,&scale)){llama_adapter_lora_free(adapter);throw std::runtime_error("LoRA activation failed");}h->adapter=adapter;h->cache.invalidate(h->ctx);}catch(const std::exception&err){if(!e->ExceptionCheck())fail(e,err.what());}
}
static std::vector<llama_token> prompt_tokens(JNIEnv *e,JobsModel *h,jobjectArray roles,jobjectArray texts,jstring system,int feedback=-1,size_t* checkpoint=nullptr){
        int n=e->GetArrayLength(roles);std::vector<std::string> rs,ts;
        rs.emplace_back("system");ts.emplace_back(utf8(e,system));
        for(int i=0;i<n;i++){auto r=(jstring)e->GetObjectArrayElement(roles,i);auto t=(jstring)e->GetObjectArrayElement(texts,i);rs.push_back(utf8(e,r));ts.push_back(utf8(e,t));e->DeleteLocalRef(r);e->DeleteLocalRef(t);}
        auto tokens=jobs_prompt_tokens(h->model,rs,ts);if(checkpoint)*checkpoint=jobs_feedback_checkpoint(h->model,rs,ts,feedback<0||feedback>=n?-1:feedback+1,tokens);return tokens;
}
extern "C" JNIEXPORT jint JNICALL JOBS_JNI(nativePromptTokens)(JNIEnv *e,jclass,jlong ptr,jobjectArray roles,jobjectArray texts,jstring system){
    try{auto h=(JobsModel*)ptr;if(!h)throw std::runtime_error("Model is stopped");return prompt_tokens(e,h,roles,texts,system).size();}catch(const std::exception &err){if(!e->ExceptionCheck())fail(e,err.what());return 0;}
}
extern "C" JNIEXPORT jint JNICALL JOBS_JNI(nativeGenerate)(JNIEnv *e,jclass,jlong ptr,jobjectArray roles,jobjectArray texts,jint max,jobject output,jobject cancel,jstring system,jstring scope,jint feedback){
    auto native_start=std::chrono::steady_clock::now();auto h=(JobsModel*)ptr;if(!h)return 0;llama_sampler *sampler=nullptr;int finish=0,generated=0;
    try{
        size_t feedback_at=0;auto tokens=prompt_tokens(e,h,roles,texts,system,feedback,&feedback_at);int count=tokens.size();auto vocab=llama_model_get_vocab(h->model);
        if(tokens.empty())throw std::runtime_error("Empty prompt");
        int limit=std::clamp((int)max,1,2048);if(count+limit+1>(int)llama_n_ctx(h->ctx))throw std::runtime_error("CONTEXT_TOO_SMALL: current input exceeds the runtime context after budgeting; saved history is kept.");
        using clock=std::chrono::steady_clock;auto millis=[](clock::time_point t){return std::chrono::duration_cast<std::chrono::milliseconds>(clock::now()-t).count();};
        auto cache_start=clock::now();size_t reused=h->cache.begin(h->ctx,tokens,utf8(e,scope),utf8(e,system));long long cache_ms=millis(cache_start);
        // A system-only probe with two different user bodies gives a stable, token-exact boundary.
        auto a=jobs_prompt_tokens(h->model,{"system","user"},{utf8(e,system),"A-cache-boundary"});auto b=jobs_prompt_tokens(h->model,{"system","user"},{utf8(e,system),"B-cache-boundary"});
        size_t base_at=std::min({jobs_prefix_cache::common(a,b),jobs_prefix_cache::common(a,tokens),tokens.size()-1});size_t tail_at=feedback_at>base_at?feedback_at:std::max(base_at,tokens.size()>64?tokens.size()-64:size_t(0));

        jclass cancelClass=e->GetObjectClass(cancel);jmethodID get=e->GetMethodID(cancelClass,"get","()Z");e->DeleteLocalRef(cancelClass);
        jclass outClass=e->GetObjectClass(output);jmethodID emit=e->GetMethodID(outClass,"text","(Ljava/lang/String;)V");jmethodID progress=e->GetMethodID(outClass,"performance","(Ljava/lang/String;)V");jmethodID stage=e->GetMethodID(outClass,"stage","(Ljava/lang/String;)V");e->DeleteLocalRef(outClass);
        auto prefill_start=clock::now();long long snapshot_ms=0,final_prefill=-1;const long long setup_ms=std::max(0LL,millis(native_start)-cache_ms);int processed=(int)reused;
        auto update=[&](bool done,long long first_ms){std::string json="{\"processed\":"+std::to_string(processed)+",\"total\":"+std::to_string(count)+",\"cached\":"+std::to_string(reused)+",\"prefillMs\":"+std::to_string(final_prefill>=0?final_prefill:std::max(0LL,millis(prefill_start)-snapshot_ms))+",\"setupMs\":"+std::to_string(setup_ms)+",\"cacheMs\":"+std::to_string(cache_ms+snapshot_ms)+",\"firstTokenMs\":"+std::to_string(first_ms)+",\"complete\":"+(done?"true":"false")+",\"cacheStatus\":\""+h->cache.status+"\"}";jstring value=string(e,json);e->CallVoidMethod(output,progress,value);e->DeleteLocalRef(value);if(e->ExceptionCheck())throw std::runtime_error("Progress callback failed");};
        update(false,-1);
        while(processed<count){if(e->CallBooleanMethod(cancel,get))throw std::runtime_error("Cancelled");int end=std::min(count,processed+256);for(size_t boundary:{base_at,tail_at})if(boundary>(size_t)processed&&boundary<(size_t)end)end=boundary;
            int batch=end-processed;if(llama_decode(h->ctx,llama_batch_get_one(tokens.data()+processed,batch))!=0)throw std::runtime_error("Prompt evaluation failed");h->cache.append(tokens.data()+processed,batch);processed=end;
            if((size_t)processed==base_at||(size_t)processed==tail_at){auto now=clock::now();if((size_t)processed==base_at)h->cache.capture(h->ctx,h->cache.base);if((size_t)processed==tail_at&&tail_at!=base_at)h->cache.capture(h->ctx,h->cache.tail);snapshot_ms+=millis(now);}
            update(processed==count,-1);
        }
        auto prefill_end=clock::now();final_prefill=std::max(0LL,millis(prefill_start)-snapshot_ms);jstring next=string(e,"first_token");e->CallVoidMethod(output,stage,next);e->DeleteLocalRef(next);if(e->ExceptionCheck())throw std::runtime_error("Stage callback failed");
        sampler=llama_sampler_chain_init(llama_sampler_chain_default_params());llama_sampler_chain_add(sampler,llama_sampler_init_top_k(40));llama_sampler_chain_add(sampler,llama_sampler_init_top_p(.9f,1));llama_sampler_chain_add(sampler,llama_sampler_init_temp(.65f));llama_sampler_chain_add(sampler,llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        std::string response;
        finish=1;
        for(int i=0;i<limit;i++){
            if(e->CallBooleanMethod(cancel,get))throw std::runtime_error("Cancelled");llama_token token=llama_sampler_sample(sampler,h->ctx,-1);if(llama_vocab_is_eog(vocab,token)){finish=0;break;}
            char piece[512];int len=llama_token_to_piece(vocab,token,piece,sizeof(piece),0,false);if(len<0)throw std::runtime_error("Invalid token piece");response.append(piece,len);generated++;if(generated==1)update(true,millis(prefill_end));
            jstring value=string(e,response);e->CallVoidMethod(output,emit,value);e->DeleteLocalRef(value);if(e->ExceptionCheck())throw std::runtime_error("Output callback failed");
            if(llama_decode(h->ctx,llama_batch_get_one(&token,1))!=0)throw std::runtime_error("Response evaluation failed");h->cache.append(&token,1);
        }
        llama_sampler_free(sampler);
    }catch(const std::exception &err){if(sampler)llama_sampler_free(sampler);h->cache.invalidate(h->ctx);if(!e->ExceptionCheck())fail(e,err.what());}
    return (generated<<1)|finish;
}
extern "C" JNIEXPORT void JNICALL JOBS_JNI(nativeBenchmark)(JNIEnv*e,jclass,jlong ptr,jint mode,jint threads,jobject output,jobject cancel){
 try{auto*h=(JobsModel*)ptr;if(!h)throw std::runtime_error("Model is stopped");jclass oc=e->GetObjectClass(output),cc=e->GetObjectClass(cancel);auto emit=e->GetMethodID(oc,"performance","(Ljava/lang/String;)V"),get=e->GetMethodID(cc,"get","()Z");e->DeleteLocalRef(oc);e->DeleteLocalRef(cc);
 jobs_benchmark(h->model,llama_n_ctx(h->ctx),mode,threads,[&](){return e->CallBooleanMethod(cancel,get)||e->ExceptionCheck();},[&](const std::string&s){jstring value=string(e,s);e->CallVoidMethod(output,emit,value);e->DeleteLocalRef(value);if(e->ExceptionCheck())throw std::runtime_error("Benchmark callback failed");});
 }catch(const std::exception&err){if(!e->ExceptionCheck())fail(e,err.what());}
}
extern "C" JNIEXPORT void JNICALL JOBS_JNI(nativeFree)(JNIEnv*,jclass,jlong ptr){auto h=(JobsModel*)ptr;if(h){llama_free(h->ctx);if(h->adapter)llama_adapter_lora_free(h->adapter);llama_model_free(h->model);delete h;}}
