package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity

class LogoutUserDialogFragment : AppCompatDialogFragment() {

    var unbinder: Unbinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_logoutuser, container)
        unbinder = ButterKnife.bind(this, view)

        dialog!!.setTitle(R.string.logoutuser_title)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
    }

    @OnClick(R.id.button_logoutuser_ok)
    fun onBtnOkClick(view: View?) {
        (activity as MainActivity?)!!.onLogout()
        dismiss()
    }

    @OnClick(R.id.button_logoutuser_cancel)
    fun onBtnCancelClick(view: View?) {
        dismiss()
    }
}