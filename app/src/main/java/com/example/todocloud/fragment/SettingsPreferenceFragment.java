package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.example.todocloud.R;

public class SettingsPreferenceFragment extends PreferenceFragmentCompat {

  private ISettingsPreferenceFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ISettingsPreferenceFragment) context;
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
  }

}
