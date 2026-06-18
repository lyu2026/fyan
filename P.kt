package com.fyan

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

fun SF(u:String):String=java.net.URL(u).openStream().bufferedReader().use{it.readText()} // 同步HTTP读取文本，仅在IO协程中调用

@Composable fun AyfHome(){ // 主页（tab栏+内容区）
	val s=listOf("history" to "历史记录","movie" to "电影","drama" to "剧集","anime" to "动漫","variety" to "综艺","documentary" to "纪录片","news" to "新闻") // tab列表
	val c by Fyan.cg("ayf_tab","history").collectAsState(initial="history") // 当前选中tab，持久化
	val sc=rememberCoroutineScope()
	Column(Modifier.fillMaxSize().background(Fyan.cc.bg)){
		Row(Modifier.fillMaxWidth().height(38.dp).background(Fyan.cc.cg).horizontalScroll(rememberScrollState()),Arrangement.Start,Alignment.CenterVertically){ // tab横向滚动栏
			s.forEach{o->Box(Modifier.fillMaxHeight().background(if(o.first==c)Fyan.cc.fc.copy(alpha=0.15f)else Color.Transparent).padding(horizontal=16.dp).clickable{sc.launch{Fyan.cs("ayf_tab",o.first)}},Alignment.Center){
				BasicText(o.second,style=Fyan.ff.h4.copy(color=if(o.first==c)Fyan.cc.fc else Fyan.cc.c.copy(alpha=0.7f),fontWeight=if(o.first==c)FontWeight.Bold else FontWeight.Normal))
			}}
		}
		Box(Modifier.fillMaxWidth().weight(1f)){
			key(c){when(c){"history"->AyfHistory();else->AyfList(id=c)}} // key强制切tab时重建子页面，避免状态残留
		}
	}
}

@Composable fun AyfHistory(){ // 历史记录页（网格展示，支持单条删除/清空全部）
	val sc=rememberCoroutineScope()
	val rs by Fyan.cg("ayf_history","").collectAsState(initial="") // 历史原始字符串（每行：id type title cover ec ps）
	val vs=remember(rs){ // 解析历史条目为Map列表，至少需要6字段
		if(rs.isBlank())emptyList()else rs.lines().filter{it.isNotBlank()}.mapNotNull{
			val x=it.trim().split(Regex("\\s+"),6)
			if(x.size<6)null else mapOf("id" to x[0],"type" to x[1],"title" to x[2],"cover" to x[3],"ec" to x[4],"ps" to x[5])
		}
	}
	var id by remember{mutableStateOf<String?>(null)} // 长按待删除的条目id，null=无弹窗
	var cc by remember{mutableStateOf(false)} // 清空全部确认弹窗状态
	Column(Modifier.fillMaxSize().background(Fyan.cc.bg)){
		Row(Modifier.fillMaxWidth().height(30.dp).padding(horizontal=8.dp),Alignment.CenterVertically,Arrangement.SpaceBetween){ // 顶栏：标题+清空按钮
			BasicText("记录清单",style=Fyan.ff.h3.copy(color=Fyan.cc.c))
			Box(Modifier.size(28.dp).clickable{cc=true},Alignment.Center){BasicText("🗑",style=Fyan.ff.h4)}
		}
		if(vs.isEmpty())Box(Modifier.fillMaxSize(),Alignment.Center){
			BasicText("暂无观看记录",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.4f)))
		}else LazyVerticalGrid(GridCells.Fixed(Fyan.gc),Modifier.fillMaxWidth().weight(1f), // 使用Fyan.gc自适应列数
			contentPadding=PaddingValues(2.dp),verticalArrangement=Arrangement.spacedBy(2.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)){
			items(vs,{it["id"]!!}){o->
				Column(Modifier.clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).pointerInput(o["id"]!!){
					detectTapGestures(onTap={Fyan.goto("ayf_info/"+o["id"])},onLongPress={id=o["id"]}) // 点击进详情，长按删除
				},horizontalAlignment=Alignment.CenterHorizontally){
					Box(Modifier.fillMaxWidth().aspectRatio(0.7f).background(Fyan.cc.ag),Alignment.BottomCenter){ // 封面区
						AsyncImage(model=o["cover"],contentDescription=null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
						if(o["ec"]!="0")Box(Modifier.padding(4.dp).background(Color(0x80000000),RoundedCornerShape(1.dp)).padding(horizontal=4.dp,vertical=2.dp)){ // 非0才显示集数角标
							BasicText("第${o["ec"]}集",style=Fyan.ff.p12.copy(color=Color.White))
						}
					}
					BasicText(o["title"]!!,Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.p12.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
				}
			}
		}
	}
	if(cc)Dialog(onDismissRequest={cc=false}){ // 清空全部确认弹窗
		Column(Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(Fyan.cc.cg).padding(20.dp),Arrangement.spacedBy(16.dp)){
			BasicText("清空历史记录？",style=Fyan.ff.h4.copy(color=Fyan.cc.c))
			Row(Modifier.fillMaxWidth(),Arrangement.End){
				BasicText("取消",Modifier.clickable{cc=false}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				BasicText("确定",Modifier.clickable{sc.launch{Fyan.cs("ayf_history","")};cc=false}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.fc))
			}
		}
	}
	if(id!=null)Dialog(onDismissRequest={id=null}){ // 删除单条确认弹窗
		Column(Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(Fyan.cc.cg).padding(20.dp),Arrangement.spacedBy(16.dp)){
			BasicText("删除此记录？",style=Fyan.ff.h4.copy(color=Fyan.cc.c))
			Row(Modifier.fillMaxWidth(),Arrangement.End){
				BasicText("取消",Modifier.clickable{id=null}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				BasicText("确定",Modifier.clickable{ // 过滤掉当前id开头的行后写回
					sc.launch{Fyan.cs("ayf_history",rs.lines().filter{it.isNotBlank()&&!it.startsWith("${id!!} ")}.joinToString("\n"))}
					id=null
				}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.fc))
			}
		}
	}
}

@Composable fun AyfList(id:String){ // 视频筛选列表页（支持多维标签筛选）
	var vs by remember{mutableStateOf(emptyList<Map<String,String>>())} // 视频列表
	var fs by remember{mutableStateOf(emptyList<List<Pair<String,String>>>())} // 筛选条（外层=行，内层=选项pair<id,name>）
	val fc=remember{mutableStateMapOf<Int,String>()} // 各行当前选中的筛选id（初始全"0"=全部）
	var X by remember{mutableStateOf(true)} // 加载中状态
	val sc=rememberCoroutineScope()

	// 构造列表请求URL（新闻/普通分类不同端点）
	fun au()="https://api.iyf.tv/api"+(if(id=="news")"/home/getrelativevideosbysub?titleid=$id&Tags=${fc.values.joinToString(",")}"else"/list/getconditionfilterdata?titleid=$id&ids=${fc.values.joinToString(",")}")+"&page=1&size=21"

	// 解析视频列表JSON为统一Map格式
	suspend fun fv(u:String)=withContext(Dispatchers.IO){
		runCatching{
			val j=JSONObject(SF(u)).optJSONObject("data")?:return@runCatching emptyList<Map<String,String>>()
			val s=j.optJSONArray("list")?:return@runCatching emptyList<Map<String,String>>()
			buildList{for(i in 0 until s.length()){val v=s.getJSONObject(i)
				add(mapOf("id" to v.optString("mediaKey",""),"type" to v.optString("videoType","1"),"title" to v.optString("title",""),"cover" to v.optString("coverImgUrl",""),"score" to v.optString("score",""),"tip" to v.optString("updateStatus","")))}
		}.getOrElse{emptyList()}
	}

	LaunchedEffect(id){ // 初始化：加载筛选条 → 初始化选中态 → 加载列表
		fs=withContext(Dispatchers.IO){
			runCatching{
				if(id=="news")listOf(listOf("国际" to "国际","国内" to "国内","华人资讯" to "华人资讯","财经" to "财经","军事" to "军事"))
				else{
					val j=JSONObject(SF("https://api.iyf.tv/api/list/getfiltertagsdata?SecondaryCode=$id")).optJSONObject("data")?:return@runCatching emptyList<List<Pair<String,String>>>()
					val s=j.optJSONArray("list")?:return@runCatching emptyList<List<Pair<String,String>>>()
					buildList{for(i in 0 until s.length()){
						val z=s.getJSONObject(i).optJSONArray("list")?:continue
						add(buildList{for(r in 0 until z.length()){val o=z.getJSONObject(r);add(o.optString("classifyId","0") to o.optString("classifyName",""))}})
					}}
				}
			}.getOrElse{emptyList()}
		}
		fs.indices.forEach{i->fc[i]="0"} // 初始化每行选中为全部
		vs=fv(au());X=false
	}

	Column(Modifier.fillMaxSize().background(Fyan.cc.bg)){
		if(fs.isNotEmpty())Column(Modifier.fillMaxWidth().background(Fyan.cc.cg)){ // 筛选条区域
			fs.forEachIndexed{i,g->
				Row(Modifier.fillMaxWidth().height(24.dp).horizontalScroll(rememberScrollState()),Arrangement.Start,Alignment.CenterVertically){
					g.forEach{(fi,fn)->
						val at=fi==fc[i] // 是否为当前行选中项
						Box(Modifier.fillMaxHeight().background(if(at)Fyan.cc.fc.copy(alpha=0.15f)else Color.Transparent).clickable{
							fc[i]=fi;sc.launch{X=true;vs=fv(au());X=false} // 切换筛选项后重新请求
						}.padding(horizontal=12.dp),Alignment.Center){
							BasicText("  $fn  ",style=Fyan.ff.p12.copy(color=if(at)Fyan.cc.fc else Fyan.cc.c.copy(alpha=0.7f),fontWeight=if(at)FontWeight.W600 else FontWeight.W400))
						}
					}
				}
			}
		}
		Box(Modifier.fillMaxWidth().weight(1f)){
			when{
				X->Box(Modifier.fillMaxSize(),Alignment.Center){BasicText("加载中…",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.4f)))} // 加载中占位
				vs.isEmpty()->Box(Modifier.fillMaxSize(),Alignment.Center){BasicText("暂无视频",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.4f)))} // 空数据占位
				else->LazyVerticalGrid(GridCells.Fixed(Fyan.gc),contentPadding=PaddingValues(2.dp), // 使用Fyan.gc自适应列数
					verticalArrangement=Arrangement.spacedBy(2.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)){
					items(vs,{it["id"]!!}){o->
						Column(Modifier.padding(2.dp).clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).clickable{
							if(id!="news")Fyan.goto("ayf_info/"+o["id"]) // 新闻分类无详情页，不导航
						},horizontalAlignment=Alignment.CenterHorizontally){
							Box(Modifier.fillMaxWidth().aspectRatio(0.7f).background(Fyan.cc.ag),Alignment.BottomCenter){
								AsyncImage(model=o["cover"],contentDescription=null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
								val bd=listOfNotNull(o["score"]?.takeIf{it.isNotEmpty()},o["tip"]?.takeIf{it.isNotEmpty()}).joinToString(" · ")
								if(bd.isNotEmpty())Box(Modifier.padding(2.dp).background(Color(0x80000000),RoundedCornerShape(1.dp)).padding(4.dp,2.dp)){BasicText(bd,style=Fyan.ff.p12.copy(color=Color.White))} // 评分/更新状态角标
							}
							BasicText(o["title"]!!,Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.p12.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
						}
					}
				}
			}
		}
	}
}

@Composable fun AyfInfo(id:String){ // 视频详情页（封面+简介+选集+播放，TV/手机双布局）
	var O by remember{mutableStateOf<Map<String,Any>?>(null)} // 详情数据（null=加载中或失败）
	var X by remember{mutableStateOf(true)} // 加载状态
	var ec by remember{mutableIntStateOf(0)} // 当前选集索引
	var uc by remember{mutableStateOf("")} // 当前集播放URL
	var pr by remember{mutableStateOf(false)} // 是否已点击播放
	var fs by remember{mutableStateOf(false)} // 是否全屏（手机端）
	val hs by Fyan.cg("ayf_history","").collectAsState("") // 历史记录原始字符串（用于更新）
	val sc=rememberCoroutineScope()

	LaunchedEffect(id){ // 加载视频详情
		X=true;ec=0;pr=false;uc=""
		O=withContext(Dispatchers.IO){
			runCatching{
				val j=JSONObject(SF("https://api.iyf.tv/api/video/videodetails?mediaKey=$id")).optJSONObject("data")?.optJSONObject("detailInfo")?:return@runCatching null
				val x=j.optJSONArray("episodes") // 剧集列表
				val s=mutableListOf<Map<String,String>>()
				if(x!=null)for(i in 0 until x.length()){val v=x.getJSONObject(i);s.add(mapOf("id" to v.optString("episodeId",""),"title" to v.optString("episodeTitle","${i+1}")))}
				mapOf("id" to id,"type" to j.optString("videoType","1"),"title" to j.optString("title","未知"),"brief" to j.optString("introduce","空空如也"),"cover" to j.optString("coverImgUrl",""),"s" to s)
			}.getOrElse{null}
		}
		X=false
		if(O!=null){ // 更新历史记录：写入本次观看到首位，去除旧记录中的同id条目
			val t=O!!["title"] as String;val cv=O!!["cover"] as String;val tp=O!!["type"] as String
			Fyan.cs("ayf_history",(listOf("$id $tp $t $cv 0 0")+hs.lines().filter{it.isNotBlank()&&!it.startsWith("$id ")}).joinToString("\n"))
		}
	}

	LaunchedEffect(ec){ // 切集时解析播放地址
		if(O==null)return@LaunchedEffect
		pr=false;uc=""
		@Suppress("UNCHECKED_CAST") val ei=(O!!["s"] as List<Map<String,String>>).getOrNull(ec)?.get("id")?:"" // 当前集episodeId
		uc=withContext(Dispatchers.IO){
			runCatching{
				val s=JSONObject(SF("https://api.iyf.tv/api/video/getplaydata?mediaKey=$id&videoId=$ei&videoType=${O!!["type"]}")).optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
				var u="" // 取第一个非空mediaUrl
				for(i in 0 until s.length()){val v=s.getJSONObject(i).optString("mediaUrl","");if(v.isNotEmpty()){u=v;break}};u
			}.getOrElse{""}
		}
	}

	@Suppress("UNCHECKED_CAST") val sl=if(O!=null)(O!!["s"] as List<Map<String,String>>)else emptyList() // 剧集列表（只读，O有值时才使用）

	// ExoPlayer实例（uc变化时重建，Composable离开时自动释放）
	val P=if(uc.isNotEmpty())remember(uc){ExoPlayer.Builder(Fyan.me).build().apply{
		val f=if(uc.contains(".m3u8",true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
		setMediaSource(f.createMediaSource(MediaItem.fromUri(Uri.parse(uc))));prepare();playWhenReady=true
	}}else null
	if(P!=null)DisposableEffect(P){onDispose{P.release()}} // 离开页面时释放播放器资源

	Column(Modifier.fillMaxSize().background(Fyan.cc.bg)){
		Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal=2.dp).background(Fyan.cc.cg),Arrangement.Start,Alignment.CenterVertically){ // 顶部返回栏
			Box(Modifier.size(32.dp).clip(CircleShape).clickable{Fyan.nc.popBackStack()},Alignment.Center){ // 返回按钮
				Image(painterResource(R.drawable.arrow_back),null,Modifier.size(20.dp),colorFilter=ColorFilter.tint(Fyan.cc.c))
			}
			BasicText(O?.get("title")as?String?:"视频详情",Modifier.padding(horizontal=6.dp).weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.h4.copy(color=Fyan.cc.c))
		}
		when{
			X->Box(Modifier.fillMaxSize(),Alignment.Center){BasicText("加载视频详情…",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.5f)))} // 加载中
			O==null->Box(Modifier.fillMaxSize(),Alignment.Center){ // 加载失败
				Column(horizontalAlignment=Alignment.CenterHorizontally){
					BasicText("加载失败",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					Box(Modifier.padding(top=8.dp).clip(RoundedCornerShape(4.dp)).background(Fyan.cc.hv).clickable{sc.launch{X=true;val j=withContext(Dispatchers.IO){runCatching{JSONObject(SF("https://api.iyf.tv/api/video/videodetails?mediaKey=$id")).optJSONObject("data")?.optJSONObject("detailInfo")}.getOrNull()};if(j!=null){val x=j.optJSONArray("episodes");val s=mutableListOf<Map<String,String>>();if(x!=null)for(i in 0 until x.length()){val v=x.getJSONObject(i);s.add(mapOf("id" to v.optString("episodeId",""),"title" to v.optString("episodeTitle","${i+1}")))};O=mapOf("id" to id,"type" to j.optString("videoType","1"),"title" to j.optString("title","未知"),"brief" to j.optString("introduce","空空如也"),"cover" to j.optString("coverImgUrl",""),"s" to s)};X=false}}.padding(horizontal=16.dp,vertical=6.dp)){
						BasicText("重试",style=Fyan.ff.p.copy(color=Fyan.cc.fc))
					}
				}
			}
			Fyan.tv->Row(Modifier.fillMaxSize()){ // TV大屏布局：左侧3/4播放器+选集，右侧1/4简介
				Column(Modifier.fillMaxHeight().weight(3f)){
					Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),Alignment.Center){ // 播放窗
						if(uc.isEmpty())BasicText("加载中...",style=Fyan.ff.p12.copy(color=Color.White.copy(alpha=0.5f)))
						else if(!pr)Box(Modifier.fillMaxSize().clickable{pr=true},Alignment.Center){ // 封面+播放按钮
							AsyncImage(model=O!!["cover"],contentDescription=null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
							Box(Modifier.size(56.dp).clip(CircleShape).background(Color.Black.copy(0.5f)),Alignment.Center){BasicText("▶",style=Fyan.ff.h2.copy(color=Color.White))}
						}else AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=true;requestFocus()}},Modifier.fillMaxSize()) // TV端直接显示内置控制条
					}
					if(sl.isNotEmpty())LazyRow(Modifier.fillMaxWidth(),contentPadding=PaddingValues(10.dp,8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){ // 集数横向滚动条
						items(sl.indices.toList()){k->
							Box(Modifier.width(50.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(k==ec)Fyan.cc.fc else Fyan.cc.x).clickable{ec=k},Alignment.Center){
								BasicText(sl[k]["title"]!!,style=Fyan.ff.p12.copy(color=if(k==ec)Color.White else Fyan.cc.c,fontWeight=if(k==ec)FontWeight.W600 else FontWeight.W400))
							}
						}
					}
				}
				Column(Modifier.fillMaxHeight().weight(1f).padding(16.dp,12.dp).verticalScroll(rememberScrollState())){ // 右侧简介
					BasicText("视频简介",style=Fyan.ff.p12.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					BasicText(O!!["brief"] as String,Modifier.padding(top=6.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				}
			}
			else->Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ // 手机竖屏布局：上播放器，下内容滚动
				Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),Alignment.Center){ // 播放窗
					if(uc.isEmpty())BasicText("加载中...",style=Fyan.ff.p12.copy(color=Color.White.copy(alpha=0.5f)))
					else if(!pr)Box(Modifier.fillMaxSize().clickable{pr=true},Alignment.Center){ // 封面+播放按钮
						AsyncImage(model=O!!["cover"],contentDescription=null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
						Box(Modifier.size(56.dp).clip(CircleShape).background(Color.Black.copy(0.5f)),Alignment.Center){BasicText("▶",style=Fyan.ff.h2.copy(color=Color.White))}
					}else{ // 已播放：嵌入PlayerView，点击触发全屏
						Box(Modifier.fillMaxSize().clickable{fs=true},Alignment.Center){
							AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=false;requestFocus()}},Modifier.fillMaxSize())
						}
						if(fs)Dialog(onDismissRequest={fs=false},properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=true,dismissOnClickOutside=false)){ // 全屏弹窗（强制横屏）
							val ctx=LocalContext.current
							DisposableEffect(Unit){ // 进入全屏时强制横屏，退出时还原
								(ctx as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
								onDispose{(ctx as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
							}
							Box(Modifier.fillMaxSize().background(Color.Black),Alignment.Center){
								AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=true;requestFocus()}},Modifier.fillMaxSize()) // 全屏PlayerView带控制条
								Box(Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp).clip(CircleShape).background(Color.Black.copy(0.5f)).clickable{fs=false},Alignment.Center){
									BasicText("✕",style=Fyan.ff.p.copy(color=Color.White)) // 关闭全屏按钮
								}
							}
						}
					}
				}
				Column(Modifier.padding(14.dp,12.dp)){ // 简介块
					BasicText("视频简介",style=Fyan.ff.p12.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					BasicText(O!!["brief"] as String,Modifier.padding(top=4.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				}
				Box(Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal=14.dp).background(Fyan.cc.bd)) // 分割线（使用bd边框色）
				if(sl.isNotEmpty())Column(Modifier.padding(12.dp,8.dp)){ // 选集网格（每行5个，末行不足补空）
					BasicText("选集",style=Fyan.ff.p12.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					Column(Modifier.padding(top=8.dp),Arrangement.spacedBy(6.dp)){
						repeat((sl.size+4)/5){r->
							Row(Modifier.fillMaxWidth(),Arrangement.spacedBy(6.dp)){
								repeat(5){c->
									val i=r*5+c
									if(i<sl.size)Box(Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(i==ec)Fyan.cc.fc else Fyan.cc.x).clickable{ec=i},Alignment.Center){
										BasicText(sl[i]["title"]!!,style=Fyan.ff.p12.copy(color=if(i==ec)Color.White else Fyan.cc.c,fontWeight=if(i==ec)FontWeight.W600 else FontWeight.W400))
									}else Box(Modifier.weight(1f)) // 末行补占位空盒
								}
							}
						}
					}
				}
				Box(Modifier.height(24.dp)) // 底部留白
			}
		}
	}
}