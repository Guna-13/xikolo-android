package de.xikolo.testing.instrumented.unit

import androidx.test.rule.ActivityTestRule
import de.xikolo.controllers.main.MainActivity
import de.xikolo.download.DownloadHandler
import de.xikolo.download.DownloadIdentifier
import de.xikolo.download.DownloadRequest
import de.xikolo.download.DownloadStatus
import de.xikolo.download.filedownload.FileDownloadHandler
import de.xikolo.download.filedownload.FileDownloadIdentifier
import de.xikolo.download.filedownload.FileDownloadRequest
import de.xikolo.testing.instrumented.mocking.SampleMockData
import de.xikolo.testing.instrumented.mocking.base.BaseTest
import de.xikolo.utils.extensions.createIfNotExists
import de.xikolo.utils.extensions.preferredStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

abstract class DownloadHandlerTest<T : DownloadHandler<I, R>,
    I : DownloadIdentifier, R : DownloadRequest> : BaseTest() {

    class FileDownloadHandlerTest : DownloadHandlerTest<FileDownloadHandler,
        FileDownloadIdentifier, FileDownloadRequest>() {

        @Before
        fun setUp() {
            context.preferredStorage.file.deleteRecursively()
            context.preferredStorage.file.createIfNotExists()
        }

        override var downloadHandler = FileDownloadHandler(context)
        override var successfulTestRequest = FileDownloadRequest(
            SampleMockData.mockVideoStreamSdUrl,
            createTempFile(directory = context.preferredStorage.file),
            "TITLE",
            true
        )
        override var failingTestRequest = FileDownloadRequest(
            "https://i.vimeocdn.com/video/525223631.webpnotfound",
            createTempFile(directory = context.preferredStorage.file),
            "TITLE",
            true
        )
        override var invalidIdentifier = FileDownloadIdentifier("doesnotexist")
    }

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule(MainActivity::class.java, false, true)

    abstract var downloadHandler: T

    abstract var successfulTestRequest: R
    abstract var failingTestRequest: R
    abstract var invalidIdentifier: I

    @Test
    fun testDownloadIdentifier() {
        val identifier = downloadHandler.download(successfulTestRequest)
        assertTrue(identifier.toString().isNotBlank())
    }

    @Test
    fun testInvalidIdentifier() {
        assertNull(downloadHandler.status(invalidIdentifier))
        assertFalse(downloadHandler.cancel(invalidIdentifier))
    }

    @Test
    fun testDownloadStatusAfterStart() {
        val identifier = downloadHandler.download(successfulTestRequest)
        assertNotNull(downloadHandler.status(identifier))

        assertNotEquals(DownloadStatus.State.FAILED, downloadHandler.status(identifier)!!.state)
        assertNotEquals(DownloadStatus.State.SUCCESSFUL, downloadHandler.status(identifier)!!.state)
        assertTrue(
            downloadHandler.status(identifier)!!.totalBytes
                <= downloadHandler.status(identifier)!!.totalBytes
        )
    }

    @Test
    fun testDownloadStatusAfterCancel() {
        var calledIdentifier: I? = null
        downloadHandler.onCompletionListener = { identifier, status ->
            calledIdentifier = identifier
        }

        val identifier = downloadHandler.download(successfulTestRequest)
        assertNotNull(downloadHandler.status(identifier))

        assertTrue(downloadHandler.cancel(identifier))
        assertNull(downloadHandler.status(identifier))

        assertNotNull(calledIdentifier)
        assertEquals(calledIdentifier!!, identifier)
    }

    @Test
    fun testDownloadStatusAfterSuccess() {
        var calledIdentifier: I? = null
        downloadHandler.onCompletionListener = { identifier, status ->
            calledIdentifier = identifier

            assertNotNull(downloadHandler.status(identifier))
            assertEquals(DownloadStatus.State.SUCCESSFUL, status.state)
            assertTrue(
                downloadHandler.status(identifier)!!.downloadedBytes
                    == downloadHandler.status(identifier)!!.totalBytes
            )
        }

        val identifier = downloadHandler.download(successfulTestRequest)
        assertNotNull(downloadHandler.status(identifier))

        waitWhile(
            { downloadHandler.status(identifier)?.state != DownloadStatus.State.SUCCESSFUL },
            30000
        )
        Thread.sleep(1000)

        assertNotNull(calledIdentifier)
        assertEquals(calledIdentifier!!, identifier)
    }

    @Test
    fun testDownloadStatusAfterFailure() {
        var calledIdentifier: I? = null
        downloadHandler.onCompletionListener = { identifier, status ->
            calledIdentifier = identifier

            assertNotNull(downloadHandler.status(identifier))
            assertEquals(DownloadStatus.State.FAILED, status.state)
            assertFalse(
                downloadHandler.status(identifier)!!.downloadedBytes
                    == downloadHandler.status(identifier)!!.totalBytes
            )
        }

        val identifier = downloadHandler.download(failingTestRequest)
        assertNotNull(downloadHandler.status(identifier))

        waitWhile(
            { downloadHandler.status(identifier)?.state != DownloadStatus.State.FAILED },
            5000
        )
        Thread.sleep(1000)

        assertNotNull(calledIdentifier)
        assertEquals(calledIdentifier!!, identifier)
    }

    private fun waitWhile(condition: () -> Boolean, timeout: Long = Long.MAX_VALUE) {
        var waited = 0
        while (condition()) {
            Thread.sleep(100)
            waited += 100
            if (waited > timeout) {
                throw Exception()
            }
        }
    }
}
