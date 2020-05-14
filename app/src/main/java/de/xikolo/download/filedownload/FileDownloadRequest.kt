package de.xikolo.download.filedownload

import android.app.DownloadManager
import android.app.DownloadManager.Request.NETWORK_MOBILE
import android.app.DownloadManager.Request.NETWORK_WIFI
import android.app.DownloadManager.Request.VISIBILITY_HIDDEN
import android.app.DownloadManager.Request.VISIBILITY_VISIBLE
import android.net.Uri
import androidx.core.net.toUri
import de.xikolo.App
import de.xikolo.R
import de.xikolo.config.Config
import de.xikolo.download.DownloadRequest
import de.xikolo.managers.UserManager
import de.xikolo.storages.ApplicationPreferences
import java.io.File
import java.util.Locale

data class FileDownloadRequest(
    val url: String,
    val localFile: File,
    override val title: String,
    override val showNotification: Boolean
) : DownloadRequest {

    fun buildDownloadManagerRequest(): DownloadManager.Request {
        val uri = Uri.parse(url)
        return DownloadManager.Request(uri)
            .setAllowedNetworkTypes(
                NETWORK_WIFI or
                    if (ApplicationPreferences().isDownloadNetworkLimitedOnMobile) {
                        NETWORK_MOBILE
                    } else 0
            )
            .setDestinationUri(localFile.toUri())
            .setNotificationVisibility(
                if (showNotification) {
                    VISIBILITY_VISIBLE
                } else {
                    VISIBILITY_HIDDEN
                }
            )
            .setTitle(title)
            .addRequestHeader(Config.HEADER_USER_AGENT, Config.HEADER_USER_AGENT_VALUE)
            .addRequestHeader(
                Config.HEADER_ACCEPT,
                Config.MEDIA_TYPE_JSON_API + "; xikolo-version=" + Config.XIKOLO_API_VERSION
            )
            .addRequestHeader(Config.HEADER_CONTENT_TYPE, Config.MEDIA_TYPE_JSON_API)
            .addRequestHeader(Config.HEADER_USER_PLATFORM, Config.HEADER_USER_PLATFORM_VALUE)
            .addRequestHeader(Config.HEADER_ACCEPT_LANGUAGE, Locale.getDefault().language)
            .apply {
                if (uri.host == App.instance.getString(R.string.app_host)
                    && UserManager.isAuthorized
                ) {
                    addRequestHeader(
                        Config.HEADER_AUTH,
                        Config.HEADER_AUTH_VALUE_PREFIX_JSON_API + UserManager.token!!
                    )
                }
            }
    }
}
