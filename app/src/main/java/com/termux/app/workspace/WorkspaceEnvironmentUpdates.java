package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.text.DateFormat;
import java.util.Date;

/** Explicit package/image changes; dismissing a window never cancels its job. */
final class WorkspaceEnvironmentUpdates {
 private WorkspaceEnvironmentUpdates(){}
 static String tr(Context c,String ru,String en){return WorkspaceLanguage.code(c).equals("ru")?ru:en;}
 static String label(Context c){return tr(c,"Проверить обновления окружений","Check environment updates");}
 static String phase(Context c,String p){
  String ru;
  switch(p){
   case "starting":ru="Подготовка";break;case "checking":ru="Проверка пакетов";break;
   case "checking-official-images":ru="Проверка официальных образов";break;
   case "scan":ru="Проверка файлов";break;case "copy":ru="Копирование";break;case "verify":ru="Проверка копии";break;
   case "rechecking":ru="Повторная проверка пакетов";break;case "upgrading":ru="Обновление пакетов";break;
   case "validating":ru="Проверка результата";break;case "committing":case "swapped":ru="Сохранение снимка";break;
   case "preserving-home":ru="Сохранение домашних каталогов";break;case "downloading-image":ru="Загрузка образа";break;
   case "verifying-image":ru="Проверка образа";break;case "installing-image":ru="Установка образа";break;
   case "downloading-desktop":ru="Загрузка компонентов XFCE";break;case "installing-desktop":ru="Установка XFCE";break;
   case "preparing-keyrings":ru="Подготовка ключей репозитория";break;case "complete":ru="Готово";break;
   default:ru="Обработка";
  }
  if(WorkspaceLanguage.code(c).equals("ru"))return ru;
  return p.isEmpty()?"Preparing":p.replace('-',' ');
 }
 static String error(Context c,String code){
  switch(code){
   case "PYTHON_REQUIRED":return tr(c,"Нужен Python в Termux. Установите окружение через приложение или выполните в Termux: pkg install python.","Python is required in Termux. Install an environment through the app, or run in Termux: pkg install python.");
   case "ENVIRONMENT_BUSY":return tr(c,"Окружение используется. Закройте его терминалы, рабочий стол и фоновые процессы, затем повторите.","Environment is in use. Close its terminals, desktop and background processes, then retry.");
   case "NATIVE_LOCKS_REQUIRED":return tr(c,"Нужен proot-distro с блокировками окружений. Старый формат не обновляется этим механизмом.","A proot-distro version with environment locks is required. Legacy environments are not changed.");
   case "PLAN_EXPIRED":case "PLAN_CHANGED":case "PACKAGE_STATE_CHANGED":return tr(c,"Список пакетов изменился или устарел. Выполните проверку ещё раз.","Package candidates changed or the preview expired. Check again.");
   case "NOT_ENOUGH_SPACE":return tr(c,"Недостаточно места для полной копии. Освободите место или удалите ненужные снимки.","Not enough space for a full copy. Free space or delete unused checkpoints.");
   case "CANCELLED":return tr(c,"Операция отменена.","Operation cancelled.");
   case "OFFICIAL_PLAN_EXPIRED":case "OFFICIAL_PLAN_CHANGED":return tr(c,"Образ или окружение изменились, либо проверка устарела. Проверьте официальный образ ещё раз.","The image or environment changed, or the preview expired. Check the official image again.");
   case "OFFICIAL_GPG_REQUIRED":return tr(c,"Для проверки подписи Arch Linux ARM нужен GnuPG в Termux: pkg install gnupg.","Arch Linux ARM signature verification needs GnuPG in Termux: pkg install gnupg.");
   case "OFFICIAL_SIGNATURE_INVALID":case "OFFICIAL_CHECKSUM_REQUIRED":case "OFFICIAL_PLAN_INVALID":return tr(c,"Подлинность выбранного официального образа не подтверждена. Установка остановлена, текущее окружение сохранено.","The selected official image could not be verified. Installation stopped and the current environment is preserved.");
   case "OFFICIAL_DOWNLOAD_FAILED":return tr(c,"Загрузка официального образа не завершилась. Повторите попытку; текущее окружение сохранено.","The official image download did not complete. Retry; the current environment is preserved.");
   case "OFFICIAL_NETWORK":return tr(c,"Не удалось подключиться к официальному источнику с проверкой HTTPS. Повторите позже.","Could not reach the official source with HTTPS verification. Retry later.");
   case "OFFICIAL_RATE_LIMIT":return tr(c,"Официальный источник ограничил запросы или отказал в доступе. Повторите позже.","The official source rate-limited the request or denied access. Retry later.");
   case "OFFICIAL_SOURCE_UNAVAILABLE":case "OFFICIAL_CHECK_FAILED":return tr(c,"Проверка официального источника не завершена. Повторите позже.","The official source check did not complete. Retry later.");
   case "OFFICIAL_ARM64_UNAVAILABLE":return tr(c,"В источнике не найден подходящий образ Linux ARM64.","No matching Linux ARM64 image was found at the source.");
   case "OFFICIAL_IMAGE_UNAVAILABLE":case "OFFICIAL_METADATA_INVALID":case "OFFICIAL_METADATA_TOO_LARGE":case "OFFICIAL_SOURCE_INVALID":return tr(c,"Не удалось подтвердить данные официального образа. Это не означает, что обновлений нет.","Official image metadata could not be verified. This does not mean no update exists.");
   case "MATCHED_DESKTOP_BASE_REQUIRED":return tr(c,"Для нового образа ещё нет совместимого закреплённого комплекта XFCE. Текущее окружение сохранено.","This image has no matching pinned XFCE set yet. The current environment is preserved.");
   case "RECOVERY_REQUIRES_REVIEW":return tr(c,"Состояние файлов изменилось. Автоматическое восстановление остановлено, копии сохранены.","File identities changed. Automatic recovery was refused and copies were retained.");
   default:return tr(c,"Операция не завершена. Код: ","Operation did not complete. Code: ")+code;
  }
 }
 static LinearLayout column(Context c){LinearLayout b=new LinearLayout(c);b.setOrientation(LinearLayout.VERTICAL);int p=WorkspaceDialogs.dp(c,12);b.setPadding(p,p,p,p);return b;}
 static TextView text(Context c,String s){TextView v=new TextView(c);v.setText(s);v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(14);v.setPadding(0,8,0,12);return v;}
 static void action(Context c,LinearLayout b,String s,boolean enabled,Runnable run){TextView v=WorkspaceDialogs.action(c,s,0);v.setEnabled(enabled);v.setAlpha(enabled?1f:.4f);v.setOnClickListener(w->run.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=WorkspaceDialogs.dp(c,6);b.addView(v,p);}
 static ScrollView scroll(Context c,LinearLayout b){ScrollView s=new ScrollView(c);s.addView(b);return s;}
 static void run(Context c,JSONObject q){if(!EnvironmentUpdateJobs.start(c,q))Toast.makeText(c,tr(c,"Дождитесь завершения текущей операции.","Wait for the current operation to finish."),Toast.LENGTH_LONG).show();}
 static void show(Context c){
  LinearLayout body=column(c);TextView status=text(c,"");body.addView(status);
  ProgressBar progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);body.addView(progress);
  TextView stop=WorkspaceDialogs.action(c,tr(c,"Остановить","Stop"),0);stop.setOnClickListener(v->EnvironmentUpdateJobs.cancel((String)v.getTag()));body.addView(stop);
  LinearLayout rows=column(c);body.addView(rows);
  AlertDialog d=WorkspaceDialogs.builder(c).setTitle(label(c)).setView(scroll(c,body)).setNegativeButton(tr(c,"Закрыть","Close"),null).create();
  Handler h=new Handler(Looper.getMainLooper());String[] signature={""};
  Runnable refresh=new Runnable(){public void run(){
   if(!d.isShowing())return;
   if(c instanceof android.app.Activity&&(((android.app.Activity)c).isFinishing()||((android.app.Activity)c).isDestroyed())){d.dismiss();return;}
   JSONObject state=EnvironmentUpdateJobs.state();boolean active=EnvironmentUpdateJobs.active();
   String target=WorkspaceEnvironments.label(state.optString("target"));
   String message=active?(state.optBoolean("stopping")?tr(c,"Остановка установщика…","Stopping installer…"):target+" · "+phase(c,state.optString("phase"))):
    state.optString("status").equals("error")||state.optString("status").equals("cancelled")?error(c,state.optString("error")):tr(c,"Проверка не устанавливает обновления. Для применения закройте все сессии выбранного окружения.","Checking does not install upgrades. Close all sessions of the selected environment before applying changes.");
   if(active)message+=" · "+state.optLong("elapsed")+" s";
   status.setText(message);stop.setTag(state.optString("id"));stop.setVisibility(active?View.VISIBLE:View.GONE);stop.setEnabled(!state.optBoolean("stopping"));
   progress.setVisibility(active?View.VISIBLE:View.GONE);long total=state.optLong("total"),done=state.optLong("processed");progress.setIndeterminate(total<=0);if(total>0){progress.setMax(1000);progress.setProgress((int)Math.min(1000,done*1000d/total));}
   String sig=EnvironmentUpdateJobs.revision()+":"+active;
   if(!sig.equals(signature[0])){signature[0]=sig;render(c,rows,EnvironmentUpdateJobs.inventory(),EnvironmentUpdateJobs.images(),active);}
   h.postDelayed(this,300);
  }};
  d.setOnDismissListener(w->h.removeCallbacks(refresh));d.show();refresh.run();
  if(!EnvironmentUpdateJobs.active())run(c,EnvironmentUpdateJobs.request("inventory",null));
 }
 static void render(Context c,LinearLayout rows,JSONObject inventory,JSONObject images,boolean busy){
  rows.removeAllViews();
  action(c,rows,tr(c,"Проверить пакеты всех окружений","Check packages in all environments"),!busy&&inventory.optBoolean("native"),()->run(c,EnvironmentUpdateJobs.request("check-all",null)));
  action(c,rows,tr(c,"Проверить новые базовые образы","Check for new base images"),!busy,()->run(c,EnvironmentUpdateJobs.request("images",null)));
  action(c,rows,tr(c,"Новые официальные образы","New official images"),!busy,()->official(c));
  if(!inventory.optBoolean("native"))rows.addView(text(c,error(c,"NATIVE_LOCKS_REQUIRED")));
  JSONArray targets=inventory.optJSONArray("targets");
  if(targets!=null)for(int n=0;n<targets.length();n++){
   JSONObject row=targets.optJSONObject(n);if(row==null)continue;String target=row.optString("target");String name=WorkspaceEnvironments.label(target);
   JSONObject system=row.optJSONObject("system");String version=system==null?"":system.optString("PRETTY_NAME");
   JSONObject plan=row.optJSONObject("plan");int count=plan==null?-1:plan.optJSONArray("candidates")==null?0:plan.optJSONArray("candidates").length();
   String title=name+" · "+(!row.optBoolean("installed")?tr(c,"не установлено","not installed"):version);
   if(count>=0)title+="\n"+tr(c,"Обновления пакетов: ","Package updates: ")+count;
   if(row.has("checkError"))title+="\n"+error(c,row.optString("checkError"));
   final JSONObject selected=row;
   action(c,rows,title,!busy&&inventory.optBoolean("native"),()->details(c,selected,images.optJSONObject(target)));
  }
  JSONArray snapshots=inventory.optJSONArray("snapshots");int count=snapshots==null?0:snapshots.length();
  action(c,rows,tr(c,"Снимки и откат","Checkpoints and rollback")+" · "+count,!busy&&count>0,()->snapshots(c,snapshots));
  JSONArray jobs=inventory.optJSONArray("jobs");
  if(jobs!=null)for(int n=0;n<jobs.length();n++){
   JSONObject job=jobs.optJSONObject(n);if(job==null)continue;String st=job.optString("status");
   if(st.equals("running")||st.equals("interrupted")){final String id=job.optString("id");action(c,rows,tr(c,"Восстановить прерванную операцию: ","Recover interrupted operation: ")+WorkspaceEnvironments.label(job.optString("target")),!busy,()->confirm(c,tr(c,"Восстановление","Recovery"),tr(c,"Будет проверен журнал операции. Завершённая замена сохранится; незавершённая копия будет удалена. Рабочие сессии не закрываются автоматически.","The transaction journal will be checked. A completed exchange will be retained; an unpublished partial copy will be removed. Sessions are never closed automatically."),EnvironmentUpdateJobs.request("recover",null),q->q.put("transaction",id),false));}
  }
  rows.addView(text(c,tr(c,"Termux и Ubuntu AI используют прежний порядок обновления. Снимки хранятся в закрытой памяти приложения и могут содержать ключи и пароли.","Termux and Ubuntu AI retain their existing update policy. Checkpoints stay in private app storage and may contain keys and passwords.")));
 }
 static String releaseStatus(Context c,String target,JSONObject system,JSONObject upstream){
  if(upstream.optString("kind").equals("rolling"))return tr(c,"Rolling: обновления системы распространяются пакетами; образ — отдельный снимок.","Rolling: system updates arrive as packages; the image is a separate snapshot.");
  String installed=system==null?"":system.optString("VERSION_ID"),latest=upstream.optString("series");
  if(installed.matches("[0-9]{1,4}(?:\\.[0-9]{1,4}){0,3}")&&latest.matches("[0-9]{1,4}(?:\\.[0-9]{1,4}){0,3}")&&!(system.optString("ID").equals("opensuse-tumbleweed"))){
   String[] old=installed.split("\\."),next=latest.split("\\.");
   for(int n=0;n<next.length;n++){
    long a=n<old.length?Long.parseLong(old[n]):0,b=Long.parseLong(next[n]);
    if(b>a)return tr(c,"Доступен более новый официальный выпуск.","A newer official release is available.");
    if(b<a)return tr(c,"Установленный выпуск новее проверенного стабильного канала.","The installed release is newer than the checked stable channel.");
   }
   return tr(c,"Ветка выпуска совпадает. Точечные исправления проверяются через обновления пакетов.","Release branch matches. Check package updates for point-release fixes.");
  }
  return tr(c,"Сравнение с установленной системой недоступно.","Comparison with the installed system is unavailable.");
 }
 static void officialRows(Context c,LinearLayout rows,JSONObject inventory,JSONObject result,boolean busy){
  rows.removeAllViews();
  rows.addView(text(c,tr(c,"Официальные источники дистрибутивов, отдельно от проверенных наборов Components. Проверка загружает только метаданные. Она не устанавливает образ и не выполняет переход на новый выпуск. Наличие образа не подтверждает совместимость с нашим XFCE и драйверами.","Official distribution sources, separate from verified Components sets. Only metadata is fetched. This check does not install images or migrate to a new release. An available image does not establish compatibility with our XFCE and drivers.")));
  action(c,rows,tr(c,"Проверить официальные образы","Check official images"),!busy,()->run(c,EnvironmentUpdateJobs.request("official-images",null)));
  if(result.has("checkedAt"))rows.addView(text(c,tr(c,"Проверено: ","Checked: ")+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(result.optLong("checkedAt")*1000))));
  JSONObject found=result.optJSONObject("targets");if(found==null)return;
  JSONArray targets=inventory.optJSONArray("targets");
  for(String target:new String[]{"ubuntu","debian","kali","alpine","fedora","opensuse","void","archlinux","manjaro"}){
   JSONObject row=found.optJSONObject(target);if(row==null)continue;
   JSONObject system=null;boolean installed=false;
   if(targets!=null)for(int n=0;n<targets.length();n++){JSONObject r=targets.optJSONObject(n);if(r!=null&&r.optString("target").equals(target)){system=r.optJSONObject("system");installed=r.optBoolean("installed");break;}}
   rows.addView(text(c,WorkspaceEnvironments.label(target)));
   rows.addView(text(c,tr(c,"Установлено: ","Installed: ")+(installed&&system!=null?system.optString("PRETTY_NAME"):tr(c,"не установлено","not installed"))));
   if(row.has("error")){rows.addView(text(c,error(c,row.optString("error"))));continue;}
   String value=row.optString("version");if(value.equals("rolling"))value=tr(c,"rolling · текущий снимок ARM64","rolling · current ARM64 snapshot");
   rows.addView(text(c,tr(c,"Официальный образ: ","Official image: ")+value+"\n"+releaseStatus(c,target,installed?system:null,row)));
   if(row.has("published"))rows.addView(text(c,tr(c,"Опубликован: ","Published: ")+row.optString("published")));
   if(row.has("differsFromPinned"))rows.addView(text(c,row.optBoolean("differsFromPinned")?tr(c,"ARM64-образ отличается от закреплённого в приложении. Это не сравнение с изменённой установленной системой.","The ARM64 image differs from the app-pinned base. This is not a comparison with the modified installed system."):tr(c,"ARM64-образ совпадает с закреплённым в приложении.","The ARM64 image matches the app-pinned base.")));
   if(row.has("newerThanPinned"))rows.addView(text(c,row.optBoolean("newerThanPinned")?tr(c,"Официальный снимок новее закреплённого в приложении.","The official snapshot is newer than the app-pinned base."):tr(c,"Более нового официального снимка относительно базы приложения не найдено.","No official snapshot newer than the app-pinned base was found.")));
   if(target.equals("archlinux"))rows.addView(text(c,tr(c,"Дата официального снимка. Исходный образ установленной системы и его новизна по этой дате не определяются.","Official snapshot date. It does not identify the installed system's original image or establish an available upgrade.")));
   TextView identity=text(c,row.optString("image")+(row.has("digest")?"\n"+row.optString("digest"):""));identity.setTextIsSelectable(true);rows.addView(identity);
   final String url=row.optString("source");action(c,rows,tr(c,"Открыть официальный источник","Open official source"),c instanceof android.app.Activity&&WorkspaceDocumentation.https(url)!=null,()->WorkspaceDocumentationUi.link((android.app.Activity)c,url));
   final String selectedTarget=target;JSONObject selected=new JSONObject();if(targets!=null)for(int n=0;n<targets.length();n++){JSONObject r=targets.optJSONObject(n);if(r!=null&&r.optString("target").equals(target)){selected=r;break;}}
   final JSONObject environment=selected;
   action(c,rows,tr(c,"Открыть меню окружения для установки","Open environment menu to install"),!busy&&inventory.optBoolean("native"),()->details(c,environment,EnvironmentUpdateJobs.images().optJSONObject(selectedTarget)));
  }
 }
 static void official(Context c){
  LinearLayout body=column(c);TextView status=text(c,"");body.addView(status);
  ProgressBar progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);body.addView(progress);
  TextView stop=WorkspaceDialogs.action(c,tr(c,"Остановить","Stop"),0);stop.setOnClickListener(v->EnvironmentUpdateJobs.cancel((String)v.getTag()));body.addView(stop);
  LinearLayout rows=column(c);body.addView(rows);
  AlertDialog d=WorkspaceDialogs.builder(c).setTitle(tr(c,"Новые официальные образы","New official images")).setView(scroll(c,body)).setNegativeButton(tr(c,"Закрыть","Close"),null).create();
  Handler h=new Handler(Looper.getMainLooper());String[] signature={""};
  Runnable poll=new Runnable(){public void run(){
   if(!d.isShowing())return;
   if(c instanceof android.app.Activity&&(((android.app.Activity)c).isFinishing()||((android.app.Activity)c).isDestroyed())){d.dismiss();return;}
   boolean active=EnvironmentUpdateJobs.active();JSONObject state=EnvironmentUpdateJobs.state();
   boolean checking=active&&state.optString("operation").equals("official-images");
   progress.setVisibility(checking?View.VISIBLE:View.GONE);stop.setVisibility(checking?View.VISIBLE:View.GONE);stop.setTag(state.optString("id"));stop.setEnabled(!state.optBoolean("stopping"));
   status.setText(active?WorkspaceEnvironments.label(state.optString("target"))+" · "+phase(c,state.optString("phase")):
    state.optString("operation").equals("official-images")&&(state.optString("status").equals("error")||state.optString("status").equals("cancelled"))?error(c,state.optString("error")):"");
   String next=EnvironmentUpdateJobs.revision()+":"+active;
   if(!next.equals(signature[0])){signature[0]=next;officialRows(c,rows,EnvironmentUpdateJobs.inventory(),EnvironmentUpdateJobs.officialImages(),active);}
   h.postDelayed(this,300);
  }};
  d.setOnDismissListener(v->h.removeCallbacks(poll));d.show();poll.run();
  if(!EnvironmentUpdateJobs.active()&&!EnvironmentUpdateJobs.officialImages().has("checkedAt"))run(c,EnvironmentUpdateJobs.request("official-images",null));
 }
 static void officialForTarget(Context c,LinearLayout b,JSONObject installed){
  String target=installed.optString("target");boolean busy=EnvironmentUpdateJobs.active();
  action(c,b,tr(c,"Проверить официальный образ","Check official image"),!busy,()->run(c,EnvironmentUpdateJobs.request("official-images",target)));
  JSONObject all=EnvironmentUpdateJobs.officialImages().optJSONObject("targets"),row=all==null?null:all.optJSONObject(target);
  if(row==null)return;
  if(row.has("error")){b.addView(text(c,error(c,row.optString("error"))));return;}
  b.addView(text(c,tr(c,"Официальный образ: ","Official image: ")+row.optString("version")+"\n"+releaseStatus(c,target,installed.optJSONObject("system"),row)));
  TextView ref=text(c,row.optString("image")+(row.has("digest")?"\n"+row.optString("digest"):""));ref.setTextIsSelectable(true);b.addView(ref);
  String plan=row.optString("plan");
  if(row.has("installError"))b.addView(text(c,error(c,row.optString("installError"))));
  action(c,b,installed.optBoolean("installed")?tr(c,"Обновить из официального образа","Update from official image"):tr(c,"Установить официальный образ","Install official image"),!busy&&!plan.isEmpty(),()->confirm(c,WorkspaceEnvironments.label(target),tr(c,"Будет установлена выбранная официальная база в отдельное окружение. После проверки она заменит текущее, а прежняя система со всеми программами останется в полном снимке. Дополнительные программы в новой базе потребуется установить заново. Если был XFCE, его обязательные компоненты установятся из репозиториев новой системы. Совместимость нового выпуска с драйверами требует проверки. Закройте все сессии этого окружения.","The selected official base is installed separately. After validation it replaces the current environment; the complete previous system and applications remain in a checkpoint. Additional applications must be installed again in the new base. If XFCE was installed, its required components will be installed from the new system's repositories. Driver compatibility with a new release still needs testing. Close all sessions of this environment."),EnvironmentUpdateJobs.request("official-install",target),q->q.put("plan",plan),installed.optBoolean("installed")));
  final String url=row.optString("source");action(c,b,tr(c,"Открыть официальный источник","Open official source"),c instanceof android.app.Activity&&WorkspaceDocumentation.https(url)!=null,()->WorkspaceDocumentationUi.link((android.app.Activity)c,url));
 }
 static void details(Context c,JSONObject initial,JSONObject fallbackImage){
  String target=initial.optString("target");LinearLayout body=column(c);TextView status=text(c,"");body.addView(status);
  ProgressBar progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);body.addView(progress);
  TextView stop=WorkspaceDialogs.action(c,tr(c,"Остановить","Stop"),0);stop.setOnClickListener(v->EnvironmentUpdateJobs.cancel((String)v.getTag()));body.addView(stop);
  LinearLayout rows=column(c);body.addView(rows);ScrollView content=scroll(c,body);
  AlertDialog d=WorkspaceDialogs.builder(c).setTitle(WorkspaceEnvironments.label(target)).setView(content).setNegativeButton(tr(c,"Закрыть","Close"),null).create();
  Handler h=new Handler(Looper.getMainLooper());String[] signature={""};
  Runnable refresh=new Runnable(){public void run(){
   if(!d.isShowing())return;
   if(c instanceof android.app.Activity&&(((android.app.Activity)c).isFinishing()||((android.app.Activity)c).isDestroyed())){d.dismiss();return;}
   JSONObject state=EnvironmentUpdateJobs.state();boolean active=EnvironmentUpdateJobs.active(),own=target.equals(state.optString("target"));
   progress.setVisibility(active&&own?View.VISIBLE:View.GONE);stop.setVisibility(active&&own?View.VISIBLE:View.GONE);stop.setTag(state.optString("id"));stop.setEnabled(!state.optBoolean("stopping"));
   status.setText(own?(active?(state.optBoolean("stopping")?tr(c,"Остановка…","Stopping…"):phase(c,state.optString("phase"))+" · "+state.optLong("elapsed")+" s"):
    state.optString("status").equals("error")||state.optString("status").equals("cancelled")?error(c,state.optString("error")):state.optString("status").equals("complete")?tr(c,"Проверка или операция завершена.","Check or operation completed."):""):"");
   String next=EnvironmentUpdateJobs.revision()+":"+active;
   if(!next.equals(signature[0])){signature[0]=next;JSONObject row=initial;JSONArray targets=EnvironmentUpdateJobs.inventory().optJSONArray("targets");if(targets!=null)for(int n=0;n<targets.length();n++){JSONObject r=targets.optJSONObject(n);if(r!=null&&target.equals(r.optString("target"))){row=r;break;}}
    int position=content.getScrollY();rows.removeAllViews();JSONObject image=EnvironmentUpdateJobs.images().optJSONObject(target);detailsBody(c,rows,row,image==null?fallbackImage:image);content.scrollTo(0,position);
   }
   h.postDelayed(this,300);
  }};
  d.setOnDismissListener(v->h.removeCallbacks(refresh));d.show();refresh.run();
 }
 static void detailsBody(Context c,LinearLayout b,JSONObject row,JSONObject image){
  String target=row.optString("target");JSONObject plan=row.optJSONObject("plan");JSONArray packages=plan==null?null:plan.optJSONArray("candidates");
  officialForTarget(c,b,row);
  if(!row.optBoolean("installed"))return;
  JSONObject provenance=row.optJSONObject("origin");b.addView(text(c,provenance==null?tr(c,"Исходный базовый образ: не подтверждён.","Original base image: unconfirmed."):tr(c,"Базовый образ: ","Base image: ")+provenance.optString("release")));
  b.addView(text(c,tr(c,"Пакеты обновляются из настроенных репозиториев дистрибутива. Сначала создаётся полная рабочая копия. После проверки она заменит окружение, а прежнее состояние сохранится для отката. Потребуется дополнительное место размером с окружение.","Packages update from the configured distribution repositories. A full working copy is created first. After validation it replaces the environment and the previous state is kept for rollback. Extra space roughly equal to the environment is required.")));
  action(c,b,tr(c,"Проверить обновления пакетов","Check package updates"),!EnvironmentUpdateJobs.active(),()->{run(c,EnvironmentUpdateJobs.request("check",target));});
  if(packages!=null){
   StringBuilder lines=new StringBuilder(tr(c,"Кандидаты на обновление. Зависимости, замены и удаления определит пакетный менеджер при обновлении копии.\n\n","Upgrade candidates. Dependencies, replacements and removals are resolved by the package manager in the working copy.\n\n"));
   for(int n=0;n<packages.length();n++){JSONObject p=packages.optJSONObject(n);if(p!=null)lines.append(p.optString("name")).append("  ").append(p.optString("installed")).append(" → ").append(p.optString("version")).append('\n');}
   action(c,b,tr(c,"Посмотреть список","Review candidates")+" · "+packages.length(),true,()->WorkspaceDialogs.builder(c).setTitle(WorkspaceEnvironments.label(target)).setView(scroll(c,withText(c,lines.toString()))).setNegativeButton(tr(c,"Закрыть","Close"),null).show());
   String id=plan.optString("id");
   action(c,b,tr(c,"Обновить пакеты","Upgrade packages"),packages.length()>0&&!EnvironmentUpdateJobs.active(),()->{confirm(c,WorkspaceEnvironments.label(target),tr(c,"Обновить пакеты в отдельной копии и сохранить текущее окружение для отката? Закройте все его сессии и рабочий стол.","Upgrade packages in a separate copy and keep the current environment for rollback? Close all its sessions and desktop."),EnvironmentUpdateJobs.request("upgrade",target),q->q.put("plan",id),false);});
  }
  action(c,b,tr(c,"Вернуть закреплённый базовый набор","Restore the pinned baseline"),!EnvironmentUpdateJobs.active(),()->{confirm(c,WorkspaceEnvironments.label(target),tr(c,"Будет установлена базовая версия из Components. Системные пакеты и программы заменятся базовыми; текущее полное окружение останется в снимке. Закройте все его сессии.","The Components baseline will replace system packages and applications. The current complete environment will remain in a checkpoint. Close all its sessions."),EnvironmentUpdateJobs.request("baseline",target),q->{},true);});
  if(image!=null){String release=image.optString("release"),sha=image.optString("sha256");JSONObject origin=row.optJSONObject("origin");boolean same=origin!=null&&sha.equals(origin.optString("sha256"));
   b.addView(text(c,tr(c,"Последний опубликованный образ: ","Latest published base: ")+release+(same?tr(c," · уже используется"," · already in use"):"")));
   action(c,b,tr(c,"Установить этот базовый образ","Install this base image"),!same&&!EnvironmentUpdateJobs.active(),()->{confirm(c,WorkspaceEnvironments.label(target),tr(c,"Это замена системы, а не обычное обновление пакетов. Полное текущее окружение сохранится в снимке. Для установленного XFCE нужен совместимый закреплённый комплект.","This replaces the system rather than upgrading its packages. The complete current environment will be kept in a checkpoint. An installed XFCE needs a matching pinned package set."),EnvironmentUpdateJobs.request("baseline",target),q->q.put("image",new JSONObject().put("channel","latest").put("sha256",sha)),true);});
  }
 }
 static LinearLayout withText(Context c,String s){LinearLayout b=column(c);b.addView(text(c,s));return b;}
 interface Extra{void put(JSONObject q)throws JSONException;}
 static void confirm(Context c,String title,String message,JSONObject request,Extra extra,boolean home){
  LinearLayout b=withText(c,message);CheckBox preserve=new CheckBox(c);preserve.setTextColor(WorkspaceDialogs.TEXT);preserve.setText(tr(c,"Сохранить текущие /root и /home","Keep current /root and /home"));preserve.setChecked(true);
  if(home){b.addView(preserve);b.addView(text(c,tr(c,"Файлы вне этих каталогов не объединяются со старой системой: они остаются в полном снимке текущего окружения.","Files outside these directories are not merged into an older system: they remain in the full checkpoint of the current environment.")));}
  WorkspaceDialogs.builder(c).setTitle(title).setView(scroll(c,b)).setNegativeButton(tr(c,"Отмена","Cancel"),null).setPositiveButton(tr(c,"Продолжить","Continue"),(d,w)->{try{extra.put(request);if(home)request.put("preserveHome",preserve.isChecked());run(c,request);}catch(JSONException e){throw new IllegalStateException(e);}}).show();
 }
 static void snapshots(Context c,JSONArray saved){
  AlertDialog[] dialog={null};LinearLayout b=column(c);
  for(int n=0;n<saved.length();n++){JSONObject s=saved.optJSONObject(n);if(s==null)continue;String id=s.optString("id"),target=s.optString("target");String when=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date((long)(s.optDouble("created")*1000)));
   b.addView(text(c,WorkspaceEnvironments.label(target)+" · "+when));
   action(c,b,tr(c,"Откатить к этому снимку","Restore this checkpoint"),true,()->{dialog[0].dismiss();confirm(c,WorkspaceEnvironments.label(target),tr(c,"Восстановить снимок? Полное текущее окружение тоже сохранится для обратного отката. Закройте его сессии.","Restore this checkpoint? The complete current environment will also be saved for an undo. Close its sessions."),EnvironmentUpdateJobs.request("rollback",target),q->q.put("snapshot",id),true);});
   action(c,b,tr(c,"Удалить снимок","Delete checkpoint"),true,()->confirm(c,tr(c,"Удаление снимка","Delete checkpoint"),tr(c,"Удалить эту резервную копию без возможности восстановления? Активное окружение останется на месте.","Permanently delete this checkpoint? The active environment will remain in place."),EnvironmentUpdateJobs.request("delete-snapshot",target),q->q.put("snapshot",id),false));
  }
  dialog[0]=WorkspaceDialogs.builder(c).setTitle(tr(c,"Снимки и откат","Checkpoints and rollback")).setView(scroll(c,b)).setNegativeButton(tr(c,"Закрыть","Close"),null).create();dialog[0].show();
 }
}
