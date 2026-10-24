package com.termux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.shared.settings.preferences.TermuxAPIAppSharedPreferences;

@Keep
public class TermuxAPIPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TermuxAPIPreferencesDataStore.create(context));

        setPreferencesFromResource(R.xml.termux_api_preferences, rootKey);
    }

}

class TermuxAPIPreferencesDataStore extends PreferenceDataStore {

    private final TermuxAPIAppSharedPreferences mPreferences;


    private TermuxAPIPreferencesDataStore(Context context) {
        mPreferences = TermuxAPIAppSharedPreferences.build(context, true);
    }

    // SharedPreferences owns the values; this adapter belongs only to its screen.
    public static TermuxAPIPreferencesDataStore create(Context context) {
        return new TermuxAPIPreferencesDataStore(context);
    }

}
