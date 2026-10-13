package com.termux.app.workspace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Guarded adapter for the CLI's own picker; never submits a model turn. */
final class CodexModelSelection {
    interface Terminal {
        String screen();
        boolean emptyPrompt();
        boolean menuOpen();
        void write(String keys);
    }
    private final String model, display, effort;
    private int phase, moves;
    private boolean singleEffort;
    private String baseline;
    private long deadline;
    String error = "";
    boolean finished, confirmed;
    CodexModelSelection(String model, String display, String effort) {
        this.model=model; this.display=display; this.effort=effort;
    }
    void singleEffort(boolean value) {singleEffort=value;}
    static String normalize(String value) { return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    static String effortLabel(String value) {
        switch(value) {
            case "none": return "None"; case "minimal": return "Minimal"; case "low": return "Low";
            case "medium": return "Medium"; case "high": return "High"; case "xhigh": return "Extra high";
            case "max": return "Max"; case "ultra": return "Ultra"; default: return "";
        }
    }
    private static final Pattern ROW = Pattern.compile("(?m)^\\s*([›>]?)\\s*(\\d+)\\.\\s*([^\\r\\n]+)");
    static int[] row(String screen, String label) {
        Matcher matcher=ROW.matcher(screen); int target=-1, selected=-1;
        String wanted=normalize(label);
        while(matcher.find()) {
            int index=Integer.parseInt(matcher.group(2));
            if(!matcher.group(1).isEmpty())selected=index;
            String text=matcher.group(3).split("\\s{2,}|\\(|\\u2026|\\.\\.\\.",2)[0].trim();
            if(normalize(text).equals(wanted)) {if(target!=-1)return new int[]{-1,-1};target=index;}
        }
        return new int[]{target,selected};
    }
    static String acknowledgement(String screen) {
        Matcher m=Pattern.compile("(?m)Model changed to ([A-Za-z0-9._-]+)(?:\\s+(none|minimal|low|medium|high|xhigh|max|ultra))?\\b").matcher(screen);
        String result=""; while(m.find())result=m.group(); return result;
    }
    private void stop(String reason) {error=reason;finished=true;}
    private boolean position(Terminal terminal, String screen, String label) {
        int[] row=row(screen,label);
        if(row[1]<1) {stop(WorkspaceText.tr("Не удалось определить выделение в меню Codex. Завершите выбор в терминале."));return false;}
        if(row[0]<1) {
            if(++moves>20) {stop(WorkspaceText.tr("Пункт не найден в меню Codex. Завершите выбор в терминале."));return false;}
            terminal.write("\u001b[B");return false;
        }
        if(row[0]==row[1])return true;
        if(++moves>20) {stop(WorkspaceText.tr("Не удалось выделить пункт Codex. Завершите выбор в терминале."));return false;}
        terminal.write(row[0]>row[1] ? "\u001b[B" : "\u001b[A"); return false;
    }
    void step(Terminal terminal, long now) {
        if(finished)return;
        String screen=terminal.screen();
        if(phase==0) {
            baseline=acknowledgement(screen);deadline=now+15000;
            if(terminal.menuOpen() && screen.contains("Select Model and Effort")) {phase=1;return;}
            if(!terminal.emptyPrompt()) {stop(WorkspaceText.tr("Ввод Codex занят или открыто другое меню. Освободите его и повторите выбор."));return;}
            terminal.write("/model\r");phase=1;return;
        }
        if(now>deadline) {stop(WorkspaceText.tr("Codex не подтвердил смену. Проверьте меню терминала; настройки не считаются применёнными."));return;}
        if(phase==1) {
            if(!terminal.menuOpen() || !screen.contains("Select Model and Effort"))return;
            if(position(terminal,screen,display)) {
                if(singleEffort) {
                    if(screen.contains("s session")) {terminal.write("s");phase=4;}
                    else stop(WorkspaceText.tr("Codex не предлагает применение только к сессии для этой модели. Завершите выбор в терминале."));
                } else {terminal.write("\r");phase=2;moves=0;}
            }
        } else if(phase==2) {
            if(!terminal.menuOpen() || (!screen.contains("Select Reasoning Level") && !screen.contains("Advanced Reasoning")))return;
            // The effort picker must belong to the selected model.
            if(!normalize(screen).contains(normalize(display))) {stop(WorkspaceText.tr("Открылось меню другой модели. Завершите выбор в терминале."));return;}
            boolean advanced=effort.equals("max") || effort.equals("ultra");
            boolean direct=row(screen,effortLabel(effort))[0]>0;
            String label=advanced && !direct ? "More reasoning" : effortLabel(effort);
            if(position(terminal,screen,label)) {
                if(advanced && !direct) {terminal.write("\r");phase=3;moves=0;}
                else if(screen.contains("s session")) {terminal.write("s");phase=4;}
                else stop(WorkspaceText.tr("Эта версия Codex не предлагает применение только к сессии. Завершите выбор в терминале."));
            }
        } else if(phase==3) {
            if(!terminal.menuOpen() || row(screen,effortLabel(effort))[0]<1)return;
            if(position(terminal,screen,effortLabel(effort))) {
                if(screen.contains("s session")) {terminal.write("s");phase=4;}
                else stop(WorkspaceText.tr("Для этого уровня Codex требует отдельное подтверждение. Завершите выбор в терминале."));
            }
        } else {
            String changed=acknowledgement(screen);
            String wanted="Model changed to "+model+" "+effort;
            if(!terminal.menuOpen() && changed.equals(wanted) && !changed.equals(baseline)) {confirmed=true;finished=true;}
        }
    }
}
