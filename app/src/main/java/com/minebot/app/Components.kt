package com.minebot.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

enum class ButtonKind {
    Primary, Start, Stop
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: ButtonKind = ButtonKind.Primary
) {
    val containerColor = when (kind) {
        ButtonKind.Primary -> MaterialTheme.colorScheme.primary
        ButtonKind.Start -> Color(0xFF16A34A)
        ButtonKind.Stop -> Color(0xFFDC2626)
    }
    val contentColor = Color.White

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(text)
    }
}

@Composable
fun MineBotTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        singleLine = true
    )
}

@Composable
fun LabeledValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null
) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatusCard(botStatus: BotStatus) {
    val statusText = botStatus.status.replaceFirstChar { it.uppercase() }
    val dot = when (botStatus.status.lowercase()) {
        "connected", "online" -> Color(0xFF22C55E)
        "reconnecting", "starting" -> Color(0xFFF59E0B)
        "error" -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Bot Status", style = MaterialTheme.typography.titleLarge)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(dot)
                        .width(18.dp)
                        .padding(0.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(statusText, style = MaterialTheme.typography.headlineMedium)
            }

            LabeledValue("Server", botStatus.server ?: "—")
            LabeledValue("Uptime", botStatus.uptimeMs?.let ::formatDuration ?: "—")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServerPicker(
    servers: List<ServerRecord>,
    selectedServerId: String?,
    onServerSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = servers.firstOrNull { it.id == selectedServerId } ?: servers.firstOrNull()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.label ?: "No server selected",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            servers.forEach { server ->
                DropdownMenuItem(
                    text = { Text(server.label) },
                    onClick = {
                        onServerSelected(server.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = max(0L, ms / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
