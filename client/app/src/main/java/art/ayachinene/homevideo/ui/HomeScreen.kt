package art.ayachinene.homevideo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import art.ayachinene.homevideo.data.model.DirectoryItem
import art.ayachinene.homevideo.data.model.HistoryItem
import art.ayachinene.homevideo.ui.components.ThumbnailPlaceholder
import art.ayachinene.homevideo.ui.theme.CSGOColors
import art.ayachinene.homevideo.ui.theme.PlaceholderColors

@Composable
fun HomeScreen(
    onPlayVideo: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CSGOColors.Background)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Search) {
                    if (uiState.isSearchMode) viewModel.stopSearch()
                    else viewModel.startSearch()
                    true
                } else false
            }
    ) {
        when (uiState.connectionState) {
            is ConnectionState.Loading -> {
                LoadingContent("Connecting to server...")
            }
            is ConnectionState.Error -> {
                ErrorContent(
                    message = (uiState.connectionState as ConnectionState.Error).message,
                    onRetry = { viewModel.retry() }
                )
            }
            is ConnectionState.Connected -> {
                if (uiState.isSearchMode) {
                    SearchContent(
                        query = uiState.searchQuery,
                        results = uiState.searchResults,
                        onQueryChange = { viewModel.search(it) },
                        onFileClick = { item -> onPlayVideo(item.path, item.name) }
                    )
                } else {
                    NetflixHomeContent(
                        heroItem = uiState.heroItem,
                        folderRows = uiState.folderRows,
                        recentHistory = uiState.recentHistory,
                        onPlayVideo = onPlayVideo,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun NetflixHomeContent(
    heroItem: HistoryItem?,
    folderRows: List<FolderRow>,
    recentHistory: List<HistoryItem>,
    onPlayVideo: (String, String) -> Unit,
    onOpenSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top bar
        item {
            TopBar(onOpenSettings = onOpenSettings)
        }

        // Hero banner
        if (heroItem != null) {
            item {
                val isFromHistory = recentHistory.isNotEmpty() &&
                    heroItem.videoPath == recentHistory.firstOrNull()?.videoPath
                HeroBanner(
                    item = heroItem,
                    label = if (isFromHistory) "CONTINUE WATCHING" else "FEATURED",
                    onPlay = { onPlayVideo(heroItem.videoPath, heroItem.videoName) }
                )
            }
        }

        // Continue watching row
        if (recentHistory.isNotEmpty()) {
            item {
                ContentRow(
                    title = "Continue Watching",
                    items = recentHistory.map {
                        DirectoryItem(
                            name = it.videoName,
                            path = it.videoPath,
                            type = "file"
                        )
                    },
                    onItemClick = { item -> onPlayVideo(item.path, item.name) }
                )
            }
        }

        // Folder rows
        items(folderRows) { row ->
            ContentRow(
                title = row.folderName,
                items = row.items,
                onItemClick = { item -> onPlayVideo(item.path, item.name) }
            )
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CSGOColors.Background.copy(alpha = 0.95f),
                        CSGOColors.Background.copy(alpha = 0f)
                    )
                )
            )
            .padding(horizontal = 48.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "HOMEVIDEO",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            color = CSGOColors.Primary
        )
        Surface(
            onClick = onOpenSettings,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(CSGOColors.Surface.copy(alpha = 0.6f))
                .border(1.dp, CSGOColors.Surface.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        ) {
            Text(
                "Settings",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = CSGOColors.OnBackground
            )
        }
    }
}

@Composable
private fun HeroBanner(
    item: HistoryItem,
    label: String = "CONTINUE WATCHING",
    onPlay: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val title = item.videoName.substringBeforeLast(".")
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        onClick = { onPlay() },
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(8.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.0f else 0.98f)
            .graphicsLayer {
                shadowElevation = if (isFocused) 16.dp.toPx() else 0f
            }
    ) {
        Box {
            // Background with large letter
            val letter = title.firstOrNull()?.uppercaseChar() ?: '?'
            val colorIndex = letter.code % PlaceholderColors.size
            val bgColor = PlaceholderColors[colorIndex]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Dark gradient overlay (bottom to top)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CSGOColors.Background.copy(alpha = 0.7f),
                                CSGOColors.Background.copy(alpha = 0.95f)
                            ),
                            startY = 200f,
                            endY = 420f
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = CSGOColors.Secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = CSGOColors.OnBackground
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CSGOColors.Primary)
                        .clickable { onPlay() }
                ) {
                    Text(
                        "  Play  ",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentRow(
    title: String,
    items: List<DirectoryItem>,
    onItemClick: (DirectoryItem) -> Unit
) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            ),
            color = CSGOColors.OnBackground,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            items(items, key = { it.path }) { item ->
                VideoCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun VideoCard(
    item: DirectoryItem,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val displayName = item.name.substringBeforeLast(".")

    Surface(
        onClick = { onClick() },
        modifier = Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CSGOColors.Card)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.08f else 1.0f)
            .graphicsLayer {
                shadowElevation = if (isFocused) 12.dp.toPx() else 0f
            }
            .then(
                if (isFocused) Modifier.border(2.dp, CSGOColors.Primary, RoundedCornerShape(6.dp))
                else Modifier
            )
    ) {
        Column {
            Box {
                ThumbnailPlaceholder(
                    name = item.name,
                    width = 240.dp
                )
                // Subtle gradient at bottom of thumbnail for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, CSGOColors.Card.copy(alpha = 0.8f))
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isFocused) CSGOColors.OnBackground else CSGOColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Connection Failed", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onRetry() }) { Text("Retry") }
    }
}

@Composable
private fun SearchContent(
    query: String,
    results: List<DirectoryItem>,
    onQueryChange: (String) -> Unit,
    onFileClick: (DirectoryItem) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var text by remember { mutableStateOf(query) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(text) {
        onQueryChange(text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CSGOColors.Background)
            .padding(48.dp)
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .padding(bottom = 16.dp),
            textStyle = TextStyle(
                fontSize = 20.sp,
                color = CSGOColors.OnBackground
            ),
            keyboardOptions = KeyboardOptions.Default,
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (text.isEmpty()) {
                        Text("Search videos...", color = CSGOColors.OnSurface)
                    }
                    innerTextField()
                }
            }
        )

        if (query.isNotEmpty() && results.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No matching videos found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(results, key = { it.path }) { item ->
                    VideoCard(
                        item = item,
                        onClick = { onFileClick(item) }
                    )
                }
            }
        }
    }
}
