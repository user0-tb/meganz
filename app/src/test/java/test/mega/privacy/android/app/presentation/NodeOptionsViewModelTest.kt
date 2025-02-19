package test.mega.privacy.android.app.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel
import mega.privacy.android.app.presentation.bottomsheet.model.NodeBottomSheetUIState
import mega.privacy.android.app.presentation.bottomsheet.model.NodeShareInformation
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * Test class for [NodeOptionsViewModel]
 */
@ExperimentalCoroutinesApi
class NodeOptionsViewModelTest {

    private lateinit var underTest: NodeOptionsViewModel
    private val getNodeByHandle =
        mock<GetNodeByHandle> { onBlocking { invoke(any()) }.thenReturn(null) }
    private val createShareKey = mock<CreateShareKey>()
    private val isNodeDeletedFromBackupsUseCase = mock<IsNodeDeletedFromBackupsUseCase> {
        onBlocking { invoke(NodeId(any())) }.thenReturn(false)
    }
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase> {
        onBlocking { invoke() }.thenReturn(
            MutableStateFlow(true)
        )
    }

    private val nodeIdFlow = MutableStateFlow(-1L)

    private val nodeShareInformationFlow = MutableStateFlow<NodeShareInformation?>(null)

    private val savedStateHandle = mock<SavedStateHandle> {
        on {
            getStateFlow(
                NodeOptionsViewModel.NODE_ID_KEY,
                -1L
            )
        }.thenReturn(nodeIdFlow)

        on {
            getStateFlow<NodeShareInformation?>(NodeOptionsViewModel.SHARE_DATA_KEY, null)
        }.thenReturn(nodeShareInformationFlow)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = NodeOptionsViewModel(
            createShareKey = createShareKey,
            getNodeByHandle = getNodeByHandle,
            isNodeDeletedFromBackupsUseCase = isNodeDeletedFromBackupsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.canMoveNode).isFalse()
            assertThat(initial.canRestoreNode).isFalse()
            assertThat(initial.isOnline).isTrue()
            assertThat(initial.node).isNull()
            assertThat(initial.shareData).isNull()
            assertThat(initial.shareKeyCreated).isNull()
        }
    }

    @Test
    fun `test that shareKeyCreated is true if created successfully`() = runTest {
        val node = mock<MegaNode>()
        val nodeId = 123L
        getNodeByHandle.stub {
            onBlocking { invoke(nodeId) }.thenReturn(node)
        }
        nodeIdFlow.emit(nodeId)

        underTest.state.filter { it.node != null }
            .distinctUntilChangedBy(NodeBottomSheetUIState::shareKeyCreated)
            .test {
                assertThat(awaitItem().shareKeyCreated).isNull()
                underTest.createShareKey()
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem().shareKeyCreated).isTrue()
            }
    }

    @Test
    fun `test that shareKeyCreated is false if created throws an exception`() = runTest {
        val node = mock<MegaNode>()
        val nodeId = 123L
        getNodeByHandle.stub {
            onBlocking { invoke(nodeId) }.thenReturn(node)
        }
        nodeIdFlow.emit(nodeId)

        createShareKey.stub {
            onBlocking { invoke(any()) }.thenAnswer { throw Throwable() }
        }
        underTest.state.filter { it.node != null }
            .distinctUntilChangedBy(NodeBottomSheetUIState::shareKeyCreated)
            .test {
                assertThat(awaitItem().shareKeyCreated).isNull()
                underTest.createShareKey()
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem().shareKeyCreated).isFalse()
            }
    }

    @Test
    fun `test that shareKeyCreated is false if called with null node`() = runTest {
        createShareKey.stub {
            onBlocking { invoke(any()) }.thenAnswer { throw Throwable() }
        }

        underTest.state
            .distinctUntilChangedBy(NodeBottomSheetUIState::shareKeyCreated)
            .test {
                val initialState = awaitItem()
                assertThat(initialState.shareKeyCreated).isNull()
                assertThat(initialState.node).isNull()
                underTest.createShareKey()
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem().shareKeyCreated).isFalse()
            }
    }

    @Test
    fun `test that share info is returned if found`() = runTest {
        val expectedShareInformation = NodeShareInformation(
            user = null,
            isPending = false,
            isVerified = false
        )

        underTest.state.test {
            assertThat(awaitItem().shareData).isNull()
            nodeShareInformationFlow.emit(expectedShareInformation)
            assertThat(awaitItem().shareData).isEqualTo(expectedShareInformation)
        }
    }

    @Test
    fun `test that the restore node functionality works as expected for deleted non-backup nodes`() =
        runTest {
            val node = mock<MegaNode>()
            val nodeId = 123L
            getNodeByHandle.stub {
                onBlocking { invoke(nodeId) }.thenReturn(node)
            }
            isNodeDeletedFromBackupsUseCase.stub {
                onBlocking { invoke(NodeId(any())) }.thenReturn(false)
            }

            underTest.state.test {
                assertThat(awaitItem().node).isNull()
                nodeIdFlow.emit(nodeId)
                assertThat(awaitItem().node).isEqualTo(node)
                underTest.setRestoreNodeClicked(true)
                assertThat(awaitItem().canRestoreNode).isTrue()
            }
        }

    @Test
    fun `test that the restore node functionality becomes the move node functionality for deleted backup nodes`() =
        runTest {
            val node = mock<MegaNode>()
            val nodeId = 123L
            getNodeByHandle.stub {
                onBlocking { invoke(nodeId) }.thenReturn(node)
            }
            isNodeDeletedFromBackupsUseCase.stub {
                onBlocking { invoke(NodeId(any())) }.thenReturn(true)
            }

            underTest.state.test {
                assertThat(awaitItem().node).isNull()
                nodeIdFlow.emit(nodeId)
                assertThat(awaitItem().node).isEqualTo(node)
                underTest.setRestoreNodeClicked(true)
                assertThat(awaitItem().canMoveNode).isTrue()
            }
        }
}