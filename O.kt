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
import androidx.core.content.pm.PackageInfoCompat
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
	lateinit var nc:NavHostController
	lateinit var me:Context
	var tv=false
	var vs=0L
	var vn=""

	@Suppress("DEPRECATION") fun init(a:Context){
		me=a.applicationContext
		tv=(me.resources.configuration.uiMode and 15)==4
		val p=me.packageManager.getPackageInfo(me.packageName,0)
		vs=PackageInfoCompat.getLongVersionCode(p)
		vn=p.versionName?:"0"
	}
	fun goto(o:String)=nc.navigate(o)

	val sw:Int get()=me.resources.configuration.screenWidthDp
	val sh:Int get()=me.resources.configuration.screenHeightDp
	val gc:Int get()=if(sw>=840)6 else if(sw>=600)4 else 3

	object ff{
		val h1=TextStyle(fontSize=24.sp,fontWeight=FontWeight.Bold)
		val h2=TextStyle(fontSize=20.sp,fontWeight=FontWeight.Bold)
		val h3=TextStyle(fontSize=18.sp,fontWeight=FontWeight.Bold)
		val h4=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Bold)
		val p=TextStyle(fontSize=14.sp,fontWeight=FontWeight.Normal)
		val ps=TextStyle(fontSize=12.sp,fontWeight=FontWeight.Normal)
		val pb=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Normal)
	}

	class CC(o:Boolean){
		val m=if(o)Color(0x99000000)else Color(0x66000000)
		val bg=if(o)Color(0xFF111111)else Color(0xFFF5F5F5)
		val cg=if(o)Color(0xFF222222)else Color(0xFFFFFFFF)
		val ag=if(o)Color(0xFF333333)else Color(0xFFEEEEEE)
		val c=if(o)Color(0xFFDDDDDD)else Color(0xFF222222)
		val bd=if(o)Color(0xFF444444)else Color(0xFFDDDDDD)
		val fc=if(o)Color(0xFF66AFFF)else Color(0xFF0066FF)
		val hv=if(o)Color(0xFF444444)else Color(0xFFE0E0E0)
		val x=if(o)Color(0xFF555555)else Color(0xFFCCCCCC)
	}
	val cc:CC @Composable get()=CC(isSystemInDarkTheme())

	private val Context.ds by preferencesDataStore("fyan")
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
	fun <T> cg(k:String,d:T)=me.ds.data.map{p->k.cype(d)?.let{p[it]}?:d}
	suspend fun <T> cs(k:String,v:T)=me.ds.edit{p->k.cype(v)?.let{p[it]=v}}
	suspend fun cx(k:String)=me.ds.edit{p->p.asMap().keys.firstOrNull{it.name==k}?.let{p.remove(it)}}
	suspend fun <T> co(k:String,d:T)=cg(k,d).first()

	private val gs=mutableStateListOf<String>()
	private var gn by mutableStateOf(false)
	private var gy by mutableStateOf(0f)
	private val gh=Handler(Looper.getMainLooper())
	private fun gc()=gs.clear()
	private fun gx(i:String)=gs.removeAll{it.startsWith(i)}
	fun log(m:String,o:String,c:Char='i'){
		val t=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date())
		val x=when(c){'i'->"#2196F3";'u'->"#9C27B0";'e'->"#F44336";'s'->"#00BCD4";'n'->"#4CAF50";'w'->"#FF9800";else->"#9E9E9E"}
		val v=UUID.randomUUID().toString().replace("-","")+".$x●$t $m ➜ $o"
		gh.post{gs.add(v)}
	}
	@Composable fun Record(){if(gn||gs.isEmpty())RX()else RO()}
	@Composable private fun RX(){Box(modifier=Modifier.fillMaxWidth(0.7f).height(5.dp).navigationBarsPadding().padding(bottom=2.dp).clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).clickable{gn=false;gy=0f},contentAlignment=Alignment.Center){}}
	@Composable private fun RO(){
		val s=rememberLazyListState()
		LaunchedEffect(gs.size){if(gs.isNotEmpty())s.animateScrollToItem(gs.size-1)}
		Box(modifier=Modifier.fillMaxWidth().heightIn(max=(Fyan.sh/3).dp).navigationBarsPadding()
			.offset(y=gy.roundToInt().dp).pointerInput(Unit){
				detectDragGestures(
					onDragEnd={if(gy>40f){gn=true;gy=0f}else gy=0f},
					onDrag={ch,o->ch.consume();if(gy+o.y>=0f)gy+=o.y}
				)
		}){
			Box(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=2.dp,topEnd=2.dp))
			.border(1.dp,Fyan.cc.bd,RoundedCornerShape(topStart=2.dp,topEnd=2.dp)).background(Fyan.cc.m).offset(y=(-1.2).dp)){
				Column(modifier=Modifier.fillMaxWidth().padding(4.dp)){
					Box(modifier=Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){
						Box(modifier=Modifier.fillMaxWidth(0.25f).height(4.dp).clip(RoundedCornerShape(2.dp))
						.background(Fyan.cc.bg).clickable(enabled=tv){gn=true;gy=0f})
					}
					Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
						BasicText("日志 · ${gs.size}条",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.7f),fontFamily=FontFamily.Monospace))
						Box(modifier=Modifier.padding(3.dp).clickable{gc()}){BasicText("清空",style=Fyan.ff.ps.copy(color=Color(0xFFF44336)))}
					}
					LazyColumn(state=s,modifier=Modifier.fillMaxWidth()){
						items(gs){o->
							val x=o.split("●",limit=2)
							val z=x[0].lastIndexOf('.')
							val id=if(z>0)x[0].substring(0,z)else x[0]
							val cx=if(z>0)x[0].substring(z+1)else"#9E9E9E"
							val c=try{Color(android.graphics.Color.parseColor(cx))}catch(_:Exception){Color(0xFF9E9E9E)}
							Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(Fyan.cc.bd))
							Row(modifier=Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){
								BasicText(x.getOrElse(1){"...."},modifier=Modifier.weight(1f).padding(end=4.dp),style=Fyan.ff.ps.copy(color=c,lineHeight=1.2.em,fontFamily=FontFamily.Monospace))
								Box(modifier=Modifier.size(16.dp).clickable{gx(id)},contentAlignment=Alignment.Center){
									BasicText("✕",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.7f)))
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
	private var br:BroadcastReceiver?=null

	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		Fyan.init(this)
		setContent{
			Fyan.nc=rememberNavController()
			var exit by remember{mutableStateOf(false)}
			BackHandler(enabled=!exit){exit=true}
			if(exit)Dialog(onDismissRequest={exit=false}){
				Column(modifier=Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(12.dp)).background(Fyan.cc.cg).border(1.dp,Fyan.cc.bd,RoundedCornerShape(12.dp)).padding(24.dp),
					verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally){
					BasicText("系统提醒",style=Fyan.ff.h3.copy(color=Fyan.cc.c))
					BasicText("确定杀死应用并退出吗？",style=Fyan.ff.pb.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
					Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
						Box(modifier=Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp)).background(Fyan.cc.ag).clickable{exit=false},contentAlignment=Alignment.Center){BasicText("取消",style=Fyan.ff.p.copy(color=Fyan.cc.c))}
						Box(modifier=Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(6.dp)).background(Fyan.cc.fc).clickable{
							finishAffinity()
							Handler(Looper.getMainLooper()).postDelayed({Process.killProcess(Process.myPid())},100)
						},contentAlignment=Alignment.Center){BasicText("确定",style=Fyan.ff.p.copy(color=Fyan.cc.cg))}
					}
				}
			}
			Box(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg).systemBarsPadding(),contentAlignment=Alignment.BottomCenter){
				NavHost(navController=Fyan.nc,startDestination="ayf_home"){
					composable("ayf_home"){Fyan.log("路由","进入爱壹帆首页");AyfHome()}
					composable("ayf_history"){Fyan.log("路由","进入爱壹帆历史记录页");AyfHistory()}
					composable("ayf_list/{id}"){x->{Fyan.log("路由","进入爱壹帆筛选列表页");AyfList(id=x.arguments?.getString("id")?:"")}}
					composable("ayf_info/{id}"){x->{Fyan.log("路由","进入爱壹帆视频详情页");AyfInfo(id=x.arguments?.getString("id")?:"")}}
				}
				Fyan.Record()
			}
			LaunchedEffect(Unit){Fyan.log("系统","检查更新");check()}
		}
	}

	override fun onDestroy(){
		super.onDestroy()
		br?.let{unregisterReceiver(it)}
	}

	private fun check(){
		lifecycleScope.launch(Dispatchers.IO){
			runCatching{
				val r=OkHttpClient().newCall(Request.Builder().url("https://github.com/lyu2026/fyan/releases/latest").build()).execute()
				val v=r.request.url.toString().substringAfterLast("/")
				val nv=v.filter(Char::isDigit).toLongOrNull()?:0L
				val cv=Fyan.vn.filter(Char::isDigit).toLongOrNull()?:0L
				if(nv>cv)withContext(Dispatchers.Main){Fyan.log("系统","下载新版本($v)包");upgrade(v)}
			}
		}
	}

	private fun upgrade(v:String){
		val r=DownloadManager.Request(Uri.parse("https://github.com/lyu2026/fyan/releases/download/$v/fyan.apk")).apply{
			setTitle("Fyan → $v")
			setDescription("后台下载中...")
			setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
			setMimeType("application/vnd.android.package-archive")
			setDestinationInExternalFilesDir(Fyan.me,"Download","fyan_$v.apk")
		}
		val m=Fyan.me.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		val id=m.enqueue(r)
		Toast.makeText(Fyan.me,"检测到新版，已开启后台静默下载",Toast.LENGTH_SHORT).show()
		br?.let{unregisterReceiver(it)}
		br=object:BroadcastReceiver(){
			override fun onReceive(c:Context,i:Intent){
				if(i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1L)!=id)return
				val f=File(Fyan.me.getExternalFilesDir("Download"),"fyan_$v.apk")
				if(!f.exists())return
				val u=if(Build.VERSION.SDK_INT>=24)FileProvider.getUriForFile(Fyan.me,"${Fyan.me.packageName}.fileprovider",f)else Uri.fromFile(f)
				Fyan.me.startActivity(Intent(Intent.ACTION_VIEW).apply{
					setDataAndType(u,"application/vnd.android.package-archive")
					addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
				})
			}
		}
		if(Build.VERSION.SDK_INT>=33)registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),Context.RECEIVER_EXPORTED)
		else registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
	}
}