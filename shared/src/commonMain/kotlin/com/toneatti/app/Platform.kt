package com.toneatti.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform