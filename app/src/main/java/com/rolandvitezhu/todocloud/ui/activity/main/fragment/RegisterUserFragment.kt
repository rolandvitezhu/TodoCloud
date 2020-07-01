package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.data.User
import com.rolandvitezhu.todocloud.databinding.FragmentRegisteruserBinding
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.helper.InstallationIdHelper
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator
import com.rolandvitezhu.todocloud.helper.SessionManager
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.network.api.user.dto.RegisterUserRequest
import com.rolandvitezhu.todocloud.network.api.user.dto.RegisterUserResponse
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_registeruser.*
import kotlinx.android.synthetic.main.fragment_registeruser.view.*
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject

class RegisterUserFragment : Fragment() {

    private val TAG = javaClass.simpleName
    private lateinit var apiService: ApiService
    private val disposable = CompositeDisposable()

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var dbLoader: DbLoader

    @Inject
    lateinit var retrofit: Retrofit

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (Objects.requireNonNull(activity)?.application as AppController).appComponent.
        fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiService = retrofit!!.create(ApiService::class.java)
        if (sessionManager!!.isLoggedIn) {
            (activity as MainActivity?)!!.onFinishLoginUser()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val fragmentRegisteruserBinding: FragmentRegisteruserBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_registeruser, container, false)
        val view: View = fragmentRegisteruserBinding.root
        fragmentRegisteruserBinding.registerUserFragment = this

        applyTextChangedEvents(view)
        applyEditorActionEvents(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.onSetActionBarTitle(getString(R.string.all_register))
        applyOrientationPortrait()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }

    private fun applyTextChangedEvents(view: View) {
        view.textinputedittext_registeruser_name!!.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_registeruser_name!!))
        view.textinputedittext_registeruser_email!!.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_registeruser_email!!))
        view.textinputedittext_registeruser_password!!.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_registeruser_password!!))
        view.textinputedittext_registeruser_confirmpassword!!.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_registeruser_confirmpassword!!))
    }

    private fun applyEditorActionEvents(view: View) {
        view.textinputedittext_registeruser_confirmpassword!!.setOnEditorActionListener(
                OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                view.button_registeruser!!.performClick()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun hideSoftInput() {
        val activity = activity
        val inputMethodManager = activity!!.getSystemService(
                Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        val currentlyFocusedView = activity.currentFocus
        if (currentlyFocusedView != null) {
            val windowToken = currentlyFocusedView.windowToken
            inputMethodManager.hideSoftInputFromWindow(
                    windowToken,
                    0
            )
        }
    }

    private fun handleRegisterUser() {
        val areFieldsValid = (validateName()
                and validateEmail()
                and validatePassword()
                and validateConfirmPassword())
        if (areFieldsValid) {
            dbLoader!!.reCreateDb()
            val user = User()
            val _id = dbLoader!!.createUser(user)
            val user_online_id = OnlineIdGenerator.generateUserOnlineId(_id)
            val name = this.textinputedittext_registeruser_name!!.text.toString().trim { it <= ' ' }
            val email = this.textinputedittext_registeruser_email!!.text.toString().trim { it <= ' ' }
            val password = this.textinputedittext_registeruser_password!!.text.toString().trim { it <= ' ' }
            val registerUserRequest = RegisterUserRequest()
            registerUserRequest.userOnlineId = user_online_id
            registerUserRequest.name = name
            registerUserRequest.email = email
            registerUserRequest.password = password
            apiService
                    .registerUser(registerUserRequest)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribeWith(createRegisterUserDisposableSingleObserver(_id, registerUserRequest))?.let {
                        disposable.add(
                                it
                        )
                    }
        }
    }

    /**
     * Generally the cause of error is, that the userOnlineId generated by the client is
     * already registered in the remote database. In this case, it generate a different
     * userOnlineId, and send the registration request again.
     */
    private fun handleError(
            _id: Long,
            registerUserRequest: RegisterUserRequest,
            apiService: ApiService?,
            registerUserDisposableSingleObserver: DisposableSingleObserver<RegisterUserResponse?>
    ) {
        InstallationIdHelper.getNewInstallationId()
        val new_user_online_id = OnlineIdGenerator.generateUserOnlineId(_id)
        registerUserRequest.userOnlineId = new_user_online_id
        apiService
                ?.registerUser(registerUserRequest)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeWith(createRegisterUserDisposableSingleObserver(_id, registerUserRequest))?.let {
                    disposable.add(
                            it
                    )
                }
    }

    private fun createRegisterUserDisposableSingleObserver(_id: Long, registerUserRequest: RegisterUserRequest): DisposableSingleObserver<RegisterUserResponse?> {
        return object : DisposableSingleObserver<RegisterUserResponse?>() {
            override fun onSuccess(registerUserResponse: RegisterUserResponse) {
                Log.d(TAG, "Register Response: $registerUserResponse")
                if (registerUserResponse != null && registerUserResponse.error == "false") {
                    onFinishRegisterUser()
                } else if (registerUserResponse != null) {
                    var message = registerUserResponse.getMessage()
                    if (message == null) message = "Unknown error"
                    if (message.contains("Oops! An error occurred while registering")) {
                        handleError(_id, registerUserRequest, apiService, this)
                    } else {
                        onSyncError(message)
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Register Response - onFailure: $throwable")
            }
        }
    }

    private fun applyOrientationPortrait() {
        if (activity != null) activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onPause() {
        super.onPause()
        applyOrientationFullSensor()
    }

    private fun applyOrientationFullSensor() {
        if (activity != null) activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    private fun validateName(): Boolean {
        val givenName = this.textinputedittext_registeruser_name!!.text.toString().trim { it <= ' ' }
        return if (givenName.isEmpty()) {
            this.textinputlayout_registeruser_name!!.error = getString(R.string.registeruser_nameerrorlabel)
            false
        } else {
            this.textinputlayout_registeruser_name!!.isErrorEnabled = false
            true
        }
    }

    private fun validateEmail(): Boolean {
        val givenEmail = this.textinputedittext_registeruser_email!!.text.toString().trim { it <= ' ' }
        val isGivenEmailValid = !givenEmail.isEmpty() && isValidEmail(givenEmail)
        return if (!isGivenEmailValid) {
            this.textinputlayout_registeruser_email!!.error = getString(R.string.registeruser_entervalidemailhint)
            false
        } else {
            this.textinputlayout_registeruser_email!!.isErrorEnabled = false
            true
        }
    }

    private fun validatePassword(): Boolean {
        val givenPassword = this.textinputedittext_registeruser_password!!.text.toString().trim { it <= ' ' }
        val isGivenPasswordValid = !givenPassword.isEmpty() && isValidPassword(givenPassword)
        return if (!isGivenPasswordValid) {
            this.textinputlayout_registeruser_password!!.error = getString(R.string.registeruser_enterproperpasswordhint)
            false
        } else {
            this.textinputlayout_registeruser_password!!.isErrorEnabled = false
            true
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val givenPassword = this.textinputedittext_registeruser_password!!.text.toString().trim { it <= ' ' }
        val givenConfirmPassword = this.textinputedittext_registeruser_confirmpassword!!.text.toString().trim { it <= ' ' }
        val isGivenConfirmPasswordValid = (!givenConfirmPassword.isEmpty()
                && givenPassword == givenConfirmPassword)
        return if (!isGivenConfirmPasswordValid) {
            this.textinputlayout_registeruser_confirmpassword!!.error = getString(R.string.registeruser_confirmpassworderrorlabel)
            false
        } else {
            this.textinputlayout_registeruser_confirmpassword!!.isErrorEnabled = false
            true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Valid password should contain at least a lowercase letter, an uppercase letter, a number,
     * it should not contain whitespace character and it should be at least 8 characters long.
     */
    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }

    fun onFinishRegisterUser() {
        hideFormSubmissionErrors()
        (activity as MainActivity?)!!.onFinishRegisterUser()
    }

    fun onSyncError(errorMessage: String) {
        showErrorMessage(errorMessage)
    }

    private fun showErrorMessage(errorMessage: String) {
        if (errorMessage.contains("failed to connect")) {
            hideFormSubmissionErrors()
            showFailedToConnectError()
        } else if (errorMessage.contains("Sorry, this email already existed")) {
            showThisEmailAlreadyExistedError()
        } else {
            hideFormSubmissionErrors()
            showAnErrorOccurredError()
        }
    }

    private fun hideFormSubmissionErrors() {
        try {
            this.textview_registeruser_formsubmissionerrors!!.text = ""
            this.textview_registeruser_formsubmissionerrors!!.visibility = View.GONE
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

    private fun showThisEmailAlreadyExistedError() {
        try {
            this.textview_registeruser_formsubmissionerrors!!.setText(R.string.registeruser_thisemailalreadyexisted)
            this.textview_registeruser_formsubmissionerrors!!.visibility = View.VISIBLE
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

    fun onBtnRegisterClick(view: View) {
        hideSoftInput()
        handleRegisterUser()
    }

    private inner class MyTextWatcher(private val view: View) : TextWatcher {
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