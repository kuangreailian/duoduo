package com.example.cardcounter.ui.roi

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.cardcounter.capture.Roi
import com.example.cardcounter.capture.RoiConfig
import com.example.cardcounter.core.LatestFrameHolder

@Composable
fun RoiSelectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val bitmap = LatestFrameHolder.latestBitmap

    if (bitmap == null) {
        Column(Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("暂无画面")
            Spacer(Modifier.height(8.dp))
            Text("请先开始自动识别，再进入区域设置")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("返回") }
        }
        return
    }

    var rect by remember { mutableStateOf(Rect(0.05f, 0.2f, 0.95f, 0.8f)) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("框选牌面识别区域", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp))) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            RoiSelector(rect = rect, onRectChange = { rect = it })
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val roi = toPixelRoi(bitmap, rect)
                RoiConfig.save(context, roi)
                onBack()
            }) { Text("保存") }
            OutlinedButton(onClick = onBack) { Text("取消") }
        }
    }
}

private fun toPixelRoi(bmp: Bitmap, rect: Rect): Roi = Roi(
    left = (rect.left * bmp.width).toInt().coerceIn(0, bmp.width - 1),
    top = (rect.top * bmp.height).toInt().coerceIn(0, bmp.height - 1),
    width = ((rect.right - rect.left) * bmp.width).toInt().coerceAtLeast(1),
    height = ((rect.bottom - rect.top) * bmp.height).toInt().coerceAtLeast(1)
)

private const val HANDLE_DP = 40

@Composable
fun RoiSelector(
    modifier: Modifier = Modifier,
    rect: Rect,
    onRectChange: (Rect) -> Unit
) {
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        fun update(l: Float, t: Float, r: Float, b: Float) {
            val nl = l.coerceIn(0f, 1f)
            val nt = t.coerceIn(0f, 1f)
            val nr = r.coerceIn(0f, 1f)
            val nb = b.coerceIn(0f, 1f)
            if (nr - nl > 0.02f && nb - nt > 0.02f) {
                onRectChange(Rect(nl, nt, nr, nb))
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val l = rect.left * size.width
            val t = rect.top * size.height
            val r = rect.right * size.width
            val b = rect.bottom * size.height

            drawRect(Color.Black.copy(alpha = 0.45f), topLeft = Offset(0f, 0f), size = Size(size.width, t))
            drawRect(Color.Black.copy(alpha = 0.45f), topLeft = Offset(0f, t), size = Size(l, b - t))
            drawRect(Color.Black.copy(alpha = 0.45f), topLeft = Offset(r, t), size = Size(size.width - r, b - t))
            drawRect(Color.Black.copy(alpha = 0.45f), topLeft = Offset(0f, b), size = Size(size.width, size.height - b))

            drawRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(l, t),
                size = Size(r - l, b - t),
                style = Stroke(width = 4f)
            )
        }

        RoiHandle(Alignment.TopStart, rect, w, h) { dx, dy -> update(rect.left + dx, rect.top + dy, rect.right, rect.bottom) }
        RoiHandle(Alignment.TopEnd, rect, w, h) { dx, dy -> update(rect.left, rect.top + dy, rect.right + dx, rect.bottom) }
        RoiHandle(Alignment.BottomStart, rect, w, h) { dx, dy -> update(rect.left + dx, rect.top, rect.right, rect.bottom + dy) }
        RoiHandle(Alignment.BottomEnd, rect, w, h) { dx, dy -> update(rect.left, rect.top, rect.right + dx, rect.bottom + dy) }

        val density = LocalDensity.current
        Box(
            Modifier
                .offset { IntOffset((rect.left * w).toInt(), (rect.top * h).toInt()) }
                .size(
                    width = with(density) { ((rect.right - rect.left) * w).toDp() },
                    height = with(density) { ((rect.bottom - rect.top) * h).toDp() }
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        update(
                            rect.left + drag.x / w,
                            rect.top + drag.y / h,
                            rect.right + drag.x / w,
                            rect.bottom + drag.y / h
                        )
                    }
                }
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.RoiHandle(
    align: Alignment,
    rect: Rect,
    w: Float,
    h: Float,
    onDrag: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val sizePx = with(density) { HANDLE_DP.dp.toPx() }

    val cx = when (align) {
        Alignment.TopStart, Alignment.BottomStart -> rect.left * w
        else -> rect.right * w
    }
    val cy = when (align) {
        Alignment.TopStart, Alignment.TopEnd -> rect.top * h
        else -> rect.bottom * h
    }

    Box(
        Modifier
            .offset { IntOffset((cx - sizePx / 2).toInt(), (cy - sizePx / 2).toInt()) }
            .size(with(density) { HANDLE_DP.dp })
            .background(Color.White, CircleShape)
            .border(2.dp, Color(0xFF4CAF50), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x / w, drag.y / h)
                }
            }
    )
}
