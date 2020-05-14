package de.xikolo.download

interface DownloadHandler<I : DownloadIdentifier, R : DownloadRequest> {

    fun download(request: R): I

    fun cancel(identifier: I): Boolean

    fun status(identifier: I): DownloadStatus?

    var onCompletionListener: ((I, DownloadStatus) -> Unit)?

    var onDownloadsCancelledListener: (() -> Unit)?

    var onRunningNotificationClickListener: ((List<I>) -> Unit)?

    var onCompletedNotificationClickListener: ((I) -> Unit)?
}
