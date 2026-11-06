package com.termux.app.workspace;

import com.termux.shared.terminal.io.extrakeys.ExtraKeysView;
import android.graphics.drawable.GradientDrawable;

/** Same native keyboard appearance in ordinary and agent terminals. */
public final class WorkspaceExtraKeysStyle {
    private WorkspaceExtraKeysStyle(){}
    public static void apply(ExtraKeysView keys){
        keys.setBackgroundColor(android.graphics.Color.BLACK);
        keys.setButtonColors(WorkspaceDialogs.TEXT,WorkspaceDialogs.ACCENT,android.graphics.Color.BLACK,WorkspaceDialogs.TINT);
        keys.setButtonBackgroundFactory(()->{
            GradientDrawable shape=new GradientDrawable();shape.setColor(android.graphics.Color.BLACK);shape.setCornerRadius(WorkspaceDialogs.dp(keys.getContext(),6));shape.setStroke(WorkspaceDialogs.dp(keys.getContext(),1),WorkspaceDialogs.LINE);
            return WorkspaceRipple.background(keys.getContext(),shape,6);
        });
    }
}
