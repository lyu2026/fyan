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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
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

val HC=OkHttpClient() // 全局复用HTTP客户端单例，避免重复创建

object Fyan{ // 全局数据与工具集
	lateinit var nc:NavHostController // 全局路由控制器（Compose域内初始化）
	lateinit var me:Context // 应用级Context，生命周期超越Activity
	var tv=false // 当前设备是否为TV/大屏模式
	var vs=0L // 应用版本号(数字，用于比较)
	var vn="" // 应用版本名(字符串，用于显示)

	@Suppress("DEPRECATION") fun init(a:Context){ // 初始化全局基础参数与版本信息
		me=a.applicationContext
		tv=(me.resources.configuration.uiMode and 15)==4 // UI_MODE_TYPE_MASK=0xF，TV=4
		val p=me.packageManager.getPackageInfo(me.packageName,0)
		vs=androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(p)
		vn=p.versionName?:"0"
	}
	fun goto(o:String)=nc.navigate(o) // 触发页面路由跳转

	// 屏幕尺寸（实时从配置读取，响应横竖屏切换）
	val sw:Int get()=me.resources.configuration.screenWidthDp // 屏宽dp
	val sh:Int get()=me.resources.configuration.screenHeightDp // 屏高dp
	// 自适应网格列数（840宽屏=6列，600平板=4列，手机=3列）
	val gc:Int get()=if(sw>=840)6 else if(sw>=600)4 else 3

	object ff{ // HTML标签风格字体样式集
		val h1=TextStyle(fontSize=24.sp,fontWeight=FontWeight.Bold)
		val h2=TextStyle(fontSize=20.sp,fontWeight=FontWeight.Bold)
		val h3=TextStyle(fontSize=18.sp,fontWeight=FontWeight.Bold)
		val h4=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Bold)
		val hi1=TextStyle(fontSize=24.sp,fontWeight=FontWeight.Bold,fontStyle=FontStyle.Italic)
		val hi2=TextStyle(fontSize=20.sp,fontWeight=FontWeight.Bold,fontStyle=FontStyle.Italic)
		val hi3=TextStyle(fontSize=18.sp,fontWeight=FontWeight.Bold,fontStyle=FontStyle.Italic)
		val hi4=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Bold,fontStyle=FontStyle.Italic)
		val p=TextStyle(fontSize=14.sp,fontWeight=FontWeight.Normal)
		val pi=TextStyle(fontSize=14.sp,fontWeight=FontWeight.Normal,fontStyle=FontStyle.Italic)
		val p12=TextStyle(fontSize=12.sp,fontWeight=FontWeight.Normal)
		val pi12=TextStyle(fontSize=12.sp,fontWeight=FontWeight.Normal,fontStyle=FontStyle.Italic)
		val p16=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Normal)
		val pi16=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Normal,fontStyle=FontStyle.Italic)
	}

	class CC(d:Boolean){ // 动态主题颜色集（深/浅模式自动切换）
		val m=if(d)Color(0x99000000)else Color(0x66000000) // 蒙版透明背景色
		val bg=if(d)Color(0xFF111111)else Color(0xFFF5F5F5) // 界面最底层背景色
		val cg=if(d)Color(0xFF222222)else Color(0xFFFFFFFF) // 第二层卡片/面板背景色
		val ag=if(d)Color(0xFF333333)else Color(0xFFEEEEEE) // 第三层区域背景色
		val c=if(d)Color(0xFFDDDDDD)else Color(0xFF222222) // 主文本色
		val bd=if(d)Color(0xFF444444)else Color(0xFFDDDDDD) // 边框色
		val fc=if(d)Color(0xFF66AFFF)else Color(0xFF0066FF) // 主题强调色（选中/获焦）
		val hv=if(d)Color(0xFF444444)else Color(0xFFE0E0E0) // 轻触反馈色
		val x=if(d)Color(0xFF555555)else Color(0xFFCCCCCC) // 禁用/不可用背景色
	}
	val cc:CC @Composable get()=CC(isSystemInDarkTheme()) // Composable内调用，自动感知深浅模式

	// DataStore持久化缓存（应用私有，key-value结构）
	private val Context.ds by preferencesDataStore("fyan")
	@Suppress("UNCHECKED_CAST") private fun <T> String.cype(v:T)=(when(v){ // 根据值类型推断DataStore Key
		is Boolean->booleanPreferencesKey(this)
		is String->stringPreferencesKey(this)
		is Int->intPreferencesKey(this)
		is Long->longPreferencesKey(this)
		is Float->floatPreferencesKey(this)
		is Double->doublePreferencesKey(this)
		is ByteArray->byteArrayPreferencesKey(this)
		is Set<*>->stringSetPreferencesKey(this)
		else->null
	}as?Preferences.Key<T>)
	fun <T> cg(k:String,d:T)=me.ds.data.map{p->k.cype(d)?.let{p[it]}?:d} // 返回Flow供UI响应式订阅
	suspend fun <T> cs(k:String,v:T)=me.ds.edit{p->k.cype(v)?.let{p[it]=v}} // 协程挂起写入
	suspend fun cx(k:String)=me.ds.edit{p->p.asMap().keys.firstOrNull{it.name==k}?.let{p.remove(it)}} // 按key名删除条目
	suspend fun <T> co(k:String,d:T)=cg(k,d).first() // 挂起单次读取（不长期订阅）

	// 运行日志系统，条目格式：{uuid}.{colorHex}●{HH:mm:ss} {模块} ➜ {内容}
	private val logs=mutableStateListOf<String>()
	private var logf by mutableStateOf(false) // 日志面板是否折叠（true=折叠为横条）
	private var logy by mutableStateOf(0f) // 面板向下拖拽偏移量（px）
	private val MH=Handler(Looper.getMainLooper()) // 主线程Handler单例，保证State更新线程安全
	private fun logc()=logs.clear() // 清空全部日志
	private fun logx(i:String)=logs.removeAll{it.startsWith(i)} // 按uuid前缀删除单条日志
	fun log(m:String,o:String,c:Char='i'){ // 写入日志（m=模块 o=内容 c=级别：i信息 u用户 e错误 s系统 n成功 w警告）
		val t=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date())
		val x=when(c){'i'->"#2196F3";'u'->"#9C27B0";'e'->"#F44336";'s'->"#00BCD4";'n'->"#4CAF50";'w'->"#FF9800";else->"#9E9E9E"} // 级别颜色映射
		val v=UUID.randomUUID().toString().replace("-","")+".$x●$t $m ➜ $o" // uuid用于删除定位，颜色编码跟在点后
		MH.post{logs.add(v)} // 切主线程追加，避免跨线程State写入崩溃
	}
	@Composable fun Record(){if(logf)RecordX()else RecordO()} // 日志面板入口：折叠→横条，展开→完整面板
	@Composable private fun RecordX(){ // 折叠态：70%宽横条居中，点击恢复展开
		Box(Modifier.fillMaxWidth(0.7f).height(6.dp).navigationBarsPadding().background(Color(0x80808080)).clickable{logf=false;logy=0f},Alignment.Center){}
	}
	@Composable private fun RecordO(){ // 展开态：占屏高1/3，支持向下拖拽折叠
		val ls=rememberLazyListState() // 列表滚动状态
		LaunchedEffect(logs.size){if(logs.isNotEmpty())ls.animateScrollToItem(logs.size-1)} // 新日志自动滚底
		Box(Modifier.fillMaxWidth().heightIn(max=(Fyan.sh/3).dp).navigationBarsPadding() // 最大高度=屏高1/3，消费导航栏高度
			.offset(y=logy.roundToInt().dp)
			.pointerInput(Unit){detectDragGestures( // 手势拖拽：只允许向下拖，超100px折叠
				onDragEnd={if(logy>100f){logf=true;logy=0f}else logy=0f},
				onDrag={ch,d->ch.consume();if(logy+d.y>=0f)logy+=d.y}
			)}){
			Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=6.dp,topEnd=6.dp)) // 上圆角+描边+半透明深色背景
				.border(1.dp,Color(0x80808080),RoundedCornerShape(topStart=6.dp,topEnd=6.dp))
				.background(Color(0xE0222222))){
				Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp)){
					Box(Modifier.fillMaxWidth(),Alignment.Center){ // 顶部拖拽把手指示条
						Box(Modifier.fillMaxWidth(0.25f).height(3.dp).clip(RoundedCornerShape(2.dp)) // TV端点击折叠，手机端拖拽折叠
							.background(Color(0x66808080)).clickable(enabled=tv){logf=true;logy=0f})
					}
					Row(Modifier.fillMaxWidth().padding(vertical=8.dp),Arrangement.SpaceBetween,Alignment.CenterVertically){ // 标题栏：日志条数+清空按钮
						BasicText("日志 · ${logs.size}条",style=TextStyle(color=Color(0xFF9E9E9E),fontFamily=FontFamily.Monospace,fontSize=12.sp))
						Box(Modifier.padding(8.dp).clickable{logc()}){BasicText("清空",style=TextStyle(color=Color(0xFFF44336),fontSize=12.sp))}
					}
					LazyColumn(state=ls,Modifier.fillMaxWidth()){ // 日志条目列表
						items(logs){ey->
							val pt=ey.split("●",limit=2) // 按●分割为：[uuid.color前缀, 正文]
							val dt=pt[0].lastIndexOf('.')
							val id=if(dt>0)pt[0].substring(0,dt)else pt[0] // uuid（删除用）
							val hx=if(dt>0)pt[0].substring(dt+1)else"#9E9E9E" // 颜色hex
							val ec=try{Color(android.graphics.Color.parseColor(hx))}catch(_:Exception){Color(0xFF9E9E9E)} // 解析失败兜底灰色
							Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0x1A808080))) // 分隔线
							Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
								BasicText(pt.getOrElse(1){""},Modifier.weight(1f).padding(end=4.dp),
									style=TextStyle(color=ec,lineHeight=1.4.em,fontFamily=FontFamily.Monospace,fontSize=11.sp))
								Box(Modifier.size(18.dp).clickable{logx(id)},Alignment.Center){ // 单条删除按钮
									BasicText("✕",style=TextStyle(color=Color(0xFF9E9E9E),fontSize=10.sp))
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
	private var br:BroadcastReceiver?=null // 系统下载完成广播接收器

	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		Fyan.init(this) // 优先初始化全局工具类（me/tv/vs/vn）
		setContent{
			Fyan.nc=rememberNavController() // Compose域内绑定路由实例
			var exit by remember{mutableStateOf(false)} // 退出确认弹窗显示状态
			BackHandler(enabled=!exit){exit=true} // 未弹窗时拦截返回键显示弹窗
			if(exit)Dialog(onDismissRequest={exit=false}){
				Column(Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(12.dp))
					.background(Fyan.cc.cg).border(1.dp,Fyan.cc.bd,RoundedCornerShape(12.dp)).padding(24.dp),
					Arrangement.spacedBy(12.dp),Alignment.CenterHorizontally){
					BasicText("提示",style=Fyan.ff.h3.copy(color=Fyan.cc.c))
					BasicText("确定杀死应用并退出吗？",style=Fyan.ff.p16.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
					Row(Modifier.fillMaxWidth(),Arrangement.spacedBy(12.dp)){
						Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp)).background(Fyan.cc.ag).clickable{exit=false},Alignment.Center){BasicText("取消",style=Fyan.ff.p.copy(color=Fyan.cc.c))}
						Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp)).background(Fyan.cc.fc).clickable{ // 确认退出：清栈+延迟杀进程
							finishAffinity()
							Handler(Looper.getMainLooper()).postDelayed({Process.killProcess(Process.myPid())},100)
						},Alignment.Center){BasicText("确定",style=Fyan.ff.p.copy(color=Fyan.cc.cg))}
					}
				}
			}
			Box(Modifier.fillMaxSize().background(Fyan.cc.bg).systemBarsPadding(),Alignment.BottomCenter){ // 全局根节点：底层背景+系统栏避让
				NavHost(Fyan.nc,startDestination="ayf_home"){ // 全局路由注册中心
					composable("ayf_home"){AyfHome()}
					composable("ayf_history"){AyfHistory()}
					composable("ayf_list/{id}"){x->AyfList(id=x.arguments?.getString("id")?:"")} // 动态提取分类id
					composable("ayf_play/{id}"){x->AyfInfo(id=x.arguments?.getString("id")?:"")} // 动态提取视频id
				}
				Fyan.Record() // 悬浮于NavHost顶层的全局日志面板
			}
			LaunchedEffect(Unit){check()} // 启动后异步检查版本更新
		}
	}

	override fun onDestroy(){
		super.onDestroy()
		br?.let{unregisterReceiver(it)} // 销毁时注销广播防内存泄漏
	}

	private fun check(){ // 请求GitHub最新Release，通过重定向URL提取版本号
		lifecycleScope.launch(Dispatchers.IO){
			runCatching{
				val r=HC.newCall(Request.Builder().url("https://github.com/lyu2026/fyan/releases/latest").build()).execute()
				val v=r.request.url.toString().substringAfterLast("/") // 重定向后URL末尾即版本号
				val nv=v.filter(Char::isDigit).toLongOrNull()?:0L // 过滤非数字后比较
				val cv=Fyan.vn.filter(Char::isDigit).toLongOrNull()?:0L
				if(nv>cv)withContext(Dispatchers.Main){upgrade(v)} // 有新版则切主线程触发下载
			}
		}
	}

	private fun upgrade(v:String){ // 调用系统DownloadManager静默下载新APK
		val r=DownloadManager.Request(Uri.parse("https://github.com/lyu2026/fyan/releases/download/$v/fyan.apk")).apply{
			setTitle("Fyan → $v") // 通知栏标题
			setDescription("后台下载中...") // 通知栏描述
			setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) // 下载中及完成后显示通知
			setMimeType("application/vnd.android.package-archive") // APK标准MIME类型
			setDestinationInExternalFilesDir(Fyan.me,"Download","fyan_$v.apk") // 存至应用私有下载目录
		}
		val m=Fyan.me.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		val id=m.enqueue(r) // 提交任务并记录任务ID（用于广播匹配）
		Toast.makeText(Fyan.me,"检测到新版，已开启后台静默下载",Toast.LENGTH_SHORT).show()
		br?.let{unregisterReceiver(it)} // 先解绑旧广播防泄漏
		br=object:BroadcastReceiver(){
			override fun onReceive(c:Context,i:Intent){
				if(i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1L)!=id)return // 过滤非本次任务
				val f=File(Fyan.me.getExternalFilesDir("Download"),"fyan_$v.apk")
				if(!f.exists())return // 文件未落地则不处理
				// Android 7+用FileProvider临时授权URI，低版本直接fromFile
				val u=if(Build.VERSION.SDK_INT>=24)FileProvider.getUriForFile(Fyan.me,"${Fyan.me.packageName}.fileprovider",f)else Uri.fromFile(f)
				Fyan.me.startActivity(Intent(Intent.ACTION_VIEW).apply{
					setDataAndType(u,"application/vnd.android.package-archive")
					addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) // 临时读权限+新任务栈
				})
			}
		}
		// Android 13+需显式声明RECEIVER_EXPORTED
		if(Build.VERSION.SDK_INT>=33)registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),Context.RECEIVER_EXPORTED)
		else registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
	}
}