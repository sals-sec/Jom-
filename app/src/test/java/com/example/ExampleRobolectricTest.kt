package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.calls.WebRtcCallEngine
import com.example.core.config.AppEnvironmentConfig
import com.example.network.MediaStorageService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun appName_matchesJomBranding() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Jom!", appName)
    }

    @Test
    fun mediaStorageService_validatesSupportedMultimediaFormats() = runBlocking {
        val mediaService = MediaStorageService()
        assertTrue(mediaService.validateExtension("photo.jpg"))
        assertTrue(mediaService.validateExtension("clip.mp4"))
        assertTrue(mediaService.validateExtension("report.pdf"))
        assertTrue(mediaService.validateExtension("archive.zip"))
        assertFalse(mediaService.validateExtension("malware.exe"))

        val progressEvents = mediaService.uploadMultimediaWithProgress(
            fileName = "architecture.pdf",
            mimeType = "application/pdf",
            fileSizeBytes = 500_000L
        ).toList()

        assertTrue(progressEvents.isNotEmpty())
        assertTrue(progressEvents.last().isCompleted)
        assertTrue(progressEvents.last().downloadUrl?.contains("architecture.pdf") == true)
    }

    @Test
    fun webRtcCallEngine_generatesValidSdpOfferAndAnswer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = WebRtcCallEngine(context)
        val offer = engine.createSdpOffer(isVideo = true)
        val answer = engine.createSdpAnswer(isVideo = true)

        assertTrue(offer.contains("m=audio"))
        assertTrue(offer.contains("m=video"))
        assertTrue(answer.contains("s=JomWebRTCAnswer"))
        assertTrue(AppEnvironmentConfig.stunServers.isNotEmpty())
    }
}
