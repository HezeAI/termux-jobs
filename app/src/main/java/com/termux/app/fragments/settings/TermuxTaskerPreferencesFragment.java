package com.termux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.shared.settings.preferences.TermuxTaskerAppSharedPreferences;

@Keep
public class TermuxTaskerPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TermuxTaskerPreferencesDataStore.create(context));

        setPreferencesFromResource(R.xml.termux_tasker_preferences, rootKey);
    }

}

class TermuxTaskerPreferencesDataStore extends PreferenceDataStore {

    private final TermuxTaskerAppSharedPreferences mPreferences;


    private TermuxTaskerPreferencesDataStore(Context context) {
        mPreferences = TermuxTaskerAppSharedPreferences.build(context, true);
    }

    // SharedPreferences owns the values; this adapter belongs only to its screen.
    public static TermuxTaskerPreferencesDataStore create(Context context) {
        return new TermuxTaskerPreferencesDataStore(context);
    }

}
