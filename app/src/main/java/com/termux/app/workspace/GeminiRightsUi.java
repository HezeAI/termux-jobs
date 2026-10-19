package com.termux.app.workspace;

/** Same native approval modes in launch settings and the live card. No OS sandbox claims. */
final class GeminiRightsUi {
    static final String[] MODES={"default","auto_edit","plan"};
    static String normalize(String mode){return "autoEdit".equals(mode)?"auto_edit":mode==null?"":mode;}
    static boolean valid(String mode){return java.util.Arrays.asList(MODES).contains(mode);}
    static String label(String mode){switch(normalize(mode)){case "default":return WorkspaceText.tr("Подтверждать действия");case "auto_edit":return WorkspaceText.tr("Разрешать редактирование");case "plan":return WorkspaceText.tr("Планирование");case "yolo":return "YOLO";default:return WorkspaceText.tr("нет данных");}}
    static String description(String mode){switch(normalize(mode)){case "default":return WorkspaceText.tr("Gemini запрашивает разрешения на действия в терминале.");case "auto_edit":return WorkspaceText.tr("Gemini автоматически разрешает инструменты редактирования; остальные действия требуют подтверждения.");case "plan":return WorkspaceText.tr("Анализ и план без выполнения изменений. Это режим Gemini, а не системная песочница.");default:return "";}}
}
