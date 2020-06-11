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
import de.xikolo.utils.extensions.preferredStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

abstract class DownloadHandlerTest<T : DownloadHandler<I, R>,
    I : DownloadIdentifier, R : DownloadRequest> : BaseTest() {

    class FileDownloadHandlerTest : DownloadHandlerTest<FileDownloadHandler,
        FileDownloadIdentifier, FileDownloadRequest>() {

        override var downloadHandler = FileDownloadHandler(context)
        override var successfulTestRequest = FileDownloadRequest(
            SampleMockData.mockVideoStreamSdUrl,
            createTempFile(directory = context.preferredStorage.file),
            "TITLE",
            true
        )
        override var successfulTestRequest2 = FileDownloadRequest(
            SampleMockData.mockVideoStreamThumbnailUrl,
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
    abstract var successfulTestRequest2: R
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

        downloadHandler.cancel(identifier)
    }

    @Test
    fun testDownloadStatusAfterCancel() {
        val identifier = downloadHandler.download(successfulTestRequest)
        assertNotNull(downloadHandler.status(identifier))

        assertTrue(downloadHandler.cancel(identifier))
        Thread.sleep(1000)

        assertNotEquals(DownloadStatus.State.SUCCESSFUL, downloadHandler.status(identifier)!!.state)
        assertNotEquals(DownloadStatus.State.FAILED, downloadHandler.status(identifier)!!.state)
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
        Thread.sleep(5000)

        assertNotNull(calledIdentifier)
        assertEquals(calledIdentifier!!, identifier)
    }

    @Test
    fun testParallelDownloading() {
        val identifier = downloadHandler.download(successfulTestRequest)
        val identifier2 = downloadHandler.download(successfulTestRequest2)
        assertNotNull(downloadHandler.status(identifier))
        assertNotNull(downloadHandler.status(identifier2))

        assertNotEquals(
            DownloadStatus.State.FAILED,
            downloadHandler.status(identifier)!!.state
        )
        assertNotEquals(
            DownloadStatus.State.SUCCESSFUL,
            downloadHandler.status(identifier)!!.state
        )
        assertNotEquals(
            DownloadStatus.State.FAILED,
            downloadHandler.status(identifier2)!!.state
        )
        assertNotEquals(
            DownloadStatus.State.SUCCESSFUL,
            downloadHandler.status(identifier2)!!.state
        )

        downloadHandler.cancel(identifier)
        downloadHandler.cancel(identifier2)
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
