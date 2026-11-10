package com.termux.app.workspace;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=28,application=android.app.Application.class)
public class AgentCommandTitleTest {
 String title(String cmd)throws Exception{return AgentCommandTitle.title(new JSONObject().put("command",cmd),true);}
 @Test public void knownCommandsUseConservativeActionNames()throws Exception{
  assertEquals("Чтение файла",title("head -n 45 'file name'"));
  assertEquals("Чтение файла",title("sed -n '1,45p' file"));
  assertEquals("Поиск по проекту",title("rg 'foo|bar' src"));
  assertEquals("Проверка изменений",title("git -C /project diff --stat"));
  assertEquals("Проверка изменений",title("bash -lc 'git status --short'"));
  assertEquals("Запуск тестов",title("python3 -m pytest tests"));
  assertEquals("Запуск тестов",title("./gradlew :app:testDebugUnitTest"));
  assertEquals("Сборка приложения",title("./gradlew :app:assembleRelease"));
  assertEquals("Проверка изменений",title("rtk git diff"));
 }
 @Test public void unknownAndCompoundCommandsDoNotInventPurpose()throws Exception{
  for(String cmd:new String[]{"custom-run arg","python3 script.py","sed -i s/a/b/ file","cat > file",""})assertEquals(cmd,"Выполнение команды",title(cmd));
  for(String cmd:new String[]{"git status && ./gradlew test","cat a | head","bash -lc 'cat a; rg x .'","cat $(generate)","x &","cat a\ncat b"})assertEquals(cmd,"Выполнение нескольких команд",title(cmd));
  assertEquals("Выполнение команды",AgentCommandTitle.title(new JSONObject().put("command","git diff").put("commandTruncated",true),true));
 }
 @Test public void suppliedDescriptionWinsWithoutChangingNativeOutcome()throws Exception{
  JSONObject row=new JSONObject().put("command","foo && bar").put("actionDescription","Проверить формат проекта").put("exitCode",0).put("status","completed");String before=row.toString();
  assertEquals("Проверить формат проекта",AgentCommandTitle.title(row,true));assertEquals(before,row.toString());
 }
}
