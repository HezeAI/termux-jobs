package com.termux.x11;

/** Bounded APT status-fd parser. ETA is for the current measured stage, never a fake total. */
public final class X11InstallProgress {
    // APT's acquire debug record carries measured sizes; dlstatus's first number
    // is a FILE count, never a byte count. Units are decimal and rounded by APT.
    private static final java.util.regex.Pattern BYTES=java.util.regex.Pattern.compile("^\\[\\s*([0-9]+(?:\\.[0-9]+)?)\\] Bytes: ([0-9]+(?:\\.[0-9]+)?)\\s*([kMGT]?) / ([0-9]+(?:\\.[0-9]+)?)\\s*([kMGT]?) # Files: [0-9]+ / [0-9]+$");
    private static final java.util.regex.Pattern APK=java.util.regex.Pattern.compile("^([0-9]+)/([0-9]+)(?: ([A-Za-z_-]+))?$");
    private static final java.util.regex.Pattern DNF=java.util.regex.Pattern.compile("^\\[\\s*([0-9]+)\\s*/\\s*([0-9]+)\\]\\s+(Installing|Upgrading|Reinstalling|Downgrading|Downloading)\\b.*",java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern ZYPP=java.util.regex.Pattern.compile("^(?:\\(\\s*[0-9]+\\s*/\\s*[0-9]+\\s*\\)\\s*)?(Preloading|Retrieving|Installing|Upgrading|Updating|Removing):\\s*([A-Za-z0-9][A-Za-z0-9+_.:~%-]{0,255})(\\s.*)$");
    private static final java.util.regex.Pattern ZYPP_COUNT=java.util.regex.Pattern.compile("\\(\\s*([0-9]+)\\s*/\\s*([0-9]+)\\s*\\)");
    private long downloaded=-1,total=-1;
    private final StringBuilder line=new StringBuilder();
    private boolean overflow;private String apt="",stage="preparing",item="";
    private int percent=-1;private long start=-1,lastTime=-1;private double initial,last;
    public void feed(String chunk,long now){for(int i=0;i<chunk.length();i++){
        char c=chunk.charAt(i);if(c=='\n'||c=='\r'){if(!overflow)parse(line.toString(),now);line.setLength(0);overflow=false;}
        else if(!overflow){if(line.length()<4096)line.append(c);else{line.setLength(0);overflow=true;}}
    }if(!overflow&&line.length()>0)zypp(line.toString(),now,false);}
    private void parse(String value,long now){
        if(value.equals("EXTRA_STAGE:kali-style")||value.equals("EXTRA_STAGE:virgl-alpine")){
            apt="extra";newStage("downloading");percent=-1;item=value.endsWith("kali-style")?"Kali theme":"Alpine VirGL client";return;
        }
        if(value.startsWith("EXTRA_PROGRESS:")&&apt.equals("extra")){
            if(!value.matches("EXTRA_PROGRESS:[0-9]{1,8}:[0-9]{1,8}"))return;
            String[] n=value.split(":");long done=Long.parseLong(n[1]),all=Long.parseLong(n[2]);if(all<=0||all>8*1024*1024||done>all)return;
            double p=100.0*done/all;if(start<0||p<last){start=now;initial=p;}last=p;lastTime=now;downloaded=done;total=all;percent=(int)p;return;
        }
        if((value.equals("JOBS_APT_PHASE:update")||value.equals("JOBS_APT_PHASE:install"))&&apt.equals("extra"))percent=-1;
        if(value.equals("JOBS_APT_PHASE:update")||value.equals("JOBS_APT_PHASE:install")){apt=value.substring(15);item="";if(apt.equals("update"))percent=-1;newStage(apt.equals("update")?"updating":"downloading");return;}
        if(zypp(value,now,true))return;
        java.util.regex.Matcher dnf=DNF.matcher(value);
        if(dnf.matches()&&!apt.isEmpty()){
            double done=Double.parseDouble(dnf.group(1)),all=Double.parseDouble(dnf.group(2));
            if(!Double.isFinite(done)||!Double.isFinite(all)||all<=0||done>all)return;
            parse((dnf.group(3).equalsIgnoreCase("Downloading")?"dlstatus:p:":"pmstatus:p:")+(100*done/all)+":dnf",now);return;
        }
        java.util.regex.Matcher apk=APK.matcher(value);
        if(apk.matches()&&!apt.isEmpty()){
            double done=Double.parseDouble(apk.group(1)),all=Double.parseDouble(apk.group(2));
            if(!Double.isFinite(done)||!Double.isFinite(all)||all<=0||done>all)return;
            String kind=apk.group(3)==null?"":apk.group(3).toLowerCase(java.util.Locale.ROOT);
            boolean install=kind.contains("install")||kind.contains("extract");
            parse((install?"pmstatus:p:":"dlstatus:p:")+(100*done/all)+":apk",now);return;
        }
        java.util.regex.Matcher bytes=BYTES.matcher(value);
        if(bytes.matches()){
            if(apt.isEmpty()||stage.equals("installing"))return;
            double p=Double.parseDouble(bytes.group(1));
            long done=size(bytes.group(2),bytes.group(3)),all=size(bytes.group(4),bytes.group(5));
            if(p<=100&&done>=0&&all>0&&done<=all){downloaded=done;total=all;}
            return;
        }
        String[] parts=value.split(":",4);if(parts.length!=4||apt.isEmpty()||!(parts[0].equals("dlstatus")||parts[0].equals("pmstatus")))return;
        double p;try{p=Double.parseDouble(parts[2]);}catch(NumberFormatException e){return;}if(Double.isNaN(p)||Double.isInfinite(p)||p<0||p>100)return;
        String next=apt.equals("update")?"updating":parts[0].equals("pmstatus")?"installing":"downloading";
        // Ignore late download reports once dpkg has started, rather than regress the UI.
        if(stage.equals("installing")&&next.equals("downloading"))return;
        if(!stage.equals(next))newStage(next);
        if(start<0){start=now;initial=p;}else if(p<last){start=now;initial=p;}
        last=p;lastTime=now;
        int mapped=next.equals("updating")?(int)(p*.1):next.equals("downloading")?10+(int)(p*.45):55+(int)(p*.44);
        percent=Math.max(percent,Math.min(99,mapped));
    }
    private boolean zypp(String value,long now,boolean complete){
        if(!apt.equals("install"))return false;
        value=value.replaceAll("\u001B\\[[0-9;?]*[ -/]*[@-~]","").trim();
        java.util.regex.Matcher match=ZYPP.matcher(value);if(!match.matches())return false;
        boolean downloading=match.group(1).equals("Preloading")||match.group(1).equals("Retrieving");
        if(stage.equals("installing")&&downloading)return true;
        String label=match.group(2);java.util.regex.Matcher count=ZYPP_COUNT.matcher(value);
        if(count.find()){
            double done=Double.parseDouble(count.group(1)),all=Double.parseDouble(count.group(2));
            if(!Double.isFinite(done)||!Double.isFinite(all)||all<=0||done<1||done>all)return true;
            label+=" ("+count.group(1)+"/"+count.group(2)+")";
            // A displayed ordinal is the active package, not a completed one.
            if(complete){double finished=value.endsWith("[done]")?done:done-1;
                parse((downloading?"dlstatus:p:":"pmstatus:p:")+(100*finished/all)+":zypper",now);}
        }else if(complete&&!stage.equals(downloading?"downloading":"installing"))newStage(downloading?"downloading":"installing");
        item=label;return true;
    }
    private void newStage(String value){stage=value;downloaded=-1;total=-1;start=-1;lastTime=-1;last=0;initial=0;}
    private static long size(String number,String unit){
        double value=Double.parseDouble(number);int power=unit.isEmpty()?0:"kMGT".indexOf(unit)+1;
        value*=Math.pow(1000,power);return !Double.isFinite(value)||value>1e18?-1:Math.round(value);
    }
    public long downloadedBytes(){return downloaded;}
    public long totalBytes(){return total;}
    public int percent(){return percent;}
    public String stage(){return stage;}
    public String currentItem(){return item;}
    public long etaSeconds(long now){
        if(start<0||lastTime<0||now-lastTime>15000||lastTime-start<2000||last-initial<1||last>=100)return -1;
        double seconds=(lastTime-start)/1000.0*(100-last)/(last-initial)-(now-lastTime)/1000.0;
        return seconds<=0||seconds>604800?-1:Math.max(1,(long)Math.ceil(seconds));
    }
}
