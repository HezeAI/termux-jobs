package com.termux.app.workspace;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import org.json.*;

/** Export teacher-forced next-assistant examples using production protocol rendering.
 * Does not tokenize, truncate, execute tools or train weights. */
public final class ExportTrainingExamples {
 public static void main(String[] args)throws Exception {
  if(args.length!=2)throw new IllegalArgumentException("input episode JSONL, output examples JSONL");
  int count=0;
  try(BufferedReader in=Files.newBufferedReader(Paths.get(args[0]),StandardCharsets.UTF_8);BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]),StandardCharsets.UTF_8)){
   String line;while((line=in.readLine())!=null){if(line.trim().isEmpty())continue;JSONObject episode=new JSONObject(line);if(!episode.optBoolean("synthetic"))throw new IllegalArgumentException("Only reviewed synthetic draft input is supported");JSONArray messages=episode.getJSONArray("messages"),history=new JSONArray();
    for(int i=0;i<messages.length();i++){
     JSONObject m=messages.getJSONObject(i);if(m.getString("role").equals("assistant")){
      JSONObject request=m.optJSONObject("toolRequest");String target=request==null?m.getString("text"):LocalAiQwenProtocol.call(request);
      if(target.isEmpty())throw new IllegalArgumentException("Empty assistant target: "+episode.getString("id"));
      JSONObject example=new JSONObject().put("episode",episode.getString("id")).put("split",episode.getString("split")).put("profile",episode.getString("profile")).put("system",episode.getString("system")).put("messages",LocalAiQwenProtocol.render(history)).put("target",target).put("synthetic",true).put("tokenizerParityVerified",false);
      out.write(example.toString());out.newLine();count++;
     }
     history.put(new JSONObject(m.toString()));
    }
   }
  }
  System.out.println("Exported "+count+" examples; tokenization/loss masking and model evaluation still required");
 }
}
