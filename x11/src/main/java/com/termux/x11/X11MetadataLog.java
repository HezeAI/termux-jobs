package com.termux.x11;

/** Keep bounded startup backdrop results when the ordinary stderr tail rolls over. */
final class X11MetadataLog {
    private final StringBuilder line=new StringBuilder(),saved=new StringBuilder();
    private boolean oversized,windowManagerReady;
    boolean windowManagerReady(){return windowManagerReady;}
    void feed(String chunk){
        for(int i=0;i<chunk.length();i++){
            char c=chunk.charAt(i);
            if(c=='\n'){
                if(!oversized&&line.toString().equals("JOBS_X11_WINDOW_MANAGER=ready"))windowManagerReady=true;
                if(!oversized&&line.indexOf("JOBS_X11_BACKDROP ")==0&&saved.length()+line.length()+1<=4096)saved.append(line).append('\n');
                line.setLength(0);oversized=false;
            }else if(!oversized){if(line.length()<1024)line.append(c);else{line.setLength(0);oversized=true;}}
        }
    }
    String snapshot(){return saved.toString();}
}
