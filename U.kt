package com.fyan

import android.graphics.Color as AC
import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
import kotlin.math.roundToInt

// ── 主题系统 ──────────────────────────────────────────────────────────────────
object FN{ // UI主题/字形单例，纯UI相关，无状态/数据
	data class TC(val p:Color,val b:Color,val s:Color,val sv:Color,val o:Color,val os:Color,val ov:Color) // 主题色组（p=主色/b=背景/s=表面/sv=次级表面/o=轮廓/os=前景/ov=轮廓变体）
	fun cl(dk:Boolean=true)=if(dk)TC(p=Color(0xFF8AB4F8),b=Color(0xFF1C1C1E),s=Color(0xFF2C2C2E),sv=Color(0xFF3A3A3C),o=Color(0xFF938F99),os=Color(0xFFE6E1E5),ov=Color(0xFF49454F))
	else TC(p=Color(0xFF1A73E8),b=Color(0xFFF5F5F5),s=Color(0xFFFFFFFF),sv=Color(0xFFE8EAF6),o=Color(0xFFCAC4D0),os=Color(0xFF1C1B1F),ov=Color(0xFFE7E0EC)) // 暗/亮模式色组
	val LC=staticCompositionLocalOf{cl()} // CompositionLocal主题提供者

	// 字形预设（get每次返回新实例，避免跨主题复用脏值）
	val TL get()=TextStyle(fontSize=22.sp,fontWeight=FontWeight.W600,lineHeight=28.sp) // 大标题
	val TM get()=TextStyle(fontSize=16.sp,fontWeight=FontWeight.W500,lineHeight=24.sp) // 中标题/导航
	val TS get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W500,lineHeight=20.sp) // 小标题/分组
	val BM get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W400,lineHeight=20.sp) // 正文
	val BS get()=TextStyle(fontSize=12.sp,fontWeight=FontWeight.W400,lineHeight=16.sp) // 注脚/日志
}

// ── 日志面板（读取E.kt中的LG/LF/LY状态）────────────────────────────────────────
@Composable fun LP(){if(LF)LPH()else LPS()} // 日志面板入口：折叠/展开分发

@Composable private fun LPH(){ // 折叠态横条，点击恢复展开
	Box(modifier="fw0.7 h6 pb2 c2.5 pnb".css()
		.background(Color(0x80808080))
		.clickable{LF=false;LY=0f},
		contentAlignment=Alignment.Center){}
}

@Composable private fun LPS(){ // 展开态日志面板，屏高1/3，可下拖折叠
	val ls=rememberLazyListState()
	LaunchedEffect(LG.lastOrNull()){if(LG.isNotEmpty())ls.animateScrollToItem(LG.size-1)} // 新日志自动滚底
	val cfg=LocalConfiguration.current
	val tv=(cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
	val h=cfg.screenHeightDp/3
	Box(modifier="fw h<$h pnb".css().offset(y=LY.roundToInt().dp)
		.pointerInput(Unit){detectDragGestures(
			onDragEnd={if(LY>100f){LF=true;LY=0f}else LY=0f}, // 下拖>100dp折叠
			onDrag={ch,d->ch.consume();if(LY+d.y>=0f)LY+=d.y} // 仅允许向下拖
		)}
	){
		Box(modifier="fw br6,6,0,0 b1,808080,0.50 g1C1C1E.0.88".css()){
			Column(modifier="fw pv2 ph5".css()){
				Box(modifier="fw".css(),contentAlignment=Alignment.Center){ // 拖拽把手
					Box(modifier="fw0.25 h3 c2".css().background(Color(0x66808080))
						.clickable(enabled=tv){LF=true;LY=0f}
						.pointerInput(!tv){if(!tv)detectTapGestures(onTap={LF=true;LY=0f})})
				}
				Row(modifier="fw pb2".css(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){ // 标题栏+清空
					BasicText("日志 · ${LG.size}条",style=FN.BS.copy(color=Color(0xFF9E9E9E),fontFamily=FontFamily.Monospace))
					Box(modifier="p2 c4".css().clickable{lc()}){BasicText("清空",style=FN.BS.copy(color=Color(0xFFF44336)))}
				}
				LazyColumn(state=ls,modifier="fw pt4".css()){ // 日志条目列表
					items(LG){ey->
						val pt=ey.split("●",limit=2)
						val dt=pt[0].lastIndexOf('.')
						val id=if(dt>0)pt[0].substring(0,dt) else pt[0] // uuid，用于删除
						val hx=if(dt>0)pt[0].substring(dt+1) else "#9E9E9E"
						val ec=try{Color(AC.parseColor(hx))}catch(_:Exception){Color(0xFF9E9E9E)}
						Box(modifier="fw h0.5".css().background(Color(0x1A808080))) // 分隔线
						Row(modifier="fw".css(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){
							BasicText(pt.getOrElse(1){""},modifier="e1 pe4".css(this),style=FN.BS.copy(color=ec,lineHeight=1.1.em,fontFamily=FontFamily.Monospace))
							Box(modifier="w18 h18 c".css().clickable{lr(id)},contentAlignment=Alignment.Center){BasicText("✕",style=FN.BS.copy(color=Color(0xFF9E9E9E)))} // 单条删除
						}
					}
				}
			}
		}
	}
}

// ── 顶部工具栏 ────────────────────────────────────────────────────────────────
@Composable fun TB(tt:String="",ob:(()->Unit)?=null,ed:@Composable RowScope.()->Unit={}){ // tt=标题/ob=返回回调/ed=右侧插槽
	val cc=FN.LC.current
	Row(modifier="fw h38 psb".css().background(cc.s),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
		Row(modifier="ps2".css(),verticalAlignment=Alignment.CenterVertically){
			if(ob!=null){
				Box(modifier="w28 h28 c8".css().clickable(onClick=ob),contentAlignment=Alignment.Center){
					androidx.compose.foundation.Image(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier="w20 h20".css(),colorFilter=ColorFilter.tint(cc.os))
				}
				if(tt.isNotEmpty())Spacer(modifier="w6".css())
			}
			if(tt.isNotEmpty())BasicText(tt,style=FN.TM.copy(color=cc.os),maxLines=1,overflow=TextOverflow.Ellipsis,modifier="e1".css(this))
		}
		Row(verticalAlignment=Alignment.CenterVertically){ed()}
	}
}

// ── 基础组件 ──────────────────────────────────────────────────────────────────
@Composable fun IB(lb:String,modifier:Modifier,oc:()->Unit){ // 图标按钮（lb=图标文本/modifier/oc=点击）
	val cc=FN.LC.current
	Box(modifier=modifier.clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(lb,style=TextStyle(fontSize=18.sp,color=cc.os))}
}

@Composable fun CL(vc:Boolean=false,tt:String="加载中…"){ // 加载占位（vc=全屏居中/tt=提示文字）
	val cc=FN.LC.current
	Box(modifier=if(vc)"fs".css()else"fw".css(),contentAlignment=Alignment.Center){
		Row(modifier=Modifier.wrapContentSize(),verticalAlignment=Alignment.CenterVertically){
			BasicText("◌",style=TextStyle(fontSize=if(vc)30.sp else 18.sp,color=cc.p))
			Spacer(modifier="w8".css())
			BasicText(tt,style=(if(vc)FN.BM else FN.BS).copy(color=cc.os.copy(alpha=0.6f)))
		}
	}
}

@Composable fun VC(pt:String,tt:String,sb:String,modifier:Modifier,oc:()->Unit,lp:(()->Unit)?=null){ // 视频卡片（pt=封面/tt=标题/sb=副标题/lp=长按回调）
	val cc=FN.LC.current
	Box(modifier=modifier.clip(RoundedCornerShape(2.dp)).background(cc.sv)
		.then(if(lp!=null)Modifier.pointerInput(Unit){detectTapGestures(onTap={oc()},onLongPress={lp()})}
		else Modifier.clickable(onClick=oc))){
		Column{
			Box(modifier="fw r3x4".css().background(cc.s)){AsyncImage(modifier="fs".css(),model=pt,contentDescription=tt,contentScale=ContentScale.Crop)} // 3:4封面
			Column(modifier="fw ph4 pv2".css()){
				BasicText(tt,style=FN.BS.copy(color=cc.os),maxLines=2,overflow=TextOverflow.Ellipsis)
				if(sb.isNotEmpty()){Spacer(modifier="h1".css());BasicText(sb,style=FN.BS.copy(color=cc.p),maxLines=1)}
			}
		}
	}
}

@Composable fun CD(tt:String,ct:String="确认",at:String="取消",oc:()->Unit,od:()->Unit){ // 确认对话框（tt=内容/ct=确认/at=取消/oc=确认/od=取消）
	val cc=FN.LC.current
	Box(modifier="fs".css().background(Color(0x80000000)),contentAlignment=Alignment.Center){
		Column(modifier="w300 p20 c4".css().background(cc.s).border(0.1.dp,cc.ov,RoundedCornerShape(4.dp)),horizontalAlignment=Alignment.CenterHorizontally){
			Spacer(modifier="fw h18".css())
			BasicText(tt,style=FN.TM.copy(color=cc.os,textAlign=TextAlign.Center))
			Row(modifier="fw pt20 pb10".css(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
				Box(modifier="fw0.47 h30 ph10 c2".css().background(cc.sv).clickable(onClick=od),contentAlignment=Alignment.Center){BasicText(at,style=FN.BM.copy(color=cc.os))}
				Box(modifier="fw h30 ph10 c2".css().background(cc.p.copy(alpha=0.15f)).clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(ct,style=FN.BM.copy(color=cc.os))}
			}
		}
	}
}

@Composable fun TR(tb:List<Pair<String,String>>,tc:String,on:(String)->Unit){ // 横向滚动标签行（tb=id+label/tc=选中id/on=点击）
	val cc=FN.LC.current
	Row(modifier="fw h24".css().background(cc.s),verticalAlignment=Alignment.CenterVertically){
		Row(modifier=Modifier.fillMaxWidth().fillMaxHeight().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
			tb.forEach{(id,o)->
				val ac=id==tc
				Box(modifier="fh".css().clickable{on(id)}.background(if(ac)cc.p.copy(alpha=0.15f)else Color.Transparent),contentAlignment=Alignment.Center){
					BasicText("  ${o.trim()}  ",style=FN.BS.copy(color=if(ac)cc.p else cc.os.copy(alpha=0.7f),fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))
				}
			}
		}
	}
}

@Composable fun EB(lb:String,ac:Boolean,modifier:Modifier,oc:()->Unit){ // 集数/选项按钮（lb=标签/ac=选中/oc=点击）
	val cc=FN.LC.current
	Box(modifier=modifier.clip(RoundedCornerShape(2.dp)).background(if(ac)cc.p else cc.sv).clickable(onClick=oc),contentAlignment=Alignment.Center){
		BasicText(lb,style=FN.BS.copy(color=if(ac)cc.b else cc.os,fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))
	}
}

@Composable fun EG(tl:List<String>,ct:Int,cs:Int,os:(Int)->Unit){ // 剧集矩阵网格（tl=标题列表/ct=当前集/cs=列数/os=选集回调），静态布局避免嵌套滚动
	val rs=(tl.size+cs-1)/cs // 总行数向上取整
	Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
		repeat(rs){r->
			Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
				repeat(cs){c->
					val i=r*cs+c
					if(i<tl.size)EB(lb=tl[i],ac=i==ct,oc={os(i)},modifier="fw e1 h24".css(this))
					else Spacer(modifier="e1".css(this)) // 末行空位补齐防拉伸
				}
			}
		}
	}
}

// ── DSL解析引擎 ───────────────────────────────────────────────────────────────
private val CC=java.util.concurrent.ConcurrentHashMap<String,List<String>>() // Token缓存池，避免重复split
class CW(val m:Modifier,val w:Float?=null,val a:Alignment?=null,val f:Boolean=true) // DSL解析结果容器

// pcss：将紧凑DSL字符串（如"fw h48 p8 c4"）解析为Modifier链
// 支持：f(fill) w(width) h(height) p(padding/安全区) g(背景+裁剪) b(border/圆角裁剪) c(裁剪) a(对齐) e(weight) s(scroll) r(宽高比)
@Composable fun pcss(s:String):CW{
	var m:Modifier=Modifier;var w:Float?=null;var f=true;var a:Alignment?=null
	val ss=CC.getOrPut(s){s.split(' ')} // 缓存复用解析结果
	for(it in ss){
		val t=it.trim();if(t.isEmpty())continue
		when(t[0]){
			'f'->when{
				t=="fs"->m=m.fillMaxSize()
				t.startsWith("fw")->{val v=t.drop(2);m=if(v.isEmpty())m.fillMaxWidth()else m.fillMaxWidth(v.toFloatOrNull()?:1f)}
				t.startsWith("fh")->{val v=t.drop(2);m=if(v.isEmpty())m.fillMaxHeight()else m.fillMaxHeight(v.toFloatOrNull()?:1f)}
			}
			'w'->{val v=t.drop(1);m=when{v.toDoubleOrNull()!=null->m.width(v.toDouble().dp);v.startsWith(">")->m.widthIn(min=v.drop(1).toDoubleOrNull()?.dp?:0.dp);v.startsWith("<")->m.widthIn(max=v.drop(1).toDoubleOrNull()?.dp?:Dp.Unspecified);v.isEmpty()->m.fillMaxWidth();else->m}}
			'h'->{val v=t.drop(1);m=when{v.isEmpty()->m.fillMaxHeight();v.startsWith(">")->m.heightIn(min=v.drop(1).toDoubleOrNull()?.dp?:0.dp);v.startsWith("<")->m.heightIn(max=v.drop(1).toDoubleOrNull()?.dp?:Dp.Unspecified);else->m.height(v.toDoubleOrNull()?.dp?:0.dp)}}
			'p'->{val v=t.drop(1);when{
				v=="nb"->m=m.navigationBarsPadding() // 导航栏安全区
				v=="sb"->m=m.statusBarsPadding() // 状态栏安全区
				v=="bs"->m=m.systemBarsPadding() // 系统栏整体安全区
				v=="im"->m=m.imePadding() // 输入法安全区
				else->{val n=v.dropWhile{!it.isDigit()&&it!='.'}.toDoubleOrNull()?.dp?:0.dp
					m=when{v.startsWith("t")->m.padding(top=n);v.startsWith("b")->m.padding(bottom=n);v.startsWith("s")->m.padding(start=n);v.startsWith("e")->m.padding(end=n);v.startsWith("v")->m.padding(vertical=n);v.startsWith("h")->m.padding(horizontal=n);v.toDoubleOrNull()!=null->m.padding(n);else->m}
				}
			}}
			'g'->{val v=t.drop(1) // g{hex}[.alpha][r{r}|c] → 背景+可选圆角/圆形裁剪
				val hx=v.takeWhile{it.isLetterOrDigit()};val rt=v.drop(hx.length)
				val ap=if(rt.startsWith("."))rt.drop(1).toDoubleOrNull()?.toFloat()?:1f else 1f
				val bs=try{Color(AC.parseColor("#$hx"))}catch(_:Exception){Color.Transparent}
				val fc=if(ap!=1f)bs.copy(alpha=ap)else bs
				m=when{rt.endsWith("c")->m.clip(CircleShape).background(fc);rt.contains("r")->{val r=rt.dropWhile{!it.isDigit()}.toDoubleOrNull()?.dp?:0.dp;m.clip(RoundedCornerShape(r)).background(fc)};else->m.background(fc)}
			}
			'b'->{val v=t.drop(1);when{
				v.startsWith("r")->{val r=v.drop(1).split(",").mapNotNull{it.toDoubleOrNull()?.dp};val sh=when(r.size){1->RoundedCornerShape(r[0]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)};m=m.clip(sh)} // br{r1,r2,r3,r4} 圆角裁剪
				else->{val pt=v.split(",");val wd=pt.getOrNull(0)?.toDoubleOrNull()?.dp?:1.dp;val hx=pt.getOrNull(1)?:"";val ap=pt.getOrNull(2)?.toFloatOrNull()?:1f;val rr=pt.getOrNull(3)?.toDoubleOrNull()?.dp?:0.dp
					val fc=(if(hx.isNotEmpty()){try{Color(AC.parseColor("#$hx"))}catch(_:Exception){Color.Black}}else Color.Black).let{if(ap!=1f)it.copy(alpha=ap)else it}
					m=m.border(wd,fc,if(rr>0.dp)RoundedCornerShape(rr)else RoundedCornerShape(0))} // b{w},{hex},{alpha}[,{r}] 描边
			}}
			'c'->{val v=t.drop(1) // c=圆形；c{r}=单圆角；c{r1,r2,...}=多角
				m=when{v.isEmpty()->m.clip(CircleShape);v.contains(",")->{val r=v.split(",").mapNotNull{it.toDoubleOrNull()?.dp};val sh=when(r.size){1->RoundedCornerShape(r[0]);2->RoundedCornerShape(r[0],r[1]);3->RoundedCornerShape(r[0],r[1],r[2]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)};m.clip(sh)};v.toDoubleOrNull()!=null->m.clip(RoundedCornerShape(v.toDouble().dp));else->m}
			}
			'a'->{a=when(t.drop(1)){"c"->Alignment.Center;"cs"->Alignment.CenterStart;"ce"->Alignment.CenterEnd;"ts"->Alignment.TopStart;"tc"->Alignment.TopCenter;"te"->Alignment.TopEnd;"bs"->Alignment.BottomStart;"bc"->Alignment.BottomCenter;"be"->Alignment.BottomEnd;else->null}} // 对齐映射
			'e'->{val v=t.drop(1);w=v.filter{it!='f'}.toFloatOrNull()?:1f;f=!v.contains('f')} // e{n}[f] → weight
			's'->{when(t){"sh"->m=m.horizontalScroll(rememberScrollState());"sv"->m=m.verticalScroll(rememberScrollState())}} // 滚动方向
			'r'->{val v=t.drop(1);if(v.contains("x")){val pt=v.split("x");val ww=pt.getOrNull(0)?.toFloatOrNull()?:1f;val hh=pt.getOrNull(1)?.toFloatOrNull()?:1f;if(hh!=0f)m=m.aspectRatio(ww/hh)}} // r{w}x{h} 宽高比
		}
	}
	return CW(m,w,a,f)
}

// DSL扩展函数族
@Composable fun String.css()=pcss(this).m // 直接返回Modifier
@Composable fun String.css(s:ColumnScope)=pcss(this).let{with(s){var m=it.m;it.w?.let{w->m=m.weight(w,it.f)};m}} // ColumnScope weight
@Composable fun String.css(s:RowScope)=pcss(this).let{with(s){var m=it.m;it.w?.let{w->m=m.weight(w,it.f)};(it.a as? Alignment.Vertical)?.let{v->m=m.align(v)};m}} // RowScope weight+align
@Composable fun String.css(s:BoxScope)=pcss(this).let{with(s){var m=it.m;it.a?.let{a->m=m.align(a)};m}} // BoxScope align