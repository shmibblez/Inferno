/* stolen code created by this legend: https://medium.com/@ssvaghasiya61
 * found here: https://www.multiplatform.network/component/jetpack-compose/doc/number-picker */
package com.shmibblez.inferno.browser.prompts.compose.sub

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// todo: style this
@Composable
fun rememberNumberPickerState() = remember { NumberPickerState() }

class NumberPickerState {
    var selectedItem by mutableStateOf("")
}

@Composable
private fun Picker(
    state: NumberPickerState,
    items: List<String>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    visibleItemsCount: Int = 3,
    textModifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    dividerColor: Color = LocalContentColor.current,
) {

    val visibleItemsMiddle = visibleItemsCount / 2
    val listScrollCount = Integer.MAX_VALUE
    val listScrollMiddle = listScrollCount / 2
    val listStartIndex =
        listScrollMiddle - listScrollMiddle % items.size - visibleItemsMiddle + startIndex

    fun getItem(index: Int) = items[index % items.size]

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = listStartIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightPixels = remember { mutableIntStateOf(0) }
    val itemHeightDp = pixelsToDp(itemHeightPixels.intValue)

    val fadingEdgeGradient = remember {
        Brush.verticalGradient(
            0f to Color.Transparent, 0.5f to Color.Black, 1f to Color.Transparent
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.map { index -> getItem(index + visibleItemsMiddle) }
            .distinctUntilChanged().collect { item -> state.selectedItem = item }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * visibleItemsCount)
                .fadingEdge(fadingEdgeGradient)
        ) {
            items(listScrollCount) { index ->
                Text(text = getItem(index),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = textStyle,
                    modifier = Modifier
                        .onSizeChanged { size ->
                            itemHeightPixels.intValue = size.height
                        }
                        .then(textModifier))
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(top = itemHeightDp * visibleItemsMiddle)
                .height(1.dp),
            color = dividerColor
        )

        HorizontalDivider(
            modifier = Modifier
                .padding(top = (itemHeightDp * visibleItemsMiddle) + itemHeightDp)
                .height(1.dp), color = dividerColor
        )
    }
}

fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

@Composable
private fun pixelsToDp(pixels: Int) = with(LocalDensity.current) { pixels.toDp() }

@Composable
fun NumberPicker(
    state: NumberPickerState, values: List<String>, modifier: Modifier = Modifier, selected: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        val selectedIndex = values.indexOf(selected).let { if (it < 0) 0 else it }

        Picker(
            state = state,
            items = values,
            startIndex = selectedIndex,
            visibleItemsCount = 5,
            modifier = Modifier.fillMaxWidth(0.5f),
            textModifier = Modifier.padding(8.dp),
            textStyle = TextStyle(fontSize = 32.sp),
            dividerColor = Color(0xFFE8E8E8)
        )

        Text(
            text = "Result: ${state.selectedItem}",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight(500),
            fontSize = 20.sp,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(0.5f)
                .background(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(size = 8.dp))
                .padding(vertical = 10.dp, horizontal = 16.dp)
        )
    }
}