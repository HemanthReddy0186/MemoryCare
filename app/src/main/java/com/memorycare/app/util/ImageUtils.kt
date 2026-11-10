package com.memorycare.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Copies an image selected from the gallery (via Uri) into the app's internal storage
     * scoped to the specific profileId so photos never leak across profiles.
     */
    fun saveUriToInternalStorage(context: Context, sourceUri: Uri, profileId: String = "default"): String? {
        return try {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            val baseDir = File(context.filesDir, "face_match_photos")
            val imagesDir = File(baseDir, profileId)
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val destFile = File(imagesDir, "face_${System.currentTimeMillis()}.jpg")
            val output = FileOutputStream(destFile)
            input.use { inStream ->
                output.use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            destFile.absolutePath
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Saves a captured camera Bitmap into the app's internal storage scoped to profileId.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, profileId: String = "default"): String? {
        return try {
            val baseDir = File(context.filesDir, "face_match_photos")
            val imagesDir = File(baseDir, profileId)
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val destFile = File(imagesDir, "face_${System.currentTimeMillis()}.jpg")
            val output = FileOutputStream(destFile)
            output.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outStream)
            }
            destFile.absolutePath
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Purges all saved photos belonging to a specific profile when that profile is deleted.
     */
    fun deleteProfilePhotos(context: Context, profileId: String) {
        try {
            val imagesDir = File(File(context.filesDir, "face_match_photos"), profileId)
            if (imagesDir.exists()) {
                imagesDir.deleteRecursively()
            }
        } catch (_: Throwable) {
            // Ignore deletion failures
        }
    }

    /**
     * Decodes an image file safely with inSampleSize downsampling and corrects EXIF orientation
     * so photos taken from phone cameras don't appear rotated sideways.
     */
    fun decodeRotatedBitmap(path: String?, reqWidth: Int = 600, reqHeight: Int = 600): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null

        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            var inSampleSize = 1
            val height = boundsOptions.outHeight
            val width = boundsOptions.outWidth

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val decodedBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            // Read EXIF orientation to auto-rotate if needed
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return decodedBitmap
            }

            Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
        } catch (_: Throwable) {
            null
        }
    }
}
