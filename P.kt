package com.fyan

import kotlinx.coroutines.launch
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage

// ════════════════════════════════════════════════════════════════
// 首页
// ════════════════════════════════════════════════════════════════
@Composable fun HomeScreen(nav:NavController){
	val c=Fyan.LC.current
	// 从缓存恢复或默认 history
	var tab by remember{mutableStateOf(Prefs.lastTab)}
	Column(modifier="fs".css().background(c.b)){
		// 顶部 Tab 栏
		Row(
			modifier="fw h48 psb".css().background(c.s).border(0.5.dp,c.ov),
			verticalAlignment=Alignment.CenterVertically,
		){
			Row(modifier="fh".css().weight(1f).horizontalScroll(rememberScrollState())){
				NAV_TABS.forEach{o->
					val x=o.id==tab
					Box(
						modifier="fh ph14".css()
							.background(if(x)c.p.copy(alpha=0.15f)else androidx.compose.ui.graphics.Color.Transparent)
							.clickable{
								tab=o.id
								Prefs.lastTab=o.id
								Fyan.log("HomeTab","切换 → ${o.id}",'u')
							},
						contentAlignment=Alignment.Center,
					){
						BasicText(
							o.label,style=Fyan.BS.copy(
								color=if(x)c.p else c.os.copy(alpha=0.7f),
								fontWeight=if(x)androidx.compose.ui.text.font.FontWeight.W600
									else androidx.compose.ui.text.font.FontWeight.W400,
							)
						)
					}
				}
			}
		}
		// 子页面内容区
		Box(modifier="fw".css().weight(1f)){
			key(tab){
				when(tab){
					"history"->HistoryScreen(nav,embedded=true)
					else->FilterScreen(nav,id=tab,embedded=true)
				}
			}
		}
	}
}

// ════════════════════════════════════════════════════════════════
// 历史记录页
// ════════════════════════════════════════════════════════════════
// embedded 为 true 时作为首页子内容嵌入（无顶栏），false 时独立页面
@Composable fun HistoryScreen(nav:NavController,embedded:Boolean=false){
	val c=Fyan.LC.current
	var cc by remember{mutableStateOf(false)}
	var ct by remember{mutableStateOf<String?>(null)}
	val x=LocalConfiguration.current
	val cs=when{
		x.screenWidthDp>=840->5 // TV / 大平板
		x.screenWidthDp>=600->4 // 折叠手机展开 / 平板
		else->3 // 手机
	}
	Column(modifier="fs".css().background(c.b)){
		if(!embedded){
			TopBar(
				title="历史记录",onBack={nav.popBackStack()},
				end={
					IconBtn(
						label="🗑",onClick={cc=true},
						modifier="fw36 fh36 c8".css().background(c.sv),
					)
				}
			)
		}else{ // 嵌入模式下保留一个轻量工具栏
			Row(
				modifier="fw h40 ph12".css().background(c.s),
				verticalAlignment=Alignment.CenterVertically,
				horizontalArrangement=Arrangement.SpaceBetween,
			){
				BasicText("历史记录",style=Fyan.TS.copy(color=c.os))
				IconBtn(
					label="🗑",onClick={cc=true},
					modifier="fw32 fh32 c8".css().background(c.sv),
				)
			}
		}
		if(Fyan.history.isEmpty()){
			Box(modifier="fs".css(),contentAlignment=Alignment.Center){
				BasicText("暂无观看记录",style=Fyan.BM.copy(color=c.os.copy(alpha=0.4f)))
			}
		}else{
			LazyVerticalGrid(
				modifier="fw".css().weight(1f),
				columns=GridCells.Fixed(cs),
				contentPadding=PaddingValues(12.dp),
				verticalArrangement=Arrangement.spacedBy(10.dp),
				horizontalArrangement=Arrangement.spacedBy(10.dp),
			){
				gridItems(Fyan.history,key={it.id}){o->
					VideoCard(
						title=o.title,poster=o.poster,
						sub=o.progress,modifier="fw".css(),
						onClick={
							Fyan.log("History","点击 ${o.id}",'u')
							nav.navigate("detail/${o.id}")
						},
						onLongPress={ct=o.id},
					)
				}
			}
		}
	}
	// 对话框
	if(cc)ConfirmDialog(
		text="确认清空所有历史记录？",
		onDismiss={cc=false},onConfirm={clearHistory();cc=false},
	)
	ct?.let{key->
		ConfirmDialog(
			text="确认删除该条历史记录？",
			onDismiss={ct=null},onConfirm={removeHistory(key);ct=null},
		)
	}
}

// ════════════════════════════════════════════════════════════════
// 视频筛选列表页
// ════════════════════════════════════════════════════════════════
@Composable fun FilterScreen(nav:NavController,id:String,embedded:Boolean=false){
	val c=Fyan.LC.current
	val tab=NAV_TABS.find{it.id==id}

	// 状态
	var gs by remember{mutableStateOf<List<FilterGroup>>(emptyList())}
	// 每组当前选中的 option id
	var ids by remember{mutableStateOf<List<String>>(emptyList())}
	var vs by remember{mutableStateOf<List<VideoListItem>>(emptyList())}
	var lm by remember{mutableStateOf(false)}
	var hm by remember{mutableStateOf(true)}
	var page by remember{mutableIntStateOf(1)}
	var loading by remember{mutableStateOf(true)}
	val ls=androidx.compose.foundation.lazy.grid.rememberLazyGridState()
	val iss=ids.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")}

	LaunchedEffect(id){ // 初始加载 tag
		loading=true
		val s=fetchFilterTags(id)
		gs=s
		ids=s.map{"0" } // 全部默认
		vs=fetchVideoList(id,ids.joinToString(","),1) // 加载第一页
		page=1
		hm=vs.size>=21
		loading=false
		Fyan.log("FilterScreen","tab=$id list=${s.size}",'i')
	}

	suspend fun reload(){ // 筛选变化重载
		loading=true
		vs=fetchVideoList(id,iss,1)
		page=1
		hm=vs.size>=21
		loading=false
	}

	// 触底加载更多
	LaunchedEffect(ls.firstVisibleItemIndex){
		val layout=ls.layoutInfo
		val last=layout.visibleItemsInfo.lastOrNull()?.index?:return@LaunchedEffect
		if(last>=layout.totalItemsCount-3&&hm&&!lm&&!loading){
			lm=true
			val next=fetchVideoList(id,iss,page+1)
			if(next.isNotEmpty()){vs=vs+next;page++}
			else hm=false
			lm=false
		}
	}

	val x=LocalConfiguration.current
	val cs=when{
		x.screenWidthDp>=840->6
		x.screenWidthDp>=600->4
		else->3
	}

	Column(modifier="fs".css().background(c.b)){
		if(!embedded)TopBar(title=tab?.label ?:"",onBack={nav.popBackStack()})
		// 筛选 Tab 栏
		if(gs.isNotEmpty()){
			Column(modifier="fw".css().background(c.s).border(0.5.dp,c.ov)){
				gs.forEachIndexed{i,g->
					FilterTabRow(
						fixedLabel=g.name,
						tabs=g.options.map{it.id to it.label},
						selected=ids.getOrElse(i){"0"},
						onSelect={ii->
							ids=ids.toMutableList().also{it[i]=ii}
							kotlinx.coroutines.MainScope().launch{reload()}
						},
					)
					if(i<gs.lastIndex)Box(modifier="fw h0.5".css().background(c.ov))
				}
			}
		}
		// 视频宫格
		Box(modifier="fw".css().weight(1f)){
			when{
				loading->LoadingCenter("加载视频列表…")
				vs.isEmpty()->Box(modifier="fs".css(),contentAlignment=Alignment.Center){
					BasicText("暂无视频",style=Fyan.BM.copy(color=c.os.copy(alpha=0.4f)))
				}else->LazyVerticalGrid(
					state=ls,
					modifier="fw".css().weight(1f),
					columns=GridCells.Fixed(cs),
					contentPadding=PaddingValues(10.dp),
					verticalArrangement=Arrangement.spacedBy(8.dp),
					horizontalArrangement=Arrangement.spacedBy(8.dp),
				){
					gridItems(vs,key={it.id}){o->
						VideoCard(
							poster=o.poster,title=o.title,modifier="fw".css(),onClick={
								addHistory(Fyan.VideoItem(o.id,o.title,o.poster))
								nav.navigate("detail/${o.id}")
							},
							sub=listOfNotNull(
								o.score.takeIf{it.isNotEmpty()},
								o.update.takeIf{it.isNotEmpty()},
							).joinToString(" · "),
						)
					}
					if(lm)item{LoadingCenter("加载更多…")}
				}
			}
		}
	}
}

// ════════════════════════════════════════════════════════════════
// 视频详情页
// ════════════════════════════════════════════════════════════════
@Composable
fun DetailScreen(nav:NavController,id:String){
	val (c,x)=Fyan.LC.current to LocalConfiguration.current
	val tt=x.screenWidthDp>=600
	val lp=x.orientation==Configuration.ORIENTATION_LANDSCAPE
	val tv=(x.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION

	var o by remember{mutableStateOf<VideoDetail?>(null)}
	var loading by remember{mutableStateOf(true)}
	var p by remember{mutableIntStateOf(0)}

	LaunchedEffect(id){
		loading=true
		o=fetchVideoDetail(id)
		loading=false
		Fyan.log("DetailScreen","id=$id",'i')
		o?.let{ // 更新历史进度（首集）
			addHistory(Fyan.VideoItem(id=it.id,title=it.title,poster=it.poster,progress="第1集"))
		}
	}

	Column(modifier="fs".css().background(c.b)){
		TopBar(title=o?.title?:"视频详情",onBack={nav.popBackStack()})
		if(loading)LoadingCenter("加载视频详情…")
		else if(o==null)Box(modifier="fs".css(),contentAlignment=Alignment.Center){
			BasicText("加载失败",style=Fyan.BM.copy(color=c.os.copy(alpha=0.5f)))
		}else{
			val d=o!!
			if(tv||(tt&&lp)){ // TV / 横屏平板布局
				TvLayout(d,p){i->
					p=i;addHistory(Fyan.VideoItem(d.id,d.title,d.poster,"第${i+1}集"))
				}
			}else{ // 手机 / 竖屏布局
				PhoneLayout(d,p){i->
					p=i;addHistory(Fyan.VideoItem(d.id,d.title,d.poster,"第${i+1}集"))
				}
			}
		}
	}
}

// TV 布局
@Composable private fun TvLayout(d:VideoDetail,episode:Int,onEpisode:(Int)->Unit){
	val c=Fyan.LC.current
	Row(modifier="fs".css()){
		// 左列：播放器 2/3 宽
		Column(modifier="fh".css().weight(2f)){
			Box( // 播放面板（16:9）
				modifier="fw".css().aspectRatio(16f/9f)
					.background(androidx.compose.ui.graphics.Color.Black),
				contentAlignment=Alignment.Center,
			){
				VideoPlayerPlaceholder(
					poster=d.poster,src=d.episodes.getOrNull(episode)?:"",
				)
			}
			// 集数（水平滚动，单行）
			LazyRow(
				modifier="fw".css().weight(1f),
				horizontalArrangement=Arrangement.spacedBy(8.dp),
				contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp),
			){
				items(d.episodeTitles.indices.toList()){i->
					EpisodeBtn(
						label=d.episodeTitles[i],active=i==episode,
						modifier="fw60 fh32".css(),onClick={onEpisode(i)},
					)
				}
			}
		}
		// 右列：简介
		Column(modifier="fh ph16 pv12".css().weight(1f).verticalScroll(rememberScrollState())){
			BasicText("简介",style=Fyan.TS.copy(color=c.os.copy(alpha=0.5f)))
			Spacer(modifier="h6".css())
			BasicText(d.desc,style=Fyan.BM.copy(color=c.os))
		}
	}
}

// 手机布局
@Composable private fun PhoneLayout(d:VideoDetail,episode:Int,onEpisode:(Int)->Unit){
	val c=Fyan.LC.current
	Column(modifier="fs".css().verticalScroll(rememberScrollState())){
		Box( // 播放面板（16:9，贴边）
			modifier="fw".css().aspectRatio(16f/9f)
				.background(androidx.compose.ui.graphics.Color.Black),
			contentAlignment=Alignment.Center,
		){VideoPlayerPlaceholder(poster=d.poster,src=d.episodes.getOrNull(episode)?:"")}
		// 简介
		Column(modifier="fw ph14 pv12".css()){
			BasicText("简介",style=Fyan.TS.copy(color=c.os.copy(alpha=0.5f)))
			Spacer(modifier="h4".css())
			BasicText(d.desc,style=Fyan.BM.copy(color=c.os))
		}
		Box(modifier="fw h0.5 mh14".css().background(c.ov))
		// 集数宫格（不可滚动，直接展开）
		if(d.episodeTitles.isNotEmpty()){
			Column(modifier="fw ph12 pv8".css()){
				BasicText("选集",style=Fyan.TS.copy(color=c.os.copy(alpha=0.5f)))
				Spacer(modifier="h8".css())
				EpisodeGrid(titles=d.episodeTitles,current=episode,cs=5,onSelect=onEpisode)
			}
		}
		Spacer(modifier="h24".css())
	}
}

// 集数宫格（非滚动，用 Column+Row 平铺）
@Composable private fun EpisodeGrid(titles:List<String>,current:Int,cs:Int,onSelect:(Int)->Unit){
	val rows=(titles.size+cs-1)/cs
	Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
		repeat(rows){row->
			Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
				repeat(cs){col->
					val i=row*cs+col
					if(i<titles.size){
						EpisodeBtn(
							label=titles[i],active=i==current,onClick={onSelect(i)},
							modifier="fw".css().weight(1f).then("fh34".css()),
						)
					}else Spacer(modifier=Modifer.weight(1f))
				}
			}
		}
	}
}

// 视频播放器占位（接入实际播放器时替换）
@Composable private fun VideoPlayerPlaceholder(poster:String,src:String){
	Box(modifier="fs".css(),contentAlignment=Alignment.Center){
		AsyncImage(
			model=poster,contentDescription="封面",
			contentScale=ContentScale.Fit,modifier="fs".css(),
		)
		// 播放图标叠层
		Box(
			modifier="fw56 fh56 c".css()
				.background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.5f)),
			contentAlignment=Alignment.Center,
		){BasicText("▶",style=Fyan.TL.copy(color=androidx.compose.ui.graphics.Color.White))}
	}
}
