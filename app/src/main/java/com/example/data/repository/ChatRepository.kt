package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.model.Persona
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChatRepository(
    private val chatDao: ChatDao,
    private val geminiApiClient: GeminiApiClient
) {
    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun getSessionById(sessionId: Long): ChatSessionEntity? {
        return chatDao.getSessionById(sessionId)
    }

    suspend fun createNewSession(persona: Persona = Persona.DEFAULT_PERSONA, customTitle: String? = null): Long {
        val title = customTitle ?: "محادثة جديدة - ${persona.nameAr}"
        val session = ChatSessionEntity(
            title = title,
            personaId = persona.id,
            personaName = persona.nameAr,
            systemPrompt = persona.systemPrompt
        )
        return chatDao.insertSession(session)
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        val existing = chatDao.getSessionById(sessionId) ?: return
        chatDao.updateSession(existing.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateSessionPersona(sessionId: Long, persona: Persona) {
        val existing = chatDao.getSessionById(sessionId) ?: return
        chatDao.updateSession(
            existing.copy(
                personaId = persona.id,
                personaName = persona.nameAr,
                systemPrompt = persona.systemPrompt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSessionById(sessionId)
    }

    suspend fun saveUserMessage(sessionId: Long, content: String, imageUri: String? = null, fileName: String? = null): Long {
        val message = ChatMessageEntity(
            sessionId = sessionId,
            sender = "user",
            content = content,
            imageUri = imageUri,
            fileName = fileName,
            timestamp = System.currentTimeMillis()
        )
        val msgId = chatDao.insertMessage(message)

        // Auto update session title if it's the first message
        val session = chatDao.getSessionById(sessionId)
        if (session != null && (session.title.startsWith("محادثة جديدة") || session.title.startsWith("New Chat"))) {
            val autoTitle = if (content.length > 28) content.take(28) + "..." else content
            chatDao.updateSession(session.copy(title = autoTitle, updatedAt = System.currentTimeMillis()))
        }
        return msgId
    }

    suspend fun createInitialBotMessage(sessionId: Long): Long {
        val botMessage = ChatMessageEntity(
            sessionId = sessionId,
            sender = "bot",
            content = "",
            timestamp = System.currentTimeMillis()
        )
        return chatDao.insertMessage(botMessage)
    }

    suspend fun updateBotMessageContent(messageId: Long, newContent: String) {
        chatDao.updateMessageContent(messageId, newContent)
    }

    fun streamAiResponse(
        systemPrompt: String,
        history: List<ChatMessageEntity>,
        userMessage: String,
        imageUri: String? = null
    ): Flow<String> {
        return geminiApiClient.streamResponse(systemPrompt, history, userMessage, imageUri)
    }
}
