package com.fyan // 修复：必须保持全小写

import java.util.*
import java.text.SimpleDateFormat

import kotlin.math.roundToInt

import android.view.View
import android.os.Bundle
import android.app.Activity
import android.widget.EditText

import android.content.Context
import android.content.res.Configuration


import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager

import androidx.compose.runtime.*
import androidx.compose.material3.*

import androidx.core.view.WindowCompat

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class LG( // 日志类
	val i:String=UUID.randomUUID().toString(), // 标识
	val w:String, // 目标
	val o:String, // 消息
	val t:String, // 时间
	val c:LT // 类型
)
enum class LT{S,E,W,I} // 日志类型枚举

// 入口主类
class O:ComponentActivity(){
	override fun onCreate(state:Bundle?){
		super.onCreate(state)
		setContent{
			val view=LocalView.current // 当前 Composable 的宿主 View
			val dark=isSystemInDarkTheme() // 当前系统是否为深色模式
			val scheme=if(dark)darkColorScheme()else lightColorScheme() // 系统颜色主题
			if(!view.isInEditMode){ // 预览时跳过的操作
				val color=scheme.background
				SideEffect{ // 每次重组后都会执行
					val a=view.context as?Activity?:return@SideEffect
					a.window.statusBarColor=color.toArgb() // 状态栏背景色
					WindowCompat.getInsetsController(a.window,view).apply{
						isAppearanceLightStatusBars=!dark
					}
					// 全局设置点击非输入区域时键盘消失
					a.window.decorView.setOnTouchListener{_,event->
						if(event.action==MotionEvent.ACTION_DOWN){
							val fv=a.currentFocus
							if(fv is EditText&&ni(fv,event)){
								fv.clearFocus()
								val im=a.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
								im.hideSoftInputFromWindow(fv.windowToken,0)
							}
						}
						false
					}
				}
			}
			MaterialTheme(colorScheme=scheme){
				Surface(modifier=Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
					SPA()
				}
			}
		}
	}
	// 判断点击位置是否在输入框外部
	private fun ni(v:View,e:MotionEvent):Boolean{
		val loc=IntArray(2)
		v.getLocationOnScreen(loc)
		val x=e.rawX.toInt()
		val y=e.rawY.toInt()
		return x<loc[0]||x>(loc[0]+v.width)||y<loc[1]||y>(loc[1]+v.height)
	}
}

@Composable
fun SPA(){ // 单页面应用结构
	val ctx=LocalContext.current // 当前 Composable 所在环境的上下文
	val nav=rememberNavController() // 创建一个导航控制器并记住它
	val gs=remember{mutableStateListOf<LG>()} // 可观察的日志列表，列表内容变化时会触发重组
	var sg by remember{mutableStateOf(true)} // 可观察的认真面板显示状态，初始值为 true
	val log=remember{ // 稳定的 Lambda 添加日志函数
		{w:String,o:String,c:LT->
			val t=SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date())
			gs.add(LG(t=t,w=w,o=o,c=c))
		}
	}

	LaunchedEffect(Unit){log("系统","孚琰 初始化就绪",LT.S)}

	// 当前导航栈顶部的路由条目
	val ce by nav.currentBackStackEntryAsState()
	if(ce?.destination?.route=="home"){ // 当前是首页
		var exit by remember{mutableStateOf(false)}
		BackHandler(enabled=true){exit=true}
		if(exit){ // 退出应用，弹框提示
			AlertDialog(
				onDismissRequest={exit=false},
				shape=RoundedCornerShape(4.dp),
				title={Text("提示")},text={Text("确定要彻底退出应用吗？")},
				confirmButton={TextButton(onClick={(ctx as? Activity)?.finish()}){Text("确认退出")}},
				dismissButton={TextButton(onClick={exit=false}){Text("取消")}}
			)
		}
	}
	val cc=LocalConfiguration.current // 当前设备配置信息
	val tv=cc.uiMode and Configuration.UI_MODE_TYPE_MASK==Configuration.UI_MODE_TYPE_TELEVISION

	Box(modifier=Modifier.fillMaxSize()){ // 注册所有页面
		NavHost(navController=nav,startDestination="home"){
			composable("home"){
				Home(tv=tv,sg=sg,
					xg={
						sg=it
						log("首页","日志面板显示开关变更为: $it",LT.I)
					},
					go={route->
						log("导航","准备切换至页面: [$route]",LT.S)
						nav.navigate(route)
					}
				)
			}
			composable("setting"){
				Setting(
					back={
						log("导航","返回至首页",LT.I)
						nav.popBackStack()
					},
					save={k,v->log("设置页","配置项 [$k] 已自动保存为: $v",LT.S)}
				)
			}
		}
		if(sg)LP( // 显示日志面板
			modifier=Modifier.align(Alignment.BottomCenter),
			list=gs,remove={i->gs.removeAll{it.i==i}}
		)
	}
}

@Composable // 首页
fun Home(tv:Boolean,sg:Boolean,xg:(Boolean)->Unit,go:(String)->Unit){
	Column(modifier=Modifier.fillMaxSize()){
		Row(
			modifier=Modifier.fillMaxWidth().statusBarsPadding().height(48.dp).padding(start=10.dp,end=1.dp),
			horizontalArrangement=Arrangement.SpaceBetween,
			verticalAlignment=Alignment.CenterVertically
		){
			Text("孚琰 控制台",style=MaterialTheme.typography.titleLarge)
			Row(verticalAlignment=Alignment.CenterVertically){
				Text(text=if(tv)"📺 TV"else"📱 手机",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(end=8.dp))
				IconButton(onClick={xg(!sg)},modifier=Modifier.size(36.dp)){
					Icon(
						painter=painterResource(if(sg)R.drawable.visibility else R.drawable.visibility_off),
						contentDescription=null,modifier=Modifier.size(20.dp)
					)
				}
			}
		}
		Spacer(modifier=Modifier.height(4.dp)) // 间隔
		Column(modifier=Modifier.padding(horizontal=10.dp)){
			CD(title="自动化参数设置",desc="内置无缝响应式卡片、表单策略与持久化管理",click={go("setting")})
			Spacer(modifier=Modifier.height(6.dp))
			CD(title="手动投递诊断日志",desc="向贴底面板追加一条模拟警告事件进行视图验证",click={})
		}
	}
}

@Composable // 卡片
fun CD(title:String,desc:String,click:()->Unit){
	val fr=remember{FocusRequester()} // 焦点请求器
	val ms=remember{MutableInteractionSource()} // 交互事件源
	val focused by ms.collectIsFocusedAsState() // 焦点状态
	Card(
		onClick=click,interactionSource=ms,
		modifier=Modifier.fillMaxWidth().focusRequester(fr)
			.shadow(if(focused)6.dp else 1.dp,RoundedCornerShape(8.dp))
			.border(width=1.5.dp,color=if(focused)MaterialTheme.colorScheme.primary else Color.Transparent,shape=RoundedCornerShape(8.dp)),
		colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)
	){
		Box(modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),contentAlignment=Alignment.CenterStart){
			Column{
				Text(title,style=MaterialTheme.typography.titleMedium) // 标题
				Spacer(modifier=Modifier.height(2.dp)) // 间隔
				// 描述文本
				Text(desc,style=MaterialTheme.typography.bodySmall,color=Color.Gray,maxLines=2)
			}
		}
	}
}

@Composable // 设置页
fun Setting(back:()->Unit,save:(String,String)->Unit){
	var field by remember{mutableStateOf("")} // 关联的值
	var show by remember{mutableStateOf(true)} // 展开状态

	Column(modifier=Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())){
		// 顶部导航栏
		Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.height(48.dp).padding(start=2.dp,end=10.dp)){
			IconButton(onClick=back,modifier=Modifier.size(36.dp)){
				Icon(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier=Modifier.size(20.dp))
			}
			Text("系统配置",style=MaterialTheme.typography.titleLarge)
		}
		Spacer(modifier=Modifier.height(4.dp)) // 间隔
		// 表单卡片
		Card(modifier=Modifier.fillMaxWidth().padding(horizontal=10.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))){
			Column(modifier=Modifier.padding(vertical=8.dp)){
				// 顶栏
				Row(modifier=Modifier.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
					Text("核心参数联动区",style=MaterialTheme.typography.titleMedium)
					IconButton(onClick={show=!show},modifier=Modifier.size(30.dp)){
						Icon(painter=painterResource(if(show)R.drawable.expand_less else R.drawable.expand_more),contentDescription=null)
					}
				}
				if(show){ // 根据状态展示表单区域
					// 分割线
					HorizontalDivider(modifier=Modifier.padding(vertical=8.dp))
					// 输入框
					OutlinedTextField(
						value=field,onValueChange={field=it},
						label={Text("关联数据")},
						modifier=Modifier.fillMaxWidth().padding(horizontal=8.dp),singleLine=true,
						keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),
						keyboardActions=KeyboardActions(onDone={save("TEST",field)})
					)
				}
			}
		}
	}
}

@Composable // 日志面板
fun LP(modifier:Modifier=Modifier,list:List<LG>,remove:(String)->Unit){
	val cc=LocalConfiguration.current // 设备信息
	val h=cc.screenHeightDp.dp // 屏高(单位:dp)
	var y by remember{mutableStateOf(0f)} // 垂直方向偏移量(浮点值)
	var x by remember{mutableStateOf(false)} // 折叠状态
	val state=rememberLazyListState() // 懒加载列表的滚动状态

	LaunchedEffect(list.size){
		if(list.isNotEmpty())state.animateScrollToItem(list.size-1)
	}

	if(!x){
		Box(modifier=modifier.fillMaxWidth().height(h/3).padding(horizontal=1.dp)
			.navigationBarsPadding().offset{IntOffset(0,y.roundToInt())}
			.clip(RoundedCornerShape(topStart=8.dp,topEnd=8.dp))
			.background(MaterialTheme.colorScheme.surface.copy(alpha=0.90f))
			.border(1.dp,Color.Gray.copy(alpha=0.15f),RoundedCornerShape(topStart=8.dp,topEnd=8.dp))
			.pointerInput(Unit){
				detectDragGestures(
					onDragEnd={if(y>150)x=true;y=0f},
					onDrag={change,drag->change.consume();if(y+drag.y>=0)y+=drag.y}
				)
			}
		){
			Column(modifier=Modifier.fillMaxSize().padding(horizontal=5.dp,vertical=2.dp)){
				Box(modifier=Modifier.width(36.dp).height(4.dp).background(Color.Gray.copy(alpha=0.4f),RoundedCornerShape(1.5.dp)).align(Alignment.CenterHorizontally).padding(top=6.dp,bottom=8.dp))
				LazyColumn(state=state,modifier=Modifier.fillMaxSize()){
					items(list,key={it.i}){g->
						Row(modifier=Modifier.fillMaxWidth().padding(vertical=1.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top){
							val color=when(g.c){
								LT.S->Color(0xFF189B46)
								LT.E->Color(0xFFE7012F)
								LT.W->Color(0xFFFDD10D)
								LT.I->MaterialTheme.colorScheme.onSurface
							}
							Text(text="[${g.t}] ${g.w} ➜ ${g.o}",color=color,style=MaterialTheme.typography.bodySmall.copy(lineHeight=1.2.em),modifier=Modifier.weight(1f).padding(PaddingValues(end=4.dp)))
							IconButton(onClick={remove(g.i)},modifier=Modifier.size(14.dp).align(Alignment.CenterVertically)){
								Icon(painter=painterResource(R.drawable.delete),contentDescription=null,tint=Color.Gray.copy(alpha=0.7f),modifier=Modifier.size(12.dp))
							}
						}
						HorizontalDivider(color=Color.Gray.copy(alpha=0.08f))
					}
				}
			}
		}
	}else{
		Box(modifier=modifier.padding(bottom=6.dp).navigationBarsPadding().width(80.dp).height(5.dp).background(Color.Gray.copy(alpha=0.5f),RoundedCornerShape(2.5.dp)).clickable{x=false})
	}
}
