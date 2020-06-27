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
import android.widget.Button
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.data.User
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

    @BindView(R.id.constraintlayout_registeruser)
    lateinit var constraintLayout: ConstraintLayout

    @BindView(R.id.textview_registeruser_formsubmissionerrors)
    lateinit var tvFormSubmissionErrors: TextView

    @BindView(R.id.textinputlayout_registeruser_name)
    lateinit var tilName: TextInputLayout

    @BindView(R.id.textinputlayout_registeruser_email)
    lateinit var tilEmail: TextInputLayout

    @BindView(R.id.textinputlayout_registeruser_password)
    lateinit var tilPassword: TextInputLayout

    @BindView(R.id.textinputlayout_registeruser_confirmpassword)
    lateinit var tilConfirmPassword: TextInputLayout

    @BindView(R.id.textinputedittext_registeruser_name)
    lateinit var tietName: TextInputEditText

    @BindView(R.id.textinputedittext_registeruser_email)
    lateinit var tietEmail: TextInputEditText

    @BindView(R.id.textinputedittext_registeruser_password)
    lateinit var tietPassword: TextInputEditText

    @BindView(R.id.textinputedittext_registeruser_confirmpassword)
    lateinit var tietConfirmPassword: TextInputEditText

    @BindView(R.id.button_registeruser)
    lateinit var btnRegister: Button

    lateinit var unbinder: Unbinder

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
        val view = inflater.inflate(R.layout.fragment_registeruser, container, false)
        unbinder = ButterKnife.bind(this, view)
        applyTextChangedEvents()
        applyEditorActionEvents()
        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.onSetActionBarTitle(getString(R.string.all_register))
        applyOrientationPortrait()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
        disposable.clear()
    }

    private fun applyTextChangedEvents() {
        tietName!!.addTextChangedListener(MyTextWatcher(tietName!!))
        tietEmail!!.addTextChangedListener(MyTextWatcher(tietEmail!!))
        tietPassword!!.addTextChangedListener(MyTextWatcher(tietPassword!!))
        tietConfirmPassword!!.addTextChangedListener(MyTextWatcher(tietConfirmPassword!!))
    }

    private fun applyEditorActionEvents() {
        tietConfirmPassword!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                btnRegister!!.performClick()
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
            val name = tietName!!.text.toString().trim { it <= ' ' }
            val email = tietEmail!!.text.toString().trim { it <= ' ' }
            val password = tietPassword!!.text.toString().trim { it <= ' ' }
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
        val givenName = tietName!!.text.toString().trim { it <= ' ' }
        return if (givenName.isEmpty()) {
            tilName!!.error = getString(R.string.registeruser_nameerrorlabel)
            false
        } else {
            tilName!!.isErrorEnabled = false
            true
        }
    }

    private fun validateEmail(): Boolean {
        val givenEmail = tietEmail!!.text.toString().trim { it <= ' ' }
        val isGivenEmailValid = !givenEmail.isEmpty() && isValidEmail(givenEmail)
        return if (!isGivenEmailValid) {
            tilEmail!!.error = getString(R.string.registeruser_entervalidemailhint)
            false
        } else {
            tilEmail!!.isErrorEnabled = false
            true
        }
    }

    private fun validatePassword(): Boolean {
        val givenPassword = tietPassword!!.text.toString().trim { it <= ' ' }
        val isGivenPasswordValid = !givenPassword.isEmpty() && isValidPassword(givenPassword)
        return if (!isGivenPasswordValid) {
            tilPassword!!.error = getString(R.string.registeruser_enterproperpasswordhint)
            false
        } else {
            tilPassword!!.isErrorEnabled = false
            true
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val givenPassword = tietPassword!!.text.toString().trim { it <= ' ' }
        val givenConfirmPassword = tietConfirmPassword!!.text.toString().trim { it <= ' ' }
        val isGivenConfirmPasswordValid = (!givenConfirmPassword.isEmpty()
                && givenPassword == givenConfirmPassword)
        return if (!isGivenConfirmPasswordValid) {
            tilConfirmPassword!!.error = getString(R.string.registeruser_confirmpassworderrorlabel)
            false
        } else {
            tilConfirmPassword!!.isErrorEnabled = false
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
            tvFormSubmissionErrors!!.text = ""
            tvFormSubmissionErrors!!.visibility = View.GONE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showFailedToConnectError() {
        try {
            val snackbar = Snackbar.make(
                    constraintLayout!!,
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
            tvFormSubmissionErrors!!.setText(R.string.registeruser_thisemailalreadyexisted)
            tvFormSubmissionErrors!!.visibility = View.VISIBLE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showAnErrorOccurredError() {
        try {
            val snackbar = Snackbar.make(
                    constraintLayout!!,
                    R.string.all_anerroroccurred,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    @OnClick(R.id.button_registeruser)
    fun onBtnRegisterClick(view: View?) {
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