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
import coil3.compose.AsyncImage
import java.util.UUID
import kotlin.math.roundToInt

object FN{ // 全局控制单例
	data class tc(val p:Color,val b:Color,val s:Color,val sv:Color,val o:Color,val os:Color,val ov:Color) // 主题色彩配置
	fun cl(dk:Boolean=true)=if(dk) tc(p=Color(0xFF8AB4F8),b=Color(0xFF1C1C1E),s=Color(0xFF2C2C2E),sv=Color(0xFF3A3A3C),o=Color(0xFF938F99),os=Color(0xFFE6E1E5),ov=Color(0xFF49454F)) else tc(p=Color(0xFF1A73E8),b=Color(0xFFF5F5F5),s=Color(0xFFFFFFFF),sv=Color(0xFFE8EAF6),o=Color(0xFFCAC4D0),os=Color(0xFF1C1B1F),ov=Color(0xFFE7E0EC)) // 明暗色彩分发
	val LC=staticCompositionLocalOf{cl()} // 全局主题本地提供变量
	val TL get()=TextStyle(fontSize=22.sp,fontWeight=FontWeight.W600,lineHeight=28.sp) // 大号字形
	val TM get()=TextStyle(fontSize=16.sp,fontWeight=FontWeight.W500,lineHeight=24.sp) // 中号导航字形
	val TS get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W500,lineHeight=20.sp) // 小号强调字形
	val BM get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W400,lineHeight=20.sp) // 标准字形
	val BS get()=TextStyle(fontSize=12.sp,fontWeight=FontWeight.W400,lineHeight=16.sp) // 极小脚注字形
	val lg=mutableStateListOf<String>() // 运行日志队列
	var lf by mutableStateOf(false) // 日志是否折叠
	var lh by mutableStateOf(false) // 日志是否隐藏
	var ly by mutableStateOf(0f) // 拖拽位移量
	fun lg(m:String,o:String,c:Char='i'){
		val t=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date())
		val x=when(c){'i'->"#2196F3";'u'->"#9C27B0";'e'->"#F44336";'s'->"#00BCD4";'n'->"#4CAF50";'w'->"#FF9800";else->"#9E9E9E"}
		val v="${UUID.randomUUID().toString().replace("-","")}.$x●$t $m ➜ $o"
		android.os.Handler(android.os.Looper.getMainLooper()).post{lg.add(v)}
	}
	fun lc()=lg.clear() // 清除全部日志
	fun lr(i:String)=lg.removeAll{it.startsWith(i)} // 删除单条日志
	@Composable fun LP(modifier:Modifier){
		Box(modifier=modifier,contentAlignment=Alignment.BottomCenter){if(lf)LH()else LS()}
	}
	@Composable private fun LH(){Box(modifier="fw h5 c2.5".css().background(Color(0x80808080)).clickable{lf=false;ly=0f},contentAlignment=Alignment.Center){BasicText("· · ·  日志  · · ·",style=BS.copy(color=Color.White.copy(alpha=0.7f)))}}
	@Composable private fun LS(){
		val ls=rememberLazyListState()
		LaunchedEffect(lg.lastOrNull()){if(lg.isNotEmpty())ls.animateScrollToItem(lg.size-1)}
		val tv=(LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
		Box(modifier="fw br6,6,0,0 b0.5,808080,0.70 g1C1C1E.0.92".css().offset(y=ly.roundToInt().dp).pointerInput(Unit){detectDragGestures(onDragEnd={if(ly>100f){lf=true;ly=0f} else ly=0f},onDrag={ch,d->ch.consume();if(ly+d.y>=0f)ly+=d.y})}){
			Column(modifier="fw pv2 ph5 pnb".css()){
				Box(modifier="fw h16".css(),contentAlignment=Alignment.Center){Box(modifier="w40 h3 c2".css().background(Color(0x66808080)).clickable(enabled=tv){lf=true;ly=0f}.pointerInput(!tv){if(!tv)detectTapGestures(onTap={lf=true;ly=0f})})}
				Row(modifier="fw pv2".css(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
					BasicText("日志 · ${lg.size}条",style=BS.copy(color=Color(0xFF9E9E9E),fontFamily=FontFamily.Monospace))
					Box(modifier="p2 c4".css().clickable{lc()}){BasicText("清空",style=BS.copy(color=Color(0xFFF44336)))}
				}
				LazyColumn(state=ls,modifier="fw pt4".css()){
					items(lg){ey->
						val pt=ey.split("●",limit=2)
						val dt=pt[0].lastIndexOf('.')
						val id=if(dt>0)pt[0].substring(0,dt) else pt[0]
						val hx=if(dt>0)pt[0].substring(dt+1) else "#9E9E9E"
						val ec=try{Color(AC.parseColor(hx))}catch(_:Exception){Color(0xFF9E9E9E)}
						Row(modifier="fw".css(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){
							BasicText(pt.getOrElse(1){""},modifier="e1 pe4".css(this),style=BS.copy(color=ec,lineHeight=1.2.em,fontFamily=FontFamily.Monospace))
							Box(modifier="w20 h20 c".css().clickable{lr(id)},contentAlignment=Alignment.Center){BasicText("✕",style=BS.copy(color=Color(0xFF9E9E9E)))}
						}
						Box(modifier="fw h0.5".css().background(Color(0x1A808080)))
					}
				}
			}
		}
	}
	var ht by mutableStateOf<String?>(null) // 主导航激活标识
	val hi=mutableStateListOf<VT>() // 历史足迹内存队列
	data class VT(val id:String,val tt:String,val pt:String,val pg:String="") // 播放记录足迹结构
}

@Composable
fun TB(tt:String="",ob:(()->Unit)?=null,ed:@Composable RowScope.()->Unit={}){
	val cc=FN.LC.current
	Row(modifier="fw h38 psb".css().background(cc.s).border(0.5.dp,cc.ov),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
		Row(modifier="ps2".css(),verticalAlignment=Alignment.CenterVertically){
			if(ob!=null){
				Box(modifier="w28 h28 c8".css().clickable(onClick=ob),contentAlignment=Alignment.Center){androidx.compose.foundation.Image(painter=androidx.compose.ui.res.painterResource(id=R.drawable.arrow_back),contentDescription=null,modifier="w20 h20".css(),colorFilter=androidx.compose.ui.graphics.ColorFilter.tint(cc.os))}
				if(tt.isNotEmpty())Spacer(modifier="w6".css())
			}
			if(tt.isNotEmpty())BasicText(tt,style=FN.TM.copy(color=cc.os),maxLines=1,overflow=TextOverflow.Ellipsis,modifier="e1".css(this))
		}
		Row(verticalAlignment=Alignment.CenterVertically){ed()}
	}
}

@Composable fun IB(lb:String,modifier:Modifier,oc:()->Unit){
	val cc=FN.LC.current
	Box(modifier=modifier.clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(lb,style=TextStyle(fontSize=18.sp,color=cc.os))}
}

@Composable fun CL(vc:Boolean=false,tt:String="加载中…"){
	val cc=FN.LC.current
	Box(modifier=if(vc)"fs".css()else"fw".css(),contentAlignment=Alignment.Center){
		Row(modifier=if(vc)"fw".css()else"fw h30".css(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
			BasicText("◌",style=TextStyle(fontSize=20.sp,color=cc.p))
			Spacer(modifier="w8".css())
			BasicText(tt,style=FN.BS.copy(color=cc.os.copy(alpha=0.6f)))
		}
	}
}

@Composable fun VC(pt:String,tt:String,sb:String,modifier:Modifier,oc:()->Unit,lp:(()->Unit)?=null){
	val cc=FN.LC.current
	Box(modifier=modifier.clip(RoundedCornerShape(2.dp)).background(cc.sv).then(if(lp!=null)Modifier.pointerInput(Unit){detectTapGestures(onTap={oc()},onLongPress={lp()})}else Modifier.clickable(onClick=oc))){
		Column{
			Box(modifier="fw r3x4".css().background(cc.s)){AsyncImage(modifier="fs".css(),model=pt,contentDescription=tt,contentScale=ContentScale.Crop)}
			Column(modifier="fw ph4 pv2".css()){
				BasicText(tt,style=FN.BS.copy(color=cc.os),maxLines=2,overflow=TextOverflow.Ellipsis)
				if(sb.isNotEmpty()){
					Spacer(modifier="h1".css())
					BasicText(sb,style=FN.BS.copy(color=cc.p),maxLines=1)
				}
			}
		}
	}
}

@Composable fun CD(tt:String,ct:String="确认",at:String="取消",oc:()->Unit,od:()->Unit){
	val cc=FN.LC.current
	Box(modifier="fs".css().background(Color(0x80000000)),contentAlignment=Alignment.Center){
		Column(modifier="w300 p20 c4".css().background(cc.s).border(0.1.dp,cc.ov,RoundedCornerShape(4.dp)),horizontalAlignment=Alignment.CenterHorizontally){
			Spacer(modifier="h6".css())
			BasicText(tt,modifier="ph6 pb14".css(this),style=FN.BM.copy(color=cc.os,textAlign=TextAlign.Center))
			Spacer(modifier="h10".css())
			Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
				Box(modifier="fw0.5 h16 ph20 pv10 c4".css().background(cc.sv).clickable(onClick=od),contentAlignment=Alignment.Center){BasicText(at,style=FN.BM.copy(color=cc.os))}
				Box(modifier="fw h16 ph20 pv10 c4".css().background(cc.p.copy(alpha=0.15f)).clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(ct,style=FN.BM.copy(color=cc.p))}
			}
		}
	}
}

@Composable fun TR(tb:List<Pair<String,String>>,tc:String,on:(String)->Unit){
	val cc=FN.LC.current
	Row(modifier="fw h24".css().background(cc.s),verticalAlignment=Alignment.CenterVertically){
		Row(modifier=Modifier.fillMaxWidth().fillMaxHeight().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
			tb.forEach{(id,o)->
				val ac=id==tc
				Box(modifier="fh".css().clickable{on(id)}.background(if(ac)cc.p.copy(alpha=0.15f)else Color.Transparent),contentAlignment=Alignment.Center){BasicText("  ${o.trim()}  ",style=FN.BS.copy(color=if(ac)cc.p else cc.os.copy(alpha=0.7f),fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))}
			}
		}
	}
}

@Composable fun EB(lb:String,ac:Boolean,modifier:Modifier,oc:()->Unit){
	val cc=FN.LC.current
	Box(modifier=modifier.clip(RoundedCornerShape(6.dp)).background(if(ac)cc.p else cc.sv).clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(lb,style=FN.BS.copy(color=if(ac)cc.b else cc.os,fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))}
}

// 线程安全的DSL缓存池
private val CC=java.util.concurrent.ConcurrentHashMap<String,List<String>>()
// CW DSL修饰符编译打包实体
class CW(val m:Modifier,val w:Float?=null,val a:Alignment?=null,val f:Boolean=true)
// CSS流式多属性解析翻译引擎
@Composable fun pcss(s:String):CW{
	var m:Modifier=Modifier;var w:Float?=null;var f=true;var a:Alignment?=null
	val ss=CC.getOrPut(s){s.split(" ")}
	for(it in ss){
		val t=it.trim();if(t.isEmpty())continue
		when(t[0]){
			'f'->when{
				t=="fs"->m=m.fillMaxSize()
				t.startsWith("fw")->{val v=t.drop(2);m=if(v.isEmpty())m.fillMaxWidth()else m.fillMaxWidth(v.toFloatOrNull()?:1f)}
				t.startsWith("fh")->{val v=t.drop(2);m=if(v.isEmpty())m.fillMaxHeight()else m.fillMaxHeight(v.toFloatOrNull()?:1f)}
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
			'm'->{val v=t.drop(1)
				val n=v.dropWhile{!it.isDigit()&&it!='.'}.toDoubleOrNull()?.dp?:0.dp
				m=when{
					v.startsWith("h")->m.padding(horizontal=n) // 水平外边距（模拟margin）
					v.startsWith("v")->m.padding(vertical=n) // 垂直外边距
					v.startsWith("t")->m.padding(top=n)
					v.startsWith("b")->m.padding(bottom=n)
					v.startsWith("s")->m.padding(start=n)
					v.startsWith("e")->m.padding(end=n)
					v.toDoubleOrNull()!=null->m.padding(n)
					else->m
				}
			}
			'g'->{val v=t.drop(1)
				val hx=v.takeWhile{it.isLetterOrDigit()}
				val rt=v.drop(hx.length)
				val ap=if(rt.startsWith("."))rt.drop(1).toDoubleOrNull()?.toFloat()?:1f else 1f
				val bs=try{Color(AC.parseColor("#$hx"))}catch(_:Exception){Color.Transparent}
				val fc=if(ap!=1f)bs.copy(alpha=ap)else bs
				m=when{
					rt.endsWith("c")->m.clip(CircleShape).background(fc)
					rt.contains("r")->{val r=rt.dropWhile{!it.isDigit()}.toDoubleOrNull()?.dp?:0.dp;m.clip(RoundedCornerShape(r)).background(fc)}
					else->m.background(fc)
				}
			}
			'b'->{val v=t.drop(1)
				when{
					v.startsWith("r")->{
						val r=v.drop(1).split(",").mapNotNull{it.toDoubleOrNull()?.dp}
						val sh=when(r.size){1->RoundedCornerShape(r[0]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)}
						m=m.clip(sh)
					}
					else->{
						val pt=v.split(",")
						val wd=pt.getOrNull(0)?.toDoubleOrNull()?.dp?:1.dp
						val hx=pt.getOrNull(1)?:""
						val ap=pt.getOrNull(2)?.toFloatOrNull()?:1f
						val fc=if(hx.isNotEmpty()){try{Color(AC.parseColor("#$hx"))}catch(_:Exception){Color.Black}}else Color.Black
						val c2=if(ap!=1f)fc.copy(alpha=ap)else fc
						m=m.border(wd,c2)
					}
				}
			}
			'c'->{val v=t.drop(1)
				m=when{
					v.isEmpty()->m.clip(CircleShape)
					v.contains(",")->{
						val r=v.split(",").mapNotNull{it.toDoubleOrNull()?.dp}
						val sh=when(r.size){1->RoundedCornerShape(r[0]);2->RoundedCornerShape(r[0],r[1]);3->RoundedCornerShape(r[0],r[1],r[2]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)}
						m.clip(sh)
					}
					v.toDoubleOrNull()!=null->m.clip(RoundedCornerShape(v.toDouble().dp))
					else->m.clip(CircleShape)
				}
			}
			'a'->{a=when(t.drop(1)){"c"->Alignment.Center;"cs"->Alignment.CenterStart;"ce"->Alignment.CenterEnd;"ts"->Alignment.TopStart;"tc"->Alignment.TopCenter;"te"->Alignment.TopEnd;"bs"->Alignment.BottomStart;"bc"->Alignment.BottomCenter;"be"->Alignment.BottomEnd;else->null}}
			'e'->{val v=t.drop(1);w=v.filter{it!='f'}.toFloatOrNull()?:1f;f=!v.contains('f')}
			's'->{when(t){"sh"->m=m.horizontalScroll(rememberScrollState());"sv"->m=m.verticalScroll(rememberScrollState())}}
			'r'->{val v=t.drop(1)
				if(v.contains("x")){
					val pt=v.split("x")
					val ww=pt.getOrNull(0)?.toFloatOrNull()?:1f
					val hh=pt.getOrNull(1)?.toFloatOrNull()?:1f
					if(hh!=0f)m=m.aspectRatio(ww/hh)
				}
			}
		}
	}
	return CW(m,w,a,f)
}
@Composable fun String.css()=pcss(this).m
@Composable fun String.css(s:ColumnScope)=pcss(this).let{with(s){var m=it.m;it.w?.let{w->m=m.weight(w,it.f)};m}}
@Composable fun String.css(s:RowScope)=pcss(this).let{with(s){var m=it.m;it.w?.let{w->m=m.weight(w,it.f)};(it.a as? Alignment.Vertical)?.let{v->m=m.align(v)};m}}
@Composable fun String.css(s:BoxScope)=pcss(this).let{with(s){var m=it.m;it.a?.let{a->m=m.align(a)};m}}