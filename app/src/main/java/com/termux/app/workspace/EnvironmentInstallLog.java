package com.termux.app.workspace;

/** Structured current-result export only. Never reads, writes or deletes legacy logs. */
final class EnvironmentInstallLog {
    static String report(String alias,int code,String output){
        String id=alias!=null&&alias.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}")?alias:"environment";
        return "Termux Job's environment status\nEnvironment: "+id+"\nExit status: "+code+"\n\n"+EnvironmentSetupStatus.parse(output).snapshot();
    }
}
