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
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.databinding.FragmentModifypasswordBinding
import com.rolandvitezhu.todocloud.helper.GeneralHelper.validateField
import com.rolandvitezhu.todocloud.helper.applyOrientationFullSensor
import com.rolandvitezhu.todocloud.helper.applyOrientationPortrait
import com.rolandvitezhu.todocloud.helper.hideSoftInput
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.fragment_modifypassword.*
import kotlinx.android.synthetic.main.fragment_modifypassword.view.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject

class ModifyPasswordFragment : Fragment() {

    private val TAG = javaClass.simpleName
    private lateinit var apiService: ApiService

    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao
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
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val fragmentModifypasswordBinding: FragmentModifypasswordBinding =
                FragmentModifypasswordBinding.inflate(inflater, container, false)
        val view: View = fragmentModifypasswordBinding.root

        applyTextChangedEvents(view)

        fragmentModifypasswordBinding.lifecycleOwner = this
        fragmentModifypasswordBinding.modifyPasswordFragment = this
        fragmentModifypasswordBinding.userViewModel = userViewModel
        fragmentModifypasswordBinding.executePendingBindings()

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.onSetActionBarTitle(getString(R.string.all_change_password))
        applyOrientationPortrait()
    }

    override fun onPause() {
        super.onPause()
        applyOrientationFullSensor()
    }

    private fun applyTextChangedEvents(view: View) {
        view.textinputedittext_modifypassword_currentpassword?.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_modifypassword_currentpassword!!))
        view.textinputedittext_modifypassword_newpassword?.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_modifypassword_newpassword!!))
        view.textinputedittext_modifypassword_confirmpassword?.addTextChangedListener(
                MyTextWatcher(view.textinputedittext_modifypassword_confirmpassword!!))
    }

    private fun handleChangePassword() {
        if (validateCurrentPassword()
                and validateNewPassword()
                and validateConfirmPassword()) {
            lifecycleScope.launch {
                try {
                    userViewModel.onModifyPassword()
                    onFinishModifyPassword()
                } catch (cause: Throwable) {
                    showErrorMessage(cause.message)
                }
            }
        }
    }

    /**
     * Check whether the current password is provided and show an error message, if necessary.
     */
    private fun validateCurrentPassword(): Boolean {
        if (view != null && view?.textinputlayout_modifypassword_currentpassword != null) {
            return validateField(userViewModel.isCurrentPasswordProvided(),
                    view?.textinputlayout_modifypassword_currentpassword as TextInputLayout,
                    getString(R.string.modifypassword_currentpassworderrorlabel))
        }

        return false
    }

    /**
     * Check whether the new password is valid and show an error message, if necessary. The new
     * password is valid, if it is provided and it has a valid password pattern.
     */
    private fun validateNewPassword(): Boolean {
        if (view != null && view?.textinputlayout_modifypassword_newpassword != null) {
            return validateField(userViewModel.isPasswordValid(),
                    view?.textinputlayout_modifypassword_newpassword as TextInputLayout,
                    getString(R.string.registeruser_enterproperpasswordhint))
        }

        return false
    }

    /**
     * Check whether the confirm password field is valid and show an error message, if necessary.
     * The confirm password is valid, if it is provided and matches the new password.
     */
    private fun validateConfirmPassword(): Boolean {
        if (view != null && view?.textinputlayout_modifypassword_confirmpassword != null) {
            return validateField(userViewModel.isConfirmPasswordValid(),
                    view?.textinputlayout_modifypassword_confirmpassword as TextInputLayout,
                    getString(R.string.registeruser_confirmpassworderrorlabel))
        }

        return false
    }

    fun onFinishModifyPassword() {
        hideFormSubmissionErrors()
        (activity as MainActivity?)?.onFinishModifyPassword()
    }

    private fun showErrorMessage(errorMessage: String?) {
        if (errorMessage != null) {
            val upperCaseErrorMessage = errorMessage.toUpperCase()
            if (upperCaseErrorMessage.contains("FAILED TO CONNECT") ||
                    upperCaseErrorMessage.contains("UNABLE TO RESOLVE HOST") ||
                    upperCaseErrorMessage.contains("TIMEOUT")) {
                hideFormSubmissionErrors()
                showFailedToConnectError()
            } else if (upperCaseErrorMessage.contains("YOUR CURRENT PASSWORD IS INCORRECT.")) {
                showIncorrectCurrentPasswordError()
            } else {
                hideFormSubmissionErrors()
                showAnErrorOccurredError()
            }
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
            this.textview_modifypassword_formsubmissionerrors?.setText(R.string.modifypassword_incorrectcurrentpassword)
            this.textview_modifypassword_formsubmissionerrors?.visibility = View.VISIBLE
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

    fun onButtonChangePasswordClick() {
        hideSoftInput()
        handleChangePassword()
    }
}