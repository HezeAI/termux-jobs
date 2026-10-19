package com.termux.app.workspace;

import org.json.JSONObject;

/** UI names only: a tool call is never presented as proof that a skill was used. */
final class CodexActionLabels {
    private CodexActionLabels() {}
    static String title(JSONObject row,boolean ru) {
        String type=row.optString("toolType");
        if(row.optString("skillSource").equals("read"))return (ru?"Чтение инструкций: ":"Read instructions: ")+row.optString("skillName");
        if(type.equals("skill"))return (ru?"Подключён навык: ":"Skill attached: ")+row.optString("skillName");
        String original=row.optString("title");
        String name=row.optString("toolName");if(name.isEmpty())name=type.isEmpty()?original:type;
        if(name.equals("mcpToolCall")||name.equals("dynamicToolCall")||name.equals("functionCallOutput"))name=row.optString("toolName",original);
        String[] parts=name.split("[./]|__");String last=parts.length==0?name:parts[parts.length-1];
        switch(last) {
            case "imageView":case "image_view":case "view_image":return ru?"Просмотр изображения":"View image";
            case "imageGeneration":case "image_generation":case "imagegen":case "generate_image":return ru?"Создание изображения":"Generate image";
            case "WebSearch":case "webSearch":case "web_search":case "google_web_search":return ru?"Поиск в интернете":"Search the web";
            case "Tool result":return ru?"Результат действия":"Action result";
            case "Tool error":return ru?"Ошибка действия":"Action error";
            case "Read":case "read":case "read_file":case "read_many_files":return ru?"Чтение файлов":"Read files";
            case "Write":case "write":case "write_file":return ru?"Запись файла":"Write file";
            case "Edit":case "edit":case "replace":case "apply_patch":return ru?"Изменение файлов":"Edit files";
            case "Glob":case "glob":case "glob_files":return ru?"Поиск файлов":"Find files";
            case "Grep":case "grep":case "grep_search":case "search_file_content":return ru?"Поиск в файлах":"Search file contents";
            case "Bash":case "bash":case "run_shell_command":return ru?"Команда shell":"Shell command";
            case "WebFetch":case "webfetch":case "web_fetch":return ru?"Чтение веб-страницы":"Read web page";
            default:return original;
        }
    }
}
