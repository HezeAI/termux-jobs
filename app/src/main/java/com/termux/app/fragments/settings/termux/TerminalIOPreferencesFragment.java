package com.termux.app.fragments.settings.termux;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.shared.settings.preferences.TermuxAppSharedPreferences;

@Keep
public class TerminalIOPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalIOPreferencesDataStore.create(context));

        setPreferencesFromResource(R.xml.termux_terminal_io_preferences, rootKey);
    }

}

class TerminalIOPreferencesDataStore extends PreferenceDataStore {

    private final TermuxAppSharedPreferences mPreferences;


    private TerminalIOPreferencesDataStore(Context context) {
        mPreferences = TermuxAppSharedPreferences.build(context, true);
    }

    // SharedPreferences owns the values; this adapter belongs only to its screen.
    public static TerminalIOPreferencesDataStore create(Context context) {
        return new TerminalIOPreferencesDataStore(context);
    }



    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null) return;
        if (key == null) return;

        switch (key) {
            case "soft_keyboard_enabled":
                    mPreferences.setSoftKeyboardEnabled(value);
                break;
            case "soft_keyboard_enabled_only_if_no_hardware":
                mPreferences.setSoftKeyboardEnabledOnlyIfNoHardware(value);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences == null) return false;

        switch (key) {
            case "soft_keyboard_enabled":
                return mPreferences.isSoftKeyboardEnabled();
            case "soft_keyboard_enabled_only_if_no_hardware":
                return mPreferences.isSoftKeyboardEnabledOnlyIfNoHardware();
            default:
                return false;
        }
    }

}
