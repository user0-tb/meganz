package mega.privacy.android.app.presentation.clouddrive

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.presentation.clouddrive.model.FileBrowserState
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * ViewModel associated to FileBrowserFragment
 *
 * @param getRootFolder Fetch the root node
 * @param getBrowserChildrenNode Fetch the cloud drive nodes
 * @param monitorMediaDiscoveryView Monitor media discovery view settings
 * @param monitorNodeUpdates Monitor node updates
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val getRootFolder: GetRootFolder,
    private val getBrowserChildrenNode: GetBrowserChildrenNode,
    monitorMediaDiscoveryView: MonitorMediaDiscoveryView,
    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    private val _state = MutableStateFlow(FileBrowserState())

    /**
     * State flow
     */
    val state: StateFlow<FileBrowserState> = _state

    init {
        viewModelScope.launch {
            monitorMediaDiscoveryView().collect { mediaDiscoveryViewSettings ->
                _state.update {
                    it.copy(
                        mediaDiscoveryViewSettings = mediaDiscoveryViewSettings
                            ?: MediaDiscoveryViewSettings.INITIAL.ordinal
                    )
                }
            }
        }
    }

    /**
     * Update Browser Nodes when a node update callback happens
     */
    val updateBrowserNodes: LiveData<Event<List<MegaNode>>> =
        monitorNodeUpdates()
            .mapNotNull { getBrowserChildrenNode(_state.value.fileBrowserHandle) }
            .map { Event(it) }
            .asLiveData()

    /**
     * Set the current browser handle to the UI state
     *
     * @param handle the id of the current browser handle to set
     */
    fun setBrowserParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(fileBrowserHandle = handle) }
    }

    /**
     * Get the browser parent handle
     * If not previously set, set the browser parent handle to root handle
     *
     * @return the handle of the browser section
     */
    fun getSafeBrowserParentHandle(): Long = runBlocking {
        if (_state.value.fileBrowserHandle == -1L) {
            setBrowserParentHandle(getRootFolder()?.handle ?: MegaApiJava.INVALID_HANDLE)
        }
        return@runBlocking _state.value.fileBrowserHandle
    }

    /**
     * If a folder only contains images or videos, then go to MD mode directly
     */
    fun shouldEnterMDMode(nodes: List<MegaNode>, mediaDiscoveryViewSettings: Int): Boolean {
        if (nodes.isEmpty())
            return false
        val isMediaDiscoveryEnable =
            mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.ENABLED.ordinal ||
                    mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.INITIAL.ordinal
        if (!isMediaDiscoveryEnable)
            return false

        for (node: MegaNode in nodes) {
            if (node.isFolder ||
                !MimeTypeList.typeForName(node.name).isImage &&
                !MimeTypeList.typeForName(node.name).isVideoReproducible
            ) {
                return false
            }
        }
        return true
    }
}
