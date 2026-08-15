package org.alexrust.callwhitelist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.alexrust.callwhitelist.designsystem.WhiteListTheme
import org.alexrust.callwhitelist.model.OPEN_JOURNAL_ACTION

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhiteListTheme {
                WhiteListApp(openJournal = intent?.action == OPEN_JOURNAL_ACTION)
            }
        }
    }
}
