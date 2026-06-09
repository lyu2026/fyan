package com.fyan

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable fun HS(nv:NavController){ // 应用首页主架：顶部Tab栏 + 内容区
	val cc=FN.LC.current
	var tb by remember{mutableStateOf(PR.lt)} // 当前激活Tab id，初始值从SP读取
	Column(modifier="fs".css().background(cc.b)){
		Row(modifier="fw h38".css().background(cc.s),verticalAlignment=Alignment.CenterVertically){
			Row(modifier=Modifier.fillMaxWidth().fillMaxHeight().horizontalScroll(rememberScrollState())){
				NAV_TABS.forEach{o->
					val ac=o.id==tb
					Box(
						modifier="fh".css()
							.background(if(ac)cc.p.copy(alpha=0.15f)else Color.Transparent)
							.clickable{tb=o.id;PR.lt=o.id;FN.lg("HomeTab","切换 → ${o.id}",'u')},
						contentAlignment=Alignment.Center
					){
						BasicText("  ${o.lb}  ",style=FN.TM.copy(color=if(ac)cc.p else cc.os.copy(alpha=0.7f),fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))
					}
				}
			}
		}
		Box(modifier="fw e1".css(this)){
			key(tb){ // key强制Tab切换时销毁重建子组件，避免状态串台
				when(tb){
					"history"->HI(nv)
					else->FS(nv,id=tb)
				}
			}
		}
	}
}

@Composable fun HI(nv:NavController){ // 历史记录页：网格展示 + 长按删除 + 清空确认
	val cc=FN.LC.current
	var cl by remember{mutableStateOf(false)} // 清空全部确认弹窗开关
	var rk by remember{mutableStateOf<String?>(null)} // 待删除的单条记录id
	val cf=LocalConfiguration.current
	val cs=when{
		cf.screenWidthDp>=840->5
		cf.screenWidthDp>=600->4
		else->3
	}
	Column(modifier="fs".css().background(cc.b)){
		Row(modifier="fw h30 ps8 pe2".css(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
			BasicText("记录清单",style=FN.TS.copy(color=cc.os))
			IB(lb="🗑",oc={cl=true},modifier="w28 h28".css())
		}
		if(FN.hi.isEmpty()){
			Box(modifier="fs".css(),contentAlignment=Alignment.Center){BasicText("暂无观看记录",style=FN.BM.copy(color=cc.os.copy(alpha=0.4f)))}
		}else{
			LazyVerticalGrid(modifier="fw".css(),columns=GridCells.Fixed(cs),contentPadding=PaddingValues(2.dp),verticalArrangement=Arrangement.spacedBy(2.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)){
				gridItems(FN.hi,key={it.id}){o->
					VC(tt=o.tt,pt=o.pt,sb=o.pg,modifier="fw".css(),oc={FN.lg("History","点击 ${o.id}",'u');nv.navigate("detail/${o.id}")},lp={rk=o.id})
				}
			}
		}
	}
	if(cl)CD(tt="确认清空所有历史记录？",od={cl=false},oc={cH();cl=false})
	rk?.let{ky->CD(tt="确认删除该条历史记录？",od={rk=null},oc={rH(ky);rk=null})}
}

@Composable fun FS(nv:NavController,id:String){ // 筛选列表页：过滤条件树 + 分页视频网格
	val cc=FN.LC.current
	var gs by remember{mutableStateOf<List<FG>>(emptyList())}
	var ds by remember{mutableStateOf<List<String>>(emptyList())}
	var vs by remember{mutableStateOf<List<VI>>(emptyList())}
	var lm by remember{mutableStateOf(false)} // 加载更多防抖锁
	var hm by remember{mutableStateOf(true)} // 是否还有更多页
	var pg by remember{mutableIntStateOf(1)}
	var ld by remember{mutableStateOf(true)}
	val ls=rememberLazyGridState()
	val sc=rememberCoroutineScope()

	LaunchedEffect(id){
		ld=true
		val s=fG(id)
		gs=s;ds=s.map{"0"}
		FN.lg("FilterScreen","ids=${ds.joinToString(",")}",'i')
		vs=fL(id,ds.joinToString(","),1)
		pg=1;hm=vs.size>=21;ld=false
		FN.lg("FilterScreen","tab=$id list=${s.size}",'i')
	}

	suspend fun rl(){
		ld=true
		val nq=ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")}
		vs=fL(id,nq,1);pg=1;hm=vs.size>=21;ld=false
	}

	val nr by remember{derivedStateOf{val lt=ls.layoutInfo;val lx=lt.visibleItemsInfo.lastOrNull()?.index?:-1;lx>=lt.totalItemsCount-3&&lt.totalItemsCount>0}}
	LaunchedEffect(nr){
		if(nr&&hm&&!lm&&!ld){
			lm=true
			val nq=ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")}
			val nx=fL(id,nq,pg+1)
			if(nx.isNotEmpty()){vs=vs+nx;pg++}else hm=false
			lm=false
		}
	}

	val cf=LocalConfiguration.current
	val cs=when{id=="news"->1;cf.screenWidthDp>=840->6;cf.screenWidthDp>=600->4;else->3}
	Column(modifier="fs".css().background(cc.b)){
		if(gs.isNotEmpty()){
			Column(modifier="fw".css().background(cc.s).border(0.5.dp,cc.ov)){
				gs.forEachIndexed{i,g->
					TR(tb=g.op.map{it.id to it.lb},tc=ds.getOrElse(i){"0"},on={ii->
						ds=ds.toMutableList().also{it[i]=ii}
						sc.launch{rl()}
					})
					if(i<gs.lastIndex)Box(modifier="fw h0.5".css().background(cc.ov))
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
							id=="news"->Box(modifier="fw".css().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
								var pg by remember{mutableIntStateOf(0)}
								var playing by remember(pg){mutableStateOf(false)}
								VP(pt=o.pt,sc=o.id,playing=playing,onPlay={playing=true})
							}else->VC(pt=o.pt,tt=o.tt,modifier="fw".css(),oc={
								aH(FN.VT(o.id,o.type,o.tt,o.pt));
								nv.navigate("detail/${o.id}")
							},sb=listOfNotNull(o.sc.takeIf{it.isNotEmpty()},o.ut.takeIf{it.isNotEmpty()}).joinToString(" · "))
						}
					}
					if(lm)item{CL(tt="加载更多…")}
				}
			}
		}
	}
}

@Composable fun DS(nv:NavController,id:String){ // 详情播放页
	val cc=FN.LC.current
	val cf=LocalConfiguration.current
	val tt=cf.screenWidthDp>=600
	val lp=cf.orientation==Configuration.ORIENTATION_LANDSCAPE
	val tv=(cf.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
	var o by remember{mutableStateOf<VD?>(null)}
	var ld by remember{mutableStateOf(true)}
	var pg by remember{mutableIntStateOf(0)}

	LaunchedEffect(id){
		ld=true;pg=0 // 切视频时重置集数指针
		o=fD(id);ld=false
		FN.lg("DetailScreen","id=$id",'i')
		if(FN.hi.none{it.id==id})o?.let{aH(FN.VT(it.id,it.type,it.tt,it.pt,"第1集"))}
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
			else->{
				val d=o!!
				if(tv||(tt&&lp))TV(id,d,pg){i->pg=i;aH(FN.VT(d.id,d.type,d.tt,d.pt,"第${i+1}集"))}
				else PL(id,d,pg){i->pg=i;aH(FN.VT(d.id,d.type,d.tt,d.pt,"第${i+1}集"))}
			}
		}
	}
}

@Composable private fun TV(id:String,d:VD,ep:Int,oe:(Int)->Unit){ // TV/大屏横向分栏布局
	val cc=FN.LC.current
	var u by remember(ep){mutableStateOf("")}
	var playing by remember(ep){mutableStateOf(false)}
	LaunchedEffect(ep){playing=false;u=rs(id,d,ep);FN.lg("VideoSource","ep=$ep url=$u",'u')}
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

@Composable private fun PL(id:String,d:VD,ep:Int,oe:(Int)->Unit){ // 手机竖屏布局
	val cc=FN.LC.current
	var u by remember(ep){mutableStateOf("")}
	var playing by remember(ep){mutableStateOf(false)}
	LaunchedEffect(ep){playing=false;u=rs(id,d,ep);FN.lg("VideoSource","ep=$ep url=$u",'u')}
	// ★ 外层用 verticalScroll，子项不能再用 fillMaxSize/fillMaxHeight
	//   播放器区域必须用固定宽高比约束，不能用"fs"，否则在无界约束下崩溃
	Column(modifier="fs sv".css()){
		// 播放器容器：固定16:9宽高比，fillMaxWidth驱动高度，绝不使用fillMaxHeight/fillMaxSize
		Box(modifier="fw".css().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
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
				EG(tl=d.et,ct=ep,cs=5,os=oe)
			}
		}
		Spacer(modifier="h24".css())
	}
}

// 提取公共集数解析：TV和PL共用，避免重复代码
private suspend fun rs(id:String,d:VD,ep:Int):String{
	val ru=d.ep.getOrNull(ep)?:""
	return if(!ru.isNotEmpty()||!ru.startsWith("http",ignoreCase=true))fS(id,d.type,ru)else""
}

@Composable private fun EG(tl:List<String>,ct:Int,cs:Int,os:(Int)->Unit){ // 静态剧集矩阵网格，避免嵌套滚动冲突
	val cc=FN.LC.current
	val rs=(tl.size+cs-1)/cs
	Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
		repeat(rs){r->
			Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
				repeat(cs){c->
					val i=r*cs+c
					if(i<tl.size)EB(lb=tl[i],ac=i==ct,oc={os(i)},modifier="fw e1 h24".css(this))
					else Spacer(modifier="e1".css(this)) // 末行空位补齐防拉伸
				}
			}
		}
	}
}

@Composable private fun VP(pt:String,sc:String,playing:Boolean,onPlay:()->Unit){ // 播放器核心组件
	val cc=FN.LC.current
	if(!playing){
		Box(modifier="fs".css().clickable{onPlay()},contentAlignment=Alignment.Center){
			AsyncImage(model=pt,contentDescription="封面",contentScale=ContentScale.Fit,modifier="fs".css())
			Box(modifier="w56 h56 c".css().background(Color.Black.copy(alpha=0.5f)),contentAlignment=Alignment.Center){BasicText("▶",style=FN.TL.copy(color=Color.White))}
		}
	}else{
		val c=LocalContext.current
		val cfg=LocalConfiguration.current
		val tv=(cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
		var fs by remember{mutableStateOf(false)} // 全屏状态开关

		val listener=remember{object:Player.Listener{
			override fun onPlaybackStateChanged(s:Int){if(s==Player.STATE_READY)FN.lg("VP","可播放",'n')}
		}}

		val player=remember{ExoPlayer.Builder(c).build().apply{playWhenReady=true}}
		DisposableEffect(player){
			player.addListener(listener)
			onDispose{player.removeListener(listener);player.release()}
		}

		// 组件彻底销毁时，如果是手机则强制切回竖屏
		DisposableEffect(Unit){onDispose{
			if(!tv)(c as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
		}}

		LaunchedEffect(sc){
			FN.lg("VideoPlay",sc,'u')
			val factory=if(sc.contains(".m3u8",ignoreCase=true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory()) else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
			player.setMediaSource(factory.createMediaSource(MediaItem.fromUri(Uri.parse(sc))))
			player.prepare()
		}

		if(fs){
			Dialog(
				onDismissRequest={fs=false},
				properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=true,dismissOnClickOutside=false)
			){
				// ★ 修复点：手机设备触发横屏，TV设备保持自然状态；使用标准 requestedOrientation 触发旋转
				LaunchedEffect(Unit){if(!tv)(c as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
				// ★ 修复点：Dialog 关闭时恢复竖屏
				DisposableEffect(Unit){onDispose{if(!tv)(c as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}}
				Box(
					modifier=Modifier.fillMaxSize().background(Color.Black)
						.pointerInput(Unit){detectDragGestures(
							onDragEnd={if(xo>80f){fs=false;xo=0f}else xo=0f},
							onDrag={ch:PointerInputChange,d:Offset->ch.consume();xo=if(d.x>0)xo+d.x else if(xo>0)maxOf(xo+d.x,0f)else 0f}
						)},
					contentAlignment=Alignment.Center
				){
					AndroidView(factory={PlayerView(c).apply{this.player=player;useController=true}},modifier=Modifier.fillMaxSize())
					Box(
						modifier=Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Black.copy(alpha=0.5f)).clickable{fs=false},
						contentAlignment=Alignment.Center
					){BasicText("✕",style=FN.BM.copy(color=Color.White))}
				}
			}
		}

		Box(
			// ★ 修复点：移除 if(!tv) 限制，允许 TV 设备同样响应点击事件放大全屏（或唤起带有控制器的界面）
			modifier=Modifier.fillMaxSize().clickable{fs=true}, 
			contentAlignment=Alignment.Center
		){
			AndroidView(factory={PlayerView(c).apply{this.player=player;useController=false}},modifier=Modifier.fillMaxSize())
		}
	}
}


// xo 需要在 VP 作用域外声明以便 Dialog 内的手势访问（提升到文件级私有）
private var xo=0f