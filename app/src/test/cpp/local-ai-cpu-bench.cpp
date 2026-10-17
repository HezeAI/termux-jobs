#include <algorithm>
#include "llama.h"
#include "ggml-cpu.h"
#include "jobs-cpu-policy.h"
#include <sys/auxv.h>
#include <chrono>
#include <cassert>
#include <cstdio>
#include <string>
#include <vector>
int main(int argc,char**argv){
 assert(argc==2);unsigned long flags=HWCAP_ASIMDDP|HWCAP_FPHP|HWCAP_ASIMDHP|HWCAP_ATOMICS|HWCAP_ASIMDRDM|HWCAP_CRC32;assert(!jobs_fast_cpu_supported(0,0));assert(!jobs_fast_cpu_supported(flags,0));assert(jobs_fast_cpu_supported(flags,HWCAP2_I8MM));for(auto bit:{HWCAP_ASIMDDP,HWCAP_FPHP,HWCAP_ASIMDHP,HWCAP_ATOMICS,HWCAP_ASIMDRDM,HWCAP_CRC32})assert(!jobs_fast_cpu_supported(flags&~bit,HWCAP2_I8MM));
 llama_backend_init();auto mp=llama_model_default_params();mp.n_gpu_layers=0;auto*m=llama_model_load_from_file(argv[1],mp);assert(m);auto cp=llama_context_default_params();cp.n_ctx=512;cp.n_batch=256;cp.n_ubatch=128;cp.n_threads=4;cp.n_threads_batch=4;auto*c=llama_init_from_model(m,cp);assert(c);auto*v=llama_model_get_vocab(m);std::string text;for(int i=0;i<12;i++)text+="Once upon a time there was a little dog. ";std::vector<llama_token> prompt(512);int n=llama_tokenize(v,text.data(),text.size(),prompt.data(),prompt.size(),true,true);assert(n>0&&n<400);prompt.resize(n);using clock=std::chrono::steady_clock;
 for(int trial=0;trial<3;trial++){
  llama_memory_clear(llama_get_memory(c),true);auto start=clock::now();for(int i=0;i<n;i+=256)assert(llama_decode(c,llama_batch_get_one(prompt.data()+i,std::min(256,n-i)))==0);llama_synchronize(c);auto first=clock::now();auto*s=llama_sampler_init_greedy();std::vector<int> result;
  for(int i=0;i<16;i++){llama_token token=llama_sampler_sample(s,c,-1);if(llama_vocab_is_eog(v,token))break;result.push_back(token);assert(llama_decode(c,llama_batch_get_one(&token,1))==0);}llama_synchronize(c);auto end=clock::now();llama_sampler_free(s);
  printf("RESULT {\"trial\":%d,\"input\":%d,\"prefillUs\":%lld,\"generationUs\":%lld,\"dotprod\":%d,\"fp16\":%d,\"i8mm\":%d,\"tokens\":[",trial,n,(long long)std::chrono::duration_cast<std::chrono::microseconds>(first-start).count(),(long long)std::chrono::duration_cast<std::chrono::microseconds>(end-first).count(),ggml_cpu_has_dotprod(),ggml_cpu_has_fp16_va(),ggml_cpu_has_matmul_int8());for(size_t i=0;i<result.size();i++)printf("%s%d",i?",":"",result[i]);puts("]}");
 }
 llama_free(c);llama_model_free(m);
}
