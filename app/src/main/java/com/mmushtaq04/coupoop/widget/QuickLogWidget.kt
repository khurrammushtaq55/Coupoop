package com.mmushtaq04.coupoop.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.GlanceModifier

class QuickLogWidget : GlanceAppWidget() {
//    @Composable
//    override fun Content() {
//         Simple UI: show one-tap text and instruct user to tap to open app quick-log
//        Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
//            Text(text = "Last: --")
//            Text(text = "One-tap log 💩 (tap)")
//        }
//    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        TODO("Not yet implemented")
    }
}

class QuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickLogWidget()
}
