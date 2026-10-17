#include "jobs-prefix-cache.h"
#include <cassert>
#include <cmath>
#include <cstdio>
int main(int argc,char**argv){
 assert(argc==2);llama_backend_init();auto mp=llama_model_default_params();mp.n_gpu_layers=0;auto*m=llama_model_load_from_file(argv[1],mp);assert(m);auto cp=llama_context_default_params();cp.n_ctx=512;cp.n_batch=128;cp.n_threads=2;cp.n_threads_batch=2;auto*c=llama_init_from_model(m,cp);assert(c);auto*v=llama_model_get_vocab(m);
 std::string text="Once upon a time there was a little dog who lived in a house.";std::vector<llama_token> t(128);int n=llama_tokenize(v,text.data(),text.size(),t.data(),t.size(),true,true);assert(n>8);t.resize(n);jobs_prefix_cache cache;
 auto eval=[&](size_t start){if(start<t.size()){assert(llama_decode(c,llama_batch_get_one(t.data()+start,t.size()-start))==0);cache.append(t.data()+start,t.size()-start);}int size=llama_vocab_n_tokens(v);auto*l=llama_get_logits_ith(c,-1);return std::vector<float>(l,l+size);};
 assert(cache.begin(c,t,"chat-a","system-a")==0);auto cold=eval(0);size_t reused=cache.begin(c,t,"chat-a","system-a");assert(reused==t.size()-1);auto warm=eval(reused);for(size_t i=0;i<cold.size();++i)assert(std::abs(cold[i]-warm[i])<0.001f);
 cache.invalidate(c);cache.begin(c,t,"chat-a","system-a");size_t cut=t.size()/2;assert(llama_decode(c,llama_batch_get_one(t.data(),cut))==0);cache.append(t.data(),cut);assert(cache.capture(c,cache.base));eval(cut);
 reused=cache.begin(c,t,"chat-a","system-a",false);assert(reused==cut);assert(cache.status=="prefix_snapshot");auto restored=eval(reused);for(size_t i=0;i<cold.size();++i)assert(std::abs(cold[i]-restored[i])<0.001f);
 auto full=t;t.resize(cut+1);reused=cache.begin(c,t,"chat-a","system-a");assert(reused==t.size()-1);auto shortWarm=eval(reused);cache.invalidate(c);cache.begin(c,t,"chat-a","system-a");auto shortCold=eval(0);for(size_t i=0;i<shortCold.size();++i)assert(std::abs(shortCold[i]-shortWarm[i])<0.001f);t=full;t[0]=full[1];assert(cache.begin(c,t,"chat-a","system-a")==0);assert(cache.status=="prefix_miss");eval(0);
 assert(cache.begin(c,t,"chat-b","system-a")==0);assert(cache.base.bytes.empty());eval(0);assert(cache.begin(c,t,"chat-b","system-b")==0);assert(cache.decoded.empty());eval(0);cache.budget=1;assert(!cache.capture(c,cache.base));cache.invalidate(c);assert(cache.decoded.empty()&&cache.base.bytes.empty());assert(llama_memory_seq_pos_max(llama_get_memory(c),0)<0);assert(cache.begin(c,t,"","system")==0);eval(0);assert(!cache.capture(c,cache.base));
 llama_free(c);llama_model_free(m);puts("PASS real CPU cache: fresh/warm logits, forced snapshot restore, scope/system reset, budget, invalidation, disabled scope");
}
