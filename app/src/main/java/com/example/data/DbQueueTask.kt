package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tts_queue_tasks")
data class DbQueueTask(
    @PrimaryKey val id: String,
    val text: String,
    val profileId: Int,
    val profileName: String,
    val status: String, // WAITING, PROCESSING, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis()
)
