package tech.p1neapplexpress.openfluxdesktop.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Collections.emptyList

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0L,
)