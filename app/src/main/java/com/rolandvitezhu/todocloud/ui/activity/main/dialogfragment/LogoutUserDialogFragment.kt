package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.databinding.DialogLogoutuserBinding
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity

class LogoutUserDialogFragment : AppCompatDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val dialogLogoutuserBinding: DialogLogoutuserBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_logoutuser, container, false)
        val view: View = dialogLogoutuserBinding.root
        dialogLogoutuserBinding.logoutUserDialogFragment = this

        dialog!!.setTitle(R.string.logoutuser_title)

        return view
    }

    fun onBtnOkClick(view: View) {
        (activity as MainActivity?)!!.onLogout()
        dismiss()
    }

    fun onBtnCancelClick(view: View) {
        dismiss()
    }
}