package com.termux.app.workspace;
/** User-selected runtime budgets, independent of model architecture's advertised maximum. */
final class LocalAiLimits {
 static final int[] ANSWERS={256,512,768,1024,2048};
 static void validate(int context,int answer){if(context!=4096&&context!=8192)throw new IllegalArgumentException("Invalid local context");boolean found=false;for(int n:ANSWERS)found|=n==answer;if(!found)throw new IllegalArgumentException("Invalid local answer limit");}
 static void engine(String engine,int context,int answer){validate(context,answer);if(!engine.equals("llama")&&context!=4096)throw new IllegalArgumentException("This LiteRT build supports only the verified 4096 context setting");}
}
