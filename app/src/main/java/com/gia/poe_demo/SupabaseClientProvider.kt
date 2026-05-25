package com.gia.poe_demo

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage


object SupabaseClientProvider {


    private const val SUPABASE_URL = "https://ivnvswdedfswrecwbdpc.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml2bnZzd2RlZGZzd3JlY3diZHBjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk2MjkxMzIsImV4cCI6MjA5NTIwNTEzMn0.jJNnMskjQODblnayOTH7K55i8clossKxOA8Bv3yay-k"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Storage)
        install(Postgrest)
    }
}

/*
References:
Supabase, 2026. Kotlin Quickstart.
Available at: https://supabase.com/docs/guides/getting-started/quickstarts/kotlin
[Accessed 24 May 2026].

supabase-community, 2026. supabase-kt GitHub Repository.
Available at: https://github.com/supabase-community/supabase-kt
[Accessed 24 May 2026].
*/
