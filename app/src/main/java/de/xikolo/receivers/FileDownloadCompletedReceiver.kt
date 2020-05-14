package de.xikolo.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.xikolo.download.filedownload.FileDownloadHandler

class FileDownloadCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id: Long = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
        if (id != 0L) {
            FileDownloadHandler.notifyOnDownloadComplete(id)
        }
    }
}
