package com.fyan // 包名，必须保持全小写

import java.util.* // 工具类，UUID 等
import java.text.SimpleDateFormat // 时间格式化

import kotlin.math.roundToInt // Float 转 Int

import android.view.View // 原生视图基类
import android.os.Bundle // Activity 状态保存
import android.app.Activity // Activity 基类
import android.widget.EditText // 原生输入框

import android.content.Context // 上下文，获取系统服务用
import android.content.res.Configuration // 设备配置信息（屏幕方向、UI模式等）

import android.view.MotionEvent // 触摸事件
import android.view.inputmethod.InputMethodManager // 输入法管理器，收起键盘用

import androidx.compose.runtime.* // Compose 运行时：State、remember、LaunchedEffect 等
import androidx.compose.material3.* // Material3 组件：AlertDialog、Card、Text、Icon 等

import androidx.core.view.WindowCompat // 系统栏控制器兼容库

import androidx.activity.ComponentActivity // Compose 的 Activity 基类
import androidx.activity.compose.setContent // 设置 Compose 内容
import androidx.activity.compose.BackHandler // 系统返回键拦截

import androidx.compose.foundation.border // 边框
import androidx.compose.foundation.layout.* // 布局：Column、Row、Box、Spacer 等
import androidx.compose.foundation.clickable // 点击修饰符
import androidx.compose.foundation.background // 背景色
import androidx.compose.foundation.verticalScroll // 垂直滚动
import androidx.compose.foundation.rememberScrollState // 滚动状态记忆
import androidx.compose.foundation.isSystemInDarkTheme // 系统深色模式检测
import androidx.compose.foundation.shape.RoundedCornerShape // 圆角形状
import androidx.compose.foundation.gestures.detectDragGestures // 拖拽手势检测

import androidx.compose.foundation.text.KeyboardActions // 键盘动作
import androidx.compose.foundation.text.KeyboardOptions // 键盘选项

import androidx.compose.foundation.lazy.items // LazyColumn 的 items 扩展
import androidx.compose.foundation.lazy.LazyColumn // 懒加载列表
import androidx.compose.foundation.lazy.rememberLazyListState // 列表滚动状态

import androidx.compose.foundation.interaction.collectIsFocusedAsState // 收集焦点状态
import androidx.compose.foundation.interaction.MutableInteractionSource // 交互事件源

import androidx.compose.ui.Modifier // 修饰符基类
import androidx.compose.ui.Alignment // 对齐方式
import androidx.compose.ui.res.painterResource // 加载图片资源
import androidx.compose.ui.input.pointer.pointerInput // 指针输入

import androidx.compose.ui.unit.dp // dp 单位
import androidx.compose.ui.unit.em // em 单位（相对字号）
import androidx.compose.ui.unit.IntOffset // 整型偏移

import androidx.compose.ui.graphics.Color // Compose 颜色
import androidx.compose.ui.graphics.toArgb // Compose Color 转原生 Int 颜色

import androidx.compose.ui.draw.clip // 裁剪
import androidx.compose.ui.draw.shadow // 阴影
import androidx.compose.ui.focus.focusRequester // 焦点请求修饰符
import androidx.compose.ui.focus.FocusRequester // 焦点请求器

import androidx.compose.ui.text.font.FontFamily // 字体族
import androidx.compose.ui.text.input.ImeAction // 键盘动作类型

import androidx.compose.ui.platform.LocalView // 获取当前宿主 View
import androidx.compose.ui.platform.LocalContext // 获取当前 Context
import androidx.compose.ui.platform.LocalConfiguration // 获取设备配置

import androidx.navigation.compose.NavHost // 导航宿主
import androidx.navigation.compose.composable // 注册路由
import androidx.navigation.compose.rememberNavController // 导航控制器
import androidx.navigation.compose.currentBackStackEntryAsState // 当前路由状态

data class LG( // 日志数据类
	val i:String=UUID.randomUUID().toString(), // 唯一标识，默认生成 UUID
	val w:String, // 日志来源/目标
	val o:String, // 日志消息内容
	val t:String, // 日志时间
	val c:LT // 日志类型
)
enum class LT{S,E,W,I} // 日志类型枚举：S=成功、E=错误、W=警告、I=信息

// 入口主类，继承 ComponentActivity 以使用 Compose
class O:ComponentActivity(){
	// 重写触摸事件分发，实现点击非输入区域收起键盘
	override fun dispatchTouchEvent(event:MotionEvent):Boolean{
		// 只处理按下事件
		if(event.action==MotionEvent.ACTION_DOWN){
			val fv=currentFocus // 当前持有焦点的 View
			// 如果焦点在输入框上，且点击位置在输入框外部
			if(fv is EditText&&ni(fv,event)){
				fv.clearFocus() // 清除焦点
				val im=getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
				im.hideSoftInputFromWindow(fv.windowToken,0) // 收起键盘
			}
		}
		return super.dispatchTouchEvent(event) // 继续传递事件
	}

	override fun onCreate(state:Bundle?){
		super.onCreate(state)
		setContent{ // 设置 Compose 内容
			val view=LocalView.current // 当前 Composable 的宿主 View
			val dark=isSystemInDarkTheme() // 当前系统是否为深色模式
			val scheme=if(dark)darkColorScheme()else lightColorScheme() // 根据深浅色选择颜色主题
			if(!view.isInEditMode){ // 预览时跳过的操作，避免 IDE 预览崩溃
				val color=scheme.background // 获取主题背景色
				SideEffect{ // 每次成功重组后执行，将 Compose 状态同步到原生 View 系统
					val a=view.context as?Activity?:return@SideEffect // 获取 Activity
					a.window.statusBarColor=color.toArgb() // 状态栏背景色与页面背景色保持一致
					WindowCompat.getInsetsController(a.window,view).apply{
						isAppearanceLightStatusBars=!dark // 状态栏图标深浅色适配
					}
				}
			}
			MaterialTheme(colorScheme=scheme){ // 应用颜色主题
				Surface(modifier=Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){ // 全局背景
					SPA() // 加载单页面应用结构
				}
			}
		}
	}
	// 判断点击位置是否在输入框外部
	private fun ni(v:View,e:MotionEvent):Boolean{
		val loc=IntArray(2) // 存储输入框在屏幕上的坐标
		v.getLocationOnScreen(loc) // 获取输入框左上角屏幕坐标
		val x=e.rawX.toInt() // 触摸点屏幕 X 坐标
		val y=e.rawY.toInt() // 触摸点屏幕 Y 坐标
		// 触摸点不在输入框范围内则返回 true
		return x<loc[0]||x>(loc[0]+v.width)||y<loc[1]||y>(loc[1]+v.height)
	}
}

@Composable
fun SPA(){ // 单页面应用结构，管理导航和全局状态
	val ctx=LocalContext.current // 当前 Composable 所在环境的上下文
	val nav=rememberNavController() // 创建一个导航控制器并记住它
	val gs=remember{mutableStateListOf<LG>()} // 可观察的日志列表，列表内容变化时会触发重组
	var sg by remember{mutableStateOf(true)} // 可观察的日志面板显示状态，初始值为 true
	val log=remember{ // 稳定的 Lambda 添加日志函数，记住引用避免子组件不必要重组
		{w:String,o:String,c:LT-> // 参数：来源、消息、类型
			val t=SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date()) // 格式化当前时间
			gs.add(LG(t=t,w=w,o=o,c=c)) // 添加日志到可观察列表
		}
	}

	LaunchedEffect(Unit){log("系统","孚琰 初始化就绪",LT.S)} // 首次进入组合时记录初始化日志

	// 当前导航栈顶部的路由条目，页面跳转时自动跟随变化
	val ce by nav.currentBackStackEntryAsState()
	if(ce?.destination?.route=="home"){ // 当前是首页时才处理返回退出
		var exit by remember{mutableStateOf(false)} // 是否显示退出确认弹窗
		BackHandler(enabled=true){exit=true} // 拦截系统返回键，显示退出弹窗
		if(exit){ // 退出应用，弹框提示
			AlertDialog(
				onDismissRequest={exit=false}, // 点击外部关闭弹窗
				shape=RoundedCornerShape(4.dp), // 弹窗圆角
				title={Text("提示")},text={Text("确定要彻底退出应用吗？")},
				confirmButton={TextButton(onClick={(ctx as? Activity)?.finish()}){Text("确认退出")}}, // 确认退出按钮
				dismissButton={TextButton(onClick={exit=false}){Text("取消")}} // 取消按钮
			)
		}
	}
	val cc=LocalConfiguration.current // 当前设备配置信息
	val tv=cc.uiMode and Configuration.UI_MODE_TYPE_MASK==Configuration.UI_MODE_TYPE_TELEVISION // 判断是否为 TV 设备

	Box(modifier=Modifier.fillMaxSize()){ // 全屏容器，用于叠加日志面板
		NavHost(navController=nav,startDestination="home"){ // 导航宿主，起始页为 home
			composable("home"){ // 注册首页路由
				Home(tv=tv,sg=sg, // 传入 TV 标识和日志面板显隐状态
					xg={ // 切换日志面板显示状态的回调
						sg=it // 更新面板显示状态
						log("首页","日志面板显示开关变更为: $it",LT.I) // 记录切换日志
					},
					go={route-> // 页面跳转回调
						log("导航","准备切换至页面: [$route]",LT.S) // 记录导航日志
						nav.navigate(route) // 执行导航
					}
				)
			}
			composable("setting"){ // 注册设置页路由
				Setting(
					back={ // 返回首页回调
						log("导航","返回至首页",LT.I) // 记录返回日志
						nav.popBackStack() // 返回上一页
					},
					save={k,v->log("设置页","配置项 [$k] 已自动保存为: $v",LT.S)} // 保存配置回调，记录日志
				)
			}
		}
		if(sg)LP( // 如果日志面板显示状态为 true，则渲染日志面板
			modifier=Modifier.align(Alignment.BottomCenter), // 对齐到底部居中
			list=gs,remove={i->gs.removeAll{it.i==i}} // 传入日志列表和按 id 删除的回调
		)
	}
}

@Composable // 首页
fun Home(tv:Boolean,sg:Boolean,xg:(Boolean)->Unit,go:(String)->Unit){
	Column(modifier=Modifier.fillMaxSize()){ // 全屏列布局
		Row( // 顶部导航栏
			modifier=Modifier.fillMaxWidth().statusBarsPadding().height(48.dp).padding(start=10.dp,end=3.dp), // 宽度填满、状态栏安全区、固定高度、水平内边距
			horizontalArrangement=Arrangement.SpaceBetween, // 左右两端对齐
			verticalAlignment=Alignment.CenterVertically // 垂直居中
		){
			Text("孚琰 控制台",style=MaterialTheme.typography.titleLarge) // 标题
			Row(verticalAlignment=Alignment.CenterVertically){ // 右侧操作区
				Text(text=if(tv)"📺 TV"else"📱 手机",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(end=8.dp)) // TV/手机标识
				IconButton(onClick={xg(!sg)},modifier=Modifier.size(36.dp)){ // 日志面板显隐切换按钮
					Icon(
						painter=painterResource(if(sg)R.drawable.visibility else R.drawable.visibility_off), // 根据状态切换图标
						contentDescription=null,modifier=Modifier.size(20.dp)
					)
				}
			}
		}
		Spacer(modifier=Modifier.height(4.dp)) // 顶部栏与内容间隔
		Column(modifier=Modifier.padding(horizontal=10.dp)){ // 内容区，水平内边距
			CD(title="自动化参数设置",desc="内置无缝响应式卡片、表单策略与持久化管理",click={go("setting")}) // 第一张卡片
			Spacer(modifier=Modifier.height(6.dp)) // 卡片间隔
			CD(title="手动投递诊断日志",desc="向贴底面板追加一条模拟警告事件进行视图验证",click={}) // 第二张卡片
		}
	}
}

@Composable // 卡片组件
fun CD(title:String,desc:String,click:()->Unit){
	val fr=remember{FocusRequester()} // 焦点请求器，可程序化控制焦点
	val ms=remember{MutableInteractionSource()} // 交互事件源，监听按压、焦点等状态
	val focused by ms.collectIsFocusedAsState() // 从交互源收集焦点状态，focused 变化时触发重组
	Card(
		onClick=click,interactionSource=ms, // 点击事件和交互源
		modifier=Modifier.fillMaxWidth().focusRequester(fr).padding(bottom=10.dp) // 宽度填满、绑定焦点请求器、底部外边距
			.shadow(if(focused)6.dp else 1.dp,RoundedCornerShape(5.dp)) // 焦点时加大阴影
			.border(width=1.5.dp,color=if(focused)MaterialTheme.colorScheme.primary else Color.Transparent,shape=RoundedCornerShape(5.dp)), // 焦点时显示主色边框
		colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant) // 卡片背景色
	){
		Box(modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp).clip(RoundedCornerShape(5.dp)),contentAlignment=Alignment.CenterStart){ // 内容容器，左对齐居中
			Column{ // 标题和描述垂直排列
				Text(title,style=MaterialTheme.typography.titleMedium) // 卡片标题
				Spacer(modifier=Modifier.height(2.dp)) // 标题与描述间隔
				Text(desc,style=MaterialTheme.typography.bodySmall,color=Color.Gray,maxLines=2) // 描述文本，最多2行
			}
		}
	}
}

@Composable // 设置页
fun Setting(back:()->Unit,save:(String,String)->Unit){
	var field by remember{mutableStateOf("")} // 输入框关联的值
	var show by remember{mutableStateOf(true)} // 表单展开/折叠状态

	Column(modifier=Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())){ // 全屏可滚动列，状态栏安全区
		// 顶部导航栏
		Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.height(48.dp).padding(start=1.dp,end=10.dp)){
			IconButton(onClick=back,modifier=Modifier.size(36.dp)){ // 返回按钮
				Icon(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier=Modifier.size(20.dp))
			}
			Text("系统配置",style=MaterialTheme.typography.titleLarge) // 页面标题
		}
		Spacer(modifier=Modifier.height(4.dp)) // 与内容间隔
		// 表单卡片
		Card(modifier=Modifier.fillMaxWidth().padding(horizontal=10.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))){ // 半透明背景卡片
			Column{ // 卡片内容垂直排列
				// 卡片顶栏
				Row(modifier=Modifier.fillMaxWidth().padding(start=10.dp,end=8.dp,top=10.dp,bottom=10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
					Text("核心参数联动区",style=MaterialTheme.typography.titleMedium) // 区块标题
					IconButton(onClick={show=!show},modifier=Modifier.size(30.dp)){ // 展开/折叠按钮
						Icon(painter=painterResource(if(show)R.drawable.expand_less else R.drawable.expand_more),contentDescription=null)
					}
				}
				if(show){ // 根据状态展示或隐藏表单区域
					HorizontalDivider(modifier=Modifier.offset(y=-2.dp)) // 分割线
					OutlinedTextField( // 输入框
						value=field,onValueChange={field=it}, // 双向绑定
						label={Text("关联数据")}, // 标签文字
						modifier=Modifier.fillMaxWidth().padding(top=6.dp,bottom=10.dp,horizontal=10.dp),singleLine=true, // 宽度填满、内边距、单行模式
						keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done), // 键盘完成按钮
						keyboardActions=KeyboardActions(onDone={save("TEST",field)}) // 点击完成时保存
					)
				}
			}
		}
	}
}

@Composable // 日志面板
fun LP(modifier:Modifier=Modifier,list:List<LG>,remove:(String)->Unit){
	val cc=LocalConfiguration.current // 设备信息
	val h=cc.screenHeightDp.dp // 屏幕高度(单位:dp)
	var y by remember{mutableStateOf(0f)} // 垂直方向偏移量(浮点值)，用于拖拽
	var x by remember{mutableStateOf(false)} // 折叠状态，true 时面板收起
	val state=rememberLazyListState() // 懒加载列表的滚动状态

	LaunchedEffect(list.size){ // 日志列表数量变化时自动滚动到底部
		if(list.isNotEmpty())state.animateScrollToItem(list.size-1) // 滚动到最后一条
	}

	if(!x){ // 面板展开状态
		Box(modifier=modifier.fillMaxWidth().height(h/3).padding(horizontal=1.dp) // 宽度填满、高度为屏幕 1/3、水平外边距
			.navigationBarsPadding().offset{IntOffset(0,y.roundToInt())} // 导航栏安全区、拖拽偏移
			.clip(RoundedCornerShape(topStart=5.dp,topEnd=5.dp)) // 顶部圆角裁剪
			.background(MaterialTheme.colorScheme.surface.copy(alpha=0.90f)) // 90% 不透明度背景
			.border(1.dp,Color.Gray.copy(alpha=0.15f),RoundedCornerShape(topStart=5.dp,topEnd=5.dp)) // 浅灰色边框
			.pointerInput(Unit){ // 拖拽手势
				detectDragGestures(
					onDragEnd={if(y>150f)x=true;y=0f}, // 松手时偏移超过 150 像素则折叠面板，偏移归零
					onDrag={change,drag->change.consume();if(y+drag.y>=0f)y+=drag.y} // 拖拽中累加偏移，不允许向上拖
				)
			}
		){
			Column(modifier=Modifier.fillMaxSize().padding(horizontal=5.dp,vertical=2.dp)){ // 内容列，填满面板
				Box(modifier=Modifier.width(64.dp).height(3.dp).background(Color.Gray.copy(alpha=0.4f),RoundedCornerShape(1.5.dp)).align(Alignment.CenterHorizontally).padding(top=0.5.dp)) // 顶部拖拽指示横线
				LazyColumn(state=state,modifier=Modifier.fillMaxSize().padding(top=6.dp)){ // 日志列表，顶部留白
					items(list,key={it.i}){g-> // 遍历日志列表，key 为日志唯一标识
						Row(modifier=Modifier.fillMaxWidth().padding(vertical=1.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top){ // 每条日志行
							val color=when(g.c){ // 根据日志类型设置颜色
								LT.S->Color(0xFF189B46) // 成功-绿色
								LT.E->Color(0xFFE7012F) // 错误-红色
								LT.W->Color(0xFFFDD10D) // 警告-黄色
								LT.I->MaterialTheme.colorScheme.onSurface // 信息-跟随主题
							}
							Text(text="[${g.t}] ${g.w} ➜ ${g.o}",color=color,style=MaterialTheme.typography.bodySmall.copy(lineHeight=1.2.em,fontFamily=FontFamily.Monospace),modifier=Modifier.weight(1f).padding(PaddingValues(end=4.dp))) // 日志内容，等宽字体
							IconButton(onClick={remove(g.i)},modifier=Modifier.size(14.dp).align(Alignment.CenterVertically)){ // 删除按钮
								Icon(painter=painterResource(R.drawable.delete),contentDescription=null,tint=Color.Gray.copy(alpha=0.7f),modifier=Modifier.size(12.dp))
							}
						}
						HorizontalDivider(color=Color.Gray.copy(alpha=0.08f)) // 日志条目之间的分割线
					}
				}
			}
		}
	}else{ // 面板折叠状态，显示一个小横条
		Box(modifier=modifier.padding(bottom=6.dp).navigationBarsPadding().width(80.dp).height(5.dp).background(Color.Gray.copy(alpha=0.5f),RoundedCornerShape(2.5.dp)).clickable{x=false}) // 点击展开面板
	}
}