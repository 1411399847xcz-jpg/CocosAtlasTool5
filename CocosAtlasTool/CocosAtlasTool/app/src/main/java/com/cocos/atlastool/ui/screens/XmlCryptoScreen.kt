package com.cocos.atlastool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocos.atlastool.ui.viewmodel.MainViewModel
import com.cocos.atlastool.ui.viewmodel.XmlOperation

/**
 * XML 加解密屏幕
 *
 * 提供四个操作卡片：
 * 1. 单个加密
 * 2. 单个解密
 * 3. 批量加密
 * 4. 批量解密
 *
 * 点击卡片后进入对应流程：选择文件 -> 选择输出目录 -> 执行操作 -> 显示结果。
 *
 * @param onBack 返回上一页的回调
 * @param onOperation 选择操作类型的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XmlCryptoScreen(
    onBack: () -> Unit,
    onOperation: (XmlOperation) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val xmlState by viewModel.xmlCryptoState.collectAsState()

    // 四种操作卡片数据
    data class OperationCard(
        val operation: XmlOperation,
        val title: String,
        val description: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    )

    val operations = listOf(
        OperationCard(
            operation = XmlOperation.ENCRYPT_SINGLE,
            title = "单个加密",
            description = "选择一个 XML 文件进行加密处理",
            icon = Icons.Default.Lock
        ),
        OperationCard(
            operation = XmlOperation.DECRYPT_SINGLE,
            title = "单个解密",
            description = "选择一个加密的 XML 文件进行解密",
            icon = Icons.Default.LockOpen
        ),
        OperationCard(
            operation = XmlOperation.ENCRYPT_BATCH,
            title = "批量加密",
            description = "选择文件夹，批量加密其中所有 XML 文件",
            icon = Icons.Default.Lock
        ),
        OperationCard(
            operation = XmlOperation.DECRYPT_BATCH,
            title = "批量解密",
            description = "选择文件夹，批量解密其中所有 XML 文件",
            icon = Icons.Default.LockOpen
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "XML 加解密") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 说明文字
            Text(
                text = "选择需要执行的操作类型",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 四个操作卡片网格（2列）
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(operations) { op ->
                    Card(
                        onClick = {
                            viewModel.updateXmlCryptoState { it.copy(operation = op.operation) }
                            onOperation(op.operation)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = op.icon,
                                contentDescription = op.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = op.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = op.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
