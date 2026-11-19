package com.example.cardcounter.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardcounter.core.ActiveSession
import com.example.cardcounter.data.model.GameType
import com.example.cardcounter.data.model.Rank
import com.example.cardcounter.ui.components.CounterGrid
import com.example.cardcounter.ui.components.PlayerChips
import com.example.cardcounter.ui.theme.CardCounterTheme
import kotlinx.coroutines.delay

class CountOverlayService : Service() {

    companion object {
        @Volatile var instance: CountOverlayService? = null
    }

    private lateinit var wm: WindowManager
    private var view: View? = null
    private var lp: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val composeView = ComposeView(this).apply {
            setContent { CardCounterTheme { OverlayPanel() } }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
        }

        wm.addView(composeView, params)
        view = composeView
        lp = params
    }

    fun moveBy(dx: Int, dy: Int) {
        lp?.let {
            it.x += dx
            it.y += dy
            runCatching { wm.updateViewLayout(view, it) }
        }
    }

    override fun onDestroy() {
        instance = null
        view?.let { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private val Green = Color(0xFF4CAF50)
private val Red = Color(0xFFCD3538)

@Composable
fun OverlayPanel() {
    val snapState by ActiveSession.snapshot.collectAsStateWithLifecycle()
    val snap = snapState
    val started by ActiveSession.countingStarted.collectAsStateWithLifecycle()
    val selectedPlayer by ActiveSession.selectedPlayer.collectAsStateWithLifecycle()
    val order = ActiveSession.config?.gameType?.displayOrder() ?: GameType.DADAA.displayOrder()

    var expanded by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var takeEggOpen by remember { mutableStateOf(false) }

    LaunchedEffect(hidden) {
        if (hidden) {
            delay(5000)
            hidden = false
        }
    }

    if (hidden) return

    if (!expanded) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Green, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { expanded = true })
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        CountOverlayService.instance?.moveBy(drag.x.toInt(), drag.y.toInt())
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("记", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        return
    }

    // 横屏横向长条悬浮窗
    Surface(
        modifier = Modifier.width(560.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(6.dp)) {
            // 标题栏（可拖动）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            CountOverlayService.instance?.moveBy(drag.x.toInt(), drag.y.toInt())
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (snap == null) "记牌" else "剩余 ${snap.unseenTotal} 张",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = ActiveSession::undo) { Text("撤销", fontSize = 12.sp) }
                TextButton(onClick = { takeEggOpen = !takeEggOpen }) { Text("4取蛋", fontSize = 12.sp) }
            }

            if (settingsOpen) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = false; settingsOpen = false }) {
                        Text("缩小悬浮窗", fontSize = 13.sp)
                    }
                    TextButton(onClick = { hidden = true; settingsOpen = false }) {
                        Text("消失5秒", fontSize = 13.sp)
                    }
                }
            }

            if (takeEggOpen) {
                TakeEggInline(onConfirm = { fc, r -> ActiveSession.takeEgg(fc, r); takeEggOpen = false })
            }

            if (snap == null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = ActiveSession::startCounting,
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) { Text("开始记牌", fontSize = 13.sp) }
            }

            if (snap != null && started) {
                Spacer(Modifier.height(4.dp))
                PlayerChips(
                    playerCount = ActiveSession.config?.playerCount ?: 5,
                    selected = selectedPlayer,
                    handSizes = snap.playerHandSizes,
                    onSelect = ActiveSession::selectPlayer,
                    compact = true,
                    labels = ActiveSession.config?.gameType?.seatLabels()
                        ?: GameType.DADAA.seatLabels()
                )
                Spacer(Modifier.height(4.dp))
                CounterGrid(
                    order = order,
                    remaining = snap.remainingByRank,
                    started = started,
                    onRecord = ActiveSession::recordRank,
                    onSettings = { settingsOpen = !settingsOpen },
                    onReset = ActiveSession::reset,
                    onStart = ActiveSession::startCounting,
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun TakeEggInline(onConfirm: (Int, Rank) -> Unit) {
    var fourCount by remember { mutableStateOf(2) }
    var takenRank by remember { mutableStateOf(Rank.TWO) }
    val ranks = listOf(
        Rank.THREE, Rank.TWO, Rank.ACE, Rank.KING, Rank.QUEEN,
        Rank.JACK, Rank.TEN, Rank.NINE, Rank.EIGHT, Rank.SEVEN,
        Rank.SIX, Rank.FIVE, Rank.FOUR
    )

    Column {
        Row {
            listOf(2, 3, 4, 5).forEach { n ->
                Text(
                    text = "${n}个4",
                    modifier = Modifier.clickable { fourCount = n }.padding(4.dp),
                    color = if (fourCount == n) Green else Color.Black,
                    fontSize = 12.sp
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            ranks.forEach { r ->
                Text(
                    text = r.label,
                    modifier = Modifier.clickable { takenRank = r }.padding(4.dp),
                    color = if (takenRank == r) Green else Color.Black,
                    fontSize = 12.sp
                )
            }
        }
        Text(
            text = "确定取蛋",
            modifier = Modifier.clickable { onConfirm(fourCount, takenRank) }.padding(4.dp),
            color = Green,
            fontSize = 12.sp
        )
    }
}
