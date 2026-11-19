package com.example.cardcounter.ui.tuning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cardcounter.capture.TuningConfig

@Composable
fun TuningScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tuning by remember { mutableStateOf(TuningConfig.load(context)) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 返回") }
        Text("识别设置", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        SliderRow("帧间隔(ms)", tuning.frameIntervalMs.toFloat(), 200f..2000f) {
            tuning = tuning.copy(frameIntervalMs = it.toLong())
        }
        SliderRow("置信度阈值", tuning.minConfidence, 0.3f..0.95f) {
            tuning = tuning.copy(minConfidence = it)
        }
        SliderRow("去重窗口(ms)", tuning.dedupWindowMs.toFloat(), 500f..5000f) {
            tuning = tuning.copy(dedupWindowMs = it.toLong())
        }
        SliderRow("位置容差(px)", tuning.positionTolerancePx, 40f..300f) {
            tuning = tuning.copy(positionTolerancePx = it)
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = { TuningConfig.save(context, tuning); onBack() }) { Text("保存") }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text("$label：${(value * 10).toInt() / 10f}")
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
