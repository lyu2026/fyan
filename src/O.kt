package com.fyan // 修复：必须保持全小写

import android.app.Activity
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
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
					MainSpa()
				}
			}
		}
	}
}

@Composable
fun MainSpa() {
	val nav = rememberNavController()
	val ctx = LocalContext.current
	var showLog by remember { mutableStateOf(true) }
	val logs = remember { mutableStateListOf<LogItem>() }

	val addLog = remember {
		{ tag: String, msg: String, type: LogType ->
			val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
			logs.add(LogItem(time = fmt, target = tag, message = msg, type = type))
		}
	}

	LaunchedEffect(Unit) {
		addLog("系统引擎", "孚琰 原生双端内核架构初始化就绪", LogType.SUCCESS)
	}

	val curEntry by nav.currentBackStackEntryAsState()
	if (curEntry?.destination?.route == "home") {
		var showExit by remember { mutableStateOf(false) }
		BackHandler(enabled = true) { showExit = true }
		if (showExit) {
			AlertDialog(
				onDismissRequest = { showExit = false },
				title = { Text("提示") },
				text = { Text("确定要彻底退出应用吗？") },
				confirmButton = { TextButton(onClick = { (ctx as? Activity)?.finish() }) { Text("确认退出") } },
				dismissButton = { TextButton(onClick = { showExit = false }) { Text("取消") } }
			)
		}
	}

	val cfg = LocalConfiguration.current
	val isTV = cfg.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

	Box(modifier = Modifier.fillMaxSize()) {
		NavHost(navController = nav, startDestination = "home") {
			composable("home") {
				Home(
					isTV = isTV,
					showLog = showLog,
					onToggleLog = {
						showLog = it
						addLog("界面设置", "全局消息面板开关变更为: $it", LogType.INFO)
					},
					onMock = { addLog("模拟器", "手动触发高亮警告级系统排查事件！", LogType.WARNING) },
					onNav = { route ->
						addLog("路由导航", "正准备切换至原生页面: [$route]", LogType.SUCCESS)
						nav.navigate(route)
					}
				)
			}
			composable("setting") {
				Setting(
					onBack = {
						addLog("路由导航", "从设置面板安全滑回主控制台", LogType.INFO)
						nav.popBackStack()
					},
					onSave = { k, v -> addLog("存储引擎", "配置项 [$k] 自动保存成功: $v", LogType.SUCCESS) }
				)
			}
		}
		if (showLog) {
			LogPanel(
				modifier = Modifier.align(Alignment.BottomCenter),
				logs = logs,
				onDel = { id -> logs.removeAll { it.id == id } }
			)
		}
	}
}

@Composable
fun Home(isTV: Boolean, showLog: Boolean, onToggleLog: (Boolean) -> Unit, onMock: () -> Unit, onNav: (String) -> Unit) {
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
					Icon(
						painter = painterResource(if (showLog) R.drawable.visibility else R.drawable.visibility_off),
						contentDescription = null,
						modifier = Modifier.size(20.dp)
					)
				}
			}
		}
		Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
			Spacer(modifier = Modifier.height(8.dp))
			CardItem(title = "自动化参数设置", desc = "内置无缝响应式卡片、表单策略与持久化管理", onClick = { onNav("setting") })
			Spacer(modifier = Modifier.height(8.dp))
			CardItem(title = "手动投递诊断日志", desc = "向贴底面板追加一条模拟警告事件进行视图验证", onClick = onMock)
		}
	}
}

@Composable
fun CardItem(title: String, desc: String, onClick: () -> Unit) {
	val req = remember { FocusRequester() }
	val src = remember { MutableInteractionSource() }
	val focused by src.collectIsFocusedAsState()
	
	// 优化：直接利用 Card 固有的 onClick 和规范化的 Modifier 链，确保水波纹和边框严丝合缝
	Card(
		onClick = onClick,
		interactionSource = src,
		modifier = Modifier
			.fillMaxWidth()
			.height(68.dp)
			.focusRequester(req)
			.shadow(if (focused) 6.dp else 1.dp, RoundedCornerShape(8.dp))
			.border(width = 1.5.dp, color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(8.dp)),
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
fun Setting(onBack: () -> Unit, onSave: (String, String) -> Unit) {
	var exp by remember { mutableStateOf(true) }
	var txt by remember { mutableStateOf("") }
	
	Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) {
		Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(48.dp)) {
			IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
				Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = null, modifier = Modifier.size(20.dp))
			}
			Text("系统配置", style = MaterialTheme.typography.titleMedium)
		}
		Spacer(modifier = Modifier.height(4.dp))
		Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
			Column(modifier = Modifier.padding(10.dp)) {
				Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
					Text("核心参数联动区", style = MaterialTheme.typography.titleMedium)
					IconButton(onClick = { exp = !exp }, modifier = Modifier.size(36.dp)) {
						Icon(painter = painterResource(if (exp) R.drawable.expand_less else R.drawable.expand_more), contentDescription = null)
					}
				}
				if (exp) {
					HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
					// 优化：键盘输入仅改变本地文案状态，只有当按下键盘“完成”键或提交时才保存，免除高频打字卡死
					OutlinedTextField(
						value = txt,
						onValueChange = { txt = it },
						label = { Text("云端接入端点") },
						modifier = Modifier.fillMaxWidth(),
						singleLine = true,
						keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
						keyboardActions = KeyboardActions(onDone = { onSave("API_ENDPOINT", txt) })
					)
				}
			}
		}
	}
}

@Composable
fun LogPanel(modifier: Modifier = Modifier, logs: List<LogItem>, onDel: (String) -> Unit) {
	val cfg = LocalConfiguration.current
	val h = cfg.screenHeightDp.dp
	var dy by remember { mutableStateOf(0f) }
	var col by remember { mutableStateOf(false) }
	val state = rememberLazyListState()
	
	LaunchedEffect(logs.size) {
		if (logs.isNotEmpty()) state.animateScrollToItem(logs.size - 1)
	}
	
	if (!col) {
		Box(modifier = modifier
			.fillMaxWidth()
			.height(h / 3)
			.navigationBarsPadding()
			.offset { IntOffset(0, dy.roundToInt()) }
			.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
			.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))
			.border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
			.pointerInput(Unit) {
				detectDragGestures(
					onDragEnd = { if (dy > 150) col = true; dy = 0f },
					onDrag = { change, drag -> change.consume(); if (dy + drag.y >= 0) dy += drag.y }
				)
			}
		) {
			Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
				Box(modifier = Modifier.width(32.dp).height(3.dp).background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(1.5.dp)).align(Alignment.CenterHorizontally))
				LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
					items(logs, key = { it.id }) { log ->
						Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
							val color = when (log.type) {
								LogType.SUCCESS -> Color(0xFF189B46)
								LogType.ERROR -> Color(0xFFE7012F)
								LogType.WARNING -> Color(0xFFFDD10D)
								LogType.INFO -> MaterialTheme.colorScheme.onSurface
							}
							Text(text = "[${log.time}] ${log.target} ➜ ${log.message}", color = color, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 1.2.em), modifier = Modifier.weight(1f).padding(PaddingValues(end = 4.dp)))
							IconButton(onClick = { onDel(log.id) }, modifier = Modifier.size(16.dp).align(Alignment.CenterVertically)) {
								Icon(painter = painterResource(R.drawable.delete), contentDescription = null, tint = Color.Gray.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
							}
						}
						HorizontalDivider(color = Color.Gray.copy(alpha = 0.08f))
					}
				}
			}
		}
	} else {
		Box(modifier = modifier.padding(bottom = 6.dp).navigationBarsPadding().width(80.dp).height(5.dp).background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.5.dp)).clickable { col = false })
	}
}
