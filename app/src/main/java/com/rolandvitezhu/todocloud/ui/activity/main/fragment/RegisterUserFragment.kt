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
import com.rolandvitezhu.todocloud.database.TodoCloudDatabase
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.databinding.FragmentRegisteruserBinding
import com.rolandvitezhu.todocloud.helper.*
import com.rolandvitezhu.todocloud.helper.GeneralHelper.validateField
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.fragment_registeruser.*
import kotlinx.android.synthetic.main.fragment_registeruser.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import javax.inject.Inject

class RegisterUserFragment : Fragment() {

    private val TAG = javaClass.simpleName
    private lateinit var apiService: ApiService

    @Inject
    lateinit var sessionManager: SessionManager
    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao
    @Inject
    lateinit var todoCloudDatabase: TodoCloudDatabase
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

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val fragmentRegisteruserBinding: FragmentRegisteruserBinding =
                FragmentRegisteruserBinding.inflate(inflater, container, false)
        val view: View = fragmentRegisteruserBinding.root

        applyTextChangedEvents(view)

        fragmentRegisteruserBinding.lifecycleOwner = this
        fragmentRegisteruserBinding.registerUserFragment = this
        fragmentRegisteruserBinding.userViewModel = userViewModel
        fragmentRegisteruserBinding.executePendingBindings()

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.onSetActionBarTitle(getString(R.string.all_register))
        applyOrientationPortrait()
    }

    private fun applyTextChangedEvents(view: View) {
        view.textinputedittext_registeruser_name?.addTextChangedListener(
                RegisterUserTextWatcher(view.textinputedittext_registeruser_name!!))
        view.textinputedittext_registeruser_email?.addTextChangedListener(
                RegisterUserTextWatcher(view.textinputedittext_registeruser_email!!))
        view.textinputedittext_registeruser_password?.addTextChangedListener(
                RegisterUserTextWatcher(view.textinputedittext_registeruser_password!!))
        view.textinputedittext_registeruser_confirmpassword?.addTextChangedListener(
                RegisterUserTextWatcher(view.textinputedittext_registeruser_confirmpassword!!))
    }

    private fun handleRegisterUser() {
        if (validateName()
                and validateEmail()
                and validatePassword()
                and validateConfirmPassword()) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { todoCloudDatabase.clearAllTables() }
                userViewModel.user.value?._id = userViewModel.user.value?.let{
                    todoCloudDatabaseDao.insertUser(it)
                }
                userViewModel.user.value?.userOnlineId =
                        OnlineIdGenerator.generateUserOnlineId(userViewModel.user.value?._id as Long)
                handleRegisterUserRequest()
            }
        }
    }

    /**
     * Register a user on the Web and create it in the remote and in the local database. If there
     * are any error it will handle them.
     */
    private fun handleRegisterUserRequest() {
        lifecycleScope.launch {
            try {
                userViewModel.onRegisterUser()
                onFinishRegisterUser()
            } catch (cause: Throwable) {
                if (cause.message != null && cause.message!!.contains(
                                "Oops! An error occurred while registering")) {
                    handleError()
                } else {
                    showErrorMessage(cause.message)
                }
            }
        }
    }

    /**
     * Generally the cause of error is, that the userOnlineId generated by the client is
     * already registered in the remote database. In this case, it generate a different
     * userOnlineId, and send the registration request again.
     */
    private fun handleError() {
        InstallationIdHelper.getNewInstallationId()
        val newUserOnlineId =
                OnlineIdGenerator.generateUserOnlineId(userViewModel.user.value?._id as Long)
        userViewModel.user.value?.userOnlineId = newUserOnlineId
        handleRegisterUserRequest()
    }

    override fun onPause() {
        super.onPause()
        applyOrientationFullSensor()
    }

    /**
     * Validate the name field and show an error message if necessary.
     */
    private fun validateName(): Boolean {
        if (view != null && view?.textinputlayout_registeruser_name != null) {
            return validateField(userViewModel.isNameValid(),
                    view?.textinputlayout_registeruser_name as TextInputLayout,
                    getString(R.string.registeruser_nameerrorlabel))
        }

        return false
    }

    /**
     * Validate the email field and show an error message, if necessary. If the email is provided
     * and the pattern of the email is valid, then the email is valid.
     */
    private fun validateEmail(): Boolean {
        if (view != null && view?.textinputlayout_registeruser_email != null) {
            return validateField(userViewModel.isEmailValid(),
                    view?.textinputlayout_registeruser_email as TextInputLayout,
                    getString(R.string.registeruser_entervalidemailhint))
        }

        return false
    }

    /**
     * Validate the password field and show an error message, if necessary. If the password is
     * provided and the pattern of the password is valid, then the password is valid.
     */
    private fun validatePassword(): Boolean {
        if (view != null && view?.textinputlayout_registeruser_password != null) {
            return validateField(userViewModel.isPasswordValid(),
                    view?.textinputlayout_registeruser_password as TextInputLayout,
                    getString(R.string.registeruser_enterproperpasswordhint))
        }

        return false
    }

    /**
     * Validate the confirm password field. If the confirm password is provided and the same as
     * the provided password, then the confirm password is valid.
     */
    private fun validateConfirmPassword(): Boolean {
        if (view != null && view?.textinputlayout_registeruser_confirmpassword != null) {
            return validateField(userViewModel.isConfirmPasswordValid(),
            view?.textinputlayout_registeruser_confirmpassword as TextInputLayout,
            getString(R.string.registeruser_confirmpassworderrorlabel))
        }

        return false
    }

    fun onFinishRegisterUser() {
        hideFormSubmissionErrors()
        (activity as MainActivity?)?.onFinishRegisterUser()
    }

    private fun showErrorMessage(errorMessage: String?) {
        if (errorMessage != null) {
            val upperCaseErrorMessage = errorMessage.toUpperCase()
            if (upperCaseErrorMessage.contains("FAILED TO CONNECT") ||
                    upperCaseErrorMessage.contains("UNABLE TO RESOLVE HOST") ||
                    upperCaseErrorMessage.contains("TIMEOUT")) {
                hideFormSubmissionErrors()
                showFailedToConnectError()
            } else if (upperCaseErrorMessage.contains("SORRY, THIS EMAIL ALREADY EXISTED")) {
                showThisEmailIsAlreadyRegisteredError()
            } else {
                hideFormSubmissionErrors()
                showAnErrorOccurredError()
            }
        }
    }

    private fun hideFormSubmissionErrors() {
        try {
            this.textview_registeruser_formsubmissionerrors?.text = ""
            this.textview_registeruser_formsubmissionerrors?.visibility = View.GONE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showFailedToConnectError() {
        try {
            val snackbar = Snackbar.make(
                    this.constraintlayout_registeruser!!,
                    R.string.all_failedtoconnect,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    private fun showThisEmailIsAlreadyRegisteredError() {
        try {
            this.textview_registeruser_formsubmissionerrors?.setText(R.string.registeruser_thisemailalreadyexisted)
            this.textview_registeruser_formsubmissionerrors?.visibility = View.VISIBLE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showAnErrorOccurredError() {
        try {
            val snackbar = Snackbar.make(
                    this.constraintlayout_registeruser!!,
                    R.string.all_anerroroccurred,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    fun onButtonRegisterClick() {
        hideSoftInput()
        handleRegisterUser()
    }

    private inner class RegisterUserTextWatcher(private val view: View) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            when (view.id) {
                R.id.textinputedittext_registeruser_name -> validateName()
                R.id.textinputedittext_registeruser_email -> validateEmail()
                R.id.textinputedittext_registeruser_password -> validatePassword()
                R.id.textinputedittext_registeruser_confirmpassword -> validateConfirmPassword()
            }
        }

    }
}