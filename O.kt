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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt


object Fyan{ // 全局数据
	lateinit var nc:NavHostController // 全局导航控制器
	lateinit var me:Context // 全局 applicationContext，避免内存泄漏
	var tv=false // 是否为 TV 设备（uiMode 位掩码判断）
	var vs=0L // 当前安装包的 versionCode（长整型）
	var vn="" // 当前安装包的 versionName 字符串

	// 初始化全局状态：上下文、TV 标识、版本信息
	@Suppress("DEPRECATION") fun init(a:Context){
		me=a.applicationContext
		tv=(me.resources.configuration.uiMode and 15)==4 // uiMode & UI_MODE_TYPE_MASK == UI_MODE_TYPE_TELEVISION
		val p=me.packageManager.getPackageInfo(me.packageName,0)
		vs=PackageInfoCompat.getLongVersionCode(p) // 兼容 API 28 以下的 versionCode 获取
		vn=p.versionName?:"0"
	}

	// 导航跳转封装，统一入口
	fun goto(o:String)=nc.navigate(o)

	// 同步网络请求：打开 URL 输入流，读取全部文本（IO 线程调用）
	fun fetch(u:String):String=java.net.URL(u).openStream().bufferedReader().use{it.readText()}

	// 当前屏幕宽度（dp），用于响应式布局判断
	val sw:Int get()=me.resources.configuration.screenWidthDp
	// 当前屏幕高度（dp），用于日志面板高度计算
	val sh:Int get()=me.resources.configuration.screenHeightDp
	// 网格列数：平板840dp以上6列，600dp以上4列，手机3列
	val gc:Int get()=if(sw>=840)6 else if(sw>=600)4 else 3

	object ff{ // 字体样式集合，统一管理全局文字规格
		val h1=TextStyle(fontSize=24.sp,fontWeight=FontWeight.Bold) // 大标题
		val h2=TextStyle(fontSize=20.sp,fontWeight=FontWeight.Bold) // 二级标题
		val h3=TextStyle(fontSize=18.sp,fontWeight=FontWeight.Bold) // 三级标题（弹窗标题）
		val h4=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Bold) // 四级标题（导航栏）
		val p=TextStyle(fontSize=14.sp,fontWeight=FontWeight.Normal) // 正文
		val ps=TextStyle(fontSize=12.sp,fontWeight=FontWeight.Normal) // 小字（角标、简介）
		val pb=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Normal) // 正文加大（弹窗说明）
		val gr=TextStyle(fontSize=10.sp,lineHeight=1.06.em,fontWeight=FontWeight.Normal,fontFamily=FontFamily.Monospace) // 日志等宽字体
	}

	class CC(o:Boolean){ // 主题色彩系统，根据深色/浅色模式动态切换
		val w=Color.White // 白色
		val fc=if(o)Color(0xFF5C8EBE)else Color(0xFF90CAF9) // 获焦背景（亮蓝调，TV极易识别且不刺眼）
		val ps=if(o)Color(0xFF6A6A6A)else Color(0xFF9E9E9E) // 轻触反馈（稳重深/中灰，下压感明确）
		val m=if(o)Color(0xDD000000)else Color(0xDDFFFFFF) // 半透明遮罩（日志面板背景）
		val bg=if(o)Color(0xFF000000)else Color(0xFFFFFFFF) // 页面底色
		val cg=if(o)Color(0xFF222222)else Color(0xFFDDDDDD) // 卡片/容器背景
		val ag=if(o)Color(0xFF333333)else Color(0xFFCCCCCC) // 次级容器（封面占位/按钮）
		val c=if(o)Color(0xFFFFFFFF)else Color(0xFF000000) // 主文字色
		val bd=if(o)Color(0xFF444444)else Color(0xFFBBBBBB) // 分割线/边框色
		val x=if(o)Color(0xFF555555)else Color(0xFFCCCCCC) // 未选中集数按钮背景
		val info=if(o)Color(0xFF2196F3)else Color(0xFF1565C0) // 日志：信息级别
		val error=if(o)Color(0xFFF44336)else Color(0xFFF44336) // 日志：错误级别
		val warn=if(o)Color(0xFFFF9800)else Color(0xFFFF9800) // 日志：警告级别
		val debug=if(o)Color(0xFFCE93D8)else Color(0xFF6A1B9A) // 日志：调试级别
		val success=if(o)Color(0xFF4CAF50)else Color(0xFF2E7D32) // 日志：成功级别
		val primary=if(o)Color(0xFF66AFFF)else Color(0xFF0066FF) // 主题强调色（选中/按钮）
	}
	// Composable 内获取当前主题色，跟随系统深色模式实时响应
	val cc:CC @Composable get()=CC(isSystemInDarkTheme())

	// 自定义高性能交互指示器节点，适配 2026 最新版 BOM 架构，消除弃用报错
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
					if(jf!=nf||jp!=np){jf=nf;jp=np;invalidateDraw()}
				}
			}
		}
		override fun ContentDrawScope.draw(){
			if(jp)drawRect(p)else if(jf)drawRect(f)
			drawContent()
		}
	}
	// 新版指示器节点工厂
	class Idf(private val f:Color,private val p:Color):IndicationNodeFactory{
		override fun create(s:InteractionSource):DelegatableNode=Idn(s,f,p)
		override fun equals(other:Any?):Boolean=other is Idf&&f==other.f&&p==other.p
		override fun hashCode():Int=31*f.hashCode()+p.hashCode()
	}

	// DataStore 扩展属性，每个 Context 单例 DataStore 实例
	private val Context.ds by preferencesDataStore("fyan")
	// 根据值类型自动推断 Preferences.Key 类型，避免手动指定
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
	// 读取持久化键值，返回 Flow（支持 collectAsState 响应式）
	fun <T> cg(k:String,d:T)=me.ds.data.map{p->k.cype(d)?.let{p[it]}?:d}
	// 写入持久化键值（挂起函数，需在协程中调用）
	suspend fun <T> cs(k:String,v:T)=me.ds.edit{p->k.cype(v)?.let{p[it]=v}}
	// 删除指定持久化键（按 key name 匹配）
	suspend fun cx(k:String)=me.ds.edit{p->p.asMap().keys.firstOrNull{it.name==k}?.let{p.remove(it)}}
	// 同步读取一次当前值（非 Flow，用于初始化场景）
	suspend fun <T> co(k:String,d:T)=cg(k,d).first()

	// 日志条目列表（可观察状态，驱动 UI 刷新）
	private val gs=mutableStateListOf<String>()
	// 日志面板是否折叠
	private var gn by mutableStateOf(false)
	// 日志面板当前拖拽偏移量（向下拖拽折叠）
	private var gy by mutableStateOf(0f)
	// 主线程 Handler，确保日志添加操作在主线程执行
	private val gh=Handler(Looper.getMainLooper())
	// 清空所有日志
	private fun gc()=gs.clear()
	// 删除指定 UUID 前缀开头的日志条目（单条删除）
	private fun gx(i:String)=gs.removeAll{it.startsWith(i)}
	// 添加一条日志：m=模块名，o=内容，c=级别字符（i/e/w/s/d）
	fun log(m:String,o:String,c:Char='i'){
		val t=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date())
		// 格式：{UUID}.{级别}●{时间} {模块} ➜ {内容}
		val v=UUID.randomUUID().toString().replace("-","")+".${c}●$t $m ➜ $o"
		gh.post{gs.add(v)}
	}
	// 日志面板入口：折叠时显示细横条，展开时显示完整面板
	@Composable fun Record(){if(gn||gs.isEmpty())RX()else RO()}
	// 折叠态：显示可点击的细横条，点击恢复展开
	@Composable private fun RX(){Box(modifier=Modifier.fillMaxWidth(0.7f).height(8.dp).padding(bottom=2.dp).clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).clickable{gn=false;gy=0f},contentAlignment=Alignment.Center){}}
	// 展开态：完整日志面板，支持下拉折叠手势
	@Composable private fun RO(){
		val s=rememberLazyListState()
		val bc=Fyan.cc.bd
		// 新日志入队时自动滚动到底部
		LaunchedEffect(gs.size){if(gs.isNotEmpty())s.animateScrollToItem(gs.size-1)}
		Box(modifier=Modifier.fillMaxWidth().heightIn(max=(Fyan.sh/3).dp).navigationBarsPadding()
		// 支持下拉折叠：拖拽超过 40dp 触发折叠，松手自动复位
		.offset(y=gy.roundToInt().dp).pointerInput(Unit){
			detectDragGestures(
				onDragEnd={if(gy>40f){gn=true;gy=0f}else gy=0f},
				onDrag={ch,o->ch.consume();if(gy+o.y>=0f)gy+=o.y}
			)
		}){
			Box(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=4.dp,topEnd=4.dp))
			.background(Fyan.cc.m).drawWithContent{
				drawContent()
				val (w,r)=0.5.dp.toPx() to 4.dp.toPx()
				// 绘制顶部圆角边框线（仅上边+左边+右边，底部不绘制）
				drawPath(
					path=Path().apply{
						moveTo(0f,size.height)
						lineTo(0f,r)
						arcTo(Rect(0f,0f,r*2,r*2),180f,90f,false)
						lineTo(size.width-r,0f)
						arcTo(Rect(size.width-r*2,0f,size.width,r*2),270f,90f,false)
						lineTo(size.width,size.height)
					},color=bc,
					style=Stroke(w,cap=StrokeCap.Round,join=StrokeJoin.Round)
				)
			}){
				Column(modifier=Modifier.fillMaxWidth().padding(4.dp)){
					// 顶部拖拽指示条（TV 模式下可点击折叠）
					Box(modifier=Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){
						Box(modifier=Modifier.fillMaxWidth(0.25f).height(4.dp).clip(RoundedCornerShape(2.dp))
						.background(Fyan.cc.ag).padding(top=2.dp).clickable(enabled=tv){gn=true;gy=0f})
					}
					// 标题行：日志计数 + 清空按钮
					Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
						BasicText("日志 · ${gs.size}条",style=Fyan.ff.ps.copy(color=Fyan.cc.c))
						Box(modifier=Modifier.clickable{gc()}){BasicText("清空",style=Fyan.ff.ps.copy(color=Color(0xFFF44336)))}
					}
					// 日志条目列表
					LazyColumn(state=s,modifier=Modifier.fillMaxWidth()){
						items(gs){o->
							// 解析日志条目：UUID部分 | 级别 | 内容
							val x=o.split("●",limit=2)
							val z=x[0].lastIndexOf('.')
							val id=if(z>0)x[0].substring(0,z)else x[0] // UUID（用于单条删除）
							val cx=if(z>0)x[0].substring(z+1)else"i" // 级别字符
							// 根据级别字符映射对应颜色
							val c=when(cx){"i"->Fyan.cc.info;"e"->Fyan.cc.error;"w"->Fyan.cc.warn;"s"->Fyan.cc.success;"d"->Fyan.cc.debug;else->Fyan.cc.c}
							Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(Fyan.cc.bd))
							Row(modifier=Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){
								BasicText(x.getOrElse(1){"...."},modifier=Modifier.weight(1f).padding(end=4.dp),style=Fyan.ff.gr.copy(color=c))
								// 点击 ╳ 删除该条日志（通过 UUID 前缀精确匹配）
								Box(modifier=Modifier.size(14.dp).clickable{gx(id)},contentAlignment=Alignment.Center){
									BasicText("╳",style=Fyan.ff.ps.copy(color=Fyan.cc.c))
								}
							}
						}
					}
					// 底部留白撑开，使日志内容不被屏幕底部圆角遮挡
					Box(modifier=Modifier.fillMaxWidth().height(10.dp))
				}
			}
		}
	}
}




class O:ComponentActivity(){
	// 下载完成广播接收器，用于监听 APK 下载结束触发安装
	private var br:BroadcastReceiver?=null

	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		Fyan.init(this) // 初始化全局上下文及版本信息
		setContent{
			val c=Fyan.cc
			// 全局注入极简交互背景色工厂，感知主题自动刷新
			CompositionLocalProvider(LocalIndication provides remember(c){Fyan.Idf(c.fc,c.ps)}){
				Fyan.nc=rememberNavController() // 创建并保存全局导航控制器
				var exit by remember{mutableStateOf(false)} // 控制退出确认弹窗显示
				// 拦截返回键：首次按返回弹出退出确认，再次取消
				BackHandler(enabled=!exit){exit=true}
				// 退出确认弹窗
				if(exit)Dialog(onDismissRequest={exit=false}){
					Column(modifier=Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(12.dp)).background(Fyan.cc.cg).border(1.dp,Fyan.cc.bd,RoundedCornerShape(12.dp)).padding(24.dp),
						verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally){
						BasicText("系统提醒",style=Fyan.ff.h3.copy(color=Fyan.cc.c))
						BasicText("确定杀死应用并退出吗？",style=Fyan.ff.pb.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
						Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
							// 取消按钮：关闭弹窗
							Box(modifier=Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp)).background(Fyan.cc.ag).clickable{exit=false},contentAlignment=Alignment.Center){BasicText("取消",style=Fyan.ff.p.copy(color=Fyan.cc.c))}
							// 确定按钮：结束所有 Activity 后延迟 100ms 杀死进程
							Box(modifier=Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp)).background(Fyan.cc.primary).clickable{
								finishAffinity()
								Handler(Looper.getMainLooper()).postDelayed({Process.killProcess(Process.myPid())},100)
							},contentAlignment=Alignment.Center){BasicText("确定",style=Fyan.ff.p.copy(color=Fyan.cc.cg))}
						}
					}
				}
				// 根布局：全屏背景 + 系统栏内边距 + 日志面板固定在底部
				Box(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg).systemBarsPadding(),contentAlignment=Alignment.BottomCenter){
					// 路由导航宿主，startDestination 为爱壹帆首页
					NavHost(navController=Fyan.nc,startDestination="ayf_home"){
						composable("ayf_home"){AyfHome()}
						composable("ayf_history"){AyfHistory()}
						composable("ayf_list/{id}"){x->AyfList(id=x.arguments?.getString("id")?:"")}
						composable("ayf_info/{id}"){x->AyfInfo(id=x.arguments?.getString("id")?:"")}
					}
					Fyan.Record() // 日志悬浮面板（叠加在导航内容之上）
				}
				// 应用启动后异步检查版本更新
				LaunchedEffect(Unit){Fyan.log("系统","检查更新");check()}
			}
		}
	}

	override fun onDestroy(){
		super.onDestroy()
		// 解注册下载广播，防止内存泄漏
		br?.let{unregisterReceiver(it)}
	}

	// 检查 GitHub Releases 最新版本，与当前版本号（数字部分）对比
	private fun check(){
		lifecycleScope.launch(Dispatchers.IO){
			runCatching{
				// 请求 releases/latest 后跟随重定向，从最终 URL 中提取版本号
				val r=OkHttpClient().newCall(Request.Builder().url("https://github.com/lyu2026/fyan/releases/latest").build()).execute()
				val v=r.request.url.toString().substringAfterLast("/")
				// 提取纯数字部分进行大小比较，避免 "v2024.01.01" 格式干扰
				val nv=v.filter(Char::isDigit).toLongOrNull()?:0L
				val cv=Fyan.vn.filter(Char::isDigit).toLongOrNull()?:0L
				if(nv>cv)withContext(Dispatchers.Main){Fyan.log("系统","下载新版本($v)包");upgrade(v)}
			}
		}
	}

	// 使用 DownloadManager 静默后台下载新版 APK
	private fun upgrade(v:String){
		val r=DownloadManager.Request(Uri.parse("https://github.com/lyu2026/fyan/releases/download/$v/fyan.apk")).apply{
			setTitle("Fyan → $v")
			setDescription("后台下载中...")
			setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) // 完成后通知栏提示
			setMimeType("application/vnd.android.package-archive")
			setDestinationInExternalFilesDir(Fyan.me,"Download","fyan_$v.apk") // 存入应用私有外部下载目录
		}
		val m=Fyan.me.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		val id=m.enqueue(r) // 提交下载任务，获取任务 ID
		Toast.makeText(Fyan.me,"检测到新版，已开启后台静默下载",Toast.LENGTH_SHORT).show()
		// 先解注册旧的接收器，防止重复注册
		br?.let{unregisterReceiver(it)}
		br=object:BroadcastReceiver(){
			override fun onReceive(c:Context,i:Intent){
				// 过滤非本次下载任务的广播
				if(i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1L)!=id)return
				val f=File(Fyan.me.getExternalFilesDir("Download"),"fyan_$v.apk")
				if(!f.exists())return // 文件不存在则忽略（下载失败场景）
				// API 24+ 使用 FileProvider 共享文件 URI，低版本直接使用 file:// URI
				val u=if(Build.VERSION.SDK_INT>=24)FileProvider.getUriForFile(Fyan.me,"${Fyan.me.packageName}.fileprovider",f)else Uri.fromFile(f)
				// 启动系统安装器安装 APK
				Fyan.me.startActivity(Intent(Intent.ACTION_VIEW).apply{
					setDataAndType(u,"application/vnd.android.package-archive")
					addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
				})
			}
		}
		// API 33+ 需显式声明 RECEIVER_EXPORTED 标志
		if(Build.VERSION.SDK_INT>=33)registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),Context.RECEIVER_EXPORTED)
		else registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
	}
}
