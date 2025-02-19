package test.mega.privacy.android.app.presentation.inbox

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.inbox.InboxViewModel
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import nz.mega.sdk.MegaNode
import org.junit.Rule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.domain.usecase.FakeMonitorBackupFolder
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

/**
 * Test class for [InboxViewModel]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InboxViewModelTest {
    private lateinit var underTest: InboxViewModel

    private val getChildrenNode = mock<GetChildrenNode>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getParentNodeHandle = mock<GetParentNodeHandle>()

    private val monitorBackupFolder = FakeMonitorBackupFolder()
    private val monitorNodeUpdates = FakeMonitorUpdates()
    private val monitorViewType = mock<MonitorViewType>()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @BeforeAll
    internal fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(
            getChildrenNode,
            getCloudSortOrder,
            getNodeByHandle,
            getParentNodeHandle,
            monitorViewType,
        )
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setUnderTest() {
        underTest = InboxViewModel(
            getChildrenNode = getChildrenNode,
            getCloudSortOrder = getCloudSortOrder,
            getNodeByHandle = getNodeByHandle,
            getParentNodeHandle = getParentNodeHandle,
            monitorBackupFolder = monitorBackupFolder,
            monitorNodeUpdates = monitorNodeUpdates,
            monitorViewType = monitorViewType,
        )
    }

    @Test
    internal fun `test that initial state is returned`() = runTest {
        setUnderTest()

        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.hideMultipleItemSelection).isFalse()
            assertThat(initialState.inboxHandle).isEqualTo(-1L)
            assertThat(initialState.nodes).isEmpty()
            assertThat(initialState.shouldExitInbox).isFalse()
            assertThat(initialState.triggerBackPress).isFalse()
            assertThat(initialState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }

    @Test
    internal fun `test that isPendingRefresh is true when receiving a node update`() = runTest {
        val inboxNode = mock<MegaNode> {
            on { this.handle }.thenReturn(INBOX_NODE_HANDLE)
        }
        val retrievedNode = mock<MegaNode> {
            on { this.handle }.thenReturn(RETRIEVED_NODE_HANDLE)
        }
        whenever(getNodeByHandle(any())).thenReturn(inboxNode)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            getChildrenNode(
                parent = inboxNode,
                order = getCloudSortOrder(),
            )
        ).thenReturn(listOf(retrievedNode))

        setUnderTest()

        underTest.updateInboxHandle(INBOX_NODE_HANDLE)
        monitorNodeUpdates.emit(NodeUpdate(emptyMap()))

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isPendingRefresh).isTrue()
        }
    }

    @Test
    internal fun `test that nodes are not refreshed when receiving a node update and the inbox handle is invalid`() =
        runTest {
            val inboxNode = mock<MegaNode> {
                on { this.handle }.thenReturn(INBOX_NODE_HANDLE)
            }
            val retrievedNode = mock<MegaNode> {
                on { this.handle }.thenReturn(RETRIEVED_NODE_HANDLE)
            }
            whenever(getNodeByHandle(any())).thenReturn(inboxNode)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                getChildrenNode(
                    parent = inboxNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(listOf(retrievedNode))

            setUnderTest()

            val update = mapOf(mock<Node>() to emptyList<NodeChanges>())
            monitorNodeUpdates.emit(NodeUpdate(update))

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.nodes).isEmpty()
            }
        }

    @Test
    internal fun `test that when receiving a my backups folder update, the nodes are refreshed`() =
        runTest {
            val inboxNode = mock<MegaNode> {
                on { this.handle }.thenReturn(INBOX_NODE_HANDLE)
            }
            val myBackupsNode = mock<NodeId> {
                on { this.longValue }.thenReturn(MY_BACKUPS_HANDLE)
            }
            val retrievedNode = mock<MegaNode> {
                on { this.handle }.thenReturn(RETRIEVED_NODE_HANDLE)
            }
            whenever(getNodeByHandle(any())).thenReturn(inboxNode)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                getChildrenNode(
                    parent = inboxNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(listOf(retrievedNode))

            setUnderTest()

            underTest.updateInboxHandle(INBOX_NODE_HANDLE)
            monitorBackupFolder.emit(Result.success(myBackupsNode))

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.inboxHandle).isEqualTo(INBOX_NODE_HANDLE)
                assertThat(state.nodes).isEqualTo(listOf(retrievedNode))
            }
        }

    @Test
    internal fun `test that when receiving a my backups folder update, the nodes are refreshed using the my backups folder node handle`() =
        runTest {
            val inboxNode = mock<MegaNode> {
                on { this.handle }.thenReturn(INBOX_NODE_HANDLE)
            }
            val myBackupsNode = mock<NodeId> {
                on { this.longValue }.thenReturn(MY_BACKUPS_HANDLE)
            }
            val retrievedNode = mock<MegaNode> {
                on { this.handle }.thenReturn(RETRIEVED_NODE_HANDLE)
            }
            whenever(getNodeByHandle(any())).thenReturn(inboxNode)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                getChildrenNode(
                    parent = inboxNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(listOf(retrievedNode))

            setUnderTest()

            monitorBackupFolder.emit(Result.success(myBackupsNode))

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.inboxHandle).isEqualTo(myBackupsNode.longValue)
                assertThat(state.nodes).isEqualTo(listOf(retrievedNode))
            }
        }

    @Test
    internal fun `test that the inbox handle is updated if a new value is provided`() = runTest {
        setUnderTest()

        underTest.state.map { it.inboxHandle }.distinctUntilChanged()
            .test {
                val newHandle = 123456L
                assertThat(awaitItem()).isEqualTo(-1L)
                underTest.updateInboxHandle(newHandle)
                assertThat(awaitItem()).isEqualTo(newHandle)
            }
    }

    @Test
    internal fun `test that the multiple item selection has been handled`() = runTest {
        setUnderTest()

        underTest.hideMultipleItemSelectionHandled()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.hideMultipleItemSelection).isFalse()
        }
    }

    @Test
    internal fun `test that exiting the inbox has been handled`() = runTest {
        setUnderTest()

        underTest.exitInboxHandled()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.shouldExitInbox).isFalse()
        }
    }

    @Test
    internal fun `test that the back press has been handled`() = runTest {
        setUnderTest()

        underTest.triggerBackPressHandled()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.triggerBackPress).isFalse()
        }
    }

    @Test
    internal fun `test that the user is currently on the backup folder level`() = runTest {
        val myBackupsNode = mock<NodeId> {
            on { this.longValue }.thenReturn(MY_BACKUPS_HANDLE)
        }
        val inboxNode = mock<MegaNode> {
            on { this.handle }.thenReturn(INBOX_NODE_HANDLE)
        }
        val retrievedNode = mock<MegaNode> {
            on { this.handle }.thenReturn(RETRIEVED_NODE_HANDLE)
        }
        whenever(getNodeByHandle(any())).thenReturn(inboxNode)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            getChildrenNode(
                parent = inboxNode,
                order = getCloudSortOrder(),
            )
        ).thenReturn(listOf(retrievedNode))

        setUnderTest()

        monitorBackupFolder.emit(Result.success(myBackupsNode))

        assertThat(underTest.isCurrentlyOnBackupFolderLevel()).isTrue()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.inboxHandle).isEqualTo(myBackupsNode.longValue)
        }
    }

    @Test
    internal fun `test that the user is currently on the backup folder level if the inbox handle is invalid`() =
        runTest {
            setUnderTest()

            assertThat(underTest.isCurrentlyOnBackupFolderLevel()).isTrue()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.inboxHandle).isEqualTo(-1L)
            }
        }

    @Test
    internal fun `test that the user exits the inbox on back press if the my backups folder handle is -1L`() =
        runTest {
            setUnderTest()

            monitorBackupFolder.emit(Result.success(NodeId(-1L)))

            underTest.state.map { it.shouldExitInbox }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isFalse()
                    underTest.handleBackPress()
                    assertThat(awaitItem()).isTrue()
                }
        }

    @Test
    internal fun `test that the user exits the inbox on back press if both the my backups folder and inbox ui state have the same handles`() =
        runTest {
            val myBackupsNode = mock<NodeId> {
                on { this.longValue }.thenReturn(MY_BACKUPS_HANDLE)
            }
            setUnderTest()

            monitorBackupFolder.emit(Result.success(myBackupsNode))

            underTest.state.map { it.shouldExitInbox }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isFalse()
                    with(underTest) {
                        updateInboxHandle(MY_BACKUPS_HANDLE)
                        handleBackPress()
                    }
                    assertThat(awaitItem()).isTrue()
                }
        }

    @Test
    internal fun `test that the user exits the inbox on back press if the parent node handle is null`() =
        runTest {
            val myBackupsNode = mock<NodeId> {
                on { this.longValue }.thenReturn(MY_BACKUPS_HANDLE)
            }
            whenever(getParentNodeHandle(any())).thenReturn(null)

            setUnderTest()

            monitorBackupFolder.emit(Result.success(myBackupsNode))

            underTest.state.map { it.shouldExitInbox }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isFalse()
                    with(underTest) {
                        updateInboxHandle(INBOX_NODE_HANDLE)
                        handleBackPress()
                    }
                    assertThat(awaitItem()).isTrue()
                }
        }

    @Test
    internal fun `test that the nodes are updated on back press`() = runTest {
        val parentNode = mock<MegaNode> {
            on { it.handle }.thenReturn(654L)
        }
        val myBackupsNode = mock<NodeId> {
            on { this.longValue }.thenReturn(MY_BACKUPS_HANDLE)
        }
        val retrievedNode = mock<MegaNode> {
            on { this.handle }.thenReturn(RETRIEVED_NODE_HANDLE)
        }
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(getParentNodeHandle(any())).thenReturn(654L)
        whenever(getNodeByHandle(any())).thenReturn(parentNode)
        whenever(
            getChildrenNode(
                parent = parentNode,
                order = getCloudSortOrder()
            )
        ).thenReturn(listOf(retrievedNode))

        setUnderTest()

        monitorBackupFolder.emit(Result.success(myBackupsNode))

        with(underTest) {
            updateInboxHandle(INBOX_NODE_HANDLE)
            handleBackPress()
        }

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.inboxHandle).isEqualTo(654L)
            assertThat(state.triggerBackPress).isTrue()
            assertThat(state.nodes).isEqualTo(listOf(retrievedNode))
        }
    }

    @Test
    internal fun `test that get order returns cloud sort order`() = runTest {
        val expected = SortOrder.ORDER_SIZE_DESC
        whenever(getCloudSortOrder()).thenReturn(expected)

        setUnderTest()

        assertThat(underTest.getOrder()).isEqualTo(expected)
    }

    internal companion object {
        private const val MY_BACKUPS_HANDLE = 12L
        private const val INBOX_NODE_HANDLE = 34L
        private const val RETRIEVED_NODE_HANDLE = 56L
    }
}