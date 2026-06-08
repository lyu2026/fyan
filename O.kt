package com.fyan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class O:androidx.activity.ComponentActivity(){
	private var dr:BroadcastReceiver?=null // 托管系统下载完毕动作监听器
	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		PR.init(applicationContext);lH() // 进场第一时间夺取SP环境上下文并反序列化历史记录
		setContent{
			var se by remember{mutableStateOf(false)}
			BackHandler{se=true}
			if(se)CD(tt="确定杀死应用并退出吗？",od={se=false},oc={finishAffinity()})
			X();LaunchedEffect(Unit){cu()}
		}
	}
	override fun onDestroy(){super.onDestroy();dr?.let{unregisterReceiver(it)}} // 防泄漏注销广播
	private fun cu(){
		lifecycleScope.launch(Dispatchers.IO){
			runCatching{
				val client=OkHttpClient.Builder().followRedirects(true).build()
				val res=client.newCall(Request.Builder().url("https://github.com/lyu2026/fyan/releases/latest").build()).execute()
				val ver=res.request.url.toString().substringAfterLast("/")
				val cv="v"+packageManager.getPackageInfo(packageName,0).versionName // 动态拉取当前物理包体 versionName，拒绝硬编码
				if(ver.startsWith("v")&&ver!=cv){
					withContext(Dispatchers.Main){ni("https://github.com/lyu2026/fyan/releases/download/$ver/fyan.apk")}
				}
			}
		}
	}
	private fun ni(url:String){
		val req=android.app.DownloadManager.Request(Uri.parse(url)).apply{
			setTitle("fyan 更新");setDescription("后台下载中...");setNotificationVisibility(1)
			setMimeType("application/vnd.android.package-archive")
			setDestinationInExternalFilesDir(this@O,android.os.Environment.DIRECTORY_DOWNLOADS,"fyan.apk")
		}
		val dm=getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
		val did=dm.enqueue(req) // 发射下载任务并获取唯一流水ID
		Toast.makeText(this,"检测到新版，已开启后台静默下载",Toast.LENGTH_LONG).show()
		
		dr=object:BroadcastReceiver(){ // 动态构建安装意图截获器
			override fun onReceive(c:Context,i:Intent){
				if(i.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID,-1)==did){
					val f=File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),"fyan.apk")
					if(f.exists()){ // 校验文件触达
						val uri=if(Build.VERSION.SDK_INT>=24) FileProvider.getUriForFile(c,"${packageName}.fileprovider",f) else Uri.fromFile(f)
						val int=Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,"application/vnd.android.package-archive");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)}
						c.startActivity(int) // 暴力拉起系统级包安装界面的 UI 视图
					}
				}
			}
		}
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU) registerReceiver(dr,IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),RECEIVER_EXPORTED)
		else registerReceiver(dr,IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))
	}
}

@Composable private fun X(){
	val c=rememberNavController()
	val h=androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp/3
	androidx.compose.foundation.layout.Box(modifier="fs".css()){
		NavHost(navController=c,startDestination="home"){
			composable("home"){HS(c)}
			composable("history"){HI(c)}
			composable("filter/{id}"){b:NavBackStackEntry->FS(c,id=b.arguments?.getString("id")?:"movie")}
			composable("detail/{id}"){b:NavBackStackEntry->DS(c,id=b.arguments?.getString("id")?:"")}
		}
		FN.LP(modifier="fw h<$h ph0.5 pnb".css())
	}
}
