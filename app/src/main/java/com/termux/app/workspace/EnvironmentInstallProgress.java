package com.termux.app.workspace;

import java.util.regex.*;

/** Measured progress of the current installer phase; never invent a total ETA. */
final class EnvironmentInstallProgress {
    int percent=-1; long eta=-1;
    String line="";
    private long firstTime=-1;private int firstPercent;
    private static final Pattern PERCENT=Pattern.compile("(?<![0-9])([0-9]{1,3})(?:\\.[0-9]+)?\\s*%");
    static String failureLine(String report){
        String[] lines=report.split("[\\r\\n]+");
        for(String value:lines){String line=value.trim();if(line.startsWith("ERROR:")||line.matches("(?i).*(?:error:|failed to install:|no match for argument|timed? out|connection reset|could not resolve|cannot download).*"))return line;}
        for(String value:lines){String line=value.trim();if(line.startsWith("MISSING:"))return line;}
        for(int i=lines.length-1;i>=0;i--){String line=lines[i].trim();if(!line.isEmpty()&&!line.equals("CHECK:done")&&!line.startsWith("READY:")&&!line.startsWith("STAGE:"))return line;}
        return "";
    }
    void update(String report,long now){
        line=DesktopProgramProgress.lastLine(report);
        Matcher m=PERCENT.matcher(line);int measured=-1;
        while(m.find()){int n=Integer.parseInt(m.group(1));if(n<=100)measured=n;}
        if(measured<0){percent=-1;eta=-1;firstTime=-1;return;}
        if(firstTime<0||measured<percent){firstTime=now;firstPercent=measured;}
        percent=measured;int delta=measured-firstPercent;
        eta=delta>0&&measured<100?Math.max(1,(now-firstTime)*(100-measured)/delta/1000):-1;
    }
}
