package de.xikolo.download.filedownload

import de.xikolo.download.DownloadIdentifier

data class FileDownloadIdentifier(
    val url: String
) : DownloadIdentifier {
    override fun toString(): String {
        return url
    }
}
