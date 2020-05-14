package de.xikolo.download

import android.content.Intent
import androidx.fragment.app.FragmentActivity

abstract class DownloadItem<out F, out I : DownloadIdentifier> {

    abstract val identifier: I

    abstract val isDownloadable: Boolean

    abstract val downloadSize: Long

    abstract val title: String

    abstract val download: F?

    abstract val status: DownloadStatus?

    abstract val openIntent: Intent?

    abstract fun start(activity: FragmentActivity): Boolean

    abstract fun cancel(activity: FragmentActivity): Boolean

    abstract fun delete(activity: FragmentActivity): Boolean

    val isDownloadRunning: Boolean
        get() {
            return status?.let {
                it.state == DownloadStatus.State.RUNNING || it.state == DownloadStatus.State.PENDING
            } ?: false
        }

    val downloadExists: Boolean
        get() {
            return download != null
        }
}
