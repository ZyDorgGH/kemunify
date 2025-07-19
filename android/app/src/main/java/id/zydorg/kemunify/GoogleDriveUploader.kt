package id.zydorg.kemunify

import android.accounts.Account
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import id.zydorg.kemunify.data.preference.UserPreferences
import id.zydorg.kemunify.data.preference.userDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections


class GoogleDriveUploader(private val context: Context) {
    private var fileId = ""
    private val userPreferencesDataStore = UserPreferences.getInstance(context.userDataStore)

    suspend fun uploadFileToDrive(file: File) {
        withContext(Dispatchers.IO) {
            try {
                val myCredential = GoogleAccountCredential.usingOAuth2(
                    context,
                    Collections.singleton(DriveScopes.DRIVE_FILE)
                )

                val email = userPreferencesDataStore.getUserSession()
                    .map { it.email }
                    .firstOrNull()

                myCredential.selectedAccount = email?.let { Account(it, "com.google") }

                val driveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    myCredential
                ).setApplicationName("Kemunify").build()

                val folderId = getOrCreateFolder(driveService)

                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = file.name
                    parents = listOf(folderId)
                }

                val mediaContent = com.google.api.client.http.FileContent(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    file
                )

                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink")
                    .execute()

                withContext(Dispatchers.Main) {
                    fileId = uploadedFile.id
                    Toast.makeText(
                        context,
                        "File berhasil diupload! ID: ${uploadedFile.id}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: GoogleJsonResponseException) {
                Log.e("DRIVE_ERROR", "Google API error: ${e.details?.code} - ${e.details?.message}", e)
            } catch (e: Exception) {
                Log.e("DRIVE_ERROR", "General error: ${e.message}", e)
            }
        }
    }

    private fun getOrCreateFolder(driveService: Drive): String {
        return try {
            val folderName = "Rekap Sampah Bank Kemuning"
            val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val result: FileList = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()
            if (result.files.isNotEmpty()) {
                return result.files[0].id
            }

            val folderMetadata = com.google.api.services.drive.model.File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
            }

            val folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()

            folder.id
        } catch (e: Exception) {
            Log.e("FOLDER_ERROR", "Gagal membuat folder: ${e.message}")
            throw e // Propagate error ke caller
        }
    }
}