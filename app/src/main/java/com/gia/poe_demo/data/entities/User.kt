package com.gia.poe_demo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User entity - stores account credentials locally.
 * Passwords are hashed with MD5 before storage.
 * Reference: IIE PROG7313 Module Manual (2026)
 */

// @Entity tells Room this is a database table called "users"
// ref: https://developer.android.com/training/data-storage/room/defining-data
// ref: https://developer.android.com/reference/androidx/room/Entity
@Entity(tableName = "users")
// data class used here so Kotlin auto-generates equals, hashCode and toString
// ref: https://kotlinlang.org/docs/data-classes.html
data class User(
    // @PrimaryKey with autoGenerate = true so Room automatically assigns a unique ID
    // ref: https://developer.android.com/reference/androidx/room/PrimaryKey
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val username: String,
    val password: String,
    val passwordHash: String,
    val honeyPoints: Int = 0,
    val streak: Int = 0,
    val lastLoginDate: Long = 0L,
    val joinedAt: Long = System.currentTimeMillis()
)

/*
References:

Android Developers, 2024. Define data using Room entities.
Available at: https://developer.android.com/training/data-storage/room/defining-data
[Accessed 27 April 2026].

Android Developers, 2024. Entity.
Available at: https://developer.android.com/reference/androidx/room/Entity
[Accessed 27 April 2026].

Android Developers, 2024. PrimaryKey.
Available at: https://developer.android.com/reference/androidx/room/PrimaryKey
[Accessed 27 April 2026].

Kotlin, 2024. Data Classes.
Available at: https://kotlinlang.org/docs/data-classes.html
[Accessed 27 April 2026].

Android Developers, 2024. MessageDigest.
Available at: https://developer.android.com/reference/java/security/MessageDigest
[Accessed 27 April 2026].

IIE, 2026. PROG7313 Module Manual.
Varsity College: The Independent Institute of Education.
*/