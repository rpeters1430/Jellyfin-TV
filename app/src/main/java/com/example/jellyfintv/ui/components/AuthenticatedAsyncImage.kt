package com.example.jellyfintv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * AsyncImage wrapper that attaches auth headers to the Coil request instead of relying on a
 * token baked into the URL query string - keeps the token out of any URL that could end up in
 * logs (e.g. RetrofitClient's request-line logging), matching how video streaming already
 * authenticates via headers rather than an api_key param.
 */
@Composable
fun AuthenticatedAsyncImage(
    url: String,
    headers: Map<String, String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val model = remember(url, headers) {
        ImageRequest.Builder(context)
            .data(url)
            .apply { headers.forEach { (name, value) -> setHeader(name, value) } }
            .build()
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}
