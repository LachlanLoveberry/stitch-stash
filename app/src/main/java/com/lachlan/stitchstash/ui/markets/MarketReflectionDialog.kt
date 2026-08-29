package com.lachlan.stitchstash.ui.markets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lachlan.stitchstash.data.db.entities.Market

/**
 * Post-market check-in: asks whether the maker went, then invites (all optional)
 * reflections on how it went, how they feel, and what they learned.
 */
@Composable
fun MarketReflectionDialog(
    market: Market,
    onDismissDidNotGo: () -> Unit,
    onSaveReflection: (howItWent: String?, howItFelt: String?, whatLearned: String?) -> Unit,
    onSkipReflection: () -> Unit,
) {
    var attended by remember { mutableStateOf<Boolean?>(null) }

    Dialog(
        onDismissRequest = { /* require an explicit choice */ },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
        ) {
            if (attended != true) {
                AttendedStep(
                    marketName = market.name,
                    onWent = { attended = true },
                    onDidNotGo = onDismissDidNotGo,
                )
            } else {
                ReflectionStep(
                    marketName = market.name,
                    onSave = onSaveReflection,
                    onSkip = onSkipReflection,
                )
            }
        }
    }
}

@Composable
private fun AttendedStep(
    marketName: String?,
    onWent: () -> Unit,
    onDidNotGo: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (marketName != null) "Did you make it to $marketName?" else "Did you make it to your market?",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
        )
        Text(
            "No pressure either way — just checking in.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDidNotGo,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Not this time") }
            Button(
                onClick = onWent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("I went!") }
        }
    }
}

@Composable
private fun ReflectionStep(
    marketName: String?,
    onSave: (String?, String?, String?) -> Unit,
    onSkip: () -> Unit,
) {
    var howItWent by remember { mutableStateOf("") }
    var howItFelt by remember { mutableStateOf("") }
    var whatLearned by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (marketName != null) "How was $marketName?" else "How was the market?",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
        )
        Text(
            "Jot down as much or as little as you like — all of this is optional.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ReflectionField(
            label = "How did it go?",
            placeholder = "Sales, conversations, anything that stood out...",
            value = howItWent,
            onValueChange = { howItWent = it },
        )
        ReflectionField(
            label = "How are you feeling about it?",
            placeholder = "Proud, tired, inspired — however it landed",
            value = howItFelt,
            onValueChange = { howItFelt = it },
        )
        ReflectionField(
            label = "What did you learn?",
            placeholder = "Something to try again, or differently, next time",
            value = whatLearned,
            onValueChange = { whatLearned = it },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Skip for now") }
            Button(
                onClick = { onSave(howItWent, howItFelt, whatLearned) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Save reflection") }
        }
    }
}

@Composable
private fun ReflectionField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
