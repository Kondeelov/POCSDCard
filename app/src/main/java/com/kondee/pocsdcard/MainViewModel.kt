package com.kondee.pocsdcard

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kondee.pocsdcard.utils.DataStore.downloadLocationDataStore
import com.kondee.pocsdcard.enums.DownloadLocation
import com.kondee.pocsdcard.enums.FileStatus
import com.kondee.pocsdcard.utils.Constant
import com.kondee.pocsdcard.utils.DataStore
import com.kondee.pocsdcard.utils.Utils
import com.kondee.pocsdcard.worker.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(
    private val application: Application
) : AndroidViewModel(application) {

    private val _sdCardAvailableFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val sdCardAvailableFlow: StateFlow<Boolean> = _sdCardAvailableFlow.asStateFlow()

    private val _fileStatusFlow: MutableStateFlow<FileStatus> = MutableStateFlow(FileStatus.Idle)
    val fileStatusFlow: StateFlow<FileStatus> = _fileStatusFlow.asStateFlow()

    private val downloadLocationDataStore = application.applicationContext.downloadLocationDataStore
    val downloadLocationFlow = downloadLocationDataStore.data.map {
        val index = it[DataStore.downloadLocationPreferenceKey] ?: 0
        DownloadLocation.entries[index]
    }

    fun getSDCardStatus() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sdCardDirectory = Utils.sdCardDirectory(application.applicationContext)
                _sdCardAvailableFlow.emit(sdCardDirectory != null)
            }
        }
    }

    fun handleDownloadStates(workInfos: List<WorkInfo>) {

        viewModelScope.launch {
            workInfos.forEach { info ->
                when (info.state) {
                    WorkInfo.State.ENQUEUED -> {

                    }
                    WorkInfo.State.RUNNING -> {
                        val progress = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                        _fileStatusFlow.emit(FileStatus.Downloading(progress))
                    }
                    WorkInfo.State.SUCCEEDED -> {

                        checkFileStatus()

                        WorkManager.getInstance(application.applicationContext).pruneWork()
                    }
                    WorkInfo.State.FAILED -> {
                        val errorMessage =
                            info.outputData.getString(DownloadWorker.KEY_ERROR_MESSAGE)
                        _fileStatusFlow.emit(FileStatus.Error(Throwable(errorMessage)))

                        WorkManager.getInstance(application.applicationContext).pruneWork()
                    }
                    WorkInfo.State.BLOCKED -> {

                    }
                    WorkInfo.State.CANCELLED -> {
                        val errorMessage =
                            info.outputData.getString(DownloadWorker.KEY_ERROR_MESSAGE)
                        _fileStatusFlow.emit(FileStatus.Error(Throwable(errorMessage)))

                        WorkManager.getInstance(application.applicationContext).pruneWork()
                    }
                }
            }
        }
    }

    fun setDownloadLocation(location: DownloadLocation, shouldMoveFiles: Boolean = false) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                val sourceRoot: File?
                val targetRoot: File?
                when (location) {
                    DownloadLocation.Internal -> {
                        sourceRoot = Utils.sdCardDirectory(application.applicationContext)
                        targetRoot = application.applicationContext.filesDir
                    }
                    DownloadLocation.SDCard -> {
                        sourceRoot = application.applicationContext.filesDir
                        targetRoot = Utils.sdCardDirectory(application.applicationContext)
                    }
                }

                val sourceDirectory = File(sourceRoot, "/user_${Constant.USER_ID}/${Constant.BOOK_ID}")
                val targetDirectory = File(targetRoot, "/user_${Constant.USER_ID}/${Constant.BOOK_ID}")

                if (shouldMoveFiles) {
                    _fileStatusFlow.emit(FileStatus.Moving)
                    if (sourceDirectory.exists()) {
                        sourceDirectory.copyRecursively(targetDirectory, overwrite = true)
                    }
                }
                sourceDirectory.deleteRecursively()

                downloadLocationDataStore.edit { prefs ->
                    prefs[DataStore.downloadLocationPreferenceKey] = location.ordinal
                }

                checkFileStatus()
            }
        }
    }

    fun deleteFile() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val rootDirectory = when (downloadLocationFlow.first()) {
                    DownloadLocation.Internal -> {
                        application.applicationContext.filesDir
                    }
                    DownloadLocation.SDCard -> {
                        Utils.sdCardDirectory(application.applicationContext)
                    }
                }

                val folder = File(rootDirectory, "/user_${Constant.USER_ID}/${Constant.BOOK_ID}")
                if (folder.exists()) {
                    folder.deleteRecursively()
                }

                checkFileStatus()
            }
        }
    }

    fun checkFileStatus() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                val rootDirectory = when (downloadLocationFlow.first()) {
                    DownloadLocation.Internal -> {
                        application.applicationContext.filesDir
                    }
                    DownloadLocation.SDCard -> {
                        Utils.sdCardDirectory(application.applicationContext)
                    }
                }

                val folder = File(rootDirectory, "/user_${Constant.USER_ID}/${Constant.BOOK_ID}")
                val file = File(folder, "file_1.mp4")

                if (file.exists()) {
                    _fileStatusFlow.emit(FileStatus.Downloaded(file.absolutePath))
                } else {
                    _fileStatusFlow.emit(FileStatus.Idle)
                }
            }
        }
    }

    init {
        getSDCardStatus()
        checkFileStatus()
    }
}