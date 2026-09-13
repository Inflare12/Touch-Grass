package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TouchGrassApp
import com.example.blocking.AndroidBlockingManager
import com.example.data.database.entity.BadgeEntity
import com.example.data.database.entity.ChallengeRecord
import com.example.data.database.entity.UserStreak
import com.example.data.preferences.AppSettings
import com.example.domain.model.AppUsageInfo
import com.example.domain.model.VerificationState
import com.example.ml.ContactVerifier
import com.example.ml.GrassDetectionResult
import com.example.ml.HandDetectionResult
import com.example.ml.LivenessResult
import com.example.usage.AndroidUsageMonitor
import com.example.utils.FunnyQuotes
import com.example.utils.SoundVibrationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TouchGrassUiState(
    val totalScreenTimeMillis: Long = 0L, val streak: UserStreak = UserStreak(), val monitoredApps: List<AppUsageInfo> = emptyList(),
    val allInstalledApps: List<AppUsageInfo> = emptyList(), val badges: List<BadgeEntity> = emptyList(), val records: List<ChallengeRecord> = emptyList(),
    val settings: AppSettings = AppSettings(), val hasUsageStatsPermission: Boolean = false, val isAccessibilityEnabled: Boolean = false,
    val canDrawOverlays: Boolean = false, val currentVerificationState: VerificationState = VerificationState.Idle, val lastCompletedRecord: ChallengeRecord? = null,
    val funnySuccessQuote: String = "", val grassDetectionResult: GrassDetectionResult? = null, val handDetectionResult: HandDetectionResult? = null,
    val livenessResult: LivenessResult? = null, val isCameraPermissionGranted: Boolean = false
)

class TouchGrassViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TouchGrassApp
    private val repository = app.repository
    private val preferences = app.preferencesManager
    val usageMonitor = AndroidUsageMonitor(application, repository)
    val blockingManager = AndroidBlockingManager(application)
    val soundHelper = SoundVibrationHelper(application)
    val contactVerifier = ContactVerifier(requiredHoldSeconds = 3)
    private val _uiState = MutableStateFlow(TouchGrassUiState())
    val uiState: StateFlow<TouchGrassUiState> = _uiState.asStateFlow()

    init { loadData(); observeStreams() }
    private fun loadData() { viewModelScope.launch { refreshPermissions(); _uiState.value = _uiState.value.copy(allInstalledApps = usageMonitor.getInstalledInteractiveApps(), totalScreenTimeMillis = usageMonitor.getTodayTotalScreenTimeMillis()) } }
    fun refreshPermissions() { _uiState.value = _uiState.value.copy(hasUsageStatsPermission = usageMonitor.hasUsageStatsPermission(), isAccessibilityEnabled = blockingManager.isAccessibilityServiceEnabled(), canDrawOverlays = blockingManager.canDrawOverlays()) }
    private fun observeStreams() {
        viewModelScope.launch { combine(repository.streakFlow, repository.allRecords, repository.allBadges, preferences.settingsFlow) { streak, records, badges, settings -> _uiState.value.copy(streak = streak ?: UserStreak(), records = records, badges = badges, settings = settings) }.collect { _uiState.value = it } }
        viewModelScope.launch { usageMonitor.observeMonitoredAppsWithUsage().collect { list -> _uiState.value = _uiState.value.copy(monitoredApps = list.filter { it.isMonitored }, allInstalledApps = list, totalScreenTimeMillis = usageMonitor.getTodayTotalScreenTimeMillis()) } }
    }
    fun setCameraPermissionGranted(granted: Boolean) { _uiState.value = _uiState.value.copy(isCameraPermissionGranted = granted) }
    fun completeOnboarding() { viewModelScope.launch { preferences.setOnboardingCompleted(true) } }
    fun toggleAppMonitored(packageName: String, appName: String, isMonitored: Boolean) = viewModelScope.launch { repository.updateAppMonitoring(packageName, appName, isMonitored, repository.getAppUsageLimit(packageName)?.customLimitMinutes ?: 30) }
    fun setAppLimitMinutes(packageName: String, appName: String, minutes: Int) = viewModelScope.launch { val safeMinutes = minutes.coerceIn(1, 24 * 60); repository.updateAppMonitoring(packageName, appName, repository.getAppUsageLimit(packageName)?.isMonitored ?: true, safeMinutes) }
    fun setGlobalLimit(minutes: Int) = viewModelScope.launch { preferences.setGlobalDailyLimitMinutes(minutes.coerceIn(1, 24 * 60)) }
    fun setHoldDuration(seconds: Int) = viewModelScope.launch { preferences.setHoldDurationSeconds(seconds.coerceIn(1, 30)) }
    fun toggleFunnyMessages(enabled: Boolean) = viewModelScope.launch { preferences.setFunnyMessagesEnabled(enabled) }
    fun toggleSoundVibration(enabled: Boolean) = viewModelScope.launch { preferences.setSoundVibrationEnabled(enabled) }
    fun toggleNotifications(enabled: Boolean) = viewModelScope.launch { preferences.setNotificationsEnabled(enabled) }
    fun toggleStrictLock(enabled: Boolean) = viewModelScope.launch { preferences.setStrictLockEnabled(enabled) }
    fun toggleMonitoringService(enable: Boolean) { if (enable) blockingManager.startMonitoringService() else blockingManager.stopMonitoringService() }
    fun resetStatistics() = viewModelScope.launch { repository.resetAllData(); preferences.resetAllSettings(); loadData() }
    fun startChallengeSession() { contactVerifier.setRequiredHoldSeconds(_uiState.value.settings.holdDurationSeconds); contactVerifier.start(); _uiState.value = _uiState.value.copy(currentVerificationState = VerificationState.LiveCamera) }
    fun onFrameProcessed(grass: GrassDetectionResult, hand: HandDetectionResult, liveness: LivenessResult) {
        val oldState = _uiState.value.currentVerificationState
        val state = contactVerifier.currentState
        _uiState.value = _uiState.value.copy(currentVerificationState = state, grassDetectionResult = grass, handDetectionResult = hand, livenessResult = liveness)
        if (_uiState.value.settings.soundVibrationEnabled) when {
            state is VerificationState.Holding && oldState !is VerificationState.Holding -> soundHelper.vibrateCountdown()
            state is VerificationState.Holding && oldState is VerificationState.Holding && state.remainingSeconds != oldState.remainingSeconds -> soundHelper.vibrateCountdown()
            state is VerificationState.CheatingDetected && oldState !is VerificationState.CheatingDetected -> soundHelper.vibrateCheatWarning()
            state is VerificationState.ContactDetected && oldState !is VerificationState.ContactDetected -> soundHelper.vibrateStep()
            state is VerificationState.GrassDetected && oldState !is VerificationState.GrassDetected -> soundHelper.vibrateStep()
        }
        if (state is VerificationState.Verified && oldState !is VerificationState.Verified) onChallengeVerified(state.durationSeconds)
    }
    private fun onChallengeVerified(durationSeconds: Int) = viewModelScope.launch {
        if (_uiState.value.settings.soundVibrationEnabled) soundHelper.vibrateSuccess()
        preferences.clearGlobalLock()
        val record = repository.recordGrassTouch(durationSeconds, cheatAttempts = contactVerifier.cheatAttempts)
        _uiState.value = _uiState.value.copy(lastCompletedRecord = record, funnySuccessQuote = FunnyQuotes.getRandomSuccessQuote())
    }
    override fun onCleared() { soundHelper.release(); super.onCleared() }
}
