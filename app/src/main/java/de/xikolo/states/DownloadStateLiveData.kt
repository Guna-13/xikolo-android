package de.xikolo.states

import de.xikolo.download.DownloadIdentifier
import de.xikolo.states.base.LiveDataState

class DownloadStateLiveData :
    LiveDataState<DownloadStateLiveData.DownloadStateCode>(DownloadStateCode.DELETED) {

    companion object {
        private val map = mutableMapOf<String, DownloadStateLiveData>()

        fun of(key: DownloadIdentifier?): DownloadStateLiveData {
            return if (key != null) {
                val newObj = DownloadStateLiveData()
                if (!map.containsKey(key.toString())) {
                    map[key.toString()] = newObj
                }

                map[key.toString()] ?: newObj
            } else {
                DownloadStateLiveData()
            }
        }
    }

    fun started() {
        super.state(DownloadStateCode.STARTED)
    }

    fun deleted() {
        super.state(DownloadStateCode.DELETED)
    }

    fun completed() {
        super.state(DownloadStateCode.COMPLETED)
    }

    enum class DownloadStateCode {
        STARTED, COMPLETED, DELETED
    }
}
