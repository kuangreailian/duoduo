package com.example.cardcounter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardcounter.data.model.Rank

/**
 * 记牌器核心网格：
 * 一行横向滚动，依次为 设置 | 大王 小王 A K Q J 10 9 8 7 6 5 4 3 2 | 重置 | 开始
 * 每个点数格子分两行：上行牌面，下行剩余数。
 */
@Composable
fun CounterGrid(
    order: List<Rank>,
    remaining: Map<Rank, Int>,
    started: Boolean,
    onRecord: (Rank) -> Unit,
    onSettings: () -> Unit,
    onReset: () -> Unit,
    onStart: () -> Unit,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        GridActionButton("设置", compact, onClick = onSettings)
        order.forEach { rank ->
            RankCell(rank, remaining[rank] ?: 0, compact, onRecord)
        }
        GridActionButton("重置", compact, onClick = onReset)
        GridActionButton("开始", compact, enabled = !started, onClick = onStart)
    }
}

@Composable
private fun RankCell(
    rank: Rank,
    count: Int,
    compact: Boolean,
    onRecord: (Rank) -> Unit
) {
    val width = if (compact) 22.dp else 26.dp
    Column(
        modifier = Modifier
            .width(width)
            .clickable(enabled = count > 0) { onRecord(rank) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = rank.label,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (rank == Rank.BIG_JOKER || rank == Rank.SMALL_JOKER) Color.Red else Color.Black
        )
        Text(
            text = "$count",
            fontSize = if (compact) 11.sp else 12.sp,
            color = Color.Red
        )
    }
}

@Composable
private fun GridActionButton(
    label: String,
    compact: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(if (compact) 38.dp else 48.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50),
            disabledContainerColor = Color(0xFFBDBDBD)
        )
    ) {
        Text(label, fontSize = if (compact) 10.sp else 12.sp, color = Color.White)
    }
}

/**
 * 玩家选择条：未知 + 玩家1..N，选中绿色高亮，下方显示该玩家剩余手牌数。
 */
@Composable
fun PlayerChips(
    playerCount: Int,
    selected: Int?,
    handSizes: List<Int>,
    onSelect: (Int?) -> Unit,
    compact: Boolean = false,
    labels: List<String> = emptyList()
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (compact) 22.dp else 28.dp)
                    .background(if (selected == null) Color(0xFF4CAF50) else Color(0xFFE0E0E0), CircleShape)
                    .clickable { onSelect(null) },
                contentAlignment = Alignment.Center
            ) {
                Text("?", color = if (selected == null) Color.White else Color.Black,
                    fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Bold)
            }
            Text("未知", fontSize = if (compact) 8.sp else 10.sp, color = Color.Black)
        }
        repeat(playerCount) { p ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val label = labels.getOrNull(p) ?: "${p + 1}"
                Box(
                    modifier = Modifier
                        .size(if (compact) 22.dp else 28.dp)
                        .background(if (selected == p) Color(0xFF4CAF50) else Color(0xFFE0E0E0), CircleShape)
                        .clickable { onSelect(p) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (selected == p) Color.White else Color.Black,
                        fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("${handSizes.getOrNull(p) ?: 0}", fontSize = if (compact) 9.sp else 11.sp, color = Color.Red)
            }
        }
    }
}
