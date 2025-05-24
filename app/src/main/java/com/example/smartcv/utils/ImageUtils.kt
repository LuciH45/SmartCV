package com.example.smartcv.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.smartcv.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility function to load an image from a URI asynchronously
 * Returns a painter that can be used in Image composable
 */
@Composable
fun rememberAsyncImagePainter(uri: Uri): androidx.compose.ui.graphics.painter.Painter {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    bitmap = BitmapFactory.decodeStream(input)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return if (bitmap != null) {
        androidx.compose.ui.graphics.painter.BitmapPainter(bitmap!!.asImageBitmap())
    } else {
        painterResource(R.drawable.ic_launcher_foreground)
    }
}

