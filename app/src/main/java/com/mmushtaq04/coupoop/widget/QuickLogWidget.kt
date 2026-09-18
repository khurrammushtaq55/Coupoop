package com.mmushtaq04.coupoop.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.background
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.material3.GlanceTheme
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mmushtaq04.coupoop.MainActivity

class QuickLogWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Content(context)
            }
        }
    }

    @Composable
    private fun Content(context: Context) {
        // Tapping the widget opens MainActivity, which reads this extra
        // and fires a background one-tap log (see MainActivity.onCreate).
        val quickLogIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("quick_log", true)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer, cornerRadius = 16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(quickLogIntent))
        ) {
            Text(
                text = "One-tap log 💩",
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
            )
            Text(
                text = "Tap to log",
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
            )
        }
    }
}

class QuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickLogWidget()
}
