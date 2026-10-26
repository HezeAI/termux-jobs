package com.termux.app.workspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Named OpenCode connections are independent from Codex authorization and from each other. */
public final class AiAgentsUi {
    private AiAgentsUi() {}
    interface Choice {void chosen(String value,String title);}
    interface ModelChoice {void chosen(String provider,String model,String variant);}
    private interface Result {void ready(JSONObject value);}
    private static LinearLayout column(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(24,12,24,12);return l;}
    private static TextView text(Activity a,String label){TextView t=new TextView(a);t.setText(label);t.setTextColor(WorkspaceDialogs.TEXT);t.setTextSize(14);t.setPadding(0,12,0,12);return t;}
    private static TextView action(Activity a,String label,Runnable call){TextView v=WorkspaceDialogs.action(a,label,0);v.setOnClickListener(x->call.run());return v;}
    private static ScrollView scroll(Activity a,View child){ScrollView v=new ScrollView(a);v.addView(child);return v;}
    private static EditText field(Activity a,String hint,String value){EditText e=new EditText(a);e.setTextColor(WorkspaceDialogs.TEXT);e.setHintTextColor(WorkspaceDialogs.MUTED);e.setHint(hint);e.setText(value);e.setSingleLine();return e;}
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("ai-connections",Context.MODE_PRIVATE);}
    static JSONArray connections(Context c){try{return new JSONArray(prefs(c).getString("list","[]"));}catch(JSONException e){return new JSONArray();}}
    static String title(Context c,String id){JSONArray items=connections(c);for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item!=null&&id.equals(item.optString("id")))return item.optString("name");}return WorkspaceText.tr("Основное подключение");}
    private static void query(Activity a,String id,WorkspaceStore.Session live,JSONObject q,Result result){
        final WorkspaceStore.Session target=live==null?null:AiAgents.snapshot(live);
        AlertDialog loading=WorkspaceDialogs.builder(a).setTitle("OpenCode").setMessage(WorkspaceText.tr("Проверка окружения и чтение данных…")).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();if(q.optString("op").equals("connect") || q.optString("op").equals("model")){loading.setCancelable(false);loading.setCanceledOnTouchOutside(false);loading.setButton(AlertDialog.BUTTON_NEGATIVE,null,(android.content.DialogInterface.OnClickListener)null);}loading.show();
        new Thread(()->{JSONObject data=null;try{data=AiAgents.call(a,id,target,q);}catch(Exception ignored){}JSONObject value=data;
            a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed()||!loading.isShowing())return;loading.dismiss();
                if(value==null){WorkspaceDialogs.builder(a).setTitle("OpenCode").setMessage(WorkspaceText.tr("Нет подключения. Сначала создайте и запустите сессию OpenCode: компоненты установятся в Ubuntu автоматически. Затем повторите выбор модели или настройку подключения. Codex работает независимо.")).setPositiveButton(WorkspaceText.tr("Закрыть"),null).show();return;}result.ready(value);});
        },"ai-agent-read").start();
    }
    static JSONObject op(String op){try{return new JSONObject().put("op",op);}catch(JSONException e){throw new IllegalStateException(e);}}
    static void chooseConnection(Activity a,String selected,Choice result){JSONArray items=connections(a);List<String> names=new ArrayList<>(),ids=new ArrayList<>();names.add(WorkspaceText.tr("Основное подключение"));ids.add(AiAgents.DEFAULT_CONNECTION);
        for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item!=null){names.add(item.optString("name"));ids.add(item.optString("id"));}}
        WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Подключение OpenCode")).setSingleChoiceItems(names.toArray(new String[0]),ids.indexOf(selected),(d,w)->{result.chosen(ids.get(w),names.get(w));d.dismiss();})
            .setNeutralButton(WorkspaceText.tr("Аккаунты и API-ключи"),(d,w)->showConnections(a)).show();
    }
    public static void showConnections(Activity a){JSONArray items=connections(a);List<String> names=new ArrayList<>(),ids=new ArrayList<>();names.add(WorkspaceText.tr("Основное подключение"));ids.add(AiAgents.DEFAULT_CONNECTION);
        for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item!=null){names.add(item.optString("name"));ids.add(item.optString("id"));}}
        WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Аккаунты и API-ключи")).setItems(names.toArray(new String[0]),(d,w)->editConnection(a,ids.get(w),names.get(w)))
            .setPositiveButton(WorkspaceText.tr("+ Подключение"),(d,w)->{EditText name=field(a,WorkspaceText.tr("Название"),"");AlertDialog create=WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Новое подключение")).setView(name).setPositiveButton(WorkspaceText.tr("Создать"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();
                create.setOnShowListener(x->create.getButton(-1).setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()||n.length()>60){name.setError(WorkspaceText.tr("Укажите название"));return;}String id=UUID.randomUUID().toString();try{JSONArray next=connections(a);next.put(new JSONObject().put("id",id).put("name",n));prefs(a).edit().putString("list",next.toString()).apply();}catch(JSONException e){return;}create.dismiss();editConnection(a,id,n);}));create.show();})
            .show();
    }
    private static void editConnection(Activity a,String id,String name){LinearLayout form=column(a);
        form.addView(text(a,WorkspaceText.tr("OpenCode · Ubuntu. Подключение выбирается отдельно для каждой сессии. Вход Codex не меняется.")));
        String[] provider={"google"};String[] codes={"google","openai","anthropic","openrouter"};String[] labels={"Google Gemini","OpenAI","Anthropic Claude","OpenRouter"};
        TextView choose=action(a,"Google Gemini",()->{});form.addView(choose);choose.setOnClickListener(v->WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Провайдер")).setSingleChoiceItems(labels,Arrays.asList(codes).indexOf(provider[0]),(d,w)->{provider[0]=codes[w];choose.setText(labels[w]);d.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        EditText key=field(a,WorkspaceText.tr("API-ключ (не отображается)"),"");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setSaveEnabled(false);key.setLongClickable(false);form.addView(key);
        form.addView(text(a,WorkspaceText.tr("Ключ хранится локально средствами OpenCode в отдельном подключении. Сохранение не проверяет баланс или доступ к модели и не отправляет запрос к ИИ. Для подписки Codex используйте агент Codex и его обычный вход.")));
        form.addView(action(a,WorkspaceText.tr("Каталог моделей"),()->chooseModel(a,id,null,(p,m,v)->{})));
        AlertDialog editor=WorkspaceDialogs.builder(a).setTitle(name).setView(scroll(a,form)).setPositiveButton(WorkspaceText.tr("Сохранить ключ"),null).setNegativeButton(WorkspaceText.tr("Закрыть"),null).create();
        editor.setOnDismissListener(d->key.setText(""));editor.setOnShowListener(x->editor.getButton(-1).setOnClickListener(v->{String secret=key.getText().toString().trim();if(secret.isEmpty()){key.setError(WorkspaceText.tr("Введите API-ключ"));return;}JSONObject q=op("connect");try{q.put("provider",provider[0]).put("key",secret);}catch(JSONException e){return;}key.setText("");query(a,id,null,q,value->{editor.dismiss();WorkspaceDialogs.builder(a).setMessage(WorkspaceText.tr("Ключ сохранён. Доступ к модели проверяется при обращении к провайдеру.")).setPositiveButton(WorkspaceText.tr("Закрыть"),null).show();});}));editor.show();
    }
    static void chooseModel(Activity a,String connection,WorkspaceStore.Session live,ModelChoice result){query(a,connection,live,op("models"),data->{JSONArray models=data.optJSONArray("models");if(models==null)models=new JSONArray();final JSONArray catalog=models;
        LinearLayout body=column(a);EditText search=field(a,WorkspaceText.tr("Поиск моделей"),"");body.addView(search);ListView list=new ListView(a);body.addView(list,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(a,300)));
        List<JSONObject> visible=new ArrayList<>();List<String> labels=new ArrayList<>();ArrayAdapter<String> adapter=new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,labels){@Override public View getView(int position,View recycled,android.view.ViewGroup parent){return WorkspaceDialogs.action(a,getItem(position),0);}};list.setAdapter(adapter);
        Runnable refresh=()->{visible.clear();labels.clear();String q=search.getText().toString().toLowerCase(Locale.ROOT);for(int i=0;i<catalog.length();i++){JSONObject m=catalog.optJSONObject(i);if(m==null)continue;String label=m.optString("provider")+" / "+m.optString("name");if(label.toLowerCase(Locale.ROOT).contains(q)){visible.add(m);labels.add(label);}}adapter.notifyDataSetChanged();};
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int x,int y,int z){}public void afterTextChanged(android.text.Editable e){}public void onTextChanged(CharSequence s,int x,int y,int z){refresh.run();}});refresh.run();
        body.addView(text(a,WorkspaceText.tr("Каталог не гарантирует доступ аккаунта. Модель и поведение применяются после подтверждения.")));
        AlertDialog picker=WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Выбрать модель")).setView(body).setNeutralButton(WorkspaceText.tr("Ввести ID модели"),(d,w)->{EditText id=field(a,"provider/model","");AlertDialog manual=WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Ввести ID модели")).setView(id).setPositiveButton(WorkspaceText.tr("Применить"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();manual.setOnShowListener(x->manual.getButton(-1).setOnClickListener(v->{String value=id.getText().toString().trim();if(value.isEmpty()||!AiAgents.validModel(value)){id.setError("provider/model");return;}int index=value.indexOf('/');WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Подтвердить выбор")).setMessage(value).setPositiveButton(WorkspaceText.tr("Применить"),(confirm,n)->{result.chosen(value.substring(0,index),value.substring(index+1),"");manual.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();}));manual.show();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();
        list.setOnItemClickListener((p,v,pos,row)->{JSONObject m=visible.get(pos);picker.dismiss();JSONArray variants=m.optJSONArray("variants");List<String> values=new ArrayList<>();values.add("");if(variants!=null)for(int i=0;i<variants.length();i++)values.add(variants.optJSONObject(i).optString("id"));String[] names=values.toArray(new String[0]);names[0]=WorkspaceText.tr("По умолчанию");
            WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Поведение модели")).setItems(names,(d,w)->WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Подтвердить выбор")).setMessage(m.optString("provider")+" / "+m.optString("name")+"\n"+names[w]).setPositiveButton(WorkspaceText.tr("Применить"),(dlg,n)->result.chosen(m.optString("provider"),m.optString("id"),values.get(w))).setNegativeButton(WorkspaceText.tr("Отмена"),null).show()).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();});picker.show();
    });}
    static void chooseChat(Activity a,String connection,Choice result){chooseChat(a,connection,null,result);}
    static void chooseChat(Activity a,String connection,WorkspaceStore.Session profile,Choice result){query(a,connection,profile,op("sessions"),data->{JSONArray sessions=data.optJSONArray("sessions");List<String> names=new ArrayList<>(),ids=new ArrayList<>();if(sessions!=null)for(int i=0;i<sessions.length();i++){JSONObject s=sessions.optJSONObject(i);if(s!=null&&AiAgents.validThread(s.optString("id"))){names.add(s.optString("name")+" · "+s.optString("directory"));ids.add(s.optString("id"));}}
        if(names.isEmpty()){WorkspaceDialogs.builder(a).setMessage(WorkspaceText.tr("Сохранённых чатов OpenCode в этом подключении нет.")).setPositiveButton(WorkspaceText.tr("Закрыть"),null).show();return;}
        WorkspaceDialogs.builder(a).setTitle(WorkspaceText.tr("Чаты OpenCode")).setItems(names.toArray(new String[0]),(d,w)->result.chosen(ids.get(w),names.get(w))).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();});}
    static String modeDescription(String mode) {
        return WorkspaceText.tr(mode.equals("plan")?"Plan — анализ и подготовка плана. Режим не заменяет ограничения доступа: для запрета изменений выберите «Только чтение».":"Build — реализация задач: правки файлов, сборки и команды. Возможные действия определяются уровнем доступа.");
    }
    static String accessLabel(String access) {
        return WorkspaceText.tr(access.equals("full")?"Полный доступ · без подтверждений":access.equals("read")?"Только чтение · без команд":"С подтверждениями");
    }
    static String accessDescription(String access) {
        return WorkspaceText.tr(access.equals("full")?"Разрешённые инструменты могут изменять видимые Ubuntu файлы и выполнять команды без подтверждений. Это не root Android и не отдельная песочница. Политики OpenCode могут дополнительно ограничивать действия.":access.equals("read")?"Чтение, поиск файлов и вопросы разрешены. Редактирование, shell, субагенты, MCP и неизвестные инструменты запрещены правилами OpenCode. Это ограничения инструментов, а не защита всей файловой системы.":"OpenCode запрашивает разрешение на действия. Если выбрать «Разрешать всегда», OpenCode может сохранить разрешение для проекта. Файловой песочницы этот режим не создаёт.");
    }
    static boolean showConfirmedModel(WorkspaceStore.Session profile,JSONObject actual,TextView label,Runnable changed) {
        if(actual==null||!AiAgents.validModel(actual.optString("providerID")+"/"+actual.optString("id")))return false;
        label.setText(modelLabel(actual));profile.aiModel=actual.optString("providerID")+"/"+actual.optString("id");profile.aiVariant=actual.optString("variant");changed.run();return true;
    }
    static String sessionModelLabel(WorkspaceStore.Session profile,JSONObject actual){
        String value=actual==null?"":actual.optString("providerID")+"/"+actual.optString("id");String variant=actual==null?"":actual.optString("variant");
        if(value.isEmpty()||!AiAgents.validModel(value)){value=profile.aiModel;variant=profile.aiVariant;}
        if(value.isEmpty())return WorkspaceText.tr("По умолчанию");int slash=value.indexOf('/');return (slash<0?value:value.substring(slash+1))+(variant.isEmpty()?"":" · "+variant);
    }
    static String modelLabel(JSONObject model) {
        if(model==null||model.optString("id").isEmpty())return WorkspaceText.tr("По умолчанию");
        String variant=model.optString("variant");return model.optString("providerID")+" / "+model.optString("id")+(variant.isEmpty()?"":" · "+variant);
    }
    static void active(Activity a,WorkspaceStore.Session s,Runnable changed){active(a,s,changed,()->{},changed);}
    static void active(Activity a,WorkspaceStore.Session s,Runnable changed,Runnable preview,Runnable save){OpenCodeStatusUi ui=new OpenCodeStatusUi(a,s,changed,null);ui.chatPresentation(preview,save);ui.show();}
}
