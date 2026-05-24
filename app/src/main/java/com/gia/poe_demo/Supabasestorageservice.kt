package com.gia.poe_demo

import android.util.Log
import io.github.jan.supabase.storage.storage
import java.io.File

//Service object that handles all supabase storage operations for reciept images
//Adapted from supabaseDOCS
object SupabaseStorageService {

    private const val TAG = "SupabaseStorage"

    private const val BUCKET_NAME = "receipts"

    /**
     * Uploads a receipt image file to Supabase Storage.
     *
     * The file is stored under: receipts/userId_timestamp.jpg
     * This makes each path unique so files never overwrite each other.
     *
     * Returns the public URL of the uploaded file, or null if the upload failed.
     *
     *Adapted from supabase DOCS
     */
    suspend fun uploadReceiptImage(file: File, userId: Int): String? {
        return try {
            // Build a unique storage path using the userId and current timestamp
            // This prevents filename collisions between users and uploads
            val fileName = "${userId}_${System.currentTimeMillis()}.jpg"
            val storagePath = "receipts/$fileName"

            Log.d(TAG, "Starting upload: $storagePath")

             //Read the file into a ByteArray for upload
            val bytes = file.readBytes()

            //Upload to the receipts bucket
            // upsert = true means it will overwrite if the same path already exists
            SupabaseClientProvider.client.storage
                .from(BUCKET_NAME)
                .upload(storagePath, bytes) {
                    upsert = true
                }

            // Build and return the public URL so it can be saved to the local DB
            // and later loaded in ExpensesListActivity
            val publicUrl = SupabaseClientProvider.client.storage
                .from(BUCKET_NAME)
                .publicUrl(storagePath)

            Log.d(TAG, "Upload successful. Public URL: $publicUrl")
            publicUrl

        } catch (e: Exception) {
            // Log the error but don't crash
            Log.e(TAG, "Upload failed: ${e.message}", e)
            null
        }
    }
}

/*
References:
Supabase, 2026. Storage Documentation.
Available at: https://supabase.com/docs/guides/storage
[Accessed 24 May 2026].

supabase-community, 2026. Storage README — supabase-kt.
Available at: https://github.com/supabase-community/supabase-kt/blob/master/Storage/README.md
[Accessed 24 May 2026].

Supabase, 2026. storage-from-upload (Kotlin reference).
Available at: https://supabase.com/docs/reference/kotlin/storage-from-upload
[Accessed 24 May 2026].

Supabase 2023. Getting started with Android and Supabase. [YouTube video]
Available at: https://youtu.be/_iXUVJ6HTHU
[Accessed 24 May 2026].
*/


