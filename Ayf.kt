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
	val s=listOf("history" to "历史记录","movie" to "电影","drama" to "剧集","anime" to "动漫","variety" to "综艺","documentary" to "纪录片")
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
		return "https://api.iyf.tv/api/list/getconditionfilterdata?titleid=$id&ids=$o&page=$p&size=21"
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
							Fyan.goto("ayf_info/"+o["id"])
						},horizontalAlignment=Alignment.CenterHorizontally){
							Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(Fyan.cc.ag)){
								AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
								if(!o["score"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.TopStart).padding(2.dp).background(Fyan.cc.m,RoundedCornerShape(4.dp)).padding(2.dp)){
									BasicText(o["score"]!!,style=Fyan.ff.ps.copy(color=Fyan.cc.c))
								}
								if(!o["tip"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.BottomCenter).padding(4.dp,2.dp).background(Fyan.cc.m,RoundedCornerShape(2.dp)).padding(2.dp)){
									BasicText(o["tip"]!!,style=Fyan.ff.ps.copy(color=Fyan.cc.c))
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
							BasicText("꧁ 已加载全部 ꧂",style=Fyan.ff.ps.copy(color=Fyan.cc.c.copy(alpha=0.3f)))
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
	var ec by remember{mutableIntStateOf(-1)} // 当前选中集数索引
	var ep by remember{mutableLongStateOf(0L)} // 当前选中集数的播放位置
	var uc by remember{mutableStateOf("")} // 当前集播放 URL
	var pr by remember{mutableStateOf(false)} // 是否已点击播放（显示播放器）
	var fs by remember{mutableStateOf(false)} // 是否全屏播放弹窗
	val sc=rememberCoroutineScope()
	Fyan.log("路由","进入爱壹帆视频详情页")

	// 进入详情页：拉取视频详情数据，写入历史记录
	LaunchedEffect(id){
		X=true;pr=false;uc=""
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
		if(O!=null){
			val h=Fyan.co("ayf_history","")
			val x=h.lines().firstOrNull{it.startsWith("$id ")}
			// 写入历史记录：新条目置顶，去重同 id 旧条目
			if(x.isNullOrBlank()){
				ec=0;ep=0L
				Fyan.cs("ayf_history",(listOf("$id ${O!!["type"]} ${O!!["title"]} ${O!!["cover"]} 0 0")+h.lines().filter{it.isNotEmpty()}).joinToString("\n"))
			}else{
				val s=x.trim().split(Regex("\\s+"),6)
				ec=s.getOrElse(4){"0"}.toIntOrNull()?:0
				ep=s.getOrElse(5){"0"}.toLongOrNull()?:0L
			}
		}
	}

	// 切换集数：重置播放状态，拉取新集播放链接
	LaunchedEffect(ec){
		if(O==null||ec==-1)return@LaunchedEffect
		pr=false;uc=""
		val h=Fyan.co("ayf_history","")
		val x=h.lines().firstOrNull{it.startsWith("$id ")}
		if(x.isNullOrBlank())ep=0L else{
			val s=x.trim().split(Regex("\\s+"),6)
			if(s.getOrElse(4){"0"}.toIntOrNull()?:0==ec)ep=s.getOrElse(5){"0"}.toLongOrNull()?:0L else ep=0L
		}
		@Suppress("UNCHECKED_CAST") val ei=(O!!["s"] as List<Map<String,String>>).getOrNull(ec)?.get("id")?:""
		val url=withContext(Dispatchers.IO){
			runCatching<String>{
				// 请求播放数据接口，取第一个非空 mediaUrl
				val s=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/getplaydata?mediaKey=$id&videoId=$ei&videoType=${O!!["type"]}")).optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
				var u=""
				for(i in 0 until s.length()){val v=s.getJSONObject(i).optString("mediaUrl","");if(v.isNotEmpty()){u=v;break}};u
			}.getOrElse{""}
		}
		uc=url // 确保 ep 就绪后再赋值触发播放器创建，时序完美闭环
		Fyan.cs("ayf_history",(listOf("$id ${O!!["type"]} ${O!!["title"]} ${O!!["cover"]} ${ec} ${ep}")+h.lines().filter{it.isNotEmpty()&&!it.startsWith("${id} ")}).joinToString("\n"))
	}

	// 根据 URL 创建播放器实例（纯粹创建，隔离副作用）
	val P:ExoPlayer?=remember(uc){if(uc.isNotEmpty())ExoPlayer.Builder(Fyan.me).build() else null}

	// 统一管控换源、历史寻道控制，杜绝状态竞争
	LaunchedEffect(P,uc){
		P?.apply{
			val f=if(uc.contains(".m3u8",true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
			setMediaSource(f.createMediaSource(MediaItem.fromUri(Uri.parse(uc))));if(ep>0L)seekTo(ep);prepare();playWhenReady=true
		}
	}

	// 播放时定时轮询同步进度至 ep
	LaunchedEffect(P){
		while(true){
			if(P?.isPlaying==true)ep=P.currentPosition
			kotlinx.coroutines.delay(1000)
		}
	}

	// 组件销毁或重置换源时的现场清理与终极持久化
	DisposableEffect(P){
		onDispose{
			if(P!=null&&O!=null){
				val cp=P.currentPosition
				val t=O!!["type"];val tl=O!!["title"];val cv=O!!["cover"]
				kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch{
					// 直接在后台协程中拉取最新历史，彻底干掉全页面的 State 监听
					val h=Fyan.co("ayf_history","")
					val s=listOf("$id $t $tl $cv $ec $cp")+h.lines().filter{it.isNotEmpty()&&!it.startsWith("$id ")}
					Fyan.cs("ayf_history",s.joinToString("\n"))
				}
			}
			P?.release()
		}
	}

	@Suppress("UNCHECKED_CAST") val sl:List<Map<String,String>> = if(O!=null)O!!["s"] as List<Map<String,String>> else emptyList()

	// 根布局采用 Box 包裹，方便 TV 端全屏时直接在顶层覆盖全屏
	Box(modifier=Modifier.fillMaxSize()){
		Column(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg)){
			// 顶部导航栏：返回箭头 + 视频标题
			Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(Fyan.cc.cg),verticalAlignment=Alignment.CenterVertically){
				Box(modifier=Modifier.size(32.dp).clip(CircleShape).padding(3.dp).clickable{Fyan.nc.popBackStack()},contentAlignment=Alignment.Center){
					Image(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier=Modifier.size(20.dp),colorFilter=ColorFilter.tint(Fyan.cc.c))
				}
				BasicText((O?.get("title") as? String)?:"视频详情",modifier=Modifier.weight(1f).padding(start=3.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.h4.copy(color=Fyan.cc.c))
			}
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
								AsyncImage(model=O!!["cover"].toString()+"?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg",contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
								Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(Fyan.cc.m),contentAlignment=Alignment.Center){BasicText("▶",style=Fyan.ff.h2.copy(color=Color.White))}
							}else{
								// TV 预览态播放器：点击或按确定键直接切入全屏
								Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){
									AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=false;requestFocus()}},modifier=Modifier.fillMaxSize())
								}
							}
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
						}else Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){
							AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=false;requestFocus()}},modifier=Modifier.fillMaxSize())
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

		// 独立于 when 分支外的全局全屏渲染层
		if(fs){
			if(Fyan.tv){
				// TV 专属全屏架构：采用原生 Box 顶层平铺覆盖，完美保留遥控器 D-Pad 进度条寻道焦点
				Box(modifier=Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){
					AndroidView(factory={
						PlayerView(Fyan.me).apply{
							player=P;useController=true
							// 绑定 Click 监听：遥控器 OK 键直接控制控制栏显示与隐藏切换
							setOnClickListener{if(isControllerFullyVisible)hideController() else showController()}
							requestFocus()
						}
					},modifier=Modifier.fillMaxSize())
					// 拦截遥控器返回键：直接退出全屏模式回到简介页
					BackHandler{fs=false}
				}
			}else{
				// 手机专属全屏架构：继续沿用 Dialog 方便进行强制横屏控制
				Dialog(onDismissRequest={fs=false},properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=true,dismissOnClickOutside=false)){
					val ctx=LocalContext.current
					DisposableEffect(Unit){
						// 使用 SENSOR_LANDSCAPE 锁定横屏并支持重力感应 180 度翻转
						(ctx as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
						onDispose{(ctx as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
					}
					Box(modifier=Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){
						AndroidView(factory={
							PlayerView(Fyan.me).apply{
								player=P;useController=true
								setOnClickListener{if(isControllerFullyVisible)hideController() else showController()}
								requestFocus()
							}
						},modifier=Modifier.fillMaxSize())
						Box(modifier=Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp).clip(CircleShape).background(Fyan.cc.m).clickable{fs=false},contentAlignment=Alignment.Center){
							BasicText("✕",style=Fyan.ff.p.copy(color=Color.White))
						}
					}
				}
			}
		}
	}
}
