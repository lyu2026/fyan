package com.fyan

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request

class O:androidx.activity.ComponentActivity(){
	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		setContent{
			var se by remember{mutableStateOf(false)}
			BackHandler{se=true}
			if(se)CD(tt="确定杀死应用并退出吗？",od={se=false},oc={finishAffinity();se=false})
			X();LaunchedEffect(Unit){cu()}
		}
	}
	private fun cu(){
		lifecycleScope.launch(Dispatchers.IO){
			runCatching{
				val client=OkHttpClient.Builder().followRedirects(true).build()
				val res=client.newCall(Request.Builder().url("https://github.com/lyu2026/fyan/releases/latest").build()).execute()
				val url=res.request.url.toString()
				val ver=url.substringAfterLast("/")
				if(ver!="v2026.6.8"){
					withContext(Dispatchers.Main){
						// 弹窗提示，点击后下载
						ni("https://github.com/lyu2026/fyan/releases/download/$ver/fyan.apk")
					}
				}
			}
		}
	}
	private fun ni(url:String){
		lifecycleScope.launch(Dispatchers.IO){
			try{
				val request=android.app.DownloadManager.Request(Uri.parse(url)).apply{
					setTitle("fyan 更新下载")
					setDescription("正在下载新版本...")
					if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.Q){
						setDestinationInExternalFilesDir(this@O,android.os.Environment.DIRECTORY_DOWNLOADS,"fyan.apk")
					}else{
						setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS,"fyan.apk")
					}
					setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
					setMimeType("application/vnd.android.package-archive")
				}
				val manager=getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
				manager.enqueue(request)
				withContext(Dispatchers.Main){
					android.widget.Toast.makeText(this@O,"开始下载，完成后请在通知栏点击安装",android.widget.Toast.LENGTH_LONG).show()
				}
			}catch(e:Exception){
				withContext(Dispatchers.Main){
					android.widget.Toast.makeText(this@O,"下载失败: ${e.message}",android.widget.Toast.LENGTH_SHORT).show()
				}
			}
		}
	}
}

@Composable private fun X(){ // NV (NavHostComponent) 应用声明式全局路由骨干网拓扑分发控制中心组件
	val c=androidx.navigation.compose.rememberNavController() // 初始化Compose轻量全局路由核心状态控制器
	val h=androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp/3 // 动态划分设备屏幕视口的三分之一高度
	androidx.compose.foundation.layout.Box(modifier="fs".css()){ // 全景绝对满幅包裹的UI最外层箱盒子根节点
		androidx.navigation.compose.NavHost(navController=c,startDestination="home"){ // 调度构建路由图谱大图解
			composable("home"){HS(c)} // 路由图声明映射首页HS路由物理节点
			composable("history"){HI(c)} // 路由图声明映射独立足迹页HI路由物理节点
			composable("filter/{id}"){bk->FS(c,id=bk.arguments?.getString("id")?:"movie")} // 路由图声明映射附带过滤参数的分类FS检索网格大页
			composable("detail/{id}"){bk->DS(c,id=bk.arguments?.getString("id")?:"")} // 路由图声明映射附带正片ID主键的视频详情DS播放视窗大页
		}
		FN.LP(modifier="fw h<$h ph0.5 pnb".css()) // 贴底且最大高度限制为屏高三分之一的日志悬浮面板定位包裹盒
	}
}
