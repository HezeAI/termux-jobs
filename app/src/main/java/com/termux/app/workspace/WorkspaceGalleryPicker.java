package com.termux.app.workspace;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;
import java.util.*;
import java.util.function.Consumer;

/** Native media selection; grants only the URIs explicitly picked by the user. */
public final class WorkspaceGalleryPicker {
    private final Activity activity;
    private Consumer<List<Uri>> pending;
    private int request=24000,limit;
    private boolean closed;
    public WorkspaceGalleryPicker(Activity activity){this.activity=activity;}
    static Intent intent(int limit,boolean photoPicker){
        if(photoPicker){Intent i=new Intent(MediaStore.ACTION_PICK_IMAGES);if(limit>1)i.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX,limit);return i;}
        return new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"}).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,limit>1).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
    public void pick(int count,Consumer<List<Uri>> selected){
        if(closed||pending!=null)return;limit=Math.max(1,Math.min(ChatAttachments.MAX_COUNT,count));pending=selected;request++;if(request>28000)request=24001;
        try{activity.startActivityForResult(intent(limit,Build.VERSION.SDK_INT>=33),request);}
        catch(ActivityNotFoundException e){try{activity.startActivityForResult(intent(limit,false),request);}catch(ActivityNotFoundException unavailable){Consumer<List<Uri>> callback=pending;pending=null;callback.accept(Collections.emptyList());}}
    }
    static List<Uri> selection(Intent data,int limit){
        LinkedHashSet<Uri> items=new LinkedHashSet<>();
        if(data!=null){if(data.getData()!=null)items.add(data.getData());ClipData clips=data.getClipData();if(clips!=null){if(clips.getItemCount()>ChatAttachments.MAX_COUNT)throw new IllegalArgumentException("MEDIA_LIMIT");for(int i=0;i<clips.getItemCount();i++)items.add(clips.getItemAt(i).getUri());}}
        if(items.size()>limit)throw new IllegalArgumentException("MEDIA_LIMIT");
        for(Uri uri:items)if(uri==null||!"content".equals(uri.getScheme()))throw new IllegalArgumentException("MEDIA_URI");
        return new ArrayList<>(items);
    }
    public boolean result(int code,int result,Intent data){
        if(code!=request)return false;Consumer<List<Uri>> callback=pending;pending=null;
        if(closed||callback==null)return true;
        List<Uri> items;try{items=result==Activity.RESULT_OK?selection(data,limit):Collections.emptyList();}catch(IllegalArgumentException error){items=Collections.emptyList();Toast.makeText(activity,WorkspaceLanguage.code(activity).equals("ru")?"Выберите фото или видео: до "+limit+" вложений.":"Choose photos or videos: up to "+limit+" attachments.",Toast.LENGTH_LONG).show();}
        callback.accept(items);return true;
    }
    public void close(){closed=true;pending=null;}
}
