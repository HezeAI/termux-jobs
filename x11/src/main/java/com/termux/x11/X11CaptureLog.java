package com.termux.x11;

/** Bounded manual-capture output and latest completed current-backdrop snapshot. */
final class X11CaptureLog {
    private final String token;
    private final StringBuilder line=new StringBuilder(),events=new StringBuilder(),pendingBackdrop=new StringBuilder();
    private String backdrop="";
    private int serial=-1;private boolean oversized,backdropOpen;
    X11CaptureLog(String token){this.token=token;}
    void feed(String chunk){for(int i=0;i<chunk.length();i++){char c=chunk.charAt(i);if(c=='\n'){if(!oversized)accept(line.toString());line.setLength(0);oversized=false;}else if(!oversized){if(line.length()<2048)line.append(c);else{line.setLength(0);oversized=true;}}}}
    private void accept(String value){
        if(value.startsWith("JOBS_X11_CAPTURE_BEGIN ")){
            String[] p=value.split(" ");if(p.length!=3||!p[1].equals(token))return;
            try{int next=Integer.parseInt(p[2]);if(next<=serial)return;serial=next;events.setLength(0);backdrop="";backdropOpen=false;}catch(NumberFormatException e){return;}
        }else if(serial<0||!(value.startsWith("JOBS_X11_OBSERVE ")||value.startsWith("JOBS_X11_GTK ")||value.startsWith("JOBS_X11_BACKDROP_CHANGE ")||value.startsWith("JOBS_X11_BACKDROP_CURRENT")))return;
        if(value.startsWith("JOBS_X11_GTK ")&&!value.contains(" capture="+serial+" "))return;
        events.append(value).append('\n');if(events.length()>32768)events.delete(0,events.length()-32768);
        if(value.startsWith("JOBS_X11_BACKDROP_CURRENT_BEGIN")){pendingBackdrop.setLength(0);backdropOpen=true;}
        else if(value.startsWith("JOBS_X11_BACKDROP_CURRENT_END")){if(backdropOpen)backdrop=pendingBackdrop.toString();backdropOpen=false;}
        else if(backdropOpen&&value.startsWith("JOBS_X11_BACKDROP_CURRENT ")&&pendingBackdrop.length()+value.length()<4096)pendingBackdrop.append(value).append('\n');
    }
    String events(){return events.toString();}
    String backdrop(){return backdrop;}
}
