package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.account.qr.GetQRCodeFileUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDeleteQRCodeTest {

    private lateinit var underTest: DefaultDeleteQRCode

    private val accountRepository: AccountRepository = mock()
    private val nodeRepository: NodeRepository = mock()
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase = mock()

    @Before
    fun setup() {
        underTest = DefaultDeleteQRCode(
            accountRepository = accountRepository,
            nodeRepository = nodeRepository,
            ioDispatcher = UnconfinedTestDispatcher(),
            getQRCodeFileUseCase = getQRCodeFileUseCase

        )
    }

    @Test
    fun `test that QR code can be deleted successfully`() = runTest {
        val contactLink = "https://mega.nz/C!MTAwMDAwMA=="
        val handle = 1000000L
        val qrFileName = "tester@mega.co.nzQR_code_image.jpg"
        val qrCodeFile: File = mock()

        whenever(nodeRepository.convertBase64ToHandle(any())).thenReturn(handle)
        whenever(getQRCodeFileUseCase.invoke()).thenReturn(qrCodeFile)

        underTest(contactLink)
        verify(accountRepository).deleteContactLink(handle)
    }

    @Test(expected = MegaException::class)
    fun `test that exception is thrown if MegaApi to delete accountLink fails`() = runTest {
        val contactLink = "https://mega.nz/C!MTAwMDAwMA=="
        val handle = 1000000L
        val qrFileName = "tester@mega.co.nzQR_code_image.jpg"

        whenever(nodeRepository.convertBase64ToHandle(any())).thenReturn(handle)
        whenever(accountRepository.deleteContactLink(handle)).thenAnswer {
            throw MegaException(
                errorCode = -1,
                errorString = "error"
            )
        }

        underTest(contactLink)
    }

    @Test(expected = MegaException::class)
    fun `test that exception is thrown if accountLink is invalid`() = runTest {
        val contactLink = "https://mega.nz/C!MTAwMDAwMA=="
        val qrFileName = "tester@mega.co.nzQR_code_image.jpg"

        whenever(nodeRepository.convertBase64ToHandle(any())).thenAnswer {
            throw MegaException(
                -1,
                "convert base64 failed"
            )
        }

        underTest(contactLink)
    }

}