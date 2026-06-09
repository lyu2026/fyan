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

object FN{ // 全局UI控制单例，持有主题/日志/历史等共享状态
	data class tc(val p:Color,val b:Color,val s:Color,val sv:Color,val o:Color,val os:Color,val ov:Color) // 主题色彩配置（p=主色/b=背景/s=表面/sv=次级表面/o=轮廓/os=前景/ov=轮廓变体）
	fun cl(dk:Boolean=true)=if(dk) tc(p=Color(0xFF8AB4F8),b=Color(0xFF1C1C1E),s=Color(0xFF2C2C2E),sv=Color(0xFF3A3A3C),o=Color(0xFF938F99),os=Color(0xFFE6E1E5),ov=Color(0xFF49454F)) else tc(p=Color(0xFF1A73E8),b=Color(0xFFF5F5F5),s=Color(0xFFFFFFFF),sv=Color(0xFFE8EAF6),o=Color(0xFFCAC4D0),os=Color(0xFF1C1B1F),ov=Color(0xFFE7E0EC)) // 按暗/亮模式返回对应色彩组
	val LC=staticCompositionLocalOf{cl()} // CompositionLocal主题提供者，全树共享当前主题

	val TL get()=TextStyle(fontSize=22.sp,fontWeight=FontWeight.W600,lineHeight=28.sp) // 大号字形（标题）
	val TM get()=TextStyle(fontSize=16.sp,fontWeight=FontWeight.W500,lineHeight=24.sp) // 中号字形（导航/小标题）
	val TS get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W500,lineHeight=20.sp) // 小号强调字形（分组标签）
	val BM get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W400,lineHeight=20.sp) // 标准正文字形
	val BS get()=TextStyle(fontSize=12.sp,fontWeight=FontWeight.W400,lineHeight=16.sp) // 极小字形（注脚/日志）

	val lg=mutableStateListOf<String>() // 运行日志队列，每条格式：uuid.colorHex●时间 模块 ➜ 内容
	var lf by mutableStateOf(false) // 日志面板是否折叠（true=折叠为横条）
	var lh by mutableStateOf(false) // 日志面板是否完全隐藏（预留，当前未使用）
	var ly by mutableStateOf(0f) // 日志面板拖拽下移的像素偏移量

	// 复用单例Handler，避免每次log调用都重新构造Handler对象
	private val MH=android.os.Handler(android.os.Looper.getMainLooper())

	fun lg(m:String,o:String,c:Char='i'){ // 写入一条日志（m=模块名/o=内容/c=级别字符 i信息 u用户 e错误 s系统 n成功 w警告）
		val t=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date())
		val x=when(c){'i'->"#2196F3";'u'->"#9C27B0";'e'->"#F44336";'s'->"#00BCD4";'n'->"#4CAF50";'w'->"#FF9800";else->"#9E9E9E"} // 级别对应颜色
		val v="${UUID.randomUUID().toString().replace("-","")}.$x●$t $m ➜ $o" // 拼装日志条目，uuid作删除key，颜色编码在点后
		MH.post{lg.add(v)} // 切主线程追加，保证State更新安全
	}
	fun lc()=lg.clear() // 清空全部日志
	fun lr(i:String)=lg.removeAll{it.startsWith(i)} // 按条目uuid前缀删除单条日志

	@Composable fun LP(){if(lf)LH()else LS()} // 日志面板入口：折叠态显示横条，展开态显示完整面板

	@Composable private fun LH(){ // 折叠态横条：宽70%居中，加导航栏padding防止被系统栏遮挡
		Box(modifier="fw0.7 h5 pb4 c2.5 pnb".css()
			.background(Color(0x80808080))
			.clickable{lf=false;ly=0f}, // 点击恢复展开
			contentAlignment=Alignment.Center){}
	}

	@Composable private fun LS(){ // 展开态日志面板：占屏高1/3，可向下拖拽折叠
		val ls=rememberLazyListState() // 日志列表滚动状态
		LaunchedEffect(lg.lastOrNull()){if(lg.isNotEmpty())ls.animateScrollToItem(lg.size-1)} // 新日志自动滚底
		val tv=(LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION // 是否TV端（TV不支持手势拖拽）
		val h=androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp/3 // 面板最大高度=屏高1/3
		Box(
			// pnb：消费导航栏高度，防止面板底边被系统导航栏遮挡
			modifier="fw h<$h pnb".css().offset(y=ly.roundToInt().dp)
				.pointerInput(Unit){detectDragGestures(
					onDragEnd={
						if(ly>100f){lf=true;ly=0f} // 下拖超100dp触发折叠
						else ly=0f // 未达阈值弹回
					},
					onDrag={ch,d->ch.consume();if(ly+d.y>=0f)ly+=d.y} // 仅允许向下拖（不允许上拖超出原位）
				)}
		){
			Box(modifier="fw br6,6,0,0 b1,808080,0.50 g1C1C1E.0.88".css()){ // 上圆角+半透明深色背景+描边
				Column(modifier="fw pv2 ph5".css()){
					// 顶部拖拽把手指示条
					Box(modifier="fw".css(),contentAlignment=Alignment.Center){Box(modifier="fw0.25 h3 c2".css().background(Color(0x66808080)).clickable(enabled=tv){lf=true;ly=0f}.pointerInput(!tv){if(!tv)detectTapGestures(onTap={lf=true;ly=0f})})}
					// 标题栏：日志条数 + 清空按钮
					Row(modifier="fw pb2".css(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
						BasicText("日志 · ${lg.size}条",style=BS.copy(color=Color(0xFF9E9E9E),fontFamily=FontFamily.Monospace))
						Box(modifier="p2 c4".css().clickable{lc()}){BasicText("清空",style=BS.copy(color=Color(0xFFF44336)))}
					}
					// 日志条目列表
					LazyColumn(state=ls,modifier="fw pt4".css()){
						items(lg){ey->
							// 解析条目：按●分割id+颜色段 与 正文段
							val pt=ey.split("●",limit=2)
							val dt=pt[0].lastIndexOf('.')
							val id=if(dt>0)pt[0].substring(0,dt) else pt[0] // uuid（用于删除）
							val hx=if(dt>0)pt[0].substring(dt+1) else "#9E9E9E" // 颜色hex
							val ec=try{Color(AC.parseColor(hx))}catch(_:Exception){Color(0xFF9E9E9E)} // 解析失败兜底灰色
							Box(modifier="fw h0.5".css().background(Color(0x1A808080))) // 分隔线
							Row(modifier="fw".css(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){
								BasicText(pt.getOrElse(1){""},modifier="e1 pe4".css(this),style=BS.copy(color=ec,lineHeight=1.1.em,fontFamily=FontFamily.Monospace))
								Box(modifier="w18 h18 c".css().clickable{lr(id)},contentAlignment=Alignment.Center){BasicText("✕",style=BS.copy(color=Color(0xFF9E9E9E)))} // 单条删除按钮
							}
						}
					}
				}
			}
		}
	}

	var ht by mutableStateOf<String?>(null) // 当前激活的主导航标识（预留）
	val hi=mutableStateListOf<VT>() // 播放历史队列（内存，同步持久化到SP）
	data class VT(val id:String,val tt:String,val pt:String,val pg:String="") // 历史记录条目（id=视频key/tt=标题/pt=封面url/pg=进度描述）
}

@Composable
fun TB(tt:String="",ob:(()->Unit)?=null,ed:@Composable RowScope.()->Unit={}){ // 顶部工具栏（tt=标题/ob=返回回调/ed=右侧插槽）
	val cc=FN.LC.current
	Row(modifier="fw h38 psb".css().background(cc.s),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
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

@Composable fun IB(lb:String,modifier:Modifier,oc:()->Unit){ // 图标按钮（lb=图标文本/modifier/oc=点击）
	val cc=FN.LC.current
	Box(modifier=modifier.clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(lb,style=TextStyle(fontSize=18.sp,color=cc.os))}
}

@Composable fun CL(vc:Boolean=false,tt:String="加载中…"){ // 通用加载占位（vc=是否全屏居中/tt=提示文字）
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
	Box(modifier=modifier.clip(RoundedCornerShape(2.dp)).background(cc.sv).then(if(lp!=null)Modifier.pointerInput(Unit){detectTapGestures(onTap={oc()},onLongPress={lp()})}else Modifier.clickable(onClick=oc))){
		Column{
			Box(modifier="fw r3x4".css().background(cc.s)){AsyncImage(modifier="fs".css(),model=pt,contentDescription=tt,contentScale=ContentScale.Crop)} // 3:4封面图
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

@Composable fun CD(tt:String,ct:String="确认",at:String="取消",oc:()->Unit,od:()->Unit){ // 确认对话框（tt=内容/ct=确认文字/at=取消文字/oc=确认/od=取消）
	val cc=FN.LC.current
	Box(modifier="fs".css().background(Color(0x80000000)),contentAlignment=Alignment.Center){ // 全屏半透明遮罩
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

@Composable fun TR(tb:List<Pair<String,String>>,tc:String,on:(String)->Unit){ // 横向滚动标签行（tb=id+label列表/tc=当前选中id/on=点击回调）
	val cc=FN.LC.current
	Row(modifier="fw h24".css().background(cc.s),verticalAlignment=Alignment.CenterVertically){
		Row(modifier=Modifier.fillMaxWidth().fillMaxHeight().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
			tb.forEach{(id,o)->
				val ac=id==tc // 是否当前选中项
				Box(modifier="fh".css().clickable{on(id)}.background(if(ac)cc.p.copy(alpha=0.15f)else Color.Transparent),contentAlignment=Alignment.Center){BasicText("  ${o.trim()}  ",style=FN.BS.copy(color=if(ac)cc.p else cc.os.copy(alpha=0.7f),fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))}
			}
		}
	}
}

@Composable fun EB(lb:String,ac:Boolean,modifier:Modifier,oc:()->Unit){ // 集数/选项按钮（lb=标签/ac=是否选中/oc=点击）
	val cc=FN.LC.current
	Box(modifier=modifier.clip(RoundedCornerShape(2.dp)).background(if(ac)cc.p else cc.sv).clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(lb,style=FN.BS.copy(color=if(ac)cc.b else cc.os,fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))}
}

// DSL字符串Token缓存池，避免重复split，线程安全
private val CC=java.util.concurrent.ConcurrentHashMap<String,List<String>>()
// pcss解析结果容器（m=最终Modifier/w=weight值/a=对齐/f=fill模式）
class CW(val m:Modifier,val w:Float?=null,val a:Alignment?=null,val f:Boolean=true)

// CSS风格DSL解析引擎：将紧凑字符串（如"fw h48 p8 c4"）翻译为Compose Modifier链
// 支持的前缀：f(fill) w(width) h(height) p(padding) g(background+clip) b(border/borderRadius) c(clip) a(alignment) e(weight) s(scroll) r(aspectRatio)
@Composable fun pcss(s:String):CW{
	var m:Modifier=Modifier;var w:Float?=null;var f=true;var a:Alignment?=null
	val ss=CC.getOrPut(s){s.split(' ')} // 单字符split比字符串split略快，结果缓存复用
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
					v=="nb"->m=m.navigationBarsPadding() // 导航栏安全区
					v=="sb"->m=m.statusBarsPadding() // 状态栏安全区
					v=="bs"->m=m.systemBarsPadding() // 系统栏整体安全区
					v=="im"->m=m.imePadding() // 输入法安全区
					else->{
						val n=v.dropWhile{!it.isDigit()&&it!='.'}.toDoubleOrNull()?.dp?:0.dp
						m=when{
							v.startsWith("t")->m.padding(top=n)
							v.startsWith("b")->m.padding(bottom=n)
							v.startsWith("s")->m.padding(start=n)
							v.startsWith("e")->m.padding(end=n)
							v.startsWith("v")->m.padding(vertical=n)
							v.startsWith("h")->m.padding(horizontal=n)
							v.toDoubleOrNull()!=null->m.padding(n) // 四边等距
							else->m
						}
					}
				}
			}
			'g'->{val v=t.drop(1) // g{hex}[.alpha][r{radius}|c] → 背景色+可选圆角/圆形裁剪
				val hx=v.takeWhile{it.isLetterOrDigit()}
				val rt=v.drop(hx.length)
				val ap=if(rt.startsWith("."))rt.drop(1).toDoubleOrNull()?.toFloat()?:1f else 1f // 透明度（0~1）
				val bs=try{Color(AC.parseColor("#$hx"))}catch(_:Exception){Color.Transparent}
				val fc=if(ap!=1f)bs.copy(alpha=ap)else bs
				m=when{
					rt.endsWith("c")->m.clip(CircleShape).background(fc) // 圆形裁剪
					rt.contains("r")->{val r=rt.dropWhile{!it.isDigit()}.toDoubleOrNull()?.dp?:0.dp;m.clip(RoundedCornerShape(r)).background(fc)} // 圆角矩形
					else->m.background(fc)
				}
			}
			'b'->{val v=t.drop(1)
				when{
					v.startsWith("r")->{ // br{r1,r2,r3,r4} → 单独圆角裁剪（不加border）
						val r=v.drop(1).split(",").mapNotNull{it.toDoubleOrNull()?.dp}
						val sh=when(r.size){1->RoundedCornerShape(r[0]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)}
						m=m.clip(sh)
					}
					else->{ // b{width},{hex},{alpha}[,{cornerRadius}] → 描边，支持可选圆角
						val pt=v.split(",")
						val wd=pt.getOrNull(0)?.toDoubleOrNull()?.dp?:1.dp
						val hx=pt.getOrNull(1)?:""
						val ap=pt.getOrNull(2)?.toFloatOrNull()?:1f
						val rr=pt.getOrNull(3)?.toDoubleOrNull()?.dp?:0.dp // 第4位圆角，与clip配合防溢出
						val fc=if(hx.isNotEmpty()){try{Color(AC.parseColor("#$hx"))}catch(_:Exception){Color.Black}}else Color.Black
						val c2=if(ap!=1f)fc.copy(alpha=ap)else fc
						val sh=if(rr>0.dp)RoundedCornerShape(rr)else RoundedCornerShape(0)
						m=m.border(wd,c2,sh)
					}
				}
			}
			'c'->{val v=t.drop(1) // c → 圆形；c{r} → 单圆角；c{r1,r2,...} → 多角独立圆角
				m=when{
					v.isEmpty()->m.clip(CircleShape)
					v.contains(",")->{
						val r=v.split(",").mapNotNull{it.toDoubleOrNull()?.dp}
						val sh=when(r.size){1->RoundedCornerShape(r[0]);2->RoundedCornerShape(r[0],r[1]);3->RoundedCornerShape(r[0],r[1],r[2]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)}
						m.clip(sh)
					}
					v.toDoubleOrNull()!=null->m.clip(RoundedCornerShape(v.toDouble().dp))
					else->m // 无法识别则跳过，不做任何裁剪
				}
			}
			'a'->{a=when(t.drop(1)){"c"->Alignment.Center;"cs"->Alignment.CenterStart;"ce"->Alignment.CenterEnd;"ts"->Alignment.TopStart;"tc"->Alignment.TopCenter;"te"->Alignment.TopEnd;"bs"->Alignment.BottomStart;"bc"->Alignment.BottomCenter;"be"->Alignment.BottomEnd;else->null}} // 对齐方式映射
			'e'->{val v=t.drop(1);w=v.filter{it!='f'}.toFloatOrNull()?:1f;f=!v.contains('f')} // e{n}[f] → Row/Column的weight权重（f=不填充）
			's'->{when(t){"sh"->m=m.horizontalScroll(rememberScrollState());"sv"->m=m.verticalScroll(rememberScrollState())}} // 滚动方向
			'r'->{val v=t.drop(1) // r{w}x{h} → 宽高比（如r16x9）
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
// 扩展函数：直接从String调用DSL解析，返回纯Modifier
@Composable fun String.css()=pcss(this).m
// ColumnScope扩展：解析weight并应用到列子项
@Composable fun String.css(s:ColumnScope)=pcss(this).let{with(s){var m=it.m;it.w?.let{w->m=m.weight(w,it.f)};m}}
// RowScope扩展：解析weight+垂直对齐并应用到行子项
@Composable fun String.css(s:RowScope)=pcss(this).let{with(s){var m=it.m;it.w?.let{w->m=m.weight(w,it.f)};(it.a as? Alignment.Vertical)?.let{v->m=m.align(v)};m}}
// BoxScope扩展：解析align并应用到Box子项
@Composable fun String.css(s:BoxScope)=pcss(this).let{with(s){var m=it.m;it.a?.let{a->m=m.align(a)};m}}