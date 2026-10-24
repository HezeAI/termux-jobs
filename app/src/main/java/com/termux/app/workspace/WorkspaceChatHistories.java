package com.termux.app.workspace;

import android.app.*;
import android.graphics.Typeface;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.termux.R;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/** User-invoked history browser. Codex deletion uses its native thread/delete API only. */
final class WorkspaceChatHistories {
    private static final java.util.Set<String> deleting=java.util.concurrent.ConcurrentHashMap.newKeySet();
    static String key(Entry e){return e.kind+"|"+e.environment+"|"+e.id;}
    static boolean deleting(WorkspaceStore.Session s){return deleting.contains("codex|"+s.codexEnvironment+"|"+s.codexThread);}

    interface Call {JSONObject call(String method,JSONObject params)throws Exception;}
    static void deleteCodex(String id,Call call)throws Exception{if(!CodexSessionShell.validThread(id))throw new IllegalArgumentException("Invalid thread ID");call.call("thread/delete",new JSONObject().put("threadId",id));}
    static final class Entry {
        final String kind,environment,id,title,project;final long modified;
        Entry(String k,String e,String i,String t,String p,long m){kind=k;environment=e;id=i;title=t;project=p;modified=m;}
    }
    final Activity activity;final WorkspaceStore store;final Executor worker;final Predicate<Entry> active;final Runnable saved;final boolean guarded;
    final List<Entry> entries=new ArrayList<>();AlertDialog dialog;TextView status;LinearLayout rows;EditText search;String filter="all";boolean busy;
    WorkspaceChatHistories(Activity a,WorkspaceStore s,Executor w,Predicate<Entry> check,Runnable changed){this(a,s,w,check,changed,false);}
    WorkspaceChatHistories(Activity a,WorkspaceStore s,Executor w,Predicate<Entry> check,Runnable changed,boolean guarded){activity=a;store=s;worker=w;active=check;saved=changed;this.guarded=guarded;}
    WorkspaceDialogs.Builder builder(){WorkspaceDialogs.Builder b=WorkspaceDialogs.builder(activity);if(guarded){b.blockOutsideDismiss();b.setCancelable(false);}return b;}
    String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    void ui(Runnable r){activity.runOnUiThread(()->{if(!activity.isFinishing()&&dialog!=null&&dialog.isShowing())r.run();});}
    int dp(int v){return WorkspaceDialogs.dp(activity,v);}
    TextView text(String s,int size){TextView v=new TextView(activity);v.setText(s);v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(size);v.setPadding(dp(12),dp(10),dp(12),dp(10));return v;}
    LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    void show(){
        LinearLayout body=column();search=new EditText(activity);search.setTextColor(WorkspaceDialogs.TEXT);search.setHintTextColor(WorkspaceDialogs.MUTED);search.setHint(t("Найти чат или проект","Find chat or project"));body.addView(search);
        TextView filters=WorkspaceDialogs.action(activity,t("Фильтр: Все","Filter: All"),0);filters.setOnClickListener(v->builder().setTitle(t("Истории чатов","Chat histories")).setItems(new String[]{t("Все","All"),"Codex · Termux","Codex · Ubuntu","Codex · Ubuntu AI"},(d,w)->{filter=new String[]{"all","termux","ubuntu","ubuntu-ai"}[w];filters.setText(new String[]{t("Все","All"),"Codex · Termux","Codex · Ubuntu","Codex · Ubuntu AI"}[w]);render();}).setNegativeButton(t("Отмена","Cancel"),null).show());body.addView(filters);
        status=text(t("Загрузка историй…","Loading histories…"),12);body.addView(status);rows=column();ScrollView viewport=new ScrollView(activity);viewport.addView(rows);body.addView(viewport,new LinearLayout.LayoutParams(-1,dp(340)));
        dialog=builder().setTitle(t("Истории чатов","Chat histories")).setView(body).setNegativeButton(t("Назад","Back"),null).create();dialog.show();search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){render();}public void afterTextChanged(Editable e){}});load();
    }

    void load(){busy=true;worker.execute(()->{
        List<Entry> found=new ArrayList<>();List<String> errors=new ArrayList<>();
        for(String environment:new String[]{"termux","ubuntu","ubuntu-ai"}){WorkspaceStore.Session p=new WorkspaceStore.Session();p.codexEnvironment=environment;try(CodexRpc rpc=CodexRpc.history(activity,p)){
            for(boolean archived:new boolean[]{false,true}){String cursor=null;Set<String> seen=new HashSet<>();for(int page=0;page<200;page++){JSONObject params=CodexChatCatalog.params(cursor).put("archived",archived);JSONObject result=rpc.call("thread/list",params);JSONArray data=result.getJSONArray("data");for(int i=0;i<data.length();i++){CodexChatCatalog.Entry e=CodexChatCatalog.parse(data.getJSONObject(i));if(e!=null&&found.stream().noneMatch(x->x.kind.equals("codex")&&x.environment.equals(environment)&&x.id.equals(e.id)))found.add(new Entry("codex",environment,e.id,e.name,e.cwd,e.updated*1000));}String next=result.optString("nextCursor","");if(next.isEmpty()||next.equals("null"))break;if(!seen.add(next)||page==199)throw new IOException("Partial catalog: pagination limit");cursor=next;}}
        }catch(Exception e){errors.add("Codex · "+environment+": "+t("каталог недоступен или прочитан частично","catalog unavailable or partially read"));}}
        found.sort((a,b)->Long.compare(b.modified,a.modified));ui(()->{entries.clear();entries.addAll(found);busy=false;status.setText(t("Историй: ","Histories: ")+entries.size()+(errors.isEmpty()?"":"\n"+android.text.TextUtils.join("\n",errors)));render();});
    });}
    void render(){if(rows==null)return;rows.removeAllViews();String q=search.getText().toString().toLowerCase(Locale.ROOT);for(Entry e:entries){if(!filter.equals("all")&&!e.environment.equals(filter))continue;if(!(e.title+" "+e.project+" "+e.id).toLowerCase(Locale.ROOT).contains(q))continue;
        LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);String date=e.modified>0?android.text.format.DateFormat.format("dd.MM.yyyy HH:mm",e.modified).toString():"";TextView open=WorkspaceDialogs.action(activity,e.title+"\n"+("Codex · "+e.environment)+" · "+date+"\n"+e.project,0);open.setOnClickListener(v->view(e));row.addView(open,new LinearLayout.LayoutParams(0,-2,1));WorkspaceIconButton trash=new WorkspaceIconButton(activity);trash.setIcon(activity.getDrawable(R.drawable.workspace_delete).mutate(),dp(22));trash.setTextColor(0xffff8888);trash.setContentDescription(t("Удалить историю","Delete history"));trash.setOnClickListener(v->confirmDelete(e));row.addView(trash,new LinearLayout.LayoutParams(dp(48),dp(48)));rows.addView(row);
    }}

    static String codexTranscript(JSONObject thread)throws Exception{StringBuilder text=new StringBuilder();JSONArray turns=thread.optJSONArray("turns");if(turns==null)throw new IOException("This Codex version requires paginated history; full view unavailable");for(int i=0;i<turns.length();i++){JSONArray items=turns.getJSONObject(i).optJSONArray("items");if(items==null)continue;for(int j=0;j<items.length();j++){JSONObject item=items.getJSONObject(j);String type=item.optString("type");if(type.equals("agentMessage"))text.append("AI: ").append(item.optString("text")).append("\n\n");else if(type.equals("userMessage")){text.append("You: ");JSONArray content=item.optJSONArray("content");if(content!=null)for(int k=0;k<content.length();k++){JSONObject c=content.optJSONObject(k);if(c!=null)text.append(c.optString("text", "["+c.optString("type")+"]")).append('\n');}text.append('\n');}if(text.length()>512000)return text+"\n[Preview limited; saved history unchanged.]";}}return text.toString();}
    void view(Entry e){status.setText(t("Чтение истории…","Reading history…"));worker.execute(()->{try{String value;{WorkspaceStore.Session p=new WorkspaceStore.Session();p.codexEnvironment=e.environment;try(CodexRpc rpc=CodexRpc.history(activity,p)){value=codexTranscript(rpc.call("thread/read",new JSONObject().put("threadId",e.id).put("includeTurns",true)).getJSONObject("thread"));}}String out=value;ui(()->{TextView body=text(out.isEmpty()?t("История пуста","History is empty"):out,14);body.setTextIsSelectable(true);ScrollView scroll=new ScrollView(activity);scroll.addView(body);builder().setTitle(e.title).setView(scroll).setPositiveButton(t("Назад","Back"),null).show();status.setText(t("Историй: ","Histories: ")+entries.size());});}catch(Exception ex){ui(()->status.setText(t("Не удалось открыть историю: ","Cannot open history: ")+ex.getClass().getSimpleName()));}});}
    void confirmDelete(Entry e){if(busy)return;if(active.test(e)){status.setText(t("Сначала закройте сессии с этим чатом.","Close sessions using this chat first."));return;}
        if(guarded){StorageDeletionConsent.show(activity,t("Удалить историю чата?","Delete chat history?"),e.title+"\n"+t("Разговор будет удалён; файлы проекта и авторизация сохранятся.","The conversation will be deleted; project files and authentication are retained."),()->delete(e));return;}
        WorkspaceDialogs.builder(activity).setTitle(t("Удалить историю чата?","Delete chat history?")).setMessage(e.title+"\n"+e.id+"\n"+t("Разговор будет удалён. Файлы проекта и авторизация не удаляются. Пресеты останутся, но потеряют привязку к этому чату. Закройте также разговоры, открытые вне приложения.","The conversation will be deleted. Project files and authorization are kept. Presets remain but lose this chat binding. Also close conversations opened outside this app."))
        .setNegativeButton(t("Назад","Back"),null).setPositiveButton(t("Удалить","Delete"),(d,w)->delete(e)).show();
    }
    void delete(Entry e){if(!store.isWritable()){status.setText(t("Библиотека недоступна для записи","Library is not writable"));return;}if(active.test(e)){status.setText(t("Чат снова открыт — удаление отменено.","Chat was reopened; deletion cancelled."));return;}if(!deleting.add(key(e)))return;busy=true;worker.execute(()->{try{{WorkspaceStore.Session p=new WorkspaceStore.Session();p.codexEnvironment=e.environment;try(CodexRpc rpc=CodexRpc.history(activity,p)){deleteCodex(e.id,rpc::call);}}activity.runOnUiThread(()->{for(WorkspaceStore.Session s:store.sessions){if(e.kind.equals("codex")&&s.codexEnvironment.equals(e.environment)&&s.codexThread.equals(e.id)){s.codexChat="new";s.codexThread=java.util.UUID.randomUUID().toString();}}store.save();entries.remove(e);busy=false;deleting.remove(key(e));saved.run();if(dialog==null||!dialog.isShowing())return;status.setText(t("История удалена","History deleted"));render();});}catch(Exception ex){deleting.remove(key(e));ui(()->{busy=false;status.setText(t("Удаление не подтверждено. Codex должен поддерживать thread/delete; архивирование вместо удаления не выполняется.","Deletion not confirmed. Codex must support thread/delete; archiving is not substituted for deletion."));});}});}
}
