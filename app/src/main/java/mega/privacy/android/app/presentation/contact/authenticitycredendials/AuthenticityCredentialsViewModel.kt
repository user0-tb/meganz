package mega.privacy.android.app.presentation.contact.authenticitycredendials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.contact.authenticitycredendials.model.AuthenticityCredentialsState
import mega.privacy.android.app.presentation.extensions.getErrorStringId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.AreCredentialsVerified
import mega.privacy.android.domain.usecase.GetContactCredentials
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.GetMyCredentials
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.ResetCredentials
import mega.privacy.android.domain.usecase.VerifyCredentials
import javax.inject.Inject

/**
 * View model for [AuthenticityCredentialsActivity].
 *
 * @property getContactCredentials  [GetContactCredentials]
 * @property areCredentialsVerified [AreCredentialsVerified]
 * @property getMyCredentials       [GetMyCredentials]
 * @property verifyCredentials      [VerifyCredentials]
 * @property resetCredentials       [ResetCredentials]
 * @property state                  Current view state as [AuthenticityCredentialsState]
 */
@HiltViewModel
class AuthenticityCredentialsViewModel @Inject constructor(
    private val getContactCredentials: GetContactCredentials,
    private val areCredentialsVerified: AreCredentialsVerified,
    private val getMyCredentials: GetMyCredentials,
    private val verifyCredentials: VerifyCredentials,
    private val resetCredentials: ResetCredentials,
    monitorConnectivity: MonitorConnectivity,
    private val getFeatureFlagValue: GetFeatureFlagValue,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthenticityCredentialsState())
    val state: StateFlow<AuthenticityCredentialsState> = _state

    private val isConnected =
        monitorConnectivity().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            _state.update { it.copy(myAccountCredentials = getMyCredentials()) }
        }
        getMandatoryFingerPrintVerificationFeatureFlag()
    }

    /**
     * Request data related to a contact.
     *
     * @param userEmail User's email.
     */
    fun requestData(userEmail: String) {
        viewModelScope.launch {
            _state.update { it.copy(contactCredentials = getContactCredentials(userEmail)) }
        }
        viewModelScope.launch {
            _state.update { it.copy(areCredentialsVerified = areCredentialsVerified(userEmail)) }
        }
    }

    /**
     * Resets credentials if already verified, verifies them if not.
     */
    fun actionClicked() {
        when {
            !isConnected.value -> {
                _state.update { it.copy(error = R.string.check_internet_connection_error) }
            }
            state.value.isVerifyingCredentials -> {
                _state.update { it.copy(error = R.string.already_verifying_credentials) }
            }
            state.value.areCredentialsVerified -> {
                resetContactCredentials()
            }
            else -> {
                verifyContactCredentials()
            }
        }
    }

    /**
     * Verifies contact credentials.
     */
    private fun verifyContactCredentials() = state.value.contactCredentials?.let {
        _state.update { it.copy(isVerifyingCredentials = true) }

        viewModelScope.launch {
            kotlin.runCatching {
                verifyCredentials(it.email)
            }.onSuccess {
                _state.update {
                    it.copy(
                        areCredentialsVerified = true,
                        isVerifyingCredentials = false,
                        error = R.string.label_verified)
                }
            }.onFailure { showError(it) }
        }
    }

    /**
     * Reset contact credentials.
     */
    private fun resetContactCredentials() = state.value.contactCredentials?.let {
        _state.update { it.copy(isVerifyingCredentials = true) }

        viewModelScope.launch {
            kotlin.runCatching {
                resetCredentials(it.email)
            }.onSuccess {
                _state.update {
                    it.copy(
                        isVerifyingCredentials = false,
                        areCredentialsVerified = false)
                }
            }.onFailure { showError(it) }
        }
    }

    /**
     * Updates the state for showing error.
     */
    private fun showError(error: Throwable) {
        if (error is MegaException) {
            _state.update {
                it.copy(
                    isVerifyingCredentials = false,
                    error = error.getErrorStringId())
            }
        }
    }

    /**
     * Updates state after shown error.
     */
    fun errorShown() = _state.update { it.copy(error = null) }

    private fun getMandatoryFingerPrintVerificationFeatureFlag() {
        viewModelScope.launch {
            _state.update {
                it.copy(isMandatoryFingerPrintVerificationNeeded = getFeatureFlagValue(AppFeatures.MandatoryFingerprintVerification))
            }
        }
    }
}