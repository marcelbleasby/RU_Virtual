package com.bmo.mennu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bmo.mennu.ui.cardapio.DietTag
import com.bmo.mennu.ui.theme.DietGreen

@Composable
fun DietTagChip(label: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = tint, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun DietTagChip(tag: DietTag, modifier: Modifier = Modifier) {
    val (icon, tint) = tag.iconAndTint()
    DietTagChip(label = tag.label, icon = icon, tint = tint, modifier = modifier)
}

@Composable
fun DietTag.iconAndTint(): Pair<ImageVector, Color> = when (this) {
    DietTag.VEGETARIANO -> Icons.Filled.Eco to DietGreen
    DietTag.VEGANO -> Icons.Filled.Spa to DietGreen
    DietTag.SEM_GLUTEN -> Icons.Filled.Grain to MaterialTheme.colorScheme.secondaryContainer
    DietTag.SEM_LACTOSE -> Icons.Filled.LocalDrink to MaterialTheme.colorScheme.tertiary
}
