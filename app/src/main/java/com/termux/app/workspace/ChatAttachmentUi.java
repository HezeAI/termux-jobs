package com.termux.app.workspace;

import android.app.AlertDialog;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Attachment drafts are owned by a chat run, not by the currently visible tab. */
final class ChatAttachmentUi implements AutoCloseable {
    private final TermuxActivity activity;private final AgentChatView view;
    private final WorkspaceLocalFilePicker picker;private final ExecutorService work=Executors.newFixedThreadPool(2);
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LinkedHashMap<String,List<JSONObject>> drafts=new LinkedHashMap<>();
    private final Set<String> preparing=new HashSet<>(),submitted=new HashSet<>();
    void submitted(JSONArray refs){for(int i=0;i<refs.length();i++)submitted.add(refs.optJSONObject(i).optString("id"));}
    private void discard(JSONObject a){if(!submitted.contains(a.optString("id")))ChatAttachments.discard(activity,a);}
    private volatile boolean closed;private String owner="";private WorkspaceStore.Session profile;private boolean supported;
    private AlertDialog viewer;
    ChatAttachmentUi(TermuxActivity activity,AgentChatView view){this.activity=activity;this.view=view;picker=new WorkspaceLocalFilePicker(activity);view.folder.setOnClickListener(v->choose());view.apkDetails=this::apkDetails;view.localFileAction=this::capturePath;view.fileCardAction=this::captureAttachment;}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    void sync(String next,WorkspaceStore.Session p,boolean available){
        boolean changed=!owner.equals(next);if(changed){picker.cancelSelection();}
        owner=next;profile=p;supported=available;view.attachmentPicker(ChatAttachments.supported(p));
        if(changed)render();
    }
    boolean has(){return !drafts.getOrDefault(owner,Collections.emptyList()).isEmpty();}
    boolean busy(){return preparing.contains(owner);}
    JSONArray references(){JSONArray refs=new JSONArray();for(JSONObject a:drafts.getOrDefault(owner,Collections.emptyList())){JSONObject ref=new JSONObject();try{ref.put("id",a.getString("id"));}catch(JSONException ignored){}refs.put(ref);}return refs;}
    void accepted(String run,JSONArray refs){List<JSONObject> list=drafts.get(run);if(list!=null)for(int i=0;i<refs.length();i++){String id=refs.optJSONObject(i).optString("id");list.removeIf(a->a.optString("id").equals(id));}if(run.equals(owner))render();}
    private void choose(){
        if(closed||!ChatAttachments.supported(profile))return;
        if(!supported){Toast.makeText(activity,t("Для вложений перезапустите эту сессию после обновления и дождитесь подключения чата.","For attachments, restart this session after updating and wait for the chat to connect."),Toast.LENGTH_LONG).show();return;}
        if(busy()||drafts.getOrDefault(owner,Collections.emptyList()).size()>=ChatAttachments.MAX_COUNT){Toast.makeText(activity,t("Дождитесь подготовки файла. В одном сообщении — до 4 вложений.","Wait for file preparation. Up to 4 attachments per message."),Toast.LENGTH_LONG).show();return;}
        final String run=owner;final WorkspaceStore.Session destination=profile.copy();
        final int available=ChatAttachments.MAX_COUNT-drafts.getOrDefault(run,Collections.emptyList()).size();
        picker.chooseChatAttachments(view.folder,destination,available,(source,entries)->{
            if(closed||!run.equals(owner))return;List<Staging> jobs=new ArrayList<>();for(ProjectFiles.Entry entry:entries)jobs.add(()->ChatAttachments.stage(activity,source,entry,destination));prepare(run,jobs);
        },()->activity.requestWorkspaceGallery(available,uris->{
            if(closed||!run.equals(owner))return;List<Staging> jobs=new ArrayList<>();for(android.net.Uri uri:uris)jobs.add(()->ChatAttachments.stageUri(activity,uri,destination));prepare(run,jobs);
        }));
    }
    private interface Staging {JSONObject stage()throws Exception;}
    private void prepare(String run,List<Staging> jobs){
        if(closed||jobs.isEmpty()||preparing.contains(run))return;
        if(jobs.size()+drafts.getOrDefault(run,Collections.emptyList()).size()>ChatAttachments.MAX_COUNT){Toast.makeText(activity,t("В одном сообщении — до 4 вложений.","Up to 4 attachments per message."),Toast.LENGTH_LONG).show();return;}
        preparing.add(run);render();
        work.execute(()->{
            List<JSONObject> ready=new ArrayList<>();Exception failure=null;
            for(Staging job:jobs){if(closed||Thread.currentThread().isInterrupted())break;try{ready.add(job.stage());}catch(Exception error){if(failure==null)failure=error;}}
            final Exception error=failure;main.post(()->{
                preparing.remove(run);if(closed){for(JSONObject a:ready)discard(a);return;}
                if(!ready.isEmpty())drafts.computeIfAbsent(run,k->new ArrayList<>()).addAll(ready);
                while(drafts.size()>32){String oldest=drafts.keySet().iterator().next();for(JSONObject a:drafts.remove(oldest))discard(a);}
                if(run.equals(owner)){render();if(error!=null)Toast.makeText(activity,error(error),Toast.LENGTH_LONG).show();}
            });
        });
    }

    private String error(Exception e){String code=e.getMessage();
        if("SUPPORTED_TYPES".equals(code))return t("Выберите изображение, текстовый файл/лог или видео MP4, MKV, WebM, MOV, M4V, 3GP.","Choose an image, text/log file, or MP4, MKV, WebM, MOV, M4V, 3GP video.");
        if("FILE_LIMIT".equals(code))return t("Файл пуст или слишком большой: изображения до 100 МБ, остальные файлы до 5 ГБ.","The file is empty or too large: images up to 100 MB, other files up to 5 GB.");
        return t("Не удалось подготовить вложение. Проверьте доступ, формат файла и свободное место.","Could not prepare the attachment. Check access, file format and free space.");
    }
    private void render(){
        view.attachmentDrafts.removeAllViews();
        if(busy())view.attachmentDrafts.addView(view.label(t("Подготовка вложения…","Preparing attachment…"),12));
        for(JSONObject a:drafts.getOrDefault(owner,Collections.emptyList())){
            LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name=view.label(a.optString("name")+(a.optBoolean("imagePreviewUnavailable")?t(" · без предпросмотра, отправится как файл"," · no preview, sent as a file"):"")+(a.optString("type").equals("video")?t(" · до 12 кадров, без звука"," · up to 12 frames, no audio"):""),12);name.setMaxLines(2);name.setOnClickListener(v->open(a));row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            TextView remove=view.pill("×",()->{List<JSONObject> list=drafts.get(owner);if(list!=null&&list.remove(a)){discard(a);render();}});remove.setTextColor(Color.TRANSPARENT);android.graphics.drawable.LayerDrawable cross=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{activity.getDrawable(com.termux.R.drawable.workspace_attachment_remove)});cross.setLayerSize(0,view.dp(18),view.dp(18));cross.setLayerGravity(0,Gravity.CENTER);remove.setForeground(cross);remove.setContentDescription(t("Убрать вложение","Remove attachment"));row.addView(remove);view.attachmentDrafts.addView(row);
        }
        view.attachmentDrafts.setVisibility(busy()||has()?View.VISIBLE:View.GONE);
        view.scaleContent();
    }
    private final java.util.ArrayDeque<java.lang.ref.WeakReference<ImageView>> thumbnails=new java.util.ArrayDeque<>();
    void thumbnail(JSONObject a,ImageView target){
        final Object token=new Object();target.setTag(token);thumbnails.add(new java.lang.ref.WeakReference<>(target));while(thumbnails.size()>12){ImageView old=thumbnails.removeFirst().get();if(old!=null){old.setTag(null);old.setImageDrawable(null);}}
        final String run=owner;final WorkspaceStore.Session p=profile;if(p==null)return;
        JSONObject image=a;
        if(a.optString("type").equals("video")){JSONArray frames=a.optJSONArray("frames");if(frames==null||frames.length()==0)return;try{image=new JSONObject().put("path",frames.getJSONObject(0).getString("path"));}catch(JSONException e){return;}}
        if(!a.optString("type").equals("image")&&!a.optString("type").equals("video"))return;
        final JSONObject requested=image;work.execute(()->{Bitmap bitmap=null;try{bitmap=ChatAttachments.bitmap(ChatAttachments.materialize(activity,p,requested),320);}catch(Exception ignored){}final Bitmap b=bitmap;main.post(()->{if(!closed&&run.equals(owner)&&target.getTag()==token&&target.getParent()!=null&&b!=null)target.setImageBitmap(b);});});
    }
    private void apkDetails(JSONObject a,TextView target){
        final Object token=new Object();target.setTag(token);final String run=owner;final WorkspaceStore.Session p=profile;if(p==null||closed)return;
        work.execute(()->{String info;try{File file=ChatAttachments.materialize(activity,p,a);String hash=ChatAttachments.fileSha256(file);info=android.text.format.Formatter.formatFileSize(activity,file.length())+"\nSHA-256: "+hash;}catch(Exception e){info=t("Файл недоступен","File unavailable");}
            final String text=info;main.post(()->{if(!closed&&run.equals(owner)&&target.getTag()==token&&target.getParent()!=null)target.setText(text);});});
    }
    static WorkspaceStore.Session source(WorkspaceStore.Session p){
        if(p==null)return null;WorkspaceStore.Session copy=p.copy();copy.id=p.id;copy.handle=p.handle;copy.codexSocketId=p.codexSocketId;return copy;
    }
    io.noties.markwon.Markwon markdown(){final WorkspaceStore.Session captured=source(profile);return AgentMarkdown.create(activity,path->()->openPath(captured,path));}
    Runnable capturePath(String url){final WorkspaceStore.Session captured=source(profile);return ()->openPath(captured,url);}
    Runnable captureAttachment(JSONObject attachment){final WorkspaceStore.Session captured=source(profile);final JSONObject copy;try{copy=new JSONObject(attachment.toString());}catch(JSONException error){return null;}return ()->openFor(captured,copy);}
    static String localPath(String url){
        if(url==null||!url.startsWith("/")||url.startsWith("//"))throw new IllegalArgumentException("INVALID_PATH");
        String path=android.net.Uri.decode(url);for(int i=0;i<path.length();i++)if(Character.isISOControl(path.charAt(i)))throw new IllegalArgumentException("INVALID_PATH");
        return java.nio.file.Paths.get(path).normalize().toString();
    }
    private void openPath(WorkspaceStore.Session source,String url){
        try{String path=localPath(url),name=new File(path).getName();JSONObject a=new JSONObject().put("path",path).put("name",name).put("type",ChatAttachments.type(name)).put("localFileLink",true);openFor(source,a);}
        catch(Exception error){Toast.makeText(activity,t("Не удалось открыть файл: некорректный путь.","Cannot open file: invalid path."),Toast.LENGTH_LONG).show();}
    }
    void openProjectFile(WorkspaceStore.Session captured,JSONObject file){openFor(source(captured),file);}
    AlertDialog projectViewer(){return viewer;}
    void open(JSONObject a){openFor(source(profile),a);}
    private void openFor(WorkspaceStore.Session p,JSONObject input){
        final JSONObject a;try{a=new JSONObject(input.toString());}catch(Exception invalid){Toast.makeText(activity,t("Не удалось открыть файл: некорректные сведения.","Cannot open file: invalid file details."),Toast.LENGTH_LONG).show();return;}
        if(closed)return;if(p==null){Toast.makeText(activity,t("Среда исходного чата недоступна.","The originating chat environment is unavailable."),Toast.LENGTH_LONG).show();return;}if(viewer!=null)viewer.dismiss();
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);TextView loading=view.label(t("Загрузка…","Loading…"),13);box.addView(loading);
        final boolean apk=a.optString("type").equals("apk"),binary=a.optString("type").equals("binary");
        final boolean gallery=!apk&&ImageGalleryExport.candidate(a.optString("name"),a.optString("type"));
        AlertDialog.Builder builder=WorkspaceDialogs.builder(activity).setTitle(a.optString("name",t("Вложение","Attachment"))).setView(box);
        if(apk)builder.setNeutralButton(t("В Download","To Download"),null).setPositiveButton(t("Установить","Install"),null);
        else {builder.setPositiveButton(t("Сохранить в Download","Save to Download"),null);if(gallery)builder.setNeutralButton(t("В галерею","To gallery"),null);else if(!binary&&!a.optString("type").equals("image"))builder.setNegativeButton(t("Закрыть","Close"),null);}
        AlertDialog dialog=builder.create();viewer=dialog;dialog.setCanceledOnTouchOutside(true);dialog.show();dialog.getWindow().setLayout(Math.round(activity.getResources().getDisplayMetrics().widthPixels*.94f),android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        final int saveButton=apk?AlertDialog.BUTTON_NEUTRAL:AlertDialog.BUTTON_POSITIVE;
        dialog.getButton(saveButton).setEnabled(false);if(gallery){dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);for(int which:new int[]{AlertDialog.BUTTON_NEUTRAL,AlertDialog.BUTTON_POSITIVE}){android.widget.Button imageAction=dialog.getButton(which);imageAction.setSingleLine(false);imageAction.setMaxLines(Integer.MAX_VALUE);imageAction.setEllipsize(null);imageAction.setAllCaps(false);imageAction.setMinWidth(view.dp(48));imageAction.setMinHeight(view.dp(48));imageAction.setPadding(view.dp(8),imageAction.getPaddingTop(),view.dp(8),imageAction.getPaddingBottom());}}if(apk)dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        work.execute(()->{File file=null;Bitmap bitmap=null;String text="";int mediaWidth=0,mediaHeight=0;Exception failure=null;try{
            if(a.optBoolean("localFileLink")&&!a.optString("path").startsWith(ChatAttachments.prefix(p))){try(ProjectFiles files=new ProjectFiles(activity,p)){a.put("cacheKey",files.fingerprint(a.getString("path")));}}
            if(ChatFileViewer.handles(a.optString("name")))a.put("originalPreview",true);
            file=ChatAttachments.materialize(activity,p,a);String type=a.optString("type");
            if(ChatFileViewer.handles(a.optString("name"))){new ChatFileViewer.Document(file,ChatFileViewer.html(a.optString("name")));}
            else if(type.equals("image")){bitmap=ChatAttachments.bitmap(file,2048);mediaWidth=bitmap.getWidth();mediaHeight=bitmap.getHeight();}
            else if(type.equals("video")){android.media.MediaMetadataRetriever metadata=new android.media.MediaMetadataRetriever();try{metadata.setDataSource(file.getPath());mediaWidth=Integer.parseInt(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));mediaHeight=Integer.parseInt(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));String rotation=metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);if("90".equals(rotation)||"270".equals(rotation)){int swap=mediaWidth;mediaWidth=mediaHeight;mediaHeight=swap;}}finally{metadata.release();}}
            else if(type.equals("apk"))text=android.text.format.Formatter.formatFileSize(activity,file.length())+"\nSHA-256: "+ChatAttachments.fileSha256(file)+t("\n\nХеш описывает этот файл, но не подтверждает доверие к его автору. Установку подтверждает Android.","\n\nThe hash identifies this file; it does not establish publisher trust. Android confirms installation.");
            else if(type.equals("binary"))text=ChatAttachments.badge(a.optString("name"))+" · "+android.text.format.Formatter.formatFileSize(activity,file.length());
            else if(type.equals("file"))text=ChatAttachments.textPreview(file,65536)+(file.length()>65536?t("\n\nПоказаны первые 64 КБ. Полный файл сохранён во вложении.","\n\nShowing the first 64 KB. The complete file remains attached."):"");
        }catch(Exception e){failure=e;}final File ready=file;final Bitmap image=bitmap;final String content=text;final Exception error=failure;final int originalWidth=mediaWidth,originalHeight=mediaHeight;
            main.post(()->{if(closed||!dialog.isShowing())return;box.removeAllViews();if(gallery&&ready!=null&&ready.isFile())galleryAction(dialog,ready,a.optString("name","image"));if(error!=null){
                    if(ready!=null&&ready.isFile()){
                        box.addView(view.label("PREVIEW_LIMIT".equals(error.getMessage())?t("Предпросмотр ограничен: HTML до 32 МиБ, GIF до 64 МиБ. Оригинал можно сохранить.","Preview limits: HTML up to 32 MiB, GIF up to 64 MiB. You can save the original."):t("Предпросмотр недоступен: формат не поддерживается или файл повреждён. Оригинал можно сохранить.","Preview unavailable: unsupported format or damaged file. You can save the original."),14));
                        dialog.getButton(saveButton).setEnabled(true);dialog.getButton(saveButton).setOnClickListener(v->activity.requestWorkspaceStorageAccess(()->save(ready,a.optString("name","attachment"),dialog,saveButton)));
                    }else box.addView(view.label(t("Вложение недоступно: исходный файл удалён или нет доступа.","Attachment unavailable: the source was removed or access is missing."),14));return;
                }
                if(ChatFileViewer.handles(a.optString("name"))){
                    try{
                        ChatFileViewer.Document document=new ChatFileViewer.Document(ready,ChatFileViewer.html(a.optString("name")));
                        final boolean[] viewerError={false};android.webkit.WebView web=ChatFileViewer.create(activity,document,code->{if(!dialog.isShowing())return;if(!code.equals("LINK_BLOCKED")&&!viewerError[0]){viewerError[0]=true;box.addView(view.label(t("Просмотрщик не смог открыть файл. Оригинал можно сохранить.","The viewer could not open the file. You can save the original."),14));}Toast.makeText(activity,code.equals("LINK_BLOCKED")?t("Внешние ссылки и доступ к другим файлам в этом предпросмотре отключены.","External links and access to other files are disabled in this preview."):t("Просмотрщик не смог открыть файл. Оригинал можно сохранить.","The viewer could not open the file. You can save the original."),Toast.LENGTH_LONG).show();});
                        box.addView(web,new LinearLayout.LayoutParams(-1,Math.max(view.dp(120),Math.min(view.dp(420),Math.round(activity.getResources().getDisplayMetrics().heightPixels*.55f)))));
                        dialog.setOnDismissListener(d->ChatFileViewer.release(web));
                    }catch(Exception unavailable){box.addView(view.label(t("HTML/GIF-просмотрщик недоступен. Проверьте Android System WebView. Оригинал можно сохранить.","HTML/GIF viewer unavailable. Check Android System WebView. You can save the original."),14));}
                }
                else if(image!=null){ZoomImage img=new ZoomImage(activity);img.setImageBitmap(image);box.addView(img,compactMedia(dialog,originalWidth,originalHeight));}
                else if(a.optString("type").equals("video")){VideoView video=new VideoView(activity);MediaController controls=new MediaController(activity);controls.setAnchorView(video);video.setMediaController(controls);video.setVideoPath(ready.getPath());box.addView(video,compactMedia(dialog,originalWidth,originalHeight));box.addView(view.label(t("Агенту передаются выбранные кадры с временными метками, без звука.","The agent receives sampled timestamped frames, without audio."),12));dialog.setOnDismissListener(d->video.stopPlayback());video.start();}
                else if(apk){TextView details=view.label(content,12);details.setTextIsSelectable(true);box.addView(details);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->install(ready));}
                else if(binary){box.addView(view.label(t("Для этого формата доступно сохранение оригинала.","You can save the original; this format has no preview."),12));if(a.optBoolean("imagePreviewUnavailable"))box.addView(view.label(t("Изображение не декодируется на этом устройстве. Отправляется исходный файл; просмотр моделью не гарантирован.","This device cannot decode the image. The original is sent as a file; model viewing is not guaranteed."),12));TextView body=view.label(content,13);body.setTextIsSelectable(true);box.addView(body);}
                else {ScrollView scroll=new ScrollView(activity);TextView body=view.label(content,13);body.setTextIsSelectable(true);scroll.addView(body);box.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(view.dp(420),Math.round(activity.getResources().getDisplayMetrics().heightPixels*.60f))));}
                dialog.getButton(saveButton).setEnabled(true);dialog.getButton(saveButton).setOnClickListener(v->activity.requestWorkspaceStorageAccess(()->save(ready,a.optString("name","attachment"),dialog,saveButton)));
            });
        });
    }
    private void install(File apk){
        try{
            if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
                WorkspaceDialogs.builder(activity).setTitle(t("Разрешение на установку","Installation permission"))
                    .setMessage(t("Разрешите установку из Termux Job’s в настройках Android, затем вернитесь и снова нажмите «Установить».","Allow installs from Termux Job’s in Android settings, then return and tap Install again."))
                    .setPositiveButton(t("Настройки Android","Android settings"),(d,w)->{try{activity.startActivity(new android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,android.net.Uri.parse("package:"+activity.getPackageName())));}catch(Exception e){Toast.makeText(activity,t("Не удалось открыть настройки","Could not open settings"),Toast.LENGTH_LONG).show();}})
                    .setNegativeButton(t("Отмена","Cancel"),null).show();return;
            }
            activity.startActivity(installIntent(activity,apk));
        }catch(Exception e){Toast.makeText(activity,t("Не удалось открыть установщик APK","Could not open the APK installer"),Toast.LENGTH_LONG).show();}
    }
    static android.content.Intent installIntent(android.content.Context context,File apk){
        android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(context,context.getPackageName()+".workspace.files",apk);
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(android.content.ClipData.newRawUri("APK",uri));return intent;
    }
    static int[] mediaBounds(int screenWidth,int screenHeight,int width,int height,float density){
        int maxWidth=Math.max(1,Math.round(screenWidth*.94f-48*density)),maxHeight=Math.max(1,Math.round(screenHeight*.82f-144*density));
        if(width<=0||height<=0){width=16;height=9;}
        double scale=Math.min((double)maxWidth/width,(double)maxHeight/height);
        return new int[]{Math.max(1,(int)Math.round(width*scale)),Math.max(1,(int)Math.round(height*scale))};
    }
    private LinearLayout.LayoutParams compactMedia(AlertDialog dialog,int width,int height){
        android.util.DisplayMetrics display=activity.getResources().getDisplayMetrics();int[] size=mediaBounds(display.widthPixels,display.heightPixels,width,height,display.density);
        int window=Math.min(Math.round(display.widthPixels*.94f),Math.max(view.dp(280),size[0]+view.dp(48)));dialog.getWindow().setLayout(window,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(size[0],size[1]);params.gravity=Gravity.CENTER_HORIZONTAL;return params;
    }
    private void galleryAction(AlertDialog dialog,File original,String name){
        android.widget.Button button=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);button.setEnabled(true);
        button.setOnClickListener(v->{if(closed||!dialog.isShowing()||!button.isEnabled())return;button.setEnabled(false);
            activity.requestWorkspaceImageSave(()->{if(closed||!dialog.isShowing())return;button.setText(t("Сохранение…","Saving…"));work.execute(()->{
                ImageGalleryExport.Result result=null;String code="SAVE_FAILED";
                try{result=ImageGalleryExport.save(activity,original,name,()->closed);}catch(ImageGalleryExport.Failure failed){code=failed.code;}catch(Exception ignored){}
                final ImageGalleryExport.Result saved=result;final String failure=code;
                main.post(()->{if(closed)return;if(dialog.isShowing()){button.setEnabled(true);button.setText(t("В галерею","To gallery"));}
                    String message=saved!=null?t("Сохранено: ","Saved: ")+ImageGalleryExport.ALBUM+saved.name:failure.equals("UNSUPPORTED_IMAGE")?t("Не удалось сохранить этот формат как изображение в медиатеку. Оригинал можно сохранить в Download.","This format could not be saved as an image in the media library. You can save the original to Download."):failure.equals("CLEANUP_FAILED")?t("Не удалось сохранить изображение и подтвердить удаление незавершённой записи.","Could not save the image or confirm removal of its unfinished media entry."):t("Не удалось сохранить изображение. Оригинал можно сохранить в Download.","Could not save the image. You can save the original to Download.");
                    Toast.makeText(activity,message,Toast.LENGTH_LONG).show();});
            });},()->{if(!closed&&dialog.isShowing()){button.setEnabled(true);Toast.makeText(activity,t("Доступ для сохранения изображения не предоставлен.","Permission to save the image was not granted."),Toast.LENGTH_LONG).show();}});
        });
    }
    private void save(File source,String name,AlertDialog dialog,int saveButton){
        dialog.getButton(saveButton).setEnabled(false);work.execute(()->{boolean ok=false;try{
            saveAttachment(source,new File("/sdcard/Download"),name,()->closed);
            ok=true;
        }catch(Exception e){}final boolean saved=ok;main.post(()->{if(closed)return;if(dialog.isShowing())dialog.getButton(saveButton).setEnabled(true);Toast.makeText(activity,saved?t("Сохранено в Download","Saved to Download"):t("Не удалось сохранить файл","Could not save the file"),Toast.LENGTH_LONG).show();});});
    }
    static File saveAttachment(File source,File directory,String name,BooleanSupplier cancelled)throws IOException {
        String safe=name.replaceAll("[\\\\/\\p{Cntrl}]","_");
        if(safe.equals(".")||safe.equals("..")||safe.isEmpty())safe="attachment";
        if(safe.length()>150)safe=safe.substring(0,150);
        try(InputStream input=new FileInputStream(source)){
            return ProjectFileExport.save(input,directory,safe,Long.MAX_VALUE,cancelled);
        }
    }
    @Override public void close(){closed=true;picker.close();if(viewer!=null)viewer.dismiss();for(List<JSONObject> list:drafts.values())for(JSONObject a:list)discard(a);drafts.clear();work.shutdownNow();}
    /** Fit initially, pinch to zoom and drag without recreating the chat surface. */
    static final class ZoomImage extends androidx.appcompat.widget.AppCompatImageView {
        private final Matrix transform=new Matrix();private float x,y,downX,downY;private boolean tap;private final ScaleGestureDetector scale;
        ZoomImage(android.content.Context c){super(c);setScaleType(ScaleType.MATRIX);scale=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){transform.postScale(d.getScaleFactor(),d.getScaleFactor(),d.getFocusX(),d.getFocusY());setImageMatrix(transform);return true;}});}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);if(getDrawable()!=null){float s=Math.min((float)w/getDrawable().getIntrinsicWidth(),(float)h/getDrawable().getIntrinsicHeight());transform.setScale(s,s);transform.postTranslate((w-getDrawable().getIntrinsicWidth()*s)/2,(h-getDrawable().getIntrinsicHeight()*s)/2);setImageMatrix(transform);}}
        @Override public boolean performClick(){super.performClick();return true;}
        @Override public boolean onTouchEvent(MotionEvent e){
            scale.onTouchEvent(e);
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();tap=true;}
            if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_CANCEL||scale.isInProgress())tap=false;
            if(action==MotionEvent.ACTION_MOVE){
                int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if(Math.abs(e.getX()-downX)>slop||Math.abs(e.getY()-downY)>slop)tap=false;
                if(!scale.isInProgress()){transform.postTranslate(e.getX()-x,e.getY()-y);setImageMatrix(transform);}
            }
            if(action==MotionEvent.ACTION_UP){if(tap)performClick();tap=false;}
            x=e.getX();y=e.getY();return true;
        }
    }
}
