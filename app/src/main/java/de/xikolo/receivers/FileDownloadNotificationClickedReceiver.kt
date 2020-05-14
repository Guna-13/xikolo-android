package de.xikolo.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.xikolo.download.filedownload.FileDownloadHandler

class FileDownloadNotificationClickedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        FileDownloadHandler.notifyOnRunningNotificationClicked(
            (intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)
                ?: longArrayOf())
                .asList()
        )
    }
}
