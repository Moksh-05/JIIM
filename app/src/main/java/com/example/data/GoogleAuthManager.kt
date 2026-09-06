package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class GoogleAuthState(
  val isConnected: Boolean = false,
  val email: String? = null,
  val displayName: String? = null,
  val photoUrl: String? = null,
  val accessToken: String? = null,
  val customApiKey: String? = null,
  val isAuthenticating: Boolean = false,
  val message: String? = null,
  val linkedSpreadsheetId: String? = null,
  val linkedSpreadsheetTitle: String? = null,
  val linkedTabTitle: String? = null,
  val autoExportEnabled: Boolean = false
)

class GoogleAuthManager(private val context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("google_sheets_auth_prefs", Context.MODE_PRIVATE)

  val defaultOAuthClientId = "1000191376254-vnkpdp95g377p3b1ml6fr6pu2qih7n9b.apps.googleusercontent.com"
  val defaultProjectId = "gen-lang-client-0153718473"
  val defaultApiKey = "AIzaSyAkwRMaSZFYtSgJEtiYXvzUH3mlCewyF0c"

  private val _authState = MutableStateFlow(
    GoogleAuthState(
      isConnected = prefs.getBoolean("is_connected", false),
      email = prefs.getString("email", null),
      displayName = prefs.getString("display_name", null),
      photoUrl = prefs.getString("photo_url", null),
      accessToken = prefs.getString("access_token", null),
      customApiKey = prefs.getString("custom_api_key", null),
      linkedSpreadsheetId = prefs.getString("linked_spreadsheet_id", null),
      linkedSpreadsheetTitle = prefs.getString("linked_spreadsheet_title", null),
      linkedTabTitle = prefs.getString("linked_tab_title", "Sheet1"),
      autoExportEnabled = prefs.getBoolean("auto_export_enabled", false)
    )
  )
  val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()

  fun saveLinkedSpreadsheet(spreadsheetId: String, title: String, tabTitle: String) {
    prefs.edit()
      .putString("linked_spreadsheet_id", spreadsheetId)
      .putString("linked_spreadsheet_title", title)
      .putString("linked_tab_title", tabTitle)
      .apply()
    _authState.value = _authState.value.copy(
      linkedSpreadsheetId = spreadsheetId,
      linkedSpreadsheetTitle = title,
      linkedTabTitle = tabTitle
    )
  }

  fun setAutoExport(enabled: Boolean) {
    prefs.edit().putBoolean("auto_export_enabled", enabled).apply()
    _authState.value = _authState.value.copy(autoExportEnabled = enabled)
  }

  fun clearLinkedSpreadsheet() {
    prefs.edit()
      .remove("linked_spreadsheet_id")
      .remove("linked_spreadsheet_title")
      .remove("linked_tab_title")
      .remove("auto_export_enabled")
      .apply()
    _authState.value = _authState.value.copy(
      linkedSpreadsheetId = null,
      linkedSpreadsheetTitle = null,
      linkedTabTitle = null,
      autoExportEnabled = false
    )
  }

  val effectiveApiKey: String
    get() {
      val custom = _authState.value.customApiKey?.trim()
      return if (!custom.isNullOrBlank()) custom else defaultApiKey
    }

  val effectiveAccessToken: String?
    get() = _authState.value.accessToken?.trim()?.ifBlank { null }

  suspend fun signInWithGoogle(activityContext: Context): Result<String> = withContext(Dispatchers.IO) {
    _authState.value = _authState.value.copy(isAuthenticating = true, message = "Opening Google Sign-In...")
    try {
      val credentialManager = CredentialManager.create(activityContext)
      val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(defaultOAuthClientId)
        .setAutoSelectEnabled(false)
        .build()

      val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential

      if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val userEmail = googleIdTokenCredential.id
        val userName = googleIdTokenCredential.displayName ?: userEmail.substringBefore("@")
        val photoUri = googleIdTokenCredential.profilePictureUri?.toString()

        prefs.edit()
          .putBoolean("is_connected", true)
          .putString("email", userEmail)
          .putString("display_name", userName)
          .putString("photo_url", photoUri)
          .apply()

        _authState.value = _authState.value.copy(
          isConnected = true,
          email = userEmail,
          displayName = userName,
          photoUrl = photoUri,
          isAuthenticating = false,
          message = "Signed in as $userEmail"
        )
        Result.success("Signed in as $userEmail")
      } else {
        _authState.value = _authState.value.copy(
          isAuthenticating = false,
          message = "Unexpected credential format received"
        )
        Result.failure(Exception("Unsupported credential type"))
      }
    } catch (e: GetCredentialCancellationException) {
      Log.d("GoogleAuthManager", "User cancelled Google Sign-In")
      _authState.value = _authState.value.copy(
        isAuthenticating = false,
        message = "Sign-in cancelled"
      )
      Result.failure(e)
    } catch (e: GetCredentialException) {
      Log.e("GoogleAuthManager", "Google Sign-In failed: ${e.message}", e)
      // Allow user to use API key directly or enter token
      _authState.value = _authState.value.copy(
        isAuthenticating = false,
        message = "Sign-In notice: ${e.message}. You can still sync public/shared Google Sheets with API Key."
      )
      Result.failure(e)
    } catch (e: Exception) {
      Log.e("GoogleAuthManager", "Error during Google Sign-In", e)
      _authState.value = _authState.value.copy(
        isAuthenticating = false,
        message = "Connection error: ${e.localizedMessage ?: "Unknown"}"
      )
      Result.failure(e)
    }
  }

  fun setManualOAuthToken(token: String) {
    val clean = token.trim()
    prefs.edit().putString("access_token", clean.ifBlank { null }).apply()
    _authState.value = _authState.value.copy(
      accessToken = clean.ifBlank { null },
      isConnected = clean.isNotBlank() || _authState.value.email != null
    )
  }

  fun setCustomApiKey(apiKey: String) {
    val clean = apiKey.trim()
    prefs.edit().putString("custom_api_key", clean.ifBlank { null }).apply()
    _authState.value = _authState.value.copy(customApiKey = clean.ifBlank { null })
  }

  fun disconnect() {
    prefs.edit()
      .putBoolean("is_connected", false)
      .remove("email")
      .remove("display_name")
      .remove("photo_url")
      .remove("access_token")
      .apply()

    _authState.value = _authState.value.copy(
      isConnected = false,
      email = null,
      displayName = null,
      photoUrl = null,
      accessToken = null,
      message = "Google Account disconnected"
    )
  }
}
