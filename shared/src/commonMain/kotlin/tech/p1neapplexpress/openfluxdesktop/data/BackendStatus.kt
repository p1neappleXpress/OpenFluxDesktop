package tech.p1neapplexpress.openfluxdesktop.data


data class BackendStatus(
    val isRunning: Boolean = false,
    val isConnected: Boolean = false,
    val pid: Long? = null,
    val output: String = "",
    val error: String = "",
    val exitCode: Int? = null,
    val upKbps: Float = 0f,
    val downKbps: Float = 0f,
    val pktUp: Int = 0,
    val pktDown: Int = 0,
    val uptimeSec: Long = 0,
    val exitNodePayload: String? = null,
)
