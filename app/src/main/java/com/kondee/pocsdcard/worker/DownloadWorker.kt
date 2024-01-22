package com.kondee.pocsdcard.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kondee.pocsdcard.enums.DownloadLocation
import com.kondee.pocsdcard.utils.Constant
import com.kondee.pocsdcard.utils.DataStore
import com.kondee.pocsdcard.utils.DataStore.downloadLocationDataStore
import com.kondee.pocsdcard.utils.Utils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL


class DownloadWorker(
    appContext: Context, params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val downloadLocationDataStore = applicationContext.downloadLocationDataStore

    override suspend fun doWork(): Result {
        try {

            val urlString = inputData.getString(KEY_URL) ?: return Result.failure()

            val downloadLocation = downloadLocationDataStore.data.map {
                val index = it[DataStore.downloadLocationPreferenceKey] ?: 0
                DownloadLocation.entries[index]
            }.first()

            val rootDirectory = when (downloadLocation) {
                DownloadLocation.Internal -> {
                    applicationContext.filesDir
                }
                DownloadLocation.SDCard -> {
                    Utils.sdCardDirectory(applicationContext)
                }
            }

            val folder = File(rootDirectory, "/user_${Constant.USER_ID}/${Constant.BOOK_ID}")
            if (!folder.exists()) {
                folder.mkdirs()
            }

            val file = File(folder, "file_1.mp4")
            if (!file.exists()) {
                file.createNewFile()
            }

            return downloadFile(urlString, file)

//            val url = URL(urlString)
//
////            val connection = url.openConnection()
////            connection.connect()
////            val size = connection.contentLength
//
//
//            Log.i("Kondee", "size: ${getFileSize(url)}")
////            url.openStream().use { input ->
////                file.outputStream().use { output ->
////                    input.copyTo(output) { bytesCopied ->
////                        val progress = (bytesCopied.toDouble() / size.toDouble() * 100.0).toInt()
////                        setProgress(progress)
////                    }
////                }
////            }

//            setProgress(100)

//            return Result.success(
//                workDataOf(KEY_FILE_LOCATION to file.absolutePath)
//            )
        } catch (e: Exception) {
            return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to e.message)
            )
        }
    }

    private suspend fun downloadFile(url: String, file: File): Result {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
//        val file: File
        try {
//            file = File(downloadPath)
//            if (!file.mkdirs()) {
//                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Can not create folder."))
//            }

            val client = OkHttpClient()
            val request: Request = Request.Builder().url(url).build()
            val response: Response = client.newCall(request).execute()
            inputStream = response.body?.byteStream()
                ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Can not download."))
            val contentLength = response.body?.contentLength() ?: 0

            Log.i("Kondee", "contentLength: $contentLength")

            outputStream = FileOutputStream(file)
            val buffer = ByteArray(2 * 1024)
            var len: Int
            var readLen = 0
            while (true) {
                len = inputStream.read(buffer)
                if (len == -1) {
                    break
                }

                outputStream.write(buffer, 0, len)
                readLen += len

                /**
                 * Use this function to set progress.
                 */
                val progress = (readLen.toFloat() / contentLength.toFloat() * 100f).toInt()
                setProgress(progress)
            }
        } catch (e: IOException) {
            return Result.failure(workDataOf("Error" to e.message))
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return Result.success(
            workDataOf(
                KEY_PROGRESS to 100f,
                KEY_FILE_LOCATION to file.absolutePath,
            )
        )
    }

    private suspend fun setProgress(progress: Int) {
        setProgress(
            workDataOf(KEY_PROGRESS to progress)
        )
    }

    private suspend fun InputStream.copyTo(
        out: OutputStream,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        callback: suspend (bytesCopied: Long) -> Unit
    ): Long {
        var bytesCopied: Long = 0
        val buffer = ByteArray(bufferSize)
        var bytes = read(buffer)
        while (bytes >= 0) {
            out.write(buffer, 0, bytes)
            bytesCopied += bytes
            callback(bytesCopied)
            bytes = read(buffer)
        }
        return bytesCopied
    }

    companion object {

        private const val KEY_URL = "KEY_URL"

        const val KEY_PROGRESS = "KEY_PROGRESS"
        const val KEY_FILE_LOCATION = "KEY_FILE_LOCATION"
        const val KEY_ERROR_MESSAGE = "KEY_ERROR_MESSAGE"

        fun getWorkRequest(url: String): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    workDataOf(KEY_URL to url)
                )
                .addTag(url)
                .build()
        }
    }
}