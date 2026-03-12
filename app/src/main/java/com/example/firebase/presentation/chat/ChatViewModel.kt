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

    // Referencia al nodo "messages" en la Realtime Database
    private val databaseRef = Firebase.database.reference.child("messages")

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // Obtenemos el ID del usuario actual para saber qué mensajes son suyos
    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    init {
        // Al inicializar el ViewModel, empezamos a escuchar los mensajes
        listenForMessages()
    }

    private fun listenForMessages() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatList = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    // Firebase mapea el JSON automáticamente a nuestra Data Class
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) {
                        // Guardamos la clave generada por Firebase como ID
                        chatList.add(msg.copy(id = child.key ?: ""))
                    }
                }
                // Ordenamos por timestamp para que los más antiguos salgan arriba
                _messages.value = chatList.sortedBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                // Aquí manejaríamos el error (ej. log)
            }
        })
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val user = auth.currentUser ?: return

        // Generamos un nuevo ID único para el mensaje
        val messageId = databaseRef.push().key ?: return

        val chatMessage = ChatMessage(
            id = messageId,
            senderId = user.uid,
            senderEmail = user.email ?: "Usuario Anónimo",
            message = text,
            timestamp = System.currentTimeMillis()
        )

        // Guardamos el mensaje en la base de datos
        databaseRef.child(messageId).setValue(chatMessage)
    }

    // Requisito: Eliminar un mensaje propio
    fun deleteMessage(messageId: String) {
        databaseRef.child(messageId).removeValue()
    }
}