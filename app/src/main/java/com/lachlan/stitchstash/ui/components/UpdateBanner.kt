package com.lachlan.stitchstash.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.lachlan.stitchstash.data.update.UpdateChecker
import com.lachlan.stitchstash.data.update.UpdateInfo

@Composable
fun UpdateBanner() {
    val context = LocalContext.current
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        update = UpdateChecker.check()
    }

    val current = update
    if (current == null || dismissed) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Update available · v${current.latestVersion}",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, current.htmlUrl.toUri())
                context.startActivity(intent)
            }) { Text("Open") }
            TextButton(onClick = { dismissed = true }) { Text("Later") }
        }
    }
}
