package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.rolandvitezhu.todocloud.helper.SessionManager
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserRequest
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserResponse
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject

class LoginUserFragment : Fragment() {

    private val TAG = javaClass.simpleName
    private lateinit var apiService: ApiService
    private val disposable = CompositeDisposable()

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var dbLoader: DbLoader

    @Inject
    lateinit var retrofit: Retrofit

    @BindView(R.id.constraintlayout_loginuser)
    lateinit var constraintLayout: ConstraintLayout

    @BindView(R.id.textview_loginuser_formsubmissionerrors)
    lateinit var tvFormSubmissionErrors: TextView

    @BindView(R.id.textinputlayout_loginuser_email)
    lateinit var tilEmail: TextInputLayout

    @BindView(R.id.textinputlayout_loginuser_password)
    lateinit var tilPassword: TextInputLayout

    @BindView(R.id.textinputedittext_loginuser_email)
    lateinit var tietEmail: TextInputEditText

    @BindView(R.id.textinputedittext_loginuser_password)
    lateinit var tietPassword: TextInputEditText

    @BindView(R.id.button_loginuser_login)
    lateinit var btnLogin: Button

    @BindView(R.id.button_loginuser_linktoregister)
    lateinit var btnLinkToRegister: Button

    @BindView(R.id.button_loginuser_linktoresetpassword)
    lateinit var btnLinkToResetPassword: Button

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_loginuser, container, false)
        unbinder = ButterKnife.bind(this, view)

        applyTextChangedEvents()
        applyEditorActionEvents()
        preventButtonTextCapitalization()

        return view
    }

    override fun onResume() {
        super.onResume()
        try {
            (activity as MainActivity?)!!.onSetActionBarTitle(getString(R.string.all_login))
        } catch (e: NullPointerException) {
            // Activity doesn't exists already.
        }
        applyOrientationPortrait()
    }

    override fun onPause() {
        super.onPause()
        applyOrientationFullSensor()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
        disposable.clear()
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

    private fun handleLoginUser() {
        val areFieldsValid = validateEmail() and validatePassword()
        if (areFieldsValid) {
            dbLoader!!.reCreateDb()
            val email = tietEmail!!.text.toString().trim { it <= ' ' }
            val password = tietPassword!!.text.toString().trim { it <= ' ' }
            val loginUserRequest = LoginUserRequest()
            loginUserRequest.email = email
            loginUserRequest.password = password
            apiService
                    .loginUser(loginUserRequest)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribeWith(createLoginUserDisposableSingleObserver())?.let {
                        disposable.add(
                                it
                        )
                    }
        }
    }

    private fun createLoginUserDisposableSingleObserver(): DisposableSingleObserver<LoginUserResponse?> {
        return object : DisposableSingleObserver<LoginUserResponse?>() {
            override fun onSuccess(loginUserResponse: LoginUserResponse) {
                Log.d(TAG, "Login Response: $loginUserResponse")
                if (loginUserResponse != null && loginUserResponse.error == "false") {
                    handleLogin(loginUserResponse)
                    onFinishLoginUser()
                } else if (loginUserResponse != null) {
                    var message = loginUserResponse.getMessage()
                    if (message == null) message = "Unknown error"
                    onSyncError(message)
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Login Response - onFailure: $throwable")
            }
        }
    }

    private fun handleLogin(loginUserResponse: LoginUserResponse) {
        val user = User(loginUserResponse)
        dbLoader!!.createUser(user)
        sessionManager!!.setLogin(true)
    }

    private fun preventButtonTextCapitalization() {
        btnLinkToRegister!!.transformationMethod = null
        btnLinkToResetPassword!!.transformationMethod = null
    }

    private fun applyEditorActionEvents() {
        tietPassword!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                btnLogin!!.performClick()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun applyTextChangedEvents() {
        tietEmail!!.addTextChangedListener(MyTextWatcher(tietEmail!!))
        tietPassword!!.addTextChangedListener(MyTextWatcher(tietPassword!!))
    }

    private fun applyOrientationPortrait() {
        if (activity != null) activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun applyOrientationFullSensor() {
        if (activity != null) activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    private fun validateEmail(): Boolean {
        val givenEmail = tietEmail!!.text.toString().trim { it <= ' ' }
        return if (givenEmail.isEmpty()) {
            tilEmail!!.error = getString(R.string.registeruser_enteremailhint)
            false
        } else {
            tilEmail!!.isErrorEnabled = false
            true
        }
    }

    private fun validatePassword(): Boolean {
        val givenPassword = tietPassword!!.text.toString().trim { it <= ' ' }
        return if (givenPassword.isEmpty()) {
            tilPassword!!.error = getString(R.string.registeruser_enterpasswordhint)
            false
        } else {
            tilPassword!!.isErrorEnabled = false
            true
        }
    }

    fun onFinishLoginUser() {
        hideFormSubmissionErrors()
        (activity as MainActivity?)!!.onFinishLoginUser()
    }

    fun onSyncError(errorMessage: String) {
        showErrorMessage(errorMessage)
    }

    private fun showErrorMessage(errorMessage: String) {
        if (errorMessage.contains("failed to connect")) {
            hideFormSubmissionErrors()
            showFailedToConnectError()
        } else if (errorMessage.contains("Login failed. Incorrect credentials")) {
            showIncorrectCredentialsError()
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

    private fun showIncorrectCredentialsError() {
        try {
            tvFormSubmissionErrors!!.setText(R.string.loginuser_error)
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

    @OnClick(R.id.button_loginuser_login)
    fun onBtnLoginClick(view: View?) {
        hideSoftInput()
        handleLoginUser()
    }

    @OnClick(R.id.button_loginuser_linktoregister)
    fun onBtnLinkToRegisterClick(view: View?) {
        (activity as MainActivity?)!!.onClickLinkToRegisterUser()
    }

    @OnClick(R.id.button_loginuser_linktoresetpassword)
    fun onBtnLinkToResetPasswordClick(view: View?) {
        (activity as MainActivity?)!!.onClickLinkToResetPassword()
    }

    private inner class MyTextWatcher(private val view: View) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            when (view.id) {
                R.id.textinputedittext_loginuser_email -> validateEmail()
                R.id.textinputedittext_loginuser_password -> validatePassword()
            }
        }

    }
}