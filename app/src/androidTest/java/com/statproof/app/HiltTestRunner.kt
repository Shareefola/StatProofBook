package com.statproof.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner that replaces the application class with
 * [HiltTestApplication] for Hilt-powered instrumented tests.
 *
 * Registered in app/build.gradle.kts via:
 *   testInstrumentationRunner = "com.statproof.app.HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
