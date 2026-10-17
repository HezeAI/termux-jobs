#include "llama.h"
#include "jobs-chat-policy.h"
#include <cassert>
#include <fstream>
#include <iterator>
#include <iostream>
int main(int argc,char**argv){
 assert(argc==2);std::ifstream f(argv[1]);std::string source((std::istreambuf_iterator<char>(f)),{});assert(source.find("enable_thinking")!=std::string::npos);
 llama_chat_message msg{"user","List /sdcard"};int n=llama_chat_apply_template(source.c_str(),&msg,1,true,nullptr,0);assert(n>0);std::string prompt(n+1,'\0');llama_chat_apply_template(source.c_str(),&msg,1,true,prompt.data(),prompt.size());prompt.resize(n);
 auto fixed=jobs_complete_generation_prompt(source.c_str(),prompt);assert(fixed.find("List /sdcard")!=std::string::npos);assert(fixed.size()>prompt.size());std::string suffix="<think>\n\n</think>\n\n";assert(fixed.substr(fixed.size()-suffix.size())==suffix);
 assert(jobs_complete_generation_prompt("chatml",prompt)==prompt);assert(jobs_complete_generation_prompt(source.c_str(),fixed)==fixed);std::cout<<"PASS: real legacy template plus official Qwen non-thinking suffix; unrelated templates unchanged\n";
}
