package com.fyan

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
import java.util.UUID

@Composable fun HS(nv:NavController){ // 应用首页主架：顶部Tab栏 + 内容区
	val cc=FN.LC.current
	var tb by remember{mutableStateOf(PR.lt)} // 当前激活Tab id，初始值从SP读取
	Column(modifier="fs".css().background(cc.b)){
		Row(modifier="fw h38".css().background(cc.s),verticalAlignment=Alignment.CenterVertically){
			Row(modifier=Modifier.fillMaxWidth().fillMaxHeight().horizontalScroll(rememberScrollState())){
				NAV_TABS.forEach{o->
					val ac=o.id==tb // 是否当前激活Tab
					Box(
						modifier="fh".css()
							.background(if(ac)cc.p.copy(alpha=0.15f)else Color.Transparent)
							.clickable{tb=o.id;PR.lt=o.id;FN.lg("HomeTab","切换 → ${o.id}",'u')}, // 点击切Tab并持久化
						contentAlignment=Alignment.Center
					){
						BasicText("  ${o.lb}  ",style=FN.TM.copy(color=if(ac)cc.p else cc.os.copy(alpha=0.7f),fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))
					}
				}
			}
		}
		Box(modifier="fw e1".css(this)){ // 内容区占剩余高度
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
	var cl by remember{mutableStateOf(false)} // 清空全部确认对话框显示状态
	var rk by remember{mutableStateOf<String?>(null)} // 待删除的单条记录id（非null则弹确认框）
	val cf=LocalConfiguration.current
	val cs=when{ // 自适应列数：宽屏多列
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
					VC(tt=o.tt,pt=o.pt,sb=o.pg,modifier="fw".css(),oc={FN.lg("History","点击 ${o.id}",'u');nv.navigate("detail/${o.id}")},lp={rk=o.id}) // 长按触发单条删除确认
				}
			}
		}
	}
	if(cl)CD(tt="确认清空所有历史记录？",od={cl=false},oc={cH();cl=false}) // 清空二次确认
	rk?.let{ky->CD(tt="确认删除该条历史记录？",od={rk=null},oc={rH(ky);rk=null})} // 单条删除二次确认
}

@Composable fun FS(nv:NavController,id:String){ // 筛选列表页：过滤条件树 + 分页视频网格
	val cc=FN.LC.current
	var gs by remember{mutableStateOf<List<FG>>(emptyList())} // 过滤条件分组列表
	var ds by remember{mutableStateOf<List<String>>(emptyList())} // 各组当前选中项id
	var vs by remember{mutableStateOf<List<VI>>(emptyList())} // 当前页视频列表
	var lm by remember{mutableStateOf(false)} // 加载更多防抖锁，防止触底重复触发
	var hm by remember{mutableStateOf(true)} // 是否还有更多页（false=已到末页）
	var pg by remember{mutableIntStateOf(1)} // 当前分页游标
	var ld by remember{mutableStateOf(true)} // 首屏加载中标志
	val ls=rememberLazyGridState()
	val sc=rememberCoroutineScope()

	LaunchedEffect(id){ // Tab切换时重新拉取过滤条件和首页数据
		ld=true
		val s=fG(id)
		gs=s
		ds=s.map{"0"} // 默认每组选"全部"（id=0）
		FN.lg("FilterScreen","ids=${ds.joinToString(",")}",'i')
		vs=fL(id,ds.joinToString(","),1)
		pg=1
		hm=vs.size>=21 // 不足21条说明已是最后一页
		ld=false
		FN.lg("FilterScreen","tab=$id list=${s.size}",'i')
	}

	suspend fun rl(){ // 过滤条件变更后重新加载第一页
		ld=true
		val nq=ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")}
		FN.lg("FilterScreen","ids=$nq",'i')
		vs=fL(id,nq,1)
		pg=1
		hm=vs.size>=21
		ld=false
	}

	// 触底检测：最后可见item的索引接近总数时触发加载更多
	val nr by remember{derivedStateOf{val lt=ls.layoutInfo;val lx=lt.visibleItemsInfo.lastOrNull()?.index?:-1;lx>=lt.totalItemsCount-3&&lt.totalItemsCount>0}}
	LaunchedEffect(nr){
		if(nr&&hm&&!lm&&!ld){
			lm=true
			val nq=ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")}
			FN.lg("FilterScreen","ids=$nq",'i')
			val nx=fL(id,nq,pg+1)
			if(nx.isNotEmpty()){vs=vs+nx;pg++}
			else hm=false // 返回空则标记无更多
			lm=false
		}
	}

	val cf=LocalConfiguration.current
	val cs=when{cf.screenWidthDp>=840->6;cf.screenWidthDp>=600->4;else->3} // 自适应列数
	Column(modifier="fs".css().background(cc.b)){
		if(gs.isNotEmpty()){
			Column(modifier="fw".css().background(cc.s).border(0.5.dp,cc.ov)){ // 过滤条件区
				gs.forEachIndexed{i,g->
					TR(tb=g.op.map{it.id to it.lb},tc=ds.getOrElse(i){"0"},on={ii->
						ds=ds.toMutableList().also{it[i]=ii} // 更新第i组选中项
						sc.launch{rl()} // 重新加载列表
					})
					if(i<gs.lastIndex)Box(modifier="fw h0.5".css().background(cc.ov)) // 组间分隔线
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
							aH(FN.VT(o.id,o.tt,o.pt)) // 点击时写入历史
							nv.navigate("detail/${o.id}")
						},sb=listOfNotNull(o.sc.takeIf{it.isNotEmpty()},o.ut.takeIf{it.isNotEmpty()}).joinToString(" · "))
					}
					if(lm)item{CL(tt="加载更多…")} // 底部加载中占位
				}
			}
		}
	}
}

@Composable fun DS(nv:NavController,id:String){ // 详情播放页：自动识别布局模式（手机竖屏/大屏横屏/TV）
	val cc=FN.LC.current
	val cf=LocalConfiguration.current
	val tt=cf.screenWidthDp>=600 // 宽屏判定（平板/TV）
	val lp=cf.orientation==Configuration.ORIENTATION_LANDSCAPE // 当前是否横屏
	val tv=(cf.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION // TV端判定
	var o by remember{mutableStateOf<VD?>(null)} // 当前视频详情数据
	var ld by remember{mutableStateOf(true)} // 加载中标志
	var pg by remember{mutableIntStateOf(0)} // 当前选集指针

	LaunchedEffect(id){ // 视频id变化时重新拉取详情
		ld=true;pg=0 // 重置集数指针，防止切视频时残留上一个视频的集数
		o=fD(id)
		ld=false
		FN.lg("DetailScreen","id=$id",'i')
		val existing=FN.hi.firstOrNull{it.id==id}
		if(existing==null){ // 首次访问该视频时写入历史（已有记录不覆盖进度）
			o?.let{aH(FN.VT(id=it.id,tt=it.tt,pt=it.pt,pg="第1集"))}
		}
	}

	Column(modifier="fs".css().background(cc.b)){
		TB(tt=o?.tt?:"视频详情",ob={nv.popBackStack()}) // 顶栏显示视频标题，返回箭头退出详情页
		when{
			ld->CL(vc=true,tt="加载视频详情…")
			o==null->Box(modifier="fs".css(),contentAlignment=Alignment.Center){ // 加载失败+重试
				Column(horizontalAlignment=Alignment.CenterHorizontally){
					BasicText("加载失败",style=FN.BM.copy(color=cc.os.copy(alpha=0.5f)))
					Spacer(modifier="h8".css())
					Box(modifier="ph16 pv6 c4".css().background(cc.sv).clickable{ld=true},contentAlignment=Alignment.Center){
						BasicText("重试",style=FN.BS.copy(color=cc.p))
					}
				}
			}
			else->{
				val d=o!!
				// TV或宽屏横屏走左右分栏布局，否则走手机竖向滚动布局
				if(tv||(tt&&lp))TV(id,d,pg){i->pg=i;aH(FN.VT(d.id,d.tt,d.pt,"第${i+1}集"))}
				else PL(id,d,pg){i->pg=i;aH(FN.VT(d.id,d.tt,d.pt,"第${i+1}集"))}
			}
		}
	}
}

@Composable private fun TV(id:String,d:VD,ep:Int,oe:(Int)->Unit){ // TV/大屏横向分栏布局：左侧播放器+集数，右侧简介
	val cc=FN.LC.current
	var u by remember(ep){mutableStateOf("")} // 当前集播放直链（ep变化时自动重置）
	var playing by remember(ep){mutableStateOf(false)} // 播放状态（ep变化时重置为未播放）
	LaunchedEffect(ep){
		playing=false
		u=rs(id,d,ep) // 解析当前集播放直链
		FN.lg("VideoSource","${ep}: -> $u",'u')
	}
	Row(modifier="fs".css()){
		Column(modifier="fh e2".css(this)){ // 左侧占2/3宽
			Box(modifier="fw".css().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
				if(u.isNotEmpty()){VP(pt=d.pt,sc=u,playing=playing,onPlay={playing=true})}else{BasicText("加载中...",style=FN.TS.copy(color=cc.os))}
			}
			LazyRow(modifier="fw".css(),horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)){
				items(d.et.indices.toList()){i->EB(lb=d.et[i],ac=i==ep,modifier="w50 h24".css(),oc={oe(i)})}
			}
		}
		Column(modifier="fh ph16 pv12 e1 sv".css(this)){ // 右侧简介区占1/3宽，可垂直滚动
			BasicText("简介",style=FN.TS.copy(color=cc.os.copy(alpha=0.5f)))
			Spacer(modifier="h6".css())
			BasicText(d.ds,style=FN.BM.copy(color=cc.os))
		}
	}
}

@Composable private fun PL(id:String,d:VD,ep:Int,oe:(Int)->Unit){ // 手机竖屏布局：播放器+简介+选集网格，整体可垂直滚动
	val cc=FN.LC.current
	var u by remember(ep){mutableStateOf("")} // 当前集播放直链（ep变化时自动重置）
	var playing by remember(ep){mutableStateOf(false)} // 播放状态（ep变化时重置）
	LaunchedEffect(ep){
		playing=false
		u=rs(id,d,ep) // 解析当前集播放直链
		FN.lg("VideoSource","${ep}: -> $u",'u')
	}
	Column(modifier="fs sv".css()){ // 整体竖向可滚动
		Box(modifier="fw".css().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
			if(u.isNotEmpty()){VP(pt=d.pt,sc=u,playing=playing,onPlay={playing=true})}else{BasicText("加载中...",style=FN.TS.copy(color=cc.os))}
		}
		Column(modifier="fw ph14 pv12".css()){
			BasicText("简介",style=FN.TS.copy(color=cc.os.copy(alpha=0.5f)))
			Spacer(modifier="h4".css())
			BasicText(d.ds,style=FN.BM.copy(color=cc.os))
		}
		Box(modifier="fw h0.5 ph14".css().background(cc.ov)) // 分隔线
		if(d.et.isNotEmpty()){
			Column(modifier="fw ph12 pv8".css()){
				BasicText("选集",style=FN.TS.copy(color=cc.os.copy(alpha=0.5f)))
				Spacer(modifier="h8".css())
				EG(tl=d.et,ct=ep,cs=5,os=oe) // 静态网格，5列布局
			}
		}
		Spacer(modifier="h24".css())
	}
}

// 提取公共集数解析逻辑，TV和PL共用，避免代码重复
private suspend fun rs(id:String,d:VD,ep:Int):String{
	val ru=d.ep.getOrNull(ep)?:"" // 取当前集的episodeId
	return if(ru.isNotEmpty()&&!ru.startsWith("http",ignoreCase=true))fS(id,ru) // episodeId不是直链则走解析接口
	else "" // 空或已是直链则直接返回空（直链情况由VP内的sc参数判断）
}

@Composable private fun EG(tl:List<String>,ct:Int,cs:Int,os:(Int)->Unit){ // 静态剧集矩阵网格：避免嵌套滚动冲突，全量渲染适合中小集数
	val cc=FN.LC.current
	val rs=(tl.size+cs-1)/cs // 向上取整计算总行数
	Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
		repeat(rs){r->
			Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
				repeat(cs){c->
					val i=r*cs+c
					if(i<tl.size){
						EB(lb=tl[i],ac=i==ct,oc={os(i)},modifier="fw e1 h24".css(this))
					}else Spacer(modifier="e1".css(this)) // 末行空位补齐，防止有效按钮被拉伸
				}
			}
		}
	}
}

@Composable private fun VP(pt:String,sc:String,playing:Boolean,onPlay:()->Unit){ // 播放器核心组件：封面点击播放 + 全屏横屏 + 侧滑退出全屏
	val cc=FN.LC.current
	if(!playing){
		// 未播放态：封面图 + 居中播放按钮
		Box(modifier="fs".css().clickable{onPlay()},contentAlignment=Alignment.Center){
			AsyncImage(model=pt,contentDescription="封面",contentScale=ContentScale.Fit,modifier="fs".css())
			Box(modifier="w56 h56 c".css().background(Color.Black.copy(alpha=0.5f)),contentAlignment=Alignment.Center){BasicText("▶",style=FN.TL.copy(color=Color.White))}
		}
	}else{
		val c=LocalContext.current
		val cfg=LocalConfiguration.current
		val tv=(cfg.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION // TV端不允许旋转屏幕
		val isLandscape=cfg.orientation==Configuration.ORIENTATION_LANDSCAPE // 当前是否横屏

		// key绑定isLandscape：方向真实切换时fs跟随重置，断开方向切换的死循环（原bug根因）
		var fs by remember(isLandscape){mutableStateOf(isLandscape)}
		var xo by remember{mutableStateOf(0f)} // 横屏侧滑偏移量（向右滑动退出全屏）

		val player=remember{ExoPlayer.Builder(c).build().apply{playWhenReady=true}} // 播放器实例，整个playing=true生命周期内复用

		// listener用remember缓存，避免每次重组重复addListener导致回调多次触发
		val listener=remember{object:Player.Listener{
			override fun onPlaybackStateChanged(s:Int){if(s==Player.STATE_READY)FN.lg("VP:Ready","可播放",'n')}
		}}

		// 注册监听器，组件销毁时同步移除并释放播放器资源
		DisposableEffect(player){player.addListener(listener);onDispose{player.removeListener(listener);player.release()}}

		// 组件销毁时（如页面退出）恢复竖屏方向，防止其他页面残留横屏
		DisposableEffect(Unit){onDispose{
			val act=c as? android.app.Activity
			if(!tv)act?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
		}}

		// fs变化时切换屏幕旋转方向（仅非TV设备）
		LaunchedEffect(fs){
			val act=c as? android.app.Activity
			if(tv)return@LaunchedEffect
			act?.requestedOrientation=if(fs)ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
		}

		// 视频源变化时重新构建MediaSource并准备播放
		LaunchedEffect(sc){
			FN.lg("VideoPlay",sc,'u')
			val factory=if(sc.contains(".m3u8"))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
			else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory()) // m3u8走HLS，其余走Progressive
			player.setMediaSource(factory.createMediaSource(MediaItem.fromUri(Uri.parse(sc))))
			player.prepare()
		}

		Box(
			// 横屏时fillMaxSize+系统栏安全区+侧滑偏移；竖屏时填满父容器
			// 修复：横屏不再硬写screenWidthDp/screenHeightDp，避免冲破父约束导致容器崩溃
			modifier=(if(isLandscape)"fs pnb pim".css() else "fs".css())
				.offset(x=xo.dp)
				.pointerInput(isLandscape){if(isLandscape)detectDragGestures( // 横屏时注册侧滑手势
					onDragEnd={if(xo>80f){fs=false;xo=0f}else xo=0f}, // 右滑超80dp退出全屏，否则弹回
					onDrag={ch:PointerInputChange,d:Offset->ch.consume();xo=if(d.x>0)xo+d.x else if(xo>0)maxOf(xo+d.x,0f)else 0f} // 仅允许向右拖动
				)}
				.clickable{if(!tv)fs=!fs} // 非TV点击切换全屏状态
		){
			AndroidView(
				factory={PlayerView(c).apply{this.player=player}}, // factory只做一次性初始化，不在此读取外部状态
				update={v->v.useController=isLandscape}, // update在每次重组时同步状态，横屏显示控制条，竖屏隐藏
				modifier="fs".css()
			)
			// 横屏时显示左上角关闭全屏按钮（TV端不需要）
			if(isLandscape&&!tv)Box(modifier="w32 h32 c mt6 ms6".css().background(Color.Black.copy(alpha=0.5f)).clickable{fs=false}){BasicText("✕",style=FN.BM.copy(color=Color.White))}
		}
	}
}