package dev.aaa1115910.bv.screen.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.LoadingTip
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun RecommendScreen(
    modifier: Modifier = Modifier,
    lazyGridState: LazyGridState,
    recommendViewModel: RecommendViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentFocusedIndex by remember { mutableIntStateOf(0) }
    val shouldLoadMore by remember {
        derivedStateOf { currentFocusedIndex + 24 > recommendViewModel.recommendVideoList.size }
    }

    //不能直接使用 LaunchedEffect(currentFocusedIndex)，会导致整个页面重组
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            scope.launch(Dispatchers.IO) {
                recommendViewModel.loadMore()
                //加载完成后重置shouldLoadMore为false，避免如果加载失败后无法重新加载
                currentFocusedIndex = -100
            }
        }
    }

    LazyVerticalGrid(
        modifier = modifier,
        state = lazyGridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        itemsIndexed(recommendViewModel.recommendVideoList) { index, video ->
            SmallVideoCard(
                data = VideoCardData(
                    avid = video.aid,
                    title = video.title,
                    cover = video.cover,
                    play = with(video.play) { if (this == -1) null else this },
                    danmaku = with(video.danmaku) { if (this == -1) null else this },
                    upName = video.author,
                    time = video.duration * 1000L
                ),
                onClick = { VideoInfoActivity.actionStart(context, video.aid) },
                onFocus = { currentFocusedIndex = index }
            )
        }
        if (recommendViewModel.loading)
            item(
                span = { GridItemSpan(4) }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingTip()
                }
            }
    }
}