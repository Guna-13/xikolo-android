package de.xikolo.network.jobs

import android.util.Log
import de.xikolo.config.Config
import de.xikolo.network.ApiService
import de.xikolo.network.jobs.base.HealthCheckNetworkStateLiveData
import de.xikolo.network.jobs.base.NetworkJob
import ru.gildor.coroutines.retrofit.awaitResponse
import java.util.*

class CheckHealthJob(private val networkState: HealthCheckNetworkStateLiveData, private val userRequest: Boolean) : NetworkJob(networkState, userRequest) {

    companion object {
        val TAG: String = CheckHealthJob::class.java.simpleName
    }

    override suspend fun onRun() {
        val response = ApiService.instance.base().awaitResponse()

        when (response.code()) {
            in 200..299 -> {
                val apiVersionExpirationDate = response.headers().getDate(Config.HEADER_API_VERSION_EXPIRATION_DATE)
                if (apiVersionExpirationDate != null) {
                    if (Config.DEBUG) Log.e(TAG, "Health check: api deprecated and will expire at $apiVersionExpirationDate")
                    deprecated(apiVersionExpirationDate)
                } else {
                    if (Config.DEBUG) Log.i(TAG, "Health check: successful")
                    success()
                }
            }
            406         -> {
                if (Config.DEBUG) Log.e(TAG, "Health check: api version expired")
                apiVersionExpired()
            }
            503         -> {
                if (Config.DEBUG) Log.e(TAG, "Health check: server maintenance ongoing")
                maintenance()
            }
            else        -> {
                if (Config.DEBUG) Log.e(TAG, "Health check: unclassified error")
                error()
            }
        }
    }

    private fun deprecated(deprecationDate: Date) = networkState.deprecated(deprecationDate, userRequest)

    private fun apiVersionExpired() = networkState.apiVersionExpired(userRequest)

    private fun maintenance() = networkState.maintenance(userRequest)

}
