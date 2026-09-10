package tech.p1neapplexpress.mprox.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BackendStatus(
    val isRunning: Boolean = false,
    val pid: Long? = null,
    val output: String = "",
    val error: String = "",
    val exitCode: Int? = null
)