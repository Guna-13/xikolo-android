package de.xikolo.testing.instrumented.unit

import de.xikolo.App
import de.xikolo.download.DownloadStatus
import de.xikolo.download.filedownload.FileDownloadIdentifier
import de.xikolo.download.filedownload.FileDownloadItem
import de.xikolo.extensions.observeOnce
import de.xikolo.states.DownloadStateLiveData
import de.xikolo.testing.instrumented.mocking.SampleMockData
import de.xikolo.utils.extensions.preferredStorage
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class FileDownloadItemTest : DownloadItemTest<FileDownloadItem,
    File, FileDownloadIdentifier>() {

    private val storage = context.preferredStorage

    override var testDownloadItem =
        FileDownloadItem(SampleMockData.mockVideoStreamSdUrl, "sdvideo.mp4", storage)
    override var testDownloadItemNotDownloadable = FileDownloadItem(null, "null")

    private var testSecondaryItem = FileDownloadItem(
        SampleMockData.mockVideoStreamThumbnailUrl,
        "thumb.jpg",
        storage
    )
    private var testDownloadItemWithSecondary =
        object : FileDownloadItem(
            SampleMockData.mockVideoStreamSdUrl,
            "sdvideo2.mp4",
            storage
        ) {
            override val secondaryDownloadItems: Set<FileDownloadItem> = setOf(testSecondaryItem)
        }
    private var testDownloadItemWithSecondaryNotDeletingSecondary =
        object : FileDownloadItem(
            SampleMockData.mockVideoStreamSdUrl,
            "sdvideo3.mp4",
            storage
        ) {
            override val secondaryDownloadItems: Set<FileDownloadItem> = setOf(testSecondaryItem)
            override val deleteSecondaryDownloadItemPredicate: (FileDownloadItem) -> Boolean =
                { false }
        }

    @Before
    @After
    fun cleanUp() {
        testDownloadItem.cancel(activityTestRule.activity)
        testDownloadItem.delete(activityTestRule.activity)

        testDownloadItemWithSecondary.cancel(activityTestRule.activity)
        testDownloadItemWithSecondary.delete(activityTestRule.activity)

        testSecondaryItem.cancel(activityTestRule.activity)
        testSecondaryItem.delete(activityTestRule.activity)
    }

    @Test
    fun testStartDownloadWithSecondary() {
        var calledMain = false
        activityTestRule.activity.runOnUiThread {
            App.instance.state.download.of(testDownloadItemWithSecondary.identifier)
                .observeOnce(activityTestRule.activity) {
                    calledMain = it == DownloadStateLiveData.DownloadStateCode.COMPLETED
                    calledMain
                }
        }
        var calledSecondary = false
        activityTestRule.activity.runOnUiThread {
            App.instance.state.download.of(testSecondaryItem.identifier)
                .observeOnce(activityTestRule.activity) {
                    calledSecondary = it == DownloadStateLiveData.DownloadStateCode.COMPLETED
                    calledSecondary
                }
        }

        activityTestRule.activity.runOnUiThread {
            assertTrue(testDownloadItemWithSecondary.start(activityTestRule.activity))
        }
        Thread.sleep(1000)

        assertNotEquals(
            DownloadStateLiveData.DownloadStateCode.DELETED,
            App.instance.state.download.of(testDownloadItemWithSecondary.identifier).value
        )
        assertNotEquals(
            DownloadStateLiveData.DownloadStateCode.DELETED,
            App.instance.state.download.of(testSecondaryItem.identifier).value
        )

        assertTrue(testDownloadItemWithSecondary.isDownloadRunning)
        assertTrue(testSecondaryItem.isDownloadRunning || testSecondaryItem.downloadExists)
        assertFalse(testDownloadItemWithSecondary.start(activityTestRule.activity))
        assertFalse(testSecondaryItem.start(activityTestRule.activity))

        assertNotNull(testDownloadItemWithSecondary.status)
        assertNotNull(testSecondaryItem.status)

        waitWhile(
            { testDownloadItemWithSecondary.status?.state != DownloadStatus.State.SUCCESSFUL },
            30000
        )
        Thread.sleep(3000)

        assertTrue(calledMain)
        assertTrue(calledSecondary)
        assertTrue(testDownloadItemWithSecondary.downloadExists)
        assertTrue(testSecondaryItem.downloadExists)
        assertNotNull(testDownloadItemWithSecondary.download)
        assertNotNull(testSecondaryItem.download)
        assertFalse(testDownloadItemWithSecondary.start(activityTestRule.activity))
        assertFalse(testSecondaryItem.start(activityTestRule.activity))
    }

    @Test
    fun testCancelDownloadWithSecondary() {
        activityTestRule.activity.runOnUiThread {
            assertTrue(testDownloadItemWithSecondary.start(activityTestRule.activity))
        }
        Thread.sleep(1000)

        assertTrue(testDownloadItemWithSecondary.isDownloadRunning)
        assertTrue(testSecondaryItem.isDownloadRunning || testSecondaryItem.downloadExists)
        assertTrue(testDownloadItemWithSecondary.cancel(activityTestRule.activity))
        Thread.sleep(1000)

        assertFalse(testDownloadItemWithSecondary.isDownloadRunning)
        assertFalse(testDownloadItemWithSecondary.downloadExists)
        assertNull(testDownloadItemWithSecondary.download)

        assertFalse(testSecondaryItem.isDownloadRunning)
        assertFalse(testSecondaryItem.downloadExists)
        assertNull(testSecondaryItem.download)
    }

    @Test
    fun testDeleteDownloadWithSecondary() {
        var calledMain = false
        activityTestRule.activity.runOnUiThread {
            App.instance.state.download.of(testDownloadItemWithSecondary.identifier)
                .observeOnce(activityTestRule.activity) {
                    calledMain = it == DownloadStateLiveData.DownloadStateCode.DELETED
                    calledMain
                }
        }
        var calledSecondary = false
        activityTestRule.activity.runOnUiThread {
            App.instance.state.download.of(testSecondaryItem.identifier)
                .observeOnce(activityTestRule.activity) {
                    calledSecondary = it == DownloadStateLiveData.DownloadStateCode.DELETED
                    calledSecondary
                }
        }

        assertFalse(testDownloadItemWithSecondary.delete(activityTestRule.activity))

        activityTestRule.activity.runOnUiThread {
            assertTrue(testDownloadItemWithSecondary.start(activityTestRule.activity))
        }

        waitWhile(
            { testDownloadItemWithSecondary.status?.state != DownloadStatus.State.SUCCESSFUL },
            30000
        )
        Thread.sleep(3000)

        assertTrue(testDownloadItemWithSecondary.downloadExists)

        assertTrue(testDownloadItemWithSecondary.delete(activityTestRule.activity))
        Thread.sleep(1000)

        assertTrue(calledMain)
        assertTrue(calledSecondary)
        assertFalse(testDownloadItemWithSecondary.downloadExists)
        assertFalse(testSecondaryItem.downloadExists)
        assertNull(testDownloadItemWithSecondary.download)
        assertNull(testSecondaryItem.download)
        assertEquals(
            DownloadStateLiveData.DownloadStateCode.DELETED,
            App.instance.state.download.of(testDownloadItemWithSecondary.identifier).value
        )
        assertEquals(
            DownloadStateLiveData.DownloadStateCode.DELETED,
            App.instance.state.download.of(testSecondaryItem.identifier).value
        )
    }

    @Test
    fun testDeleteDownloadWithoutSecondary() {
        activityTestRule.activity.runOnUiThread {
            assertTrue(
                testDownloadItemWithSecondaryNotDeletingSecondary.start(activityTestRule.activity)
            )
        }

        waitWhile(
            {
                testDownloadItemWithSecondaryNotDeletingSecondary.status?.state !=
                    DownloadStatus.State.SUCCESSFUL
            },
            30000
        )
        Thread.sleep(3000)

        assertTrue(
            testDownloadItemWithSecondaryNotDeletingSecondary.delete(activityTestRule.activity)
        )
        Thread.sleep(1000)

        assertFalse(testDownloadItemWithSecondaryNotDeletingSecondary.downloadExists)
        assertTrue(testSecondaryItem.downloadExists)
        assertNull(testDownloadItemWithSecondaryNotDeletingSecondary.download)
        assertNotNull(testSecondaryItem.download)
        assertEquals(
            DownloadStateLiveData.DownloadStateCode.DELETED,
            App.instance.state.download.of(
                testDownloadItemWithSecondaryNotDeletingSecondary.identifier
            ).value
        )
        assertNotEquals(
            DownloadStateLiveData.DownloadStateCode.DELETED,
            App.instance.state.download.of(testSecondaryItem.identifier).value
        )
    }
}
