package com.lachlan.stitchstash.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.lachlan.stitchstash.domain.stickers.StickerCatalog
import com.lachlan.stitchstash.ui.components.ConfettiBurst
import com.lachlan.stitchstash.ui.stickers.StickerVisual

@Composable
fun CelebrationDialog(
    data: CelebrationData,
    onCreateCard: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box {
            ConfettiBurst(active = true)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = 380.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Beautiful work ",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                        textAlign = TextAlign.Center,
                    )

                    if (data.photoPath != null) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            AsyncImage(
                                model = data.photoPath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Text(
                        "${data.patternName} · ${data.colourwayName}",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                        textAlign = TextAlign.Center,
                    )

                    if (data.stickers.isNotEmpty()) {
                        Text(
                            "Stickers earned",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            data.stickers.forEach { sticker ->
                                val def = StickerCatalog.get(sticker.type)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    StickerVisual(
                                        type = sticker.type,
                                        size = 72.dp,
                                        earned = true,
                                        spin = true,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        def.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) { Text("Done") }
                        Button(
                            onClick = onCreateCard,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) { Text("Make card") }
                    }
                }
            }
        }
    }
}
