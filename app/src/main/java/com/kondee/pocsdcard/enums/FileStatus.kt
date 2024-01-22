package com.kondee.pocsdcard.enums

//enum class FileStatus(val status: String) {
//    Idle("No File!"),
//    Downloading("Downloading..."),
//    Downloaded("Downloaded!"),
//    Moving("Moving >>>"),
//    Error("Error!"),
//}

sealed class FileStatus(open val status: String) {

    data object Idle : FileStatus("No File!")
    data class Downloading(val progress: Int = 0) : FileStatus("Downloading...")
    data class Downloaded(val fileLocation: String = "-") : FileStatus("Downloaded!")
    data object Moving : FileStatus("Moving >>>")
    data class Error(val e: Throwable) : FileStatus("Error!")
}