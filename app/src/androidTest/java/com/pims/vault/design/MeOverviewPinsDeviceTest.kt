package com.pims.vault.design

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.presentation.hub.MeOverviewPins
import com.pims.vault.presentation.hub.OverviewPin
import com.pims.vault.presentation.hub.OverviewSource
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeOverviewPinsDeviceTest {
    @Test
    fun retainsOrderAndSeparatesAccountLayouts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val localScope = "test-local/${UUID.randomUUID()}"
        val cloudScope = "test-cloud/${UUID.randomUUID()}"
        val note = OverviewPin(OverviewSource.Note, "same-id")
        val id = OverviewPin(OverviewSource.Vault, "same-id")
        val approved = OverviewPin(OverviewSource.Profile, "sample-private-field", showValue = true)
        val store = MeOverviewPins(context)

        try {
            store.write(localScope, listOf(note, id, note))
            store.write(cloudScope, listOf(id, approved))
            val reopened = MeOverviewPins(context)
            assertEquals(listOf(note, id), reopened.read(localScope))
            assertEquals(listOf(id, approved), reopened.read(cloudScope))

            reopened.write(localScope, listOf(id, note))
            assertEquals(listOf(id, note), store.read(localScope))
            reopened.write(localScope, emptyList())
            assertTrue(store.read(localScope).isEmpty())
            assertEquals(listOf(id, approved), store.read(cloudScope))
        } finally {
            store.write(localScope, emptyList())
            store.write(cloudScope, emptyList())
        }
    }
}
