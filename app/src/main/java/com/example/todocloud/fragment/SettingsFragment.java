package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.example.todocloud.R;

public class SettingsFragment extends PreferenceFragmentCompat {

  private ISettingsFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ISettingsFragment) context;
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences);
  }

  @Override
  public void onResume() {
    super.onResume();
    listener.setActionBarTitle(getString(R.string.itemSettings));
  }

  /**
   * Interfész a MainActivity-vel történő kommunikációra.
   */
  public interface ISettingsFragment {
    void setActionBarTitle(String title);
  }

}
