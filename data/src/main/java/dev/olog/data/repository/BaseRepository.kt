package dev.olog.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dev.olog.core.dagger.ApplicationContext
import dev.olog.core.gateway.base.BaseGateway
import dev.olog.data.DataObserver
import dev.olog.data.utils.PermissionsUtils
import dev.olog.data.utils.assertBackground
import dev.olog.data.utils.assertBackgroundThread
import dev.olog.shared.CustomScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

internal abstract class BaseRepository<T, Param>(
    @ApplicationContext protected val context: Context
) : BaseGateway<T, Param>, CoroutineScope by CustomScope() {

    protected val contentResolver: ContentResolver = context.contentResolver

    protected val channel = ConflatedBroadcastChannel<List<T>>()

    protected fun firstQuery() {
        launch {
            assertBackgroundThread()

            while (!PermissionsUtils.canReadStorage(context)) {
                delay(300)
            }

            val contentUri = registerMainContentUri()

            contentResolver.registerContentObserver(
                contentUri.uri,
                contentUri.notifyForDescendants,
                DataObserver { channel.offer(queryAll()) }
            )
            channel.offer(queryAll())
        }
    }

    override fun getAll(): List<T> {
        assertBackgroundThread()
        return channel.valueOrNull
            ?: queryAll() // fallback to normal query if channel never emitted
    }

    override fun observeAll(): Flow<List<T>> {
        return channel.asFlow().assertBackground()
    }

    protected fun <R> observeByParamInternal(
        contentUri: ContentUri,
        action: () -> R
    ): Flow<R> {

        val flow: Flow<R> = channelFlow {

            if (!isClosedForSend) {
                offer(action())
            }

            val observer = DataObserver {
                if (!isClosedForSend) {
                    offer(action())
                }
            }

            contentResolver.registerContentObserver(
                contentUri.uri,
                contentUri.notifyForDescendants,
                observer
            )
            awaitClose { contentResolver.unregisterContentObserver(observer) }
        }
        return flow.assertBackground()
    }

    protected abstract fun registerMainContentUri(): ContentUri
    protected abstract fun queryAll(): List<T>

}

class ContentUri(
    @JvmField
    val uri: Uri,
    @JvmField
    val notifyForDescendants: Boolean
)