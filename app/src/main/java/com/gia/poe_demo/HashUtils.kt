package com.gia.poe_demo

import java.security.MessageDigest

// utility object for hashing passwords using MD5
// ref: https://developer.android.com/reference/java/security/MessageDigest
object HashUtils {

    // hashes a plain text password using MD5 and returns the hex string
    // MD5 produces a 128-bit hash represented as a 32 character hex string
    // ref: https://developer.android.com/reference/java/security/MessageDigest
    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/*
References:

Android Developers, 2024. MessageDigest.
Available at: https://developer.android.com/reference/java/security/MessageDigest
[Accessed 22 April 2026].

Oracle, 2024. MessageDigest (Java SE 11).
Available at: https://docs.oracle.com/en/java/api/java.base/java/security/MessageDigest.html
[Accessed 24 April 2026].

Kotlin, 2024. joinToString.
Available at: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/join-to-string.html
[Accessed 20 April 2026].
*/