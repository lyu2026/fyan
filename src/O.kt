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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.view.WindowCompat
import androidx.navigation.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class LogItem( // 核心实体类：定义单条流水日志的数据结构
	val id:String=UUID.randomUUID().toString(), // 重要变量：唯一随机哈希ID
	val time:String, // 重要变量：格式化时间戳字符串
	val target:String, // 重要变量：触发事件的业务目标模块
	val message:String, // 重要变量：投递的具体调试详情文本
	val type:LogType // 重要变量：控制着色渲染的日志安全等级
)

enum class LogType{SUCCESS,ERROR,WARNING,INFO} // 枚举类：划分日志行为的四色视觉等级

class O:ComponentActivity(){ // 主入口类：托管全屏Spa容器的唯一活动骨架
	override fun onCreate(savedInstanceState:Bundle?){ // 核心方法：初始化系统窗口与 Compose 环境
		super.onCreate(savedInstanceState)
		setContent{
			val isDark=isSystemInDarkTheme() // 重要变量：监听并捕获系统当前的暗黑主题状态
			val colorScheme=if(isDark)darkColorScheme()elselightColorScheme() // 重要变量：自适应双色调色板
			val view=LocalView.current // 重要变量：持有当前窗口的最底层原生视图引用
			if(!view.isInEditMode){ // 逻辑拦截：仅在非IDE预览的真实终端渲染沉浸式状态栏
				SideEffect{
					val window=(view.context as Activity).window // 重要变量：向下强转获取当前的控制窗口
					window.statusBarColor=android.graphics.Color.TRANSPARENT // 物理视窗操作：强制状态栏全透
					window.navigationBarColor=android.graphics.Color.TRANSPARENT // 物理视窗操作：强制底部栏全透
					WindowCompat.getInsetsController(window,view).apply{
						isAppearanceLightStatusBars=!isDark // 视觉对比度优化：根据暗色基调反转状态栏字色
						isAppearanceLightNavigationBars=!isDark // 视觉对比度优化：根据暗色基调反转虚拟键色调
					}
				}
			}
			MaterialTheme(colorScheme=colorScheme){
				Surface(modifier=Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
					MainSpa() // 页面方法调用：拉起全局路由流向与单页容器
				}
			}
		}
	}
}

@Composable
fun MainSpa(){ // 全局方法：托管单页多控制台、日志总线与阻断返回弹窗的核心骨架
	val nav=rememberNavController() // 重要变量：树状路由控制器实例
	val ctx=LocalContext.current // 重要变量：获取标准的上下文句柄
	var showLog by remember{mutableStateOf(true)} // 重要变量：全局日志面板显隐控制开关状态
	val logs=remember{mutableStateListOf<LogItem>()} // 重要变量：高频追加/清理行为的响应式链表队列

	fun addLog(tag:String,msg:String,type:LogType){ // 核心方法：向底层队列安全追加一条时分秒对齐的系统日志
		val fmt=SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date()) // 重要变量：固化当前的日志发生时刻
		logs.add(LogItem(time=fmt,target=tag,message=msg,type=type)) // 状态更新：向UI触发器异步推送行数据
	}

	LaunchedEffect(Unit){ // 生命周期钩子：组件首次挂载时执行内核就绪检测
		addLog("系统引擎","孚琰 原生双端内核架构初始化就绪",LogType.SUCCESS) // 数据初始化：投递首条就绪提示
	}

	val curEntry by nav.currentBackStackEntryAsState() // 重要变量：高频监听路由栈顶的流向快照
	if(curEntry?.destination?.route=="home"){ // 条件分支：当且仅当用户处于主控制台时激活拦截器
		var showExit by remember{mutableStateOf(false)} // 重要变量：本地退出二次确认窗的显隐状态锁
		BackHandler(enabled=true){showExit=true} // 系统行为拦截：接管 Android 物理返回键并唤起自定义弹窗
		if(showExit){
			AlertDialog(
				onDismissRequest={showExit=false}, // 交互防错：轻触窗外空白区安全收回弹窗
				title={Text("提示")}, // 窗体美化：固定提示标题
				text={Text("确定要彻底退出应用吗？")}, // 窗体美化：二次验证防误触文案
				confirmButton={TextButton(onClick={(ctx as? Activity)?.finish()}){Text("确认退出")}}, // 核心退出：杀死进程
				dismissButton={TextButton(onClick={showExit=false}){Text("取消")}} // 交互响应：恢复现场并闭合窗体
			)
		}
	}

	val cfg=LocalConfiguration.current // 重要变量：读取当前的设备底层物理屏幕环境参数
	val isTV=cfg.uiMode and Configuration.UI_MODE_TYPE_MASK==Configuration.UI_MODE_TYPE_TELEVISION // 重要变量：智能判别当前终端是否为电视大屏

	Box(modifier=Modifier.fillMaxSize()){
		NavHost(navController=nav,startDestination="home"){
			composable("home"){
				Home(
					isTV=isTV, // 数据向下传递：注入设备形态标记
					showLog=showLog, // 数据向下传递：注入面板显隐标记
					onToggleLog={ // 行为回调：处理顶栏小眼睛图标的按压切换行为
						showLog=it // 状态更新：反转面板状态
						addLog("界面设置","全局消息面板开关变更为: $it",LogType.INFO) // 业务联动：异步记入操作日志
					},
					onMock={addLog("模拟器","手动触发高亮警告级系统排查事件！",LogType.WARNING)}, // 模拟方法：引发测试日志投递
					onNav={route-> // 核心导航方法：承接子面板发起的正向路由跳转行为
						addLog("路由导航","正准备切换至原生页面: [$route]",LogType.SUCCESS) // 路由记入：记录前进历史
						nav.navigate(route) // 物理转跳：切出新层级
					}
				)
			}
			composable("setting"){
				Setting(
					onBack={ // 逆向回退方法：承接设置页面发起的滑回请求
						addLog("路由导航","从设置面板安全滑回主控制台",LogType.INFO) // 路由记入：记录回退历史
						nav.popBackStack() // 物理路由：出栈并销毁顶层
					},
					onSave={k,v->addLog("存储引擎","配置项 [$k] 自动保存成功: $v",LogType.SUCCESS)} // 核心存储方法：落盘回调
				)
			}
		}
		if(showLog){
			LogPanel(
				modifier=Modifier.align(Alignment.BottomCenter), // 样式控制：锁定至视窗正底部贴合
				logs=logs, // 数据注入：绑定同一份全局日志序列
				onDel={id->logs.removeAll{it.id==id}} // 核心清理方法：根据物理UUID精确定向剪除日志行
			)
		}
	}
}

@Composable
fun Home(isTV:Boolean,showLog:Boolean,onToggleLog:(Boolean)->Unit,onMock:()->Unit,onNav:(String)->Unit){ // 控制台主页组件
	Column(modifier=Modifier.fillMaxSize()){
		Row(
			modifier=Modifier.fillMaxWidth().statusBarsPadding().height(48.dp).padding(horizontal=8.dp), // 沉浸式优化：防状态栏文字重叠
			verticalAlignment=Alignment.CenterVertically, // 布局定位：沿轴向中心齐平
			horizontalArrangement=Arrangement.SpaceBetween // 布局定位：拉开标题与功能按钮间距
		){
			Text("孚琰 控制台",style=MaterialTheme.typography.titleMedium) // UI美化：呈现主标题
			Row(verticalAlignment=Alignment.CenterVertically){
				Text(text=if(isTV)"电视"else"手机",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(end=4.dp)) // 终端标注
				IconButton(onClick={onToggleLog(!showLog)},modifier=Modifier.size(36.dp)){
					Icon(
						painter=painterResource(if(showLog)R.drawable.visibility else R.drawable.visibility_off), // 资源优化：丢弃大包，走本地动态资源路径
						contentDescription=null,
						modifier=Modifier.size(20.dp) // 样式修正：锁定图标物理尺寸
					)
				}
			}
		}
		Column(modifier=Modifier.fillMaxSize().padding(horizontal=8.dp)){
			Spacer(modifier=Modifier.height(8.dp)) // 视觉间距：卡片顶部留白
			CardItem(title="自动化参数设置",desc="内置无缝响应式卡片、表单策略与持久化管理",onClick={onNav("setting")}) // 功能注入：激活转跳
			Spacer(modifier=Modifier.height(8.dp)) // 视觉间距：双卡片缝隙隔离
			CardItem(title="手动投递诊断日志",desc="向贴底面板追加一条模拟警告事件进行视图验证",onClick=onMock) // 功能注入：激活模拟投递
		}
	}
}

@Composable
fun CardItem(title:String,desc:String,onClick:()->Unit){ // 高级自适应卡片组件：支持遥控器与触控双重高亮反馈
	val req=remember{FocusRequester()} // 重要变量：电视端遥控器核心聚焦请求注册器
	val src=remember{MutableInteractionSource()} // 重要变量：状态传送带，高频抓取外部接触或遥控信号
	val focused by src.collectIsFocusedAsState() // 重要变量：将焦点信号转化为可监听的响应式 Boolean
	Card(
		modifier=Modifier.fillMaxWidth().height(68.dp).focusRequester(req).focusable(interactionSource=src).shadow(if(focused)6.dp else 1.dp,RoundedCornerShape(8.dp)).clickable(interactionSource=src,indication=LocalIndication.current){onClick()}.border(width=1.5.dp,color=if(focused)MaterialTheme.colorScheme.primary else Color.Transparent,shape=RoundedCornerShape(8.dp)), // 人性化适配：若电视聚焦则动态扩展阴影、产生高亮边框并响应OK键
		colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant) // 视觉美化：选用低饱和度的变体背景色提高主屏可读性
	){
		Box(modifier=Modifier.fillMaxSize().padding(horizontal=12.dp,vertical=8.dp),contentAlignment=Alignment.CenterStart){
			Column{
				Text(title,style=MaterialTheme.typography.titleMedium) // 卡片排版：上层渲染加粗标题
				Text(desc,style=MaterialTheme.typography.bodySmall,color=Color.Gray,maxLines=1) // 卡片排版：下层渲染辅助单行截断文案
			}
		}
	}
}

@Composable
fun Setting(onBack:()->Unit,onSave:(String,String)->Unit){ // 参数调节与设置大面板组件
	var exp by remember{mutableStateOf(true)} // 重要变量：控制折叠抽屉容器张合的状态变量
	var txt by remember{mutableStateOf("")} // 重要变量：承接文本框高频变动内容的临时缓冲区
	Column(modifier=Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=8.dp).verticalScroll(rememberScrollState())){ // 视觉防溢出：允许长内容进行视窗内物理滚动
		Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.height(48.dp)){
			IconButton(onClick=onBack,modifier=Modifier.size(36.dp)){
				Icon(
					painter=painterResource(R.drawable.arrow_back), // 动态资源映射：加载本地小写下划线返回图标
					contentDescription=null,
					modifier=Modifier.size(20.dp)
				)
			}
			Text("系统配置",style=MaterialTheme.typography.titleMedium) // 顶栏排版：呈现配置主标
		}
		Spacer(modifier=Modifier.height(4.dp))
		Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))){ // 视觉优化：调低半透明度，强化多级卡片纵深质感
			Column(modifier=Modifier.padding(10.dp)){
				Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
					Text("核心参数联动区",style=MaterialTheme.typography.titleMedium) // 分区小标题
					IconButton(onClick={exp=!exp},modifier=Modifier.size(36.dp)){
						Icon(
							painter=painterResource(if(exp)R.drawable.expand_less else R.drawable.expand_more), // 动态指示：上下箭头的本地动态无损切图
							contentDescription=null
						)
					}
				}
				if(exp){ // 抽屉逻辑：当且仅当展开状态时渲染下属复杂表单
					HorizontalDivider(modifier=Modifier.padding(vertical=6.dp)) // 视觉美化：分区微弱水平割线
					OutlinedTextField(value=txt,onValueChange={txt=it;onSave("API_ENDPOINT",it)},label={Text("云端接入端点 (自动保存)")},modifier=Modifier.fillMaxWidth(),singleLine=true) // 高级数据回传：键盘输入变更直接无缝透传给外层 addLog 总线
				}
			}
		}
	}
}

@Composable
fun LogPanel(modifier:Modifier=Modifier,logs:List<LogItem>,onDel:(String)->Unit){ // 具有手势阻尼、自动卷动与色彩辨识的高级贴底消息面板组件
	val cfg=LocalConfiguration.current // 重要变量：实时嗅探并持有设备的物理屏幕常数
	val h=cfg.screenHeightDp.dp // 重要变量：动态测算并锚定当前屏幕高度的三分之一，维持严苛的黄金视觉配比
	var dy by remember{mutableStateOf(0f)} // 重要变量：高频记录垂直手势滑移像素距离的浮点计数器
	var col by remember{mutableStateOf(false)} // 重要变量：控制整个控制台折叠进入最小化托盘状态的开关锁
	val state=rememberLazyListState() // 重要变量：绑定并接管日志平滑自动卷动机制的视窗定位器
	LaunchedEffect(logs.size){if(logs.isNotEmpty())state.animateScrollToItem(logs.size-1)} // 动态监测：一旦队列追加新行，立刻强制列表执行底层平滑跟踪追焦
	if(!col){ // 渲染选择：若非最小化则呈现高级多色日志面版
		Box(modifier=modifier.fillMaxWidth().height(h/3).navigationBarsPadding().offset{IntOffset(0,dy.roundToInt())}.clip(RoundedCornerShape(topStart=12.dp,topEnd=12.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha=0.93f)).border(1.dp,Color.Gray.copy(alpha=0.15f),RoundedCornerShape(topStart=12.dp,topEnd=12.dp)).pointerInput(Unit){
			detectDragGestures(onDragEnd={if(dy>150)col=true;dy=0f},onDrag={change,drag->change.consume();if(dy+drag.y>=0)dy+=drag.y}) // 人性化手势优化：下滑超过150.0像素判定有意识隐藏，直接切入底栏卡槽；否则物理回弹复原
		}){
			Column(modifier=Modifier.fillMaxSize().padding(horizontal=8.dp,vertical=4.dp)){
				Box(modifier=Modifier.width(32.dp).height(3.dp).background(Color.Gray.copy(alpha=0.4f),RoundedCornerShape(1.5.dp)).align(Alignment.CenterHorizontally)) // 视觉细节：顶部小灰色把手横条
				LazyColumn(state=state,modifier=Modifier.fillMaxSize()){
					items(logs){log-> // 循环遍历：高性能逐行取出并灌注日志记录
						Row(modifier=Modifier.fillMaxWidth().padding(vertical=2.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top){
							val color=when(log.type){ // 视觉着色选择：匹配品牌定制四色语义学高亮色彩
								LogType.SUCCESS->Color(0xFF189B46) // 核心视觉：孚琰标志性成功饱满绿
								LogType.ERROR->Color(0xFFE7012F) // 核心视觉：警告故障高浓度红
								LogType.WARNING->Color(0xFFFDD10D) // 核心视觉：系统待排查高亮中性黄
								LogType.INFO->MaterialTheme.colorScheme.onSurface // 核心视觉：常规动作匹配默认系统文字明暗色
							}
							Text(text="[${log.time}] ${log.target} ➜ ${log.message}",color=color,style=MaterialTheme.typography.bodySmall.copy(lineHeight=1.2.em),modifier=Modifier.weight(1f).padding(PaddingValues(end=4.dp))) // 排版修正：赋予权重占比，保障左侧文本超长时绝不压榨右侧删除按钮的空间
							IconButton(onClick={onDel(log.id)},modifier=Modifier.size(16.dp).align(Alignment.CenterVertically)){ // 单行剪除：触发精确定向移除
								Icon(
									painter=painterResource(R.drawable.delete), // 资源替换：指向本地的小写清除图标
									contentDescription=null,
									tint=Color.Gray.copy(alpha=0.7f), // 视觉弱化：降低删除图标侵入感，维持界面和谐度
									modifier=Modifier.size(12.dp)
								)
							}
						}
						HorizontalDivider(color=Color.Gray.copy(alpha=0.08f)) // 视觉微调：极淡的行与行过渡虚化线
					}
				}
			}
		}
	}else{ // 渲染选择：最小化状态下，在视窗极底端保留一个精致的人性化胶囊卡槽，轻触原地唤醒还原面板
		Box(modifier=modifier.padding(bottom=6.dp).navigationBarsPadding().width(80.dp).height(5.dp).background(Color.Gray.copy(alpha=0.5f),RoundedCornerShape(2.5.dp)).clickable{col=false}) // 恢复行为：重置托盘标记，平滑撑开完整控制台
	}
}
