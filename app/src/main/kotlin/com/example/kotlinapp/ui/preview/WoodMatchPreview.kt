package com.example.kotlinapp.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.kotlinapp.ui.theme.WoodMatchTheme

/** Variants shared by the screen previews in Android Studio. */
@Preview(name = "Teléfono · claro", group = "WoodMatch", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Teléfono · oscuro", group = "WoodMatch", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Texto ampliado", group = "WoodMatch", widthDp = 360, heightDp = 800, fontScale = 1.3f, showBackground = true)
@Preview(name = "Tableta", group = "WoodMatch", widthDp = 840, heightDp = 900, showBackground = true)
annotation class WoodMatchPreviews

@Composable
fun PreviewFrame(content: @Composable () -> Unit) {
    WoodMatchTheme {
        Surface(Modifier.fillMaxSize()) { content() }
    }
}
