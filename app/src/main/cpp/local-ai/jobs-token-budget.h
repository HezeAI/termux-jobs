#pragma once
#include "llama.h"
#include "jobs-chat-policy.h"
#include <algorithm>
#include <string>
#include <vector>
#include <stdexcept>
// Exact formatted-prompt tokenization, shared by JNI counting and generation.
inline std::vector<llama_token> jobs_prompt_tokens(llama_model *model,const std::vector<std::string>& rs,const std::vector<std::string>& ts){
        std::vector<llama_chat_message> messages;for(size_t i=0;i<rs.size();i++)messages.push_back({rs[i].c_str(),ts[i].c_str()});
        const char *tmpl=llama_model_chat_template(model,nullptr);int len=llama_chat_apply_template(tmpl,messages.data(),messages.size(),true,nullptr,0);
        if(len<=0){tmpl="chatml";len=llama_chat_apply_template(tmpl,messages.data(),messages.size(),true,nullptr,0);}if(len<=0)throw std::runtime_error("Unsupported chat template");
        std::vector<char> prompt(len+1);llama_chat_apply_template(tmpl,messages.data(),messages.size(),true,prompt.data(),prompt.size());
        std::string rendered=jobs_complete_generation_prompt(tmpl,std::string(prompt.data(),len));
        auto vocab=llama_model_get_vocab(model);int count=-llama_tokenize(vocab,rendered.data(),rendered.size(),nullptr,0,true,true);if(count<=0)throw std::runtime_error("Cannot size prompt tokens");std::vector<llama_token> tokens(count);
        count=llama_tokenize(vocab,rendered.data(),rendered.size(),tokens.data(),tokens.size(),true,true);if(count<=0)throw std::runtime_error("Cannot tokenize prompt");
        return tokens;
}

// Conservative token-exact checkpoint before a final app-owned feedback message.
inline size_t jobs_feedback_checkpoint(llama_model* model,const std::vector<std::string>& roles,const std::vector<std::string>& texts,int index,const std::vector<llama_token>& full){
 if(index<1||index!=(int)roles.size()-1||roles.size()!=texts.size()||roles[index]!="user"||full.empty())return 0;
 auto alternate=roles;alternate[index]="assistant";auto other=jobs_prompt_tokens(model,alternate,texts);size_t n=0;while(n<full.size()&&n<other.size()&&full[n]==other[n])++n;return std::min(n,full.size()-1);
}
