package com.termux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.shared.settings.preferences.TermuxFloatAppSharedPreferences;

@Keep
public class TermuxFloatPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TermuxFloatPreferencesDataStore.create(context));

        setPreferencesFromResource(R.xml.termux_float_preferences, rootKey);
    }

}

class TermuxFloatPreferencesDataStore extends PreferenceDataStore {

    private final TermuxFloatAppSharedPreferences mPreferences;


    private TermuxFloatPreferencesDataStore(Context context) {
        mPreferences = TermuxFloatAppSharedPreferences.build(context, true);
    }

    // SharedPreferences owns the values; this adapter belongs only to its screen.
    public static TermuxFloatPreferencesDataStore create(Context context) {
        return new TermuxFloatPreferencesDataStore(context);
    }

}
