package mega.privacy.android.data.repository

import android.graphics.BitmapFactory
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.repository.DefaultAvatarRepository.Companion.AVATAR_PRIMARY_COLOR
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.data.wrapper.BitmapFactoryWrapper
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Default [AvatarRepository] implementation
 *
 * @param megaApiGateway
 * @param cacheGateway
 * @param sharingScope scope for share flow
 * @param ioDispatcher coroutine dispatcher to execute
 * @param bitmapFactoryWrapper
 */
internal class DefaultAvatarRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val contactsRepository: ContactsRepository,
    private val cacheGateway: CacheGateway,
    private val avatarWrapper: AvatarWrapper,
    private val bitmapFactoryWrapper: BitmapFactoryWrapper,
    private val databaseHandler: DatabaseHandler,
    @ApplicationScope private val sharingScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AvatarRepository {
    private val myAvatarFile = MutableSharedFlow<File?>()

    init {
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
            .mapNotNull {
                val currentUserHandle = megaApiGateway.myUser?.handle
                it.users?.find { user ->
                    user.isOwnChange == 0
                            && user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong())
                            && user.handle == currentUserHandle
                }
            }
            .map { user ->
                deleteAvatarFile(user)
                myAvatarFile.emit(loadAvatarFile(user))
            }
            .catch { Timber.e(it) }
            .flowOn(ioDispatcher)
            .launchIn(sharingScope)
    }

    override fun monitorMyAvatarFile() = myAvatarFile.asSharedFlow()

    private suspend fun deleteAvatarFile(user: MegaUser) {
        val oldFile =
            cacheGateway.buildAvatarFile(user.email + FileConstant.JPG_EXTENSION) ?: return
        if (oldFile.exists()) {
            oldFile.delete()
        }
    }

    private suspend fun loadAvatarFile(user: MegaUser): File? {
        val avatarFile =
            cacheGateway.buildAvatarFile(user.email + FileConstant.JPG_EXTENSION)
                ?: return null
        megaApiGateway.getUserAvatar(user, avatarFile.absolutePath)
        return avatarFile
    }

    override suspend fun getMyAvatarColor(): Int {
        val user = megaApiGateway.getLoggedInUser() ?: return getColor(null)
        val avatarFile = getMyAvatarFile()
        if (avatarFile != null) {
            val avatarBitmap = bitmapFactoryWrapper.decodeFile(
                avatarFile.absolutePath,
                BitmapFactory.Options()
            )
            if (avatarBitmap != null) {
                return avatarWrapper.getDominantColor(avatarBitmap)
            }
        }
        return getColor(megaApiGateway.getUserAvatarColor(user))
    }

    override suspend fun getMyAvatarFile(isForceRefresh: Boolean): File? =
        withContext(ioDispatcher) {
            if (isForceRefresh) {
                megaApiGateway.myUser?.let {
                    return@withContext loadAvatarFile(it)
                }
            }
            return@withContext cacheGateway.buildAvatarFile(databaseHandler.myEmail + FileConstant.JPG_EXTENSION)
        }

    override suspend fun getAvatarFile(userHandle: Long, skipCache: Boolean): File =
        withContext(ioDispatcher) {
            val userEmail = getUserEmail(userHandle) ?: error("Could not get user email")
            getAvatarFile(userEmail, skipCache)
        }

    override suspend fun getAvatarFile(userEmail: String, skipCache: Boolean): File =
        withContext(ioDispatcher) {
            val file = cacheGateway.buildAvatarFile(userEmail + FileConstant.JPG_EXTENSION)
                ?: error("Could not generate avatar file")

            if (!skipCache && file.exists() && file.canRead() && file.length() > 0) {
                return@withContext file
            }

            suspendCoroutine { continuation ->
                megaApiGateway.getContactAvatar(
                    userEmail,
                    file.absolutePath,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { _: MegaRequest, error: MegaError ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resume(file)
                            } else {
                                if (error.errorCode == MegaError.API_ENOENT && file.exists()) {
                                    file.delete()
                                }
                                continuation.failWithError(error, "getAvatarFile")
                            }
                        }
                    ))
            }
        }

    override suspend fun getAvatarColor(userHandle: Long): Int =
        withContext(ioDispatcher) {
            getColor(megaApiGateway.getUserAvatarColor(userHandle))
        }

    private suspend fun getUserEmail(userHandle: Long): String? =
        runCatching { contactsRepository.getUserEmail(userHandle) }.getOrNull()

    private fun getColor(color: String?): Int =
        color?.toColorInt() ?: avatarWrapper.getSpecificAvatarColor(AVATAR_PRIMARY_COLOR)

    override suspend fun updateMyAvatarWithNewEmail(oldEmail: String, newEmail: String): Boolean =
        withContext(ioDispatcher) {
            if (oldEmail.isBlank() || oldEmail == newEmail) return@withContext false
            val oldFile =
                cacheGateway.buildAvatarFile(oldEmail + FileConstant.JPG_EXTENSION)
                    ?: return@withContext false
            if (oldFile.exists()) {
                val newFile =
                    cacheGateway.buildAvatarFile(newEmail + FileConstant.JPG_EXTENSION)
                        ?: return@withContext false
                return@withContext oldFile.renameTo(newFile)

            }
            return@withContext false
        }

    override suspend fun setAvatar(filePath: String?) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("setAvatar") {}
            megaApiGateway.setAvatar(filePath, listener)
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
        megaApiGateway.myUser?.let { user ->
            deleteAvatarFile(user)
            myAvatarFile.emit(loadAvatarFile(user))
        }
        Unit
    }

    companion object {
        /**
         * refer [mega.privacy.android.app.utils.Constants.AVATAR_PRIMARY_COLOR]
         */
        private const val AVATAR_PRIMARY_COLOR = "AVATAR_PRIMARY_COLOR"
    }
}