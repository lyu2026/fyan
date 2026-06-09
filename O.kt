package com.fyan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

class O:androidx.activity.ComponentActivity(){
	private var dr:BroadcastReceiver?=null // 下载完成广播接收器

	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		PR.init(applicationContext);lH() // 初始化SP并恢复历史记录
		setContent{X(onExit={finishAffinity()})} // X来自P.kt，传入退出回调
		lifecycleScope.launch{cu()} // 启动后台版本检测
	}

	override fun onDestroy(){super.onDestroy();dr?.let{unregisterReceiver(it)}} // 防泄漏：注销广播

	private suspend fun cu(){ // 静默检测GitHub最新版本
		withContext(Dispatchers.IO){
			runCatching{
				val res=HC.newCall(Request.Builder().url("https://github.com/lyu2026/fyan/releases/latest").build()).execute()
				val ver=res.request.url.toString().substringAfterLast("/") // 从重定向URL末段提取版本号
				val vn=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)
					packageManager.getPackageInfo(packageName,android.content.pm.PackageManager.PackageInfoFlags.of(0)).versionName
				else @Suppress("DEPRECATION")packageManager.getPackageInfo(packageName,0).versionName
				val cv="v"+vn
				if(ver.startsWith("v")&&ver!=cv)withContext(Dispatchers.Main){ni(ver,"https://github.com/lyu2026/fyan/releases/download/$ver/fyan.apk")}
			}
		}
	}

	private fun ni(v:String,u:String){ // 触发DownloadManager后台下载新APK
		val req=android.app.DownloadManager.Request(Uri.parse(u)).apply{
			setTitle("Fyan → $v");setDescription("后台下载中...");setNotificationVisibility(1)
			setMimeType("application/vnd.android.package-archive")
			setDestinationInExternalFilesDir(this@O,android.os.Environment.DIRECTORY_DOWNLOADS,"fyan.apk")
		}
		val dm=getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
		val did=dm.enqueue(req) // 提交下载任务，返回id供广播匹配
		Toast.makeText(this,"检测到新版，已开启后台静默下载",Toast.LENGTH_LONG).show()
		dr=object:BroadcastReceiver(){
			override fun onReceive(c:Context,i:Intent){
				if(i.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID,-1)==did){
					val f=File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),"fyan.apk")
					if(f.exists()){
						val uri=if(Build.VERSION.SDK_INT>=24)FileProvider.getUriForFile(c,"${packageName}.fileprovider",f)else Uri.fromFile(f)
						c.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,"application/vnd.android.package-archive");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)})
					}
				}
			}
		}
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)registerReceiver(dr,IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),RECEIVER_EXPORTED)
		else registerReceiver(dr,IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))
	}
}