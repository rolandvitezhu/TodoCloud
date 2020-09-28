package com.rolandvitezhu.todocloud.repository

import com.rolandvitezhu.todocloud.data.User
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordRequest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor() : BaseRepository() {

    /**
     * Register a user on the Web and create it in the remote and in the local database.
     */
    suspend fun registerUser(user: User) {
        val response = apiService.registerUser(user)
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response.
            // There is no response data to process.
        } else {
            // Process the error response.
            throw Throwable(response.message)
        }
    }

    /**
     * Log in the user to the application. Send the user login data to the remote web service to
     * authenticate, get the user data from the remote database through the web service call and
     * save it into the local database.
     */
    suspend fun loginUser(user: User): User {
        val response = apiService.loginUser(user)
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response.
            return User(response)
        } else {
            // Process the error response.
            throw Throwable(response.message)
        }
    }

    /**
     * Generate and set a new password for the user and send it to it's e-mail address.
     */
    suspend fun resetPassword(user: User) {
        val response = apiService.resetPassword(user)
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response.
            // There is no response data to process.
        } else {
            // Process the error response.
            throw Throwable(response.message)
        }
    }

    /**
     * Modify the current password of the user to the one that the user have provided.
     */
    suspend fun modifyPassword(modifyPasswordRequest: ModifyPasswordRequest) {
        val response = apiService.modifyPassword(modifyPasswordRequest)
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response.
            // There is no data to process.
        } else {
            // Process the error response.
            throw Throwable(response.message)
        }
    }
}