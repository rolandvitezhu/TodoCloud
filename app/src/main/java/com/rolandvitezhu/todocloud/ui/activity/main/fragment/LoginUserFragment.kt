package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.databinding.FragmentLoginuserBinding
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.helper.GeneralHelper.validateField
import com.rolandvitezhu.todocloud.helper.SessionManager
import com.rolandvitezhu.todocloud.helper.applyOrientationFullSensor
import com.rolandvitezhu.todocloud.helper.applyOrientationPortrait
import com.rolandvitezhu.todocloud.helper.hideSoftInput
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.fragment_loginuser.*
import kotlinx.android.synthetic.main.fragment_loginuser.view.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject

class LoginUserFragment : Fragment() {

    private val TAG = javaClass.simpleName
    private lateinit var apiService: ApiService

    @Inject
    lateinit var sessionManager: SessionManager
    @Inject
    lateinit var dbLoader: DbLoader
    @Inject
    lateinit var retrofit: Retrofit

    private val userViewModel by lazy {
        ViewModelProvider(this).get(UserViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as AppController).appComponent.
        fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiService = retrofit.create(ApiService::class.java)
        if (sessionManager.isLoggedIn) {
            (activity as MainActivity?)?.onFinishLoginUser()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val fragmentLoginuserBinding: FragmentLoginuserBinding =
                FragmentLoginuserBinding.inflate(inflater, container, false)
        val view: View = fragmentLoginuserBinding.root

        preventButtonTextCapitalization(view)

        fragmentLoginuserBinding.lifecycleOwner = this
        fragmentLoginuserBinding.loginUserFragment = this
        fragmentLoginuserBinding.userViewModel = userViewModel
        fragmentLoginuserBinding.executePendingBindings()

        return view
    }

    override fun onResume() {
        super.onResume()
        try {
            (activity as MainActivity?)?.onSetActionBarTitle(getString(R.string.all_login))
        } catch (e: NullPointerException) {
            // Activity doesn't exists already.
        }
        applyOrientationPortrait()
    }

    override fun onPause() {
        super.onPause()
        applyOrientationFullSensor()
    }

    private fun handleLoginUser() {
        if (this.view?.let { validateFields(it) } == true) {
            lifecycleScope.launch {
                try {
                    userViewModel.onLoginUser()
                    sessionManager.setLogin(true)
                    onFinishLoginUser()
                } catch (cause: Throwable) {
                    showErrorMessage(cause.message)
                }
            }
        }
    }

    /**
     * Validate the fields and show or hide the error messages.
     */
    private fun validateFields(view: View) =
        validateField(userViewModel.isEmailProvided(),
                view.textinputlayout_loginuser_email,
                getString(R.string.registeruser_enteremailhint)) and
        validateField(userViewModel.isPasswordProvided(),
                view.textinputlayout_loginuser_password,
                getString(R.string.registeruser_enterpasswordhint))

    private fun preventButtonTextCapitalization(view: View) {
        view.button_loginuser_linktoregister?.transformationMethod = null
        view.button_loginuser_linktoresetpassword?.transformationMethod = null
    }

    private fun onFinishLoginUser() {
        hideFormSubmissionErrors()
        (activity as MainActivity?)?.onFinishLoginUser()
    }

    private fun showErrorMessage(errorMessage: String?) {
        if (errorMessage != null) {
            val upperCaseErrorMessage = errorMessage.toUpperCase()
            if (upperCaseErrorMessage.contains("FAILED TO CONNECT") ||
                    upperCaseErrorMessage.contains("UNABLE TO RESOLVE HOST") ||
                    upperCaseErrorMessage.contains("TIMEOUT")) {
                hideFormSubmissionErrors()
                showFailedToConnectError()
            } else if (upperCaseErrorMessage.contains("LOGIN FAILED. INCORRECT CREDENTIALS")) {
                showIncorrectCredentialsError()
            } else {
                hideFormSubmissionErrors()
                showAnErrorOccurredError()
            }
        }
    }

    private fun hideFormSubmissionErrors() {
        try {
            this.textview_loginuser_formsubmissionerrors?.text = ""
            this.textview_loginuser_formsubmissionerrors?.visibility = View.GONE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showFailedToConnectError() {
        try {
            val snackbar = Snackbar.make(
                    this.constraintlayout_loginuser!!,
                    R.string.all_failedtoconnect,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    private fun showIncorrectCredentialsError() {
        try {
            this.textview_loginuser_formsubmissionerrors?.setText(R.string.loginuser_error)
            this.textview_loginuser_formsubmissionerrors?.visibility = View.VISIBLE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showAnErrorOccurredError() {
        try {
            val snackbar = Snackbar.make(
                    this.constraintlayout_loginuser!!,
                    R.string.all_anerroroccurred,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    fun onButtonLoginClick() {
        hideSoftInput()
        handleLoginUser()
    }

    fun onButtonLinkToRegisterClick() {
        (activity as MainActivity?)?.onClickLinkToRegisterUser()
    }

    fun onButtonLinkToResetPasswordClick() {
        (activity as MainActivity?)?.onClickLinkToResetPassword()
    }
}