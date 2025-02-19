package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.offline.InboxOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GetOfflineFileUseCaseTest {
    private lateinit var underTest: GetOfflineFileUseCase

    private val fileName = "fileName"
    private val offlinePath = "Offline path"
    private val offlineInboxPath = "Offline inbox path"
    private val handle = "handle"

    private val fileSystemRepository = mock<FileSystemRepository> {
        onBlocking { getOfflinePath() }.thenReturn(offlinePath)
        onBlocking { getOfflineInboxPath() }.thenReturn(offlineInboxPath)
    }

    @Before
    fun setUp() {
        underTest = GetOfflineFileUseCase(
            fileSystemRepository = fileSystemRepository
        )
    }

    @Test
    fun `test that OtherOfflineNodeInformation returns a file with the offline path and the file name if the path is a file separator`() =
        runTest {
            val input = OtherOfflineNodeInformation(
                path = File.separator,
                name = fileName,
                handle = handle,
                isFolder = false,
            )
            val expected = offlinePath + File.separator + fileName

            val actual = underTest(input)

            assertThat(actual.path).isEqualTo(expected)
        }

    @Test
    fun `test that OtherOfflineNodeInformation returns a file with the offline path and the file name if the path is empty`() =
        runTest {
            val input = OtherOfflineNodeInformation(
                path = "",
                name = fileName,
                handle = handle,
                isFolder = false,
            )
            val expected = offlinePath + File.separator + fileName

            val actual = underTest(input)

            assertThat(actual.path).isEqualTo(expected)
        }

    @Test
    fun `test that OtherOfflineNodeInformation returns a file with the offline path, the file path, and the file name if the path is not empty`() =
        runTest {
            val path = "path/to/file"
            val input = OtherOfflineNodeInformation(
                path = path,
                name = fileName,
                handle = handle,
                isFolder = false,
            )
            val expected = offlinePath + File.separator + path + File.separator + fileName

            val actual = underTest(input)

            assertThat(actual.path).isEqualTo(expected)
        }

    @Test
    fun `test that InboxOfflineNodeInformation returns a file with the offline inbox path and the file name if the path is a file separator`() =
        runTest {
            val input = InboxOfflineNodeInformation(
                path = File.separator,
                name = fileName,
                handle = handle,
                isFolder = false,
            )
            val expected = offlineInboxPath + File.separator + fileName

            val actual = underTest(input)

            assertThat(actual.path).isEqualTo(expected)
        }

    @Test
    fun `test that InboxOfflineNodeInformation returns a file with the offline inbox path and the file name if the path is empty`() =
        runTest {
            val input = InboxOfflineNodeInformation(
                path = "",
                name = fileName,
                handle = handle,
                isFolder = false,
            )
            val expected = offlineInboxPath + File.separator + fileName

            val actual = underTest(input)

            assertThat(actual.path).isEqualTo(expected)
        }

    @Test
    fun `test that InboxOfflineNodeInformation returns a file with the offline inbox path, the file path, and the file name if the path is not empty`() =
        runTest {
            val path = "path/to/file"
            val input = InboxOfflineNodeInformation(
                path = path,
                name = fileName,
                handle = handle,
                isFolder = false,
            )
            val expected = offlineInboxPath + File.separator + path + File.separator + fileName

            val actual = underTest(input)

            assertThat(actual.path).isEqualTo(expected)
        }

    @Test
    fun `test that IncomingShareOfflineNodeInformation returns a file with the inbox path, incoming handle, and the file name if the path is a file separator`() =
        runTest {
            val incomingHandle = "handle"
            val input = IncomingShareOfflineNodeInformation(
                path = File.separator,
                name = fileName,
                handle = handle,
                incomingHandle = incomingHandle,
                isFolder = false,
            )
            val expected = offlinePath + File.separator + incomingHandle + File.separator + fileName

            val actual = underTest(input)

            assertThat(actual.path).isEqualTo(expected)
        }

    @Test
    fun `test that IncomingShareOfflineNodeInformation returns a file with the inbox path, incoming handle, and the file name if the path is empty`() =
        runTest {
            val incomingHandle = "handle"
            val input = IncomingShareOfflineNodeInformation(
                path = "",
                name = fileName,
                handle = handle,
                incomingHandle = incomingHandle,
                isFolder = false,
            )
            val expected = offlinePath + File.separator + incomingHandle + File.separator + fileName

            val actual = underTest(input)

            assertThat(actual.path).isEqualTo(expected)
        }

    @Test
    fun `test that IncomingShareOfflineNodeInformation returns a file with the inbox path, incoming handle, the file path, and the file name if the path is not empty`() =
        runTest {
            val incomingHandle = "handle"
            val path = "path/to/file"
            val input = IncomingShareOfflineNodeInformation(
                path = path,
                name = fileName,
                handle = handle,
                incomingHandle = incomingHandle,
                isFolder = false,
            )
            val expected =
                offlinePath + File.separator + incomingHandle + File.separator + path + File.separator + fileName

            val actual = underTest(input)

            assertThat(actual.path).isEqualTo(expected)
        }
}