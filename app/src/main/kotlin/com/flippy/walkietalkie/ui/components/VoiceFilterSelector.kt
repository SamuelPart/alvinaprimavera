package com.flippy.walkietalkie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flippy.walkietalkie.audio.SoundEffects
import com.flippy.walkietalkie.data.model.VoiceFilter
import com.flippy.walkietalkie.ui.theme.*

@Composable
fun VoiceFilterSelector(
    selectedFilter: VoiceFilter,
    onSelectFilter: (VoiceFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filtro de voz",
                style = Typography.labelMedium
            )
            Text(
                text = selectedFilter.displayName,
                style = Typography.labelMedium,
                color = Accent,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chips tipo píldora, estilo moderno
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VoiceFilter.values().forEach { filter ->
                val isSelected = filter == selectedFilter

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isSelected) AccentContainer else Surface)
                        .clickable {
                            SoundEffects.playDialTick()
                            onSelectFilter(filter)
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = getFilterIcon(filter.iconName),
                        contentDescription = filter.displayName,
                        tint = if (isSelected) Accent else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = filter.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Accent else TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun getFilterIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Radio" -> Icons.Default.Radio
        "Bot" -> Icons.Default.SmartToy
        "Shield" -> Icons.Default.Security
        "Sparkles" -> Icons.Default.AutoAwesome
        "Megaphone" -> Icons.Default.Campaign
        "Orbit" -> Icons.Default.Language
        else -> Icons.Default.Mic
    }
}
