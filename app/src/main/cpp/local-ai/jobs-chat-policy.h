#pragma once
#include <string>
// Mirror Qwen's official enable_thinking=false generation suffix. The legacy
// llama_chat_apply_template helper recognizes ChatML but ignores this Jinja flag.
inline std::string jobs_complete_generation_prompt(const char * tmpl,std::string prompt) {
    std::string source=tmpl?tmpl:"",suffix="<|im_start|>assistant\n";
    if(source.find("enable_thinking")!=std::string::npos&&source.find("<think>")!=std::string::npos&&prompt.size()>=suffix.size()&&prompt.compare(prompt.size()-suffix.size(),suffix.size(),suffix)==0)
        prompt+="<think>\n\n</think>\n\n";
    return prompt;
}
