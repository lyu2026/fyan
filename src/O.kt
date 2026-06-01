package com.fyan

import android.app.Activity
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.view.WindowCompat
import androidx.navigation.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class LogItem(
    val id: String = UUID.randomUUID().toString(),
    val time: String,
    val target: String,
    val message: String,
    val type: LogType
)

enum class LogType { SUCCESS, ERROR, WARNING, INFO }

class O : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDark = isSystemInDarkTheme()
            val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }
            }
            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainSpaContainer()
                }
            }
        }
    }
}

@Composable
fun MainSpaContainer() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showLogPanelGlobal by remember { mutableStateOf(true) }
    val logList = remember { mutableStateListOf<LogItem>() }

    fun appendLog(target: String, message: String, type: LogType) {
        val timeStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logList.add(LogItem(time = timeStamp, target = target, message = message, type = type))
    }

    LaunchedEffect(Unit) {
        appendLog("系统引擎", "孚琰 原生双端内核架构初始化就绪", LogType.SUCCESS)
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    if (currentRoute == "home") {
        var showExitDialog by remember { mutableStateOf(false) }
        BackHandler(enabled = true) { showExitDialog = true }
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("提示") },
                text = { Text("确定要彻底退出应用吗？") },
                confirmButton = { TextButton(onClick = { (context as? Activity)?.finish() }) { Text("确认退出") } },
                dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("取消") } }
            )
        }
    }

    val config = LocalConfiguration.current
    val isTV = (config.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    isTV = isTV,
                    showLog = showLogPanelGlobal,
                    onToggleLog = { 
                        showLogPanelGlobal = it
                        appendLog("界面设置", "全局消息面板开关变更为: $it", LogType.INFO)
                    },
                    onTriggerLogMock = { appendLog("模拟器", "手动触发高亮警告级系统排查事件！", LogType.WARNING) },
                    onNavigate = { route ->
                        appendLog("路由导航", "正准备切换至原生页面: [$route]", LogType.SUCCESS)
                        navController.navigate(route)
                    }
                )
            }
            composable("setting") {
                SettingScreen(
                    onBack = { 
                        appendLog("路由导航", "从设置面板安全滑回主控制台", LogType.INFO)
                        navController.popBackStack() 
                    },
                    onSaveAction = { key, value -> appendLog("存储引擎", "配置项 [$key] 自动保存成功: $value", LogType.SUCCESS) }
                )
            }
        }
        if (showLogPanelGlobal) {
            FloatingLogPanel(
                modifier = Modifier.align(Alignment.BottomCenter), 
                logs = logList, 
                onDelete = { id -> logList.removeAll { it.id == id } }
            )
        }
    }
}

@Composable
fun HomeScreen(isTV: Boolean, showLog: Boolean, onToggleLog: (Boolean) -> Unit, onTriggerLogMock: () -> Unit, onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().height(48.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("孚琰 控制台", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (isTV) "电视" else "手机", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 4.dp))
                IconButton(onClick = { onToggleLog(!showLog) }, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = if (showLog) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            MuiCard(title = "自动化参数设置", desc = "内置无缝响应式卡片、表单策略与持久化管理", onClick = { onNavigate("setting") })
            Spacer(modifier = Modifier.height(8.dp))
            MuiCard(title = "手动投递诊断日志", desc = "向贴底面板追加一条模拟警告事件进行视图验证", onClick = onTriggerLogMock)
        }
    }
}

@Composable
fun MuiCard(title: String, desc: String, onClick: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Card(
        modifier = Modifier.fillMaxWidth().height(68.dp).focusRequester(focusRequester).focusable(interactionSource = interactionSource).shadow(if (isFocused) 6.dp else 1.dp, RoundedCornerShape(8.dp)).clickable { onClick() }.border(width = 1.5.dp, color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

@Composable
fun SettingScreen(onBack: () -> Unit, onSaveAction: (String, String) -> Unit) {
    var isExpanded by remember { mutableStateOf(true) }
    var configText by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(48.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp)) }
            Text("系统配置", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("核心参数联动区", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(36.dp)) { Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null) }
                }
                if (isExpanded) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    OutlinedTextField(value = configText, onValueChange = { configText = it; onSaveAction("API_ENDPOINT", it) }, label = { Text("云端接入端点 (自动保存)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
        }
    }
}

@Composable
fun FloatingLogPanel(modifier: Modifier = Modifier, logs: List<LogItem>, onDelete: (String) -> Unit) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    var offsetY by remember { mutableStateOf(0f) }
    var isCollapsed by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) lazyListState.animateScrollToItem(logs.size - 1) }
    if (!isCollapsed) {
        Box(modifier = modifier.fillMaxWidth().height(screenHeight / 3).navigationBarsPadding().offset { IntOffset(0, offsetY.roundToInt()) }.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)).border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).pointerInput(Unit) {
            detectDragGestures(onDragEnd = { offsetY = 0f }, onDrag = { change, dragAmount -> change.consume(); if (offsetY + dragAmount.y >= 0) offsetY += dragAmount.y })
        }) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Box(modifier = Modifier.width(32.dp).height(3.dp).background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(1.5.dp)).align(Alignment.CenterHorizontally))
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    items(logs) { log ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            val logTextColor = when (log.type) {
                                LogType.SUCCESS -> Color(0xFF189B46)
                                LogType.ERROR -> Color(0xFFE7012F)
                                LogType.WARNING -> Color(0xFFFDD10D)
                                LogType.INFO -> MaterialTheme.colorScheme.onSurface
                            }
                            val textModifier = Modifier.weight(1f).padding(PaddingValues(end = 4.dp))
                            Text(text = "[${log.time}] ${log.target} ➜ ${log.message}", color = logTextColor, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 1.2.em), modifier = textModifier)
                            IconButton(onClick = { onDelete(log.id) }, modifier = Modifier.size(16.dp).align(Alignment.CenterVertically)) { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Gray.copy(alpha = 0.7f), modifier = Modifier.size(12.dp)) }
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.08f))
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier.padding(bottom = 6.dp).navigationBarsPadding().width(80.dp).height(5.dp).background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.5.dp)).clickable { isCollapsed = false })
    }
}
