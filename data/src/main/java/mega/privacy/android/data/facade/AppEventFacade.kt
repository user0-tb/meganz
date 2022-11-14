package mega.privacy.android.data.facade

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.extensions.registerReceiverAsFlow
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.domain.qualifier.ApplicationScope
import javax.inject.Inject

/**
 * Default implementation of [AppEventGateway]
 */
internal class AppEventFacade @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : AppEventGateway {

    private val _monitorCameraUploadPauseState = MutableSharedFlow<Boolean>()

    override val monitorCameraUploadPauseState =
        _monitorCameraUploadPauseState.toSharedFlow(appScope)

    override val monitorBatteryInfo =
        context.registerReceiverAsFlow(Intent.ACTION_BATTERY_CHANGED).map {
            return@map it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        }.toSharedFlow(appScope)

    override suspend fun broadcastUploadPauseState() =
        _monitorCameraUploadPauseState.emit(true)
}

private fun <T> Flow<T>.toSharedFlow(
    scope: CoroutineScope,
) = shareIn(scope, started = SharingStarted.WhileSubscribed())
