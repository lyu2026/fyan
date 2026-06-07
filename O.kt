package com.fyan.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.UUID
import kotlin.math.roundToInt

// ── 入口 ──────────────────────────────────────────────────────

class MainActivity:ComponentActivity(){
	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent{
			val dark=(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES
			CompositionLocalProvider(Fyan.LC provides Fyan.color(dark)){App()}
		}
	}
}

// ── 导航 ──────────────────────────────────────────────────────

@Composable
private fun App(){
	val nav=rememberNavController()
	Box(modifier="fs".css()){
		NavHost(navController=nav,startDestination="home"){
			composable("home"){HomeScreen(nav)}
			composable("detail/{id}"){back->
				DetailScreen(nav,back.arguments?.getString("id")?:"")
			}
		}
		Box(modifier="abc".css()){Fyan.LogPanel()}
	}
}

// ── 首页数据 ──────────────────────────────────────────────────

private data class CardItem(val id:String,val title:String,val desc:String,val tag:String)

private val cards=listOf(
	CardItem("network","网络请求","封装 OkHttp / Retrofit，支持拦截器与超时配置","网络"),
	CardItem("storage","本地存储","Room 数据库 + DataStore 偏好设置一站式管理","存储"),
	CardItem("ui","UI 组件库","自定义 Compose 组件集合，开箱即用","界面"),
	CardItem("media","媒体播放","音视频统一播放器，支持本地与流媒体","媒体"),
	CardItem("push","消息推送","厂商通道聚合，统一推送 SDK 接入","推送"),
	CardItem("log","日志系统","多级别结构化日志，支持文件输出与远程上报","调试"),
)

// ── 首页 ──────────────────────────────────────────────────────

@Composable
fun HomeScreen(nav:NavController){
	val c=Fyan.LC.current
	LaunchedEffect(Unit){Fyan.log("HomeScreen","页面初始化",'i')}
	Box(modifier="fs".css().background(c.b)){
		Column(modifier="fs".css()){
			// 顶栏
			Row(
				modifier="fw h56 ph16 psb".css().background(c.s)
					.border(0.5.dp,c.ov),
				verticalAlignment=Alignment.CenterVertically,
				horizontalArrangement=Arrangement.SpaceBetween,
			){
				BasicText("首页",style=Fyan.TL.copy(color=c.os))
				Box(
					modifier="fw36 fh36 c8".css()
						.background(if(Fyan.logs_fold)c.sv else c.p.copy(alpha=0.15f))
						.clickable{
							Fyan.logs_fold=!Fyan.logs_fold
							Fyan.log("TopBar","日志面板${if(!Fyan.logs_fold)"展开"else"折叠"}",'i')
						},
					contentAlignment=Alignment.Center,
				){BasicText("◉",style=TextStyle(fontSize=18.sp,color=if(Fyan.logs_fold)c.os.copy(alpha=0.6f)else c.p))}
			}
			// 卡片列表
			LazyColumn(
				modifier="fw".css().weight(1f),
				contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),
				verticalArrangement=Arrangement.spacedBy(10.dp),
			){
				items(cards){card->
					HomeCard(card,c){
						Fyan.log("HomeCard","点击 → ${card.title}",'u')
						nav.navigate("detail/${card.id}")
					}
				}
				item{Spacer(modifier="h80".css())}
			}
		}
	}
}

// ── 首页卡片 ──────────────────────────────────────────────────

@Composable
private fun HomeCard(card:CardItem,c:Fyan.tc,onClick:()->Unit){
	Box(
		modifier="fw c12".css().background(c.s)
			.border(0.5.dp,c.ov,RoundedCornerShape(12.dp))
			.clickable(onClick=onClick)
	){
		Column(modifier="fw ph16 pv14".css()){
			Row(
				modifier="fw".css(),
				verticalAlignment=Alignment.CenterVertically,
				horizontalArrangement=Arrangement.SpaceBetween,
			){
				BasicText(card.title,style=Fyan.TM.copy(color=c.os))
				Box(modifier="c4 ph8 pv2".css().background(c.p.copy(alpha=0.12f))){
					BasicText(card.tag,style=Fyan.BS.copy(color=c.p))
				}
			}
			Spacer(modifier="h6".css())
			Box(modifier="fw h0.5".css().background(c.ov))
			Spacer(modifier="h8".css())
			BasicText(card.desc,style=Fyan.BM.copy(color=c.os.copy(alpha=0.7f)))
			Spacer(modifier="h10".css())
			Row(modifier="fw".css(),horizontalArrangement=Arrangement.End){
				BasicText("查看详情  →",style=Fyan.BS.copy(color=c.p))
			}
		}
	}
}

// ── 详情页 ────────────────────────────────────────────────────

@Composable
fun DetailScreen(nav:NavController,id:String){
	val c=Fyan.LC.current
	val card=cards.find{it.id==id}
	LaunchedEffect(id){Fyan.log("DetailScreen","进入 id=$id",'i')}
	Box(modifier="fs".css().background(c.b)){
		Column(modifier="fs".css()){
			// 顶栏
			Row(
				modifier="fw h56 ph16 psb".css().background(c.s)
					.border(0.5.dp,c.ov),
				verticalAlignment=Alignment.CenterVertically,
				horizontalArrangement=Arrangement.spacedBy(12.dp),
			){
				Box(
					modifier="fw36 fh36 c8".css().background(c.sv)
						.clickable{nav.popBackStack()},
					contentAlignment=Alignment.Center,
				){BasicText("←",style=TextStyle(fontSize=18.sp,color=c.os))}
				BasicText(card?.title?:"详情",style=Fyan.TL.copy(color=c.os))
			}
			// 内容区
			LazyColumn(
				modifier="fw".css().weight(1f),
				contentPadding=PaddingValues(horizontal=16.dp,vertical=16.dp),
				verticalArrangement=Arrangement.spacedBy(12.dp),
			){
				if(card!=null){
					item{
						Box(modifier="fw c12".css().background(c.s).border(0.5.dp,c.ov,RoundedCornerShape(12.dp))){
							Column(modifier="fw ph16 pv14".css()){
								BasicText("模块说明",style=Fyan.BS.copy(color=c.os.copy(alpha=0.5f)))
								Spacer(modifier="h6".css())
								BasicText(card.desc,style=Fyan.BM.copy(color=c.os))
							}
						}
					}
					items(3){i->
						Box(modifier="fw c12".css().background(c.sv).border(0.5.dp,c.ov,RoundedCornerShape(12.dp))){
							Column(modifier="fw ph16 pv14".css()){
								BasicText("功能项 ${i+1}",style=Fyan.TM.copy(color=c.os))
								Spacer(modifier="h4".css())
								BasicText("${card.title} 相关功能说明与配置项 ${i+1}",style=Fyan.BM.copy(color=c.os.copy(alpha=0.65f)))
							}
						}
					}
					item{Spacer(modifier="h80".css())}
				}else{
					item{BasicText("未找到模块（id: $id）",style=Fyan.BM.copy(color=c.os))}
				}
			}
		}
	}
}

// ── Fyan 全局对象 ─────────────────────────────────────────────

object Fyan{
	val logs=mutableStateListOf<String>()
	var logs_fold by mutableStateOf(true)
	var logs_hide by mutableStateOf(true)
	var logs_y by mutableStateOf(0f)

	fun log(m:String,o:String,c:Char='i'){
		val t=java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
		val x=when(c){
			'i'->"#2196F3"
			'u'->"#9C27B0"
			'e'->"#F44336"
			's'->"#00BCD4"
			'n'->"#4CAF50"
			'w'->"#FF9800"
			else->"#9E9E9E"
		}
		logs.add("${UUID.randomUUID().toString().replace("-","")}.$x●$t $m ➜ $o")
	}
	fun log_clear()=logs.clear()
	fun log_remove(i:String)=logs.removeAll{it.startsWith(i)}

	@Composable fun LogPanel(){if(logs_fold)LogHide()else LogShow()}

	@Composable
	private fun LogHide(){
		Box(
			modifier="fw h5 c2.5".css().background(Color(0x80808080)).clickable{logs_fold=false},
			contentAlignment=Alignment.Center,
		){BasicText("· · ·  日志  · · ·",style=BS.copy(color=Color.White.copy(alpha=0.7f)))}
	}

	@Composable
	private fun LogShow(){
		val lazy=rememberLazyListState()
		LaunchedEffect(logs.lastOrNull()){if(logs.isNotEmpty())lazy.animateScrollToItem(logs.size-1)}
		val maxH=LocalConfiguration.current.screenHeightDp.dp/3
		val isTv=(LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
		Box(
			modifier="fw br6,6,0,0".css()
				.heightIn(max=maxH)
				.offset(y=logs_y.roundToInt().dp)
				.background(Color(0xEB1C1C1E))
				.border(0.5.dp,Color(0x26808080),RoundedCornerShape(topStart=6.dp,topEnd=6.dp))
				.pointerInput(Unit){
					detectDragGestures(
						onDragEnd={if(logs_y>150f)logs_fold=true else logs_y=0f},
						onDrag={ch,d->ch.consume();if(logs_y+d.y>=0f)logs_y+=d.y}
					)
				}
		){
			Column(modifier="fw pv2 ph5".css()){
				// 拖拽把手
				Box(modifier="fw h16".css(),contentAlignment=Alignment.Center){
					Box(
						modifier="fw40 fh3 c2".css().background(Color(0x66808080))
							.clickable(enabled=isTv){logs_fold=true}
							.pointerInput(!isTv){if(!isTv)detectTapGestures(onTap={logs_fold=true})}
					)
				}
				// 工具栏
				Row(
					modifier="fw ph4 pv2".css(),
					horizontalArrangement=Arrangement.SpaceBetween,
					verticalAlignment=Alignment.CenterVertically,
				){
					BasicText("日志 · ${logs.size} 条",style=BS.copy(color=Color(0xFF9E9E9E),fontFamily=FontFamily.Monospace))
					Box(
						modifier="c4".css().background(Color(0x33F44336))
							.clickable{log_clear()}.then("ph8 pv2".css())
					){BasicText("清空",style=BS.copy(color=Color(0xFFF44336)))}
				}
				// 日志列表
				LazyColumn(state=lazy,modifier="fw pt4".css()){
					items(logs){entry->
						val parts=entry.split("●",limit=2)
						val dot=parts[0].lastIndexOf('.')
						val id=if(dot>0)parts[0].substring(0,dot)else parts[0]
						val hex=if(dot>0)parts[0].substring(dot+1)else"#9E9E9E"
						val ec=try{Color(android.graphics.Color.parseColor(hex))}catch(_:Exception){Color(0xFF9E9E9E)}
						Row(
							modifier="fw pt1".css(),
							verticalAlignment=Alignment.Top,
							horizontalArrangement=Arrangement.SpaceBetween,
						){
							BasicText(
								"● ${parts.getOrElse(1){""}}",
								style=BS.copy(color=ec,lineHeight=1.2.em,fontFamily=FontFamily.Monospace),
								modifier=Modifier.weight(1f).then("pe4".css()),
							)
							Box(
								modifier="fw24 fh24 c".css().clickable{log_remove(id)},
								contentAlignment=Alignment.Center,
							){BasicText("✕",style=BS.copy(color=Color(0xFF9E9E9E)))}
						}
						Box(modifier="fw h0.5".css().background(Color(0x1A808080)))
					}
				}
			}
		}
	}

	data class tc(val p:Color,val b:Color,val s:Color,val sv:Color,val o:Color,val os:Color,val ov:Color)

	fun color(dark:Boolean=true)=if(dark)tc(
		p=Color(0xFF8AB4F8),b=Color(0xFF1C1C1E),s=Color(0xFF2C2C2E),
		sv=Color(0xFF3A3A3C),o=Color(0xFF938F99),os=Color(0xFFE6E1E5),ov=Color(0xFF49454F),
	)else tc(
		p=Color(0xFF1A73E8),b=Color(0xFFF5F5F5),s=Color(0xFFFFFFFF),
		sv=Color(0xFFE8EAF6),o=Color(0xFFCAC4D0),os=Color(0xFF1C1B1F),ov=Color(0xFFE7E0EC),
	)

	val LC=staticCompositionLocalOf{color()}
	val TL get()=TextStyle(fontSize=22.sp,fontWeight=FontWeight.W600,lineHeight=28.sp)
	val TM get()=TextStyle(fontSize=16.sp,fontWeight=FontWeight.W500,lineHeight=24.sp)
	val BM get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W400,lineHeight=20.sp)
	val BS get()=TextStyle(fontSize=12.sp,fontWeight=FontWeight.W400,lineHeight=16.sp)
}

// ── css() DSL ─────────────────────────────────────────────────

private fun String.css():Modifier{
	var m=Modifier
	for(it in split(" ")){
		val s=it
		when(s[0]){
			'f'->when{
				s=="fs"->m=m.fillMaxSize()
				s.startsWith("fw")->{val v=s.drop(2);m=if(v.isEmpty())m.fillMaxWidth()else m.width(v.toDoubleOrNull()?.dp?:0.dp)}
				s.startsWith("fh")->{val v=s.drop(2);m=if(v.isEmpty())m.fillMaxHeight()else m.height(v.toDoubleOrNull()?.dp?:0.dp)}
			}
			'w'->{val v=s.drop(1)
				when{
					v=="f"->m=m.weight(1f)
					v.toDoubleOrNull()!=null->m=m.weight(v.toDoubleOrNull()?.toFloat()?:0f)
					else->{m=when{v.isEmpty()->m.fillMaxWidth()
						v[0]=='>'->{m.widthIn(min=v.drop(1).toDoubleOrNull()?.dp?:0.dp)}
						v[0]=='<'->{m.widthIn(max=v.drop(1).toDoubleOrNull()?.dp?:Dp.Unspecified)}
						else->m.width(v.toDoubleOrNull()?.dp?:0.dp)}}
				}}
			'h'->{val v=s.drop(1);m=when{v.isEmpty()->m.fillMaxHeight()
				v[0]=='>'->{m.heightIn(min=v.drop(1).toDoubleOrNull()?.dp?:0.dp)}
				v[0]=='<'->{m.heightIn(max=v.drop(1).toDoubleOrNull()?.dp?:Dp.Unspecified)}
				else->m.height(v.toDoubleOrNull()?.dp?:0.dp)}}
			'p'->{val v=s.drop(1)
				when{
					v=="nb"->m=m.navigationBarsPadding()
					v=="sb"->m=m.statusBarsPadding()
					v=="bs"->m=m.systemBarsPadding()
					v=="im"->m=m.imePadding()
					else->{val n=v.dropWhile{!it.isDigit()}.toDoubleOrNull()?.dp?:0.dp
						when{
							v=="t"||v.startsWith("t")&&v.drop(1).toDoubleOrNull()!=null->m=m.padding(top=n)
							v=="b"||v.startsWith("b")&&v.drop(1).toDoubleOrNull()!=null->m=m.padding(bottom=n)
							v=="s"||v.startsWith("s")&&v.drop(1).toDoubleOrNull()!=null->m=m.padding(start=n)
							v=="e"||v.startsWith("e")&&v.drop(1).toDoubleOrNull()!=null->m=m.padding(end=n)
							v=="v"||v.startsWith("v")&&v.drop(1).toDoubleOrNull()!=null->m=m.padding(vertical=n)
							v=="h"||v.startsWith("h")&&v.drop(1).toDoubleOrNull()!=null->m=m.padding(horizontal=n)
							v.toDoubleOrNull()!=null->m=m.padding(n)
						}}
				}}
			'z'->{val v=s.drop(1)
				when{
					v.startsWith("x")->{val n=v.drop(1).toDoubleOrNull()?.dp?:0.dp;m=m.offset(x=n)}
					v.startsWith("y")->{val n=v.drop(1).toDoubleOrNull()?.dp?:0.dp;m=m.offset(y=n)}
				}}
			'g'->{val v=s.drop(1)
				val hex=v.takeWhile{it.isLetterOrDigit()}
				val alpha=v.drop(hex.length).let{if(it.startsWith("."))it.drop(1).toDoubleOrNull()?.toFloat()?:1f else 1f}
				val color=Color(android.graphics.Color.parseColor("#$hex"))
				val fc=if(alpha!=1f)color.copy(alpha=alpha)else color
				when{
					v.endsWith("c")->m=m.clip(CircleShape).background(fc)
					v.endsWith("r")->{val r=v.dropLast(1).takeLastWhile{it.isDigit()}.toDoubleOrNull()?.dp?:0.dp;m=m.clip(RoundedCornerShape(r)).background(fc)}
					else->m=m.background(fc)
				}}
			'b'->{val v=s.drop(1)
				when{
					v.startsWith("r")->{val r=v.drop(1).split(",").mapNotNull{it.toDoubleOrNull()?.dp}
						val shape=when(r.size){1->RoundedCornerShape(r[0]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)}
						m=m.clip(shape)}
					else->{val w=v.takeWhile{it.isDigit()}.toDoubleOrNull()?.dp?:1.dp
						val hex=v.drop(w.toString().length).takeWhile{it.isLetterOrDigit()}
						val alpha=v.drop(w.toString().length+hex.length).let{if(it.startsWith("."))it.drop(1).toDoubleOrNull()?.toFloat()?:1f else 1f}
						val color=if(hex.isNotEmpty())Color(android.graphics.Color.parseColor("#$hex"))else Color.Black
						val fc=if(alpha!=1f)color.copy(alpha=alpha)else color
						m=m.border(w,fc)}
				}}
			'c'->{val v=s.drop(1)
				when{
					v.isEmpty()->m=m.clip(CircleShape)
					v.toDoubleOrNull()!=null->{val r=v.toDoubleOrNull()?.dp?:0.dp;m=m.clip(RoundedCornerShape(r))}
					v.contains(",")->{val r=v.split(",").mapNotNull{it.toDoubleOrNull()?.dp}
						val shape=when(r.size){1->RoundedCornerShape(r[0]);2->RoundedCornerShape(r[0],r[1]);3->RoundedCornerShape(r[0],r[1],r[2]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)}
						m=m.clip(shape)}
					else->m=m.clip(CircleShape)
				}}
			'a'->{val v=s.drop(1)
				val align=when(v){
					"c"->Alignment.Center;"cs"->Alignment.CenterStart;"ce"->Alignment.CenterEnd
					"ts"->Alignment.TopStart;"tc"->Alignment.TopCenter;"te"->Alignment.TopEnd
					"bs"->Alignment.BottomStart;"bc"->Alignment.BottomCenter;"be"->Alignment.BottomEnd
					else->null
				}
				if(align!=null)m=m.align(align)
			}
		}
	}
	return m
}