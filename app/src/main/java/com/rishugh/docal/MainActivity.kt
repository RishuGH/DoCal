package com.rishugh.docal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rishugh.docal.ui.DoCalApp
import com.rishugh.docal.ui.theme.DoCalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoCalTheme {
                DoCalApp()
            }
        }
    }
}

@Preview(name = "Phone Portrait", showBackground = true, widthDp = 380, heightDp = 800)
@Composable
fun DoCalAppPhonePreview() {
    DoCalTheme {
        DoCalApp()
    }
}

@Preview(name = "Tablet Landscape", showBackground = true, widthDp = 840, heightDp = 480)
@Composable
fun DoCalAppPreview() {
    DoCalTheme {
        DoCalApp()
    }
}
