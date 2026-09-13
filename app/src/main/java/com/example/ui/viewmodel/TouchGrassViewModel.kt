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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TouchGrassUiState(
    val totalScreenTimeMillis: Long = 0L,
    val streak: UserStreak = UserStreak(),
    val monitoredApps: List<AppUsageInfo> = emptyList(),
    val allInstalledApps: List<AppUsageInfo> = emptyList(),
    val badges: List<BadgeEntity> = emptyList(),
    val records: List<ChallengeRecord> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val hasUsageStatsPermission: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val canDrawOverlays: Boolean = false,
    val currentVerificationState: VerificationState = VerificationState.Idle,
    val lastCompletedRecord: ChallengeRecord? = null,
    val funnySuccessQuote: String = "",
    val grassDetectionResult: GrassDetectionResult? = null,
    val handDetectionResult: HandDetectionResult? = null,
    val livenessResult: LivenessResult? = null,
    val isCameraPermissionGranted: Boolean = false
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

    init {
        loadData()
        observeStreams()
    }

    private fun loadData() {
        viewModelScope.launch {
            refreshPermissions()
            val apps = usageMonitor.getInstalledInteractiveApps()
            val screenTime = usageMonitor.getTodayTotalScreenTimeMillis()
            _uiState.value = _uiState.value.copy(
                allInstalledApps = apps,
                totalScreenTimeMillis = screenTime
            )
        }
    }

    fun refreshPermissions() {
        val hasUsage = usageMonitor.hasUsageStatsPermission()
        val hasA11y = blockingManager.isAccessibilityServiceEnabled()
        val hasOverlay = blockingManager.canDrawOverlays()

        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = hasUsage,
            isAccessibilityEnabled = hasA11y,
            canDrawOverlays = hasOverlay
        )
    }

    private fun observeStreams() {
        viewModelScope.launch {
            combine(
                repository.streakFlow,
                repository.allRecords,
                repository.allBadges,
                preferences.settingsFlow
            ) { streak, records, badges, settings ->
                _uiState.value.copy(
                    streak = streak ?: UserStreak(),
                    records = records,
                    badges = badges,
                    settings = settings
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        viewModelScope.launch {
            usageMonitor.observeMonitoredAppsWithUsage().collect { monitoredList ->
                val totalScreenTime = usageMonitor.getTodayTotalScreenTimeMillis()
                _uiState.value = _uiState.value.copy(
                    monitoredApps = monitoredList.filter { it.isMonitored },
                    allInstalledApps = monitoredList,
                    totalScreenTimeMillis = totalScreenTime
                )
            }
        }
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(isCameraPermissionGranted = granted)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
        }
    }

    fun toggleAppMonitored(packageName: String, appName: String, isMonitored: Boolean) {
        viewModelScope.launch {
            val currentLimit = repository.getAppUsageLimit(packageName)?.customLimitMinutes ?: 30
            repository.updateAppMonitoring(packageName, appName, isMonitored, currentLimit)
        }
    }

    fun setAppLimitMinutes(packageName: String, appName: String, minutes: Int) {
        viewModelScope.launch {
            val isMonitored = repository.getAppUsageLimit(packageName)?.isMonitored ?: true
            repository.updateAppMonitoring(packageName, appName, isMonitored, minutes)
        }
    }

    fun setGlobalLimit(minutes: Int) {
        viewModelScope.launch {
            preferences.setGlobalDailyLimitMinutes(minutes)
        }
    }

    fun setHoldDuration(seconds: Int) {
        viewModelScope.launch {
            preferences.setHoldDurationSeconds(seconds)
        }
    }

    fun toggleFunnyMessages(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setFunnyMessagesEnabled(enabled)
        }
    }

    fun toggleSoundVibration(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setSoundVibrationEnabled(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationsEnabled(enabled)
        }
    }

    fun toggleMonitoringService(enable: Boolean) {
        if (enable) {
            blockingManager.startMonitoringService()
        } else {
            blockingManager.stopMonitoringService()
        }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            repository.resetAllData()
            preferences.resetAllSettings()
            loadData()
        }
    }

    fun startChallengeSession() {
        contactVerifier.reset()
        contactVerifier.start()
        _uiState.value = _uiState.value.copy(
            currentVerificationState = VerificationState.LiveCamera
        )
    }

    fun onFrameProcessed(
        grass: GrassDetectionResult,
        hand: HandDetectionResult,
        liveness: LivenessResult
    ) {
        val state = contactVerifier.currentState
        val oldState = _uiState.value.currentVerificationState

        _uiState.value = _uiState.value.copy(
            currentVerificationState = state,
            grassDetectionResult = grass,
            handDetectionResult = hand,
            livenessResult = liveness
        )

        // Haptic triggers on state transitions
        if (_uiState.value.settings.soundVibrationEnabled) {
            when {
                state is VerificationState.Holding && oldState !is VerificationState.Holding -> {
                    soundHelper.vibrateCountdown()
                }
                state is VerificationState.Holding && oldState is VerificationState.Holding -> {
                    if (state.remainingSeconds != (oldState as? VerificationState.Holding)?.remainingSeconds) {
                        soundHelper.vibrateCountdown()
                    }
                }
                state is VerificationState.CheatingDetected && oldState !is VerificationState.CheatingDetected -> {
                    soundHelper.vibrateCheatWarning()
                }
                state is VerificationState.ContactDetected && oldState !is VerificationState.ContactDetected -> {
                    soundHelper.vibrateStep()
                }
                state is VerificationState.GrassDetected && oldState !is VerificationState.GrassDetected -> {
                    soundHelper.vibrateStep()
                }
            }
        }

        if (state is VerificationState.Verified && oldState !is VerificationState.Verified) {
            onChallengeVerified(state.durationSeconds)
        }
    }

    private fun onChallengeVerified(durationSeconds: Int) {
        viewModelScope.launch {
            if (_uiState.value.settings.soundVibrationEnabled) {
                soundHelper.vibrateSuccess()
            }
            val record = repository.recordGrassTouch(
                durationSeconds = durationSeconds,
                cheatAttempts = contactVerifier.cheatAttempts
            )
            _uiState.value = _uiState.value.copy(
                lastCompletedRecord = record,
                funnySuccessQuote = FunnyQuotes.getRandomSuccessQuote()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundHelper.release()
    }
}
