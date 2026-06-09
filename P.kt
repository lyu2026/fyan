package com.fyan

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable fun HS(nv:NavController){ // HS (HomeScreen) 应用首页主架
	val cc=FN.LC.current
	var tb by remember{mutableStateOf(PR.lt)} // 主Tab游标
	Column(modifier="fs".css().background(cc.b)){
		Row(modifier="fw h38".css().background(cc.s),verticalAlignment=Alignment.CenterVertically){
			Row(modifier=Modifier.fillMaxWidth().fillMaxHeight().horizontalScroll(rememberScrollState())){
				NAV_TABS.forEach{o->
					val ac=o.id==tb
					Box(
						modifier="fh".css()
							.background(if(ac)cc.p.copy(alpha=0.15f)else androidx.compose.ui.graphics.Color.Transparent)
							.clickable{tb=o.id;PR.lt=o.id;FN.lg("HomeTab","切换 → ${o.id}",'u')},contentAlignment=Alignment.Center
					){
						BasicText("  ${o.lb}  ",style=FN.TM.copy(color=if(ac)cc.p else cc.os.copy(alpha=0.7f),fontWeight=if(ac)androidx.compose.ui.text.font.FontWeight.W600 else androidx.compose.ui.text.font.FontWeight.W400))
					}
				}
			}
		}
		Box(modifier="fw e1".css(this)){
			key(tb){
				when(tb){
					"history"->HI(nv)
					else->FS(nv,id=tb)
				}
			}
		}
	}
}

@Composable fun HI(nv:NavController){ // HI (HistoryScreen) 历史浏览记录页面
	val cc=FN.LC.current
	var cl by remember{mutableStateOf(false)} // 清空二次确认触发器
	var rk by remember{mutableStateOf<String?>(null)} // 待删除记录主键
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

@Composable fun FS(nv:NavController,id:String){ // FS (FilterScreen) 视频筛选列表页
	val cc=FN.LC.current
	var gs by remember{mutableStateOf<List<FG>>(emptyList())} // 过滤条件属性组
	var ds by remember{mutableStateOf<List<String>>(emptyList())} // 各组选中项映射
	var vs by remember{mutableStateOf<List<VI>>(emptyList())} // 视频数据集合
	var lm by remember{mutableStateOf(false)} // 分页防抖锁
	var hm by remember{mutableStateOf(true)} // 是否还有更多数据
	var pg by remember{mutableIntStateOf(1)} // 分页游标
	var ld by remember{mutableStateOf(true)} // 首次加载白屏锁
	val ls=rememberLazyGridState()
	val sc=rememberCoroutineScope()

	LaunchedEffect(id){
		ld=true
		val s=fG(id)
		gs=s
		ds=s.map{"0"}
		FN.lg("FilterScreen","ids=${ds.joinToString(",")}",'i')
		vs=fL(id,ds.joinToString(","),1)
		pg=1
		hm=vs.size>=21
		ld=false
		FN.lg("FilterScreen","tab=$id list=${s.size}",'i')
	}

	suspend fun rl(){
		ld=true
		val nq=ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")}
		FN.lg("FilterScreen","ids=$nq",'i')
		vs=fL(id,nq,1)
		pg=1
		hm=vs.size>=21
		ld=false
	}

	val nr by remember{derivedStateOf{val lt=ls.layoutInfo;val lx=lt.visibleItemsInfo.lastOrNull()?.index?:-1;lx>=lt.totalItemsCount-3&&lt.totalItemsCount>0}}
	LaunchedEffect(nr){
		if(nr&&hm&&!lm&&!ld){
			lm=true
			val nq=ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")}
			FN.lg("FilterScreen","ids=$nq",'i')
			val nx=fL(id,nq,pg+1)
			if(nx.isNotEmpty()){vs=vs+nx;pg++}
			else hm=false
			lm=false
		}
	}

	val cf=LocalConfiguration.current
	val cs=when{cf.screenWidthDp>=840->6;cf.screenWidthDp>=600->4;else->3}
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
						VC(pt=o.pt,tt=o.tt,modifier="fw".css(),oc={
							aH(FN.VT(o.id,o.tt,o.pt))
							nv.navigate("detail/${o.id}")
						},sb=listOfNotNull(o.sc.takeIf{it.isNotEmpty()},o.ut.takeIf{it.isNotEmpty()}).joinToString(" · "))
					}
					if(lm)item{CL(tt="加载更多…")}
				}
			}
		}
	}
}

@Composable fun DS(nv:NavController,id:String){ // DS (DetailScreen) 正片详情播放页
	val cc=FN.LC.current
	val cf=LocalConfiguration.current
	val tt=cf.screenWidthDp>=600 // 宽屏判定
	val lp=cf.orientation==Configuration.ORIENTATION_LANDSCAPE // 横屏判定
	val tv=(cf.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION // TV端判定
	var o by remember{mutableStateOf<VD?>(null)}
	var ld by remember{mutableStateOf(true)}
	var pg by remember{mutableIntStateOf(0)} // 当前集数指针

	LaunchedEffect(id){
		ld=true
		o=fD(id)
		ld=false
		FN.lg("DetailScreen","id=$id",'i')
		val existing=FN.hi.firstOrNull{it.id==id}
		if(existing==null){
			o?.let{aH(FN.VT(id=it.id,tt=it.tt,pt=it.pt,pg="第1集"))}
		}
	}

	Column(modifier="fs".css().background(cc.b)){
		TB(tt=o?.tt?:"视频详情",ob={nv.popBackStack()})
		when{
			ld->CL(vc=true,tt="加载视频详情…")
			o==null->Box(modifier="fs".css(),contentAlignment=Alignment.Center){BasicText("加载失败",style=FN.BM.copy(color=cc.os.copy(alpha=0.5f)))}
			else->{
				val d=o!!
				if(tv||(tt&&lp))TV(id,d,pg){i->pg=i;aH(FN.VT(d.id,d.tt,d.pt,"第${i+1}集"))}
				else PL(id,d,pg){i->pg=i;aH(FN.VT(d.id,d.tt,d.pt,"第${i+1}集"))}
			}
		}
	}
}

@Composable private fun TV(id:String,d:VD,ep:Int,oe:(Int)->Unit){ // TV 大屏横幅播放框架
	val cc=FN.LC.current
	var u by remember(ep){mutableStateOf("")}
	var playing by remember(ep){mutableStateOf(false)}
	LaunchedEffect(ep){
		playing=false
		val ru=d.ep.getOrNull(ep)?:""
		if(ru.isNotEmpty()&&!ru.startsWith("http",ignoreCase=true)){
			u=fS(id,ru)
		}else u=""
		FN.lg("VideoSource","${ep}: $ru -> $u",'u');
	}
	Row(modifier="fs".css()){
		Column(modifier="fh e2".css(this)){
			Box(modifier="fw".css().aspectRatio(16f/9f).background(androidx.compose.ui.graphics.Color.Black),contentAlignment=Alignment.Center){
				if(u.isNotEmpty()){VP(pt=d.pt,sc=u,playing=playing,onPlay={playing=true})}else{BasicText("加载中...",style=FN.TS.copy(color=cc.os))}
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

@Composable private fun PL(id:String,d:VD,ep:Int,oe:(Int)->Unit){ // PL 手机竖屏播放流组件
	val cc=FN.LC.current
	var u by remember(ep){mutableStateOf("")}
	var playing by remember(ep){mutableStateOf(false)}
	LaunchedEffect(ep){
		playing=false
		val ru=d.ep.getOrNull(ep)?:""
		if(ru.isNotEmpty()&&!ru.startsWith("http",ignoreCase=true)){
			u=fS(id,ru)
		}else u=""
		FN.lg("VideoSource","${ep}: $ru -> $u",'u');
	}
	Column(modifier="fs sv".css()){
		Box(modifier="fw".css().aspectRatio(16f/9f).background(androidx.compose.ui.graphics.Color.Black),contentAlignment=Alignment.Center){
			if(u.isNotEmpty()){VP(pt=d.pt,sc=u,playing=playing,onPlay={playing=true})}else{BasicText("加载中...",style=FN.TS.copy(color=cc.os))}
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

@Composable private fun EG(tl:List<String>,ct:Int,cs:Int,os:(Int)->Unit){ // EG 平铺剧集矩阵网格（避免嵌套滚动冲突）
	val cc=FN.LC.current
	val rs=(tl.size+cs-1)/cs // 向上取整总行数
	Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
		repeat(rs){r->
			Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
				repeat(cs){c->
					val i=r*cs+c
					if(i<tl.size){
						EB(lb=tl[i],ac=i==ct,oc={os(i)},modifier="fw e1 h24".css(this))
					}else Spacer(modifier="e1".css(this)) // 尾部空位补齐，防止前置按钮拉伸变形
				}
			}
		}
	}
}

@Composable private fun VP(pt:String,sc:String,playing:Boolean,onPlay:()->Unit){
	val cc=FN.LC.current
	if(!playing){
		Box(modifier="fs".css().clickable{onPlay()},contentAlignment=Alignment.Center){
			AsyncImage(model=pt,contentDescription="封面",contentScale=ContentScale.Fit,modifier="fs".css())
			Box(modifier="w56 h56 c".css().background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.5f)),contentAlignment=Alignment.Center){BasicText("▶",style=FN.TL.copy(color=androidx.compose.ui.graphics.Color.White))}
		}
	}else{
		val c=LocalContext.current
		val cfg=LocalConfiguration.current
		val tv=(cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION
		var fs by remember{mutableStateOf(false)}
		val player=remember{ExoPlayer.Builder(c).build().apply{playWhenReady=true;addListener(object:Player.Listener{override fun onPlaybackStateChanged(s:Int){if(s==Player.STATE_READY)FN.lg("VP:Ready","可播放",'n')}})}}
		DisposableEffect(player){onDispose{player.release()}}
		LaunchedEffect(sc){
			FN.lg("VideoPlay",sc,'u')
			val factory=if(sc.contains(".m3u8"))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
			else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
			player.setMediaSource(factory.createMediaSource(MediaItem.fromUri(Uri.parse(sc))))
			player.prepare()
		}
		val w=if(fs)cfg.screenWidthDp.dp else Dp.Unspecified
		val h=if(fs)cfg.screenHeightDp.dp else Dp.Unspecified
		val mo=if(fs)"w${w.value} h${h.value} pnb pim".css().offset(x=if(fs)xo.roundToInt().dp else 0.dp)else"fs".css()
		Box(modifier=mo.pointerInput(fs){if(fs)detectDragGestures(
			onDragEnd={if(xo>80f)fs=false;xo=0f},
			onDrag={ch,d->ch.consume();if(d.x>0)xo+=d.x else if(xo>0)xo=(xo+d.x).coerceAtLeast(0f)}
		)}.clickable{if(!tv)fs=!fs}){
			AndroidView(factory={PlayerView(c).apply{this.player=player;useController=fs}},modifier="fs".css())
			if(fs&&!tv)Box(modifier="w32 h32 c mt6 ms6".css().background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.5f)).clickable{fs=false}){BasicText("✕",style=FN.BM.copy(color=androidx.compose.ui.graphics.Color.White))}
		}
	}
}
/*
@Composable private fun VP(pt:String,sc:String,playing:Boolean,onPlay:()->Unit){ // VP 播放器核心组件
	val cc=FN.LC.current
	if(!playing){
		Box(modifier="fs".css().clickable{onPlay()},contentAlignment=Alignment.Center){
			AsyncImage(model=pt,contentDescription="封面",contentScale=ContentScale.Fit,modifier="fs".css())
			Box(modifier="w56 h56 c".css().background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.5f)),contentAlignment=Alignment.Center){BasicText("▶",style=FN.TL.copy(color=androidx.compose.ui.graphics.Color.White))}
		}
	}else{
		val c=LocalContext.current
		val player=remember{ExoPlayer.Builder(c).build().apply{playWhenReady=true}}
		DisposableEffect(player){onDispose{player.release()}}
		LaunchedEffect(sc){
			FN.lg("VideoPlay",sc,'u');
			val factory=if(sc.contains(".m3u8"))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
			else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
			player.setMediaSource(factory.createMediaSource(MediaItem.fromUri(Uri.parse(sc))))
			player.prepare()
		}
		AndroidView(factory={PlayerView(c).apply{this.player=player}},modifier="fs".css())
	}
}
*/