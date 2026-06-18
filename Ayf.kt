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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


@Composable fun AyfHome(){ // 爱壹帆首页
	// Tab 定义：路由 id -> 显示名称
	val s=listOf("history" to "历史记录","movie" to "电影","drama" to "剧集","anime" to "动漫","variety" to "综艺","documentary" to "纪录片","news" to "新闻")
	// 从 DataStore 持久化读取上次选中的 Tab，默认历史记录
	val c by Fyan.cg("ayf_tab","history").collectAsState(initial="history")
	val sc=rememberCoroutineScope()
	Fyan.log("路由","进入爱壹帆首页")

	Column(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg)){
		// 横向可滚动 Tab 栏
		Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(Fyan.cc.cg).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
			s.forEach{o->Box(modifier=Modifier.fillMaxHeight().background(if(o.first==c)Fyan.cc.primary.copy(alpha=0.15f)else Color.Transparent).clickable{
				Fyan.log("爱壹帆","点击TAB: "+o.first,'u')
				// 持久化选中状态，重启后恢复
				sc.launch{Fyan.cs("ayf_tab",o.first)
			}},contentAlignment=Alignment.Center){
				BasicText("  ${o.second}  ",style=Fyan.ff.h4.copy(color=if(o.first==c)Fyan.cc.primary else Fyan.cc.c.copy(alpha=0.7f),fontWeight=if(o.first==c)FontWeight.Bold else FontWeight.Normal))
			}}
		}
		Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(Fyan.cc.bd))
		// key(c) 保证 Tab 切换时强制重建子组件，避免状态残留
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			key(c){when(c){"history"->AyfHistory();else->AyfList(id=c)}}
		}
	}
}

@Composable fun AyfHistory(){ // 爱壹帆历史记录
	// 响应式列数：平板840dp以上5列，600dp以上4列，手机3列
	val cn=if(Fyan.sw>=840)5 else if(Fyan.sw>=600)4 else 3
	var id by remember{mutableStateOf<String?>(null)} // 长按选中的条目 id（触发单条删除弹窗）
	var cc by remember{mutableStateOf(false)} // 控制清空确认弹窗
	val sc=rememberCoroutineScope()
	// 从 DataStore 读取历史记录字符串（每行一条，空格分隔各字段）
	val rs by Fyan.cg("ayf_history","").collectAsState(initial="")
	// 解析历史记录为 Map 列表；每行格式：id type title cover ec ps
	val vs=remember(rs){
		if(rs.isBlank())emptyList()else rs.lines().filter{it.isNotBlank()}.mapNotNull{
			val x=it.trim().split(Regex("\\s+"),6)
			if(x.size<6)null else mapOf("id" to x[0],"type" to x[1],"title" to x[2],"cover" to x[3],"ec" to x[4],"ps" to x[5])
		}
	}
	Fyan.log("路由","进入爱壹帆历史记录页")

	Column(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg)){
		// 顶部工具栏：标题 + 清空按钮
		Row(modifier=Modifier.fillMaxWidth().height(30.dp).padding(start=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
			BasicText("记录清单",style=Fyan.ff.pb.copy(color=Fyan.cc.c))
			Box(modifier=Modifier.size(28.dp).clickable{cc=true},contentAlignment=Alignment.Center){BasicText("🗑",style=Fyan.ff.h4.copy(color=Fyan.cc.c))}
		}
		if(vs.isEmpty())Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
			BasicText("暂无观看记录",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.4f)))
		}else LazyVerticalGrid(modifier=Modifier.fillMaxWidth().weight(1f),columns=GridCells.Fixed(cn),contentPadding=PaddingValues(4.dp),verticalArrangement=Arrangement.spacedBy(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
			items(vs,{it["id"]!!}){o->
				// 单个历史记录卡片：点击进入详情，长按触发删除确认
				Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).pointerInput(o["id"]!!){
					detectTapGestures(onTap={Fyan.goto("ayf_info/"+o["id"])},onLongPress={id=o["id"]})
				},horizontalAlignment=Alignment.CenterHorizontally){
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(Fyan.cc.ag),contentAlignment=Alignment.BottomCenter){
						AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
						// ec!=0 时显示上次观看集数角标
						if(o["ec"]!="0")Box(modifier=Modifier.padding(4.dp).background(Fyan.cc.m,RoundedCornerShape(1.dp)).padding(horizontal=4.dp,vertical=2.dp)){
							BasicText("第${o["ec"]}集",style=Fyan.ff.ps.copy(color=Color.White))
						}
					}
					BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.ps.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
				}
			}
		}
	}
	// 清空全部历史确认弹窗
	if(cc)Dialog(onDismissRequest={cc=false}){
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(Fyan.cc.cg).border(1.dp,Fyan.cc.bd,RoundedCornerShape(3.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
			BasicText("清空历史记录？",style=Fyan.ff.h3.copy(color=Fyan.cc.c))
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
				BasicText("取消",modifier=Modifier.clickable{Fyan.log("爱壹帆","取消清空历史记录",'s');cc=false}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				BasicText("确定",modifier=Modifier.clickable{Fyan.log("爱壹帆","执行清空历史记录",'w');sc.launch{Fyan.cs("ayf_history","")};cc=false}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.primary))
			}
		}
	}
	// 单条删除确认弹窗（id 不为 null 时显示）
	if(id!=null)Dialog(onDismissRequest={id=null}){
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(Fyan.cc.cg).border(1.dp,Fyan.cc.bd,RoundedCornerShape(3.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
			BasicText("删除此记录？",style=Fyan.ff.h3.copy(color=Fyan.cc.c))
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
				BasicText("取消",modifier=Modifier.clickable{Fyan.log("爱壹帆","取消删除历史记录，编号: $id",'e');id=null}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				BasicText("确定",modifier=Modifier.clickable{
					// 过滤掉该 id 开头的行，重新写入 DataStore
					val o=rs.lines().filter{it.isNotBlank()&&!it.startsWith("${id!!} ")}.joinToString("\n")
					Fyan.log("爱壹帆","执行删除历史记录，编号: $id",'d')
					sc.launch{Fyan.cs("ayf_history",o)};id=null
				}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.primary))
			}
		}
	}
}

@Composable fun AyfList(id:String){ // 爱壹帆筛选列表
	var vs by remember{mutableStateOf(emptyList<Map<String,String>>())} // 当前视频列表数据
	var fs by remember{mutableStateOf(emptyList<List<Pair<String,String>>>())} // 筛选条件组列表（多行）
	val fc=remember{mutableStateMapOf<Int,String>()} // 各筛选行当前选中的值（行索引 -> 选中 id）
	var X by remember{mutableStateOf(true)} // 是否正在加载（首屏或筛选切换）
	var pg by remember{mutableIntStateOf(1)} // 当前已加载页码
	var hm by remember{mutableStateOf(true)} // 是否还有更多数据（false 时不再触底加载）
	var lm by remember{mutableStateOf(false)} // 触底加载中标志（防止重复请求）
	val gs=rememberLazyGridState() // 网格滚动状态，用于触底检测
	val sc=rememberCoroutineScope()
	Fyan.log("路由","进入爱壹帆筛选列表页")

	// 构造当前筛选条件对应的 API 请求 URL（第1页）
	fun au(p:Int=1):String{
		var o=fc.entries.sortedBy{it.key}.joinToString(","){it.value}
		o=if(id=="news")"home/getrelativevideosbysub?titleid=$id&Tags=$o"else"list/getconditionfilterdata?titleid=$id&ids=$o"
		return "https://api.iyf.tv/api/$o&page=$p&size=21"
	}

	// 拉取指定页视频列表，返回条目 Map 列表
	suspend fun fv(u:String):List<Map<String,String>> = withContext(Dispatchers.IO){
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

	// 初始化：拉取筛选维度数据，再加载第1页视频列表
	LaunchedEffect(id){
		fs=withContext(Dispatchers.IO){
			runCatching<List<List<Pair<String,String>>>>{
				Fyan.log("爱壹帆","获取筛选数据，TAB: $id",'s')
				if(id=="news")listOf(listOf("国际" to "国际","国内" to "国内","华人资讯" to "华人资讯","财经" to "财经","军事" to "军事"))
				else{
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
				}
			}.getOrElse{emptyList()}
		}
		// 初始化各筛选行选中值为 "0"（全部）
		fs.indices.forEach{i->fc[i]="0"}
		// 重置分页状态并加载第1页
		pg=1;hm=true
		vs=fv(au(1));X=false
	}

	// 触底加载下一页（snapshotFlow 监听最后可见 item 索引）
	LaunchedEffect(gs){
		snapshotFlow{gs.layoutInfo.visibleItemsInfo.lastOrNull()?.index}
			.collect{li->
				// 当最后可见条目接近列表末尾且尚未加载中且还有更多数据时，触发翻页
				if(li!=null&&li>=vs.size-Fyan.gc*2&&hm&&!lm&&!X){
					lm=true // 标记加载中，防止重复触发
					val px=pg+1
					Fyan.log("爱壹帆","触底加载第${px}页",'d')
					val mr=fv(au(px))
					if(mr.isEmpty()){
						hm=false // 空结果说明已到最后一页
						Fyan.log("爱壹帆","已加载全部数据",'s')
					}else{
						vs=vs+mr // 追加到现有列表
						pg=px
					}
					lm=false
				}
			}
	}

	Column(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg)){
		// 筛选条件区域（多行横向滚动）
		if(fs.isNotEmpty())Column(modifier=Modifier.fillMaxWidth().background(Fyan.cc.cg)){
			fs.forEachIndexed{i,g->
				Row(modifier=Modifier.fillMaxWidth().height(20.dp).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
					g.forEach{(fi,fn)->
						val at=fi==fc[i] // 是否为当前行选中项
						Box(modifier=Modifier.fillMaxHeight().background(if(at)Fyan.cc.primary.copy(alpha=0.15f)else Color.Transparent).clickable{
							// 切换筛选项：重置到第1页，清空列表重新加载
							fc[i]=fi;sc.launch{X=true;pg=1;hm=true;vs=fv(au(1));X=false}
						},contentAlignment=Alignment.Center){
							BasicText("  $fn  ",style=Fyan.ff.ps.copy(color=if(at)Fyan.cc.primary else Fyan.cc.c.copy(alpha=0.7f),fontWeight=if(at)FontWeight.W600 else FontWeight.W400))
						}
					}
				}
				Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(Fyan.cc.bd))
			}
		}
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			when{
				// 首屏加载态
				X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					BasicText("◌ 加载中…",style=Fyan.ff.pb.copy(color=Fyan.cc.c.copy(alpha=0.4f)))
				}
				// 空结果态
				vs.isEmpty()->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					BasicText("暂无视频",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.4f)))
				}
				else->LazyVerticalGrid(state=gs,columns=GridCells.Fixed(Fyan.gc),contentPadding=PaddingValues(4.dp),
					verticalArrangement=Arrangement.spacedBy(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
					items(vs,{it["id"]!!}){o->
						// 视频卡片：封面 + 评分/更新状态角标 + 标题
						Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).clickable{
							Fyan.log("爱壹帆","进入详情页，编号: "+o["id"])
							// 新闻类不进详情页
							if(id!="news")Fyan.goto("ayf_info/"+o["id"])
						},horizontalAlignment=Alignment.CenterHorizontally){
							Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(Fyan.cc.ag)){
								AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
								if(o["score"]?.isNotEmpty())Box(modifier=Modifier.align(Alignment.TopStart).padding(2.dp).background(Fyan.cc.m,RoundedCornerShape(2.dp)).padding(2.dp)){
									BasicText(o["score"],style=Fyan.ff.ps.copy(color=Fyan.cc.c))
								}
								if(o["tip"]?.isNotEmpty())Box(modifier=Modifier.align(Alignment.BottomCenter).padding(4.dp,2.dp).background(Fyan.cc.m,RoundedCornerShape(2.dp)).padding(2.dp)){
									BasicText(o["tip"],style=Fyan.ff.ps.copy(color=Fyan.cc.c))
								}
							}
							BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.ps.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
						}
					}
					// 触底加载态尾部占位提示
					if(lm)item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}){
						Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){
							BasicText("◌ 加载更多…",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.4f)))
						}
					}
					// 已到底部提示
					if(!hm&&vs.isNotEmpty())item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}){
						Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){
							BasicText("◉ 已加载全部",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.3f)))
						}
					}
				}
			}
		}
	}
}

@Composable fun AyfInfo(id:String){ // 爱壹帆详情播放
	var O by remember{mutableStateOf<Map<String,Any>?>(null)} // 视频详情数据
	var X by remember{mutableStateOf(true)} // 详情加载中标志
	var ec by remember{mutableIntStateOf(0)} // 当前选中集数索引
	var uc by remember{mutableStateOf("")} // 当前集播放 URL
	var pr by remember{mutableStateOf(false)} // 是否已点击播放（显示播放器）
	var fs by remember{mutableStateOf(false)} // 是否全屏播放弹窗
	val hs by Fyan.cg("ayf_history","").collectAsState("") // 当前历史记录字符串
	val sc=rememberCoroutineScope()
	Fyan.log("路由","进入爱壹帆视频详情页")

	// 进入详情页：拉取视频详情数据，写入历史记录
	LaunchedEffect(id){
		X=true;ec=0;pr=false;uc=""
		O=withContext(Dispatchers.IO){
			runCatching<Map<String,Any>?>{
				val j=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/videodetails?mediaKey=$id")).optJSONObject("data")?.optJSONObject("detailInfo")?:return@runCatching null
				val x=j.optJSONArray("episodes")
				val s=mutableListOf<Map<String,String>>()
				// 解析集数列表（episodeId + episodeTitle）
				if(x!=null)for(i in 0 until x.length()){val v=x.getJSONObject(i);s.add(mapOf("id" to v.optString("episodeId",""),"title" to v.optString("episodeTitle","${i+1}")))}
				mapOf("id" to id,"type" to j.optString("videoType","1"),"title" to j.optString("title","未知"),"brief" to j.optString("introduce","空空如也"),"cover" to j.optString("coverImgUrl",""),"s" to s)
			}.getOrElse{null}
		}
		X=false
		// 写入历史记录：新条目置顶，去重同 id 旧条目
		if(O!=null){
			sc.launch{Fyan.cs("ayf_history",(listOf("$id ${O!!["type"]} ${O!!["title"]} ${O!!["cover"]} 0 0")+hs.lines().filter{it.isNotEmpty()&&!it.startsWith("$id ")}).joinToString("\n"))}
		}
	}

	// 切换集数：重置播放状态，拉取新集播放链接
	LaunchedEffect(ec){
		if(O==null)return@LaunchedEffect
		pr=false;uc=""
		@Suppress("UNCHECKED_CAST") val ei=(O!!["s"] as List<Map<String,String>>).getOrNull(ec)?.get("id")?:""
		uc=withContext(Dispatchers.IO){
			runCatching<String>{
				// 请求播放数据接口，取第一个非空 mediaUrl
				val s=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/getplaydata?mediaKey=$id&videoId=$ei&videoType=${O!!["type"]}")).optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
				var u=""
				for(i in 0 until s.length()){val v=s.getJSONObject(i).optString("mediaUrl","");if(v.isNotEmpty()){u=v;break}};u
			}.getOrElse{""}
		}
	}

	// 根据 URL 创建播放器：HLS（.m3u8）使用 HlsMediaSource，其余使用 ProgressiveMediaSource
	val P:ExoPlayer?=remember(uc){
		if(uc.isNotEmpty())ExoPlayer.Builder(Fyan.me).build().apply{
			val f=if(uc.contains(".m3u8",true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
			setMediaSource(f.createMediaSource(MediaItem.fromUri(Uri.parse(uc))));prepare();playWhenReady=true
		}else null
	}
	// 组件销毁时释放播放器资源，防止内存泄漏
	DisposableEffect(P){onDispose{P?.release()}}

	@Suppress("UNCHECKED_CAST") val sl:List<Map<String,String>> = if(O!=null)O!!["s"] as List<Map<String,String>> else emptyList()

	Column(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg)){
		// 顶部导航栏：返回箭头 + 视频标题
		Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(Fyan.cc.cg),verticalAlignment=Alignment.CenterVertically){
			Box(modifier=Modifier.size(32.dp).clip(CircleShape).padding(start=2.dp,end=6.dp).clickable{Fyan.nc.popBackStack()},contentAlignment=Alignment.Center){
				Image(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier=Modifier.size(20.dp),colorFilter=ColorFilter.tint(Fyan.cc.c))
			}
			BasicText((O?.get("title") as? String)?:"视频详情",modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.h4.copy(color=Fyan.cc.c))
		}
		Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(Fyan.cc.bd))
		when{
			// 加载态
			X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
				BasicText("◌ 加载视频详情…",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
			}
			// 加载失败态，提供重试入口
			O==null->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
				Column(horizontalAlignment=Alignment.CenterHorizontally){
					BasicText("◑ 加载失败",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					Box(modifier=Modifier.padding(top=8.dp).clip(RoundedCornerShape(4.dp)).background(Fyan.cc.ag).clickable{X=true}.padding(horizontal=16.dp,vertical=6.dp),contentAlignment=Alignment.Center){
						BasicText("重试",style=Fyan.ff.p.copy(color=Fyan.cc.primary))
					}
				}
			}
			// TV 横屏布局：左侧 3 份播放区 + 右侧 1 份简介区
			Fyan.tv->Row(modifier=Modifier.fillMaxSize()){
				Column(modifier=Modifier.fillMaxHeight().weight(3f)){
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
						if(uc.isEmpty())BasicText("◉ 加载中...",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
						else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){
							// 点击封面播放
							AsyncImage(model=O!!["cover"],contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
							Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(Fyan.cc.m),contentAlignment=Alignment.Center){BasicText("▶",style=Fyan.ff.h2.copy(color=Color.White))}
						}else AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=true;requestFocus()}},modifier=Modifier.fillMaxSize())
					}
					// TV 模式下集数选择使用横向 LazyRow
					if(sl.isNotEmpty())LazyRow(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(10.dp,8.dp)){
						items(sl.indices.toList()){k->
							Box(modifier=Modifier.width(50.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(k==ec)Fyan.cc.primary else Fyan.cc.x).clickable{ec=k},contentAlignment=Alignment.Center){
								BasicText(sl[k]["title"]!!,style=Fyan.ff.ps.copy(color=if(k==ec)Color.White else Fyan.cc.c,fontWeight=if(k==ec)FontWeight.W600 else FontWeight.W400))
							}
						}
					}
				}
				// TV 简介侧边栏，可竖向滚动
				Column(modifier=Modifier.fillMaxHeight().padding(16.dp,12.dp).weight(1f).verticalScroll(rememberScrollState())){
					BasicText("视频简介",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=6.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				}
			}
			// 手机竖屏布局：整体可竖向滚动
			else->Column(modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
				Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
					if(uc.isEmpty())BasicText("◌ 加载中...",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){
						AsyncImage(model=O!!["cover"],contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
						Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(Fyan.cc.m),contentAlignment=Alignment.Center){BasicText("▶",style=Fyan.ff.h2.copy(color=Color.White))}
					}else{
						// 播放器区域：点击触发全屏弹窗
						Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){
							AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=false;requestFocus()}},modifier=Modifier.fillMaxSize())
						}
						// 全屏播放弹窗：强制横屏，关闭时恢复竖屏
						if(fs)Dialog(onDismissRequest={fs=false},properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=true,dismissOnClickOutside=false)){
							val ctx=LocalContext.current
							DisposableEffect(Unit){
								(ctx as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
								onDispose{(ctx as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
							}
							Box(modifier=Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){
								AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=true;requestFocus()}},modifier=Modifier.fillMaxSize())
								// 全屏模式下的关闭按钮（左上角）
								Box(modifier=Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp).clip(CircleShape).background(Fyan.cc.m).clickable{fs=false},contentAlignment=Alignment.Center){
									BasicText("✕",style=Fyan.ff.p.copy(color=Color.White))
								}
							}
						}
					}
				}
				// 视频简介区
				Column(modifier=Modifier.padding(14.dp,12.dp)){
					BasicText("视频简介",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=4.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				}
				Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal=14.dp).background(Fyan.cc.bd))
				// 集数选择区（每行5个，自动换行）
				if(sl.isNotEmpty())Column(modifier=Modifier.padding(12.dp,8.dp)){
					BasicText("选集",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					Column(modifier=Modifier.padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
						repeat((sl.size+4)/5){r->
							Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
								repeat(5){c->
									val i=r*5+c
									if(i<sl.size)Box(modifier=Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(i==ec)Fyan.cc.primary else Fyan.cc.x).clickable{ec=i},contentAlignment=Alignment.Center){
										BasicText(sl[i]["title"]!!,style=Fyan.ff.ps.copy(color=if(i==ec)Color.White else Fyan.cc.c,fontWeight=if(i==ec)FontWeight.W600 else FontWeight.W400))
									}else Box(modifier=Modifier.weight(1f))
								}
							}
						}
					}
				}
				Box(modifier=Modifier.height(24.dp))
			}
		}
	}
}