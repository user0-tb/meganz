package mega.privacy.android.app.zippreview.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.app.middlelayer.reporter.CrashReporter
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.zippreview.domain.FileType
import mega.privacy.android.app.zippreview.domain.ZipFileRepository
import mega.privacy.android.app.zippreview.domain.ZipTreeNode
import mega.privacy.android.app.zippreview.ui.ZipInfoUIO
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * ViewModel regarding to zip preview
 * @param zipFileRepository ZipFileRepo
 */
@HiltViewModel
class ZipBrowserViewModel @Inject constructor(
    private val zipFileRepository: ZipFileRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {
    companion object {
        private const val TITLE_ZIP = "ZIP "
        private const val SUFFIX_ZIP = ".zip"
    }

    private lateinit var zipFullPath: String
    private lateinit var unzipRootPath: String

    private lateinit var zipFile: ZipFile

    private lateinit var rootFolderPath: String

    private var currentZipInfo: ZipInfoUIO? = null

    private var _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private var _zipInfoList = MutableLiveData<List<ZipInfoUIO>>()
    val zipInfoList: LiveData<List<ZipInfoUIO>>
        get() = _zipInfoList

    private var _showProgressDialog = MutableLiveData<Boolean>()
    val showProgressDialog: LiveData<Boolean>
        get() = _showProgressDialog

    private var _showAlert = MutableLiveData<Boolean>()
    val showAlert: LiveData<Boolean>
        get() = _showAlert

    private var _openFile = MutableLiveData<Pair<Int, ZipInfoUIO>>()
    val openFile: LiveData<Pair<Int, ZipInfoUIO>>
        get() = _openFile

    private val _deepOfFolder = MutableStateFlow(0)

    /**
     * enable back pressed handler, true when deep > 1 otherwise false
     */
    val enableBackPressedHandler = _deepOfFolder.asStateFlow().map { deep -> deep > 1 }

    /**
     * The type of clicked item
     */
    enum class StatusItemClicked {
        OPEN_FOLDER, ZIP_NOT_UNPACK, ITEM_NOT_EXIST, OPEN_FILE
    }

    /**
     * Update zip info list
     * @param folderPath the path of folder, default value is ""
     */
    private fun updateZipInfoList(context: Context, folderPath: String = "") {
        _zipInfoList.value =
            zipFileRepository.updateZipInfoList(zipFile, folderPath).map {
                zipTreeNodeToZipInfoUIO(it, context)
            }

        getTitle(folderPath.ifEmpty { "" })
        _deepOfFolder.value = _deepOfFolder.value.inc()
    }

    /**
     * Convert ZipTreeNode to ZipInfoUIO
     * @param zipTreeNode ZipTreeNode
     * @return ZipInfoUIO
     */
    private fun zipTreeNodeToZipInfoUIO(zipTreeNode: ZipTreeNode, context: Context): ZipInfoUIO {
        val imageResourceId = if (zipTreeNode.fileType == FileType.FOLDER) {
            CoreUiR.drawable.ic_folder_list
        } else {
            MimeTypeList.typeForName(zipTreeNode.path).iconResourceId
        }
        return ZipInfoUIO(
            zipTreeNode.name,
            if (zipTreeNode.fileType == FileType.FOLDER) {
                "${zipTreeNode.path}${File.separator}"
            } else {
                zipTreeNode.path
            },
            zipTreeNode.parent,
            if (zipTreeNode.fileType == FileType.FOLDER) {
                val result = countFiles(zipTreeNode)
                TextUtil.getFolderInfo(result.first, result.second, context)
            } else
                Util.getSizeString(zipTreeNode.size, context),
            imageResourceId,
            zipTreeNode.fileType
        )
    }


    /**
     * Count the files number of current folder
     * @return Pair of folder numbers and file numbers. First is folder number.
     */
    private fun countFiles(zipTreeNode: ZipTreeNode): Pair<Int, Int> {
        var counter = Pair(0, 0)
        zipTreeNode.children.forEach { child ->
            counter = if (child.fileType == FileType.FOLDER) {
                counter.copy(first = counter.first + 1)
            } else {
                counter.copy(second = counter.second + 1)
            }
        }
        return counter
    }

    /**
     * Init ViewModel and open current zip file.
     * @param zipFullPath zip file full path
     * @param unzipRootPath unzip root path
     */
    fun viewModelInit(zipFullPath: String, unzipRootPath: String, context: Context) {
        this.zipFullPath = zipFullPath
        this.unzipRootPath = "${unzipRootPath}${File.separator}"
        // Log the zip file path
        crashReporter.log("Path of ZipFile(viewModelInit) is $zipFullPath")
        try {
            zipFile = try {
                // Construct ZipFile with UTF-8
                val zipFile = ZipFile(zipFullPath)
                // Try reading the Zip File with UTF-8 Charset
                zipFile.entries().toList()
                zipFile
            } catch (e: Exception) {
                // Throws IllegalArgumentException (thrown when malformed) / ZipException (thrown when unsupported format)
                // Fallback if zip cannot be read with UTF-8 Charset, then switch to CP-437 (Default for Most Windows Zip Software)
                // i.e: 7-Zip, PeaZip, Winrar, Winzip
                ZipFile(zipFullPath, Charset.forName("Cp437"))
            }
            rootFolderPath = unzipRootPath.split("/").last()
            viewModelScope.launch {
                zipFileRepository.initZipTreeNode(zipFile)
                updateZipInfoList(context)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Validation current folder if the root folder when the back button clicked.
     */
    fun handleOnBackPressed(context: Context) {
        if (zipInfoList.value.isNullOrEmpty()) {
            currentZipInfo?.let {
                backUpdateZipInfoList(it.path, true, context)
            }
        } else {
            _zipInfoList.value?.get(0)?.path?.apply {
                backUpdateZipInfoList(this, false, context)
            }
        }
        _deepOfFolder.value = _deepOfFolder.value.dec()
    }

    /**
     * Update zip info list when the back button is clicked
     * @param parentFolderPath parent folder path
     * @param isEmptyFolder current folder whether is empty folder
     */
    private fun backUpdateZipInfoList(
        parentFolderPath: String,
        isEmptyFolder: Boolean,
        context: Context,
    ) {
        _zipInfoList.value =
            zipFileRepository.getParentZipInfoList(parentFolderPath, isEmptyFolder).map {
                zipTreeNodeToZipInfoUIO(it, context)
            }.also {
                val firstNodeParent = it.firstOrNull()?.parent
                getTitle(if (firstNodeParent.isNullOrEmpty()) "" else firstNodeParent)
            }
    }

    /**
     * Behaviours according the status of clicked file
     * @param zipInfoUIO ZipInfoUIO
     * @param position the position of clicked file
     */
    fun onZipFileClicked(zipInfoUIO: ZipInfoUIO, position: Int, context: Context) {
        when (getItemClickedStatus(zipInfoUIO, unzipRootPath)) {
            StatusItemClicked.ZIP_NOT_UNPACK -> {
                _showProgressDialog.value = true
                //If zip folder doesn't exist, unpacked the zip file.
                unpackedZipFile(zipInfoUIO, position)
            }

            StatusItemClicked.OPEN_FILE -> {
                //If the zip file name is start with ".", it cannot be unzip. So show the alert.
                if (zipInfoUIO.fileType == FileType.ZIP && zipInfoUIO.name.startsWith(".")) {
                    Timber.e("zip file ${zipInfoUIO.name} start with \".\" cannot unzip")
                    _showAlert.value = true
                } else {
                    _openFile.value = Pair(position, zipInfoUIO)
                }

            }

            StatusItemClicked.OPEN_FOLDER -> {
                currentZipInfo = zipInfoUIO
                updateZipInfoList(context, zipInfoUIO.path)
            }

            StatusItemClicked.ITEM_NOT_EXIST -> {
                Timber.e("zip entry position $position file not exists")
                _showAlert.value = true
            }
        }
    }

    /**
     * Unpack zip file and open the current clicked file.
     * @param zipInfoUIO ZipInfoUIO of clicked file
     * @param position position of clicked file
     */
    private fun unpackedZipFile(zipInfoUIO: ZipInfoUIO, position: Int) {
        viewModelScope.launch {
            zipFileRepository.unzipFile(zipFullPath, unzipRootPath)
            _showProgressDialog.value = false
            _openFile.value = Pair(position, zipInfoUIO)
        }
    }

    /**
     * Get title of actionbar
     * @param folderPath current folder path
     */
    private fun getTitle(folderPath: String) {
        //If the folder is zip root directory, title is zip filename. If not, title is folder name
        _title.value = if (folderPath.isEmpty()) {
            "${TITLE_ZIP}${zipFullPath.split("/").lastOrNull()?.removeSuffix(SUFFIX_ZIP)}"
        } else {
            File(folderPath).name
        }
    }

    /**
     * Get file status of clicked file
     * @param zipInfoUIO ZipInfoUIO of clicked file
     * @param rootPath unzip root path
     * @return TypeItemClickResult
     */
    private fun getItemClickedStatus(zipInfoUIO: ZipInfoUIO, rootPath: String): StatusItemClicked {
        return if (zipInfoUIO.fileType == FileType.FOLDER) {
            StatusItemClicked.OPEN_FOLDER
        } else {
            val currentFile = File(rootPath + zipInfoUIO.path)
            if (File(rootPath).exists()) {
                if (currentFile.exists()) {

                    StatusItemClicked.OPEN_FILE
                } else {
                    StatusItemClicked.ITEM_NOT_EXIST
                }
            } else {
                StatusItemClicked.ZIP_NOT_UNPACK
            }
        }
    }
}