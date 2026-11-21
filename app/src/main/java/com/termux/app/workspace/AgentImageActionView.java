package com.termux.app.workspace;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Collections;

/** Image inspection is its own event, with a collapsible thumbnail. */
final class AgentImageActionView extends LinearLayout {
    private final AgentChatView owner;
    final TextView heading;
    final ImageView image;
    final ImageView thumbnail;
    private JSONObject row;
    private JSONObject attachment;
    private boolean expanded;

    AgentImageActionView(AgentChatView owner) {
        super(owner.getContext());this.owner=owner;setOrientation(VERTICAL);
        heading=owner.label("",13);heading.setTextColor(WorkspaceDialogs.MUTED);
        heading.setMinHeight(owner.dp(48));heading.setSingleLine(true);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout header=new LinearLayout(getContext());header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        thumbnail=new ImageView(getContext());thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setContentDescription(owner.tr("Открыть изображение","Open image"));
        LayoutParams small=new LayoutParams(owner.dp(28),owner.dp(28));small.setMargins(0,0,owner.dp(8),0);
        header.addView(thumbnail,small);header.addView(heading,new LayoutParams(0,-2,1));
        addView(header,new LayoutParams(-1,-2));
        thumbnail.setOnClickListener(v->{if(attachment!=null)owner.openImage(attachment);});
        image=new ImageView(getContext());image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(owner.tr("Открыть изображение","Open image"));
        LayoutParams bounds=new LayoutParams(owner.dp(96),owner.dp(96));bounds.setMargins(owner.dp(10),0,0,owner.dp(8));
        addView(image,bounds);
        heading.setOnClickListener(v->{owner.preserveTimelinePosition();expanded=!expanded;owner.rememberActions(Collections.singletonList(row),expanded);refresh();});
        image.setOnClickListener(v->{if(attachment!=null)owner.openImage(attachment);});
    }

    void update(JSONObject next) {
        row=next;JSONObject preview=AgentActionPresentation.preview(row);
        try {attachment=preview==null?null:new JSONObject(preview.getJSONObject("image").toString());
            if(attachment!=null&&attachment.optString("cacheKey").isEmpty())attachment.put("cacheKey",row.optString("key"));
        } catch(org.json.JSONException invalid) {attachment=null;}
        Boolean choice=owner.actionChoice(new JSONArray().put(row));if(choice!=null)expanded=choice;
        thumbnail.setTag(null);image.setTag(null);
        thumbnail.setImageResource(com.termux.R.drawable.workspace_image_frame);
        image.setImageResource(com.termux.R.drawable.workspace_image_frame);
        if(attachment!=null){owner.thumbnail.accept(attachment,thumbnail);owner.thumbnail.accept(attachment,image);}
        refresh();
    }

    private void refresh() {
        String title=CodexActionTimeline.failed(row)?AgentActionPresentation.state(row,WorkspaceLanguage.code(getContext()).equals("ru")):
            CodexActionTimeline.running(row)?owner.tr("Просматривает изображение","Viewing image"):
            CodexActionTimeline.status(row).equals("completed")?owner.tr("Просмотрено изображение","Viewed image"):
            owner.tr("Просмотр изображения","View image")+" · "+AgentActionPresentation.state(row,WorkspaceLanguage.code(getContext()).equals("ru"));
        heading.setText(title+(expanded?" ▴":" ▾"));
        heading.setContentDescription(title+". "+owner.tr(expanded?"Свернуть":"Развернуть",expanded?"Collapse":"Expand"));
        image.setVisibility(expanded?VISIBLE:GONE);
    }
}
