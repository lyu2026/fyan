package com.fyan

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// 公共确认弹窗：title=标题，onOk=确定回调，onDismiss=取消回调
@Composable private fun ConfirmDialog(title:String,onDismiss:()->Unit,onOk:()->Unit){
	val cc=Fyan.cc;val ff=Fyan.ff
	Dialog(onDismissRequest=onDismiss){
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp)
			.clip(RoundedCornerShape(3.dp)).background(cc.cg)
			.border(1.dp,cc.bd,RoundedCornerShape(3.dp)).padding(20.dp),
			verticalArrangement=Arrangement.spacedBy(16.dp)
		){
			BasicText(title,style=ff.h3.copy(color=cc.c))
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
				BasicText("取消",modifier=Modifier.clickable{onDismiss()}.padding(8.dp),style=ff.p.copy(color=cc.c))
				BasicText("确定",modifier=Modifier.clickable{onOk()}.padding(8.dp),style=ff.p.copy(color=cc.primary))
			}
		}
	}
}

@Composable fun AyfHome(){ // 爱壹帆首页：TAB 导航 + 主面板路由
	val cs=rememberCoroutineScope();val cc=Fyan.cc;val ff=Fyan.ff
	val tabs=listOf("history" to "历史记录","movie" to "电影","drama" to "剧集","anime" to "动漫","variety" to "综艺","documentary" to "纪录片")
	val curr by Fyan.cg("ayf_tab","history").collectAsState(initial="history") // 当前选中 TAB，持久化
	Fyan.log("路由","进入爱壹帆首页")
	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
		Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(cc.cg).horizontalScroll(rememberScrollState()),
			verticalAlignment=Alignment.CenterVertically
		){
			tabs.forEach{(k,v)->
				val sel=k==curr
				Box(modifier=Modifier.fillMaxHeight()
					.background(if(sel)cc.primary.copy(alpha=0.15f)else cc.trans)
					.clickable{cs.launch{Fyan.cs("ayf_tab",k)}}, // 写入持久化 TAB 选项
					contentAlignment=Alignment.Center
				){BasicText("  $v  ",style=ff.h4.copy(color=if(sel)cc.primary else cc.c.copy(alpha=0.7f),fontWeight=if(sel)FontWeight.Bold else FontWeight.Normal))}
			}
		}
		Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd)) // TAB 与内容分割线
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			key(curr){when(curr){"history"->AyfHistory();else->AyfList(curr)}} // key 强制重组防状态残留
		}
	}
}

@Composable fun AyfHistory(){ // 历史记录页：宫格展示 + 长按单删 + 一键清空
	val cs=rememberCoroutineScope();val cc=Fyan.cc;val ff=Fyan.ff
	val cnum=if(Fyan.sw>=840)5 else if(Fyan.sw>=600)4 else 3 // 列数按屏幕宽度适配
	var delId by remember{mutableStateOf<String?>(null)} // 待删除记录 id
	var showClear by remember{mutableStateOf(false)} // 清空确认弹窗标志
	val raws by Fyan.cg("ayf_history","").collectAsState(initial="") // 历史记录原始字符串
	val list=remember(raws){ // 解析历史记录列表（空格分 6 列：id type title cover ec ps）
		if(raws.isBlank())emptyList()
		else raws.lines().filter{it.isNotBlank()}.mapNotNull{
			val x=it.trim().split(Regex("\\s+"),6)
			if(x.size<6)null else mapOf("id" to x[0],"type" to x[1],"title" to x[2],"cover" to x[3],"ec" to x[4],"ps" to x[5])
		}
	}
	Fyan.log("路由","进入爱壹帆历史记录页")
	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
		Row(modifier=Modifier.fillMaxWidth().height(30.dp).padding(start=4.dp),
			verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
			BasicText("记录清单",style=ff.pb.copy(color=cc.c))
			Box(modifier=Modifier.size(28.dp).clickable{showClear=true},contentAlignment=Alignment.Center){ // 清空按钮
				BasicText("🗑",style=ff.h4.copy(color=cc.c))
			}
		}
		if(list.isEmpty())Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ // 空态占位
			BasicText("暂无观看记录",style=ff.p.copy(color=cc.c.copy(alpha=0.4f)))
		}else LazyVerticalGrid(modifier=Modifier.fillMaxWidth().weight(1f),columns=GridCells.Fixed(cnum),
			contentPadding=PaddingValues(4.dp),verticalArrangement=Arrangement.spacedBy(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)
		){
			items(list,key={it["id"]!!}){o-> // key 保证复用稳定性
				Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(cc.cg)
					.pointerInput(o["id"]!!){detectTapGestures(
						onTap={Fyan.goto("ayf_info/"+o["id"])}, // 点击进入详情
						onLongPress={delId=o["id"]} // 长按触发单删确认
					)},horizontalAlignment=Alignment.CenterHorizontally
				){
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(cc.ag),contentAlignment=Alignment.BottomCenter){
						AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
						if(o["ec"]!="")Box(modifier=Modifier.padding(4.dp).background(cc.m,RoundedCornerShape(3.dp)).padding(horizontal=4.dp,vertical=2.dp)){ // 看到第几集
							BasicText("第"+((o["ec"]?.toIntOrNull()?:0)+1)+"集",style=ff.ps.copy(color=cc.white))
						}
					}
					BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,
						style=ff.ps.copy(color=cc.c,textAlign=TextAlign.Center))
				}
			}
		}
	}
	if(showClear)ConfirmDialog("清空历史记录？",onDismiss={showClear=false}){ // 清空确认弹窗
		Fyan.log("爱壹帆","执行清空历史记录",'w')
		cs.launch{Fyan.cs("ayf_history","")};showClear=false
	}
	if(delId!=null)ConfirmDialog("删除此记录？",onDismiss={delId=null}){ // 单条删除确认弹窗
		val nv=raws.lines().filter{it.isNotBlank()&&!it.startsWith("${delId!!} ")}.joinToString("\n")
		Fyan.log("爱壹帆","执行删除历史记录，编号: $delId",'d')
		cs.launch{Fyan.cs("ayf_history",nv)};delId=null
	}
}

@Composable fun AyfList(id:String){ // 分类筛选列表：多级筛选栏 + 宫格懒加载 + 无限分页
	val cs=rememberCoroutineScope();val cc=Fyan.cc;val ff=Fyan.ff
	var videos by remember{mutableStateOf(emptyList<Map<String,String>>())} // 视频列表
	var filters by remember{mutableStateOf(emptyList<List<Pair<String,String>>>())} // 筛选维度列表
	val curr=remember{mutableStateMapOf<Int,String>()} // 当前各维度选中值
	var loading by remember{mutableStateOf(true)} // 首次加载中
	var page by remember{mutableIntStateOf(1)} // 当前已加载页码
	var more by remember{mutableStateOf(true)} // 是否还有更多页
	var paging by remember{mutableStateOf(false)} // 分页加载中（防重入）
	val grid=rememberLazyGridState() // 宫格状态，用于触底检测

	Fyan.log("路由","进入爱壹帆筛选列表页")

	// 组装分页请求 URL，ids 按维度顺序拼接
	fun purl(p:Int=1)="https://api.iyf.tv/api/list/getconditionfilterdata?titleid=$id&ids=${curr.entries.sortedBy{it.key}.joinToString(","){it.value}}&page=$p&size=21"

	// 拉取视频列表（IO 线程，失败返回空列表）
	suspend fun vget(u:String):List<Map<String,String>>=withContext(Dispatchers.IO){
		runCatching<List<Map<String,String>>>{
			Fyan.log("爱壹帆","获取筛选列表: $u",'d')
			val j=JSONObject(Fyan.fetch(u)).optJSONObject("data")?:return@runCatching emptyList()
			val s=j.optJSONArray("list")?:return@runCatching emptyList()
			buildList{for(i in 0 until s.length()){val v=s.getJSONObject(i)
				add(mapOf("id" to v.optString("mediaKey"),"type" to v.optString("videoType","1"),
					"title" to v.optString("title"),"cover" to v.optString("coverImgUrl"),
					"score" to v.optString("score"),"tip" to v.optString("updateStatus")))}}
		}.getOrElse{emptyList()}
	}

	LaunchedEffect(id){ // TAB 切换时：拉筛选维度 → 初始化选项 → 加载第一页
		filters=withContext(Dispatchers.IO){
			runCatching<List<List<Pair<String,String>>>>{
				Fyan.log("爱壹帆","获取筛选维度，TAB: $id",'s')
				val j=JSONObject(Fyan.fetch("https://api.iyf.tv/api/list/getfiltertagsdata?SecondaryCode=$id")).optJSONObject("data")?:return@runCatching emptyList()
				val s=j.optJSONArray("list")?:return@runCatching emptyList()
				buildList{for(i in 0 until s.length()){val z=s.getJSONObject(i).optJSONArray("list")?:continue
					add(buildList{for(r in 0 until z.length()){val o=z.getJSONObject(r);add(o.optString("classifyId","0") to o.optString("classifyName",""))}})}}
			}.getOrElse{emptyList()}
		}
		filters.indices.forEach{i->curr[i]="0"} // 初始化各维度为全部
		page=1;more=true;videos=vget(purl(1));loading=false
	}

	LaunchedEffect(grid){ // 监听宫格末项索引，触底自动加载下一页
		snapshotFlow{grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index}.collect{li->
			if(li!=null&&li>=videos.size-Fyan.gc*2&&more&&!paging&&!loading){
				paging=true;val next=page+1
				Fyan.log("爱壹帆","触底加载第${next}页",'d')
				val s=vget(purl(next))
				if(s.isEmpty()){more=false;Fyan.log("爱壹帆","已全部加载",'s')}
				else{videos=videos+s;page=next}
				paging=false
			}
		}
	}

	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
		if(filters.isNotEmpty())Column(modifier=Modifier.fillMaxWidth().background(cc.cg)){ // 筛选栏
			filters.forEachIndexed{i,g->
				Row(modifier=Modifier.fillMaxWidth().height(20.dp).horizontalScroll(rememberScrollState()),
					verticalAlignment=Alignment.CenterVertically){
					g.forEach{(fi,fn)->val sel=fi==curr[i]
						Box(modifier=Modifier.fillMaxHeight()
							.background(if(sel)cc.primary.copy(alpha=0.15f)else cc.trans)
							.clickable{curr[i]=fi;cs.launch{loading=true;page=1;more=true;videos=vget(purl(1));loading=false}}, // 筛选变更重置列表
							contentAlignment=Alignment.Center
						){BasicText("  $fn  ",style=ff.ps.copy(color=if(sel)cc.primary else cc.c.copy(alpha=0.7f),fontWeight=if(sel)FontWeight.W600 else FontWeight.W400))}
					}
				}
				Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd)) // 筛选行分割线
			}
		}
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			when{
				loading->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ // 首次加载中
					BasicText("◌ 加载中…",style=ff.pb.copy(color=cc.c.copy(alpha=0.4f)))
				}
				videos.isEmpty()->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ // 无数据
					BasicText("暂无视频",style=ff.p.copy(color=cc.c.copy(alpha=0.4f)))
				}
				else->LazyVerticalGrid(state=grid,columns=GridCells.Fixed(Fyan.gc),
					contentPadding=PaddingValues(4.dp),verticalArrangement=Arrangement.spacedBy(3.dp),
					horizontalArrangement=Arrangement.spacedBy(3.dp)){
					items(videos,key={it["id"]!!}){o-> // key 保证滚动位置稳定
						Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(cc.cg)
							.clickable{Fyan.log("爱壹帆","进入详情: "+o["id"]);Fyan.goto("ayf_info/"+o["id"])},
							horizontalAlignment=Alignment.CenterHorizontally){
							Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(cc.ag)){
								AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
								if(!o["score"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.TopStart).padding(4.dp)
									.background(cc.m,RoundedCornerShape(10.dp)).padding(5.dp,2.dp)){ // 评分角标
									BasicText(o["score"]!!,style=ff.ps.copy(color=cc.c,fontFamily=FontFamily.Monospace))
								}
								if(!o["tip"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.BottomCenter).padding(4.dp)
									.background(cc.m,RoundedCornerShape(3.dp)).padding(horizontal=4.dp,vertical=2.dp)){ // 更新状态标签
									BasicText(o["tip"]!!,style=ff.ps.copy(color=cc.c))
								}
							}
							BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,
								overflow=TextOverflow.Ellipsis,style=ff.ps.copy(color=cc.c,textAlign=TextAlign.Center))
						}
					}
					if(paging)item(span={GridItemSpan(maxLineSpan)}){ // 底部分页加载指示器
						Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){
							BasicText("◌ 加载更多…",style=ff.ps.copy(color=cc.c.copy(alpha=0.4f)))
						}
					}
					if(!more&&videos.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){ // 底部已加载完提示
						Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){
							BasicText("꧁ 已加载全部 ꧂",style=ff.ps.copy(color=cc.c.copy(alpha=0.3f)))
						}
					}
				}
			}
		}
	}
}

@Composable fun AyfInfo(id:String){ // 视频详情播放页：支持手机/TV 双布局 + 全屏 + 断点续播
	val cs=rememberCoroutineScope();val cc=Fyan.cc;val ff=Fyan.ff
	val activity=LocalContext.current as? Activity
	var oo by remember{mutableStateOf<Map<String,Any>?>(null)} // 视频详情数据
	var loading by remember{mutableStateOf(true)} // 详情加载中
	var ctime by rememberSaveable{mutableLongStateOf(0L)} // 当前集断点播放位置（ms）
	var sidx by rememberSaveable{mutableIntStateOf(-1)} // 当前选中集索引
	var surl by rememberSaveable{mutableStateOf("")} // 当前集播放链接
	var cover by rememberSaveable{mutableStateOf("")} // 横向封面图链接（16:9裁剪）
	var playing by rememberSaveable{mutableStateOf(false)} // 是否正在播放
	var fscreen by rememberSaveable{mutableStateOf(false)} // 是否全屏模式

	Fyan.log("路由","进入爱壹帆视频详情页")

	LaunchedEffect(id){ // 视频 id 变化时：拉详情 → 恢复/写入历史记录 → 设置初始集数和断点
		loading=true;playing=false;surl=""
		oo=withContext(Dispatchers.IO){
			runCatching<Map<String,Any>?>{
				val j=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/videodetails?mediaKey=$id"))
					.optJSONObject("data")?.optJSONObject("detailInfo")?:return@runCatching null
				val x=j.optJSONArray("episodes")
				val s=buildList{if(x!=null)for(i in 0 until x.length()){val v=x.getJSONObject(i)
					add(mapOf("id" to v.optString("episodeId"),"title" to v.optString("episodeTitle","${i+1}")))}}
				mapOf("id" to id,"type" to j.optString("videoType","1"),"title" to j.optString("title","未知"),
					"brief" to j.optString("introduce","空空如也"),"cover" to j.optString("coverImgUrl",""),"s" to s)
			}.getOrElse{null}
		}
		if(oo==null){loading=false;return@LaunchedEffect} // 拉取失败，提前结束
		cover=oo!!["cover"].toString()+"?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg"
		loading=false
		val raws=Fyan.co("ayf_history","")
		val me=raws.lines().firstOrNull{it.startsWith("$id ")}
		if(me.isNullOrBlank()){ // 首次播放：写入新记录，从第 0 集开始
			sidx=0;ctime=0L
			Fyan.cs("ayf_history",(listOf("$id ${oo!!["type"]} ${oo!!["title"]} ${oo!!["cover"]} 0 0")+raws.lines().filter{it.isNotEmpty()}).joinToString("\n"))
		}else{
			val x=me.trim().split(Regex("\\s+"),6)
			sidx=x.getOrElse(4){"0"}.toIntOrNull()?:0 // 恢复历史集数
			ctime=x.getOrElse(5){"0"}.toLongOrNull()?:0L // 恢复历史断点
		}
	}

	LaunchedEffect(sidx){ // 切集时：重置状态 → 恢复该集断点 → 拉播放链接 → 更新历史
		if(oo==null||sidx==-1)return@LaunchedEffect
		playing=false;surl=""
		val raws=Fyan.co("ayf_history","")
		val me=raws.lines().firstOrNull{it.startsWith("$id ")}
		if(me.isNullOrBlank())ctime=0L
		else{val x=me.trim().split(Regex("\\s+"),6)
			ctime=if((x.getOrElse(4){"0"}.toIntOrNull()?:0)==sidx)x.getOrElse(5){"0"}.toLongOrNull()?:0L else 0L}
		@Suppress("UNCHECKED_CAST") val vid=(oo!!["s"] as List<Map<String,String>>).getOrNull(sidx)?.get("id")?:""
		surl=withContext(Dispatchers.IO){ // 拉取该集播放地址，取第一个非空 mediaUrl
			runCatching<String>{
				val s=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/getplaydata?mediaKey=$id&videoId=$vid&videoType=${oo!!["type"]}"))
					.optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
				var u="";for(i in 0 until s.length()){val v=s.getJSONObject(i).optString("mediaUrl","");if(v.isNotEmpty()){u=v;break}};u
			}.getOrElse{""}
		}
		// 更新历史记录：将当前条置顶
		Fyan.cs("ayf_history",(listOf("$id ${oo!!["type"]} ${oo!!["title"]} ${oo!!["cover"]} $sidx $ctime")+raws.lines().filter{it.isNotEmpty()&&!it.startsWith("$id ")}).joinToString("\n"))
	}

	// surl 变化时重建播放器；URL 为空则不建（避免空资源初始化）
	val player:ExoPlayer?=remember(surl){if(surl.isNotEmpty())ExoPlayer.Builder(Fyan.me).build()else null}
	LaunchedEffect(player,surl){ // 换源：设置 MediaSource → 断点寻道 → prepare
		player?.apply{
			val f=if(surl.contains(".m3u8",true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
				else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
			setMediaSource(f.createMediaSource(MediaItem.fromUri(Uri.parse(surl))))
			if(ctime>0L)seekTo(ctime)
			prepare()
		}
	}
	LaunchedEffect(player,playing){player?.playWhenReady=playing} // 同步封面点击与播放器状态
	LaunchedEffect(player){ // 每秒轮询当前播放位置，写入 ctime 用于断点持久化
		while(true){if(player?.isPlaying==true)ctime=player.currentPosition;kotlinx.coroutines.delay(1000)}
	}
	DisposableEffect(player){ // 组件销毁时：持久化最终进度 + 释放播放器
		onDispose{
			if(player!=null&&oo!=null){
				val now=player.currentPosition
				val(type,title,cv)=listOf(oo!!["type"],oo!!["title"],oo!!["cover"])
				CoroutineScope(Dispatchers.IO).launch{ // 使用独立 scope 防止 Composable scope 已取消
					val raws=Fyan.co("ayf_history","")
					Fyan.cs("ayf_history",(listOf("$id $type $title $cv $sidx $now")+raws.lines().filter{it.isNotEmpty()&&!it.startsWith("$id ")}).joinToString("\n"))
				}
			}
			player?.release() // 必须释放，防止 ExoPlayer 内存/资源泄漏
		}
	}

	@Suppress("UNCHECKED_CAST") val parts:List<Map<String,String>>=if(oo!=null)oo!!["s"] as List<Map<String,String>> else emptyList()

	// 集数按钮（TV 横排 LazyRow / 手机竖排 5列 Grid），抽取公用避免重复
	@Composable fun EpBtn(k:Int){ // 单集按钮
		val sel=k==sidx
		Box(modifier=Modifier.height(24.dp).clip(RoundedCornerShape(2.dp))
			.background(if(sel)cc.primary else cc.x).clickable{if(!sel)sidx=k},
			contentAlignment=Alignment.Center){
			BasicText(parts[k]["title"]!!,modifier=Modifier.fillMaxWidth().padding(2.dp),
				style=ff.px.copy(lineHeight=1.1.em,color=if(sel)cc.white else cc.c,fontWeight=if(sel)FontWeight.W600 else FontWeight.W400))
		}
	}

	// 播放器区域（封面→播放按钮→播放器→全屏），三态切换
	@Composable fun PlayerArea(allowController:Boolean,onClick:()->Unit){
		Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(cc.black),contentAlignment=Alignment.Center){
			when{
				surl.isEmpty()->BasicText("◌ 加载中...",style=ff.ps.copy(color=cc.c.copy(alpha=0.5f))) // 链接未就绪
				!playing->Box(modifier=Modifier.fillMaxSize().clickable{playing=true},contentAlignment=Alignment.Center){ // 封面+播放按钮
					AsyncImage(model=cover,contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
					Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(cc.m),contentAlignment=Alignment.Center){
						BasicText("▶",style=ff.h2.copy(color=cc.white))
					}
				}
				else->Box(modifier=Modifier.fillMaxSize().clickable{onClick()},contentAlignment=Alignment.Center){ // 播放中，点击全屏
					AndroidView(factory={ctx->PlayerView(ctx).apply{useController=allowController}},
						update={it.player=if(fscreen)null else player;if(!fscreen)it.requestFocus()}, // 全屏时置空防双轨
						modifier=Modifier.fillMaxSize())
				}
			}
		}
	}

	Box(modifier=Modifier.fillMaxSize()){
		Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
			Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(cc.cg),verticalAlignment=Alignment.CenterVertically){ // 顶部导航栏
				Box(modifier=Modifier.size(36.dp).padding(2.dp).clip(CircleShape).clickable{Fyan.nc.popBackStack()},contentAlignment=Alignment.Center){ // 返回按钮
					Image(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier=Modifier.size(20.dp),colorFilter=ColorFilter.tint(cc.c))
				}
				BasicText((oo?.get("title") as? String)?:"视频详情",modifier=Modifier.weight(1f).padding(start=3.dp),
					maxLines=1,overflow=TextOverflow.Ellipsis,style=ff.h4.copy(color=cc.c))
			}
			when{
				loading->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ // 加载中占位
					BasicText("◌ 加载视频详情…",style=ff.p.copy(color=cc.c.copy(alpha=0.5f)))
				}
				oo==null->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ // 加载失败
					Column(horizontalAlignment=Alignment.CenterHorizontally){
						BasicText("◑ 加载失败",style=ff.p.copy(color=cc.c.copy(alpha=0.5f)))
						Box(modifier=Modifier.padding(top=8.dp).clip(RoundedCornerShape(4.dp)).background(cc.ag)
							.clickable{loading=true}.padding(horizontal=16.dp,vertical=6.dp),contentAlignment=Alignment.Center){ // 重试按钮
							BasicText("重试",style=ff.p.copy(color=cc.primary))
						}
					}
				}
				Fyan.tv->Row(modifier=Modifier.fillMaxSize()){ // TV 横屏布局：播放器3/4 + 简介1/4
					Column(modifier=Modifier.fillMaxHeight().weight(3f)){
						PlayerArea(allowController=true){fscreen=true}
						if(parts.isNotEmpty())LazyRow(modifier=Modifier.fillMaxWidth(), // TV 集数横排
							horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(10.dp,8.dp)){
							items(parts.indices.toList()){k->Box(modifier=Modifier.width(50.dp)){EpBtn(k)}}
						}
					}
					Column(modifier=Modifier.fillMaxHeight().padding(16.dp,12.dp).weight(1f) // 简介区可滚动
						.verticalScroll(rememberScrollState()).clickable{}){
						BasicText("视频简介",style=ff.ps.copy(color=cc.c.copy(alpha=0.5f)))
						BasicText(oo!!["brief"] as String,modifier=Modifier.padding(top=6.dp),style=ff.p.copy(color=cc.c))
					}
				}
				else->Column(modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ // 手机竖屏布局
					PlayerArea(allowController=false){fscreen=true}
					Column(modifier=Modifier.padding(14.dp,12.dp)){ // 简介
						BasicText("视频简介",style=ff.ps.copy(color=cc.c.copy(alpha=0.5f)))
						BasicText(oo!!["brief"] as String,modifier=Modifier.padding(top=4.dp),style=ff.p.copy(color=cc.c))
					}
					Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal=14.dp).background(cc.bd)) // 分割线
					if(parts.isNotEmpty())Column(modifier=Modifier.padding(12.dp,8.dp)){ // 集数选择区
						BasicText("选集",style=ff.ps.copy(color=cc.c.copy(alpha=0.5f)))
						Column(modifier=Modifier.padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
							repeat((parts.size+4)/5){r-> // 每行 5 集，整除上取整行数
								Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
									repeat(5){ci->val i=r*5+ci
										if(i<parts.size)Box(modifier=Modifier.weight(1f)){EpBtn(i)} // 有效集按钮
										else Box(modifier=Modifier.weight(1f)) // 占位对齐
									}
								}
							}
						}
					}
					Box(modifier=Modifier.height(24.dp)) // 底部安全间距
				}
			}
		}

		if(fscreen)Box(modifier=Modifier.fillMaxSize().background(cc.black),contentAlignment=Alignment.Center){ // 全屏覆盖层
			if(!Fyan.tv)DisposableEffect(Unit){ // 手机全屏：强制横屏 + 隐藏状态栏
				Fyan.sbar?.invoke(false)
				activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
				onDispose{activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;Fyan.sbar?.invoke(true)}
			}
			AndroidView(factory={ctx->PlayerView(ctx).apply{useController=true // 全屏播放器，带控制栏
				setOnClickListener{if(isControllerFullyVisible)hideController() else showController()}}},
				update={it.player=player;it.requestFocus()},modifier=Modifier.fillMaxSize())
			if(!Fyan.tv)Box(modifier=Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp).clip(CircleShape) // 手机全屏左上角关闭按钮
				.background(cc.m).clickable{fscreen=false},contentAlignment=Alignment.Center){
				BasicText("╳",style=ff.p.copy(color=cc.white))
			}
			BackHandler{fscreen=false} // 返回键退出全屏
		}
	}
}