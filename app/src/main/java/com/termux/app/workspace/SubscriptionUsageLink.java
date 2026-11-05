package com.termux.app.workspace;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Browser navigation only: never reads credentials or changes provider/session state. */
final class SubscriptionUsageLink extends LinearLayout {
    static final String CLAUDE = "https://claude.ai/settings/usage";
    static final String CODEX = "https://chatgpt.com/codex/settings/usage";
    private final TextView subscription, button;
    private final int gap;

    SubscriptionUsageLink(Context context, String label, boolean claude) {
        super(context);
        setTag("subscription-usage-row");
        setGravity(Gravity.CENTER_VERTICAL);
        gap = WorkspaceDialogs.dp(context, 8);
        subscription = new TextView(context);
        subscription.setTag("subscription-label");
        subscription.setText(label);
        subscription.setTextSize(14);
        subscription.setTextColor(WorkspaceDialogs.TEXT);
        addView(subscription, new LayoutParams(0, -2, 1));
        button = WorkspaceDialogs.action(context, AgentStatusUi.tr(context,
                "Сброс лимита ↗", "Reset limit ↗"), 0);
        button.setTag("subscription-usage-link");
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER);
        String url = claude ? CLAUDE : CODEX;
        button.setContentDescription(button.getText() + ". " + url);
        // Reuse the existing external HTTPS/browser chooser; no return callback.
        button.setOnClickListener(v -> WorkspaceDocumentationUi.openBrowser(
                (android.app.Activity) context, url));
        addView(button, new LayoutParams(-2, -2));
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
        int free = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        subscription.measure(free, free);
        button.measure(free, free);
        boolean wrap = MeasureSpec.getMode(widthSpec) != MeasureSpec.UNSPECIFIED
                && subscription.getMeasuredWidth() + button.getMeasuredWidth() + gap > width;
        setOrientation(wrap ? VERTICAL : HORIZONTAL);
        LayoutParams name = (LayoutParams) subscription.getLayoutParams();
        name.width = wrap ? -1 : 0;
        name.weight = wrap ? 0 : 1;
        LayoutParams action = (LayoutParams) button.getLayoutParams();
        action.width = wrap ? -1 : -2;
        action.leftMargin = wrap ? 0 : gap;
        action.topMargin = wrap ? gap : 0;
        super.onMeasure(widthSpec, heightSpec);
    }

    void subscription(String label) { subscription.setText(label); }

    static SubscriptionUsageLink add(LinearLayout parent, String label, boolean claude) {
        SubscriptionUsageLink row = new SubscriptionUsageLink(parent.getContext(), label, claude);
        parent.addView(row,
                new LayoutParams(-1, -2));
        AgentStatusUi.info(parent, AgentStatusUi.tr(parent.getContext(),
                "Доступность и условия сброса указаны на сайте",
                "Reset availability and conditions are shown on the website"));
        return row;
    }
}
