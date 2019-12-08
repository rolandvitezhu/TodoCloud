package com.rolandvitezhu.todocloud.ui.activity.main.preferencefragment;

import android.os.Bundle;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsPreferenceFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Preference modifyPasswordPreference = findPreference("modify_password_preference");
    modifyPasswordPreference.setOnPreferenceClickListener(preference -> {
      ((MainActivity)getActivity()).onClickChangePassword();
      return true;
    });
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((MainActivity)getActivity()).onSetActionBarTitle(getString(R.string.all_settings));
  }

}
