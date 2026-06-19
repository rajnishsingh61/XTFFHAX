package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CallLog
import com.example.data.Contact
import com.example.data.DialerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DialerViewModel(
    application: Application,
    private val repository: DialerRepository
) : AndroidViewModel(application) {

    // Contacts & Favorites
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val contacts: StateFlow<List<Contact>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allContacts
            } else {
                repository.searchContacts(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteContacts: StateFlow<List<Contact>> = repository.favoriteContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLog>> = repository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // State for Dialer digits
    private val _dialpadDigits = MutableStateFlow("")
    val dialpadDigits = _dialpadDigits.asStateFlow()

    private val _isDialpadVisible = MutableStateFlow(false)
    val isDialpadVisible = _isDialpadVisible.asStateFlow()


    // State for Call
    private val _isCallActive = MutableStateFlow(false)
    val isCallActive = _isCallActive.asStateFlow()

    private val _activeCallName = MutableStateFlow("")
    val activeCallName = _activeCallName.asStateFlow()

    private val _activeCallNumber = MutableStateFlow("")
    val activeCallNumber = _activeCallNumber.asStateFlow()

    private val _activeCallTimer = MutableStateFlow(0)
    val activeCallTimer = _activeCallTimer.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn = _isSpeakerOn.asStateFlow()

    private val _inCallKeypadOpen = MutableStateFlow(false)
    val inCallKeypadOpen = _inCallKeypadOpen.asStateFlow()

    private val _inCallDialpadDigits = MutableStateFlow("")
    val inCallDialpadDigits = _inCallDialpadDigits.asStateFlow()

    private val _activeVoiceEffect = MutableStateFlow("Normal")
    val activeVoiceEffect = _activeVoiceEffect.asStateFlow()

    private val _isIncomingCall = MutableStateFlow(false)
    val isIncomingCall = _isIncomingCall.asStateFlow()

    private var callTimerJob: Job? = null
    private var callStartTime: Long = 0

    init {
        // Run database priming on start
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
    }

    // Search query update
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Dial pad digits actions
    fun appendDialpadDigit(char: Char) {
        _dialpadDigits.value += char
    }

    fun deleteDialpadDigit() {
        if (_dialpadDigits.value.isNotEmpty()) {
            _dialpadDigits.value = _dialpadDigits.value.dropLast(1)
        }
    }

    fun clearDialpadDigits() {
        _dialpadDigits.value = ""
    }

    fun setDialpadVisible(visible: Boolean) {
        _isDialpadVisible.value = visible
    }


    // Contact Database Operations
    fun addContact(name: String, phoneNumber: String, email: String, isFavorite: Boolean) {
        viewModelScope.launch {
            val colors = listOf("#4285F4", "#ea4335", "#f4b400", "#34a853", "#a142f4", "#f43f5e", "#10b981", "#06b6d4")
            val randomColor = colors.random()
            repository.insertContact(
                Contact(
                    name = name,
                    phoneNumber = phoneNumber,
                    email = email,
                    isFavorite = isFavorite,
                    avatarColorHex = randomColor
                )
            )
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun toggleContactFavorite(contact: Contact) {
        viewModelScope.launch {
            repository.updateContact(contact.copy(isFavorite = !contact.isFavorite))
        }
    }

    fun updateContactDetails(contact: Contact, name: String, phoneNumber: String, email: String) {
        viewModelScope.launch {
            repository.updateContact(contact.copy(name = name, phoneNumber = phoneNumber, email = email))
        }
    }


    // Call history log actions
    fun deleteCallLog(log: CallLog) {
        viewModelScope.launch {
            repository.deleteCallLog(log)
        }
    }

    fun clearCallLogs() {
        viewModelScope.launch {
            repository.clearAllCallLogs()
        }
    }


    // Voice effects list
    val voiceEffects = listOf(
        VoiceEffect("Normal", "Original voice", "mic"),
        VoiceEffect("Deep Voice", "Low pitch, thick resonant voice", "volume_up"),
        VoiceEffect("Robot", "High tech, metallic synthetic voice", "android"),
        VoiceEffect("Baby Voice", "Sweet, soft toddler pitch", "face"),
        VoiceEffect("Chipmunk", "High pitch hyperactive voice", "speed"),
        VoiceEffect("Echo", "Repetitive, trailing acoustic delay", "record_voice_over")
    )

    fun changeVoiceEffect(effectName: String) {
        _activeVoiceEffect.value = effectName
    }


    // Call Controlling
    fun initiateCall(number: String, contactName: String? = null) {
        if (number.trim().isEmpty()) return

        viewModelScope.launch {
            val resolvedName = contactName ?: repository.getContactByNumber(number)?.name ?: "Unknown"
            
            _activeCallNumber.value = number
            _activeCallName.value = resolvedName
            _isIncomingCall.value = false
            _isCallActive.value = true
            _activeCallTimer.value = 0
            _activeVoiceEffect.value = "Normal"
            _isMuted.value = false
            _isSpeakerOn.value = false
            _inCallKeypadOpen.value = false
            _inCallDialpadDigits.value = ""

            // Hide main dialpad
            _isDialpadVisible.value = false

            startCallTimer()
            callStartTime = System.currentTimeMillis()
        }
    }

    fun simulateIncomingCall(number: String = "+1 (555) 012-7491", contactName: String = "Sophia Martinez") {
        _activeCallNumber.value = number
        _activeCallName.value = contactName
        _isIncomingCall.value = true
        _isCallActive.value = true
        _activeCallTimer.value = 0
        _activeVoiceEffect.value = "Normal"
        _isMuted.value = false
        _isSpeakerOn.value = false
        _inCallKeypadOpen.value = false
        _inCallDialpadDigits.value = ""
    }

    fun acceptIncomingCall() {
        _isIncomingCall.value = false
        startCallTimer()
        callStartTime = System.currentTimeMillis()
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        _activeCallTimer.value = 0
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeCallTimer.value += 1
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleInCallKeypad() {
        _inCallKeypadOpen.value = !_inCallKeypadOpen.value
    }

    fun appendInCallKeypadDigit(char: Char) {
        _inCallDialpadDigits.value += char
    }

    fun endActiveCall() {
        callTimerJob?.cancel()
        callTimerJob = null
        
        val duration = _activeCallTimer.value
        val number = _activeCallNumber.value
        val name = _activeCallName.value
        val effect = _activeVoiceEffect.value
        val isIncoming = _isIncomingCall.value

        _isCallActive.value = false
        _isIncomingCall.value = false

        if (number.isNotEmpty() && duration > 0) {
            viewModelScope.launch {
                val callType = if (isIncoming) "MISSED" else "OUTGOING" // If accepted in simulation, we logs OUTGOING or INCOMING depending on flow
                val finalType = if (isIncoming) "MISSED" else "OUTGOING"
                
                repository.insertCallLog(
                    CallLog(
                        callerName = name,
                        phoneNumber = number,
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = duration,
                        callType = if (isIncoming) "INCOMING" else "OUTGOING",
                        voiceEffectUsed = effect
                    )
                )
            }
        } else if (number.isNotEmpty() && isIncoming) {
            // Ignored or rejected incoming call -> Missed call
            viewModelScope.launch {
                repository.insertCallLog(
                    CallLog(
                        callerName = name,
                        phoneNumber = number,
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = 0,
                        callType = "MISSED",
                        voiceEffectUsed = "None"
                    )
                )
            }
        }
    }
}

data class VoiceEffect(
    val name: String,
    val description: String,
    val iconName: String
)

class DialerViewModelFactory(
    private val application: Application,
    private val repository: DialerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DialerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DialerViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
