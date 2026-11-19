package com.example.cardcounter.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onOpenTuning: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("扑克记牌器", style = MaterialTheme.typography.headlineMedium)
        Text("打大A记牌器")

        Card(Modifier.fillMaxWidth(), onClick = onStart) {
            Column(Modifier.padding(20.dp)) {
                Text("打大A", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("108张 · 5人", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onOpenTuning) { Text("识别设置") }
        }
    }
}
