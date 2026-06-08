package com.fyan

import android.content.res.Configuration
import android.graphics.Color as AC
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.UUID
import kotlin.math.roundToInt

// ════════════════════════════════════════════════════════════════
// FYAN — 全局单例：主题 / 日志 / 排版 / 状态 / 网络
// ════════════════════════════════════════════════════════════════
object Fyan{

	// ── 颜色主题 ────────────────────────────────────────────────
	data class tc(
		val p:Color,// primary
		val b:Color,// background
		val s:Color,// surface
		val sv:Color,// surface-variant
		val o:Color,// outline
		val os:Color,// on-surface
		val ov:Color,// outline-variant
	)
	fun color(dark:Boolean=true)=if(dark) tc(
		p=Color(0xFF8AB4F8),
		b=Color(0xFF1C1C1E),
		s=Color(0xFF2C2C2E),
		sv=Color(0xFF3A3A3C),
		o=Color(0xFF938F99),
		os=Color(0xFFE6E1E5),
		ov=Color(0xFF49454F),
	)else tc(
		p=Color(0xFF1A73E8),
		b=Color(0xFFF5F5F5),
		s=Color(0xFFFFFFFF),
		sv=Color(0xFFE8EAF6),
		o=Color(0xFFCAC4D0),
		os=Color(0xFF1C1B1F),
		ov=Color(0xFFE7E0EC),
	)

	val LC=staticCompositionLocalOf{color()}

	// ── 排版 ────────────────────────────────────────────────────
	val TL get()=TextStyle(fontSize=22.sp,fontWeight=FontWeight.W600,lineHeight=28.sp)
	val TM get()=TextStyle(fontSize=16.sp,fontWeight=FontWeight.W500,lineHeight=24.sp)
	val TS get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W500,lineHeight=20.sp)
	val BM get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W400,lineHeight=20.sp)
	val BS get()=TextStyle(fontSize=12.sp,fontWeight=FontWeight.W400,lineHeight=16.sp)

	// ── 日志 ────────────────────────────────────────────────────
	val logs=mutableStateListOf<String>()
	var logs_fold by mutableStateOf(false)
	var logs_hide by mutableStateOf(false)
	var logs_y by mutableStateOf(0f)

	fun log(m:String,o:String,c:Char='i'){
		val t=java.time.LocalTime.now()
			.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
		val x=when(c){
			'i'->"#2196F3";'u'->"#9C27B0";'e'->"#F44336"
			's'->"#00BCD4";'n'->"#4CAF50";'w'->"#FF9800"
			else->"#9E9E9E"
		}
		logs.add("${UUID.randomUUID().toString().replace("-","")}.$x●$t $m ➜ $o")
	}

	fun log_clear()=logs.clear()
	fun log_remove(i:String)=logs.removeAll{it.startsWith(i)}

	@Composable fun LogPanel(){if(logs_fold)LogHide()else LogShow()}
	@Composable private fun LogHide(){
		Box(
			modifier="fw pnb h5 c2.5".css().background(Color(0x80808080)).clickable{logs_fold=false},
			contentAlignment=Alignment.Center,
		){BasicText("· · ·  日志  · · ·",style=BS.copy(color=Color.White.copy(alpha=0.7f)))}
	}
	@Composable private fun LogShow(){
		val lazy=rememberLazyListState()
		LaunchedEffect(logs.lastOrNull()){if(logs.isNotEmpty())lazy.animateScrollToItem(logs.size-1)}
		val maxH=LocalConfiguration.current.screenHeightDp.dp/3
		val isTv=(LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
		Box(
			modifier="fw pnb br6,6,0,0 b0.5,808080,0.70 h>${maxH}".css()
				.offset(y=logs_y.roundToInt().dp).background(Color(0xEB1C1C1E))
				.pointerInput(Unit){
					detectDragGestures(
						onDragEnd={if(logs_y>100f)logs_fold=true else logs_y=0f},
						onDrag={ch,d->ch.consume();if(logs_y+d.y>=0f)logs_y+=d.y}
					)
				}
		){
			Column(modifier="fw pv2 ph5".css()){
				Box(modifier="fw h16".css(),contentAlignment=Alignment.Center){
					Box(
						modifier="fw40 fh3 c2".css().background(Color(0x66808080))
							.clickable(enabled=isTv){logs_fold=true}
							.pointerInput(!isTv){if(!isTv) detectTapGestures(onTap={logs_fold=true})}
					)
				}
				Row(
					modifier="fw ph4 pv2".css(),
					horizontalArrangement=Arrangement.SpaceBetween,
					verticalAlignment=Alignment.CenterVertically,
				){
					BasicText("日志 · ${logs.size}条",
						style=BS.copy(color=Color(0xFF9E9E9E),fontFamily=FontFamily.Monospace))
					Box(
						modifier="c4".css().background(Color(0x33F44336))
							.clickable{log_clear()}.then("ph8 pv2".css())
					){BasicText("清空",style=BS.copy(color=Color(0xFFF44336)))}
				}
				LazyColumn(state=lazy,modifier="fw pt4".css()){
					items(logs){entry->
						val parts=entry.split("●",limit=2)
						val dot=parts[0].lastIndexOf('.')
						val id=if(dot > 0) parts[0].substring(0,dot) else parts[0]
						val hex=if(dot > 0) parts[0].substring(dot+1) else "#9E9E9E"
						val ec=try{Color(AC.parseColor(hex))}catch(_:Exception){Color(0xFF9E9E9E)}
						Row(
							modifier="fw pt1".css(),
							verticalAlignment=Alignment.Top,
							horizontalArrangement=Arrangement.SpaceBetween,
						){
							BasicText(
								parts.getOrElse(1){""},
								style=BS.copy(color=ec,lineHeight=1.2.em,fontFamily=FontFamily.Monospace),
								modifier="e1 pe4".css(),
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

	// ── 全局页面状态 ────────────────────────────────────────────
	/** 首页当前激活 tab id；null 时首次渲染从 SharedPreferences 恢复 */
	var homeTab by mutableStateOf<String?>(null)
	/** 历史记录列表 */
	val history=mutableStateListOf<VideoItem>()
	/** 视频基本信息（用于历史记录卡片） */
	data class VideoItem(
		val id:String,
		val title:String,
		val poster:String,
		val progress:String="",// 观看到的集数描述
	)
}

// ════════════════════════════════════════════════════════════════
// 通用组件库
// 规则：modifier 参数只能通过 "...".css() 传入；
// 组件内部的 modifier 也只用 "...".css() 拼链，
// 绝不在组件外部或调用处追加任何样式链方法。
// ════════════════════════════════════════════════════════════════

// ── TopBar ──────────────────────────────────────────────────────
@Composable
fun TopBar(
	title:String="",
	onBack:(()->Unit)?=null,
	end:@Composable RowScope.()->Unit={},
){
	val c=Fyan.LC.current
	Row(
		modifier="fw h56 ph12 psb".css()
			.background(c.s)
			.border(0.5.dp,c.ov),
		verticalAlignment=Alignment.CenterVertically,
		horizontalArrangement=Arrangement.SpaceBetween,
	){
		Row(verticalAlignment=Alignment.CenterVertically){
			if(onBack!=null){
				IconBtn(label="←",modifier="fw36 fh36 c8".css().background(c.sv),onClick=onBack)
				if(title.isNotEmpty()) Spacer(modifier="fw12".css())
			}
			if(title.isNotEmpty())
				BasicText(title,
					style=Fyan.TL.copy(color=c.os),maxLines=1,
					overflow=TextOverflow.Ellipsis,modifier="e1".css())
		}
		Row(verticalAlignment=Alignment.CenterVertically){end()}
	}
}

// ── IconBtn ──────────────────────────────────────────────────────
@Composable
fun IconBtn(
	label:String,
	modifier:Modifier,
	onClick:()->Unit,
){
	val c=Fyan.LC.current
	Box(
		modifier=modifier.clickable(onClick=onClick),
		contentAlignment=Alignment.Center,
	){BasicText(label,style=TextStyle(fontSize=18.sp,color=c.os))}
}

// ── LoadingCenter ───────────────────────────────────────────────
@Composable
fun LoadingCenter(text:String="加载中…"){
	val c=Fyan.LC.current
	Box(modifier="fs".css(),contentAlignment=Alignment.Center){
		Column(horizontalAlignment=Alignment.CenterHorizontally){
			BasicText("◌",style=TextStyle(fontSize=32.sp,color=c.p))
			Spacer(modifier="h8".css())
			BasicText(text,style=Fyan.BS.copy(color=c.os.copy(alpha=0.6f)))
		}
	}
}

// ── VideoCard（竖型卡片）────────────────────────────────────────
@Composable
fun VideoCard(
	poster:String,
	title:String,
	sub:String,
	modifier:Modifier,
	onClick:()->Unit,
	onLongPress:(()->Unit)?=null,
){
	val c=Fyan.LC.current
	Box(
		modifier=modifier
			.clip(RoundedCornerShape(10.dp))
			.background(c.sv)
			.then(
				if(onLongPress!=null)
					Modifier.pointerInput(Unit){
						detectTapGestures(onTap={onClick()},onLongPress={onLongPress()})
					}
				else
					Modifier.clickable(onClick=onClick)
			)
	){
		Column{
			Box(
				modifier="fw".css()
					.aspectRatio(3f / 4f)
					.background(c.s)
			){
				AsyncImage(
					model=poster,
					contentDescription=title,
					contentScale=ContentScale.Crop,
					modifier="fs".css(),
				)
			}
			Column(modifier="fw ph8 pv6".css()){
				BasicText(title,
					style=Fyan.BS.copy(color=c.os),
					maxLines=2,
					overflow=TextOverflow.Ellipsis)
				if(sub.isNotEmpty()){
					Spacer(modifier="h2".css())
					BasicText(sub,style=Fyan.BS.copy(color=c.p),maxLines=1)
				}
			}
		}
	}
}

// ── ConfirmDialog ───────────────────────────────────────────────
@Composable
fun ConfirmDialog(
	text:String,
	confirmText:String="确认",
	cancelText:String="取消",
	onConfirm:()->Unit,
	onDismiss:()->Unit,
){
	val c=Fyan.LC.current
	Box(
		modifier="fs".css().background(Color(0x80000000)),
		contentAlignment=Alignment.Center,
	){
		Column(
			modifier="fw300 c12".css()
				.background(c.s)
				.border(0.5.dp,c.ov,RoundedCornerShape(12.dp))
				.then("ph20 pv20".css()),
			horizontalAlignment=Alignment.CenterHorizontally,
		){
			BasicText(text,style=Fyan.BM.copy(color=c.os,textAlign=TextAlign.Center))
			Spacer(modifier="h16".css())
			Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
				Box(
					modifier="fw ph20 pv10 c8".css()
						.background(c.sv)
						.clickable(onClick=onDismiss),
					contentAlignment=Alignment.Center,
				){BasicText(cancelText,style=Fyan.BM.copy(color=c.os))}
				Box(
					modifier="fw ph20 pv10 c8".css()
						.background(c.p.copy(alpha=0.15f))
						.clickable(onClick=onConfirm),
					contentAlignment=Alignment.Center,
				){BasicText(confirmText,style=Fyan.BM.copy(color=c.p))}
			}
		}
	}
}

// ── FilterTabRow ─────────────────────────────────────────────────
@Composable
fun FilterTabRow(
	fixedLabel:String,
	tabs:List<Pair<String,String>>,
	selected:String,
	onSelect:(String)->Unit,
){
	val c=Fyan.LC.current
	Row(
		modifier="fw h28".css().background(c.s),
		verticalAlignment=Alignment.CenterVertically,
	){
		Row(
			modifier="fh sh e1".css(),
			verticalAlignment=Alignment.CenterVertically,
		){
			tabs.forEach{(id,label)->
				val active=id==selected
				Box(
					modifier="fh ph2".css()
						.background(if(active)c.p.copy(alpha=0.15f)else Color.Transparent)
						.clickable{onSelect(id)},
					contentAlignment=Alignment.Center,
				){
					BasicText(
						" $label ",style=Fyan.BS.copy(
							color=if(active)c.p else c.os.copy(alpha=0.7f),
							fontWeight=if(active)FontWeight.W600 else FontWeight.W400,
						)
					)
				}
			}
		}
	}
}

@Composable fun EpisodeBtn(
	label:String,active:Boolean,modifier:Modifier,onClick:()->Unit){
	val c=Fyan.LC.current
	Box(
		modifier=modifier.clip(RoundedCornerShape(6.dp))
			.background(if(active)c.p else c.sv).clickable(onClick=onClick),
		contentAlignment=Alignment.Center,
	){
		BasicText(
			label,style=Fyan.BS.copy(
				color=if(active)c.b else c.os,
				fontWeight=if(active)FontWeight.W600 else FontWeight.W400,
			)
		)
	}
}

// ════════════════════════════════════════════════════════════════
// css() DSL — CW + Scope 自动解包
// ════════════════════════════════════════════════════════════════
class CW(val m:Modifier,val w:Float?=null,val a:Alignment?=null,val f:Boolean=true)
@Composable fun pcss(s:String):CW{
	var m:Modifier=Modifier;var w:Float?=null;var f=true;var a:Alignment?=null
	for(it in s.split(" ")){
		val t=it.trim();if(t.isEmpty())continue
		when(t[0]){
			'f'->when{
				t=="fs"->m=m.fillMaxSize()
				t.startsWith("fw")->{val v=t.drop(2);m=if(v.isEmpty()) m.fillMaxWidth() else m.width(v.toDoubleOrNull()?.dp?:0.dp)}
				t.startsWith("fh")->{val v=t.drop(2);m=if(v.isEmpty()) m.fillMaxHeight() else m.height(v.toDoubleOrNull()?.dp?:0.dp)}
			}
			'w'->{val v=t.drop(1)
				m=when{
					v.toDoubleOrNull()!=null->m.width(v.toDouble().dp)
					v.startsWith(">")->m.widthIn(min=v.drop(1).toDoubleOrNull()?.dp?:0.dp)
					v.startsWith("<")->m.widthIn(max=v.drop(1).toDoubleOrNull()?.dp?:Dp.Unspecified)
					v.isEmpty()->m.fillMaxWidth()
					else->m
				}
			}
			'h'->{val v=t.drop(1)
				m=when{
					v.isEmpty()->m.fillMaxHeight()
					v.startsWith(">")->m.heightIn(min=v.drop(1).toDoubleOrNull()?.dp?:0.dp)
					v.startsWith("<")->m.heightIn(max=v.drop(1).toDoubleOrNull()?.dp?:Dp.Unspecified)
					else->m.height(v.toDoubleOrNull()?.dp?:0.dp)
				}
			}
			'p'->{val v=t.drop(1)
				when{
					v=="nb"->m=m.navigationBarsPadding()
					v=="sb"->m=m.statusBarsPadding()
					v=="bs"->m=m.systemBarsPadding()
					v=="im"->m=m.imePadding()
					else->{
						val n=v.dropWhile{!it.isDigit()&&it!='.'}.toDoubleOrNull()?.dp?:0.dp
						m=when{
							v.startsWith("t")->m.padding(top=n)
							v.startsWith("b")->m.padding(bottom=n)
							v.startsWith("s")->m.padding(start=n)
							v.startsWith("e")->m.padding(end=n)
							v.startsWith("v")->m.padding(vertical=n)
							v.startsWith("h")->m.padding(horizontal=n)
							v.toDoubleOrNull()!=null->m.padding(n)
							else->m
						}
					}
				}
			}
			'z'->{val v=t.drop(1)
				when{
					v.startsWith("x")->{val n=v.drop(1).toDoubleOrNull()?.dp?:0.dp;m=m.offset(x=n)}
					v.startsWith("y")->{val n=v.drop(1).toDoubleOrNull()?.dp?:0.dp;m=m.offset(y=n)}
				}
			}
			'g'->{val v=t.drop(1)
				val hex=v.takeWhile{it.isLetterOrDigit()}
				val rest=v.drop(hex.length)
				val alpha=if(rest.startsWith(".")) rest.drop(1).toDoubleOrNull()?.toFloat()?:1f else 1f
				val base=Color(AC.parseColor("#$hex"))
				val fc=if(alpha!=1f) base.copy(alpha=alpha) else base
				m=when{
					rest.endsWith("c")->m.clip(CircleShape).background(fc)
					rest.contains("r")->{
						val r=rest.dropWhile{!it.isDigit()}.toDoubleOrNull()?.dp?:0.dp
						m.clip(RoundedCornerShape(r)).background(fc)
					}
					else->m.background(fc)
				}
			}
			'b'->{val v=t.drop(1)
				when{
					v.startsWith("r")->{
						val r=v.drop(1).split(",").mapNotNull{it.toDoubleOrNull()?.dp}
						val shape=when(r.size){
							1->RoundedCornerShape(r[0])
							4->RoundedCornerShape(r[0],r[1],r[2],r[3])
							else->RoundedCornerShape(0.dp)
						}
						m=m.clip(shape)
					}
					else->{
						val parts=v.split(",")
						val width=parts.getOrNull(0)?.toDoubleOrNull()?.dp?:1.dp
						val hex=parts.getOrNull(1)?:""
						val alpha=parts.getOrNull(2)?.toFloatOrNull()?:1f
						val fc=if(hex.isNotEmpty()){
							try{Color(AC.parseColor("#$hex"))}catch(_:Exception){Color.Black}
						}else Color.Black
						val c2=if(alpha!=1f)fc.copy(alpha=alpha)else fc
						m=m.border(width,c2)
					}
				}
			}
			'c'->{val v=t.drop(1)
				m=when{
					v.isEmpty()->m.clip(CircleShape)
					v.contains(",")->{
						val r=v.split(",").mapNotNull{it.toDoubleOrNull()?.dp}
						val shape=when(r.size){
							1->RoundedCornerShape(r[0])
							2->RoundedCornerShape(r[0],r[1])
							3->RoundedCornerShape(r[0],r[1],r[2])
							4->RoundedCornerShape(r[0],r[1],r[2],r[3])
							else->RoundedCornerShape(0.dp)
						}
						m.clip(shape)
					}
					v.toDoubleOrNull()!=null->m.clip(RoundedCornerShape(v.toDouble().dp))
					else->m.clip(CircleShape)
				}
			}
			'a'->{a=when(t.drop(1)){
				"c"->Alignment.Center;"cs"->Alignment.CenterStart;"ce"->Alignment.CenterEnd
				"ts"->Alignment.TopStart;"tc"->Alignment.TopCenter;"te"->Alignment.TopEnd
				"bs"->Alignment.BottomStart;"bc"->Alignment.BottomCenter;"be"->Alignment.BottomEnd
				else->null
			}}
			'e'->{val v=t.drop(1);w=v.filter{it!='f'}.toFloatOrNull()?:1f;f='f'!in v}
			's'->{when(t){"sh"->m=m.horizontalScroll(rememberScrollState());"sv"->m=m.verticalScroll(rememberScrollState())}}
			'r'->{val v=t.drop(1)
				if(v.contains("x")){
					val parts=v.split("x")
					val ww=parts.getOrNull(0)?.toFloatOrNull()?:1f
					val hh=parts.getOrNull(1)?.toFloatOrNull()?:1f
					if(hh!=0f)m=m.aspectRatio(ww/hh)
				}
			}
		}
	}
	return CW(m,w,a,f)
}
@Composable fun RowScope.css(s:String)=pcss(s).let{var m=it.m;it.w?.let{w->m=m.weight(w,it.f)};it.a?.let{a->m=m.align(a)};m}
@Composable fun BoxScope.css(s:String)=pcss(s).let{var m=it.m;it.a?.let{a->m=m.align(a)};m}
@Composable fun css(s:String)=pcss(s).m