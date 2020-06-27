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
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.network.api.user.dto.ResetPasswordRequest
import com.rolandvitezhu.todocloud.network.api.user.dto.ResetPasswordResponse
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject

class ResetPasswordFragment : Fragment() {

    private val TAG = javaClass.simpleName
    private lateinit var apiService: ApiService
    private val disposable = CompositeDisposable()

    @Inject
    lateinit var dbLoader: DbLoader

    @Inject
    lateinit var retrofit: Retrofit

    @BindView(R.id.constraintlayout_resetpassword)
    lateinit var constraintLayout: ConstraintLayout

    @BindView(R.id.textview_resetpassword_formsubmissionerrors)
    lateinit var tvFormSubmissionErrors: TextView

    @BindView(R.id.textinputlayout_resetpassword_email)
    lateinit var tilEmail: TextInputLayout

    @BindView(R.id.textinputedittext_resetpassword_email)
    lateinit var tietEmail: TextInputEditText

    @BindView(R.id.button_resetpassword)
    lateinit var btnSubmit: Button

    lateinit var unbinder: Unbinder

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (Objects.requireNonNull(activity)?.application as AppController).appComponent.fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiService = retrofit!!.create(ApiService::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_resetpassword, container, false)
        unbinder = ButterKnife.bind(this, view)
        applyTextChangedEvents()
        applyEditorActionEvents()
        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.onSetActionBarTitle(getString(R.string.all_reset_password))
        applyOrientationPortrait()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
        disposable.clear()
    }

    private fun applyTextChangedEvents() {
        tietEmail!!.addTextChangedListener(MyTextWatcher(tietEmail!!))
    }

    private fun applyEditorActionEvents() {
        tietEmail!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                btnSubmit!!.performClick()
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

    private fun handleResetPassword() {
        if (validateEmail()) {
            val email = tietEmail!!.text.toString().trim { it <= ' ' }
            val resetPasswordRequest = ResetPasswordRequest()
            resetPasswordRequest.email = email
            apiService
                    .resetPassword(resetPasswordRequest)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribeWith(createResetPasswordDisposableSingleObserver())?.let {
                        disposable.add(
                                it
                        )
                    }
        }
    }

    private fun createResetPasswordDisposableSingleObserver(): DisposableSingleObserver<ResetPasswordResponse?> {
        return object : DisposableSingleObserver<ResetPasswordResponse?>() {
            override fun onSuccess(resetPasswordResponse: ResetPasswordResponse) {
                Log.d(TAG, "Reset Password Response: $resetPasswordResponse")
                if (resetPasswordResponse != null && resetPasswordResponse.error == "false") {
                    onFinishResetPassword()
                } else if (resetPasswordResponse != null) {
                    var message = resetPasswordResponse.getMessage()
                    if (message == null) message = "Unknown error"
                    onSyncError(message)
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Reset Password Response - onFailure: $throwable")
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

    private fun isValidEmail(email: String): Boolean {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun onFinishResetPassword() {
        hideFormSubmissionErrors()
        (activity as MainActivity?)!!.onFinishResetPassword()
    }

    fun onSyncError(errorMessage: String) {
        showErrorMessage(errorMessage)
    }

    private fun showErrorMessage(errorMessage: String) {
        if (errorMessage.contains("failed to connect")) {
            hideFormSubmissionErrors()
            showFailedToConnectError()
        } else if (errorMessage.contains("Failed to reset password. Please try again!")) {
            showFailedToResetPasswordError()
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

    private fun showFailedToResetPasswordError() {
        try {
            tvFormSubmissionErrors!!.setText(R.string.modifypassword_failedtoresetpassword)
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

    @OnClick(R.id.button_resetpassword)
    fun onBtnSubmitClick(view: View?) {
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