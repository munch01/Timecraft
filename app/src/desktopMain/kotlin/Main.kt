import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.emeric.timecraft.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "TimeCraft") {
        App()
    }
}
