package com.example.jellyfintv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.ui.theme.TextPrimary

@Composable
fun MediaRow(
    title: String,
    items: List<MediaItem>,
    getPosterUrl: (String) -> String,
    onItemClick: (MediaItem) -> Unit,
    onItemFocused: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
    posterHeaders: Map<String, String> = emptyMap()
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary
            ),
            modifier = Modifier.padding(start = 36.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = items,
                key = { item -> item.id }
            ) { item ->
                MediaCard(
                    item = item,
                    posterUrl = getPosterUrl(item.id),
                    onClick = { onItemClick(item) },
                    onFocus = { onItemFocused(item) },
                    posterHeaders = posterHeaders
                )
            }
        }
    }
}
