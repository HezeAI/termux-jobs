package com.termux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.shared.settings.preferences.TermuxWidgetAppSharedPreferences;

@Keep
public class TermuxWidgetPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TermuxWidgetPreferencesDataStore.create(context));

        setPreferencesFromResource(R.xml.termux_widget_preferences, rootKey);
    }

}

class TermuxWidgetPreferencesDataStore extends PreferenceDataStore {

    private final TermuxWidgetAppSharedPreferences mPreferences;


    private TermuxWidgetPreferencesDataStore(Context context) {
        mPreferences = TermuxWidgetAppSharedPreferences.build(context, true);
    }

    // SharedPreferences owns the values; this adapter belongs only to its screen.
    public static TermuxWidgetPreferencesDataStore create(Context context) {
        return new TermuxWidgetPreferencesDataStore(context);
    }

}
