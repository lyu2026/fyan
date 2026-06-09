package com.fyan

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

// ── 根Composable（由O.kt调用）────────────────────────────────────────────────
@Composable fun X(onExit:()->Unit){ // 路由树 + 退出确认 + 日志浮层
	val c=rememberNavController()
	var se by remember{mutableStateOf(false)} // 退出确认弹窗状态
	BackHandler{se=!se}
	val cc=FN.LC.current
	Box(modifier="fs psb pnb".css(),contentAlignment=Alignment.BottomCenter){ // 全屏+安全区，日志贴底
		NavHost(navController=c,startDestination="home"){
			composable("home"){HS(c)}
			composable("filter/{id}"){b->FS(c,id=b.arguments?.getString("id")?:"movie")}
			composable("detail/{id}"){b->DS(c,id=b.arguments?.getString("id")?:"")}
		}
		LP() // 日志浮层始终在NavHost上层
	}
	if(se)CD(tt="确定杀死应用并退出吗？",od={se=false},oc={onExit()}) // 退出确认框
}

// ── 首页：顶部Tab栏 + 内容区 ──────────────────────────────────────────────────
@Composable fun HS(nv:NavController){
	val cc=FN.LC.current
	var tb by remember{mutableStateOf(PR.lt)} // 当前激活Tab，初始从SP恢复
	Column(modifier="fs".css().background(cc.b)){
		Row(modifier="fw h38".css().background(cc.s),verticalAlignment=Alignment.CenterVertically){ // Tab栏
			Row(modifier=Modifier.fillMaxWidth().fillMaxHeight().horizontalScroll(rememberScrollState())){
				NAV_TABS.forEach{o->
					val ac=o.id==tb
					Box(modifier="fh".css().background(if(ac)cc.p.copy(alpha=0.15f)else Color.Transparent)
						.clickable{tb=o.id;PR.lt=o.id;lg("HomeTab","切换 → ${o.id}",'u')},
						contentAlignment=Alignment.Center){
						BasicText("  ${o.lb}  ",style=FN.TM.copy(color=if(ac)cc.p else cc.os.copy(alpha=0.7f)))
					}
				}
			}
		}
		Box(modifier="fw e1".css(this)){
			key(tb){ // key强制Tab切换时销毁重建，避免状态串台
				when(tb){"history"->HI(nv);else->FS(nv,id=tb)}
			}
		}
	}
}

// ── 历史记录页：网格 + 长按删除 + 清空确认 ───────────────────────────────────
@Composable fun HI(nv:NavController){
	val cc=FN.LC.current
	var cl by remember{mutableStateOf(false)} // 清空全部确认弹窗
	var rk by remember{mutableStateOf<String?>(null)} // 待删除单条id
	val cs=when{LocalConfiguration.current.screenWidthDp>=840->5;LocalConfiguration.current.screenWidthDp>=600->4;else->3} // 响应式列数
	Column(modifier="fs".css().background(cc.b)){
		Row(modifier="fw h30 ps8 pe2".css(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
			BasicText("记录清单",style=FN.TS.copy(color=cc.os))
			IB(lb="🗑",oc={cl=true},modifier="w28 h28".css())
		}
		if(HI.isEmpty()){
			Box(modifier="fs".css(),contentAlignment=Alignment.Center){BasicText("暂无观看记录",style=FN.BM.copy(color=cc.os.copy(alpha=0.4f)))}
		}else{
			LazyVerticalGrid(modifier="fw".css(),columns=GridCells.Fixed(cs),contentPadding=PaddingValues(2.dp),verticalArrangement=Arrangement.spacedBy(2.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)){
				gridItems(HI,key={it.id}){o->
					VC(tt=o.tt,pt=o.pt,sb=o.pg,modifier="fw".css(),oc={lg("History","点击 ${o.id}",'u');nv.navigate("detail/${o.id}")},lp={rk=o.id})
				}
			}
		}
	}
	if(cl)CD(tt="确认清空所有历史记录？",od={cl=false},oc={cH();cl=false})
	rk?.let{ky->CD(tt="确认删除该条历史记录？",od={rk=null},oc={rH(ky);rk=null})}
}

// ── 筛选列表页：过滤条件 + 分页网格 ─────────────────────────────────────────
@Composable fun FS(nv:NavController,id:String){
	val cc=FN.LC.current
	var gs by remember{mutableStateOf<List<FG>>(emptyList())}
	var ds by remember{mutableStateOf<List<String>>(emptyList())} // 各分组当前选中id
	var vs by remember{mutableStateOf<List<VI>>(emptyList())}
	var lm by remember{mutableStateOf(false)} // 加载更多防抖锁
	var hm by remember{mutableStateOf(true)} // 是否还有更多页
	var pg by remember{mutableIntStateOf(1)}
	var ld by remember{mutableStateOf(true)}
	val ls=rememberLazyGridState()
	val sc=rememberCoroutineScope()

	LaunchedEffect(id){ // Tab切换时重新拉取过滤树和首页列表
		ld=true;val s=fG(id);gs=s;ds=s.map{"0"}
		vs=fL(id,ds.joinToString(","),1);pg=1;hm=vs.size>=21;ld=false
		lg("FS","tab=$id filters=${s.size}",'i')
	}

	suspend fun rl(){ // 条件变更时重新加载第1页
		ld=true;vs=fL(id,ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")},1);pg=1;hm=vs.size>=21;ld=false
	}

	val nr by remember{derivedStateOf{ // 接近末尾时触发加载更多
		val lt=ls.layoutInfo;val lx=lt.visibleItemsInfo.lastOrNull()?.index?:-1;lx>=lt.totalItemsCount-3&&lt.totalItemsCount>0
	}}
	LaunchedEffect(nr){
		if(nr&&hm&&!lm&&!ld){lm=true
			val nx=fL(id,ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")},pg+1)
			if(nx.isNotEmpty()){vs=vs+nx;pg++}else hm=false;lm=false
		}
	}

	val cs=when{id=="news"->1;LocalConfiguration.current.screenWidthDp>=840->6;LocalConfiguration.current.screenWidthDp>=600->4;else->3}
	Column(modifier="fs".css().background(cc.b)){
		if(gs.isNotEmpty()){
			Column(modifier="fw".css().background(cc.s)){ // 过滤条件面板
				gs.forEachIndexed{i,g->
					TR(tb=g.op.map{it.id to it.lb},tc=ds.getOrElse(i){"0"},on={ii->
						ds=ds.toMutableList().also{it[i]=ii};sc.launch{rl()}
					})
					if(i<gs.lastIndex)Box(modifier="fw h0.5".css().background(cc.ov)) // 分隔线
				}
			}
		}
		Box(modifier="fw e1".css(this)){
			when{
				ld->CL(vc=true,tt="加载视频列表…")
				vs.isEmpty()->Box(modifier="fs".css(),contentAlignment=Alignment.Center){BasicText("暂无视频",style=FN.BM.copy(color=cc.os.copy(alpha=0.4f)))}
				else->LazyVerticalGrid(state=ls,modifier="fw".css(),columns=GridCells.Fixed(cs),contentPadding=PaddingValues(2.dp),verticalArrangement=Arrangement.spacedBy(2.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)){
					gridItems(vs,key={it.id}){o->
						when{
							id=="news"->NW(o) // 新闻行内播放
							else->VC(pt=o.pt,tt=o.tt,modifier="fw".css(),sb=listOfNotNull(o.sc.takeIf{it.isNotEmpty()},o.ut.takeIf{it.isNotEmpty()}).joinToString(" · "),
								oc={aH(RT(o.id,o.type,o.tt,o.pt));nv.navigate("detail/${o.id}")})
						}
					}
					if(lm)item{CL(tt="加载更多…")}
				}
			}
		}
	}
}

@Composable private fun NW(o:VI){ // 新闻行内播放卡片
	Box(modifier="fw".css().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
		var playing by remember{mutableStateOf(false)}
		VP(pt=o.pt,sc=o.id,playing=playing,onPlay={playing=true})
	}
}

// ── 详情播放页 ────────────────────────────────────────────────────────────────
@Composable fun DS(nv:NavController,id:String){
	val cc=FN.LC.current
	val cfg=LocalConfiguration.current
	val tt=cfg.screenWidthDp>=600 // 平板/大屏
	val lp=cfg.orientation==Configuration.ORIENTATION_LANDSCAPE
	val tv=(cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
	var o by remember{mutableStateOf<VD?>(null)}
	var ld by remember{mutableStateOf(true)}
	var ep by remember{mutableIntStateOf(0)} // 当前集数指针

	LaunchedEffect(id){
		ld=true;ep=0;o=fD(id);ld=false
		lg("DS","id=$id",'i')
		if(HI.none{it.id==id})o?.let{aH(RT(it.id,it.type,it.tt,it.pt,"第1集"))}
	}

	Column(modifier="fs".css().background(cc.b)){
		TB(tt=o?.tt?:"视频详情",ob={nv.popBackStack()})
		when{
			ld->CL(vc=true,tt="加载视频详情…")
			o==null->Box(modifier="fs".css(),contentAlignment=Alignment.Center){
				Column(horizontalAlignment=Alignment.CenterHorizontally){
					BasicText("加载失败",style=FN.BM.copy(color=cc.os.copy(alpha=0.5f)))
					Spacer(modifier="h8".css())
					Box(modifier="ph16 pv6 c4".css().background(cc.sv).clickable{ld=true},contentAlignment=Alignment.Center){BasicText("重试",style=FN.BS.copy(color=cc.p))}
				}
			}
			else->{val d=o!!
				if(tv||(tt&&lp))DTV(id,d,ep){i->ep=i;aH(RT(d.id,d.type,d.tt,d.pt,"第${i+1}集"))}
				else DPL(id,d,ep){i->ep=i;aH(RT(d.id,d.type,d.tt,d.pt,"第${i+1}集"))}
			}
		}
	}
}

@Composable private fun DTV(id:String,d:VD,ep:Int,oe:(Int)->Unit){ // TV/大屏横向分栏
	val cc=FN.LC.current
	var u by remember(ep){mutableStateOf("")}
	var playing by remember(ep){mutableStateOf(false)}
	LaunchedEffect(ep){playing=false;u=rs(id,d,ep);lg("VS","ep=$ep url=$u",'u')}
	Row(modifier="fs".css()){
		Column(modifier="fh e2".css(this)){
			Box(modifier="fw".css().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
				if(u.isNotEmpty())VP(pt=d.pt,sc=u,playing=playing,onPlay={playing=true})
				else BasicText("加载中...",style=FN.TS.copy(color=cc.os))
			}
			LazyRow(modifier="fw".css(),horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)){
				items(d.et.indices.toList()){i->EB(lb=d.et[i],ac=i==ep,modifier="w50 h24".css(),oc={oe(i)})}
			}
		}
		Column(modifier="fh ph16 pv12 e1 sv".css(this)){
			BasicText("简介",style=FN.TS.copy(color=cc.os.copy(alpha=0.5f)))
			Spacer(modifier="h6".css())
			BasicText(d.ds,style=FN.BM.copy(color=cc.os))
		}
	}
}

@Composable private fun DPL(id:String,d:VD,ep:Int,oe:(Int)->Unit){ // 手机竖屏布局
	val cc=FN.LC.current
	var u by remember(ep){mutableStateOf("")}
	var playing by remember(ep){mutableStateOf(false)}
	LaunchedEffect(ep){playing=false;u=rs(id,d,ep);lg("VS","ep=$ep url=$u",'u')}
	Column(modifier="fs sv".css()){ // 外层verticalScroll，子项禁止fillMaxHeight
		Box(modifier="fw".css().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){ // 固定16:9，fillMaxWidth驱动高度
			if(u.isNotEmpty())VP(pt=d.pt,sc=u,playing=playing,onPlay={playing=true})
			else BasicText("加载中...",style=FN.TS.copy(color=cc.os))
		}
		Column(modifier="fw ph14 pv12".css()){
			BasicText("简介",style=FN.TS.copy(color=cc.os.copy(alpha=0.5f)))
			Spacer(modifier="h4".css())
			BasicText(d.ds,style=FN.BM.copy(color=cc.os))
		}
		Box(modifier="fw h0.5 ph14".css().background(cc.ov))
		if(d.et.isNotEmpty()){
			Column(modifier="fw ph12 pv8".css()){
				BasicText("选集",style=FN.TS.copy(color=cc.os.copy(alpha=0.5f)))
				Spacer(modifier="h8".css())
				EG(tl=d.et,ct=ep,cs=5,os=oe) // 来自U.kt的剧集矩阵组件
			}
		}
		Spacer(modifier="h24".css())
	}
}

// ── 播放器核心（SurfaceView实现，不依赖media3-ui）────────────────────────────
@Composable private fun VP(pt:String,sc:String,playing:Boolean,onPlay:()->Unit){
	val cc=FN.LC.current
	if(!playing){ // 封面+播放按钮占位
		Box(modifier="fs".css().clickable{onPlay()},contentAlignment=Alignment.Center){
			AsyncImage(model=pt,contentDescription="封面",contentScale=ContentScale.Fit,modifier="fs".css())
			Box(modifier="w56 h56 c".css().background(Color.Black.copy(alpha=0.5f)),contentAlignment=Alignment.Center){BasicText("▶",style=FN.TL.copy(color=Color.White))}
		}
	}else{
		val ctx=LocalContext.current
		val cfg=LocalConfiguration.current
		val tv=(cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
		var fs by remember{mutableStateOf(false)} // 全屏状态
		var paused by remember{mutableStateOf(false)} // 播放/暂停切换

		val player=remember{ExoPlayer.Builder(ctx).build().apply{playWhenReady=true}}
		DisposableEffect(player){ // 组件销毁时释放ExoPlayer
			player.addListener(object:Player.Listener{
				override fun onPlaybackStateChanged(s:Int){if(s==Player.STATE_READY)lg("VP","可播放",'n')}
			})
			onDispose{
				player.release()
				if(!tv)(ctx as? Activity)?.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT // 非TV退出时恢复竖屏
			}
		}

		LaunchedEffect(sc){ // 视频源变更时重新准备播放
			lg("VP",sc,'u')
			val factory=if(sc.contains(".m3u8",ignoreCase=true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
			else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
			player.setMediaSource(factory.createMediaSource(MediaItem.fromUri(Uri.parse(sc))))
			player.prepare()
		}

		// SurfaceView内联播放区域（不依赖media3-ui PlayerView）
		Box(modifier="fs".css().clickable{paused=!paused;if(paused)player.pause()else player.play()},contentAlignment=Alignment.Center){
			AndroidView(factory={android.view.SurfaceView(ctx).also{player.setVideoSurfaceView(it)}},modifier="fs".css()) // SurfaceView绑定ExoPlayer
			if(paused)Box(modifier="w56 h56 c".css().background(Color.Black.copy(alpha=0.45f)),contentAlignment=Alignment.Center){BasicText("▶",style=FN.TL.copy(color=Color.White))} // 暂停时显示播放图标
			if(!tv)Box(modifier="w32 h32 c".css(this as androidx.compose.ui.Alignment@Alignment).background(Color.Black.copy(alpha=0.4f)).clickable{fs=true}.let{Modifier.align(Alignment.TopEnd).padding(8.dp).then(it)},contentAlignment=Alignment.Center){BasicText("⛶",style=FN.BS.copy(color=Color.White))} // 非TV显示全屏按钮
		}

		if(fs){ // 全屏Dialog
			var xo by remember{mutableStateOf(0f)} // 右划退出手势偏移
			LaunchedEffect(Unit){if(!tv)(ctx as? Activity)?.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE} // 进入全屏时横屏
			DisposableEffect(Unit){onDispose{if(!tv)(ctx as? Activity)?.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}} // 退出全屏时竖屏
			androidx.compose.ui.window.Dialog(
				onDismissRequest={fs=false},
				properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=true,dismissOnClickOutside=false)
			){
				Box(modifier="fs".css().background(Color.Black)
					.pointerInput(Unit){androidx.compose.foundation.gestures.detectDragGestures(
						onDragEnd={if(xo>80f){fs=false;xo=0f}else xo=0f},
						onDrag={ch,d->ch.consume();xo=if(d.x>0)xo+d.x else if(xo>0)maxOf(xo+d.x,0f)else 0f} // 右划>80dp退出全屏
					)},
					contentAlignment=Alignment.Center
				){
					AndroidView(factory={android.view.SurfaceView(ctx).also{player.setVideoSurfaceView(it)}},modifier="fs".css()) // 全屏复用同一player
					Box(modifier=Modifier.align(Alignment.TopStart).padding(8.dp)
						.then("w32 h32 c".css()).background(Color.Black.copy(alpha=0.5f)).clickable{fs=false},
						contentAlignment=Alignment.Center){BasicText("✕",style=FN.BM.copy(color=Color.White))} // 关闭全屏
				}
			}
		}
	}
}