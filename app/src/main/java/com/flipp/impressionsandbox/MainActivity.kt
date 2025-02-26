package com.flipp.impressionsandbox

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flipp.dl.design.composables.LargeCard
import com.flipp.dl.design.composables.SmallCard
import com.flipp.impressionsandbox.impression.ImpressionQualifier
import com.flipp.impressionsandbox.impression.PercentageViewableQualifier
import com.flipp.impressionsandbox.impression.impression
import com.flipp.impressionsandbox.ui.theme.ImpressionSandboxTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch


@Composable
fun <T : Any> Modifier.track(
    qualifier: ImpressionQualifier,
    key: T,
    impressionableChanged: (Boolean) -> Unit
): Modifier = with(this) {
    val view = LocalView.current
    var impressionable: Boolean? = null

    fun notify(value: Boolean) {
        if (impressionable != value) {
            impressionable = value
            impressionableChanged(value)
        }
    }

    DisposableEffect(key) {
        onDispose {
            notify(false)
        }
    }

    onGloballyPositioned { viewGlobalCoordinates: LayoutCoordinates ->
        val viewGlobalVisibleRect = android.graphics.Rect()
            .apply { view.getGlobalVisibleRect(this) }
            .toComposeRect()

        val viewBoundsInWindow = viewGlobalCoordinates.boundsInWindow()

        val visibleTop = maxOf(viewBoundsInWindow.top, viewGlobalVisibleRect.top)
        val visibleBottom = minOf(viewBoundsInWindow.bottom, viewGlobalVisibleRect.bottom)
        val visibleHeightPx = (visibleBottom - visibleTop).toInt()
        if (visibleHeightPx < 0) {
            // vertical component of view is off-screen
            notify(false)
        }

        val visibleLeft = maxOf(viewBoundsInWindow.left, viewGlobalVisibleRect.left)
        val visibleRight = minOf(viewBoundsInWindow.right, viewGlobalVisibleRect.right)
        val visibleWidthPx = (visibleRight - visibleLeft).toInt()
        if (visibleWidthPx < 0) {
            // horizontal component of view is off-screen
            notify(false)
        }

        val globalWidthPx = viewGlobalCoordinates.size.width
        val globalHeightPx = viewGlobalCoordinates.size.height

        val value = qualifier.isImpression(
            visibleWidthPx = visibleWidthPx,
            visibleHeightPx = visibleHeightPx,
            globalWidthPx = globalWidthPx,
            globalHeightPx = globalHeightPx
        )

        Log.d("MainActivity", "Calculate $key $value")
        notify(value)
    }
}


class MainActivity : ComponentActivity() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val IMPRESSION_MINIMUM_VISIBLE_PERCENTAGE: Float = 0.5F
    }

    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme by remember { mutableStateOf(false) }

            ImpressionSandboxTheme(darkTheme = isDarkTheme) {
                val pagerState = rememberPagerState(pageCount = 4)
                Column(modifier = Modifier.fillMaxSize()) {
                    TabLayout(listOf("Screen 1", "Screen 2", "Screen 3", "Screen 4"), pagerState)
                    HorizontalPager(state = pagerState) { index ->
                        when (index) {
                            0 -> Screen1()
                            1 -> Screen2()
                            2 -> Screen3()
                            3 -> Screen4()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun TabLayout(tabData: List<String>, pagerState: PagerState) {
        val scope = rememberCoroutineScope()
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            divider = { Spacer(modifier = Modifier.height(5.dp)) },
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
                    height = 5.dp,
                    color = Color.White
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            tabData.forEachIndexed { index, title ->
                Tab(selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(text = title) }
                )
            }
        }
    }

    //region helper methods
    @Composable
    private fun Screen1() {
        val verticalListState = rememberLazyListState()
        val horizontalListState = rememberLazyListState()

        val items by remember { mutableStateOf(createData()) }
        val impressionable = remember { mutableSetOf<Int>() }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            state = verticalListState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { count ->
                if (count == 5) LazyRow(
                    state = horizontalListState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items((31..50).toList()) { count ->
                        SmallCardPreview(
                            modifier = Modifier.impression(
                                qualifier = PercentageViewableQualifier(
                                    IMPRESSION_MINIMUM_VISIBLE_PERCENTAGE
                                ),
                                key = count,
                                onImpression = {
                                    Log.d(TAG, "SmallCard impression $count.")
                                }
                            ),
                            count = count
                        )
                    }
                }
                else LargeCardPreview(
                    modifier = Modifier.track(
                        qualifier = PercentageViewableQualifier(
                            IMPRESSION_MINIMUM_VISIBLE_PERCENTAGE
                        ),
                        key = count,
                        impressionableChanged = { value ->
                            val action = if (value) impressionable::add
                            else impressionable::remove

                            action.invoke(count)
                        }
                    ),
                    count = count
                )
            }
        }

        LaunchedEffect(verticalListState.isScrollInProgress, items.size) {
            if (!verticalListState.isScrollInProgress) {
                Log.d(TAG, "Impressions ${impressionable.joinToString(",")}")
            }

            Log.d(TAG, "Scrolling vertical ${verticalListState.isScrollInProgress}.")
        }

        LaunchedEffect(horizontalListState.isScrollInProgress, items.size) {
            Log.d(TAG, "Scrolling horizontal ${horizontalListState.isScrollInProgress}.")
        }
    }

    @Composable
    private fun Screen2() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val count = remember { 100 }
            LargeCardPreview(
                modifier = Modifier.impression(
                    qualifier = PercentageViewableQualifier(
                        IMPRESSION_MINIMUM_VISIBLE_PERCENTAGE
                    ),
                    key = count,
                    onImpression = {
                        Log.d(TAG, "LargeCard impression $count.")
                    }
                ),
                count = count
            )
        }
    }

    @Composable
    private fun Screen3() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val count = remember { 101 }
            SmallCardPreview(
                modifier = Modifier.impression(
                    qualifier = PercentageViewableQualifier(
                        IMPRESSION_MINIMUM_VISIBLE_PERCENTAGE
                    ),
                    key = count,
                    onImpression = {
                        Log.d(TAG, "SmallCard impression $count.")
                    }
                ),
                count = count
            )
        }
    }

    @Composable
    private fun Screen4() {
        val items = remember { mutableStateListOf<Int>() }
        val lazyListState = rememberLazyListState()

        Column {
            Row {
                Button(onClick = { items.add(items.size) }) { Text(text = "Add") }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                state = lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { count ->
                    LargeCardPreview(
                        modifier = Modifier.impression(
                            qualifier = PercentageViewableQualifier(
                                IMPRESSION_MINIMUM_VISIBLE_PERCENTAGE
                            ),
                            key = count,
                            onImpression = {
                                Log.d(TAG, "LargeCard impression $count.")
                            }
                        ),
                        count = count
                    )
                }
            }
        }

        LaunchedEffect(lazyListState.isScrollInProgress, items.size) {
            Log.d(TAG, "User is scrolling ${lazyListState.isScrollInProgress}.")
        }
    }

    @Composable
    private fun LargeCardPreview(modifier: Modifier, count: Int) {
        Box(modifier = modifier) {
            LargeCard(
                title = "Sample Large Card $count",
                titleThumbnailImage = {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(R.drawable.logo),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = null
                    )
                },
                contentThumbnailImage = {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(R.drawable.flyer),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = null
                    )
                },
                iconButtonImage = {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(R.drawable.logo),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = null
                    )
                })
        }
    }

    @Composable
    private fun SmallCardPreview(modifier: Modifier, count: Int) {
        Box(modifier = modifier) {
            SmallCard(
                title = "Sample Small Card $count",
                iconButtonImage = {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(R.drawable.logo),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = null
                    )
                },
                thumbnailImage = {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(R.drawable.flyer),
                        contentScale = ContentScale.Crop,
                        contentDescription = null
                    )
                })
        }
    }

    private fun createData(): List<Int> = ((1..50).toList())
    //endregion
}