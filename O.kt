package com.fyan

import android.os.Bundle
import androidx.compose.runtime.*
import android.content.res.Configuration
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.navigation.compose.composable

// ════════════════════════════════════════════════════════════════
// 入口 Activity
// ════════════════════════════════════════════════════════════════
class O:androidx.activity.ComponentActivity(){
	override fun onCreate(savedInstanceState:Bundle?){
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		Prefs.init(applicationContext)
		setContent{
			val dark=(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES
			CompositionLocalProvider(Fyan.LC provides Fyan.color(dark)){X()}
		}
	}
}

// ════════════════════════════════════════════════════════════════
// 导航路由
// ════════════════════════════════════════════════════════════════
@Composable
private fun X(){
	val nav=androidx.navigation.compose.rememberNavController()
	androidx.compose.foundation.layout.Box(modifier="fs".css()){
		androidx.navigation.compose.NavHost(navController=nav,startDestination="home"){
			composable("home"){HomeScreen(nav)}
			composable("history"){HistoryScreen(nav)}
			composable("filter/{id}"){back->FilterScreen(nav,back.arguments?.getString("id")?:"movie")}
			composable("detail/{id}"){back->DetailScreen(nav,back.arguments?.getString("id")?:"")}
		}
		Fyan.LogPanel()
	}
}
