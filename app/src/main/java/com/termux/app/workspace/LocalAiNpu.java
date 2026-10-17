package com.termux.app.workspace;
import android.content.Context;
import android.system.Os;
import java.io.File;
import org.json.JSONObject;
/** Qualcomm runtime setup, only inside the dedicated inference process. No environment/tool changes. */
final class LocalAiNpu {
    static final String SHA="41dd675fbe735b6029012b5576a5716bac614fd8156de0128db4c9dff3cebd4e";
    static final String[] LIBRARIES={"libLiteRtDispatch_Qualcomm.so","libQnnHtp.so","libQnnSystem.so","libQnnHtpPrepare.so","libQnnHtpV79Stub.so","libQnnHtpV79Skel.so"};
    static boolean verifiedModel(JSONObject model){return "litert".equals(model.optString("engine"))&&SHA.equals(model.optString("sha256"));}
    static boolean model(JSONObject model){return "npu".equals(model.optString("backend"))||verifiedModel(model);}
    static boolean available(File directory){for(String name:LIBRARIES)if(!new File(directory,name).isFile())return false;return true;}
    static void prepare(Context context)throws Exception{
        // Hexagon skeletons are DSP ELF files, not ARM64 libraries to dlopen in Android.
        String directory=context.getApplicationInfo().nativeLibraryDir;
        Os.setenv("ADSP_LIBRARY_PATH",directory+";/vendor/lib/rfsa/adsp;/vendor/dsp/cdsp;/system/lib/rfsa/adsp;/vendor/dsp",true);
    }
}
