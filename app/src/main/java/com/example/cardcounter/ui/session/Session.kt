package com.example.cardcounter.ui.session

import android.content.Context
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardcounter.capture.CaptureController
import com.example.cardcounter.core.ActiveSession
import com.example.cardcounter.data.engine.CountingEngine
import com.example.cardcounter.data.engine.EngineSnapshot
import com.example.cardcounter.data.engine.toSnapshot
import com.example.cardcounter.data.model.GameConfig
import com.example.cardcounter.data.model.Rank
import com.example.cardcounter.data.presets.GamePresets
import com.example.cardcounter.ui.components.CounterGrid
import com.example.cardcounter.ui.components.PlayerChips
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionViewModel(private val config: GameConfig) : ViewModel() {

    private val engine = CountingEngine(config)

    private val _snapshot = MutableStateFlow(buildSnapshot())
    val snapshot: StateFlow<EngineSnapshot> = _snapshot.asStateFlow()

    var countingStarted by mutableStateOf(false)
        private set

    var selectedPlayer: Int? by mutableStateOf(null)
        private set

    fun selectPlayer(index: Int?) { selectedPlayer = index }

    fun startCounting() {
        countingStarted = true
        ActiveSession.startCounting()
    }

    fun recordRank(rank: Rank) {
        if (!countingStarted) return
        engine.recordByRank(rank, selectedPlayer)
        emit()
    }

    fun takeEgg(fourCount: Int, takenRank: Rank) {
        if (!countingStarted) return
        engine.takeEggWithFours(fourCount, takenRank)
        emit()
    }

    fun undo() { engine.undo(); emit() }
    fun reset() { engine.reset(); emit() }

    fun stopAutoCapture(context: Context) {
        ActiveSession.endSession()
        CaptureController.stopCapture(context)
        CaptureController.hideOverlay(context)
    }

    private fun emit() { _snapshot.value = buildSnapshot() }

    private fun buildSnapshot(): EngineSnapshot = engine.toSnapshot()
}

@Composable
fun SessionScreen(
    onBack: () -> Unit,
    onOpenRoi: () -> Unit = {},
    onOpenTuning: () -> Unit = {}
) {
    val config = GamePresets.DADAA
    val vm: SessionViewModel = viewModel { SessionViewModel(config) }
    val snap by vm.snapshot.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        TopBar(onBack)

        if (!vm.countingStarted) {
            Text("确定大A后，点击右侧【开始】按钮开始记牌", color = Color.Red, fontSize = 13.sp)
        } else {
            Text("剩余未出：${snap.unseenTotal} 张", color = Color.Black, fontSize = 14.sp)
        }

        Spacer(Modifier.height(4.dp))

        PlayerChips(
            playerCount = config.playerCount,
            selected = vm.selectedPlayer,
            handSizes = snap.playerHandSizes,
            onSelect = vm::selectPlayer
        )

        Spacer(Modifier.height(4.dp))

        CounterGrid(
            order = config.gameType.displayOrder(),
            remaining = snap.remainingByRank,
            started = vm.countingStarted,
            onRecord = vm::recordRank,
            onSettings = onOpenTuning,
            onReset = vm::reset,
            onStart = vm::startCounting
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TakeEggSection(vm::takeEgg)
            AutoCaptureSection(config, vm, onOpenRoi)
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("← 返回", color = Color.Black) }
        Text("打大A", color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TakeEggSection(onTakeEgg: (Int, Rank) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showDialog = true }) { Text("4取蛋子", fontSize = 13.sp) }

    if (showDialog) {
        TakeEggDialog(
            onDismiss = { showDialog = false },
            onConfirm = { fourCount, rank ->
                onTakeEgg(fourCount, rank)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TakeEggDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Rank) -> Unit
) {
    var fourCount by remember { mutableStateOf(2) }
    var takenRank by remember { mutableStateOf(Rank.TWO) }

    val ranks = listOf(
        Rank.THREE, Rank.TWO, Rank.ACE, Rank.KING, Rank.QUEEN,
        Rank.JACK, Rank.TEN, Rank.NINE, Rank.EIGHT, Rank.SEVEN,
        Rank.SIX, Rank.FIVE, Rank.FOUR
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("4取蛋子") },
        text = {
            Column {
                Text("用几个4（取回张数 = 4数 + 1）")
                Row {
                    listOf(2, 3, 4, 5).forEach { n ->
                        FilterChip(
                            selected = fourCount == n,
                            onClick = { fourCount = n },
                            label = { Text("$n 个4") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("被取的点数（蛋子/大蛋子）")
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    ranks.forEach { r ->
                        FilterChip(
                            selected = takenRank == r,
                            onClick = { takenRank = r },
                            label = { Text(r.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(fourCount, takenRank) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AutoCaptureSection(
    config: GameConfig,
    vm: SessionViewModel,
    onOpenRoi: () -> Unit
) {
    val context = LocalContext.current
    var autoRunning by remember { mutableStateOf(false) }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            ActiveSession.start(config)
            CaptureController.startCapture(context, result.resultCode, result.data!!)
            CaptureController.showOverlay(context)
            autoRunning = true
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            enabled = !autoRunning,
            onClick = {
                if (!CaptureController.hasOverlayPermission(context)) {
                    CaptureController.openOverlayPermissionSettings(context)
                } else {
                    val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                            as MediaProjectionManager
                    projectionLauncher.launch(mpm.createScreenCaptureIntent())
                }
            }
        ) { Text(if (autoRunning) "识别中…" else "开始自动识别", fontSize = 13.sp) }

        if (autoRunning) {
            OutlinedButton(onClick = { vm.stopAutoCapture(context); autoRunning = false }) {
                Text("停止", fontSize = 13.sp)
            }
            OutlinedButton(onClick = onOpenRoi) { Text("设置识别区域", fontSize = 13.sp) }
        }
    }
}
