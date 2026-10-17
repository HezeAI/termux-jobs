#include "jobs-token-budget.h"
#include <cassert>
#include <iostream>
int main(int argc,char**argv){
 assert(argc==2);llama_backend_init();auto options=llama_model_default_params();options.vocab_only=true;
 auto model=llama_model_load_from_file(argv[1],options);assert(model);
 std::vector<std::string> roles{"system"},texts{"Current permissions: read only. Не изменяй файлы."};
 for(int i=0;i<31;i++){roles.emplace_back(i%2?"assistant":"user");texts.emplace_back("Message "+std::to_string(i));}
 auto actual=jobs_prompt_tokens(model,roles,texts);
 std::vector<llama_chat_message> messages;for(size_t i=0;i<roles.size();i++)messages.push_back({roles[i].c_str(),texts[i].c_str()});
 int len=llama_chat_apply_template("chatml",messages.data(),messages.size(),true,nullptr,0);assert(len>0);std::string prompt(len+1,'\0');llama_chat_apply_template("chatml",messages.data(),messages.size(),true,prompt.data(),prompt.size());prompt.resize(len);
 assert(prompt.find("Message 0")!=std::string::npos&&prompt.find("Message 30")!=std::string::npos);
 auto vocab=llama_model_get_vocab(model);int n=-llama_tokenize(vocab,prompt.data(),prompt.size(),nullptr,0,true,true);assert(n>0);std::vector<llama_token> expected(n);int used=llama_tokenize(vocab,prompt.data(),prompt.size(),expected.data(),expected.size(),true,true);assert(used==n);assert(actual==expected);
 roles.resize(3);texts.resize(3);assert(jobs_prompt_tokens(model,roles,texts).size()<actual.size());
 llama_model_free(model);llama_backend_free();std::cout<<"PASS: exact real-vocabulary prompt tokens, UTF-8 permissions, all 31 messages, shorter context and unchanged template fallback\n";
}
