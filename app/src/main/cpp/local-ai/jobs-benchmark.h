#pragma once
#include "llama.h"
#include <algorithm>
#include <cstdint>
#include <functional>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>
#include <time.h>
struct jobs_benchmark_case {int input,threads,batch,ubatch;};
inline std::vector<jobs_benchmark_case> jobs_benchmark_plan(int mode,int threads,int context){
 if(mode<0||mode>2)throw std::runtime_error("Invalid benchmark mode");
 std::vector<jobs_benchmark_case> out;threads=std::clamp(threads,1,8);
 if(mode==0){for(int n:{512,1024,2048,4096})if(n<=context)out.push_back({n,threads,256,128});}
 if(mode==1){for(int t:{2,4,6,8})if(context>=2048)out.push_back({2048,t,256,128});}
 if(mode==2){for(int b:{256,512})if(context>=2048)out.push_back({2048,threads,b,b/2});}
 auto first=out;for(auto i=first.rbegin();i!=first.rend();++i)out.push_back(*i);
 if(out.empty())throw std::runtime_error("Benchmark context is too small");return out;
}
inline int64_t jobs_benchmark_clock(clockid_t id){timespec ts;if(clock_gettime(id,&ts)!=0)throw std::runtime_error("Benchmark clock unavailable");return int64_t(ts.tv_sec)*1000000000LL+ts.tv_nsec;}
inline void jobs_benchmark(llama_model*model,int context,int mode,int threads,const std::function<bool()>&cancel,const std::function<void(const std::string&)>&emit){
 auto plan=jobs_benchmark_plan(mode,threads,context);auto*vocab=llama_model_get_vocab(model);std::string seed="A small neutral sentence for a synthetic computation test. 0123456789 ";int size=-llama_tokenize(vocab,seed.data(),seed.size(),nullptr,0,false,true);if(size<=0)throw std::runtime_error("Benchmark tokenization failed");std::vector<llama_token> pattern(size);if(llama_tokenize(vocab,seed.data(),seed.size(),pattern.data(),pattern.size(),false,true)!=size)throw std::runtime_error("Benchmark tokenization mismatch");
 int trial=0;for(auto c:plan){if(cancel())throw std::runtime_error("Cancelled");++trial;
  auto cp=llama_context_default_params();cp.n_ctx=context;cp.n_batch=c.batch;cp.n_ubatch=c.ubatch;cp.n_threads=c.threads;cp.n_threads_batch=c.threads;
  std::unique_ptr<llama_context,decltype(&llama_free)> ctx(llama_init_from_model(model,cp),llama_free);if(!ctx)throw std::runtime_error("Cannot allocate benchmark context");
  std::vector<llama_token> tokens(c.input);for(int i=0;i<c.input;i++)tokens[i]=pattern[i%pattern.size()];
  int64_t decode=0,cpu=0;int processed=0;auto start=jobs_benchmark_clock(CLOCK_MONOTONIC);
  auto report=[&](bool complete,int count,int64_t wall,int64_t cpu_step){std::string s="{\"trial\":"+std::to_string(trial)+",\"trials\":"+std::to_string(plan.size())+",\"input\":"+std::to_string(c.input)+",\"processed\":"+std::to_string(processed)+",\"threads\":"+std::to_string(c.threads)+",\"batch\":"+std::to_string(c.batch)+",\"ubatch\":"+std::to_string(c.ubatch)+",\"context\":"+std::to_string(context)+",\"count\":"+std::to_string(count)+",\"decodeNs\":"+std::to_string(decode)+",\"cpuNs\":"+std::to_string(cpu)+",\"batchNs\":"+std::to_string(wall)+",\"batchCpuNs\":"+std::to_string(cpu_step)+",\"elapsedNs\":"+std::to_string(jobs_benchmark_clock(CLOCK_MONOTONIC)-start)+",\"complete\":"+(complete?"true":"false")+"}";emit(s);};
  report(false,0,0,0);
  while(processed<c.input){if(cancel())throw std::runtime_error("Cancelled");int n=std::min(c.batch,c.input-processed);auto t0=jobs_benchmark_clock(CLOCK_MONOTONIC),p0=jobs_benchmark_clock(CLOCK_PROCESS_CPUTIME_ID);int result=llama_decode(ctx.get(),llama_batch_get_one(tokens.data()+processed,n));llama_synchronize(ctx.get());auto p1=jobs_benchmark_clock(CLOCK_PROCESS_CPUTIME_ID),t1=jobs_benchmark_clock(CLOCK_MONOTONIC);if(result!=0)throw std::runtime_error("Benchmark prompt evaluation failed");processed+=n;decode+=t1-t0;cpu+=p1-p0;report(processed==c.input,n,t1-t0,p1-p0);}
 }
}
