package com.pims.vault.design

import com.pims.vault.presentation.ui.components.ux.*
import org.junit.Assert.*
import org.junit.Test

class PersonaToastControllerTest {
    @Test fun feedbackDoesNotReportBackupPendingOrFailureAsSavedSuccess() {
        assertEquals(ToastType.WARNING, personaFeedbackType("Saved locally; cloud backup pending"))
        assertEquals(ToastType.ERROR, personaFeedbackType("Save failed. The previous copy is preserved."))
        assertEquals(ToastType.SUCCESS, personaFeedbackType("Document saved"))
    }
    @Test fun dismissalCompletesTheSourceMessageOnce() {
        val controller = PersonaToastController()
        var dismissed = 0
        controller.show(PersonaToastMessage(message = "Saved", onDismiss = { dismissed++ }))
        assertEquals(0, dismissed)
        controller.dismiss()
        controller.dismiss()
        assertEquals(1, dismissed)
    }

    @Test fun replacingAMessageCompletesOldActionLifecycleAndKeepsNewMessage() {
        val controller = PersonaToastController()
        var dismissed = 0
        controller.show(PersonaToastMessage(message = "Old", onDismiss = { dismissed++ }))
        val next = PersonaToastMessage(message = "New")
        controller.show(next)
        assertEquals(1, dismissed)
        assertSame(next, controller.currentToast)
    }
}
