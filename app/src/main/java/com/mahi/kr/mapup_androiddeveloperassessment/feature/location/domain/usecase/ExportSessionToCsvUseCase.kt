package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Use case for exporting location session data to CSV format
 */
class ExportSessionToCsvUseCase(private val context: Context) {

    /**
     * Export single or multiple sessions to CSV file
     * @param sessions List of sessions to export
     * @return File containing CSV data
     */
    fun execute(sessions: List<LocationSession>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "location_sessions_$timestamp.csv"
        val file = File(context.cacheDir, fileName)

        file.bufferedWriter().use { writer ->
            // CSV Header - simple format
            writer.write("latitude\tlongitude\ttimestamp\n")

            // ISO 8601 date formatter
            val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            sessions.forEach { session ->
                session.locations.forEach { location ->
                    writer.write(
                        "${location.latitude}\t" +
                        "${location.longitude}\t" +
                        "${iso8601Format.format(Date(location.timestamp))}\n"
                    )
                }
            }
        }

        return file
    }

    /**
     * Share the exported CSV file
     */
    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Export Location Data").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
