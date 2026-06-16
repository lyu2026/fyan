package com.fyan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.compose.ui.Alignment
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

class O:androidx.activity.ComponentActivity(){ // 唯一Activity，承载Compose UI树
	private var dr:BroadcastReceiver?=null // 系统下载完成广播接收器，下载APK后触发安装

	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		PR.init(applicationContext);lH() // 初始化SP引用并从磁盘恢复历史记录
		setContent{
			var se by remember{mutableStateOf(false)} // 退出确认对话框显示状态
			BackHandler{se=!se} // 返回键切换退出确认弹窗
			X();if(se)CD(tt="确定杀死应用并退出吗？",od={se=false},oc={finishAffinity()}) // 主导航树 + 退出确认框
			LaunchedEffect(Unit){cu()} // 启动后台静默版本检测
		}
	}

	override fun onDestroy(){super.onDestroy();dr?.let{unregisterReceiver(it)}} // 防内存泄漏：Activity销毁时注销广播

	private fun cu(){ // 静默检测GitHub最新版本，有新版则触发后台下载
		lifecycleScope.launch(Dispatchers.IO){
			runCatching{
				// 复用全局HC，followRedirects已开启，GitHub releases/latest会跳转到具体版本页
				val res=HC.newCall(Request.Builder().url("https://github.com/lyu2026/fyan/releases/latest").build()).execute()
				val ver=res.request.url.toString().substringAfterLast("/") // 从重定向后URL末段提取版本号（如v2026.6.9）
				val vn=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)
					packageManager.getPackageInfo(packageName,android.content.pm.PackageManager.PackageInfoFlags.of(0)).versionName
				else
					@Suppress("DEPRECATION")packageManager.getPackageInfo(packageName,0).versionName
				if(ver.drop(1).filter(Char::isDigit).toLong()>vn.filter(Char::isDigit).toLong()){
					withContext(Dispatchers.Main){ni(ver,"https://github.com/lyu2026/fyan/releases/download/$ver/fyan.apk")}
				}
			}
		}
	}

	private fun ni(v:String,u:String){ // 触发系统DownloadManager下载新版APK并在完成后弹出安装
		val req=android.app.DownloadManager.Request(Uri.parse(u)).apply{
			setTitle("Fyan → $v");setDescription("后台下载中...");setNotificationVisibility(1)
			setMimeType("application/vnd.android.package-archive")
			setDestinationInExternalFilesDir(this@O,android.os.Environment.DIRECTORY_DOWNLOADS,"fyan.apk")
		}
		val dm=getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
		val did=dm.enqueue(req) // 提交下载任务，返回任务ID供广播匹配
		Toast.makeText(this,"检测到新版，已开启后台静默下载",Toast.LENGTH_LONG).show()
		dr=object:BroadcastReceiver(){
			override fun onReceive(c:Context,i:Intent){
				if(i.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID,-1)==did){ // 确认是本次任务完成
					val f=File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),"fyan.apk")
					if(f.exists()){
						val uri=if(Build.VERSION.SDK_INT>=24)FileProvider.getUriForFile(c,"${packageName}.fileprovider",f)else Uri.fromFile(f)
						val ins=Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,"application/vnd.android.package-archive");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)}
						c.startActivity(ins) // 拉起系统安装器
					}
				}
			}
		}
		// Android 13+需要显式声明RECEIVER_EXPORTED权限
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)registerReceiver(dr,IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),RECEIVER_EXPORTED)
		else registerReceiver(dr,IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))
	}
}

@Composable private fun X(){ // 根Composable：NavHost路由树 + 浮层日志面板
	val c=rememberNavController()
	androidx.compose.foundation.layout.Box(modifier="fs psb pnb".css(),contentAlignment=Alignment.BottomCenter){ // 全屏+状态栏+导航栏安全区，日志浮层贴底
		NavHost(navController=c,startDestination="home"){
			composable("home"){HS(c)} // 首页（Tab切换入口）
			composable("history"){HI(c)} // 历史记录页（独立路由，预留）
			composable("filter/{id}"){b:NavBackStackEntry->FS(c,id=b.arguments?.getString("id")?:"movie")} // 分类筛选页
			composable("detail/{id}"){b:NavBackStackEntry->DS(c,id=b.arguments?.getString("id")?:"")} // 视频详情播放页
		}
		FN.LP() // 日志浮层：始终浮于NavHost之上，贴底显示
	}
}