package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.User
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordRequest
import com.rolandvitezhu.todocloud.repository.UserRepository
import javax.inject.Inject

class UserViewModel : ViewModel() {

    @Inject
    lateinit var userRepository: UserRepository
    @Inject
    lateinit var dbLoader: DbLoader

    private val _user = MutableLiveData(User())
    val user: LiveData<User>
        get() = _user
    val password = MutableLiveData<String>()
    val confirmPassword = MutableLiveData<String>()
    val currentPassword = MutableLiveData<String>()

    fun isEmailProvided() = !_user.value?.email.isNullOrBlank()
    fun isPasswordProvided() = !password.value.isNullOrBlank()
    fun isCurrentPasswordProvided() = !currentPassword.value.isNullOrBlank()
    private fun isConfirmPasswordProvided() = !confirmPassword.value.isNullOrBlank()
    fun isNameValid()  = !_user.value?.name.isNullOrBlank()
    fun isEmailValid() = isEmailProvided() && isValidEmailPattern()
    fun isPasswordValid() = isPasswordProvided() && isValidPasswordPattern()
    fun isConfirmPasswordValid() =
            isConfirmPasswordProvided() && password.value == confirmPassword.value

    /**
     * Set the user.
     */
    suspend fun updateUserViewModel() {
        _user.value = dbLoader.user
    }

    /**
     * Check whether the pattern of the email is valid.
     */
    private fun isValidEmailPattern(): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(_user.value?.email ?: "").matches()
    }

    /**
     * Check whether the pattern of the password is valid.
     * A valid password should contain at least a lowercase letter, an uppercase letter, a number,
     * it should not contain whitespace characters and it should be at least 8 characters long.
     */
    private fun isValidPasswordPattern(): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$"
        return password.value?.matches(passwordRegex.toRegex()) ?: false
    }

    /**
     * Register a user on the Web and create it in the remote and in the local database.
     */
    suspend fun onRegisterUser() {
        password.value?.let { user.value?.password = it }
        user.value?.let { userRepository.registerUser(it) }
    }

    /**
     * Log in the user to the application. Send the user login data to the remote web service to
     * authenticate, get the user data from the remote database through the web service call and
     * save it into the local database.
     */
    suspend fun onLoginUser() {
        dbLoader.reCreateDb()
        password.value?.let { user.value?.password = it }
        _user.value = user.value?.let { userRepository.loginUser(it) }
        dbLoader.createUser(_user.value)
    }

    /**
     * Generate and set a new password for the user and send it to it's e-mail address.
     */
    suspend fun onResetPassword() {
        user.value?.let { userRepository.resetPassword(it) }
    }

    /**
     * Modify the current password of the user to the one that the user have provided.
     */
    suspend fun onModifyPassword() {
        userRepository.modifyPassword(ModifyPasswordRequest(currentPassword.value, password.value))
    }

    init {
        instance?.appComponent?.inject(this)
    }
}