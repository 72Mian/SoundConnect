package com.example.firebase.presentation.chat

import androidx.lifecycle.ViewModel
import com.example.firebase.domain.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel(private val auth: FirebaseAuth) : ViewModel() {


    private val databaseRef = Firebase.database.reference.child("messages")

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    init {

        listenForMessages()
    }

    private fun listenForMessages() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatList = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) {

                        chatList.add(msg.copy(id = child.key ?: ""))
                    }
                }
                _messages.value = chatList.sortedBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val user = auth.currentUser ?: return

        val messageId = databaseRef.push().key ?: return

        val chatMessage = ChatMessage(
            id = messageId,
            senderId = user.uid,
            senderEmail = user.email ?: "Usuario Anónimo",
            message = text,
            timestamp = System.currentTimeMillis()
        )

        databaseRef.child(messageId).setValue(chatMessage)
    }

    fun deleteMessage(messageId: String) {
        databaseRef.child(messageId).removeValue()
    }
}