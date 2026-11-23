package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.SpeakCraftRepository
import com.example.data.model.LearningChallenge
import com.example.data.model.Lesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Slim Admin catalog ViewModel retained until T-04.1.4 removes the Admin console.
 * All in-app updater, release publishing, backup snapshot, and update telemetry logic
 * was removed per ADR-004 (T-04.1.3).
 */
class UpdateAndAdminViewModel(
    private val repository: SpeakCraftRepository
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun saveSharedLesson(lesson: Lesson) {
        viewModelScope.launch {
            val res = repository.adminSaveSharedLesson(lesson)
            _statusMessage.value = if (res.isSuccess) "Shared lesson '${lesson.title}' saved." else res.exceptionOrNull()?.message
        }
    }

    fun saveSharedChallenge(challenge: LearningChallenge) {
        viewModelScope.launch {
            val res = repository.adminSaveSharedChallenge(challenge)
            _statusMessage.value = if (res.isSuccess) "Challenge '${challenge.title}' saved." else res.exceptionOrNull()?.message
        }
    }
}
