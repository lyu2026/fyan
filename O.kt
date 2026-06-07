package com.fyan

import java.util.*
import java.text.SimpleDateFormat
import kotlin.math.*
import kotlin.math.roundToInt
import android.view.View
import android.os.Bundle
import android.app.Activity
import android.graphics.Rect
import android.content.Context
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.animateContentSize


// ─── css DSL 顶层工具函数（提升出 css() 避免每次重建）───

private fun String.cssAlpha()=substringAfter(".","1").toFloatOrNull()?.let{if(it>1f)it/100f else it}?:1f
private fun String.cssColor()=runCatching{
	Color(android.graphics.Color.parseColor("#${substringBefore(".")}")).copy(alpha=cssAlpha())
}.getOrNull()
private fun String.cssColorStrict()=runCatching{Color(android.graphics.Color.parseColor("#$this"))}.getOrNull()

private fun cssParseStops(parts:List<String>):Pair<List<Float>,List<Color>>{
	val st=mutableListOf<Float>();val cl=mutableListOf<Color>()
	parts.forEach{seg->seg.split(":").let{s->
		if(s.size==2)s[0].toFloatOrNull()?.let{f->s[1].cssColorStrict()?.let{c->st+=if(f>1f)f/100f else f;cl+=c}}
		else seg.cssColorStrict()?.let{cl+=it}
	}}
	if(st.size<cl.size)repeat(cl.size-st.size){i->st.add(i.toFloat()/(cl.size-1).coerceAtLeast(1))}
	return st to cl
}

private fun List<Float>.zipColors(cl:List<Color>)=zip(cl).map{(a,b)->a to b}.toTypedArray()

private fun cssGradAngle(a:Float)=a*PI.toFloat()/180f
private fun cssGradOffsets(angle:Float)=Pair(
	Offset((0.5f-cos(angle)*0.5f).coerceIn(0f,1f)*10000f,(0.5f-sin(angle)*0.5f).coerceIn(0f,1f)*10000f),
	Offset((0.5f+cos(angle)*0.5f).coerceIn(0f,1f)*10000f,(0.5f+sin(angle)*0.5f).coerceIn(0f,1f)*10000f)
)

private fun String.cssFillFrac(skip:Int)=drop(skip).toFloatOrNull()


// ─── Modifier.css ───

fun Modifier.css(s:String,scope:Any?=null,clicks:Map<String,()->Unit>=emptyMap()):Modifier=s.split(" ").fold(this){m,o->
	val idx=o.indexOf(':')
	if(idx<0)return@fold m
	val k=o.substring(0,idx);val v=o.substring(idx+1)
	when(k){
		"bd"->{
			val segs=v.split("/")
			val w=segs.getOrNull(0)?.toFloatOrNull()?.dp?:return@fold m
			when(segs.getOrNull(1)){
				"circle"->segs.getOrNull(2)?.cssColor()?.let{m.border(BorderStroke(w,it),CircleShape)}?:m
				"cut"->{
					val r=segs.getOrNull(2)?.toFloatOrNull()?.dp?:0.dp
					segs.getOrNull(3)?.cssColor()?.let{m.border(BorderStroke(w,it),CutCornerShape(r))}?:m
				}
				"lg"->{
					val rest=v.removePrefix("${segs[0]}/lg/").split(",")
					val ang=cssGradAngle(rest.firstOrNull()?.toFloatOrNull()?:0f)
					val(st,cl)=cssParseStops(rest.drop(1));if(cl.size<2)return@fold m
					val(s,e)=cssGradOffsets(ang)
					val r=segs.getOrNull(2)?.toFloatOrNull()?.dp?:0.dp
					m.border(BorderStroke(w,Brush.linearGradient(colorStops=st.zipColors(cl),start=s,end=e)),RoundedCornerShape(r))
				}
				"rg"->{
					val(st,cl)=cssParseStops(v.removePrefix("${segs[0]}/rg/").split(","));if(cl.size<2)return@fold m
					val r=segs.getOrNull(2)?.toFloatOrNull()?.dp?:0.dp
					m.border(BorderStroke(w,Brush.radialGradient(colorStops=st.zipColors(cl))),RoundedCornerShape(r))
				}
				"sg"->{
					val(st,cl)=cssParseStops(v.removePrefix("${segs[0]}/sg/").split(","));if(cl.size<2)return@fold m
					val r=segs.getOrNull(2)?.toFloatOrNull()?.dp?:0.dp
					m.border(BorderStroke(w,Brush.sweepGradient(colorStops=st.zipColors(cl))),RoundedCornerShape(r))
				}
				"side"->{
					val c=segs.getOrNull(2)?.cssColor()?:return@fold m
					val sides=(segs.getOrNull(3)?:"t,b,s,e").split(",").toSet()
					m.drawBehind{
						val wp=w.toPx()
						if("t" in sides)drawLine(c,Offset(0f,0f),Offset(size.width,0f),wp)
						if("b" in sides)drawLine(c,Offset(0f,size.height),Offset(size.width,size.height),wp)
						if("s" in sides)drawLine(c,Offset(0f,0f),Offset(0f,size.height),wp)
						if("e" in sides)drawLine(c,Offset(size.width,0f),Offset(size.width,size.height),wp)
					}
				}
				"dash"->{
					val interval=segs.getOrNull(2)?.toFloatOrNull()?:8f
					val c=segs.getOrNull(3)?.cssColor()?:return@fold m
					val r=segs.getOrNull(4)?.toFloatOrNull()?.dp?:0.dp
					m.drawBehind{
						val wp=w.toPx()
						drawPath(
							Path().apply{addRoundRect(RoundRect(wp/2,wp/2,size.width-wp/2,size.height-wp/2,radiusX=r.toPx(),radiusY=r.toPx()))},
							c,style=Stroke(width=wp,pathEffect=PathEffect.dashPathEffect(floatArrayOf(interval,interval)))
						)
					}
				}
				else->{
					val r=segs.getOrNull(1)?.toFloatOrNull()?.dp?:0.dp
					segs.getOrNull(2)?.cssColor()?.let{m.border(BorderStroke(w,it),RoundedCornerShape(r))}?:m
				}
			}
		}
		"bg"->when{
			v.startsWith("lg/")->{
				val segs=v.removePrefix("lg/").split(",")
				val ang=cssGradAngle(segs.firstOrNull()?.toFloatOrNull()?:0f)
				val(st,cl)=cssParseStops(segs.drop(1));if(cl.size<2)return@fold m
				val(s,e)=cssGradOffsets(ang)
				m.background(Brush.linearGradient(colorStops=st.zipColors(cl),start=s,end=e))
			}
			v.startsWith("rg/")->{
				val(st,cl)=cssParseStops(v.removePrefix("rg/").split(","));if(cl.size<2)return@fold m
				m.background(Brush.radialGradient(colorStops=st.zipColors(cl)))
			}
			v.startsWith("sg/")->{
				val(st,cl)=cssParseStops(v.removePrefix("sg/").split(","));if(cl.size<2)return@fold m
				m.background(Brush.sweepGradient(colorStops=st.zipColors(cl)))
			}
			v.startsWith("a/")->v.removePrefix("a/").toFloatOrNull()?.let{m.alpha(if(it>1f)it/100f else it)}?:m
			else->v.cssColor()?.let{m.background(it)}?:m
		}
		"cp"->{
			val args=v.split("/")
			val shape=when(args[0]){
				"circle"->CircleShape
				"rect"->RoundedCornerShape(args.getOrNull(1)?.toFloatOrNull()?.dp?:0.dp)
				"cut"->CutCornerShape(args.getOrNull(1)?.toFloatOrNull()?.dp?:0.dp)
				else->return@fold m
			}
			m.clip(shape)
		}
		"sz"->{
			if(v.startsWith("hin/")){
				val p=v.removePrefix("hin/").split(",")
				return@fold m.heightIn(min=p.getOrNull(0)?.toFloatOrNull()?.dp?:0.dp,max=p.getOrNull(1)?.toFloatOrNull()?.dp?:Dp.Infinity)
			}
			if(v.startsWith("win/")){
				val p=v.removePrefix("win/").split(",")
				return@fold m.widthIn(min=p.getOrNull(0)?.toFloatOrNull()?.dp?:0.dp,max=p.getOrNull(1)?.toFloatOrNull()?.dp?:Dp.Infinity)
			}
			val raw=v.split(",")
			val x=raw.map{it.toDoubleOrNull()?.dp}
			if(x.isEmpty()||x.all{it==null})return@fold m
			when(raw.size){
				1->when{
					raw[0].startsWith("fw")->raw[0].cssFillFrac(2)?.let{m.fillMaxWidth(it)}?:m.fillMaxWidth()
					raw[0].startsWith("fh")->raw[0].cssFillFrac(2)?.let{m.fillMaxHeight(it)}?:m.fillMaxHeight()
					raw[0].startsWith("f")->raw[0].cssFillFrac(1)?.let{m.fillMaxSize(it)}?:m.fillMaxSize()
					else->x[0]?.let{m.size(it)}?:m
				}
				2->m
					.let{r->when{
						raw[0].startsWith("fw")->raw[0].cssFillFrac(2)?.let{r.fillMaxWidth(it)}?:r.fillMaxWidth()
						raw[0].startsWith("f")->raw[0].cssFillFrac(1)?.let{r.fillMaxWidth(it)}?:r.fillMaxWidth()
						else->x[0]?.let{r.width(it)}?:r
					}}
					.let{r->when{
						raw[1].startsWith("fh")->raw[1].cssFillFrac(2)?.let{r.fillMaxHeight(it)}?:r.fillMaxHeight()
						raw[1].startsWith("f")->raw[1].cssFillFrac(1)?.let{r.fillMaxHeight(it)}?:r.fillMaxHeight()
						else->x[1]?.let{r.height(it)}?:r
					}}
				else->m
			}
		}
		"pd"->{
			when(v){
				"nb"->return@fold m.navigationBarsPadding()
				"sb"->return@fold m.statusBarsPadding()
				"sys"->return@fold m.systemBarsPadding()
				"ime"->return@fold m.imePadding()
			}
			val x=v.split(",").map{it.toDoubleOrNull()?.dp}
			if(x.isEmpty()||x.all{it==null}||x.size>4)return@fold m
			when(x.size){
				1->x[0]?.let{m.padding(it)}?:m
				2->m.let{r->x[0]?.let{r.padding(vertical=it)}?:r}.let{r->x[1]?.let{r.padding(horizontal=it)}?:r}
				3->m.let{r->x[0]?.let{r.padding(top=it)}?:r}.let{r->x[1]?.let{r.padding(horizontal=it)}?:r}.let{r->x[2]?.let{r.padding(bottom=it)}?:r}
				4->m.let{r->x[0]?.let{r.padding(top=it)}?:r}.let{r->x[1]?.let{r.padding(end=it)}?:r}.let{r->x[2]?.let{r.padding(bottom=it)}?:r}.let{r->x[3]?.let{r.padding(start=it)}?:r}
				else->m
			}
		}
		"ox"->{
			val isPx=v.startsWith("px:")
			val pair=(if(isPx)v.removePrefix("px:") else v).split(",")
			val x=pair.getOrNull(0)?.toFloatOrNull()?:0f
			val y=pair.getOrNull(1)?.toFloatOrNull()?:0f
			if(isPx)m.offset{IntOffset(x.roundToInt(),y.roundToInt())} else m.offset(x.dp,y.dp)
		}
		"wg"->when(scope){
			is RowScope->with(scope){v.split("/").let{a->a[0].toFloatOrNull()?.let{w->m.weight(w,a.getOrNull(1)?.let{it!="f"}?:true)}?:m}}
			is ColumnScope->with(scope){v.split("/").let{a->a[0].toFloatOrNull()?.let{w->m.weight(w,a.getOrNull(1)?.let{it!="f"}?:true)}?:m}}
			else->m
		}
		"ag"->{
			val al=when(v){
				"tl","ts"->Alignment.TopStart;"tc","tm"->Alignment.TopCenter;"tr","te"->Alignment.TopEnd
				"cl","cs"->Alignment.CenterStart;"c","cc","cm"->Alignment.Center;"cr","ce"->Alignment.CenterEnd
				"bl","bs"->Alignment.BottomStart;"bc","bm"->Alignment.BottomCenter;"br","be"->Alignment.BottomEnd
				else->return@fold m
			}
			m.wrapContentSize(al,unbounded=false)
		}
		"cc"->{
			val onClick=clicks[v]?:clicks[""]?:{}
			if(v=="norip")m.clickable(indication=null,interactionSource=remember{MutableInteractionSource()},onClick=onClick)
			else m.clickable(indication=ripple(),interactionSource=remember{MutableInteractionSource()},onClick=onClick)
		}
		else->m
	}
}


// ─── 日志 ───

data class Log(
	val i:String=UUID.randomUUID().toString(),
	val w:String,
	val o:String,
	val t:String,  // 由 add() 传入，不用默认值避免初始化时固化
	val c:Char=I
){
	companion object{
		const val S='s';const val E='e';const val W='w';const val I='i'
		// SimpleDateFormat 非线程安全，ThreadLocal 隔离
		private val fmt=ThreadLocal.withInitial{SimpleDateFormat("HH:mm:ss",Locale.getDefault())}
		private val list=mutableStateListOf<Log>()

		fun add(w:String,o:String,c:Char=I){list.add(Log(w=w,o=o,c=c,t=fmt.get()!!.format(Date())))}
		fun remove(id:String){list.removeAll{it.i==id}}
		fun clear(){list.clear()}

		fun color(c:Char):Color=when(c){
			S->Color(0xFF189B46);E->Color(0xFFE7012F);W->Color(0xFFFDD10D)
			else->Color.Unspecified
		}

		@Composable
		fun Panel(modifier:Modifier=Modifier,tv:Boolean=false){
			var expanded by remember{mutableStateOf(false)}
			val h=LocalConfiguration.current.screenHeightDp.dp
			if(expanded)Expanded(modifier,tv,h/3,list){expanded=false}
			else Collapsed(modifier){expanded=true}
		}

		@Composable
		private fun Expanded(modifier:Modifier,tv:Boolean,height:Dp,logs:List<Log>,onCollapse:()->Unit){
			val sp=remember{RoundedCornerShape(topStart=6.dp,topEnd=6.dp)}
			var dragY by remember{mutableStateOf(0f)}
			val ls=rememberLazyListState()
			LaunchedEffect(logs.lastOrNull()?.i){if(logs.isNotEmpty())ls.animateScrollToItem(logs.size-1)}
			Box(
				modifier=modifier
					.css("sz:fw pd:0,0.5")
					.heightIn(max=height)
					.css("pd:nb")
					.offset{IntOffset(0,dragY.roundToInt())}
					.css("cp:rect/6 bg:1C1C1E.92 bd:1/0/808080.15")
					.pointerInput(Unit){detectDragGestures(
						onDragEnd={if(dragY>150f)onCollapse() else dragY=0f},
						onDrag={c,d->c.consume();if(dragY+d.y>=0f)dragY+=d.y}
					)}
			){
				Column(modifier=Modifier.css("sz:fw pd:2,5")){
					Handle(tv=tv,onCollapse=onCollapse)
					ItemList(state=ls,logs=logs)
				}
			}
		}

		@Composable
		private fun ColumnScope.Handle(tv:Boolean,onCollapse:()->Unit){
			Box(
				modifier=Modifier
					.css("sz:64,12")
					.align(Alignment.CenterHorizontally)
					.pointerInput(tv){if(!tv)detectTapGestures(onTap={onCollapse()})}
					.clickable(interactionSource=remember{MutableInteractionSource()},indication=null,enabled=tv){onCollapse()},
				contentAlignment=Alignment.Center
			){
				Box(modifier=Modifier.css("sz:64,3 bg:808080.40 cp:rect/1.5"))
			}
		}

		@Composable
		private fun ItemList(state:LazyListState,logs:List<Log>){
			LazyColumn(state=state,modifier=Modifier.css("sz:fw pd:4,0,0,0")){
				items(logs,key={it.i}){Item(it)}
			}
		}

		@Composable
		private fun LazyItemScope.Item(log:Log){
			val c=color(log.c)
			Row(
				modifier=Modifier.css("sz:fw pd:1,0"),
				horizontalArrangement=Arrangement.SpaceBetween,
				verticalAlignment=Alignment.Top
			){
				Text(
					text="[${log.t}] ${log.w} ➜ ${log.o}",
					color=if(c==Color.Unspecified)MaterialTheme.colorScheme.onSurface else c,
					style=MaterialTheme.typography.bodySmall.copy(lineHeight=1.2.em,fontFamily=FontFamily.Monospace),
					modifier=Modifier.weight(1f).css("pd:0,4,0,0")
				)
				IconButton(
					onClick={remove(log.i)},
					modifier=Modifier.css("sz:14,14").align(Alignment.CenterVertically)
				){
					Icon(
						painter=painterResource(R.drawable.delete),
						contentDescription=null,
						tint=Color.Gray.copy(alpha=0.7f),
						modifier=Modifier.css("sz:12,12")
					)
				}
			}
			HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
		}

		@Composable
		private fun Collapsed(modifier:Modifier,onExpand:()->Unit){
			Box(
				modifier=modifier
					.css("pd:0,0,6,0 pd:nb sz:80,5 bg:808080.50 cp:rect/2.5")
					.clickable(interactionSource=remember{MutableInteractionSource()},indication=null){onExpand()}
			)
		}
	}
}


// ─── 首页卡片 ───

@Composable
fun Rard(m:Modifier=Modifier,c:Boolean=false,t:String,x:String="",click:()->Unit){
	val primary=MaterialTheme.colorScheme.primary
	val bodySmall=MaterialTheme.typography.bodySmall
	val surfaceVar=MaterialTheme.colorScheme.surfaceVariant
	val fr=remember{FocusRequester()}
	val src=remember{MutableInteractionSource()}
	val focused by src.collectIsFocusedAsState()
	val pressed by src.collectIsPressedAsState()
	// focused 时用 primary，仅 pressed 时半透明 primary，其余透明
	val borderColor=when{focused->primary;pressed->primary.copy(alpha=0.6f);else->Color.Transparent}
	Card(
		shape=RoundedCornerShape(5.dp),
		modifier=m.focusRequester(fr)
			.border(BorderStroke(1.dp,borderColor),RoundedCornerShape(5.dp))
			.clickable(src,null,onClick=click),
		colors=CardDefaults.cardColors(containerColor=surfaceVar.copy(alpha=0.6f))
	){
		Box(
			Modifier.fillMaxWidth().css("pd:12,10"),
			contentAlignment=if(c)Alignment.Center else Alignment.CenterStart
		){
			Column{
				Text(t,style=MaterialTheme.typography.titleMedium)
				if(x.isNotBlank()){
					Spacer(Modifier.height(2.dp))
					Text(x,style=bodySmall,color=Color.Gray,maxLines=2)
				}
			}
		}
	}
}


// ─── 退出确认 ───

@Composable
fun Quit(c:Context){
	var show by remember{mutableStateOf(false)}
	BackHandler(show){show=false}
	if(show)AlertDialog(
		onDismissRequest={show=false},
		shape=RoundedCornerShape(4.dp),
		title={Text("系统提示")},
		text={Text("确定要退出吗？")},
		dismissButton={TextButton(onClick={show=false}){Text("取消")}},
		confirmButton={TextButton(onClick={(c as?Activity)?.finish()?:run{show=false}}){Text("确认")}}
	)
}


// ─── Activity ───

class O:ComponentActivity(){
	override fun dispatchTouchEvent(e:MotionEvent):Boolean{
		val v=currentFocus
		if(e.action!=MotionEvent.ACTION_DOWN||v==null)return super.dispatchTouchEvent(e)
		val loc=IntArray(2).also{v.getLocationOnScreen(it)}
		val r=Rect(loc[0],loc[1],loc[0]+v.width,loc[1]+v.height)
		if(r.contains(e.rawX.toInt(),e.rawY.toInt()))return super.dispatchTouchEvent(e)
		(getSystemService(Context.INPUT_METHOD_SERVICE)as InputMethodManager).hideSoftInputFromWindow(v.windowToken,0)
		v.clearFocus()
		return super.dispatchTouchEvent(e)
	}
	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent{
			val scheme=if(isSystemInDarkTheme())darkColorScheme() else lightColorScheme()
			MaterialTheme(colorScheme=scheme){
				Surface(modifier=Modifier.fillMaxSize(),color=scheme.background){X()}
			}
		}
	}
}


// ─── 入口 ───

@Composable
fun X(){
	val ctx=LocalContext.current
	val nav=rememberNavController()
	val fm=LocalFocusManager.current
	var sg by remember{mutableStateOf(true)}
	val cfg=LocalConfiguration.current
	val tv=(cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION

	Box(
		modifier=Modifier.fillMaxSize().pointerInput(Unit){
			awaitPointerEventScope{
				while(true){
					val ev=awaitPointerEvent(PointerEventPass.Initial)
					if(ev.changes.any{it.changedToDown()})fm.clearFocus()
				}
			}
		}
	){
		LaunchedEffect(Unit){Log.add("系统","初始化就绪",Log.S)}

		NavHost(navController=nav,startDestination="home"){
			composable("home"){
				Quit(ctx)
				Home(
					tv=tv,sg=sg,
					tg={sg=!sg;Log.add("首页","日志面板: ${if(sg)"显示" else "隐藏"}",Log.I)},
					go={route->
						Log.add("导航","切换至: [$route]",Log.S)
						if(route=="home"||route=="setting")nav.navigate(route)
					},
					test={Log.add("测试","模拟点击",Log.S)}
				)
			}
			composable("setting"){
				Setting(
					back={Log.add("导航","返回首页",Log.I);nav.popBackStack()},
					save={k,v->Log.add("设置","[$k] = $v",Log.S)}
				)
			}
		}

		if(sg)Log.Panel(modifier=Modifier.align(Alignment.BottomCenter),tv=tv)
	}
}


// ─── 首页 ───

@Composable
fun Home(tv:Boolean,sg:Boolean,tg:()->Unit,go:(String)->Unit,test:()->Unit){
	Column(modifier=Modifier.fillMaxSize()){
		Row(
			modifier=Modifier.fillMaxWidth().statusBarsPadding().height(48.dp).css("pd:0,3,0,10"),
			horizontalArrangement=Arrangement.SpaceBetween,
			verticalAlignment=Alignment.CenterVertically
		){
			Text("首页",style=MaterialTheme.typography.titleLarge)
			Row(verticalAlignment=Alignment.CenterVertically){
				Text(
					text=if(tv)"📺 TV" else "📱 MB",
					style=MaterialTheme.typography.bodyMedium,
					modifier=Modifier.css("pd:0,8,0,0")
				)
				IconButton(onClick=tg,modifier=Modifier.css("sz:36,36")){
					Icon(
						painter=painterResource(if(sg)R.drawable.visibility else R.drawable.visibility_off),
						contentDescription="切换日志面板",
						modifier=Modifier.css("sz:20,20")
					)
				}
			}
		}
		Column(modifier=Modifier.css("pd:4,10,0,10")){
			Row(verticalAlignment=Alignment.CenterVertically){
				Rard(m=Modifier.fillMaxWidth(1f/3f).css("pd:0,2,0,0"),c=true,t="努努",click={go("nnu")})
				Rard(m=Modifier.fillMaxWidth(1f/2f).css("pd:0,2"),c=true,t="欧乐",click={go("ole")})
				Rard(m=Modifier.fillMaxWidth().css("pd:0,0,0,2"),c=true,t="爱壹帆",click={go("ayf")})
			}
			Rard(m=Modifier.fillMaxWidth().css("pd:4,0,0,0"),t="游戏大全",x="本地益智小游戏",click={go("setting")})
			Rard(m=Modifier.fillMaxWidth().css("pd:4,0,0,0"),t="科学书城",x="追加模拟警告事件",click={test()})
			Rard(m=Modifier.fillMaxWidth().css("pd:4,0,0,0"),t="私人日记",x="随心散记",click={test()})
		}
	}
}


// ─── 设置页 ───

@Composable
fun Setting(back:()->Unit,save:(String,String)->Unit){
	var field by remember{mutableStateOf("")}
	var expanded by remember{mutableStateOf(true)}
	val sp=RoundedCornerShape(5.dp)
	Column(modifier=Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())){
		Row(
			verticalAlignment=Alignment.CenterVertically,
			modifier=Modifier.height(48.dp).css("pd:0,10,0,1")
		){
			IconButton(onClick=back,modifier=Modifier.css("sz:36,36")){
				Icon(painter=painterResource(R.drawable.arrow_back),contentDescription="返回",modifier=Modifier.css("sz:20,20"))
			}
			Text("系统配置",style=MaterialTheme.typography.titleLarge)
		}
		Card(
			shape=sp,
			modifier=Modifier.fillMaxWidth().css("pd:4,10,0,10"),
			colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f))
		){
			Column(modifier=Modifier.animateContentSize().clip(sp)){
				Row(
					modifier=Modifier.fillMaxWidth().css("pd:10"),
					horizontalArrangement=Arrangement.SpaceBetween,
					verticalAlignment=Alignment.CenterVertically
				){
					Text("测试数据",style=MaterialTheme.typography.titleMedium)
					Icon(
						painter=painterResource(if(expanded)R.drawable.expand_less else R.drawable.expand_more),
						contentDescription=if(expanded)"折叠" else "展开",
						modifier=Modifier.css("sz:24,24 cc:norip"),
						clicks=mapOf("norip" to {expanded=!expanded}) // cc:norip 需 clicks 配合
					).also{}
					// 直接用 clickable 更简洁：
					// modifier=Modifier.css("sz:24,24").clickable{expanded=!expanded}
				}
				if(expanded){
					HorizontalDivider(color=MaterialTheme.colorScheme.background.copy(alpha=0.7f))
					OutlinedTextField(
						value=field,onValueChange={field=it},label={Text("关联数据")},
						modifier=Modifier.fillMaxWidth().css("pd:6,10,10,10"),
						singleLine=true,
						keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),
						keyboardActions=KeyboardActions(onDone={save("TEST",field)})
					)
				}
			}
		}
	}
}