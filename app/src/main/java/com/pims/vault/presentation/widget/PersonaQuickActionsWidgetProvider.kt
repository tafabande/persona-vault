package com.pims.vault.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.pims.vault.R
import com.pims.vault.presentation.MainActivity

/**
 * Persona Quick Actions Native Home-Screen Widget Provider.
 *
 * Provides 4 immediate actions:
 * 1. Documents (R.id.widget_action_documents) -> Opens document creation/import
 * 2. People (R.id.widget_action_people)       -> Opens people & kinship directory
 * 3. Vault (R.id.widget_action_vault)         -> Gated biometric unlock & vault fortress
 * 4. Notes (R.id.widget_action_notes)         -> Opens instant encrypted note editor
 *
 * Characteristics:
 * - Responsive sizing: 4-in-a-row layout when wide, 2x2 grid layout when narrow.
 * - Dynamic color & theme integration: Uses system neutral/accent tokens on Android 12+,
 *   and Persona theme tokens on Android 9-11.
 * - Privacy-first: Never displays secrets, titles, or decrypted personal info on the widget surface.
 * - Hardware auth preserved: Vault action strictly enforces biometric/PIN authentication.
 */
class PersonaQuickActionsWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_DOCUMENTS = "DOCUMENTS"
        const val ACTION_PEOPLE = "PEOPLE"
        const val ACTION_VAULT = "VAULT"
        const val ACTION_NOTES = "NOTES"

        private const val REQUEST_CODE_DOCUMENTS = 201
        private const val REQUEST_CODE_PEOPLE = 202
        private const val REQUEST_CODE_VAULT = 203
        private const val REQUEST_CODE_NOTES = 204

        /**
         * Requests an immediate widget refresh across all installed instances
         * (e.g. after theme changes or profile updates).
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, PersonaQuickActionsWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName) ?: return
            if (ids.isNotEmpty()) {
                val intent = Intent(context, PersonaQuickActionsWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }

        fun createActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                this.action = Intent.ACTION_VIEW
                data = Uri.parse("persona://widget/action/${action.lowercase()}")
                putExtra(MainActivity.EXTRA_QUICK_ACTION, action)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun buildRemoteViews(context: Context, widthDp: Int, heightDp: Int): RemoteViews {
            // Determine layout based on aspect ratio and width
            val isNarrow = widthDp in 1..220 || (widthDp < 260 && heightDp >= 110)
            val layoutId = if (isNarrow) {
                R.layout.widget_quick_actions_grid
            } else {
                R.layout.widget_quick_actions_row
            }

            return createRemoteViewsForLayout(context, layoutId)
        }

        private fun createRemoteViewsForLayout(context: Context, layoutId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, layoutId)

            // Wire Documents Action
            val pendingDocs = createActionPendingIntent(context, ACTION_DOCUMENTS, REQUEST_CODE_DOCUMENTS)
            views.setOnClickPendingIntent(R.id.widget_action_documents, pendingDocs)

            // Wire People Action
            val pendingPeople = createActionPendingIntent(context, ACTION_PEOPLE, REQUEST_CODE_PEOPLE)
            views.setOnClickPendingIntent(R.id.widget_action_people, pendingPeople)

            // Wire Vault Action
            val pendingVault = createActionPendingIntent(context, ACTION_VAULT, REQUEST_CODE_VAULT)
            views.setOnClickPendingIntent(R.id.widget_action_vault, pendingVault)

            // Wire Notes Action
            val pendingNotes = createActionPendingIntent(context, ACTION_NOTES, REQUEST_CODE_NOTES)
            views.setOnClickPendingIntent(R.id.widget_action_notes, pendingNotes)

            return views
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle? = null
    ) {
        val widgetOptions = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200)
        val minHeight = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)

        val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ Responsive Layout Mapping: Launcher picks seamlessly based on current cell size
            val rowViews = createRemoteViewsForLayout(context, R.layout.widget_quick_actions_row)
            val gridViews = createRemoteViewsForLayout(context, R.layout.widget_quick_actions_grid)
            RemoteViews(
                mapOf(
                    SizeF(120f, 100f) to gridViews,
                    SizeF(220f, 50f) to rowViews
                )
            )
        } else {
            buildRemoteViews(context, minWidth, minHeight)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
