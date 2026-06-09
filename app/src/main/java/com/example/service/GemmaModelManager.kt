package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow

class GemmaModelManager {

    val downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotDownloaded)

    fun isModelAvailable(): Boolean {
        // In the prototype we simulate the model availability. 
        // We consider it available if the user triggered the download.
        return downloadState.value is ModelDownloadState.Ready
    }

    fun downloadModel() {
        downloadState.value = ModelDownloadState.Downloading(0)
        // Simulate download
        Thread {
            for (i in 1..100) {
                Thread.sleep(20)
                downloadState.value = ModelDownloadState.Downloading(i)
            }
            downloadState.value = ModelDownloadState.Ready
        }.start()
    }
}

sealed class ModelDownloadState {
    object NotDownloaded : ModelDownloadState()
    data class Downloading(val progressPercent: Int) : ModelDownloadState()
    object Ready : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}
