package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.model.Persona
import com.example.data.remote.GeminiApiClient
import com.example.data.repository.ChatRepository
import com.example.util.AttachedFileInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val sessions: List<ChatSessionEntity> = emptyList(),
    val activeSession: ChatSessionEntity? = null,
    val messages: List<ChatMessageEntity> = emptyList(),
    val inputText: String = "",
    val attachedImageUri: String? = null,
    val attachedFile: AttachedFileInfo? = null,
    val isStreaming: Boolean = false,
    val currentPersona: Persona = Persona.DEFAULT_PERSONA,
    val isDarkMode: Boolean = true,
    val showPersonaDialog: Boolean = false,
    val showExportDialog: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(db.chatDao(), GeminiApiClient(application))

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messageSubscriptionJob: Job? = null
    private var streamingJob: Job? = null

    init {
        // Observe sessions from Room DB
        viewModelScope.launch {
            repository.allSessions.collectLatest { sessionList ->
                _uiState.update { it.copy(sessions = sessionList) }

                // Auto select or create first session if none selected
                if (sessionList.isEmpty()) {
                    createNewSession()
                } else if (_uiState.value.activeSession == null) {
                    selectSession(sessionList.first().id)
                }
            }
        }
    }

    fun selectSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId) ?: return@launch
            val persona = Persona.DEFAULT_PERSONAS.find { it.id == session.personaId } ?: Persona.DEFAULT_PERSONA

            _uiState.update {
                it.copy(
                    activeSession = session,
                    currentPersona = persona
                )
            }

            // Cancel previous message listener and observe new session messages
            messageSubscriptionJob?.cancel()
            messageSubscriptionJob = viewModelScope.launch {
                repository.getMessagesForSession(sessionId).collectLatest { msgList ->
                    _uiState.update { it.copy(messages = msgList) }
                }
            }
        }
    }

    fun createNewSession(persona: Persona = Persona.DEFAULT_PERSONA) {
        viewModelScope.launch {
            val newId = repository.createNewSession(persona)
            selectSession(newId)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            val currentList = _uiState.value.sessions.filter { it.id != sessionId }
            if (currentList.isNotEmpty()) {
                selectSession(currentList.first().id)
            } else {
                createNewSession()
            }
        }
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
            if (_uiState.value.activeSession?.id == sessionId) {
                _uiState.update {
                    it.copy(activeSession = it.activeSession?.copy(title = newTitle))
                }
            }
        }
    }

    fun setPersona(persona: Persona) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentPersona = persona) }
            val currentSession = _uiState.value.activeSession ?: return@launch
            repository.updateSessionPersona(currentSession.id, persona)
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setAttachedImage(uriString: String?) {
        _uiState.update { it.copy(attachedImageUri = uriString, attachedFile = null) }
    }

    fun setAttachedFile(file: AttachedFileInfo?) {
        _uiState.update { it.copy(attachedFile = file, attachedImageUri = null) }
    }

    fun toggleDarkMode(isDark: Boolean) {
        _uiState.update { it.copy(isDarkMode = isDark) }
    }

    fun setShowPersonaDialog(show: Boolean) {
        _uiState.update { it.copy(showPersonaDialog = show) }
    }

    fun setShowExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val text = currentState.inputText.trim()
        val imageUri = currentState.attachedImageUri
        val attachedFile = currentState.attachedFile
        val activeSession = currentState.activeSession ?: return

        if (text.isEmpty() && imageUri.isNullOrEmpty() && attachedFile == null) return

        // Prepare prompt and file metadata
        val fileName = attachedFile?.name
        val promptForAi = StringBuilder(text)

        if (attachedFile != null) {
            if (!attachedFile.textContent.isNullOrBlank()) {
                if (promptForAi.isNotEmpty()) promptForAi.append("\n\n")
                promptForAi.append("[محتوى الملف المرفق (${attachedFile.name})]:\n${attachedFile.textContent}")
            } else {
                if (promptForAi.isNotEmpty()) promptForAi.append("\n\n")
                promptForAi.append("[تم إرفاق ملف: ${attachedFile.name} بحجم ${attachedFile.sizeFormatted}]")
            }
        }

        val displayContent = when {
            text.isNotEmpty() -> text
            imageUri != null -> "صورة مرفقة 📷"
            attachedFile != null -> "ملف مرفق 📄 (${attachedFile.name})"
            else -> ""
        }

        // Clear input field and attachments immediately
        _uiState.update {
            it.copy(
                inputText = "",
                attachedImageUri = null,
                attachedFile = null,
                isStreaming = true
            )
        }

        viewModelScope.launch {
            // Save user message to Room DB
            repository.saveUserMessage(
                sessionId = activeSession.id,
                content = displayContent,
                imageUri = imageUri,
                fileName = fileName
            )

            // Create initial empty bot message placeholder in Room DB
            val botMsgId = repository.createInitialBotMessage(activeSession.id)

            // Stream response
            val history = currentState.messages
            val systemPrompt = currentState.currentPersona.systemPrompt

            val accumulatedSb = StringBuilder()

            streamingJob?.cancel()
            streamingJob = viewModelScope.launch {
                try {
                    repository.streamAiResponse(systemPrompt, history, promptForAi.toString(), imageUri).collect { chunk ->
                        accumulatedSb.append(chunk)
                        repository.updateBotMessageContent(botMsgId, accumulatedSb.toString())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    repository.updateBotMessageContent(botMsgId, accumulatedSb.toString() + "\n[حدث خطأ في الاتصال]")
                } finally {
                    _uiState.update { it.copy(isStreaming = false) }
                }
            }
        }
    }
}
