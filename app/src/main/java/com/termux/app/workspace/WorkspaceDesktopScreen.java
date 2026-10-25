package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.termux.x11.X11ScreenSettings;

/** Saved X11 geometry only; never restarts a desktop or changes system display settings. */
final class WorkspaceDesktopScreen {
    static String label(Context c,String target){
        X11ScreenSettings s=X11ScreenSettings.load(c,target);
        return tr(c,"Экран X11: ","X11 screen: ")+(s.automatic()?tr(c,"Авто","Auto"):s.effectiveWidth()+" × "+s.height);
    }
    interface Apply {void run(X11ScreenSettings settings,java.util.function.Consumer<String> done);}
    static void show(Context c,String target,Runnable saved,Runnable back){show(c,target,saved,back,(settings,done)->done.accept(null));}
    static void show(Context c,String target,Runnable saved,Runnable back,Apply apply){
        X11ScreenSettings s=X11ScreenSettings.load(c,target);
        LinearLayout body=AgentStatusUi.body(c);
        CheckBox automatic=new CheckBox(c);automatic.setText(tr(c,"Автонастройка","Auto setup"));automatic.setTextColor(WorkspaceDialogs.TEXT);automatic.setTag("x11-screen-auto");automatic.setChecked(s.automatic()&&s.horizontalDp==0&&s.verticalDp==0&&s.scalePercent==100);body.addView(automatic);
        LinearLayout dimensions=new LinearLayout(c);
        EditText width=number(dimensions,tr(c,"Ширина, px","Width, px"),"x11-screen-width",s.automatic()?1600:s.width);
        EditText height=number(dimensions,tr(c,"Высота, px","Height, px"),"x11-screen-height",s.automatic()?720:s.height);
        body.addView(dimensions);
        AgentStatusUi.info(body,tr(c,"Размер сохраняется для этого окружения. Вручную: 320–4096 × 200–4096, не более 8 Мп. X11 выравнивает ширину по 8 пикселей: 1300 → 1296. Разрешение поворачивается вместе с телефоном. Изображение растягивается на всю область рабочего стола, кроме заданных ниже отступов.","Saved for this environment. Manual: 320–4096 × 200–4096, up to 8 MP. X11 aligns width to 8 pixels: 1300 → 1296. The resolution rotates with the phone. The image stretches to fill the desktop area, apart from the margins below."));
        LinearLayout scaling=new LinearLayout(c);
        EditText scale=number(scaling,tr(c,"Масштаб интерфейса, %","Interface scale, %"),"x11-screen-scale",s.scalePercent);body.addView(scaling);
        AgentStatusUi.info(body,tr(c,"50–150%. Меньше — мельче шрифты, панели, значки и новые окна файлового менеджера. Применяется по кнопке «Применить». Некоторые открытые программы потребуется открыть заново. 100% — исходный размер. При отключении авто подставляется 1600×720 и 75%.","50–150%. Lower values reduce fonts, panels, icons and new file manager windows. Applied with Apply. Some open applications may need to be reopened. 100% restores the original size. Disabling auto fills in 1600×720 and 75%."));
        AgentStatusUi.info(body,tr(c,"Отступы с каждой стороны (dp)","Margins on each side (dp)"));
        LinearLayout margins=new LinearLayout(c);
        EditText horizontal=number(margins,tr(c,"Слева / справа","Left / right"),"x11-screen-horizontal",s.horizontalDp);
        EditText vertical=number(margins,tr(c,"Сверху / снизу","Top / bottom"),"x11-screen-vertical",s.verticalDp);
        body.addView(margins);
        AgentStatusUi.info(body,tr(c,"От 0 до 96 dp. Защищают края от скруглений и вырезов; на маленьком окне ограничиваются четвертью его размера. Настройка действует в окне и на полном экране, без остановки стола.","0–96 dp. Keep content away from rounded corners and cutouts; limited to a quarter of a small window. Applied in embedded and full-screen views without restarting the desktop."));
        AgentStatusUi.info(body,tr(c,"Автонастройка возвращает автоматическое разрешение, масштаб 100% и нулевые отступы. Ручные поля недоступны. Изменения применяются по кнопке «Применить».","Auto setup restores automatic resolution, 100% scale and zero margins. Manual fields are disabled. Changes take effect with Apply."));
        java.util.function.Consumer<Boolean> automaticChanged=on->{
            if(on){width.setText("");height.setText("");width.setHint(tr(c,"Авто","Auto"));height.setHint(tr(c,"Авто","Auto"));scale.setText(String.format(java.util.Locale.ROOT,"%d",100));horizontal.setText(String.format(java.util.Locale.ROOT,"%d",0));vertical.setText(String.format(java.util.Locale.ROOT,"%d",0));}
            else if(width.getText().toString().trim().isEmpty()||height.getText().toString().trim().isEmpty()){
                EditText[] fields={width,height,scale,horizontal,vertical};int[] values={1600,720,75,5,5};
                for(int i=0;i<fields.length;i++)fields[i].setText(String.format(java.util.Locale.ROOT,"%d",values[i]));
            }
            for(EditText field:new EditText[]{width,height,scale,horizontal,vertical}){field.setEnabled(!on);((View)field.getParent()).setAlpha(on?.4f:1f);if(on)field.clearFocus();}
        };
        automatic.setOnCheckedChangeListener((b,on)->automaticChanged.accept(on));automaticChanged.accept(automatic.isChecked());
        TextView error=AgentStatusUi.info(body,"");error.setTextColor(WorkspaceDialogs.DANGER);error.setTag("x11-screen-error");error.setVisibility(View.GONE);
        AlertDialog dialog=WorkspaceDialogs.builder(c).setTitle(tr(c,"Экран X11","X11 screen")).setView(AgentStatusUi.scroll(body))
            .setNegativeButton(tr(c,"Отмена","Cancel"),(d,w)->back.run()).setPositiveButton(tr(c,"Применить","Apply"),null).create();
        dialog.setOnCancelListener(d->back.run());dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                X11ScreenSettings next=automatic.isChecked()?X11ScreenSettings.defaults():new X11ScreenSettings(parse(width),parse(height),parse(horizontal),parse(vertical),parse(scale));
                dialog.getButton(-1).setEnabled(false);dialog.getButton(-2).setEnabled(false);dialog.getButton(-1).setText(tr(c,"Применяется…","Applying…"));dialog.setCancelable(false);error.setVisibility(View.GONE);automatic.setEnabled(false);for(EditText field:new EditText[]{width,height,scale,horizontal,vertical})field.setEnabled(false);
                apply.run(next,message->{
                    if(!dialog.isShowing())return;
                    if(message==null){next.save(c,target);dialog.dismiss();saved.run();}
                    else{dialog.getButton(-1).setEnabled(true);dialog.getButton(-2).setEnabled(true);dialog.getButton(-1).setText(tr(c,"Применить","Apply"));dialog.setCancelable(true);automatic.setEnabled(true);automaticChanged.accept(automatic.isChecked());error.setText(message);error.setVisibility(View.VISIBLE);}
                });
            }catch(IllegalArgumentException invalid){error.setText(tr(c,"Проверьте разрешение и отступы: размер от 320×200 до 4096×4096, до 8 Мп; отступы 0–96 dp; масштаб 50–150%.","Check the values: resolution from 320×200 to 4096×4096, up to 8 MP; margins 0–96 dp; scale 50–150%."));error.setVisibility(View.VISIBLE);}
        });
    }
    private static int parse(EditText field){return Integer.parseInt(field.getText().toString().trim());}
    private static EditText number(LinearLayout parent,String label,String tag,int value){
        Context c=parent.getContext();LinearLayout column=new LinearLayout(c);column.setOrientation(LinearLayout.VERTICAL);
        AgentStatusUi.info(column,label);EditText input=new EditText(c);input.setTag(tag);input.setContentDescription(label);input.setTextColor(WorkspaceDialogs.TEXT);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setText(String.format(java.util.Locale.ROOT,"%d",value));column.addView(input,new LinearLayout.LayoutParams(-1,-2));parent.addView(column,new LinearLayout.LayoutParams(0,-2,1));return input;
    }
    private static String tr(Context c,String ru,String en){return WorkspaceLanguage.code(c).equals("ru")?ru:en;}
}
