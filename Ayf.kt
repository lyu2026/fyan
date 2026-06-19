package com.fyan

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.activity.compose.BackHandler
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


@Composable fun AyfHome(){ // 爱壹帆首页
	val cs=rememberCoroutineScope();val cc=Fyan.cc;val ff=Fyan.ff
	// TAB栏数据
	val tabs=listOf("history" to "历史记录","movie" to "电影","drama" to "剧集","anime" to "动漫","variety" to "综艺","documentary" to "纪录片")
	// 当前选中项，默认: 历史记录
	val curr by Fyan.cg("ayf_tab","history").collectAsState(initial="history")

	Fyan.log("路由","进入爱壹帆首页")

	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
		Row( // 顶栏，TAB栏
			modifier=Modifier.fillMaxWidth().height(38.dp)
				.background(cc.cg).horizontalScroll(rememberScrollState()),
			verticalAlignment=Alignment.CenterVertically
		){
			tabs.forEach{o->Box(
				modifier=Modifier.fillMaxHeight()
					.background(if(o.first==curr)cc.primary.copy(alpha=0.15f)else cc.trans)
					.clickable{
						Fyan.log("爱壹帆","点击TAB: "+o.first,'u')
						cs.launch{Fyan.cs("ayf_tab",o.first)} // 修复原代码中未闭合的闭包语法缺陷
					},
				contentAlignment=Alignment.Center
			){
				BasicText("  ${o.second}  ",style=ff.h4.copy(
					color=if(o.first==curr)cc.primary else cc.c.copy(alpha=0.7f),
					fontWeight=if(o.first==curr)FontWeight.Bold else FontWeight.Normal
				))
			}
		}}
		// 分割线
		Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd))
		// 主面板
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			key(curr){
				when(curr){
					"history"->AyfHistory()
					else->AyfList(curr)
				}
			}
		}
	}
}

@Composable fun AyfHistory(){ // 爱壹帆历史记录
	val cs=rememberCoroutineScope();val cc=Fyan.cc;val ff=Fyan.ff
	// 列数，区分设备类型
	val cnum=if(Fyan.sw>=840)5 else if(Fyan.sw>=600)4 else 3
	// 要删除的记录的编号
	var id by remember{mutableStateOf<String?>(null)}
	// 是否需要清空
	var clear by remember{mutableStateOf(false)} // 改名规避主题变量 cc 命名冲突
	// 记录缓存字串
	val raws by Fyan.cg("ayf_history","").collectAsState(initial="")
	// 历史记录列表
	val list=remember(raws){
		if(raws.isBlank())emptyList()else raws.lines().filter{it.isNotBlank()}.mapNotNull{
			val x=it.trim().split(Regex("\\s+"),6)
			if(x.size<6)null else mapOf("id" to x[0],"type" to x[1],"title" to x[2],"cover" to x[3],"ec" to x[4],"ps" to x[5])
		}
	}

	Fyan.log("路由","进入爱壹帆历史记录页")

	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
		Row( // 顶栏，操作栏
			modifier=Modifier.fillMaxWidth().height(30.dp).padding(start=4.dp),
			verticalAlignment=Alignment.CenterVertically,
			horizontalArrangement=Arrangement.SpaceBetween
		){
			BasicText("记录清单",style=ff.pb.copy(color=cc.c))
			Box( // 清空按钮
				modifier=Modifier.size(28.dp).clickable{clear=true},
				contentAlignment=Alignment.Center
			){BasicText("🗑",style=ff.h4.copy(color=cc.c))}
		}
		if(list.isEmpty())Box( // 空内容占位
			modifier=Modifier.fillMaxSize(),
			contentAlignment=Alignment.Center
		){
			BasicText("暂无观看记录",style=ff.p.copy(color=cc.c.copy(alpha=0.4f)))
		}else LazyVerticalGrid( // 视频宫格
			modifier=Modifier.fillMaxWidth().weight(1f),
			columns=GridCells.Fixed(cnum),
			contentPadding=PaddingValues(4.dp),
			verticalArrangement=Arrangement.spacedBy(3.dp),
			horizontalArrangement=Arrangement.spacedBy(3.dp)
		){
			items(list){o->
				Column(
					modifier=Modifier.clip(RoundedCornerShape(2.dp))
						.background(cc.cg).pointerInput(o["id"]!!){
							detectTapGestures(
								onTap={Fyan.goto("ayf_info/"+o["id"])},
								onLongPress={id=o["id"]}
							)
						},
					horizontalAlignment=Alignment.CenterHorizontally
				){
					Box(
						modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(cc.ag),
						contentAlignment=Alignment.BottomCenter
					){
						AsyncImage( // 视频海报图
							model=o["cover"],contentDescription=null,
							modifier=Modifier.fillMaxSize(),
							contentScale=ContentScale.Crop
						)
						if(o["ec"]!="")Box( // 提示信息
							modifier=Modifier.padding(4.dp)
								.background(cc.m,RoundedCornerShape(3.dp))
								.padding(horizontal=4.dp,vertical=2.dp)
						){BasicText("第"+((o["ec"]?.toIntOrNull()?:0)+1)+"集",style=ff.ps.copy(color=cc.white))}
					}
					BasicText( // 标题
						o["title"]!!,
						modifier=Modifier.padding(4.dp),
						maxLines=1,overflow=TextOverflow.Ellipsis,
						style=ff.ps.copy(color=cc.c,textAlign=TextAlign.Center)
					)
				}
			}
		}
	}
	// 清空弹框提示
	if(clear)Dialog(onDismissRequest={clear=false}){
		Column(
			modifier=Modifier.fillMaxWidth().padding(32.dp)
				.clip(RoundedCornerShape(3.dp)).background(cc.cg)
				.border(1.dp,cc.bd,RoundedCornerShape(3.dp))
				.padding(20.dp),
			verticalArrangement=Arrangement.spacedBy(16.dp)
		){
			BasicText("清空历史记录？",style=ff.h3.copy(color=cc.c))
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
				BasicText("取消",
					modifier=Modifier.clickable{
						Fyan.log("爱壹帆","取消清空历史记录",'s')
						clear=false
					}.padding(8.dp),
					style=ff.p.copy(color=cc.c)
				)
				BasicText("确定",
					modifier=Modifier.clickable{
						Fyan.log("爱壹帆","执行清空历史记录",'w')
						cs.launch{Fyan.cs("ayf_history","")}
						clear=false
					}.padding(8.dp),
					style=ff.p.copy(color=cc.primary)
				)
			}
		}
	}
	// 删除单个记录弹框提示
	if(id!=null)Dialog(onDismissRequest={id=null}){
		Column(
			modifier=Modifier.fillMaxWidth().padding(32.dp)
				.clip(RoundedCornerShape(3.dp)).background(cc.cg)
				.border(1.dp,cc.bd,RoundedCornerShape(3.dp))
				.padding(20.dp),
			verticalArrangement=Arrangement.spacedBy(16.dp)
		){
			BasicText("删除此记录？",style=ff.h3.copy(color=cc.c))
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
				BasicText("取消",
					modifier=Modifier.clickable{
						Fyan.log("爱壹帆","取消删除历史记录，编号: $id",'e')
						id=null
					}.padding(8.dp),
					style=ff.p.copy(color=cc.c)
				)
				BasicText("确定",
					modifier=Modifier.clickable{
						val o=raws.lines().filter{it.isNotBlank()&&!it.startsWith("${id!!} ")}.joinToString("\n")
						Fyan.log("爱壹帆","执行删除历史记录，编号: $id",'d')
						cs.launch{Fyan.cs("ayf_history",o)}
						id=null
					}.padding(8.dp),
					style=ff.p.copy(color=cc.primary)
				)
			}
		}
	}
}

@Composable fun AyfList(id:String){ // 爱壹帆筛选列表
	val cs=rememberCoroutineScope();val cc=Fyan.cc;val ff=Fyan.ff
	// 视频列表
	var videos by remember{mutableStateOf(emptyList<Map<String,String>>())}
	// 筛选参数
	var filters by remember{mutableStateOf(emptyList<List<Pair<String,String>>>())}
	// 当前筛选
	val curr=remember{mutableStateMapOf<Int,String>()}
	// 加载中
	var loading by remember{mutableStateOf(true)}
	// 当前页码
	var page by remember{mutableIntStateOf(1)}
	// 还有更多
	var more by remember{mutableStateOf(true)}
	// 分页获取中
	var paging by remember{mutableStateOf(false)}
	// 宫格状态
	val grid=rememberLazyGridState()

	Fyan.log("路由","进入爱壹帆筛选列表页")

	// 组装列表分页链接
	fun purl(p:Int=1):String{
		var o=curr.entries.sortedBy{it.key}.joinToString(","){it.value}
		return "https://api.iyf.tv/api/list/getconditionfilterdata?titleid=$id&ids=$o&page=$p&size=21"
	}
	// 分页拉取视频列表
	suspend fun vget(u:String):List<Map<String,String>> = withContext(Dispatchers.IO){
		runCatching<List<Map<String,String>>>{
			Fyan.log("爱壹帆","获取筛选视频列表，链接: $u",'d')
			val j=JSONObject(Fyan.fetch(u)).optJSONObject("data")?:return@runCatching emptyList()
			val s=j.optJSONArray("list")?:return@runCatching emptyList()
			buildList{
				for(i in 0 until s.length()){
					val v=s.getJSONObject(i)
					add(mapOf("id" to v.optString("mediaKey",""),"type" to v.optString("videoType","1"),"title" to v.optString("title",""),"cover" to v.optString("coverImgUrl",""),"score" to v.optString("score",""),"tip" to v.optString("updateStatus","")))
				}
			}
		}.getOrElse{emptyList()}
	}

	LaunchedEffect(id){ // 监听TAB切换
		filters=withContext(Dispatchers.IO){
			runCatching<List<List<Pair<String,String>>>>{
				Fyan.log("爱壹帆","获取筛选数据，TAB: $id",'s')
				val j=JSONObject(Fyan.fetch("https://api.iyf.tv/api/list/getfiltertagsdata?SecondaryCode=$id")).optJSONObject("data")?:return@runCatching emptyList()
				val s=j.optJSONArray("list")?:return@runCatching emptyList()
				buildList{
					for(i in 0 until s.length()){
						val z=s.getJSONObject(i).optJSONArray("list")?:continue
						add(buildList{
							for(r in 0 until z.length()){
								val o=z.getJSONObject(r)
								add(o.optString("classifyId","0") to o.optString("classifyName",""))
							}
						})
					}
				}
			}.getOrElse{emptyList()}
		}
		filters.indices.forEach{i->curr[i]="0"}
		page=1;more=true;videos=vget(purl(1));loading=false
	}

	LaunchedEffect(grid){ // 监听宫格状态，触底载入下一页
		snapshotFlow{grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index}
			.collect{li->
				if(li!=null&&li>=videos.size-Fyan.gc*2&&more&&!paging&&!loading){
					paging=true
					val next=page+1
					Fyan.log("爱壹帆","触底加载第${next}页",'d')
					val s=vget(purl(next))
					if(s.isEmpty()){
						more=false
						Fyan.log("爱壹帆","已加载全部数据",'s')
					}else{
						videos=videos+s
						page=next
					}
					paging=false
				}
			}
	}

	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
		// 筛选栏
		if(filters.isNotEmpty())Column(modifier=Modifier.fillMaxWidth().background(cc.cg)){
			filters.forEachIndexed{i,g->
				Row( // 水平可滚动栏
					modifier=Modifier.fillMaxWidth().height(20.dp)
						.horizontalScroll(rememberScrollState()),
					verticalAlignment=Alignment.CenterVertically
				){
					g.forEach{(fi,fn)->
						val me=fi==curr[i] // 当前选中
						Box(
							modifier=Modifier.fillMaxHeight()
								.background(if(me)cc.primary.copy(alpha=0.15f)else cc.trans)
								.clickable{
									curr[i]=fi;cs.launch{loading=true;page=1;more=true;videos=vget(purl(1));loading=false}
								},
							contentAlignment=Alignment.Center
						){
							BasicText("  $fn  ",
								style=ff.ps.copy(
									color=if(me)cc.primary else cc.c.copy(alpha=0.7f),
									fontWeight=if(me)FontWeight.W600 else FontWeight.W400
								)
							)
						}
					}
				}
				// 分割线
				Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd))
			}
		}
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			when{
				// 加载中
				loading->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					BasicText("◌ 加载中…",style=ff.pb.copy(color=cc.c.copy(alpha=0.4f)))
				}
				// 空数据
				videos.isEmpty()->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					BasicText("暂无视频",style=ff.p.copy(color=cc.c.copy(alpha=0.4f)))
				}
				else->LazyVerticalGrid( // 渲染宫格
					state=grid,
					columns=GridCells.Fixed(Fyan.gc),
					contentPadding=PaddingValues(4.dp),
					verticalArrangement=Arrangement.spacedBy(3.dp),
					horizontalArrangement=Arrangement.spacedBy(3.dp)
				){
					items(videos){o->
						Column(
							modifier=Modifier.clip(RoundedCornerShape(2.dp))
								.background(cc.cg).clickable{
									Fyan.log("爱壹帆","进入详情页，编号: "+o["id"])
									Fyan.goto("ayf_info/"+o["id"])
								},
							horizontalAlignment=Alignment.CenterHorizontally
						){
							Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(cc.ag)){
								AsyncImage( // 海报图
									model=o["cover"],contentDescription=null,
									modifier=Modifier.fillMaxSize(),
									contentScale=ContentScale.Crop
								)
								if(!o["score"].isNullOrEmpty())Box( // 评分
									modifier=Modifier.align(Alignment.TopStart).padding(4.dp)
										.background(cc.m,RoundedCornerShape(10.dp)).padding(2.dp)
								){BasicText(o["score"]!!,style=ff.ps.copy(color=cc.c,fontFamily=FontFamily.Monospace))}
								if(!o["tip"].isNullOrEmpty())Box( // 提示
									modifier=Modifier.align(Alignment.BottomCenter).padding(4.dp)
										.background(cc.m,RoundedCornerShape(3.dp))
										.padding(horizontal=4.dp,vertical=2.dp)
								){BasicText(o["tip"]!!,style=ff.ps.copy(color=cc.c))}
							}
							BasicText(o["title"]!!, // 标题
								modifier=Modifier.padding(4.dp),maxLines=1,
								overflow=TextOverflow.Ellipsis,
								style=ff.ps.copy(color=cc.c,textAlign=TextAlign.Center)
							)
						}
					}
					// 显示底部加载中
					if(paging)item(span={GridItemSpan(maxLineSpan)}){
						Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){
							BasicText("◌ 加载更多…",style=ff.ps.copy(color=cc.c.copy(alpha=0.4f)))
						}
					}
					// 显示底部已载完
					if(!more&&videos.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){
						Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){
							BasicText("꧁ 已加载全部 ꧂",style=ff.ps.copy(color=cc.c.copy(alpha=0.3f)))
						}
					}
				}
			}
		}
	}
}

@Composable fun AyfInfo(id:String){ // 爱壹帆详情播放
	val cs=rememberCoroutineScope();val cc=Fyan.cc;val ff=Fyan.ff
	// 当前活动
	val activity=LocalContext.current as? Activity
	// 详情数据
	var oo by remember{mutableStateOf<Map<String,Any>?>(null)} // 视频详情数据
	// 详情加载中
	var loading by remember{mutableStateOf(true)}
	// 当前选中集的播放位置
	var ctime by rememberSaveable{mutableLongStateOf(0L)}
	// 当前选中集的索引
	var sidx by rememberSaveable{mutableIntStateOf(-1)}
	// 当前集播放链接
	var surl by rememberSaveable{mutableStateOf("")}
	// 当前海报图链接
	var cover by rememberSaveable{mutableStateOf("")}
	// 播放中
	var playing by rememberSaveable{mutableStateOf(false)}
	// 是否全屏播放状态
	var fscreen by rememberSaveable{mutableStateOf(false)}

	Fyan.log("路由","进入爱壹帆视频详情页")

	LaunchedEffect(id){ // 监听视频编号，写入历史记录
		loading=true;playing=false;surl=""
		oo=withContext(Dispatchers.IO){ // 拉取视频详情数据
			runCatching<Map<String,Any>?>{
				val j=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/videodetails?mediaKey=$id")).optJSONObject("data")?.optJSONObject("detailInfo")?:return@runCatching null
				val x=j.optJSONArray("episodes")
				val s=mutableListOf<Map<String,String>>()
				if(x!=null)for(i in 0 until x.length()){val v=x.getJSONObject(i);s.add(mapOf("id" to v.optString("episodeId",""),"title" to v.optString("episodeTitle","${i+1}")))}
				mapOf("id" to id,"type" to j.optString("videoType","1"),"title" to j.optString("title","未知"),"brief" to j.optString("introduce","空空如也"),"cover" to j.optString("coverImgUrl",""),"s" to s)
			}.getOrElse{null}
		}
		// 设置海报图链接，横向图
		cover=oo!!["cover"].toString()+"?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg"
		loading=false // 加载完成
		if(oo!=null){ // 记录历史
			val raws=Fyan.co("ayf_history","")
			val me=raws.lines().firstOrNull{it.startsWith("$id ")}
			if(me.isNullOrBlank()){ // 写入记录
				sidx=0;ctime=0L
				Fyan.cs("ayf_history",(listOf(id+" "+oo!!["type"]+" "+oo!!["title"]+" "+oo!!["cover"]+" 0 0")+raws.lines().filter{it.isNotEmpty()}).joinToString("\n"))
			}else{
				val x=me.trim().split(Regex("\\s+"),6)
				sidx=x.getOrElse(4){"0"}.toIntOrNull()?:0 // 设置历史记录中的集数
				ctime=x.getOrElse(5){"0"}.toLongOrNull()?:0L // 设置历史记录中的播放位置
			}
		}
	}

	LaunchedEffect(sidx){ // 监听集数，重置播放状态，拉取新集播放链接
		if(oo==null||sidx==-1)return@LaunchedEffect
		playing=false;surl=""
		val raws=Fyan.co("ayf_history","")
		val me=raws.lines().firstOrNull{it.startsWith("$id ")}
		if(me.isNullOrBlank())ctime=0L else{
			val x=me.trim().split(Regex("\\s+"),6)
			if(x.getOrElse(4){"0"}.toIntOrNull()?:0==sidx)ctime=x.getOrElse(5){"0"}.toLongOrNull()?:0L else ctime=0L
		}
		// 获取当前集数对应的视频编号
		@Suppress("UNCHECKED_CAST") val vid=(oo!!["s"] as List<Map<String,String>>).getOrNull(sidx)?.get("id")?:""
		val url=withContext(Dispatchers.IO){ // 获取播放链接
			runCatching<String>{
				val s=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/getplaydata?mediaKey=$id&videoId=$vid&videoType=${oo!!["type"]}")).optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
				var u=""
				for(i in 0 until s.length()){val v=s.getJSONObject(i).optString("mediaUrl","");if(v.isNotEmpty()){u=v;break}};u
			}.getOrElse{""}
		}
		surl=url
		// 更新历史记录
		Fyan.cs("ayf_history",(listOf(id+" "+oo!!["type"]+" "+oo!!["title"]+" "+oo!!["cover"]+" "+sidx+" "+ctime)+raws.lines().filter{it.isNotEmpty()&&!it.startsWith("${id} ")}).joinToString("\n"))
	}

	// 根据 URL 创建播放器实例
	val player:ExoPlayer?=remember(surl){if(surl.isNotEmpty())ExoPlayer.Builder(Fyan.me).build()else null}
	LaunchedEffect(player,surl){ // 统一管控换源与历史寻道控制
		player?.apply{
			val f=if(surl.contains(".m3u8",true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
			setMediaSource(f.createMediaSource(MediaItem.fromUri(Uri.parse(surl))))
			if(ctime>0L)seekTo(ctime)
			prepare()
			playWhenReady=true
		}
	}
	LaunchedEffect(player){ // 播放时定时轮询同步进度至 ctime
		while(true){
			if(player?.isPlaying==true)ctime=player.currentPosition
			kotlinx.coroutines.delay(1000)
		}
	}
	DisposableEffect(player){ // 组件销毁或重置换源时的现场清理与终极持久化
		onDispose{
			if(player!=null&&oo!=null){
				val now=player.currentPosition
				val type=oo!!["type"];val title=oo!!["title"];val cv=oo!!["cover"]
				kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch{
					val raws=Fyan.co("ayf_history","")
					Fyan.cs("ayf_history",listOf(id+" "+type+" "+title+" "+cv+" "+sidx+" "+ctime)+raws.lines().filter{it.isNotEmpty()&&!it.startsWith("$id ")}.joinToString("\n"))
				}
			}
			player?.release()
		}
	}

	// 当前集列表
	@Suppress("UNCHECKED_CAST") val parts:List<Map<String,String>> = if(oo!=null)oo!!["s"] as List<Map<String,String>> else emptyList()

	Box(modifier=Modifier.fillMaxSize()){
		Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
			Row( // 顶部导航栏
				modifier=Modifier.fillMaxWidth().height(38.dp).background(cc.cg),
				verticalAlignment=Alignment.CenterVertically
			){
				Box( // 返回箭头
					modifier=Modifier.size(26.dp).padding(6.dp).clip(CircleShape)
						.clickable{Fyan.nc.popBackStack()},
					contentAlignment=Alignment.Center
				){
					Image(
						painter=painterResource(R.drawable.arrow_back),
						contentDescription=null,modifier=Modifier.size(20.dp),
						colorFilter=ColorFilter.tint(cc.c)
					)
				}
				// 视频标题
				BasicText((oo?.get("title") as? String)?:"视频详情",
					modifier=Modifier.weight(1f).padding(start=3.dp),
					maxLines=1,overflow=TextOverflow.Ellipsis,
					style=ff.h4.copy(color=cc.c)
				)
			}
			when{
				loading->Box( // 加载中
					modifier=Modifier.fillMaxSize(),
					contentAlignment=Alignment.Center
				){
					BasicText("◌ 加载视频详情…",style=ff.p.copy(color=cc.c.copy(alpha=0.5f)))
				}
				oo==null->Box( // 加载失败，数据异常
					modifier=Modifier.fillMaxSize(),
					contentAlignment=Alignment.Center
				){
					Column(horizontalAlignment=Alignment.CenterHorizontally){
						BasicText("◑ 加载失败",style=ff.p.copy(color=cc.c.copy(alpha=0.5f)))
						Box( // 重试按钮
							modifier=Modifier.padding(top=8.dp)
								.clip(RoundedCornerShape(4.dp)).background(cc.ag)
								.clickable{loading=true}
								.padding(horizontal=16.dp,vertical=6.dp),
							contentAlignment=Alignment.Center
						){BasicText("重试",style=ff.p.copy(color=cc.primary))}
					}
				}
				Fyan.tv->Row( // TV 横屏布局
					modifier=Modifier.fillMaxSize()
				){
					// 播放器占屏宽四分之三
					Column(modifier=Modifier.fillMaxHeight().weight(3f)){
						Box(
							modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f)
								.background(cc.black),
							contentAlignment=Alignment.Center
						){
							// 获取视频源，加载中
							if(surl.isEmpty())BasicText("◌ 加载中...",style=ff.ps.copy(color=cc.c.copy(alpha=0.5f)))
							else if(!playing)Box( // 播放器已初始，视频播放区域，点击播放
								modifier=Modifier.fillMaxSize().clickable{playing=true},
								contentAlignment=Alignment.Center
							){
								AsyncImage( // 视频横向海报图
									model=cover,contentDescription=null,
									contentScale=ContentScale.Fit,
									modifier=Modifier.fillMaxSize()
								)
								Box( // 播放区域中心的播放按钮
									modifier=Modifier.size(56.dp).clip(CircleShape).background(cc.m),
									contentAlignment=Alignment.Center
								){BasicText("▶",style=ff.h2.copy(color=cc.white))}
							}else Box( // 播放器正在播放，点击全屏
								modifier=Modifier.fillMaxSize().clickable{fscreen=true},
								contentAlignment=Alignment.Center
							){
								AndroidView( // 播放器
									factory={ctx->PlayerView(ctx).apply{useController=true}},
									update={
										// 全屏切换播放丝滑
										it.player=if(fscreen)null else player
										// 非全屏状态可获焦
										if(!fscreen)it.requestFocus()
									},
									modifier=Modifier.fillMaxSize()
								)
							}
						}
						if(parts.isNotEmpty())LazyRow( // 视频集按钮栏
							modifier=Modifier.fillMaxWidth(),
							horizontalArrangement=Arrangement.spacedBy(8.dp),
							contentPadding=PaddingValues(10.dp,8.dp)
						){
							items(parts.indices.toList()){k->
								Box( // 集按钮
									modifier=Modifier.width(50.dp).height(24.dp)
										.clip(RoundedCornerShape(2.dp))
										.background(if(k==sidx)cc.primary else cc.x)
										.clickable{if(sidx!=k)sidx=k},
									contentAlignment=Alignment.Center
								){
									BasicText(parts[k]["title"]!!,
										style=ff.px.copy(
											lineHeight=1.1.em,
											color=if(k==sidx)cc.white else cc.c,
											fontWeight=if(k==sidx)FontWeight.W600 else FontWeight.W400
										),
										modifier=Modifier.fillMaxWidth().padding(2.dp)
									)
								}
							}
						}
					}
					Column( // 水平方向剩下区域为视频简介
						modifier=Modifier.fillMaxHeight()
							.padding(16.dp,12.dp).weight(1f)
							.verticalScroll(rememberScrollState())
							.clickable{}
					){
						BasicText("视频简介",style=ff.ps.copy(color=cc.c.copy(alpha=0.5f)))
						BasicText(oo!!["brief"] as String,modifier=Modifier.padding(top=6.dp),style=ff.p.copy(color=cc.c))
					}
				}
				else->Column( // 手机竖屏布局
					modifier=Modifier.fillMaxSize()
						.verticalScroll(rememberScrollState())
				){
					Box( // 播放器区域
						modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f)
							.background(cc.black),
						contentAlignment=Alignment.Center
					){
						// 加载中
						if(surl.isEmpty())BasicText("◌ 加载中...",style=ff.ps.copy(color=cc.c.copy(alpha=0.5f)))
						else if(!playing)Box( // 播放器已初始，视频播放区域，点击播放
							modifier=Modifier.fillMaxSize().clickable{playing=true},
							contentAlignment=Alignment.Center
						){
							AsyncImage( // 视频横向海报图
								model=cover,contentDescription=null,
								contentScale=ContentScale.Fit,
								modifier=Modifier.fillMaxSize()
							)
							Box( // 视频播放按钮
								modifier=Modifier.size(56.dp).clip(CircleShape)
									.background(cc.m),
								contentAlignment=Alignment.Center
							){BasicText("▶",style=ff.h2.copy(color=cc.white))}
						}else Box( // 播放器正在播放，点击全屏
							modifier=Modifier.fillMaxSize().clickable{fscreen=true},
							contentAlignment=Alignment.Center
						){
							AndroidView( // 播放器
								factory={ctx->PlayerView(ctx).apply{useController=false}},
								update={it.player=if(fscreen)null else player},
								modifier=Modifier.fillMaxSize()
							)
						}
					}
					// 视频简介
					Column(modifier=Modifier.padding(14.dp,12.dp)){
						BasicText("视频简介",style=ff.ps.copy(color=cc.c.copy(alpha=0.5f)))
						BasicText(oo!!["brief"] as String,
							modifier=Modifier.padding(top=4.dp),
							style=ff.p.copy(color=cc.c)
						)
					}
					// 分割线
					Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal=14.dp).background(cc.bd))
					// 视频集按钮栏
					if(parts.isNotEmpty())Column(modifier=Modifier.padding(12.dp,8.dp)){
						BasicText("选集",style=ff.ps.copy(color=cc.c.copy(alpha=0.5f)))
						Column(
							modifier=Modifier.padding(top=8.dp),
							verticalArrangement=Arrangement.spacedBy(6.dp)
						){
							repeat((parts.size+4)/5){r->
								Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
									repeat(5){cIdx->
										val i=r*5+cIdx
										if(i<parts.size)Box(
											modifier=Modifier.weight(1f).height(24.dp)
												.clip(RoundedCornerShape(2.dp))
												.background(if(i==sidx)cc.primary else cc.x)
												.clickable{if(sidx!=i)sidx=i},
											contentAlignment=Alignment.Center
										){
											BasicText(parts[i]["title"]!!,
												style=ff.px.copy(
													lineHeight=1.1.em,
													color=if(i==sidx)cc.white else cc.c,
													fontWeight=if(i==sidx)FontWeight.W600 else FontWeight.W400
												),
												modifier=Modifier.fillMaxWidth().padding(2.dp)
											)
										}else Box(modifier=Modifier.weight(1f))
									}
								}
							}
						}
					}
					// 占位空白
					Box(modifier=Modifier.height(24.dp))
				}
			}
		}

		if(fscreen)Box( // 全局全屏覆盖层
			modifier=Modifier.fillMaxSize().background(cc.black),
			contentAlignment=Alignment.Center
		){
			if(!Fyan.tv)DisposableEffect(Unit){
				Fyan.sbar?.invoke(false)
				activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
				onDispose{
					activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
					Fyan.sbar?.invoke(true)
				}
			} // 手机设备：全屏时启动横屏重力感应锁定，退出时自动恢复原始竖屏状态
			AndroidView( // 全屏播放器
				factory={ctx->
					PlayerView(ctx).apply{
						useController=true
						setOnClickListener{
							if(isControllerFullyVisible)hideController() else showController()
						}
					}
				},
				update={it.player=player;it.requestFocus()},
				modifier=Modifier.fillMaxSize()
			)
			if(!Fyan.tv){ // 手机全屏额外提供左上角极简关闭退出按钮
				Box(
					modifier=Modifier.align(Alignment.TopStart)
						.padding(8.dp).size(32.dp).clip(CircleShape)
						.background(cc.m).clickable{fscreen=false},
					contentAlignment=Alignment.Center
				){BasicText("╳",style=ff.p.copy(color=cc.white))}
			}
			BackHandler{fscreen=false}
		}
	}
}
