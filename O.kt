package com.fyan

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request

class O:androidx.activity.ComponentActivity(){
	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		setContent{
			var se by remember{mutableStateOf(false)}
			BackHandler{se=true}
			if(se)CD(tt="确定杀死应用并退出吗？",od={se=false},oc={finishAffinity()})
			X();LaunchedEffect(Unit){cu()}
		}
	}
	private fun cu(){
		lifecycleScope.launch(Dispatchers.IO){
			runCatching{
				val client=OkHttpClient.Builder().followRedirects(true).build()
				val res=client.newCall(Request.Builder().url("https://github.com/lyu2026/fyan/releases/latest").build()).execute()
				val ver=res.request.url.toString().substringAfterLast("/")
				if(ver!="v2026.6.8"){
					withContext(Dispatchers.Main){ni("https://github.com/lyu2026/fyan/releases/download/$ver/fyan.apk")}
				}
			}
		}
	}
	private fun ni(url:String){
		val req=android.app.DownloadManager.Request(Uri.parse(url)).apply{
			setTitle("fyan 更新");setDescription("下载中...");setNotificationVisibility(1)
			setMimeType("application/vnd.android.package-archive")
			setDestinationInExternalFilesDir(this@O,android.os.Environment.DIRECTORY_DOWNLOADS,"fyan.apk")
		}
		(getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager).enqueue(req)
		Toast.makeText(this,"开始后台下载",Toast.LENGTH_LONG).show()
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
