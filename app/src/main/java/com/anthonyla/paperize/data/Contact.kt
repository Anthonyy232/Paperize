package com.anthonyla.paperize.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat.startActivity

@Composable
fun Contact(context: Context) {
    val email = "anthonyyla.dev@gmail.com"
    val cc = ""
    val subject = "[Contact] Paperize - A Wallpaper Changer"
    val bodyText = "This is regarding the Paperize app for Android:\n"
    val mailto = "mailto:" + Uri.encode(email) +
            "?cc=" + Uri.encode(cc) +
            "&subject=" + Uri.encode(subject) +
            "&body=" + Uri.encode(bodyText)

    val emailIntent = Intent(Intent.ACTION_SENDTO)
    emailIntent.setData(Uri.parse(mailto))
    startActivity(context, emailIntent, null)
}