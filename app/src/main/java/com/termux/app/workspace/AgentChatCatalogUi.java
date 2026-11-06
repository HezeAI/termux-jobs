package com.termux.app.workspace;

import android.app.AlertDialog;
import android.view.View;
import android.widget.*;
import com.termux.app.TermuxActivity;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Explicit read-only catalog. Cancelling closes only its own file-reader helper. */
final class AgentChatCatalogUi {
    interface Backend extends AutoCloseable {JSONObject page(String cursor)throws Exception;void close();}
    static Backend history(TermuxActivity a,String agent,String environment,String project){
        return new Backend(){
            final ProjectFiles files=new ProjectFiles(a,AgentInstructionsUi.fileProfile(environment));
            public JSONObject page(String cursor)throws Exception{
                String script;
                try(InputStream input=a.getAssets().open("agent-chat-catalog.py");ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] chunk=new byte[8192];int n;while((n=input.read(chunk))!=-1)out.write(chunk,0,n);script=out.toString("UTF-8");
                }
                String code="if [ -f \"$HOME/.bashrc\" ]; then . \"$HOME/.bashrc\" >/dev/null 2>&1; fi; exec python3 -I -c "+SessionShell.quote(script)+" "+SessionShell.quote(agent)+" "+SessionShell.quote(project)+" "+SessionShell.quote(cursor);
                return new JSONObject(new String(files.readOnly("exec /bin/bash --noprofile --norc -c "+SessionShell.quote(code),256*1024),StandardCharsets.UTF_8));
            }
            public void close(){files.close();}
        };
    }
    final TermuxActivity activity;final Backend backend;final Predicate<String> occupied;final Consumer<JSONObject> choice;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final LinkedHashMap<String,JSONObject> entries=new LinkedHashMap<>();
    AlertDialog dialog;LinearLayout rows;TextView status,more;boolean closed,busy,failed;String cursor="";
    AgentChatCatalogUi(TermuxActivity a,Backend b,Predicate<String> occupied,Consumer<JSONObject> choice){activity=a;backend=b;this.occupied=occupied;this.choice=choice;}
    String t(String ru,String en){return AgentInstructionsUi.tr(activity,ru,en);}
    void show(String title){
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);
        status=AgentStatusUi.info(body,"");status.setTag("chat-catalog-state");
        rows=new LinearLayout(activity);rows.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(activity);scroll.addView(rows);body.addView(scroll,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(activity,300)));
        more=WorkspaceDialogs.action(activity,t("Загрузить ещё","Load more"),0);more.setTag("chat-catalog-more");more.setOnClickListener(v->load());body.addView(more);
        dialog=WorkspaceDialogs.builder(activity).setTitle(title).setView(body).setNegativeButton(t("Отмена","Cancel"),null).create();
        dialog.setOnDismissListener(d->{closed=true;backend.close();worker.shutdownNow();});dialog.show();load();
    }
    void load(){if(closed||busy)return;busy=true;failed=false;status.setText(t("Загрузка истории…","Loading history…"));more.setEnabled(false);String requested=cursor;
        worker.execute(()->{JSONObject answer=null;try{answer=backend.page(requested);if(answer==null||answer.has("error")||answer.optJSONArray("sessions")==null)throw new IOException("CATALOG_UNAVAILABLE");}catch(Exception ignored){answer=null;}JSONObject result=answer;
            activity.runOnUiThread(()->{if(closed)return;busy=false;more.setEnabled(true);
                if(result==null){failed=true;cursor="";status.setText(t("Не удалось загрузить. Найденные чаты сохранены.","Could not load. Previously found chats are retained."));more.setText(t("Повторить","Retry"));more.setVisibility(View.VISIBLE);return;}
                JSONArray page=result.optJSONArray("sessions");for(int i=0;i<page.length();i++){JSONObject item=page.optJSONObject(i);if(item!=null&&!item.optString("id").isEmpty())entries.put(item.optString("id"),item);}
                String next=result.optString("cursor");if(!next.isEmpty()&&next.equals(requested)){failed=true;status.setText(t("Не удалось продолжить загрузку","Could not continue loading"));more.setText(t("Повторить","Retry"));return;}
                cursor=next;render();
            });
        });
    }
    void render(){rows.removeAllViews();for(JSONObject entry:entries.values()){
        String id=entry.optString("id"),name=entry.optString("name",id),directory=entry.optString("directory");
        TextView row=WorkspaceDialogs.action(activity,name+"\n"+directory+(occupied.test(id)?t("\nЧат уже открыт","\nChat is already open"):""),0);row.setTag("chat-catalog-"+id);row.setEnabled(!occupied.test(id));row.setOnClickListener(v->{if(closed)return;if(occupied.test(id)){status.setText(t("Чат уже открыт в другой сессии","Chat is already open in another session"));render();return;}choice.accept(entry);dialog.dismiss();});rows.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        status.setText(entries.isEmpty()?(cursor.isEmpty()?t("Сохранённых чатов нет","No saved chats"):t("В этой порции чатов нет. Загрузите следующую.","No chats in this page. Load the next page.")):t("Найдено чатов: ","Chats found: ")+entries.size());more.setText(t("Загрузить ещё","Load more"));more.setVisibility(cursor.isEmpty()?View.GONE:View.VISIBLE);
    }
}
