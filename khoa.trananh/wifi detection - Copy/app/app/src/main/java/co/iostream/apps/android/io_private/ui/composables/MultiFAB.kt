package co.iostream.apps.android.io_private.ui.composables

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.iostream.apps.android.io_private.ui.theme.Theme

@Immutable
interface FabIcon {

    @Stable
    val iconRes: Int

    @Stable
    val iconResAfterRotate: Int

    @Stable
    val iconRotate: Float?
}

@Immutable
interface FabOption {
    @Stable
    val iconTint: Color

    @Stable
    val backgroundTint: Color

    @Stable
    val showLabels: Boolean
}

private class FabIconImpl(
    override val iconRes: Int,
    override val iconResAfterRotate: Int,
    override val iconRotate: Float?,
) : FabIcon

class MultiFabItem(
    val tag: String,
    val icon: Int,
    val label: String,
//    val labelColor: Color,
)

sealed class MultiFabState {

    object Collapsed : MultiFabState()
    object Expanded : MultiFabState()

    fun toggleValue() = if (isExpanded()) {
        Collapsed
    } else {
        Expanded
    }

    fun isExpanded() = this == Expanded
}

private class FabOptionImpl(
    override val iconTint: Color,
    override val backgroundTint: Color,
    override val showLabels: Boolean,
) : FabOption

fun FabIcon(
    @DrawableRes iconRes: Int,
    @DrawableRes iconResAfterRotate: Int,
    iconRotate: Float? = null,
): FabIcon =
    FabIconImpl(iconRes = iconRes, iconResAfterRotate = iconResAfterRotate, iconRotate = iconRotate)

@Composable
fun rememberMultiFabState() = remember { mutableStateOf<MultiFabState>(MultiFabState.Collapsed) }


@Composable
fun FabOption(
    backgroundTint: Color = MaterialTheme.colorScheme.secondary,
    iconTint: Color = contentColorFor(backgroundTint),
    showLabels: Boolean = false,
): FabOption =
    FabOptionImpl(iconTint = iconTint, backgroundTint = backgroundTint, showLabels = showLabels)

@Composable
fun MiniFabItem(
    item: MultiFabItem,
    showLabel: Boolean,
    miniFabColor: Color,
    onFabItemClicked: (item: MultiFabItem) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        if (showLabel) {
            Text(
                item.label,
                fontSize = 12.sp,
//                color = item.labelColor,
                modifier = Modifier
                    .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        FloatingActionButton(
            modifier = Modifier.size(40.dp),
            onClick = { onFabItemClicked(item) },
            containerColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = "multifab ${item.label}",
                tint = miniFabColor
            )
        }
    }
}

@Composable
fun MultiFloatingActionButton(
    fabIcon: FabIcon,
    fabTitle: String?,
    showFabTitle: Boolean,
    modifier: Modifier = Modifier,
    itemsMultiFab: List<MultiFabItem>,
    fabState: MutableState<MultiFabState> = rememberMultiFabState(),
    fabOption: FabOption = FabOption(),
    onFabItemClicked: (fabItem: MultiFabItem) -> Unit,
    stateChanged: (fabState: MultiFabState) -> Unit = {},
    onShowTransparent: () -> Unit = {}
) {
    val rotation by animateFloatAsState(
        if (fabState.value == MultiFabState.Expanded) fabIcon.iconRotate ?: 0f else 0f, label = ""
    )
    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = fabState.value == MultiFabState.Expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut()
        ) {
            LazyColumn(
                modifier = Modifier.wrapContentSize(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                itemsIndexed(itemsMultiFab) { _, item ->
                    MiniFabItem(
                        item = item,
                        showLabel = fabOption.showLabels,
                        miniFabColor = Theme,
                        onFabItemClicked = { onFabItemClicked(item) })
                }
                item {}
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fabState.value.isExpanded() && showFabTitle)
                Text(text = fabTitle!!, modifier = Modifier.padding(end = 16.dp), fontSize = 12.sp)
            FloatingActionButton(
                onClick = {
                    fabState.value = fabState.value.toggleValue()
                    stateChanged(fabState.value)
                    onShowTransparent()
                   // TransparentClipLayout(modifier = Modifier.fillMaxSize(), width = 0.dp, height =0.dp , offsetY =0.dp )
                },
                containerColor = fabOption.backgroundTint,
                contentColor = fabOption.iconTint,
                shape = CircleShape
            ) {
                Icon(
                    painter =
                    if (fabState.value.isExpanded()) painterResource(fabIcon.iconResAfterRotate)
                    else painterResource(fabIcon.iconRes),
                    modifier = Modifier.rotate(rotation),
                    contentDescription = null,
                    tint = fabOption.iconTint
                )
            }
        }
    }
}
//
//@Composable
//fun TransparentClipLayout(
//    modifier: Modifier,
//    width: Dp,
//    height: Dp,
//    offsetY: Dp
//) {
//    val offsetInPx: Float
//    val widthInPx: Float
//    val heightInPx: Float
//
//    with(LocalDensity.current) {
//        offsetInPx = offsetY.toPx()
//        widthInPx = width.toPx()
//        heightInPx = height.toPx()
//    }
//
//    Canvas(modifier = modifier) {
//
//        val canvasWidth = size.width
//
//        with(drawContext.canvas.nativeCanvas) {
//            val checkPoint = saveLayer(null, null)
//
//            // Destination
//            drawRect(Color(0x77000000))
//
//            // Source
//            drawRoundRect(
//                topLeft = Offset(
//                    x = (canvasWidth - widthInPx) / 2,
//                    y = offsetInPx
//                ),
//                size = Size(widthInPx, heightInPx),
//                cornerRadius = CornerRadius(30f,30f),
//                color = Color.Transparent,
//                blendMode = BlendMode.Clear
//            )
//            restoreToCount(checkPoint)
//        }
//
//    }
//}


