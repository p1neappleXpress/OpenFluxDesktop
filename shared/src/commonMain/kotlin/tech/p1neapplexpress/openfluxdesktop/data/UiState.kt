package tech.p1neapplexpress.openfluxdesktop.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


object UiState {
    var mode by mutableStateOf("client")
    var url by mutableStateOf("")
    var maxToken by mutableStateOf("")
    var maxUid by mutableStateOf("")
    var socksAddr by mutableStateOf(":1080")
    var exitMode by mutableStateOf("proxy")
    var localIp by mutableStateOf("")
    var debug by mutableStateOf(true)
    var lastTransport: String by mutableStateOf("")
}
