package dk.frankbille.iou

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dk.frankbille.iou.graphql.AndroidAppContextHolder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAppContextHolder.applicationContext = applicationContext

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun appAndroidPreview() {
    App()
}
