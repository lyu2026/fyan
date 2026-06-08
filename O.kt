package com.fyan

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class O:androidx.activity.ComponentActivity(){
	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		setContent{
			var se by remember{mutableStateOf(false)}
			BackHandler{se=true}
			if(se)AlertDialog(onDismissRequest={se=false},title={BasicText("退出")},text={BasicText("确定杀死应用并退出吗？")},confirmButton={androidx.compose.material3.TextButton(onClick={finishAffinity()}){BasicText("确定")}},dismissButton={androidx.compose.material3.TextButton(onClick={se=false}){BasicText("取消")}})
			X();LaunchedEffect(Unit){cu()}
		}
	}
	private fun cu(){
		CoroutineScope(Dispatchers.IO).launch{
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
		CoroutineScope(Dispatchers.IO).launch{
			val file=File(cacheDir,"fyan.apk")
			val client=OkHttpClient()
			val res=client.newCall(Request.Builder().url(url).build()).execute()
			file.outputStream().use{it.write(res.body!!.bytes())}
			withContext(Dispatchers.Main){
				val intent=Intent(Intent.ACTION_VIEW)
				intent.setDataAndType(Uri.fromFile(file),"application/vnd.android.package-archive")
				intent.flags=Intent.FLAG_ACTIVITY_NEW_TASK
				startActivity(intent)
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
