package com.pims.vault.core.activity

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structured entity types that activity events can reference.
 */
enum class ActivityEntityType {
    PERSON,
    DOCUMENT,
    PROFILE,
    VAULT_ITEM,
    AVATAR,
    WALLPAPER,
    SOCIAL_ACCOUNT,
    MEDICAL,
    SYSTEM
}

/**
 * The type of change that occurred.
 */
enum class ActivityAction {
    ADDED,
    UPDATED,
    REMOVED,
    VIEWED,
    UNLOCKED
}

data class ActivityEvent(
    val id: String = UUID.randomUUID().toString(),
    val entityType: ActivityEntityType = ActivityEntityType.SYSTEM,
    val entityId: String? = null,
    val action: ActivityAction = ActivityAction.UPDATED,
    val title: String,
    val subtitle: String = "",
    val fieldChanged: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedDate: String
        get() {
            val now = System.currentTimeMillis()
            val diffDays = (now - timestamp) / (1000 * 60 * 60 * 24)
            return when (diffDays) {
                0L -> "Today"
                1L -> "Yesterday"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
        }

    /**
     * Relative timestamp for compact display: "10 min ago", "1 hr ago", "Yesterday"
     */
    val relativeTimestamp: String
        get() {
            val now = System.currentTimeMillis()
            val diffMs = now - timestamp
            val diffMin = diffMs / (1000 * 60)
            val diffHr = diffMs / (1000 * 60 * 60)
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            return when {
                diffMin < 1 -> "Just now"
                diffMin < 60 -> "${diffMin} min ago"
                diffHr < 24 -> "${diffHr} hr ago"
                diffDays == 1L -> "Yesterday"
                diffDays < 7 -> "${diffDays} days ago"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
        }
}

@Singleton
class RecentActivityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("pims_recent_activity_prefs", Context.MODE_PRIVATE)

    init {
        synchronized(lock) {
            if (sharedActivitiesFlow == null) {
                sharedActivitiesFlow = MutableStateFlow(loadActivities())
            }
        }
    }

    val activities: StateFlow<List<ActivityEvent>>
        get() = synchronized(lock) {
            sharedActivitiesFlow ?: MutableStateFlow(loadActivities()).also { sharedActivitiesFlow = it }
        }.asStateFlow()

    private fun loadActivities(): List<ActivityEvent> {
        val raw = prefs.getString(KEY_ACTIVITIES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ActivityEvent>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ActivityEvent(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        entityType = try {
                            ActivityEntityType.valueOf(obj.optString("entityType", "SYSTEM"))
                        } catch (_: Exception) { ActivityEntityType.SYSTEM },
                        entityId = obj.optString("entityId", null).takeIf { !it.isNullOrBlank() },
                        action = try {
                            ActivityAction.valueOf(obj.optString("action", "UPDATED"))
                        } catch (_: Exception) { ActivityAction.UPDATED },
                        title = obj.optString("title", ""),
                        subtitle = obj.optString("subtitle",
                            obj.optString("description", "")),  // backward compat
                        fieldChanged = obj.optString("fieldChanged", null).takeIf { !it.isNullOrBlank() },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveActivities(list: List<ActivityEvent>) {
        val arr = JSONArray()
        val sorted = list.sortedByDescending { it.timestamp }
        sorted.take(MAX_STORED_EVENTS).forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("entityType", item.entityType.name)
            obj.put("entityId", item.entityId ?: "")
            obj.put("action", item.action.name)
            obj.put("title", item.title)
            obj.put("subtitle", item.subtitle)
            obj.put("fieldChanged", item.fieldChanged ?: "")
            obj.put("timestamp", item.timestamp)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_ACTIVITIES, arr.toString()).apply()
        synchronized(lock) {
            sharedActivitiesFlow?.value = sorted
        }
    }

    /**
     * Records a meaningful state-changing action with structured metadata.
     *
     * Deduplication: If an event with the same entityType, entityId, action, and
     * fieldChanged already exists within the dedup window, the existing event is
     * replaced rather than creating a duplicate.
     */
    fun recordActivity(
        title: String,
        subtitle: String = "",
        entityType: ActivityEntityType = ActivityEntityType.SYSTEM,
        entityId: String? = null,
        action: ActivityAction = ActivityAction.UPDATED,
        fieldChanged: String? = null
    ) {
        val newEvent = ActivityEvent(
            title = title,
            subtitle = subtitle,
            entityType = entityType,
            entityId = entityId,
            action = action,
            fieldChanged = fieldChanged,
            timestamp = System.currentTimeMillis()
        )

        val current = activities.value.toMutableList()

        // Deduplication: replace existing event with same entity+action+field within window
        if (entityId != null) {
            val dedupWindowMs = DEDUP_WINDOW_MS
            val now = System.currentTimeMillis()
            val existingIndex = current.indexOfFirst { existing ->
                existing.entityType == entityType &&
                existing.entityId == entityId &&
                existing.action == action &&
                existing.fieldChanged == fieldChanged &&
                (now - existing.timestamp) < dedupWindowMs
            }
            if (existingIndex >= 0) {
                current.removeAt(existingIndex)
            }
        }

        current.add(0, newEvent)
        saveActivities(current)
    }

    /**
     * Legacy compatibility: records activity with just title and description.
     * Maps to the structured system using SYSTEM entity type.
     */
    fun recordActivity(title: String, description: String = "") {
        val (inferredType, inferredAction) = inferTypeAndAction(title)
        recordActivity(
            title = title,
            subtitle = description,
            entityType = inferredType,
            action = inferredAction
        )
    }

    /**
     * Infers entity type and action from the activity title text.
     * Used for backward compatibility with existing call sites.
     */
    private fun inferTypeAndAction(title: String): Pair<ActivityEntityType, ActivityAction> {
        val lower = title.lowercase()
        val entityType = when {
            lower.contains("person") || lower.contains("people") -> ActivityEntityType.PERSON
            lower.contains("document") || lower.contains("wallet") -> ActivityEntityType.DOCUMENT
            lower.contains("avatar") || lower.contains("hairstyle") -> ActivityEntityType.AVATAR
            lower.contains("wallpaper") || lower.contains("artwork") -> ActivityEntityType.WALLPAPER
            lower.contains("social") || lower.contains("profile") && lower.contains("added") -> ActivityEntityType.SOCIAL_ACCOUNT
            lower.contains("vault") || lower.contains("security") || lower.contains("password") || lower.contains("card") -> ActivityEntityType.VAULT_ITEM
            lower.contains("profile") || lower.contains("personal") || lower.contains("information") || lower.contains("phone") || lower.contains("email") || lower.contains("theme") -> ActivityEntityType.PROFILE
            else -> ActivityEntityType.SYSTEM
        }
        val action = when {
            lower.contains("added") || lower.contains("created") -> ActivityAction.ADDED
            lower.contains("removed") || lower.contains("deleted") || lower.contains("disconnected") -> ActivityAction.REMOVED
            lower.contains("unlocked") -> ActivityAction.UNLOCKED
            else -> ActivityAction.UPDATED
        }
        return entityType to action
    }

    fun clearAll() {
        prefs.edit().remove(KEY_ACTIVITIES).apply()
        synchronized(lock) {
            sharedActivitiesFlow?.value = emptyList()
        }
    }

    companion object {
        private const val KEY_ACTIVITIES = "recent_activities_v2"
        private const val MAX_STORED_EVENTS = 100
        private const val DEDUP_WINDOW_MS = 5 * 60 * 1000L  // 5 minutes
        private val lock = Any()
        @Volatile
        private var sharedActivitiesFlow: MutableStateFlow<List<ActivityEvent>>? = null
    }
}
