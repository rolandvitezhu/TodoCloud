package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.databinding.FragmentResetpasswordBinding
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.helper.GeneralHelper.validateField
import com.rolandvitezhu.todocloud.helper.applyOrientationFullSensor
import com.rolandvitezhu.todocloud.helper.applyOrientationPortrait
import com.rolandvitezhu.todocloud.helper.hideSoftInput
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.fragment_resetpassword.*
import kotlinx.android.synthetic.main.fragment_resetpassword.view.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject

class ResetPasswordFragment : Fragment() {

    private val TAG = javaClass.simpleName
    private lateinit var apiService: ApiService

    @Inject
    lateinit var dbLoader: DbLoader
    @Inject
    lateinit var retrofit: Retrofit

    private val userViewModel by lazy {
        ViewModelProvider(this).get(UserViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as AppController)
                .appComponent.fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiService = retrofit.create(ApiService::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val fragmentResetpasswordBinding: FragmentResetpasswordBinding =
                FragmentResetpasswordBinding.inflate(inflater, container, false)
        val view: View = fragmentResetpasswordBinding.root

        applyTextChangedEvents(view)

        fragmentResetpasswordBinding.lifecycleOwner = this
        fragmentResetpasswordBinding.resetPasswordFragment = this
        fragmentResetpasswordBinding.userViewModel = userViewModel
        fragmentResetpasswordBinding.executePendingBindings()

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.onSetActionBarTitle(getString(R.string.all_reset_password))
        applyOrientationPortrait()
    }

    private fun applyTextChangedEvents(view: View) {
        view.textinputedittext_resetpassword_email?.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_resetpassword_email!!))
    }

    private fun handleResetPassword() {
        if (validateEmail()) {
            lifecycleScope.launch {
                try {
                    userViewModel.onResetPassword()
                    onFinishResetPassword()
                } catch (cause: Throwable) {
                    showErrorMessage(cause.message)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        applyOrientationFullSensor()
    }

    /**
     * Check whether the email address is valid and show an error message, if necessary. The email
     * address is valid, if it is provided and it has a valid email address pattern.
     */
    private fun validateEmail(): Boolean {
        if (view != null && view?.textinputlayout_resetpassword_email != null) {
            return validateField(userViewModel.isEmailValid(),
                    view?.textinputlayout_resetpassword_email as TextInputLayout,
                    getString(R.string.registeruser_entervalidemailhint))
        }

        return false
    }

    private fun onFinishResetPassword() {
        hideFormSubmissionErrors()
        (activity as MainActivity?)?.onFinishResetPassword()
    }

    private fun showErrorMessage(errorMessage: String?) {
        if (errorMessage != null) {
            val upperCaseErrorMessage = errorMessage.toUpperCase()
            if (upperCaseErrorMessage.contains("FAILED TO CONNECT") ||
                    upperCaseErrorMessage.contains("UNABLE TO RESOLVE HOST") ||
                    upperCaseErrorMessage.contains("TIMEOUT")) {
                hideFormSubmissionErrors()
                showFailedToConnectError()
            } else if (upperCaseErrorMessage.contains("FAILED TO RESET PASSWORD. PLEASE TRY AGAIN!")) {
                showFailedToResetPasswordError()
            } else {
                hideFormSubmissionErrors()
                showAnErrorOccurredError()
            }
        }
    }

    private fun hideFormSubmissionErrors() {
        try {
            this.textview_resetpassword_formsubmissionerrors?.text = ""
            this.textview_resetpassword_formsubmissionerrors?.visibility = View.GONE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showFailedToConnectError() {
        try {
            val snackbar = Snackbar.make(
                    this.constraintlayout_resetpassword!!,
                    R.string.all_failedtoconnect,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    private fun showFailedToResetPasswordError() {
        try {
            this.textview_resetpassword_formsubmissionerrors?.setText(R.string.modifypassword_failedtoresetpassword)
            this.textview_resetpassword_formsubmissionerrors?.visibility = View.VISIBLE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showAnErrorOccurredError() {
        try {
            val snackbar = Snackbar.make(
                    this.constraintlayout_resetpassword!!,
                    R.string.all_anerroroccurred,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    fun onButtonSubmitClick() {
        hideSoftInput()
        handleResetPassword()
    }

    private inner class MyTextWatcher(private val view: View) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            when (view.id) {
                R.id.textinputedittext_resetpassword_email -> validateEmail()
            }
        }

    }
}