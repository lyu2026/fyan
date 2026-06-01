package com.fyan

import android.app.Activity
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// 1. 日志模型定义
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
			// 全局联动系统深色/浅色模式切换
			val isDark = isSystemInDarkTheme()
			MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
				Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
					MainSpaContainer()
				}
			}
		}
	}
}

// 2. SPA 多页面核心总调度器
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

	// 首次初始化投递一条系统就绪日志
	LaunchedEffect(Unit) {
		appendLog("系统引擎", "Fyan 纯原生双端内核架构初始化就绪", LogType.SUCCESS)
	}

	// 拦截返回键：只有在首页时执行二次退出弹窗
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
				confirmButton = {
					TextButton(onClick = { (context as? Activity)?.finish() }) { Text("确认退出") }
				},
				dismissButton = {
					TextButton(onClick = { showExitDialog = false }) { Text("取消") }
				}
			)
		}
	}

	val config = LocalConfiguration.current
	val isTV = (config.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

	Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
		NavHost(navController = navController, startDestination = "home") {
			composable("home") {
				HomeScreen(
					isTV = isTV,
					showLog = showLogPanelGlobal,
					onToggleLog = { 
						showLogPanelGlobal = it
						appendLog("界面设置", "全局消息面板开关变更为: $it", LogType.INFO)
					},
					onTriggerLogMock = {
						appendLog("模拟器", "手动触发高亮警告级系统排查事件！", LogType.WARNING)
					},
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
					onSaveAction = { key, value ->
						appendLog("存储引擎", "配置项 [$key] 自动保存成功: $value", LogType.SUCCESS)
					}
				)
			}
		}

		// 全局消息日志控制台
		if (showLogPanelGlobal) {
			FloatingLogPanel(
				modifier = Modifier.align(Alignment.BottomCenter), 
				logs = logList, 
				onDelete = { id -> logList.removeAll { it.id == id } }
			)
		}
	}
}

// 3. 首页视窗
@Composable
fun HomeScreen(
	isTV: Boolean,
	showLog: Boolean,
	onToggleLog: (Boolean) -> Unit,
	onTriggerLogMock: () -> Unit,
	onNavigate: (String) -> Unit
) {
	Column(modifier = Modifier.fillMaxSize()) {
		Row(
			modifier = Modifier.fillMaxWidth().height(56.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Text("Fyan 双端总控制台", style = MaterialTheme.typography.titleLarge)
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = if (isTV) "📺 智能电视端" else "📱 移动手机端", 
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.padding(end = 8.dp)
				)
				IconButton(onClick = { onToggleLog(!showLog) }) {
					Icon(imageVector = if (showLog) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
				}
			}
		}

		Spacer(modifier = Modifier.height(16.dp))

		// Mui 特效卡片
		MuiCard(title = "自动化参数设置", desc = "内置无缝响应式卡片、表单策略与持久化管理", onClick = { onNavigate("setting") })
		Spacer(modifier = Modifier.height(12.dp))
		MuiCard(title = "手动投递诊断日志", desc = "向贴底面板追加一条模拟警告事件进行视图验证", onClick = onTriggerLogMock)
	}
}

// 4. Mui 高级质感卡片组件（完美支持手机触控阴影与电视遥控器聚焦高亮边框）
@Composable
fun MuiCard(title: String, desc: String, onClick: () -> Unit) {
	val focusRequester = remember { FocusRequester() }
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.height(76.dp)
			.focusRequester(focusRequester)
			.focusable(interactionSource = interactionSource)
			.shadow(if (isFocused) 8.dp else 2.dp, RoundedCornerShape(12.dp))
			.clickable { onClick() }
			.border(
				width = 2.dp, 
				color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent, 
				shape = RoundedCornerShape(12.dp)
			),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
	) {
		Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) {
			Column {
				Text(title, style = MaterialTheme.typography.titleMedium)
				Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
			}
		}
	}
}

// 5. 设置页视窗（大折叠表单，自动编辑保存逻辑）
@Composable
fun SettingScreen(onBack: () -> Unit, onSaveAction: (String, String) -> Unit) {
	var isExpanded by remember { mutableStateOf(true) }
	var configText by remember { mutableStateOf("") }

	Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
		Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(56.dp)) {
			IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null) }
			Text("系统级参数面板", style = MaterialTheme.typography.titleLarge)
		}

		Spacer(modifier = Modifier.height(8.dp))

		Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
			Column(modifier = Modifier.padding(12.dp)) {
				Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
					Text("核心参数联动区", style = MaterialTheme.typography.titleMedium)
					IconButton(onClick = { isExpanded = !isExpanded }) {
						Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
					}
				}
				if (isExpanded) {
					HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
					OutlinedTextField(
						value = configText,
						onValueChange = { 
							configText = it
							// 每当用户输入改变，即时回调触发自动保存事件日志
							onSaveAction("API_ENDPOINT", it)
						},
						label = { Text("云端接入端点 (修改时自动保存)") },
						modifier = Modifier.fillMaxWidth(),
						singleLine = true
					)
				}
			}
		}
	}
}

// 6. 全自动拖拽到底折叠成横线、毛玻璃渲染的高颜值日志控制台
@Composable
fun FloatingLogPanel(modifier: Modifier = Modifier, logs: List<LogItem>, onDelete: (String) -> Unit) {
	val configuration = LocalConfiguration.current
	val screenHeight = configuration.screenHeightDp.dp
	val maxHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { (screenHeight / 3).toPx() }
	
	var offsetY by remember { mutableStateOf(0f) }
	var isCollapsed by remember { mutableStateOf(false) }
	val lazyListState = rememberLazyListState()

	// 动态触底滚动：最新消息永远保持在最底部可见区域
	LaunchedEffect(logs.size) {
		if (logs.isNotEmpty()) {
			lazyListState.animateScrollToItem(logs.size - 1)
		}
	}

	if (!isCollapsed) {
		Box(
			modifier = modifier
				.fillMaxWidth()
				.height(screenHeight / 3)
				.offset { IntOffset(0, offsetY.roundToInt()) }
				.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
				// 毛玻璃混合半透明层
				.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
				.border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
				.pointerInput(Unit) {
					detectDragGestures(
						onDragEnd = {
							// 拖拽幅度超过阈值，直接下滑隐退为贴底细线
							if (offsetY > maxHeightPx * 0.5f) {
								isCollapsed = true
							}
							offsetY = 0f
						},
						onDrag = { change, dragAmount ->
							change.consume()
							if (offsetY + dragAmount.y >= 0) {
								offsetY += dragAmount.y
							}
						}
					)
				}
		) {
			Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
				// 面板顶部的防呆可拉拽手柄指示线
				Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
				Spacer(modifier = Modifier.height(8.dp))
				
				LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
					items(logs) { log ->
						Row(
							modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
							horizontalArrangement = Arrangement.SpaceBetween, 
							verticalAlignment = Alignment.CenterVertically
						) {
							// 精准映射：成功（绿）、错误（红）、警告（黄）、常规（默认色）
							val logTextColor = when (log.type) {
								LogType.SUCCESS -> Color(0xFF189B46)
								LogType.ERROR -> Color(0xFFE7012F)
								LogType.WARNING -> Color(0xFFFDD10D)
								LogType.INFO -> MaterialTheme.colorScheme.onSurface
							}
							Text(
								text = "[${log.time}] ${log.target} -> ${log.message}",
								color = logTextColor,
								style = MaterialTheme.typography.bodySmall,
								modifier = Modifier.weight(1f)
							)
							IconButton(onClick = { onDelete(log.id) }, modifier = Modifier.size(24.dp)) {
								Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
							}
						}
						HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
					}
				}
			}
		}
	} else {
		// 完全隐藏后在极底化为细长的横线小胶囊，支持点击唤醒复原
		Box(
			modifier = modifier
				.padding(bottom = 4.dp)
				.width(110.dp)
				.height(6.dp)
				.background(Color.Gray.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
				.clickable { isCollapsed = false }
		)
	}
}
