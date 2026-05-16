package com.statproof.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation
 * and create the application-level dependency container.
 *
 * This class intentionally has no additional logic — all initialization
 * is handled by Hilt modules in the di package.
 */
@HiltAndroidApp
class StatProofApplication : Application()
