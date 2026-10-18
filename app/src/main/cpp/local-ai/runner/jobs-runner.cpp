#include "llama.h"
#ifdef JOBS_OPENCL_RUNNER
#include "ggml-backend.h"
static constexpr bool jobs_gpu=true;
#else
static constexpr bool jobs_gpu=false;
#endif
#include "jobs-token-budget.h"
#include "jobs-prefix-cache.h"
#include "jobs-benchmark.h"
#include "nlohmann/json.hpp"
#include <atomic>
#include <condition_variable>
#include <iostream>
#include <memory>
#include <mutex>
#include <queue>
#include <set>
#include <thread>
#include <chrono>
#include <sys/prctl.h>
#include <unistd.h>
#include <signal.h>
using json=nlohmann::ordered_json;
using Clock=std::chrono::steady_clock;
static std::mutex mu,outmu;static std::condition_variable cv;static std::queue<json> pending;static std::set<int> cancelled;
static std::atomic<bool> cancel(false);static bool quitting=false;static int active=-1;
static void emit(int id,const char*type,const json&value){std::lock_guard<std::mutex>lock(outmu);std::cout<<json{{"id",id},{"type",type},{"value",value}}.dump(-1,' ',false,json::error_handler_t::replace)<<std::endl;}
static void check(){if(cancel.load())throw std::runtime_error("CANCELLED");}
static long long ms(Clock::time_point start){return std::chrono::duration_cast<std::chrono::milliseconds>(Clock::now()-start).count();}
struct Model {
 std::unique_ptr<llama_model,decltype(&llama_model_free)> model{nullptr,llama_model_free};
 std::unique_ptr<llama_adapter_lora,decltype(&llama_adapter_lora_free)> adapter{nullptr,llama_adapter_lora_free};
 std::unique_ptr<llama_context,decltype(&llama_free)> ctx{nullptr,llama_free};jobs_prefix_cache cache;int threads=4;
 std::vector<llama_token> tokens(const json&r){std::vector<std::string>roles{"system"},texts{r.value("system",std::string())};auto rs=r.at("roles"),ts=r.at("texts");if(rs.size()!=ts.size()||rs.size()>4096)throw std::runtime_error("INVALID_MESSAGES");for(size_t i=0;i<rs.size();i++){roles.push_back(rs.at(i).get<std::string>());texts.push_back(ts.at(i).get<std::string>());}return jobs_prompt_tokens(model.get(),roles,texts);}
 void load(const json&r){ctx.reset();adapter.reset();model.reset();cache.reset();check();llama_model_params mp=llama_model_default_params();mp.n_gpu_layers=jobs_gpu?99:0;
#ifdef JOBS_OPENCL_RUNNER
 bool found=false;ggml_backend_load_all();for(size_t i=0;i<ggml_backend_dev_count();i++)if(ggml_backend_dev_type(ggml_backend_dev_get(i))==GGML_BACKEND_DEVICE_TYPE_GPU)found=true;if(!found)throw std::runtime_error("GPU_UNAVAILABLE");
#endif
 mp.progress_callback=[](float,void*){return !cancel.load();};model.reset(llama_model_load_from_file(r.at("path").get<std::string>().c_str(),mp));if(!model)throw std::runtime_error("MODEL_LOAD_FAILED");check();threads=jobs_gpu?6:std::clamp(r.value("threads",4),1,8);auto cp=llama_context_default_params();cp.n_ctx=std::clamp(r.value("context",4096),512,8192);cp.n_batch=jobs_gpu?1024:256;cp.n_ubatch=jobs_gpu?512:128;cp.n_threads=threads;cp.n_threads_batch=threads;ctx.reset(llama_init_from_model(model.get(),cp));if(!ctx)throw std::runtime_error("CONTEXT_LOAD_FAILED");check();}
 void set_adapter(const json&r){check();if(adapter||jobs_gpu)throw std::runtime_error("INVALID_ADAPTER_STATE");adapter.reset(llama_adapter_lora_init(model.get(),r.at("path").get<std::string>().c_str()));if(!adapter)throw std::runtime_error("ADAPTER_LOAD_FAILED");auto a=adapter.get();float scale=1.0f;if(llama_set_adapters_lora(ctx.get(),&a,1,&scale)){adapter.reset();throw std::runtime_error("ADAPTER_APPLY_FAILED");}cache.invalidate(ctx.get());}
 void generate(int id,const json&r){check();auto start=Clock::now();auto toks=tokens(r);int count=toks.size(),limit=std::clamp(r.value("maxTokens",768),1,2048);if(count+limit+1>(int)llama_n_ctx(ctx.get()))throw std::runtime_error("CONTEXT_TOO_SMALL");std::string system=r.value("system",std::string());auto cstart=Clock::now();size_t reuse=cache.begin(ctx.get(),toks,r.value("scope",std::string()),system);long long cachems=ms(cstart),snapshotms=0,finalpre=-1;int done=reuse;
 auto a=jobs_prompt_tokens(model.get(),{"system","user"},{system,"A-cache-boundary"}),b=jobs_prompt_tokens(model.get(),{"system","user"},{system,"B-cache-boundary"});size_t base=std::min({jobs_prefix_cache::common(a,b),jobs_prefix_cache::common(a,toks),toks.size()-1}),tail=std::max(base,toks.size()>64?toks.size()-64:size_t(0));int feedback=r.value("feedbackStart",-1);if(feedback>=0&&feedback<(int)r.at("roles").size()){auto roles=r.at("roles").get<std::vector<std::string>>(),texts=r.at("texts").get<std::vector<std::string>>();roles.insert(roles.begin(),"system");texts.insert(texts.begin(),system);size_t point=jobs_feedback_checkpoint(model.get(),roles,texts,feedback+1,toks);if(point>base)tail=point;}long long setup=std::max(0LL,ms(start)-cachems);auto pre=Clock::now();
 auto update=[&](bool complete,long long first){emit(id,"performance",json{{"processed",done},{"total",count},{"cached",reuse},{"prefillMs",finalpre>=0?finalpre:std::max(0LL,ms(pre)-snapshotms)},{"cacheMs",cachems+snapshotms},{"setupMs",setup},{"firstTokenMs",first},{"complete",complete},{"cacheStatus",cache.status}});};update(false,-1);
 while(done<count){check();int end=std::min(count,done+(jobs_gpu?1024:256));for(size_t at:{base,tail})if(at>(size_t)done&&at<(size_t)end)end=at;int n=end-done;if(llama_decode(ctx.get(),llama_batch_get_one(toks.data()+done,n)))throw std::runtime_error("PREFILL_FAILED");cache.append(toks.data()+done,n);done=end;if((size_t)done==base||(size_t)done==tail){auto t=Clock::now();if((size_t)done==base)cache.capture(ctx.get(),cache.base);if((size_t)done==tail&&tail!=base)cache.capture(ctx.get(),cache.tail);snapshotms+=ms(t);}update(done==count,-1);}
 finalpre=std::max(0LL,ms(pre)-snapshotms);auto endpre=Clock::now();emit(id,"stage","first_token");std::unique_ptr<llama_sampler,decltype(&llama_sampler_free)> sampler(llama_sampler_chain_init(llama_sampler_chain_default_params()),llama_sampler_free);llama_sampler_chain_add(sampler.get(),llama_sampler_init_top_k(40));llama_sampler_chain_add(sampler.get(),llama_sampler_init_top_p(.9f,1));llama_sampler_chain_add(sampler.get(),llama_sampler_init_temp(.65f));llama_sampler_chain_add(sampler.get(),llama_sampler_init_dist(LLAMA_DEFAULT_SEED));auto vocab=llama_model_get_vocab(model.get());std::string response;int generated=0;bool limited=true;
 for(int i=0;i<limit;i++){check();auto token=llama_sampler_sample(sampler.get(),ctx.get(),-1);if(llama_vocab_is_eog(vocab,token)){limited=false;break;}char piece[512];int n=llama_token_to_piece(vocab,token,piece,sizeof(piece),0,false);if(n<0)throw std::runtime_error("INVALID_TOKEN");response.append(piece,n);generated++;if(generated==1)update(true,ms(endpre));emit(id,"text",response);if(llama_decode(ctx.get(),llama_batch_get_one(&token,1)))throw std::runtime_error("GENERATION_FAILED");cache.append(&token,1);}
 emit(id,"done",json{{"input",count},{"generated",generated},{"finish",limited?"token_limit":"eos"}});
 }
};
int main(){auto parent=getppid();if(prctl(PR_SET_PDEATHSIG,SIGKILL)!=0||getppid()!=parent)return 2;llama_log_set([](ggml_log_level,const char*,void*){},nullptr);llama_backend_init();std::thread reader([]{std::string line;while(std::getline(std::cin,line)){if(line.size()>2*1024*1024)break;try{auto r=json::parse(line);std::string op=r.value("op",std::string());int id=r.value("id",0);std::lock_guard<std::mutex>lock(mu);if(op=="quit"){quitting=true;cancel=true;cv.notify_all();return;}if(op=="cancel"){cancelled.insert(id);while(cancelled.size()>64)cancelled.erase(cancelled.begin());if(active==id)cancel=true;}else if(pending.size()<8){pending.push(r);cv.notify_all();}}catch(...){}}std::lock_guard<std::mutex>lock(mu);quitting=true;cancel=true;cv.notify_all();});
 Model m;while(true){json r;int id;{std::unique_lock<std::mutex>lock(mu);cv.wait(lock,[]{return quitting||!pending.empty();});if(quitting)break;r=pending.front();pending.pop();id=r.value("id",0);active=id;cancel=cancelled.erase(id)>0;}
 try{std::string op=r.value("op",std::string());check();if(op=="load"){m.load(r);emit(id,"done",json::object());}else{if(!m.ctx)throw std::runtime_error("MODEL_NOT_LOADED");if(op=="adapter"){m.set_adapter(r);emit(id,"done",json::object());}else if(op=="count")emit(id,"done",m.tokens(r).size());else if(op=="generate")m.generate(id,r);else if(op=="benchmark"){if(jobs_gpu)throw std::runtime_error("GPU_BENCHMARK_UNAVAILABLE");jobs_benchmark(m.model.get(),llama_n_ctx(m.ctx.get()),r.at("mode").get<int>(),m.threads,[]{return cancel.load();},[&](const std::string&s){emit(id,"performance",json::parse(s));});emit(id,"done",json::object());}else throw std::runtime_error("INVALID_OPERATION");}}
 catch(const std::exception&e){if(m.ctx&&r.value("op",std::string())=="generate")m.cache.invalidate(m.ctx.get());std::string reason=cancel.load()?"CANCELLED":"RUNNER_OPERATION_FAILED";emit(id,"error",reason);}
 {std::lock_guard<std::mutex>lock(mu);active=-1;}}
 reader.join();return 0;}
