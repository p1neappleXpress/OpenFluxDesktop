package tech.p1neapplexpress.openfluxdesktop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform