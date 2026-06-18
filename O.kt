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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
		val gr=TextStyle(fontSize=8.sp,lineHeight=1.1.em,fontWeight=FontWeight.Normal,fontFamily=FontFamily.Monospace)
	}

	class CC(o:Boolean){
		val m=if(o)Color(0xAA000000)else Color(0xAAFFFFFF)
		val bg=if(o)Color(0xFF000000)else Color(0xFFFFFFFF)
		val cg=if(o)Color(0xFF222222)else Color(0xFFDDDDDD)
		val ag=if(o)Color(0xFF333333)else Color(0xFFEEEEEE)
		val c=if(o)Color(0xFFFFFFFF)else Color(0xFF000000)
		val bd=if(o)Color(0xFF444444)else Color(0xFFDDDDDD)
		val fc=if(o)Color(0xFF66AFFF)else Color(0xFF0066FF)
		val hv=if(o)Color(0xFF444444)else Color(0xFFE0E0E0)
		val x=if(o)Color(0xFF555555)else Color(0xFFCCCCCC)
		val info=if(o)Color(0xFF2196F3)else Color(0xFF1565C0)
		val error=if(o)Color(0xFFF44336)else Color(0xFFF44336)
		val warn=if(o)Color(0xFFFF9800)else Color(0xFFFF9800)
		val debug=if(o)Color(0xFFCE93D8)else Color(0xFF6A1B9A)
		val success=if(o)Color(0xFF4CAF50)else Color(0xFF2E7D32)
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
		val v=UUID.randomUUID().toString().replace("-","")+".${c}●$t $m ➜ $o"
		gh.post{gs.add(v)}
	}
	@Composable fun Record(){if(gn||gs.isEmpty())RX()else RO()}
	@Composable private fun RX(){Box(modifier=Modifier.fillMaxWidth(0.7f).height(6.dp).navigationBarsPadding().padding(bottom=4.dp).clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).clickable{gn=false;gy=0f},contentAlignment=Alignment.Center){}}
	@Composable private fun RO(){
		val s=rememberLazyListState()
		val bc=Fyan.cc.bd
		LaunchedEffect(gs.size){if(gs.isNotEmpty())s.animateScrollToItem(gs.size-1)}
		Box(modifier=Modifier.fillMaxWidth().heightIn(max=(Fyan.sh/3).dp).navigationBarsPadding()
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
				drawPath(
					path=Path().apply{
						moveTo(0f,size.height);lineTo(0f,r)
						arcTo(Rect(0f,0f,r*2,r*2),180f,-90f,false)
						lineTo(size.width-r,0f)
						arcTo(Rect(size.width-r*2,0f,size.width,r*2),90f,-90f,false)
						lineTo(size.width,size.height)
					},
					color=bc,style=Stroke(w,cap=StrokeCap.Round,join=StrokeJoin.Round)
				)
			}){
				Column(modifier=Modifier.fillMaxWidth().padding(4.dp)){
					Box(modifier=Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){
						Box(modifier=Modifier.fillMaxWidth(0.25f).height(4.dp).clip(RoundedCornerShape(2.dp))
						.background(Fyan.cc.ag).padding(top=2.dp).clickable(enabled=tv){gn=true;gy=0f})
					}
					Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
						BasicText("日志 · ${gs.size}条",style=Fyan.ff.ps.copy(color=Fyan.cc.c))
						Box(modifier=Modifier.padding(3.dp).clickable{gc()}){BasicText("清空",style=Fyan.ff.ps.copy(color=Color(0xFFF44336)))}
					}
					LazyColumn(state=s,modifier=Modifier.fillMaxWidth()){
						items(gs){o->
							val x=o.split("●",limit=2)
							val z=x[0].lastIndexOf('.')
							val id=if(z>0)x[0].substring(0,z)else x[0]
							val cx=if(z>0)x[0].substring(z+1)else"i"
							val c=when(cx){"i"->Fyan.cc.info;"e"->Fyan.cc.error;"w"->Fyan.cc.warn;"s"->Fyan.cc.success;"d"->Fyan.cc.debug;else->Fyan.cc.c}
							Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(Fyan.cc.bd))
							Row(modifier=Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){
								BasicText(x.getOrElse(1){"...."},modifier=Modifier.weight(1f).padding(end=4.dp),style=Fyan.ff.gr.copy(color=c))
								Box(modifier=Modifier.size(14.dp).clickable{gx(id)},contentAlignment=Alignment.Center){
									BasicText("╳",style=Fyan.ff.ps.copy(color=Fyan.cc.c))
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
					composable("ayf_home"){AyfHome()}
					composable("ayf_history"){AyfHistory()}
					composable("ayf_list/{id}"){x->AyfList(id=x.arguments?.getString("id")?:"")}
					composable("ayf_info/{id}"){x->AyfInfo(id=x.arguments?.getString("id")?:"")}
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