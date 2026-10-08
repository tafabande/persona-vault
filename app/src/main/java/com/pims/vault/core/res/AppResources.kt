package com.pims.vault.core.res

import android.content.Context

/**
 * Resilient resource identifier provider for Persona Vault.
 * Directly maps resource identifiers to prevent compiler failures from classpath path splitting on Windows.
 */
object AppResources {
    object Font {
        const val plus_jakarta_sans: Int = 2131296257
        const val inter: Int = 2131296256
    }

    object Drawable {
        const val ic_notification_reminder: Int = 2131230852
        const val ic_default_avatar_female: Int = 2131230844
        const val ic_default_avatar_male: Int = 2131230845
        const val ic_bank: Int = 2131230837
        const val ic_google_logo: Int = 2131230846

        fun getAvatarFemale(context: Context? = null): Int {
            return context?.resources?.getIdentifier("ic_default_avatar_female", "drawable", context.packageName)?.takeIf { it != 0 } ?: ic_default_avatar_female
        }

        fun getAvatarMale(context: Context? = null): Int {
            return context?.resources?.getIdentifier("ic_default_avatar_male", "drawable", context.packageName)?.takeIf { it != 0 } ?: ic_default_avatar_male
        }

        fun getNotificationIcon(context: Context? = null): Int {
            return context?.resources?.getIdentifier("ic_notification_reminder", "drawable", context.packageName)?.takeIf { it != 0 } ?: ic_notification_reminder
        }
    }
}
