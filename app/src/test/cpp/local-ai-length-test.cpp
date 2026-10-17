#include "jobs-benchmark.h"
#include <cassert>
#include <cstdio>
int main(int argc,char**argv){assert(argc==2);assert(jobs_benchmark_plan(0,4,8192).size()==8);auto threads=jobs_benchmark_plan(1,4,8192);assert(threads.size()==8&&threads[0].threads==2&&threads[2].threads==6&&threads[3].threads==8&&threads[7].threads==2);auto batch=jobs_benchmark_plan(2,4,8192);assert(batch.size()==4&&batch[1].batch==512&&batch[1].ubatch==256);bool bad=false;try{jobs_benchmark_plan(3,4,8192);}catch(...){bad=true;}assert(bad);
 llama_backend_init();auto mp=llama_model_default_params();mp.n_gpu_layers=0;auto*m=llama_model_load_from_file(argv[1],mp);assert(m);auto cp=llama_context_default_params();cp.n_ctx=4096;cp.n_threads=2;cp.n_threads_batch=2;auto*c=llama_init_from_model(m,cp);assert(c);llama_token active[]={1,2,3};assert(llama_decode(c,llama_batch_get_one(active,3))==0);std::vector<float> logits(llama_get_logits_ith(c,-1),llama_get_logits_ith(c,-1)+llama_vocab_n_tokens(llama_model_get_vocab(m)));auto pos=llama_memory_seq_pos_max(llama_get_memory(c),0);
 int completed=0,batches=0;bool cancel=false;jobs_benchmark(m,4096,0,4,[&](){return cancel;},[&](const std::string&s){assert(s.find("\"context\":4096")!=std::string::npos);if(s.find("\"complete\":true")!=std::string::npos)completed++;batches++;});assert(completed==8&&batches>8);
 assert(llama_memory_seq_pos_max(llama_get_memory(c),0)==pos);auto*l=llama_get_logits_ith(c,-1);for(size_t i=0;i<logits.size();i++)assert(logits[i]==l[i]);
 bool stopped=false;try{jobs_benchmark(m,4096,2,4,[&](){return cancel;},[&](const std::string&){cancel=true;});}catch(const std::runtime_error&){stopped=true;}assert(stopped);assert(llama_memory_seq_pos_max(llama_get_memory(c),0)==pos);assert(llama_decode(c,llama_batch_get_one(active,1))==0);
 llama_free(c);llama_model_free(m);puts("PASS length matrix, thread/batch plans, callback cancellation, active context/logits preserved and usable");
}
