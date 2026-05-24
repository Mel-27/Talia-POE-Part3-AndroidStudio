package com.gia.poe_demo

import android.util.Log
import io.github.jan.supabase.storage.storage
import java.io.File


object SupabaseStorageService {

    private const val TAG = "SupabaseStorage"

    private const val BUCKET_NAME = "receipts"


    suspend fun uploadReceiptImage(file: File, userId: Int): String? {
        return try {

            val fileName = "${userId}_${System.currentTimeMillis()}.jpg"
            val storagePath = "receipts/$fileName"

            Log.d(TAG, "Starting upload: $storagePath")


            val bytes = file.readBytes()


            SupabaseClientProvider.client.storage
                .from(BUCKET_NAME)
                .upload(storagePath, bytes) {
                    upsert = true
                }

            val publicUrl = SupabaseClientProvider.client.storage
                .from(BUCKET_NAME)
                .publicUrl(storagePath)

            Log.d(TAG, "Upload successful. Public URL: $publicUrl")
            publicUrl

        } catch (e: Exception) {

            Log.e(TAG, "Upload failed: ${e.message}", e)
            null
        }
    }
}

