package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Use case for exporting location session data to GPX format
 * GPX (GPS Exchange Format) is an XML schema designed for transferring GPS data
 */
class ExportSessionToGpxUseCase(private val context: Context) {

    /**
     * Export single or multiple sessions to GPX file
     * @param sessions List of sessions to export
     * @return File containing GPX data
     */
    fun execute(sessions: List<LocationSession>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "location_sessions_$timestamp.gpx"
        val file = File(context.cacheDir, fileName)

        file.bufferedWriter().use { writer ->
            // GPX Header
            writer.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            writer.write("\n")
            writer.write("""<gpx version="1.1" creator="Location Tracking App" xmlns="http://www.topografix.com/GPX/1/1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">""")
            writer.write("\n")

            // Metadata
            writer.write("  <metadata>\n")
            writer.write("    <name>Location Tracking Sessions</name>\n")
            writer.write("    <time>${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())}</time>\n")
            writer.write("  </metadata>\n")

            sessions.forEach { session ->
                // Each session as a track
                writer.write("  <trk>\n")
                writer.write("    <name>Session ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(session.startTime))}</name>\n")
                writer.write("    <desc>Duration: ${session.getFormattedDuration()}, Distance: ${session.getFormattedDistance()}, Locations: ${session.locations.size}</desc>\n")

                // Track segment
                writer.write("    <trkseg>\n")

                session.locations.forEach { location ->
                    writer.write("      <trkpt lat=\"${location.latitude}\" lon=\"${location.longitude}\">\n")

                    // Elevation (altitude)
                    location.altitude?.let { altitude ->
                        writer.write("        <ele>$altitude</ele>\n")
                    }

                    // Time
                    val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(location.timestamp))
                    writer.write("        <time>$time</time>\n")

                    // Extensions for additional data
                    writer.write("        <extensions>\n")

                    location.accuracy?.let { accuracy ->
                        writer.write("          <accuracy>$accuracy</accuracy>\n")
                    }

                    location.speed?.let { speed ->
                        writer.write("          <speed>$speed</speed>\n")
                    }

                    location.bearing?.let { bearing ->
                        writer.write("          <bearing>$bearing</bearing>\n")
                    }

                    writer.write("        </extensions>\n")
                    writer.write("      </trkpt>\n")
                }

                writer.write("    </trkseg>\n")
                writer.write("  </trk>\n")
            }

            // GPX Footer
            writer.write("</gpx>\n")
        }

        return file
    }

    /**
     * Share the exported GPX file
     */
    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Export GPX Data").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
