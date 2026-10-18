package com.termux.app.workspace;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/** Best-effort local foreground-job signal, not remote SSH or AI semantic state. */
final class SessionActivitySignal {
    static int foreground(String stat){
        try{int end=stat.lastIndexOf(')');if(end<0)return -1;String[] fields=stat.substring(end+1).trim().split("\\s+");return fields.length>5?Integer.parseInt(fields[5]):-1;}catch(RuntimeException invalid){return -1;}
    }
    static boolean command(String name){return name!=null&&!name.trim().isEmpty()&&!Arrays.asList("bash","sh","dash","zsh","fish","proot","ssh","sshd","node","codex","claude","gemini","opencode","tmux","screen","python","python3","nano","vim","nvim","less","more","top","htop").contains(name.trim());}
    static boolean busy(int pid){
        if(pid<=0)return false;
        try{String value=new String(Files.readAllBytes(Paths.get("/proc",Integer.toString(pid),"stat")),java.nio.charset.StandardCharsets.UTF_8);int group=foreground(value);if(group<=0)return false;String name=new String(Files.readAllBytes(Paths.get("/proc",Integer.toString(group),"comm")),java.nio.charset.StandardCharsets.UTF_8);return command(name);}catch(Exception unavailable){return false;}
    }
}
