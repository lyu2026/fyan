package com.fyan

import java.util.*
import java.text.SimpleDateFormat

import kotlin.math.roundToInt

import android.view.View
import android.os.Bundle
import android.app.Activity
import android.graphics.Rect
import android.widget.EditText

import android.content.Context
import android.content.res.Configuration

import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager

import androidx.core.view.WindowCompat

import androidx.compose.runtime.*
import androidx.compose.material3.*

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusManager

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState

data class LG(
	val i:String=UUID.randomUUID().toString(), // 唯一标识
	val w:String, // 日志来源
	val o:String, // 日志内容
	val t:String, // 日志时间
	val c:LT // 日志类型
)
enum class LT{S,E,W,I} // S=成功、E=错误、W=警告、I=信息

class O:ComponentActivity(){
	// 收起键盘：只要当前有焦点 View 且点击在其外部，直接隐藏键盘。
	// Compose 侧焦点清除由 LocalFocusManager.clearFocus() 负责（见 SPA 内的 pointerInput）。
	override fun dispatchTouchEvent(e:MotionEvent):Boolean{
		if(e.action==MotionEvent.ACTION_DOWN){
			val f=currentFocus
			// 有焦点 View 且点击在其外部时，收起键盘（不再判断 EditText 类型）
			if(f!=null&&ni(f,e)){
				val im=getSystemService(Context.INPUT_METHOD_SERVICE)as InputMethodManager
				im.hideSoftInputFromWindow(f.windowToken,0)
				// Compose 侧焦点由 SPA 层的 pointerInput 调用 FocusManager.clearFocus() 处理
			}
		}
		return super.dispatchTouchEvent(e)
	}

	override fun onCreate(state:Bundle?){
		super.onCreate(state)
		setContent{
			val view=LocalView.current
			val dark=isSystemInDarkTheme()
			val scheme=if(dark)darkColorScheme()else lightColorScheme()
			if(!view.isInEditMode){
				val color=scheme.background
				SideEffect{
					val a=view.context as?Activity?:return@SideEffect
					a.window.statusBarColor=color.toArgb()
					WindowCompat.getInsetsController(a.window,view).apply{
						isAppearanceLightStatusBars=!dark
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

	private fun ni(v:View,e:MotionEvent):Boolean{
		val loc=IntArray(2)
		v.getLocationOnScreen(loc)
		val rect=Rect(loc[0],loc[1],loc[0]+v.width,loc[1]+v.height)
		return !rect.contains(e.rawX.toInt(),e.rawY.toInt())
	}
}

// 退出确认弹窗提取为独立 Composable，避免 SPA 层因路由变化时反复订阅/取消 BackHandler
@Composable
fun EG(ctx:Context){
	var exit by remember{mutableStateOf(false)}
	BackHandler(enabled=true){exit=true}
	if(exit){
		AlertDialog(
			onDismissRequest={exit=false},
			shape=RoundedCornerShape(4.dp),
			title={Text("提示")},text={Text("确定要彻底退出应用吗？")},
			confirmButton={
				TextButton(onClick={(ctx as?Activity)?.finish()}){Text("确认退出")}
			},
			dismissButton={
				TextButton(onClick={exit=false}){Text("取消")}
			}
		)
	}
}

@Composable // 入口
fun SPA(){
	val ctx=LocalContext.current
	val nav=rememberNavController()
	val gs=remember{mutableStateListOf<LG>()}
	var sg by remember{mutableStateOf(true)}
	val fm=LocalFocusManager.current // 获取 Compose 焦点管理器

	Box(
		modifier=Modifier.fillMaxSize().pointerInput(Unit){
			awaitPointerEventScope{
				while(true){
					val e=awaitPointerEvent()
					// 仅在按下阶段触发，避免干扰滑动/长按等手势
					if(e.changes.any{it.pressed})fm.clearFocus()
				}
			}
		}
	){
		val log=remember{
			{w:String,o:String,c:LT->
				val t=SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date())
				gs.add(LG(t=t,w=w,o=o,c=c))
			}
		}

		LaunchedEffect(Unit){log("系统","仏琰 初始化就绪",LT.S)}

		val c=LocalConfiguration.current
		val ce by nav.currentBackStackEntryAsState()
		val tv=c.uiMode and Configuration.UI_MODE_TYPE_MASK==Configuration.UI_MODE_TYPE_TELEVISION

		NavHost(navController=nav,startDestination="home"){
			composable("home"){
				EG(ctx)
				Home(tv=tv,sg=sg,
					tg={
						sg=!sg
						log("首页","日志面板显示开关变更为: $sg",LT.I)
					},
					go={route->
						log("导航","准备切换至页面: [$route]",LT.S)
						nav.navigate(route)
					},
					test={
						log("测试","模拟点击卡片操作",LT.S)
					}
				)
			}
			composable("setting"){
				Setting(
					back={
						log("导航","返回至首页",LT.I)
						nav.popBackStack()
					},
					save={k,v->
						log("设置页","配置项 [$k] 已存为: $v",LT.S)
					}
				)
			}
		}

		if(sg)LP(
			modifier=Modifier.align(Alignment.BottomCenter),
			tv=tv,list=gs,remove={id->gs.removeAll{it.i==id}}
		)
	}
}

@Composable // 首页
fun Home(tv:Boolean,sg:Boolean,tg:()->Unit,go:(String)->Unit,test:()->Unit){
	Column(modifier=Modifier.fillMaxSize()){
		Row(
			modifier=Modifier.fillMaxWidth().statusBarsPadding()
				.height(48.dp).padding(start=10.dp,end=3.dp),
			horizontalArrangement=Arrangement.SpaceBetween,
			verticalAlignment=Alignment.CenterVertically
		){
			Text("首页",style=MaterialTheme.typography.titleLarge)
			Row(verticalAlignment=Alignment.CenterVertically){
				Text(
					text=if(tv)"📺 TV"else"📱 MB",
					style=MaterialTheme.typography.bodyMedium,
					modifier=Modifier.padding(end=8.dp)
				)
				IconButton(onClick=tg,modifier=Modifier.size(36.dp)){
					Icon(
						painter=painterResource(if(sg)R.drawable.visibility else R.drawable.visibility_off),
						contentDescription="切换日志面板显示开关",
						modifier=Modifier.size(20.dp)
					)
				}
			}
		}
		Spacer(modifier=Modifier.height(4.dp))
		Column(modifier=Modifier.padding(horizontal=10.dp)){
			CD(title="自动化参数设置",desc="内置无缝响应式卡片、表单策略与持久化管理",click={go("setting")})
			Spacer(modifier=Modifier.height(6.dp))
			CD(title="手动投递诊断日志",desc="向贴底面板追加一条模拟警告事件进行视图验证",click={test()})
		}
	}
}

@Composable // 卡片
fun CD(title:String,desc:String,click:()->Unit){
	val fr=remember{FocusRequester()}
	val ms=remember{MutableInteractionSource()}
	val fs by ms.collectIsFocusedAsState()
	val sp=RoundedCornerShape(5.dp)
	val ss by animateDpAsState(
		targetValue=if(fs)6.dp else 1.dp,
		label="card_shadow"
	)

	Card(
		onClick=click,
		interactionSource=ms,shape=sp,
		modifier=Modifier.fillMaxWidth().focusRequester(fr)
			.padding(bottom=6.dp).shadow(ss,sp).border(
				width=1.5.dp,shape=sp,
				color=if(fs)MaterialTheme.colorScheme.primary else Color.Transparent
			),
		colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)
	){
		Box(
			modifier=Modifier.fillMaxWidth()
				.padding(horizontal=12.dp,vertical=10.dp).clip(sp),
			contentAlignment=Alignment.CenterStart
		){
			Column{
				Text(title,style=MaterialTheme.typography.titleMedium)
				Spacer(modifier=Modifier.height(2.dp))
				Text(desc,style=MaterialTheme.typography.bodySmall,color=Color.Gray,maxLines=2)
			}
		}
	}
}

@Composable // 设置页面
fun Setting(back:()->Unit,save:(String,String)->Unit){
	var field by remember{mutableStateOf("")}
	var s by remember{mutableStateOf(true)}
	val sp=RoundedCornerShape(5.dp)
	Column(
		modifier=Modifier.fillMaxSize()
			.statusBarsPadding().verticalScroll(rememberScrollState())
	){
		Row(
			verticalAlignment=Alignment.CenterVertically,
			modifier=Modifier.height(48.dp).padding(start=1.dp,end=10.dp)
		){
			IconButton(onClick=back,modifier=Modifier.size(36.dp)){
				Icon(
					painter=painterResource(R.drawable.arrow_back),
					contentDescription="返回",
					modifier=Modifier.size(20.dp)
				)
			}
			Text("系统配置",style=MaterialTheme.typography.titleLarge)
		}
		Spacer(modifier=Modifier.height(4.dp))
		Card(
			shape=sp,
			modifier=Modifier.fillMaxWidth().padding(horizontal=10.dp),
			colors=CardDefaults.cardColors(
				containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
			)
		){
			Column(modifier=Modifier.animateContentSize().clip(sp)){
				Row(
					modifier=Modifier.fillMaxWidth()
						.padding(start=10.dp,end=8.dp,top=10.dp,bottom=10.dp),
					horizontalArrangement=Arrangement.SpaceBetween,
					verticalAlignment=Alignment.CenterVertically
				){
					Text("测试数据",style=MaterialTheme.typography.titleMedium)
					IconButton(onClick={s=!s},modifier=Modifier.size(24.dp)){
						Icon(
							painter=painterResource(if(s)R.drawable.expand_less else R.drawable.expand_more),
							contentDescription=if(s)"折叠"else"展开"
						)
					}
				}
				if(s){
					HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.7f))
					OutlinedTextField(
						value=field,onValueChange={field=it},label={Text("关联数据")},
						modifier=Modifier.fillMaxWidth()
							.padding(top=6.dp,bottom=10.dp,start=10.dp,end=10.dp),
						singleLine=true,
						keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),
						keyboardActions=KeyboardActions(onDone={save("TEST",field)})
					)
				}
			}
		}
	}
}

@Composable // 日志面板
fun LP(modifier:Modifier=Modifier,tv:Boolean,list:List<LG>,remove:(String)->Unit){
	var s by remember{mutableStateOf(false)}
	val c=LocalConfiguration.current
	val h=c.screenHeightDp.dp
	if(!s){
		LPX(
			modifier=modifier,height=h/3,
			tv=tv,list=list,remove=remove,click={s=true}
		)
	}else{
		LPO(modifier=modifier,click={s=false})
	}
}

// 展开态独立 Composable：拖拽偏移状态 y 仅影响此层，不触发 LP 父层重组
@Composable
fun LPX(modifier:Modifier,tv:Boolean,height:androidx.compose.ui.unit.Dp,list:List<LG>,remove:(String)->Unit,click:()->Unit){
	val sp=RoundedCornerShape(topStart=6.dp,topEnd=6.dp)
	var y by remember{mutableStateOf(0f)}
	val s=rememberLazyListState()
	LaunchedEffect(list.lastOrNull()?.i){
		if(list.isNotEmpty())s.animateScrollToItem(list.size-1)
	}
	Box(
		modifier=modifier.fillMaxWidth().heightIn(max=height)
			.padding(horizontal=0.5.dp).navigationBarsPadding()
			.offset{IntOffset(0,y.roundToInt())}.clip(sp)
			.background(MaterialTheme.colorScheme.surface.copy(alpha=0.90f))
			.border(1.dp,Color.Gray.copy(alpha=0.15f),sp)
			.pointerInput(Unit){
				detectDragGestures(
					// 松手偏移超过 150px 则折叠，偏移归零
					onDragEnd={if(y>150f)click();y=0f},
					// 向下拖时累加偏移，不允许向上拖（y 不低于 0）
					onDrag={change,drag->change.consume();if(y+drag.y>=0f)y+=drag.y}
				)
			}
	){
		Column(modifier=Modifier.fillMaxSize().padding(horizontal=5.dp,vertical=2.dp)){
			Box( // 顶部拖拽指示横线
				modifier=Modifier
					.width(64.dp).height(3.dp)
					.background(Color.Gray.copy(alpha=0.4f),RoundedCornerShape(1.5.dp))
					.align(Alignment.CenterHorizontally).padding(top=1.6.dp).clickable(enabled=tv){click()}
			)
			LazyColumn(state=s,modifier=Modifier.fillMaxSize().padding(top=6.dp)){
				items(list,key={it.i}){g->
					Row(
						modifier=Modifier.fillMaxWidth().padding(vertical=1.dp),
						horizontalArrangement=Arrangement.SpaceBetween,
						verticalAlignment=Alignment.Top
					){
						val color=when(g.c){
							LT.S->Color(0xFF189B46) // 成功-绿色
							LT.E->Color(0xFFE7012F) // 错误-红色
							LT.W->Color(0xFFFDD10D) // 警告-黄色
							LT.I->MaterialTheme.colorScheme.onSurface // 信息-跟随主题
						}
						Text(
							text="[${g.t}] ${g.w} ➜ ${g.o}",
							color=color,
							style=MaterialTheme.typography.bodySmall.copy(
								lineHeight=1.2.em,
								fontFamily=FontFamily.Monospace
							),
							modifier=Modifier.weight(1f).padding(PaddingValues(end=4.dp))
						)
						IconButton(
							onClick={remove(g.i)},
							modifier=Modifier.size(14.dp).align(Alignment.CenterVertically)
						){
							Icon(
								painter=painterResource(R.drawable.delete),
								contentDescription="删除此条日志",
								tint=Color.Gray.copy(alpha=0.7f),
								modifier=Modifier.size(12.dp)
							)
						}
					}
					HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
				}
			}
		}
	}
}

// 折叠态独立 Composable，状态极简，展开态重组时完全不影响此处
@Composable
fun LPO(modifier:Modifier,click:()->Unit){
	Box(
		modifier=modifier.padding(bottom=6.dp)
			.navigationBarsPadding().width(80.dp).height(5.dp)
			.background(Color.Gray.copy(alpha=0.5f),RoundedCornerShape(2.5.dp))
			.clickable{click()} // 点击展开面板
	)
}
