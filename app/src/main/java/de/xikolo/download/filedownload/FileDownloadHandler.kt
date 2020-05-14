package de.xikolo.download.filedownload

import android.app.DownloadManager
import android.content.Context
import androidx.core.content.ContextCompat
import de.xikolo.App
import de.xikolo.download.DownloadHandler
import de.xikolo.download.DownloadStatus
import de.xikolo.utils.NotificationUtil

open class FileDownloadHandler(val context: Context) :
    DownloadHandler<FileDownloadIdentifier, FileDownloadRequest> {

    companion object {

        internal var manualCancelCount = 0

        internal val instances: MutableMap<Long, FileDownloadHandler> = mutableMapOf()

        internal fun notifyOnRunningNotificationClicked(ids: List<Long>) {
            ids
                .groupBy { instances[it] }
                .forEach {
                    it.key?.onRunningNotificationClicked(it.value)
                }
        }

        internal fun notifyOnDownloadComplete(id: Long) {
            instances[id]?.onDownloadComplete(id)
        }

        val isDownloadingAnything: Boolean
            get() {
                return GlobalInstance.manager.query(
                    DownloadManager.Query()
                        .setFilterByStatus(DownloadManager.STATUS_RUNNING)
                        .setFilterByStatus(DownloadManager.STATUS_PAUSED)
                        .setFilterByStatus(DownloadManager.STATUS_PENDING)
                )?.moveToFirst() == true
            }

        val GlobalInstance by lazy {
            FileDownloadHandler(App.instance)
        }
    }

    protected val manager: DownloadManager by lazy {
        ContextCompat.getSystemService(
            context,
            DownloadManager::class.java
        ) as DownloadManager
    }

    override fun download(request: FileDownloadRequest): FileDownloadIdentifier {
        val identifier = FileDownloadIdentifier(request.url)
        val id = manager.enqueue(request.buildDownloadManagerRequest())
        instances[id] = this

        return identifier
    }

    override fun cancel(identifier: FileDownloadIdentifier): Boolean {
        return getInternalIdentifier(identifier)?.let {
            cancelInternal(it)
        } ?: false
    }

    override fun status(identifier: FileDownloadIdentifier): DownloadStatus? {
        return getInternalIdentifier(identifier)?.let {
            getStatusInternal(it)
        }
    }

    override var onCompletionListener: ((FileDownloadIdentifier, DownloadStatus) -> Unit)? = null

    override var onDownloadsCancelledListener: (() -> Unit)? = null

    override var onRunningNotificationClickListener: ((List<FileDownloadIdentifier>) -> Unit)? =
        null

    override var onCompletedNotificationClickListener: ((FileDownloadIdentifier) -> Unit)? = null

    private fun getInternalIdentifier(identifier: FileDownloadIdentifier): Long? {
        val cursor = manager.query(
            DownloadManager.Query()
        )

        while (cursor?.moveToNext() == true) {
            if (cursor.getString(
                    cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                ) == identifier.url
            ) {
                return cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
            }
        }
        return null
    }

    private fun cancelInternal(identifier: Long): Boolean {
        manualCancelCount++
        return if (manager.remove(identifier) != 1) {
            manualCancelCount--
            false
        } else true
    }

    private fun getStatusInternal(identifier: Long): DownloadStatus? {
        val download = manager.query(
            DownloadManager.Query().setFilterById(identifier)
        )
        return if (download?.moveToFirst() == true) {
            val size =
                download.getLong(
                    download.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                )
            val current =
                download.getLong(
                    download.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                )
            val state =
                when (download.getInt(
                    download.getColumnIndex(DownloadManager.COLUMN_STATUS)
                )) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.State.SUCCESSFUL
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PAUSED -> DownloadStatus.State.RUNNING
                    DownloadManager.STATUS_PENDING -> DownloadStatus.State.PENDING
                    DownloadManager.STATUS_FAILED -> DownloadStatus.State.FAILED
                    else -> DownloadStatus.State.FAILED
                }

            DownloadStatus(
                size,
                current,
                state
            )
        } else null
    }

    internal fun onRunningNotificationClicked(ids: List<Long>) {
        onRunningNotificationClickListener?.invoke(
            ids.mapNotNull {
                val cursor = manager.query(DownloadManager.Query().setFilterById(it))
                if (cursor?.moveToFirst() == true) {
                    FileDownloadIdentifier(
                        cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))
                    )
                }
                null
            }
        )
    }

    internal fun onDownloadComplete(id: Long) {
        val status = getStatusInternal(id)
        if (status == null) {
            if (manualCancelCount > 0) {
                manualCancelCount--
            } else {
                onDownloadsCancelledListener?.invoke()
            }
            return
        }

        val cursor = manager.query(
            DownloadManager.Query().setFilterById(id)
        )
        if (cursor?.moveToFirst() == true) {
            val uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))
            val title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
            val identifier = FileDownloadIdentifier(uri)

            if (status.state == DownloadStatus.State.SUCCESSFUL) {
                NotificationUtil(App.instance).showDownloadCompletedNotification(title)
            }

            onCompletionListener?.invoke(identifier, status)
        }
    }
}
