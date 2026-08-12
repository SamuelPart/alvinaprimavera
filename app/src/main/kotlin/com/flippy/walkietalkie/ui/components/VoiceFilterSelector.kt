package com.flippy.walkietalkie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
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
                text = "FILTROS DSP DE VOZ:",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextGray
            )
            Text(
                text = selectedFilter.displayName.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = NeonEmerald
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VoiceFilter.values().forEach { filter ->
                val isSelected = filter == selectedFilter

                Column(
                    modifier = Modifier
                        .width(86.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonEmerald.copy(alpha = 0.15f) else CardBackground)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonEmerald else BorderDark,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            SoundEffects.playDialTick()
                            onSelectFilter(filter)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NeonEmerald else Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getFilterIcon(filter.iconName),
                            contentDescription = filter.displayName,
                            tint = if (isSelected) BlackBackground else TextWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = filter.displayName,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonEmerald else TextGray,
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
