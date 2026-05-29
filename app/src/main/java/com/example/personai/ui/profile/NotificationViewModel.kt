package com.example.personai.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.domain.model.CommentWithTitle
import com.example.personai.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: PersonaRepository
) : ViewModel() {

    val sentComments: StateFlow<List<CommentWithTitle>> = repository.getMySentComments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val receivedReplies: StateFlow<List<CommentWithTitle>> = repository.getRepliesToMe()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}