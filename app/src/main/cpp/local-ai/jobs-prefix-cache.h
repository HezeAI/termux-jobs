#pragma once
#include "llama.h"
#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>
#include <new>

// RAM-only, one model/context/owner/chat/system. No sampler state or disk persistence.
struct jobs_prefix_cache {
 struct snapshot {std::vector<llama_token> tokens;std::vector<uint8_t> bytes;void clear(){tokens.clear();std::vector<uint8_t>().swap(bytes);}};
 std::vector<llama_token> decoded;std::string scope,system,status="cold";snapshot base,tail;
 size_t budget=128u*1024u*1024u;bool enabled=false;
 static size_t common(const std::vector<llama_token>&a,const std::vector<llama_token>&b){size_t n=0;while(n<a.size()&&n<b.size()&&a[n]==b[n])++n;return n;}
 void reset(){decoded.clear();base.clear();tail.clear();scope.clear();system.clear();enabled=false;}
 void invalidate(llama_context*ctx){reset();llama_memory_clear(llama_get_memory(ctx),true);status="invalidated";}
 size_t begin(llama_context*ctx,const std::vector<llama_token>&next,const std::string&key,const std::string&instructions,bool allow_trim=true){
  auto mem=llama_get_memory(ctx);bool same=enabled&&!key.empty()&&scope==key&&system==instructions;
  if(!same){invalidate(ctx);scope=key;system=instructions;enabled=!key.empty();status=enabled?"cold_or_scope_reset":"disabled";return 0;}
  size_t n=std::min(common(decoded,next),next.empty()?size_t(0):next.size()-1);
  if(n>0&&allow_trim&&llama_memory_seq_pos_max(mem,0)==(llama_pos)decoded.size()-1&&llama_memory_seq_rm(mem,0,n,-1)){
   if(llama_memory_seq_pos_max(mem,0)==(llama_pos)n-1){decoded.resize(n);status="warm_prefix";return n;}
  }
  const snapshot*chosen=nullptr;for(const snapshot* s:{&base,&tail})if(!s->bytes.empty()&&s->tokens.size()<next.size()&&common(s->tokens,next)==s->tokens.size()&&(!chosen||s->tokens.size()>chosen->tokens.size()))chosen=s;
  llama_memory_clear(mem,true);decoded.clear();
  if(chosen){size_t used=llama_state_seq_set_data(ctx,chosen->bytes.data(),chosen->bytes.size(),0);if(used==chosen->bytes.size()&&llama_memory_seq_pos_max(mem,0)==(llama_pos)chosen->tokens.size()-1){decoded=chosen->tokens;status="prefix_snapshot";return decoded.size();}llama_memory_clear(mem,true);base.clear();tail.clear();status="restore_fallback";}
  else status="prefix_miss";
  return 0;
 }
 void append(const llama_token*first,size_t n){decoded.insert(decoded.end(),first,first+n);}
 bool capture(llama_context*ctx,snapshot&dest){
  if(!enabled||decoded.empty())return false;
  try{llama_synchronize(ctx);size_t size=llama_state_seq_get_size(ctx,0);size_t other=&dest==&base?tail.bytes.size():base.bytes.size();if(size==0||size>budget||other>budget-size)return false;
   std::vector<uint8_t> bytes(size);if(llama_state_seq_get_data(ctx,bytes.data(),bytes.size(),0)!=size)return false;
   dest.tokens=decoded;dest.bytes=std::move(bytes);return true;
  }catch(const std::bad_alloc&){return false;}
 }
};
