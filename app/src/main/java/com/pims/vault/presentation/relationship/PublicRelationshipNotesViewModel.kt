package com.pims.vault.presentation.relationship

import androidx.lifecycle.ViewModel
import com.pims.vault.data.local.dao.RelationshipNoteDao
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
class PublicRelationshipNotesViewModel @Inject constructor(
    private val relationshipNoteDao: RelationshipNoteDao
) : ViewModel() {
    fun observe(ownerId: String?): Flow<List<RelationshipNoteEntity>> =
        ownerId?.let(relationshipNoteDao::getPublicNotesForOwnerFlow) ?: flowOf(emptyList())
}
