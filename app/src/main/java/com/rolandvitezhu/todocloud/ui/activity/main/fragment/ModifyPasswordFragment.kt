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
import android.widget.TextView.OnEditorActionListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.databinding.FragmentModifypasswordBinding
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordRequest
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordResponse
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_modifypassword.*
import kotlinx.android.synthetic.main.fragment_modifypassword.view.*
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject

class ModifyPasswordFragment : Fragment() {

    private val TAG = javaClass.simpleName
    private lateinit var apiService: ApiService
    private val disposable = CompositeDisposable()

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
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val fragmentModifypasswordBinding: FragmentModifypasswordBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_modifypassword, container, false)
        val view: View = fragmentModifypasswordBinding.root
        fragmentModifypasswordBinding.modifyPasswordFragment = this

        applyTextChangedEvents(view)
        applyEditorActionEvents(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.onSetActionBarTitle(getString(R.string.all_change_password))
        applyOrientationPortrait()
    }

    override fun onPause() {
        super.onPause()
        applyOrientationFullSensor()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }

    private fun applyTextChangedEvents(view: View) {
        view.textinputedittext_modifypassword_currentpassword!!.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_modifypassword_currentpassword!!))
        view.textinputedittext_modifypassword_newpassword!!.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_modifypassword_newpassword!!))
        view.textinputedittext_modifypassword_confirmpassword!!.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_modifypassword_confirmpassword!!))
    }

    private fun applyEditorActionEvents(view: View) {
        view.textinputedittext_modifypassword_confirmpassword!!.setOnEditorActionListener(
                OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                view.button_changepassword!!.performClick()
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

    private fun handleChangePassword() {
        val areFieldsValid = (validateCurrentPassword()
                and validateNewPassword()
                and validateConfirmPassword())
        if (areFieldsValid) {
            val currentPassword =
                    this.textinputedittext_modifypassword_currentpassword!!.text.toString().trim { it <= ' ' }
            val newPassword = this.textinputedittext_modifypassword_newpassword!!.text.toString().trim { it <= ' ' }
            val modifyPasswordRequest = ModifyPasswordRequest()
            modifyPasswordRequest.currentPassword = currentPassword
            modifyPasswordRequest.newPassword = newPassword
            apiService
                    .modifyPassword(modifyPasswordRequest)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribeWith(createModifyPasswordDisposableSingleObserver())?.let {
                        disposable.add(
                                it
                        )
                    }
        }
    }

    private fun createModifyPasswordDisposableSingleObserver(): DisposableSingleObserver<ModifyPasswordResponse?> {
        return object : DisposableSingleObserver<ModifyPasswordResponse?>() {
            override fun onSuccess(modifyPasswordResponse: ModifyPasswordResponse) {
                Log.d(TAG, "Modify Password Response: $modifyPasswordResponse")
                if (modifyPasswordResponse != null && modifyPasswordResponse.error == "false") {
                    onFinishModifyPassword()
                } else if (modifyPasswordResponse != null) {
                    var message = modifyPasswordResponse.getMessage()
                    if (message == null) message = "Unknown error"
                    onSyncError(message)
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Modify Password Response - onFailure: $throwable")
            }
        }
    }

    private fun applyOrientationPortrait() {
        if (activity != null) activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun applyOrientationFullSensor() {
        if (activity != null) activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    private fun validateCurrentPassword(): Boolean {
        val givenName = this.textinputedittext_modifypassword_currentpassword!!.text.toString().trim { it <= ' ' }
        return if (givenName.isEmpty()) {
            this.textinputlayout_modifypassword_currentpassword!!.error = getString(R.string.modifypassword_currentpassworderrorlabel)
            false
        } else {
            this.textinputlayout_modifypassword_currentpassword!!.isErrorEnabled = false
            true
        }
    }

    private fun validateNewPassword(): Boolean {
        val givenPassword = this.textinputedittext_modifypassword_newpassword!!.text.toString().trim { it <= ' ' }
        val isGivenPasswordValid = !givenPassword.isEmpty() && isValidPassword(givenPassword)
        return if (!isGivenPasswordValid) {
            this.textinputlayout_modifypassword_newpassword!!.error = getString(R.string.registeruser_enterproperpasswordhint)
            false
        } else {
            this.textinputlayout_modifypassword_newpassword!!.isErrorEnabled = false
            true
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val givenPassword =
                this.textinputedittext_modifypassword_newpassword!!.text.toString().trim { it <= ' ' }
        val givenConfirmPassword =
                this.textinputedittext_modifypassword_confirmpassword!!.text.toString().trim { it <= ' ' }
        val isGivenConfirmPasswordValid = (!givenConfirmPassword.isEmpty()
                && givenPassword == givenConfirmPassword)
        return if (!isGivenConfirmPasswordValid) {
            this.textinputlayout_modifypassword_confirmpassword!!.error = getString(R.string.registeruser_confirmpassworderrorlabel)
            false
        } else {
            this.textinputlayout_modifypassword_confirmpassword!!.isErrorEnabled = false
            true
        }
    }

    /**
     * Valid password should contain at least a lowercase letter, an uppercase letter, a number,
     * it should not contain whitespace character and it should be at least 8 characters long.
     */
    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }

    fun onFinishModifyPassword() {
        hideFormSubmissionErrors()
        (activity as MainActivity?)!!.onFinishModifyPassword()
    }

    fun onSyncError(errorMessage: String) {
        showErrorMessage(errorMessage)
    }

    private fun showErrorMessage(errorMessage: String) {
        if (errorMessage.contains("failed to connect")) {
            hideFormSubmissionErrors()
            showFailedToConnectError()
        } else if (errorMessage.contains("Your current password is incorrect.")) {
            showIncorrectCurrentPasswordError()
        } else {
            hideFormSubmissionErrors()
            showAnErrorOccurredError()
        }
    }

    private fun hideFormSubmissionErrors() {
        try {
            this.textview_modifypassword_formsubmissionerrors!!.text = ""
            this.textview_modifypassword_formsubmissionerrors!!.visibility = View.GONE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showFailedToConnectError() {
        try {
            val snackbar = Snackbar.make(
                    this.constraintlayout_modifypassword!!,
                    R.string.all_failedtoconnect,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    private fun showIncorrectCurrentPasswordError() {
        try {
            this.textview_modifypassword_formsubmissionerrors!!.setText(R.string.modifypassword_incorrectcurrentpassword)
            this.textview_modifypassword_formsubmissionerrors!!.visibility = View.VISIBLE
        } catch (e: NullPointerException) {
            // TextView doesn't exists already.
        }
    }

    private fun showAnErrorOccurredError() {
        try {
            val snackbar = Snackbar.make(
                    this.constraintlayout_modifypassword!!,
                    R.string.all_anerroroccurred,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    private inner class MyTextWatcher(private val view: View) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            when (view.id) {
                R.id.textinputedittext_modifypassword_currentpassword -> validateCurrentPassword()
                R.id.textinputedittext_modifypassword_newpassword -> validateNewPassword()
                R.id.textinputedittext_modifypassword_confirmpassword -> {
                }
            }
        }

    }

    fun onBtnChangePasswordClick(view: View) {
        hideSoftInput()
        handleChangePassword()
    }
}