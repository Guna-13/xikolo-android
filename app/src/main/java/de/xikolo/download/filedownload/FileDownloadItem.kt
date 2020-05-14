package de.xikolo.download.filedownload

import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import de.xikolo.App
import de.xikolo.controllers.downloads.DownloadsActivity
import de.xikolo.download.DownloadItem
import de.xikolo.download.DownloadStatus
import de.xikolo.extensions.observeOnce
import de.xikolo.managers.PermissionManager
import de.xikolo.models.DownloadAsset
import de.xikolo.models.Storage
import de.xikolo.states.PermissionStateLiveData
import de.xikolo.utils.FileProviderUtil
import de.xikolo.utils.LanalyticsUtil
import de.xikolo.utils.extensions.buildWriteErrorMessage
import de.xikolo.utils.extensions.createIfNotExists
import de.xikolo.utils.extensions.internalStorage
import de.xikolo.utils.extensions.preferredStorage
import de.xikolo.utils.extensions.sdcardStorage
import de.xikolo.utils.extensions.showToast
import java.io.File

open class FileDownloadItem(
    val url: String?,
    open val fileName: String,
    var storage: Storage = App.instance.preferredStorage
) : DownloadItem<File, FileDownloadIdentifier>() {

    companion object {
        val TAG: String? = DownloadAsset::class.simpleName
    }

    protected open fun getFileFolder(): String {
        return storage.file.absolutePath
    }

    private val filePath: String
        get() = getFileFolder() + File.separator + fileName

    protected open val size: Long = 0L

    protected open val mimeType = "application/pdf"

    protected open val showNotification = true

    protected open val secondaryDownloadItems = setOf<FileDownloadItem>()

    protected open val deleteSecondaryDownloadItemPredicate: (FileDownloadItem) -> Boolean =
        { _ -> true }

    private var downloader: FileDownloadHandler? = null

    override val identifier: FileDownloadIdentifier
        get() = FileDownloadIdentifier(url!!)

    override val isDownloadable: Boolean
        get() = url != null

    override val title: String
        get() = fileName

    override val downloadSize: Long
        get() {
            var total = size
            secondaryDownloadItems.forEach { total += it.downloadSize }
            return total
        }

    override val download: File?
        get() {
            val originalStorage: Storage = storage

            storage = App.instance.internalStorage
            val internalFile = File(filePath)
            if (internalFile.exists() && internalFile.isFile) {
                storage = originalStorage
                return internalFile
            }

            val sdcardStorage: Storage? = App.instance.sdcardStorage
            if (sdcardStorage != null) {
                storage = sdcardStorage
                val sdcardFile = File(filePath)
                if (sdcardFile.exists() && sdcardFile.isFile) {
                    storage = originalStorage
                    return sdcardFile
                }
            }

            storage = originalStorage
            return null
        }

    override val openIntent: Intent?
        get() {
            return download?.let {
                val target = Intent(Intent.ACTION_VIEW)
                target.setDataAndType(FileProviderUtil.getUriForFile(it), mimeType)
                target.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                Intent.createChooser(target, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

    private fun start(activity: FragmentActivity, downloader: FileDownloadHandler): Boolean {
        return performAction(activity) {
            when {
                isDownloadRunning || downloadExists -> false
                isDownloadable -> {
                    File(
                        filePath.substring(
                            0,
                            filePath.lastIndexOf(File.separator)
                        )
                    ).createIfNotExists()

                    downloader.download(
                        FileDownloadRequest(
                            url!!,
                            File(filePath),
                            title,
                            showNotification
                        )
                    )

                    if (this is DownloadAsset.Course.Item) {
                        LanalyticsUtil.trackDownloadedFile(this)
                    }

                    App.instance.state.download.of(identifier).started()

                    secondaryDownloadItems.forEach {
                        it.start(activity, downloader)
                    }

                    true
                }
                else -> false
            }
        }
    }

    override fun start(activity: FragmentActivity): Boolean {
        return start(activity, requireDownloader(activity))
    }

    private fun cancel(activity: FragmentActivity, downloader: FileDownloadHandler): Boolean {
        return performAction(activity) {
            downloader.cancel(identifier)

            delete(activity)

            secondaryDownloadItems.forEach {
                it.cancel(activity, downloader)
            }
            true
        }
    }

    override fun cancel(activity: FragmentActivity): Boolean {
        return cancel(activity, requireDownloader(activity))
    }

    override fun delete(activity: FragmentActivity): Boolean {
        return performAction(activity) {
            if (!downloadExists) {
                File(filePath).parentFile?.let {
                    Storage(it).clean()
                }
                false
            } else {
                App.instance.state.download.of(identifier).deleted()

                if (download?.delete() == true) {
                    secondaryDownloadItems.forEach {
                        if (deleteSecondaryDownloadItemPredicate(it)) {
                            it.delete(activity)
                        }
                    }
                }
                true
            }
        }
    }

    private fun getStatus(downloader: FileDownloadHandler): DownloadStatus? {
        return url?.let {
            var totalBytes = 0L
            var writtenBytes = 0L
            var state = DownloadStatus.State.SUCCESSFUL

            val mainDownload = downloader.status(identifier)
            if (mainDownload != null && mainDownload.totalBytes > 0L) {
                totalBytes += mainDownload.totalBytes
                writtenBytes += mainDownload.downloadedBytes
                state = state.and(mainDownload.state)
            } else {
                totalBytes += size
            }

            secondaryDownloadItems.forEach {
                it.getStatus(downloader)?.let { status ->
                    totalBytes += status.totalBytes
                    writtenBytes += status.downloadedBytes
                    state = state.and(status.state)
                }
            }
            DownloadStatus(
                totalBytes,
                writtenBytes,
                state
            )
        }
    }

    override val status: DownloadStatus?
        get() = getStatus(FileDownloadHandler.GlobalInstance)

    private fun performAction(activity: FragmentActivity, action: () -> Boolean): Boolean {
        return if (storage.isWritable) {
            if (PermissionManager(activity)
                    .requestPermission(PermissionManager.WRITE_EXTERNAL_STORAGE) == 1
            ) {
                action()
            } else {
                App.instance.state.permission.of(
                    PermissionManager.REQUEST_CODE_WRITE_EXTERNAL_STORAGE
                )
                    .observeOnce(activity) { state ->
                        return@observeOnce if (
                            state == PermissionStateLiveData.PermissionStateCode.GRANTED
                        ) {
                            performAction(activity, action)
                            true
                        } else false
                    }
                false
            }
        } else {
            val msg = App.instance.buildWriteErrorMessage()
            Log.w(TAG, msg)
            activity.showToast(msg)
            false
        }
    }

    private fun requireDownloader(activity: FragmentActivity): FileDownloadHandler {
        return downloader ?: FileDownloadHandler(activity).apply {
            onDownloadsCancelledListener = {
                App.instance.state.downloadCancellation.signal()
            }
            onCompletedNotificationClickListener = {
                activity.startActivity(
                    Intent(activity, DownloadsActivity::class.java)
                )
            }
            onCompletionListener = { id: FileDownloadIdentifier, _: DownloadStatus ->
                if (!isDownloadRunning) {
                    App.instance.state.download.of(id).completed()
                }
            }
        }
    }
}
