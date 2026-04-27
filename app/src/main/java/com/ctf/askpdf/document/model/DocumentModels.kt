package com.ctf.askpdf.document.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ctf.askpdf.R
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity(tableName = "document_file")
data class DocumentFile(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    var displayName: String = "",
    var path: String = "",
    var mimeType: String = "",
    var size: Long = 0L,
    var dateAdded: Long = 0L,
    var recentViewTime: Long = 0L,
    var collected: Boolean = false,
    var collectedAt: Long = 0L
) : Parcelable {
    fun resolveType(): DocumentKind? = supportedMimeTypes.entries.firstOrNull { mimeType in it.value }?.key
}

@Keep
enum class DocumentTab {
    HOME, RECENT, COLLECTION
}

@Keep
enum class DocumentKind(@DrawableRes val iconRes: Int) {
    ALL(R.drawable.ic_pdf),
    PDF(R.drawable.ic_pdf),
    WORD(R.drawable.ic_word),
    EXCEL(R.drawable.ic_excel),
    PPT(R.drawable.ic_ppt)
}

val supportedMimeTypes: Map<DocumentKind, List<String>> = mapOf(
    DocumentKind.PDF to listOf("application/pdf"),
    DocumentKind.WORD to listOf(
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.template"
    ),
    DocumentKind.EXCEL to listOf(
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.template"
    ),
    DocumentKind.PPT to listOf(
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.openxmlformats-officedocument.presentationml.template",
        "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
    )
)
