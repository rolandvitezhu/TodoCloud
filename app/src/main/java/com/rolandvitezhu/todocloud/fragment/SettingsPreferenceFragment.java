package com.rolandvitezhu.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import com.rolandvitezhu.todocloud.R;

public class SettingsPreferenceFragment extends PreferenceFragmentCompat {

  private ISettingsPreferenceFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ISettingsPreferenceFragment) context;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Preference modifyPasswordPreference = findPreference("modify_password_preference");
    modifyPasswordPreference.setOnPreferenceClickListener(preference -> {
      listener.onClickChangePassword();
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
    listener.onSetActionBarTitle(getString(R.string.all_settings));
  }

  public interface ISettingsPreferenceFragment {
    void onSetActionBarTitle(String title);
    void onClickChangePassword();
  }

}
