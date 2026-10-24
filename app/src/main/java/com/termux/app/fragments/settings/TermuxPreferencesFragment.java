package com.termux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.shared.settings.preferences.TermuxAppSharedPreferences;

@Keep
public class TermuxPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TermuxPreferencesDataStore.create(context));

        setPreferencesFromResource(R.xml.termux_preferences, rootKey);
    }

}

class TermuxPreferencesDataStore extends PreferenceDataStore {

    private final TermuxAppSharedPreferences mPreferences;


    private TermuxPreferencesDataStore(Context context) {
        mPreferences = TermuxAppSharedPreferences.build(context, true);
    }

    // SharedPreferences owns the values; this adapter belongs only to its screen.
    public static TermuxPreferencesDataStore create(Context context) {
        return new TermuxPreferencesDataStore(context);
    }

}
