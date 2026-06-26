package com.fyan

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

object Fyan{
	lateinit var nc:NavHostController // 全局导航控制器
	lateinit var me:Context // 全局 Context，避免内存泄漏
	var sbar:((Boolean)->Unit)?=null // 状态栏显隐切换器，由 Activity 注入
	var tv=false // 是否为 TV 设备（uiMode 位掩码判断）
	var vs=0L // 当前 versionCode（长整型）
	var vn="" // 当前 versionName 字符串

	// 初始化全局上下文、TV 标识、版本信息
	@Suppress("DEPRECATION") fun init(a:Context){
		me=a.applicationContext
		tv=(me.resources.configuration.uiMode and 15)==4 // UI_MODE_TYPE_MASK == UI_MODE_TYPE_TELEVISION
		val p=me.packageManager.getPackageInfo(me.packageName,0)
		vs=PackageInfoCompat.getLongVersionCode(p) // 兼容 API 28 以下 versionCode
		vn=p.versionName?:"0"
	}

	fun goto(o:String)=nc.navigate(o) // 导航跳转统一入口

	// 同步 HTTP 读取，仅在 IO 线程调用
	fun fetch(u:String):String=java.net.URL(u).openStream().bufferedReader().use{it.readText()}

	val sw:Int get()=me.resources.configuration.screenWidthDp // 屏幕宽度 dp
	val sh:Int get()=me.resources.configuration.screenHeightDp // 屏幕高度 dp
	val gc:Int get()=if(sw>=840)6 else if(sw>=600)4 else 3 // 宫格列数：840→6，600→4，其余→3

	object ff{ // 全局字体规格集合
		val h1=TextStyle(fontSize=24.sp,fontWeight=FontWeight.Bold) // 大标题
		val h2=TextStyle(fontSize=20.sp,fontWeight=FontWeight.Bold) // 二级标题
		val h3=TextStyle(fontSize=18.sp,fontWeight=FontWeight.Bold) // 三级标题（弹窗）
		val h4=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Bold) // 四级标题（导航栏）
		val p=TextStyle(fontSize=14.sp,fontWeight=FontWeight.Normal) // 正文
		val px=TextStyle(fontSize=10.sp,fontWeight=FontWeight.Normal) // 极小字（集数按钮）
		val ps=TextStyle(fontSize=12.sp,fontWeight=FontWeight.Normal) // 小字（角标/简介）
		val pb=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Normal) // 正文加大（弹窗说明）
		val gr=TextStyle(fontSize=10.sp,lineHeight=1.06.em,fontWeight=FontWeight.Normal,fontFamily=FontFamily.Monospace) // 日志等宽字
	}

	class CC(o:Boolean){ // 主题色系，深色/浅色双套，零运行时分配
		val white=Color.White;val black=Color.Black;val trans=Color.Transparent
		val fc=if(o)Color(0xFF5C8EBE)else Color(0xFF90CAF9) // 获焦背景（TV 可识别蓝）
		val ps=if(o)Color(0xFF6A6A6A)else Color(0xFF9E9E9E) // 按压反馈灰
		val m=if(o)Color(0xDD000000)else Color(0xDDFFFFFF) // 半透明遮罩
		val bg=if(o)Color(0xFF000000)else Color(0xFFFFFFFF) // 页面底色
		val cg=if(o)Color(0xFF222222)else Color(0xFFDDDDDD) // 卡片/容器背景
		val ag=if(o)Color(0xFF333333)else Color(0xFFCCCCCC) // 次级容器（封面占位/按钮）
		val c=if(o)Color(0xFFFFFFFF)else Color(0xFF000000) // 主文字色
		val bd=if(o)Color(0xFF444444)else Color(0xFFBBBBBB) // 分割线/边框
		val x=if(o)Color(0xFF555555)else Color(0xFFCCCCCC) // 未选集数按钮背景
		val info=if(o)Color(0xFF2196F3)else Color(0xFF1565C0) // 日志信息色
		val error=Color(0xFFF44336) // 日志错误色（深浅一致）
		val warn=Color(0xFFFF9800) // 日志警告色
		val debug=if(o)Color(0xFFCE93D8)else Color(0xFF6A1B9A) // 日志调试色
		val success=if(o)Color(0xFF4CAF50)else Color(0xFF2E7D32) // 日志成功色
		val primary=if(o)Color(0xFF66AFFF)else Color(0xFF0066FF) // 主题强调色（选中/按钮）
	}
	private val cd=CC(true);private val cl=CC(false) // 深色/浅色主题单例，避免重复分配
	val cc:CC @Composable get()=if(isSystemInDarkTheme())cd else cl // Composable 内获取当前主题色

	// 高性能交互指示器节点：区分获焦/按压状态绘制背景色，适配最新 BOM Indication API
	private class Idn(private val s:InteractionSource,private val f:Color,private val p:Color):Modifier.Node(),DrawModifierNode{
		private var jf=false;private var jp=false
		override fun onAttach(){
			super.onAttach()
			coroutineScope.launch{
				var fc=0;var pc=0
				s.interactions.collect{i->
					when(i){
						is FocusInteraction.Focus->fc++
						is FocusInteraction.Unfocus->fc--
						is PressInteraction.Press->pc++
						is PressInteraction.Release,is PressInteraction.Cancel->pc--
					}
					val nf=fc>0;val np=pc>0
					if(jf!=nf||jp!=np){jf=nf;jp=np;invalidateDraw()} // 状态变化才触发重绘
				}
			}
		}
		override fun ContentDrawScope.draw(){
			if(jp)drawRect(p)else if(jf)drawRect(f) // 按压优先于获焦
			drawContent()
		}
	}
	class Idf(private val f:Color,private val p:Color):IndicationNodeFactory{ // 指示器节点工厂
		override fun create(s:InteractionSource):DelegatableNode=Idn(s,f,p)
		override fun equals(other:Any?):Boolean=other is Idf&&f==other.f&&p==other.p
		override fun hashCode():Int=31*f.hashCode()+p.hashCode()
	}

	private val Context.ds by preferencesDataStore("fyan") // DataStore 单例扩展属性
	// 根据默认值类型自动推断 Preferences.Key，避免手动指定泛型
	@Suppress("UNCHECKED_CAST") private fun <T> String.cype(v:T)=(when(v){
		is ByteArray->byteArrayPreferencesKey(this)
		is Boolean->booleanPreferencesKey(this)
		is Set<*>->stringSetPreferencesKey(this)
		is Double->doublePreferencesKey(this)
		is String->stringPreferencesKey(this)
		is Float->floatPreferencesKey(this)
		is Long->longPreferencesKey(this)
		is Int->intPreferencesKey(this)
		else->null
	}as?Preferences.Key<T>)
	fun <T> cg(k:String,d:T)=me.ds.data.map{p->k.cype(d)?.let{p[it]}?:d} // 读取 Flow，响应式
	suspend fun <T> cs(k:String,v:T)=me.ds.edit{p->k.cype(v)?.let{p[it]=v}} // 写入持久化
	suspend fun cx(k:String)=me.ds.edit{p->p.asMap().keys.firstOrNull{it.name==k}?.let{p.remove(it)}} // 删除 key
	suspend fun <T> co(k:String,d:T)=cg(k,d).first() // 同步读取一次（初始化场景）

	private val gs=mutableStateListOf<String>() // 日志条目列表，驱动 UI 刷新
	private var gn by mutableStateOf(true) // 日志面板是否折叠
	private var gy by mutableStateOf(0f) // 拖拽偏移量（向下拉超 40dp 折叠）
	private val gh=Handler(Looper.getMainLooper()) // 主线程 Handler，保证日志在主线程写入
	private fun gc()=gs.clear() // 清空所有日志
	private fun gx(i:String)=gs.removeAll{it.startsWith(i)} // 按 UUID 前缀删除单条日志
	// 写入日志：m=模块，o=内容，c=级别字符 i/e/w/s/d
	fun log(m:String,o:String,c:Char='i'){
		val t=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date())
		gh.post{gs.add(UUID.randomUUID().toString().replace("-","")+".${c}●$t $m ➜ $o")} // 格式：UUID.级别●时间 模块 ➜ 内容
	}
	@Composable fun Record(){if(gn||gs.isEmpty())RX()else RO()} // 日志面板入口，折叠/展开分支
	// 折叠态：细横条，点击展开
	@Composable private fun RX(){Box(modifier=Modifier.fillMaxWidth(0.7f).height(6.dp).padding(bottom=2.dp).clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).clickable{gn=false;gy=0f})}
	@Composable private fun RO(){ // 展开态：完整日志面板，支持下拉折叠手势
		val cc=Fyan.cc;val ff=Fyan.ff;val s=rememberLazyListState()
		LaunchedEffect(gs.size){if(gs.isNotEmpty())s.animateScrollToItem(gs.size-1)} // 新日志自动滚底
		Box(modifier=Modifier.fillMaxWidth().heightIn(max=(Fyan.sh/3).dp).navigationBarsPadding()
			.offset{IntOffset(0,gy.roundToInt())} // 跟随拖拽偏移
			.pointerInput(Unit){
				detectDragGestures(
					onDragEnd={if(gy>40f){gn=true;gy=0f}else gy=0f}, // 超阈值折叠，否则回弹
					onDrag={ch,o->ch.consume();if(gy+o.y>=0f)gy+=o.y} // 仅允许向下拖拽
				)
			}
		){
			Box(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=4.dp,topEnd=4.dp))
				.background(cc.m).drawWithContent{
					drawContent()
					val(w,r)=0.5.dp.toPx() to 4.dp.toPx()
					// 绘制三边圆角边框线（上+左+右，底部不绘）
					drawPath(path=Path().apply{
						moveTo(0f,size.height);lineTo(0f,r)
						arcTo(Rect(0f,0f,r*2,r*2),180f,90f,false)
						lineTo(size.width-r,0f)
						arcTo(Rect(size.width-r*2,0f,size.width,r*2),270f,90f,false)
						lineTo(size.width,size.height)
					},color=cc.bd,style=Stroke(w,cap=StrokeCap.Round,join=StrokeJoin.Round))
				}
			){
				Column(modifier=Modifier.fillMaxWidth().padding(4.dp)){
					Box(modifier=Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){ // 顶部拖拽指示条
						Box(modifier=Modifier.fillMaxWidth(0.25f).height(4.dp).clip(RoundedCornerShape(2.dp))
							.background(cc.ag).clickable(enabled=tv){gn=true;gy=0f}) // TV 模式可点击折叠
					}
					Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){ // 标题行
						BasicText("日志 · ${gs.size}条",style=ff.ps.copy(color=cc.c))
						Box(modifier=Modifier.clickable{gc()}){BasicText("清空",style=ff.ps.copy(color=cc.error))}
					}
					LazyColumn(state=s,modifier=Modifier.fillMaxWidth().padding(bottom=6.dp)){
						items(gs,key={it.substringBefore("●")}){o-> // 以 UUID 段为 key，避免全量重组
							val x=o.split("●",limit=2);val z=x[0].lastIndexOf('.')
							val id=if(z>0)x[0].substring(0,z)else x[0] // 提取 UUID（用于单条删除）
							val cx=if(z>0)x[0].substring(z+1)else"i" // 提取级别字符
							val c=when(cx){"e"->cc.error;"w"->cc.warn;"s"->cc.success;"d"->cc.debug;else->cc.info}
							Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd)) // 分割线
							Row(modifier=Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){
								BasicText(x.getOrElse(1){"...."},modifier=Modifier.weight(1f).padding(end=4.dp),style=ff.gr.copy(color=c))
								Box(modifier=Modifier.size(14.dp).clickable{gx(id)},contentAlignment=Alignment.Center){ // 单条删除
									BasicText("╳",style=ff.ps.copy(color=cc.c))
								}
							}
						}
					}
				}
			}
		}
	}
}

class O:ComponentActivity(){
	private var br:BroadcastReceiver?=null // 下载完成广播接收器

	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		enableEdgeToEdge() // 开启边到边沉浸体验
		Fyan.init(this)
		Fyan.sbar={o->
			WindowCompat.getInsetsController(window,window.decorView).run{
				if(o)show(WindowInsetsCompat.Type.statusBars())
				else{hide(WindowInsetsCompat.Type.statusBars());systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE}
			}
		}
		setContent{
			val cc=Fyan.cc
			CompositionLocalProvider(LocalIndication provides remember(cc){Fyan.Idf(cc.fc,cc.ps)}){ // 注入主题感知指示器
				Fyan.nc=rememberNavController()
				var exit by remember{mutableStateOf(false)} // 退出确认弹窗状态
				BackHandler(enabled=!exit){exit=true} // 首次返回弹确认
				if(exit)Dialog(onDismissRequest={exit=false}){ // 退出确认弹窗
					Column(modifier=Modifier.fillMaxWidth().padding(24.dp)
						.clip(RoundedCornerShape(10.dp)).background(Fyan.cc.cg)
						.border(1.dp,Fyan.cc.bd,RoundedCornerShape(10.dp)).padding(24.dp),
						verticalArrangement=Arrangement.spacedBy(12.dp),
						horizontalAlignment=Alignment.CenterHorizontally
					){
						BasicText("系统提醒",style=Fyan.ff.h3.copy(color=Fyan.cc.c))
						BasicText("确定杀死应用并退出吗？",style=Fyan.ff.pb.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
						Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
							Box(modifier=Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp)) // 取消按钮
								.background(Fyan.cc.ag).clickable{exit=false},contentAlignment=Alignment.Center
							){BasicText("取消",style=Fyan.ff.p.copy(color=Fyan.cc.c))}
							Box(modifier=Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp)) // 确定：finishAffinity + 杀进程
								.background(Fyan.cc.primary).clickable{
									finishAffinity()
									Handler(Looper.getMainLooper()).postDelayed({Process.killProcess(Process.myPid())},100)
								},contentAlignment=Alignment.Center
							){BasicText("确定",style=Fyan.ff.p.copy(color=Fyan.cc.cg))}
						}
					}
				}
				Box(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg).systemBarsPadding(),contentAlignment=Alignment.BottomCenter){
					NavHost(navController=Fyan.nc,startDestination="home"){ // 路由配置
						composable("home"){Home()}
						composable("ayf_home"){AyfHome()}
						composable("ayf_info/{id}"){x->AyfInfo(id=x.arguments?.getString("id")?:"")}
					}
					Fyan.Record() // 日志面板叠加在最顶层
				}
				LaunchedEffect(Unit){Fyan.log("系统","检查更新");check()} // 启动后异步检查更新
			}
		}
	}

	override fun onDestroy(){super.onDestroy();br?.let{unregisterReceiver(it)}} // 解注册防泄漏

	private fun check(){ // 对比 GitHub Releases 最新版本号
		lifecycleScope.launch(Dispatchers.IO){
			runCatching{
				val r=OkHttpClient().newCall(Request.Builder().url("https://github.com/lyu2026/fyan/releases/latest").build()).execute()
				val v=r.request.url.toString().substringAfterLast("/") // 从重定向 URL 提取版本号
				val nv=v.filter(Char::isDigit).toLongOrNull()?:0L
				val cv=Fyan.vn.filter(Char::isDigit).toLongOrNull()?:0L
				if(nv>cv)withContext(Dispatchers.Main){Fyan.log("系统","发现新版本($v)，开始下载");upgrade(v)}
			}
		}
	}

	private fun upgrade(v:String){ // DownloadManager 静默后台下载并安装新版 APK
		val req=DownloadManager.Request(Uri.parse("https://github.com/lyu2026/fyan/releases/download/$v/fyan.apk")).apply{
			setTitle("Fyan → $v");setDescription("后台下载中...")
			setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) // 完成后通知栏提示
			setMimeType("application/vnd.android.package-archive")
			setDestinationInExternalFilesDir(Fyan.me,"Download","fyan_$v.apk") // 存入应用私有外部目录
		}
		val dm=Fyan.me.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		val id=dm.enqueue(req)
		Toast.makeText(Fyan.me,"检测到新版，已开启后台静默下载",Toast.LENGTH_SHORT).show()
		br?.let{unregisterReceiver(it)} // 先解注册旧接收器，防重复
		br=object:BroadcastReceiver(){
			override fun onReceive(c:Context,i:Intent){
				if(i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1L)!=id)return // 过滤非本次任务
				val f=File(Fyan.me.getExternalFilesDir("Download"),"fyan_$v.apk")
				if(!f.exists())return // 文件不存在，忽略（下载失败场景）
				val u=if(Build.VERSION.SDK_INT>=24)FileProvider.getUriForFile(Fyan.me,"${Fyan.me.packageName}.fileprovider",f)else Uri.fromFile(f)
				Fyan.me.startActivity(Intent(Intent.ACTION_VIEW).apply{
					setDataAndType(u,"application/vnd.android.package-archive")
					addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
				})
			}
		}
		// API 33+ 需显式声明 RECEIVER_EXPORTED
		if(Build.VERSION.SDK_INT>=33)registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),Context.RECEIVER_EXPORTED)
		else registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
	}
}