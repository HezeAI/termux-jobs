package com.termux.app.workspace;

import android.content.Context;
import java.util.*;

/** One saved display order for the project browser and all of its picker modes. */
final class FileSort {
    enum Order { NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC, DATE_DESC, DATE_ASC }
    static Order read(Context context) {
        try {return Order.valueOf(context.getSharedPreferences("workspace-files",0).getString("sort",Order.NAME_ASC.name()));}
        catch(IllegalArgumentException error) {return Order.NAME_ASC;}
    }
    static void save(Context context,Order order) {context.getSharedPreferences("workspace-files",0).edit().putString("sort",order.name()).apply();}
    static String[] labels(Context context) {
        return WorkspaceLanguage.code(context).equals("ru")
            ?new String[]{"Имя · А → Я","Имя · Я → А","Размер · сначала большие","Размер · сначала маленькие","Дата · сначала новые","Дата · сначала старые"}
            :new String[]{"Name · A → Z","Name · Z → A","Size · largest first","Size · smallest first","Date · newest first","Date · oldest first"};
    }
    static List<ProjectFiles.Entry> ordered(List<ProjectFiles.Entry> entries,Order order) {
        List<ProjectFiles.Entry> sorted=new ArrayList<>(entries);
        sorted.sort((a,b)->{
            if(a.directory!=b.directory)return a.directory?-1:1;
            int result=0;
            switch(order) {
                case NAME_DESC: return -names(a,b);
                case SIZE_DESC: if(!a.directory)result=Long.compare(b.size,a.size);break;
                case SIZE_ASC: if(!a.directory)result=Long.compare(a.size,b.size);break;
                case DATE_DESC: case DATE_ASC:
                    // Old/remote metadata may have no date. Unknown dates stay last in either direction.
                    if((a.modified<0)!=(b.modified<0))return a.modified<0?1:-1;
                    result=order==Order.DATE_DESC?Long.compare(b.modified,a.modified):Long.compare(a.modified,b.modified);break;
                default: break;
            }
            return result!=0?result:names(a,b);
        });
        return sorted;
    }
    private static int names(ProjectFiles.Entry a,ProjectFiles.Entry b) {
        int compared=a.name.compareToIgnoreCase(b.name);
        if(compared==0)compared=a.name.compareTo(b.name);
        return compared!=0?compared:a.path.compareTo(b.path);
    }
}
