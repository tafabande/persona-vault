package com.pims.vault.design

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.exitProcess
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises ART linkage without invoking the dashboard or reading vault records. */
@RunWith(AndroidJUnit4::class)
class DashboardBytecodeDeviceTest {
    @Test
    fun dashboardBytecodeLinksBeforeAuthentication() {
        val loader = InstrumentationRegistry.getInstrumentation().targetContext.classLoader
        DashboardBytecodeProbe.verify(loader)
    }
}

/** Can run through app_process with the app and test APKs on CLASSPATH. */
object DashboardBytecodeProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        val output = PrintStream(FileOutputStream(FileDescriptor.out))
        try {
            val methodCount = verify(DashboardBytecodeProbe::class.java.classLoader!!)
            output.println("Dashboard bytecode verified ($methodCount methods)")
            output.flush()
            exitProcess(0)
        } catch (failure: Throwable) {
            failure.printStackTrace(output)
            output.flush()
            exitProcess(1)
        }
    }

    internal fun verify(loader: ClassLoader): Int {
        val dashboard = Class.forName(
            "com.pims.vault.presentation.PersonaDashboardScreenKt",
            true,
            loader
        )

        val methods = dashboard.declaredMethods
        check(methods.any { it.name == "PersonaDashboardScreen" }) {
            "The dashboard entry point must remain available after ART verification"
        }
        listOf(
            "com.pims.vault.presentation.hub.MoreViewKt",
            "com.pims.vault.presentation.ui.theme.SakuraEffectsKt",
            "com.pims.vault.presentation.ui.theme.JujutsuKaisenThemePreviewKt"
        ).forEach { name -> Class.forName(name, true, loader).declaredMethods }
        return methods.size
    }
}
