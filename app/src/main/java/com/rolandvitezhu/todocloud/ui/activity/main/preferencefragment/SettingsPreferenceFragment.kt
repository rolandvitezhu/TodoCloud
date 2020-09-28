package com.rolandvitezhu.todocloud.ui.activity.main.preferencefragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity

class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modifyPasswordPreference =
                findPreference<Preference>("modify_password_preference")
        modifyPasswordPreference?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
            (activity as MainActivity?)?.onClickChangePassword()
            true
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.onSetActionBarTitle(getString(R.string.all_settings))
    }
}