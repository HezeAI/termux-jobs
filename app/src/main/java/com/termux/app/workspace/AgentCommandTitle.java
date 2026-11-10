package com.termux.app.workspace;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Conservative display-only classification. Never executes or rewrites a command. */
final class AgentCommandTitle {
    static String title(JSONObject row,boolean ru){
        String supplied=row.optString("actionDescription").trim();
        if(!supplied.isEmpty())return AgentTimelineSummary.shortText(supplied,120);
        if(row.optBoolean("commandTruncated"))return unknown(ru);
        String command=row.optString("command");
        List<String> words=words(command);
        if(words==null)return t(ru,"Выполнение нескольких команд","Running multiple commands");
        if(words.isEmpty())return unknown(ru);
        String bin=words.get(0);bin=bin.substring(bin.lastIndexOf('/')+1);
        if((bin.equals("bash")||bin.equals("sh"))&&words.size()==3&&(words.get(1).equals("-c")||words.get(1).equals("-lc"))){
            words=words(words.get(2));
            if(words==null)return t(ru,"Выполнение нескольких команд","Running multiple commands");
            if(words.isEmpty())return unknown(ru);
            bin=words.get(0);bin=bin.substring(bin.lastIndexOf('/')+1);
        }
        int at=1;
        if(bin.equals("rtk")&&words.size()>1){bin=words.get(1);at=2;}
        if(bin.equals("git")){
            while(at<words.size()&&(words.get(at).equals("-C")||words.get(at).equals("-c")))at+=2;
            if(at<words.size()&&(words.get(at).equals("status")||words.get(at).equals("diff")))return t(ru,"Проверка изменений","Checking changes");
        }
        if(bin.equals("rg")||bin.equals("grep")||bin.equals("ag"))return t(ru,"Поиск по проекту","Searching project");
        if(bin.equals("find")&&!words.contains("-exec")&&!words.contains("-delete"))return t(ru,"Поиск по проекту","Searching project");
        if(bin.equals("cat")||bin.equals("head")||bin.equals("tail")||bin.equals("read"))return t(ru,"Чтение файла","Reading file");
        if(bin.equals("sed")&&words.size()>at+1&&words.get(at).equals("-n")&&words.get(at+1).matches("[0-9]+(,[0-9$]+)?p"))return t(ru,"Чтение файла","Reading file");
        if(bin.equals("pytest")||bin.equals("jest")||bin.equals("vitest"))return t(ru,"Запуск тестов","Running tests");
        if((bin.equals("python")||bin.equals("python3"))&&words.size()>at+1&&words.get(at).equals("-m")&&(words.get(at+1).equals("pytest")||words.get(at+1).equals("unittest")))return t(ru,"Запуск тестов","Running tests");
        if(bin.equals("gradle")||bin.equals("gradlew")){
            boolean test=false,build=false;
            for(int i=at;i<words.size();i++){
                String task=words.get(i);task=task.substring(task.lastIndexOf(':')+1);
                if(task.matches("test([A-Z].*)?")||task.matches("connected[A-Z].*Test"))test=true;
                if(task.equals("build")||task.matches("(assemble|bundle)([A-Z].*)?"))build=true;
            }
            if(test&&build)return t(ru,"Выполнение нескольких команд","Running multiple commands");
            if(test)return t(ru,"Запуск тестов","Running tests");
            if(build)return t(ru,"Сборка приложения","Building application");
        }
        if(bin.equals("npm")||bin.equals("pnpm")||bin.equals("yarn")){
            if(at<words.size()&&words.get(at).equals("test"))return t(ru,"Запуск тестов","Running tests");
            if(at+1<words.size()&&words.get(at).equals("run")&&words.get(at+1).equals("build"))return t(ru,"Сборка приложения","Building application");
        }
        return unknown(ru);
    }
    private static String unknown(boolean ru){return t(ru,"Выполнение команды","Running command");}
    private static String t(boolean ru,String a,String b){return ru?a:b;}
    // Operators/substitutions are deliberately not interpreted, even inside double quotes.
    private static List<String> words(String text){
        List<String> result=new ArrayList<>();StringBuilder word=new StringBuilder();char quote=0;boolean escaped=false;
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(escaped){word.append(c);escaped=false;continue;}
            if(c=='\\'&&quote!='\''){escaped=true;continue;}
            if(quote!='\''&&(c=='`'||c=='$'&&i+1<text.length()&&text.charAt(i+1)=='('))return null;
            if(quote!=0){if(c==quote)quote=0;else word.append(c);continue;}
            if(c=='\''||c=='"'){quote=c;continue;}
            if(";|&\n\r".indexOf(c)>=0)return null;
            if(c=='<'||c=='>')return new ArrayList<>();
            if(Character.isWhitespace(c)){if(word.length()>0){result.add(word.toString());word.setLength(0);}}else word.append(c);
        }
        if(quote!=0||escaped)return new ArrayList<>();
        if(word.length()>0)result.add(word.toString());return result;
    }
}
