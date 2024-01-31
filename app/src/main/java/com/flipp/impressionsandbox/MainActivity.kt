package com.flipp.impressionsandbox

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flipp.dl.design.composables.LargeCard
import com.flipp.dl.design.composables.SmallCard
import com.flipp.impressionsandbox.impression.impression
import com.flipp.impressionsandbox.impression.impression2
import com.flipp.impressionsandbox.ui.theme.ImpressionSandboxTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            LazyColumn {
//                item { LargeCardPreview(count = 101) }
//                item { LargeCardPreview(count = 102) }
//                item { LargeCardPreview(count = 103) }
//                item { LargeCardPreview(count = 104) }
//                item {
//                    Box(Modifier.impression2(105) {
//                        Log.d(TAG, "Impression 105")
//                    }) {
//                        LargeCardPreview(count = 105)
//                    }
//                }
////                item {
////                    Box(Modifier.impression2(106) {
////                        Log.d(TAG, "Impression 106")
////                    }) {
////                        LargeCardPreview(count = 106)
////                    }}
//                item { LargeCardPreview(count = 107) }
//                item { LargeCardPreview(count = 108) }
//                item { LargeCardPreview(count = 109) }
//                item { LargeCardPreview(count = 110) }
//                item { LargeCardPreview(count = 111) }
            Experiment()
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun Experiment() {
        var isDarkTheme by remember { mutableStateOf(false) }
        ImpressionSandboxTheme(darkTheme = isDarkTheme) {
            val pagerState = rememberPagerState(pageCount = 4)
            Column(modifier = Modifier.fillMaxSize()) {
                TabLayout(listOf("Screen 1", "Screen 2", "Screen 3", "Screen 4"), pagerState)
                HorizontalPager(state = pagerState) { index ->
                    when (index) {
                        0 -> Screen1 { isDarkTheme = isDarkTheme.not() }
                        1 -> Screen2()
                        2 -> Screen3()
                        3 -> Screen4()
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
    private fun Screen1(onToggleDarkTheme: () -> Unit) {
        val context = LocalContext.current
        val lazyListState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        var items by remember { mutableStateOf(createData()) }

        Column {
            Row {
                Button(onClick = {
                    scope.launch {
                        items = createData()
                        lazyListState.scrollToItem(0)
                    }
                }) { Text(text = "Refresh") }
                Button(onClick = { startActivity(Intent(context, SettingsActivity::class.java)) }) {
                    Text(
                        text = "Navigate"
                    )
                }
                Button(onClick = onToggleDarkTheme) {
                    Text(
                        text = "Theme"
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                state = lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { count ->
                    if (count == 5) LazyRow(
                        state = rememberLazyListState(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items((31..50).toList()) { count ->
                            Box(modifier = Modifier.impression2(count) {
                                Log.d(TAG, "SmallCard impression $count.")
                            }) {
                                SmallCardPreview(count = count)
                            }
                        }
                    }
                    else Box(modifier = Modifier.impression2(count) {
                        Log.d(TAG, "LargeCard impression $count.")
                    }) {
                        LargeCardPreview(count = count)
                    }
                }
            }
        }
    }

    @Composable
    private fun Screen2() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LargeCardPreview(count = 100)
        }
    }

    @Composable
    private fun Screen3() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.impression2(101) {
                Log.d(TAG, "SmallCard impression $101.")
            }) {
                SmallCardPreview(count = 101)
            }
        }
    }

    @Composable
    private fun Screen4() {
        val items = remember { mutableStateListOf<Int>() }
        Column {
            Row {
                Button(onClick = { items.add(items.size) }) { Text(text = "Add") }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                state = rememberLazyListState(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { count ->
                    Box(modifier = Modifier.impression2(count) {
                        Log.d(TAG, "LargeCard impression $count.")
                    }) { LargeCardPreview(count = count) }
                }
            }
        }
    }

    @Composable
    private fun LargeCardPreview(count: Int) {
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

    @Composable
    private fun SmallCardPreview(count: Int) {
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

    private fun createData(): List<Int> = ((1..50).toList())
//endregion
}