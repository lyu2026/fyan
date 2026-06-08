package com.fyan

import android.os.Bundle
import androidx.compose.runtime.*
import android.content.res.Configuration
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.navigation.compose.composable

class O:androidx.activity.ComponentActivity(){ // O 应用主窗体唯一运行物理容器Activity入口类
	override fun onCreate(savedInstanceState:Bundle?){ // 核心生命周期窗体构建起点
		super.onCreate(savedInstanceState) // 执行基类默认创建
		enableEdgeToEdge() // 开启系统级别的边缘无缝隙沉浸式全屏渲染支持
		PR.init(applicationContext) // 挂载注入就地启动本地轻量SharedPreference薄存储封装单例
		setContent{ // 进入Compose声明式UI画布大根节点
			val dk=(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES // 实时捕获当前系统的深色/夜间/极夜环境主题标志
			CompositionLocalProvider(FN.LC provides FN.cl(dk)){X()} // 注入供给主题局部变量，并拉起中央路由引擎大组件
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
