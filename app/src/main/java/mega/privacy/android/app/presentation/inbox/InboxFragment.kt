package mega.privacy.android.app.presentation.inbox

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Spanned
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentBackupsBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForParentNode
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.MegaNodeUtil.shareNodes
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.displayMetrics
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import java.util.Stack
import javax.inject.Inject

/**
 * An instance of [RotatableFragment] that displays all content that were backed up by the user
 */
@OptIn(FlowPreview::class)
@AndroidEntryPoint
class InboxFragment : RotatableFragment() {

    /**
     * Inject [MegaApiAndroid] to the Fragment
     */
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    /**
     * Retrieves the UI state from [InboxViewModel]
     *
     * @return the UI State
     */
    private fun state() = viewModel.state.value

    // UI Elements
    private var binding: FragmentBackupsBinding? = null
    private val itemDecoration: PositionDividerItemDecoration by lazy(LazyThreadSafetyMode.NONE) {
        PositionDividerItemDecoration(requireContext(), displayMetrics())
    }

    private var megaNodeAdapter: MegaNodeAdapter? = null
    private var lastPositionStack: Stack<Int>? = null
    private var actionMode: ActionMode? = null

    private val viewModel by activityViewModels<InboxViewModel>()
    private val sortByHeaderViewModel by activityViewModels<SortByHeaderViewModel>()

    /**
     * onCreate Implementation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")
        lastPositionStack = Stack()
    }

    /**
     * onCreateView Implementation
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView()")

        val binding = FragmentBackupsBinding.inflate(inflater, container, false)
        this.binding = binding

        setupToolbar()
        setupAdapter()
        setupRecyclerView()
        switchViewType()

        return binding.root
    }

    /**
     * onViewCreated implementation
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeDragSupportEvents(
            lifecycleOwner = viewLifecycleOwner,
            rv = binding?.backupsRecyclerView,
            viewerFrom = Constants.VIEWER_FROM_INBOX,
        )
        observeUiState()
    }

    /**
     * onDestroyView implementation
     */
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    /**
     * getAdapter implementation
     */
    override fun getAdapter(): RotatableAdapter? = megaNodeAdapter

    /**
     * activateActionMode implementation
     */
    override fun activateActionMode() {
        Timber.d("activateActionMode()")
        megaNodeAdapter?.let {
            if (!it.isMultipleSelect) {
                it.isMultipleSelect = true
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    ActionBarCallBack()
                )
            }
        }
    }

    /**
     * multipleItemClick implementation
     */
    override fun multipleItemClick(position: Int) {
        megaNodeAdapter?.toggleSelection(position)
    }

    /**
     * reselectUnHandledSingleItem implementation
     */
    override fun reselectUnHandledSingleItem(position: Int) = Unit

    /**
     * Updates the Action Mode Title
     */
    override fun updateActionModeTitle() {
        if (actionMode == null) {
            return
        }

        val documents = megaNodeAdapter?.selectedNodes ?: emptyList()
        val files = documents.filter { it.isFile }.size
        val folders = documents.filter { it.isFolder }.size
        val sum = files + folders

        actionMode?.let {
            it.title = if (files == 0 && folders == 0) {
                sum.toString()
            } else if (files == 0) {
                folders.toString()
            } else if (folders == 0) {
                files.toString()
            } else {
                sum.toString()
            }

            try {
                actionMode?.invalidate()
            } catch (e: NullPointerException) {
                e.printStackTrace()
                Timber.e(e, "Invalidate error")
            }
        }
    }

    /**
     * The Action Bar Callback
     */
    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val selectedNodes = megaNodeAdapter?.selectedNodes ?: emptyList()
            when (item.itemId) {
                R.id.cab_menu_download -> {
                    (requireActivity() as ManagerActivity).saveNodesToDevice(
                        nodes = selectedNodes,
                        highPriority = false,
                        isFolderLink = false,
                        fromMediaViewer = false,
                        fromChat = false,
                    )
                    clearSelections()
                    hideMultipleSelect()
                }

                R.id.cab_menu_copy -> {
                    val handleList = ArrayList(selectedNodes.map { it.handle })
                    NodeController(requireActivity()).also {
                        it.chooseLocationToCopyNodes(handleList)
                    }
                    clearSelections()
                    hideMultipleSelect()
                }

                R.id.cab_menu_select_all -> {
                    selectAll()
                }

                R.id.cab_menu_unselect_all -> {
                    clearSelections()
                    hideMultipleSelect()
                }

                R.id.cab_menu_share_link -> {
                    (requireActivity() as ManagerActivity).showGetLinkActivity(selectedNodes)
                    clearSelections()
                    hideMultipleSelect()
                }

                R.id.cab_menu_share_out -> {
                    shareNodes(requireActivity(), selectedNodes)
                    clearSelections()
                    hideMultipleSelect()
                }
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.inbox_action, menu)
            checkScroll()
            return true
        }

        override fun onDestroyActionMode(arg0: ActionMode) {
            Timber.d("onDestroyActionMode()")
            clearSelections()
            megaNodeAdapter?.isMultipleSelect = false
            checkScroll()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val selectedNodes = megaNodeAdapter?.selectedNodes ?: emptyList()
            val areAllNotTakenDown = selectedNodes.areAllNotTakenDown()
            var showDownload = false
            var showCopy = false
            val selectAll = menu.findItem(R.id.cab_menu_select_all)
            val unselectAll = menu.findItem(R.id.cab_menu_unselect_all)
            val download = menu.findItem(R.id.cab_menu_download)
            val copy = menu.findItem(R.id.cab_menu_copy)

            if (selectedNodes.isNotEmpty()) {
                selectAll.isVisible = selectedNodes.size != getNodeCount()
                unselectAll.title = getString(R.string.action_unselect_all)
                unselectAll.isVisible = true
                showDownload = areAllNotTakenDown
                showCopy = areAllNotTakenDown
            } else {
                selectAll.isVisible = true
                unselectAll.isVisible = false
            }
            download.isVisible = showDownload
            if (showDownload) {
                download.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
            copy.isVisible = showCopy
            if (showCopy) {
                copy.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
            return false
        }
    }

    /**
     * Establishes the Toolbar
     */
    private fun setupToolbar() {
        (requireActivity() as? ManagerActivity)?.run {
            this.setToolbarTitle()
            this.invalidateOptionsMenu()
        }
    }

    /**
     * Establishes the [MegaNodeAdapter]
     */
    private fun setupAdapter() {
        megaNodeAdapter = MegaNodeAdapter(
            requireActivity(),
            this,
            emptyList(),
            state().inboxHandle,
            binding?.backupsRecyclerView,
            Constants.INBOX_ADAPTER,
            if (state().currentViewType == ViewType.LIST) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
            sortByHeaderViewModel,
        )
        megaNodeAdapter?.isMultipleSelect = false
    }

    /**
     * Establishes the [NewGridRecyclerView]
     */
    private fun setupRecyclerView() {
        binding?.backupsRecyclerView?.let {
            it.itemAnimator = DefaultItemAnimator()
            it.setPadding(0, 0, 0, Util.scaleHeightPx(85, displayMetrics()))
            it.clipToPadding = false
            it.setHasFixedSize(true)
            it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })
            it.adapter = megaNodeAdapter
        }
    }

    /**
     * Observes changes to the UI State from [InboxViewModel]
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect {
                    Timber.d("Node Count from ViewModel is ${it.nodes.size}")
                    handleViewTypeUpdate(it.currentViewType)
                    setNodes(it.nodes.toMutableList())
                    setContent()

                    // Whenever a Node Update occurs, instruct the Fragment to hide the Multiple
                    // Item selection and instruct the ViewModel that it has been handled
                    if (it.hideMultipleItemSelection) {
                        viewModel.hideMultipleItemSelectionHandled()
                        hideMultipleSelect()
                    }

                    // If the user wants to exit the Inbox screen, instruct the ViewModel that
                    // it has been handled, and execute the behavior
                    if (it.shouldExitInbox) {
                        viewModel.exitInboxHandled()
                        (requireActivity() as ManagerActivity).exitInboxScreen()
                    }

                    // Whenever the User performs a Back Press navigation, execute the behavior and
                    // instruct the ViewModel that it has been handled
                    if (it.triggerBackPress) {
                        viewModel.triggerBackPressHandled()
                        onBackPressedHandled()
                    }
                }
            }
        }

        viewLifecycleOwner.collectFlow(viewModel.state.map { it.isPendingRefresh }
            .sample(500L)) { isPendingRefresh ->
            if (isPendingRefresh) {
                viewModel.refreshInboxNodesAndHideSelection()
                viewModel.markHandledPendingRefresh()
            }
        }

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner,
            EventObserver { showSortByPanel() }
        )
        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.refreshInboxNodes()
        })
    }

    /**
     * When receiving a View Type update from [InboxViewModel], this switches the View Type if
     * [megaNodeAdapter] has a different View Type from [InboxViewModel], as changing the View Type
     * will cause the scroll position to be lost
     *
     * @param newViewType The updated [ViewType] from [InboxViewModel]
     */
    private fun handleViewTypeUpdate(newViewType: ViewType) {
        val adapterViewType = megaNodeAdapter?.adapterType ?: MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
        if (adapterViewType != newViewType.id) {
            switchViewType()
        }
    }

    /**
     * Switches how items in the [MegaNodeAdapter] are being displayed, based on the current
     * [ViewType] in [InboxViewModel]
     */
    private fun switchViewType() = binding?.backupsRecyclerView?.run {
        when (state().currentViewType) {
            ViewType.LIST -> {
                switchToLinear()
                if (itemDecorationCount == 0) addItemDecoration(itemDecoration)
                megaNodeAdapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
            }

            ViewType.GRID -> {
                switchBackToGrid()
                removeItemDecoration(itemDecoration)
                (layoutManager as CustomizedGridLayoutManager).apply {
                    spanSizeLookup = megaNodeAdapter?.getSpanSizeLookup(spanCount)
                }
                megaNodeAdapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
            }
        }
    }

    /**
     * Invalidates the [NewGridRecyclerView]
     *
     * This function is used by [ManagerActivity.refreshInboxList]
     */
    fun invalidateRecyclerView() = binding?.backupsRecyclerView?.invalidate()

    /**
     * Selects all items from [MegaNodeAdapter]
     *
     * This function is also used by [ManagerActivity.onOptionsItemSelected]
     */
    fun selectAll() = megaNodeAdapter?.let {
        if (it.isMultipleSelect) {
            it.selectAll()
        } else {
            it.isMultipleSelect = true
            it.selectAll()
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                ActionBarCallBack()
            )
        }
        updateActionModeTitle()
    }

    /**
     * Shows the Sort by panel
     */
    private fun showSortByPanel() =
        (requireActivity() as ManagerActivity).showNewSortByPanel(Constants.ORDER_CLOUD)

    /**
     * Checks the Scrolling Behavior
     *
     * This function is also used by [ManagerActivity.checkScrollElevation]
     */
    fun checkScroll() {
        // Check if the Fragment is added to its Activity before changing the App Bar Elevation
        if (isAdded) {
            binding?.backupsRecyclerView?.let {
                val hasElevation = (it.canScrollVertically(-1) && it.isVisible) ||
                        megaNodeAdapter?.isMultipleSelect == true
                (requireActivity() as ManagerActivity).changeAppBarElevation(hasElevation)
            }
        }
    }

    /**
     * Opens the file
     *
     * @param node The [MegaNode] to be opened
     * @param position The [MegaNode] position
     */
    private fun openFile(node: MegaNode, position: Int) {
        if (MimeTypeList.typeForName(node.name).isImage) {
            val intent = getIntentForParentNode(
                context = requireContext(),
                parentNodeHandle = megaApi.getParentNode(node)?.handle,
                childOrder = viewModel.getOrder(),
                currentNodeHandle = node.handle,
            )
            putThumbnailLocation(
                launchIntent = intent,
                rv = binding?.backupsRecyclerView,
                position = position,
                viewerFrom = Constants.VIEWER_FROM_INBOX,
                thumbnailGetter = megaNodeAdapter
            )
            startActivity(intent)
            (requireActivity() as ManagerActivity).overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isVideoMimeType ||
            MimeTypeList.typeForName(node.name).isAudio
        ) {
            val mimeType = MimeTypeList.typeForName(node.name).type
            val mediaIntent: Intent
            val internalIntent: Boolean
            var opusFile = false

            if (MimeTypeList.typeForName(node.name).isVideoNotSupported || MimeTypeList.typeForName(
                    node.name
                ).isAudioNotSupported
            ) {
                mediaIntent = Intent(Intent.ACTION_VIEW)
                internalIntent = false
                val s = node.name.split("\\.".toRegex()).toTypedArray()
                if (s.size > 1 && s[s.size - 1] == "opus") {
                    opusFile = true
                }
            } else {
                internalIntent = true
                mediaIntent = Util.getMediaIntent(requireActivity(), node.name)
            }

            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_POSITION, position)
            if (megaApi.getParentNode(node)?.type == MegaNode.TYPE_INCOMING) {
                mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, -1L)
            } else {
                mediaIntent.putExtra(
                    Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                    megaApi.getParentNode(node)?.handle
                )
            }
            mediaIntent.putExtra(
                Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                viewModel.getOrder()
            )
            putThumbnailLocation(
                launchIntent = mediaIntent,
                rv = binding?.backupsRecyclerView,
                position = position,
                viewerFrom = Constants.VIEWER_FROM_INBOX,
                thumbnailGetter = megaNodeAdapter
            )

            mediaIntent.putExtra(
                Constants.INTENT_EXTRA_KEY_PLACEHOLDER,
                megaNodeAdapter?.placeholderCount
            )
            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.handle)
            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, node.name)
            mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.INBOX_ADAPTER)

            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    mediaIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            requireActivity(),
                            Constants.AUTHORITY_STRING_FILE_PROVIDER,
                            mediaFile
                        ),
                        MimeTypeList.typeForName(node.name).type
                    )
                } else {
                    mediaIntent.setDataAndType(
                        Uri.fromFile(mediaFile),
                        MimeTypeList.typeForName(node.name).type
                    )
                }
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart()
                    mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                }
                val memoryInfo = ActivityManager.MemoryInfo()
                val activityManager =
                    requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(memoryInfo)
                if (memoryInfo.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("Total memory: %d allocate 32 MB", memoryInfo.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("Total memory: %d allocate 16 MB", memoryInfo.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi.httpServerGetLocalLink(node)
                mediaIntent.setDataAndType(Uri.parse(url), mimeType)
            }
            if (opusFile) {
                mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
            }
            if (internalIntent) {
                startActivity(mediaIntent)
            } else {
                if (MegaApiUtils.isIntentAvailable(requireActivity(), mediaIntent)) {
                    startActivity(mediaIntent)
                } else {
                    (requireActivity() as ManagerActivity).showSnackbar(
                        type = Constants.SNACKBAR_TYPE,
                        content = getString(R.string.intent_not_available),
                        chatId = -1,
                    )
                    megaNodeAdapter?.notifyDataSetChanged()
                    (requireActivity() as ManagerActivity).saveNodesToDevice(
                        nodes = listOf(node),
                        highPriority = true,
                        isFolderLink = false,
                        fromMediaViewer = false,
                        fromChat = false,
                    )
                }
            }
            (requireActivity() as ManagerActivity).overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isPdf) {
            val mimeType = MimeTypeList.typeForName(node.name).type
            val pdfIntent = Intent(requireActivity(), PdfViewerActivity::class.java)
            pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
            pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.INBOX_ADAPTER)
            val localPath = FileUtil.getLocalFile(node)
            if (localPath != null) {
                val mediaFile = File(localPath)
                if (localPath.contains(Environment.getExternalStorageDirectory().path)) {
                    pdfIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            requireActivity(),
                            Constants.AUTHORITY_STRING_FILE_PROVIDER,
                            mediaFile
                        ),
                        MimeTypeList.typeForName(
                            node.name
                        ).type
                    )
                } else {
                    pdfIntent.setDataAndType(
                        Uri.fromFile(mediaFile), MimeTypeList.typeForName(
                            node.name
                        ).type
                    )
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart()
                    pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                }
                val mi = ActivityManager.MemoryInfo()
                val activityManager =
                    requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(mi)
                if (mi.totalMem > Constants.BUFFER_COMP) {
                    Timber.d("Total memory: %d allocate 32 MB", mi.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                } else {
                    Timber.d("Total memory: %d allocate 16 MB", mi.totalMem)
                    megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                }
                val url = megaApi.httpServerGetLocalLink(node)
                pdfIntent.setDataAndType(Uri.parse(url), mimeType)
            }
            pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.handle)
            putThumbnailLocation(
                launchIntent = pdfIntent,
                rv = binding?.backupsRecyclerView,
                position = position,
                viewerFrom = Constants.VIEWER_FROM_INBOX,
                thumbnailGetter = megaNodeAdapter,
            )
            if (MegaApiUtils.isIntentAvailable(requireActivity(), pdfIntent)) {
                startActivity(pdfIntent)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.intent_not_available),
                    Toast.LENGTH_LONG
                ).show()
                (requireActivity() as ManagerActivity).saveNodesToDevice(
                    nodes = listOf(node),
                    highPriority = true,
                    isFolderLink = false,
                    fromMediaViewer = false,
                    fromChat = false,
                )
            }
            (requireActivity() as ManagerActivity).overridePendingTransition(0, 0)
        } else if (MimeTypeList.typeForName(node.name).isURL) {
            manageURLNode(requireActivity(), megaApi, node)
        } else if (MimeTypeList.typeForName(node.name).isOpenableTextFile(node.size)) {
            manageTextFileIntent(requireContext(), node, Constants.INBOX_ADAPTER)
        } else {
            megaNodeAdapter?.notifyDataSetChanged()
            onNodeTapped(
                context = requireActivity(),
                node = node,
                nodeDownloader = { nodeToDownload: MegaNode ->
                    (requireActivity() as ManagerActivity).saveNodeByTap(nodeToDownload)
                },
                activityLauncher = (requireActivity() as ManagerActivity),
                snackbarShower = (requireActivity() as ManagerActivity),
            )
        }
    }

    /**
     * When a Node from [MegaNodeAdapter] is selected, handle the behavior here
     *
     * @param nodePosition The selected Node position
     */
    fun onNodeSelected(nodePosition: Int) {
        Timber.d("itemClick()")
        // Perform the following actions when Multi Select is enabled
        if (megaNodeAdapter?.isMultipleSelect == true) {
            Timber.d("Multi Select is Enabled")
            megaNodeAdapter?.toggleSelection(nodePosition)
            val selectedNodes = megaNodeAdapter?.selectedNodes ?: emptyList()
            if (selectedNodes.isNotEmpty()) updateActionModeTitle()
        } else {
            megaNodeAdapter?.getItem(nodePosition)?.let { selectedNode ->
                // When the selected Node is a Folder, perform the following actions
                if (selectedNode.isFolder) {
                    // Update the last position stack
                    pushLastPositionStack()

                    // Update to the new Inbox Handle in the ViewModel and update the list of Inbox Nodes
                    with(viewModel) {
                        updateInboxHandle(selectedNode.handle)
                        refreshInboxNodes()
                    }
                    // Notify ManagerActivity to invalidate the Options Menu and set the new Toolbar Title
                    with(requireActivity() as ManagerActivity) {
                        invalidateOptionsMenu()
                        setToolbarTitle()
                    }

                    // Update the RecyclerView scrolling behavior
                    binding?.backupsRecyclerView?.scrollToPosition(0)
                    checkScroll()
                } else {
                    // For non-Folder typed Nodes, simply open the file
                    openFile(selectedNode, nodePosition)
                }
            }
        }
    }

    /**
     * When a Folder-type Node is selected, push the last position stack in order
     * to add one level in the Node navigation hierarchy
     */
    private fun pushLastPositionStack() {
        var lastFirstVisiblePosition =
            binding?.backupsRecyclerView?.findFirstCompletelyVisibleItemPosition()
                ?: RecyclerView.NO_POSITION
        if (state().currentViewType == ViewType.GRID && lastFirstVisiblePosition == -1) {
            Timber.d("Completely -1 then find just visible position")
            lastFirstVisiblePosition = binding?.backupsRecyclerView?.findFirstVisibleItemPosition()
                ?: RecyclerView.NO_POSITION
        }
        Timber.d("Push to stack %d position", lastFirstVisiblePosition)
        lastPositionStack?.push(lastFirstVisiblePosition)
    }

    /**
     * Clear all selected items
     */
    private fun clearSelections() =
        megaNodeAdapter?.let { if (it.isMultipleSelect) it.clearSelections() }

    /**
     * Hides the Multiple Selection option
     *
     * This function is also used by [ManagerActivity.onNodesInboxUpdate] and [MegaNodeAdapter.hideMultipleSelect]
     */
    fun hideMultipleSelect() {
        Timber.d("hideMultipleSelect()")
        megaNodeAdapter?.let { it.isMultipleSelect = false }
        actionMode?.finish()
    }

    /**
     * onBackPressed behavior that has reference to [ManagerActivity]
     */
    fun onBackPressed() {
        Timber.d("onBackPressed()")

        with(requireActivity() as ManagerActivity) {
            if (megaNodeAdapter == null) {
                // Call the method from ManagerActivity to move back to the previous Drawer Item
                exitInboxScreen()
            } else if (comesFromNotifications && comesFromNotificationHandle == state().inboxHandle) {
                // Handle behavior if the Inbox is accessed through a Notification
                comesFromNotifications = false
                comesFromNotificationHandle = -1
                selectDrawerItem(DrawerItem.NOTIFICATIONS)
                this@InboxFragment.viewModel.updateInboxHandle(comesFromNotificationHandle)
                comesFromNotificationHandleSaved = -1
            } else {
                // Otherwise, instruct the ViewModel to handle the Back Press
                this@InboxFragment.viewModel.handleBackPress()
            }
        }
    }

    /**
     * Executes certain behavior when a Back Press is handled
     */
    private fun onBackPressedHandled() {
        // Notify ManagerActivity to invalidate the Options Menu and set the new Toolbar Title
        with(requireActivity() as ManagerActivity) {
            invalidateOptionsMenu()
            setToolbarTitle()
        }

        // Pop the last position stack
        popLastPositionStack()
    }

    /**
     * When a Back Press is handled, pop the last position stack in order to subtract one level
     * in the Node navigation hierarchy
     */
    private fun popLastPositionStack() {
        var lastVisiblePosition = 0

        lastPositionStack?.let {
            if (it.isNotEmpty()) {
                lastVisiblePosition = it.pop()
                Timber.d("Moved to new position $lastVisiblePosition after popping the stack")
            }
        }

        Timber.d("Scroll to position $lastVisiblePosition")
        if (lastVisiblePosition >= 0) {
            binding?.backupsRecyclerView?.scrollToPosition(lastVisiblePosition)
        }
    }

    /**
     * Sets the list of Nodes to [MegaNodeAdapter]
     *
     * @param nodes The list of Nodes to display. A [MutableList] is needed as
     * [MegaNodeAdapter.setNodes] is written in Java
     */
    private fun setNodes(nodes: MutableList<MegaNode>) {
        Timber.d("Call setNodes() with Node Size ${nodes.size}")
        megaNodeAdapter?.setNodes(nodes)
    }

    /**
     * Returns the total number of Nodes from [MegaNodeAdapter]
     *
     * This function is also used by [ManagerActivity.onCreateOptionsMenu]
     *
     * @return the total number or Nodes, or 0 if [MegaNodeAdapter] is null
     */
    fun getNodeCount(): Int = megaNodeAdapter?.itemCount ?: 0

    /**
     * Sets all content of the feature.
     *
     * If no nodes are available, empty folder information will be displayed. Otherwise, it will
     * display all available nodes.
     */
    private fun setContent() {
        Timber.d("setContent()")
        binding?.run {
            if (getNodeCount() == 0) {
                backupsRecyclerView.visibility = View.GONE
                backupsNoItemsGroup.visibility = View.VISIBLE
                if (viewModel.isCurrentlyOnBackupFolderLevel()) {
                    setEmptyFolderTextContent(
                        title = getString(R.string.backups_empty_state_title),
                        description = getString(R.string.backups_empty_state_body)
                    )
                    backupsNoItemsImageView.setImageResource(
                        if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            R.drawable.ic_zero_landscape_empty_folder
                        } else {
                            R.drawable.ic_zero_portrait_empty_folder
                        }
                    )
                } else {
                    setEmptyFolderTextContent(
                        title = getString(R.string.file_browser_empty_folder_new),
                        description = "",
                    )
                    backupsNoItemsImageView.setImageResource(
                        if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            R.drawable.empty_folder_landscape
                        } else {
                            R.drawable.empty_folder_portrait
                        }
                    )

                }
            } else {
                backupsRecyclerView.visibility = View.VISIBLE
                backupsNoItemsGroup.visibility = View.GONE
            }
        }
    }

    /**
     * Sets the title and description text when the folder is empty
     * Null checking exists for the description since this only exists on the list configuration
     * of the feature
     *
     * @param title Empty folder title
     * @param description Empty folder description
     */
    private fun setEmptyFolderTextContent(title: String, description: String) {
        binding?.run {
            backupsNoItemsTitleTextView.text = formatEmptyFolderTitleString(title)
            backupsNoItemsDescriptionTextView.text = description
        }
    }

    /**
     * Formats a String through a specified color formatting, which is then used for the title
     * message when the folder is empty
     *
     * @param title The title to be formatted
     * @return The [Spanned] title to be immediately used by the [TextView]
     */
    private fun formatEmptyFolderTitleString(title: String): Spanned {
        var textToFormat = title

        try {
            textToFormat = textToFormat.replace(
                "[A]", "<font color='"
                        + getColorHexString(requireContext(), R.color.grey_900_grey_100)
                        + "'>"
            ).replace("[/A]", "</font>").replace(
                "[B]", "<font color='"
                        + getColorHexString(requireContext(), R.color.grey_300_grey_600)
                        + "'>"
            ).replace("[/B]", "</font>")
        } catch (exception: Exception) {
            exception.printStackTrace()
        }

        return HtmlCompat.fromHtml(textToFormat, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    companion object {
        /**
         * Creates a new instance of [InboxFragment]
         */
        fun newInstance(): InboxFragment {
            Timber.d("newInstance()")
            return InboxFragment()
        }
    }
}