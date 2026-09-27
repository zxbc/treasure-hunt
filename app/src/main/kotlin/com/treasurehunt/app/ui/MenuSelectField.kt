package com.treasurehunt.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A Material outlined-field look that is a plain clickable box: a tap
 * anywhere on it opens the caller's dropdown menu.
 *
 * This exists because a real readOnly [androidx.compose.material3.OutlinedTextField]
 * swallows taps — its selectable text consumes the pointer events before a
 * clickable modifier wrapping it can see them, so the menu never opens.
 * Drawing the field chrome ourselves and using a regular clickable keeps the
 * M3 outlined look while making the whole field (and only the field) trigger
 * the menu.
 */
@Composable
fun MenuSelectField(
    value: String,
    label: String? = null,
    enabled: Boolean = true,
    expanded: Boolean = false,
    onOpen: () -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, if (enabled) outline else outline.copy(alpha = 0.4f), shape)
            .clickable(enabled = enabled, onClick = onOpen),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(
            Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) onSurfaceVariant else onSurfaceVariant.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) onSurface else onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = if (enabled) onSurfaceVariant else onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
        )
    }
}
