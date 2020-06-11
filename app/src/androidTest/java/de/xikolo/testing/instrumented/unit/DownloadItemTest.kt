package de.xikolo.testing.instrumented.unit

import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import de.xikolo.App
import de.xikolo.controllers.main.MainActivity
import de.xikolo.download.DownloadIdentifier
import de.xikolo.download.DownloadItem
import de.xikolo.extensions.observeOnce
import de.xikolo.states.DownloadStateLiveData
import de.xikolo.testing.instrumented.mocking.base.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

abstract class DownloadItemTest<T : DownloadItem<F, I>,
    F, I : DownloadIdentifier> : BaseTest() {

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule(MainActivity::class.java, false, true)

    @Rule
    @JvmField
    var permissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    abstract var testDownloadItem: T
    abstract var testDownloadItemNotDownloadable: T

    @Test
    fun testIdentifier() {
        assertNotNull(testDownloadItem.identifier)
        try {
            testDownloadItemNotDownloadable.identifier
            throw Exception("Assertion failed: call should throw an Exception")
        } catch (e: Exception) {
        }
    }

    @Test
    fun testIsDownloadable() {
        assertTrue(testDownloadItem.isDownloadable)
        assertFalse(testDownloadItemNotDownloadable.isDownloadable)
    }

    @Test
    fun testStateBeforeDownload() {
        assertNull(testDownloadItem.download)
        assertFalse(testDownloadItem.downloadExists)
        assertFalse(testDownloadItem.isDownloadRunning)
    }

    @Test
    fun testStartDownload() {
        var called = false
        activityTestRule.activity.runOnUiThread {
            App.instance.state.download.of(testDownloadItem.identifier)
                .observeOnce(activityTestRule.activity) {
                    called = it == DownloadStateLiveData.DownloadStateCode.COMPLETED
                    called
                }
        }

        activityTestRule.activity.runOnUiThread {
            assertTrue(testDownloadItem.start(activityTestRule.activity))
        }
        assertFalse(testDownloadItemNotDownloadable.start(activityTestRule.activity))
        Thread.sleep(1000)

        assertEquals(
            DownloadStateLiveData.DownloadStateCode.STARTED,
            App.instance.state.download.of(testDownloadItem.identifier).value
        )

        assertTrue(testDownloadItem.isDownloadRunning)
        assertFalse(testDownloadItem.start(activityTestRule.activity))

        assertNotNull(testDownloadItem.status)

        waitWhile({
            App.instance.state.download.of(testDownloadItem.identifier).value !=
                DownloadStateLiveData.DownloadStateCode.COMPLETED
        }, 30000)

        assertTrue(called)
        assertTrue(testDownloadItem.downloadExists)
        assertNotNull(testDownloadItem.download)
        assertFalse(testDownloadItem.start(activityTestRule.activity))
    }

    @Test
    fun testCancelDownload() {
        activityTestRule.activity.runOnUiThread {
            assertTrue(testDownloadItem.start(activityTestRule.activity))
        }
        Thread.sleep(1000)

        assertTrue(testDownloadItem.isDownloadRunning)
        assertTrue(testDownloadItem.cancel(activityTestRule.activity))
        Thread.sleep(1000)

        assertFalse(testDownloadItem.isDownloadRunning)
        assertFalse(testDownloadItem.downloadExists)
        assertNull(testDownloadItem.download)
    }

    @Test
    fun testDeleteDownload() {
        var called = false
        activityTestRule.activity.runOnUiThread {
            App.instance.state.download.of(testDownloadItem.identifier)
                .observeOnce(activityTestRule.activity) {
                    called = it == DownloadStateLiveData.DownloadStateCode.DELETED
                    called
                }
        }

        assertFalse(testDownloadItem.delete(activityTestRule.activity))

        activityTestRule.activity.runOnUiThread {
            assertTrue(testDownloadItem.start(activityTestRule.activity))
        }

        waitWhile({
            App.instance.state.download.of(testDownloadItem.identifier).value !=
                DownloadStateLiveData.DownloadStateCode.COMPLETED
        }, 30000)

        assertTrue(testDownloadItem.downloadExists)

        assertTrue(testDownloadItem.delete(activityTestRule.activity))
        Thread.sleep(1000)

        assertTrue(called)
        assertFalse(testDownloadItem.downloadExists)
        assertNull(testDownloadItem.download)
        assertEquals(
            DownloadStateLiveData.DownloadStateCode.DELETED,
            App.instance.state.download.of(testDownloadItem.identifier).value
        )
    }

    protected fun waitWhile(condition: () -> Boolean, timeout: Long = Long.MAX_VALUE) {
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
