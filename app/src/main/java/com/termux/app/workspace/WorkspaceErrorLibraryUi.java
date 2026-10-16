package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import java.util.*;
import java.util.function.Consumer;
import org.json.*;

/** On-demand reference only. Copy/paste never submits commands to a PTY. */
final class WorkspaceErrorLibraryUi {
    private final TermuxActivity activity;
    private final boolean ru;
    private final Consumer<String> insert;
    private WorkspaceErrorLibrary library;
    private AlertDialog dialog;
    private LinearLayout results;
    private TextView count;
    private EditText search;
    private String env="all",category="all",context="all";
    private final List<TextView> categoryButtons=new ArrayList<>();
    private WorkspaceErrorLibraryUi(TermuxActivity a,Consumer<String> insert){activity=a;ru=WorkspaceLanguage.code(a).equals("ru");this.insert=insert;}
    static String label(TermuxActivity a){return WorkspaceLanguage.code(a).equals("ru")?"Ошибки и решения":"Errors and solutions";}
    static void show(TermuxActivity a,Consumer<String> insert){new WorkspaceErrorLibraryUi(a,insert).open();}
    private String t(String a,String b){return ru?a:b;}
    private int dp(int n){return WorkspaceDialogs.dp(activity,n);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView text(String value,int size){TextView v=new TextView(activity);v.setText(value);v.setTextSize(size);v.setTextColor(WorkspaceDialogs.TEXT);v.setPadding(dp(10),dp(8),dp(10),dp(8));return v;}
    private void add(LinearLayout parent,View child){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.bottomMargin=dp(6);parent.addView(child,params);}
    private TextView action(String label,Runnable click){TextView b=WorkspaceDialogs.action(activity,label,0);b.setOnClickListener(v->click.run());return b;}
    private String categoryLabel(String key){String[] labels=ru?new String[]{"Все","Пакеты","Файлы","Запуск","Сеть / SSH","Сборки","Git","Окружение","Python","Node.js"}:new String[]{"All","Packages","Files","Launch","Network / SSH","Builds","Git","Environment","Python","Node.js"};return labels[Arrays.asList(WorkspaceErrorLibrary.CATEGORIES).indexOf(key)];}
    private Spinner picker(String description,String[] labels,java.util.function.IntConsumer change){Spinner spinner=new Spinner(activity);ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,labels);spinner.setAdapter(adapter);spinner.setContentDescription(description);spinner.setMinimumHeight(dp(48));spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int index,long id){change.accept(index);}});return spinner;}
    private void open(){
        try{library=WorkspaceErrorLibrary.load(activity);}catch(Exception e){WorkspaceDialogs.builder(activity).setTitle(label(activity)).setMessage(t("Не удалось прочитать встроенную базу. Данные окружений не изменены.","Could not read the bundled library. Environment data is unchanged.")).setPositiveButton("OK",null).show();return;}
        LinearLayout body=column();body.setPadding(dp(10),0,dp(10),dp(8));body.setFocusableInTouchMode(true);
        TextView info=text(t("Офлайн-справочник, не сканер терминала. Решения зависят от версии и среды. Выберите систему и способ запуска.","Offline reference, not a terminal scanner. Solutions depend on version and environment. Select the OS and launch method."),12);add(body,info);
        search=new EditText(activity);search.setSingleLine(true);search.setTextColor(WorkspaceDialogs.TEXT);search.setTextSize(14);search.setHint(t("Текст ошибки или описание проблемы","Error text or problem description"));search.setContentDescription(search.getHint());search.setMinimumHeight(dp(48));add(body,search);
        add(body,picker(t("Система","System"),new String[]{t("Все системы","All systems"),"Termux","Ubuntu","Debian","Arch Linux","Alpine Linux","Fedora","openSUSE",t("Другие Linux","Other Linux")},index->{env=WorkspaceErrorLibrary.ENV[index];update();}));
        add(body,picker(t("Способ запуска","Launch method"),ru?new String[]{"Любой способ запуска","Внутри proot","Обычный Linux / VM","Удалённо по SSH"}:new String[]{"Any launch method","Inside proot","Regular Linux / VM","Remote over SSH"},index->{context=WorkspaceErrorLibrary.CONTEXTS[index];update();}));
        HorizontalScrollView strip=new HorizontalScrollView(activity);strip.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(activity);
        for(String key:WorkspaceErrorLibrary.CATEGORIES){TextView button=action(categoryLabel(key),()->{category=key;update();});button.setTag(key);categoryButtons.add(button);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(48));p.setMarginEnd(dp(6));row.addView(button,p);}strip.addView(row);add(body,strip);
        count=text("",12);add(body,count);ScrollView scroll=new ScrollView(activity);results=column();scroll.addView(results);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        add(body,text(t("База ","Library ")+library.version+t(" · обновляется с приложением. Команды не выполняются автоматически."," · updated with the app. Commands never run automatically."),11));
        FrameLayout frame=new FrameLayout(activity);frame.addView(body,new FrameLayout.LayoutParams(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*.7)));
        LinearLayout titleRow=new LinearLayout(activity);titleRow.setGravity(Gravity.CENTER_VERTICAL);titleRow.setPadding(dp(10),dp(8),dp(10),0);
        TextView title=text(label(activity),18);title.setTypeface(null,Typeface.BOLD);titleRow.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView credit=text(t("Собрано с помощью AI!","Compiled with AI!"),11);credit.setTextColor(WorkspaceDialogs.MUTED);credit.setMaxWidth(dp(135));credit.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);titleRow.addView(credit,new LinearLayout.LayoutParams(-2,-2));
        dialog=WorkspaceDialogs.builder(activity).setCustomTitle(titleRow).setView(frame).setNegativeButton(t("Закрыть","Close"),null).create();
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){update();}public void afterTextChanged(Editable e){}});
        dialog.setOnShowListener(v->{if(dialog.getWindow()!=null){dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);}body.requestFocus();});
        update();dialog.show();
    }
    private void update(){
        if(results==null||search==null)return;
        List<WorkspaceErrorLibrary.Entry> matches=library.search(search.getText().toString(),env,category,context);results.removeAllViews();count.setText(t("Найдено: ","Found: ")+matches.size()+" / "+library.entries.size());
        for(TextView b:categoryButtons){boolean selected=category.equals(b.getTag());b.setSelected(selected);b.setBackground(WorkspaceDialogs.background(activity,selected));}
        if(matches.isEmpty())add(results,text(t("Совпадений нет. Попробуйте часть сообщения, другой фильтр или все системы. Отсутствие карточки не означает отсутствие ошибки.","No matches. Try part of the message, another filter or all systems. A missing entry does not mean there is no problem."),14));
        for(WorkspaceErrorLibrary.Entry entry:matches){LinearLayout card=column();card.setBackground(WorkspaceDialogs.background(activity,false));TextView title=text(entry.text("title",ru),15);title.setTypeface(null,Typeface.BOLD);add(card,title);add(card,text(entry.text("symptom",ru),12));if(entry.data.has("projectStatus"))add(card,text(projectStatus(entry),11));add(card,text(categoryLabel(entry.category)+" · "+String.join(", ",entry.environments),11));card.setOnClickListener(v->detail(entry));card.setContentDescription(entry.text("title",ru));add(results,card);}
    }
    private void section(LinearLayout body,String title,String value){TextView heading=text(title,14);heading.setTextColor(WorkspaceDialogs.ACCENT);add(body,heading);TextView content=text(value,14);content.setTextIsSelectable(true);add(body,content);}
    private String projectStatus(WorkspaceErrorLibrary.Entry entry){
        switch(entry.data.optString("projectStatus")){
            case "open":return t("Открытая проблема: готового решения нет","Open issue: no confirmed fix");
            case "verified-code":return t("Проверено кодом/тестами; проверка устройства отдельно","Code/tests checked; device verification separate");
            case "resolved":return t("Исправление подтверждено для описанного случая","Fix confirmed for the described case");
            default:return t("Разбор и условия применения","Explanation and conditions");
        }
    }
    private void detail(WorkspaceErrorLibrary.Entry e){
        LinearLayout body=column();body.setPadding(dp(8),0,dp(8),dp(8));
        section(body,t("Применимость","Applies to"),String.join(", ",e.environments)+(e.context.equals("proot")?" · proot":"")+"\n"+e.text("symptom",ru));
        if(e.data.has("projectStatus")){section(body,t("Опыт разработки Termux Job’s","Termux Job’s development experience"),projectStatus(e)+"\n"+e.text("projectEvidence",ru));}
        section(body,t("Сначала проверить","Check first"),e.text("check",ru));section(body,e.data.optString("projectStatus").equals("open")?t("Диагностика и временные меры","Diagnostics and temporary measures"):t("Возможное решение","Possible solution"),e.text("solution",ru));section(body,t("Условия и ограничения","Conditions and limitations"),e.text("caution",ru));
        if(!e.command.isEmpty()){
            section(body,t("Команды для проверки (не исправление)","Diagnostic commands (not a fix)"),e.command);
            add(body,action(t("Скопировать команду","Copy command"),()->((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("diagnostic command",e.command))));
        }
        add(body,text(t("Проверено по источникам: ","Sources reviewed: ")+e.data.optString("reviewed")+t(". Это не подтверждение на вашем устройстве.",". This is not device verification."),11));
        JSONArray sources=e.data.optJSONArray("sources");for(int i=0;i<sources.length();i++){JSONObject s=sources.optJSONObject(i);String url=s.optString("url");String name=(s.optString("kind").equals("project")?t("Проект Termux Job’s / отчёты","Termux Job’s project / reports"):s.optString("kind").equals("official")?t("Документация / автор проекта","Documentation / project author"):t("Обсуждение пользователей","Community discussion"))+" · "+Uri.parse(url).getHost();add(body,action(name,()->{try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException error){WorkspaceDialogs.builder(activity).setMessage(url).setPositiveButton("OK",null).show();}}));}
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);AlertDialog details=WorkspaceDialogs.builder(activity).setTitle(e.text("title",ru)).setView(scroll).setNegativeButton(t("Закрыть","Close"),null).create();
        if(!e.command.isEmpty())add(body,action(t("Вставить в сессию…","Insert into session…"),()->WorkspaceDialogs.builder(activity).setTitle(t("Вставить команду проверки?","Insert diagnostic command?"))
            .setMessage(t("Выберите терминал нужной системы. Поле ввода будет заменено, команда не отправляется. Проверьте, что внутри нет открытого чата или интерактивной программы.\n\n","Choose the terminal for the correct OS. The input draft will be replaced; the command is not sent. Check that it is not inside a chat or interactive program.\n\n")+e.command)
            .setPositiveButton(t("Вставить","Insert"),(v,w)->{details.dismiss();dialog.dismiss();insert.accept(e.command);}).setNegativeButton(t("Отмена","Cancel"),null).show()));
        details.show();
    }
}
