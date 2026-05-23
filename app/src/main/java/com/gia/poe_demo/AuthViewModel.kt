package com.gia.poe_demo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest

/**
 * AuthViewModel — manages Firebase Authentication state for login and registration.
 * Exposes LiveData so Activities can observe results without holding auth logic themselves.
 */
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()


    /** The currently signed-in Firebase user, or null if signed out. */
    val currentUser: FirebaseUser?
        get() = auth.currentUser



    private val _registerState = MutableLiveData<AuthState>()
    val registerState: LiveData<AuthState> = _registerState

    /**
     * Creates a new Firebase Auth account and updates the display name.
     *
     * @param email     user's email address
     * @param password  user's chosen password
     * @param fullName  used to set the Firebase display name
     * @param username  stored locally in SharedPreferences
     */
    fun register(email: String, password: String, fullName: String, username: String) {
        _registerState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Update Firebase profile display name
                    val profileUpdates = userProfileChangeRequest {
                        displayName = fullName
                    }

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                _registerState.value = AuthState.Success(
                                    user = user,
                                    fullName = fullName,
                                    username = username
                                )
                            } else {
                                // Profile update failed but account was created — still a success
                                _registerState.value = AuthState.Success(
                                    user = user,
                                    fullName = fullName,
                                    username = username
                                )
                            }
                        }
                } else {
                    val message = when {
                        task.exception?.message?.contains("email address is already") == true ->
                            "This email is already registered"
                        task.exception?.message?.contains("badly formatted") == true ->
                            "Invalid email address"
                        else -> task.exception?.message ?: "Registration failed"
                    }
                    _registerState.value = AuthState.Error(message)
                }
            }
    }



    private val _loginState = MutableLiveData<AuthState>()
    val loginState: LiveData<AuthState> = _loginState

    /**
     * Signs in an existing user with email and password.
     *
     * @param email    user's email address
     * @param password user's password
     */
    fun login(email: String, password: String) {
        _loginState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    _loginState.value = AuthState.Success(
                        user = user,
                        fullName = user?.displayName ?: "",
                        username = user?.email ?: ""
                    )
                } else {
                    val message = when {
                        task.exception?.message?.contains("password is invalid") == true ->
                            "Incorrect email or password"
                        task.exception?.message?.contains("no user record") == true ->
                            "No account found with this email"
                        task.exception?.message?.contains("blocked") == true ->
                            "Too many attempts. Try again later"
                        else -> task.exception?.message ?: "Login failed"
                    }
                    _loginState.value = AuthState.Error(message)
                }
            }
    }



    /**
     * Signs out the current user from Firebase.
     */
    fun signOut() {
        auth.signOut()
    }


    /**
     * Represents every possible outcome of a login or register call.
     */
    sealed class AuthState {
        /** Request is in flight — show a loading indicator. */
        object Loading : AuthState()

        /** Firebase call succeeded. */
        data class Success(
            val user: FirebaseUser?,
            val fullName: String,
            val username: String
        ) : AuthState()

        /** Firebase call failed. [message] is safe to show to the user. */
        data class Error(val message: String) : AuthState()
    }
}