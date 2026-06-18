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
import androidx.activity.compose.BackHandler
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


@Composable fun AyfHome(){ // 爱壹帆首页
	val s=listOf("history" to "历史记录","movie" to "电影","drama" to "剧集","anime" to "动漫","variety" to "综艺","documentary" to "纪录片")
	val c by Fyan.cg("ayf_tab","history").collectAsState(initial="history")
	val sc=rememberCoroutineScope()
	val cc=Fyan.cc;val f=Fyan.ff // 优先获取 Fyan 数据/样式系统
	Fyan.log("路由","进入爱壹帆首页")

	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
		Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(cc.cg).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
			s.forEach{o->Box(modifier=Modifier.fillMaxHeight().background(if(o.first==c)cc.primary.copy(alpha=0.15f)else Color.Transparent).clickable{
				Fyan.log("爱壹帆","点击TAB: "+o.first,'u')
				sc.launch{Fyan.cs("ayf_tab",o.first)} // 修复原代码中未闭合的闭包语法缺陷
			},contentAlignment=Alignment.Center){
				BasicText("  ${o.second}  ",style=f.h4.copy(color=if(o.first==c)cc.primary else cc.c.copy(alpha=0.7f),fontWeight=if(o.first==c)FontWeight.Bold else FontWeight.Normal))
			}}
		}
		Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd))
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			key(c){when(c){"history"->AyfHistory();else->AyfList(id=c)}}
		}
	}
}

@Composable fun AyfHistory(){ // 爱壹帆历史记录
	val cc=Fyan.cc;val f=Fyan.ff // 优先获取 Fyan 数据/样式系统
	val cn=if(Fyan.sw>=840)5 else if(Fyan.sw>=600)4 else 3
	var id by remember{mutableStateOf<String?>(null)}
	var clearAll by remember{mutableStateOf(false)} // 改名规避主题变量 cc 命名冲突
	val sc=rememberCoroutineScope()
	val rs by Fyan.cg("ayf_history","").collectAsState(initial="")
	val vs=remember(rs){
		if(rs.isBlank())emptyList()else rs.lines().filter{it.isNotBlank()}.mapNotNull{
			val x=it.trim().split(Regex("\\s+"),6)
			if(x.size<6)null else mapOf("id" to x[0],"type" to x[1],"title" to x[2],"cover" to x[3],"ec" to x[4],"ps" to x[5])
		}
	}
	Fyan.log("路由","进入爱壹帆历史记录页")

	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
		Row(modifier=Modifier.fillMaxWidth().height(30.dp).padding(start=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
			BasicText("记录清单",style=f.pb.copy(color=cc.c))
			Box(modifier=Modifier.size(28.dp).clickable{clearAll=true},contentAlignment=Alignment.Center){BasicText("🗑",style=f.h4.copy(color=cc.c))}
		}
		if(vs.isEmpty())Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
			BasicText("暂无观看记录",style=f.p.copy(color=cc.c.copy(alpha=0.4f)))
		}else LazyVerticalGrid(modifier=Modifier.fillMaxWidth().weight(1f),columns=GridCells.Fixed(cn),contentPadding=PaddingValues(4.dp),verticalArrangement=Arrangement.spacedBy(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
			items(vs,{it["id"]!!}){o->
				Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(cc.cg).pointerInput(o["id"]!!){
					detectTapGestures(onTap={Fyan.goto("ayf_info/"+o["id"])},onLongPress={id=o["id"]})
				},horizontalAlignment=Alignment.CenterHorizontally){
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(cc.ag),contentAlignment=Alignment.BottomCenter){
						AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
						if(o["ec"]!="0")Box(modifier=Modifier.padding(4.dp).background(cc.m,RoundedCornerShape(1.dp)).padding(horizontal=4.dp,vertical=2.dp)){
							BasicText("第${o["ec"]}集",style=f.ps.copy(color=Color.White))
						}
					}
					BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=f.ps.copy(color=cc.c,textAlign=TextAlign.Center))
				}
			}
		}
	}
	if(clearAll)Dialog(onDismissRequest={clearAll=false}){
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(cc.cg).border(1.dp,cc.bd,RoundedCornerShape(3.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
			BasicText("清空历史记录？",style=f.h3.copy(color=cc.c))
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
				BasicText("取消",modifier=Modifier.clickable{Fyan.log("爱壹帆","取消清空历史记录",'s');clearAll=false}.padding(8.dp),style=f.p.copy(color=cc.c))
				BasicText("确定",modifier=Modifier.clickable{Fyan.log("爱壹帆","执行清空历史记录",'w');sc.launch{Fyan.cs("ayf_history","")};clearAll=false}.padding(8.dp),style=f.p.copy(color=cc.primary))
			}
		}
	}
	if(id!=null)Dialog(onDismissRequest={id=null}){
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(cc.cg).border(1.dp,cc.bd,RoundedCornerShape(3.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
			BasicText("删除此记录？",style=f.h3.copy(color=cc.c))
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
				BasicText("取消",modifier=Modifier.clickable{Fyan.log("爱壹帆","取消删除历史记录，编号: $id",'e');id=null}.padding(8.dp),style=f.p.copy(color=cc.c))
				BasicText("确定",modifier=Modifier.clickable{
					val o=rs.lines().filter{it.isNotBlank()&&!it.startsWith("${id!!} ")}.joinToString("\n")
					Fyan.log("爱壹帆","执行删除历史记录，编号: $id",'d')
					sc.launch{Fyan.cs("ayf_history",o)};id=null
				}.padding(8.dp),style=f.p.copy(color=cc.primary))
			}
		}
	}
}

@Composable fun AyfList(id:String){ // 爱壹帆筛选列表
	val cc=Fyan.cc;val f=Fyan.ff // 优先获取 Fyan 数据/样式系统
	var vs by remember{mutableStateOf(emptyList<Map<String,String>>())}
	var fs by remember{mutableStateOf(emptyList<List<Pair<String,String>>>())}
	val fc=remember{mutableStateMapOf<Int,String>()}
	var X by remember{mutableStateOf(true)}
	var pg by remember{mutableIntStateOf(1)}
	var hm by remember{mutableStateOf(true)}
	var lm by remember{mutableStateOf(false)}
	val gs=rememberLazyGridState()
	val sc=rememberCoroutineScope()
	Fyan.log("路由","进入爱壹帆筛选列表页")

	fun au(p:Int=1):String{
		var o=fc.entries.sortedBy{it.key}.joinToString(","){it.value}
		return "https://api.iyf.tv/api/list/getconditionfilterdata?titleid=$id&ids=$o&page=$p&size=21"
	}

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
		fs.indices.forEach{i->fc[i]="0"}
		pg=1;hm=true
		vs=fv(au(1));X=false
	}

	LaunchedEffect(gs){
		snapshotFlow{gs.layoutInfo.visibleItemsInfo.lastOrNull()?.index}
			.collect{li->
				if(li!=null&&li>=vs.size-Fyan.gc*2&&hm&&!lm&&!X){
					lm=true
					val px=pg+1
					Fyan.log("爱壹帆","触底加载第${px}页",'d')
					val mr=fv(au(px))
					if(mr.isEmpty()){
						hm=false
						Fyan.log("爱壹帆","已加载全部数据",'s')
					}else{
						vs=vs+mr
						pg=px
					}
					lm=false
				}
			}
	}

	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){
		if(fs.isNotEmpty())Column(modifier=Modifier.fillMaxWidth().background(cc.cg)){
			fs.forEachIndexed{i,g->
				Row(modifier=Modifier.fillMaxWidth().height(20.dp).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
					g.forEach{(fi,fn)->
						val at=fi==fc[i]
						Box(modifier=Modifier.fillMaxHeight().background(if(at)cc.primary.copy(alpha=0.15f)else Color.Transparent).clickable{
							fc[i]=fi;sc.launch{X=true;pg=1;hm=true;vs=fv(au(1));X=false}
						},contentAlignment=Alignment.Center){
							BasicText("  $fn  ",style=f.ps.copy(color=if(at)cc.primary else cc.c.copy(alpha=0.7f),fontWeight=if(at)FontWeight.W600 else FontWeight.W400))
						}
					}
				}
				Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd))
			}
		}
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			when{
				X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					BasicText("◌ 加载中…",style=f.pb.copy(color=cc.c.copy(alpha=0.4f)))
				}
				vs.isEmpty()->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					BasicText("暂无视频",style=f.p.copy(color=cc.c.copy(alpha=0.4f)))
				}
				else->LazyVerticalGrid(state=gs,columns=GridCells.Fixed(Fyan.gc),contentPadding=PaddingValues(4.dp),
					verticalArrangement=Arrangement.spacedBy(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
					items(vs,{it["id"]!!}){o->
						Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(cc.cg).clickable{
							Fyan.log("爱壹帆","进入详情页，编号: "+o["id"])
							Fyan.goto("ayf_info/"+o["id"])
						},horizontalAlignment=Alignment.CenterHorizontally){
							Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(cc.ag)){
								AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
								if(!o["score"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.TopStart).padding(2.dp,6.dp).background(cc.m,RoundedCornerShape(10.dp)).padding(2.dp)){
									BasicText(o["score"]!!,style=f.ps.copy(color=cc.c))
								}
								if(!o["tip"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.BottomCenter).padding(2.dp,6.dp).background(cc.m,RoundedCornerShape(6.dp)).padding(2.dp)){
									BasicText(o["tip"]!!,style=f.ps.copy(color=cc.c))
								}
							}
							BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=f.ps.copy(color=cc.c,textAlign=TextAlign.Center))
						}
					}
					if(lm)item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}){
						Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){
							BasicText("◌ 加载更多…",style=f.ps.copy(color=cc.c.copy(alpha=0.4f)))
						}
					}
					if(!hm&&vs.isNotEmpty())item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}){
						Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){
							BasicText("꧁ 已加载全部 ꧂",style=f.ps.copy(color=cc.c.copy(alpha=0.3f)))
						}
					}
				}
			}
		}
	}
}

@Composable fun AyfInfo(id:String){ // 爱壹帆详情播放
	val act=LocalContext.current as? Activity // 提前在外侧安全提取真实 Activity 引用，防止 Dialog 内部生命周期与转换失效
	var O by remember{mutableStateOf<Map<String,Any>?>(null)} // 视频详情数据
	var X by remember{mutableStateOf(true)} // 详情加载中标志
	// 极致体验调优：关键播放指标采用 rememberSaveable 级持久代理，彻底免疫横竖屏翻转导致的 Activity 状态抹除阵亡
	var ec by rememberSaveable{mutableIntStateOf(-1)} // 当前选中集数索引
	var ep by rememberSaveable{mutableLongStateOf(0L)} // 当前选中集数的播放位置
	var uc by rememberSaveable{mutableStateOf("")} // 当前集播放 URL
	var pr by rememberSaveable{mutableStateOf(false)} // 是否已点击播放（显示播放器）
	var fs by rememberSaveable{mutableStateOf(false)} // 是否全屏播放状态
	val c=Fyan.cc // 顶层读取单例化主题，彻底切断重组过程中的多余对象分配
	val f=Fyan.ff // 优先获取 Fyan 统一字体集
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
				if(x!=null)for(i in 0 until x.length()){val v=x.getJSONObject(i);s.add(mapOf("id" to v.optString("episodeId",""),"title" to v.optString("episodeTitle","${i+1}")))}
				mapOf("id" to id,"type" to j.optString("videoType","1"),"title" to j.optString("title","未知"),"brief" to j.optString("introduce","空空如也"),"cover" to j.optString("coverImgUrl",""),"s" to s)
			}.getOrElse{null}
		}
		X=false
		if(O!=null){
			val h=Fyan.co("ayf_history","")
			val x=h.lines().firstOrNull{it.startsWith("$id ")}
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
				val s=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/getplaydata?mediaKey=$id&videoId=$ei&videoType=${O!!["type"]}")).optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
				var u=""
				for(i in 0 until s.length()){val v=s.getJSONObject(i).optString("mediaUrl","");if(v.isNotEmpty()){u=v;break}};u
			}.getOrElse{""}
		}
		uc=url
		Fyan.cs("ayf_history",(listOf("$id ${O!!["type"]} ${O!!["title"]} ${O!!["cover"]} ${ec} ${ep}")+h.lines().filter{it.isNotEmpty()&&!it.startsWith("${id} ")}).joinToString("\n"))
	}

	// 根据 URL 创建播放器实例
	val P:ExoPlayer?=remember(uc){if(uc.isNotEmpty())ExoPlayer.Builder(Fyan.me).build() else null}

	// 统一管控换源与历史寻道控制
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
					val h=Fyan.co("ayf_history","")
					val s=listOf("$id $t $tl $cv $ec $cp")+h.lines().filter{it.isNotEmpty()&&!it.startsWith("$id ")}
					Fyan.cs("ayf_history",s.joinToString("\n"))
				}
			}
			P?.release()
		}
	}

	@Suppress("UNCHECKED_CAST") val sl:List<Map<String,String>> = if(O!=null)O!!["s"] as List<Map<String,String>> else emptyList()

	// 根布局采用 Box 包裹，支持全屏顶层覆盖组件
	Box(modifier=Modifier.fillMaxSize()){
		Column(modifier=Modifier.fillMaxSize().background(c.bg)){
			// 顶部导航栏：返回箭头 + 视频标题
			Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(c.cg),verticalAlignment=Alignment.CenterVertically){
				Box(modifier=Modifier.size(26.dp).clip(CircleShape).padding(3.dp).clickable{Fyan.nc.popBackStack()},contentAlignment=Alignment.Center){
					Image(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier=Modifier.size(20.dp),colorFilter=ColorFilter.tint(c.c))
				}
				BasicText((O?.get("title") as? String)?:"视频详情",modifier=Modifier.weight(1f).padding(start=3.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=f.h4.copy(color=c.c))
			}
			when{
				X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					BasicText("◌ 加载视频详情…",style=f.p.copy(color=c.c.copy(alpha=0.5f)))
				}
				O==null->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					Column(horizontalAlignment=Alignment.CenterHorizontally){
						BasicText("◑ 加载失败",style=f.p.copy(color=c.c.copy(alpha=0.5f)))
						Box(modifier=Modifier.padding(top=8.dp).clip(RoundedCornerShape(4.dp)).background(c.ag).clickable{X=true}.padding(horizontal=16.dp,vertical=6.dp),contentAlignment=Alignment.Center){
							BasicText("重试",style=f.p.copy(color=c.primary))
						}
					}
				}
				// TV 横屏布局
				Fyan.tv->Row(modifier=Modifier.fillMaxSize()){
					Column(modifier=Modifier.fillMaxHeight().weight(3f)){
						Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
							if(uc.isEmpty())BasicText("◉ 加载中...",style=f.ps.copy(color=c.c.copy(alpha=0.5f)))
							else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){
								AsyncImage(model=O!!["cover"].toString()+"?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg",contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
								Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(c.m),contentAlignment=Alignment.Center){BasicText("▶",style=f.h2.copy(color=Color.White))}
							}else{
								Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){
									AndroidView(factory={ctx->PlayerView(ctx).apply{useController=true}},update={it.player=if(fs)null else P;if(!fs)it.requestFocus()},modifier=Modifier.fillMaxSize())
								}
							}
						}
						if(sl.isNotEmpty())LazyRow(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(10.dp,8.dp)){
							items(sl.indices.toList()){k->
								Box(modifier=Modifier.width(50.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(k==ec)c.primary else c.x).clickable{if(ec!=k)ec=k},contentAlignment=Alignment.Center){
									BasicText(sl[k]["title"]!!,style=f.ps.copy(color=if(k==ec)Color.White else c.c,fontWeight=if(k==ec)FontWeight.W600 else FontWeight.W400))
								}
							}
						}
					}
					Column(modifier=Modifier.fillMaxHeight().padding(16.dp,12.dp).weight(1f).verticalScroll(rememberScrollState())){
						BasicText("视频简介",style=f.ps.copy(color=c.c.copy(alpha=0.5f)))
						BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=6.dp),style=f.p.copy(color=c.c))
					}
				}
				// 手机竖屏布局
				else->Column(modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
						if(uc.isEmpty())BasicText("◌ 加载中...",style=f.ps.copy(color=c.c.copy(alpha=0.5f)))
						else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){
							AsyncImage(model=O!!["cover"],contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
							Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(c.m),contentAlignment=Alignment.Center){BasicText("▶",style=f.h2.copy(color=Color.White))}
						}else Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){
							AndroidView(factory={ctx->PlayerView(ctx).apply{useController=false}},update={it.player=if(fs)null else P},modifier=Modifier.fillMaxSize())
						}
					}
					Column(modifier=Modifier.padding(14.dp,12.dp)){
						BasicText("视频简介",style=f.ps.copy(color=c.c.copy(alpha=0.5f)))
						BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=4.dp),style=f.p.copy(color=c.c))
					}
					Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal=14.dp).background(c.bd))
					if(sl.isNotEmpty())Column(modifier=Modifier.padding(12.dp,8.dp)){
						BasicText("选集",style=f.ps.copy(color=c.c.copy(alpha=0.5f)))
						Column(modifier=Modifier.padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
							repeat((sl.size+4)/5){r->
								Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
									repeat(5){cIdx->
										val i=r*5+cIdx
										if(i<sl.size)Box(modifier=Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(i==ec)c.primary else c.x).clickable{if(ec!=i)ec=i},contentAlignment=Alignment.Center){
											BasicText(sl[i]["title"]!!,style=f.ps.copy(color=if(i==ec)Color.White else c.c,fontWeight=if(i==ec)FontWeight.W600 else FontWeight.W400))
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

		// 全局全屏覆盖层：全面重组优化，统一使用原生顶级 Box，彻底剔除 Dialog window 造成的旋转重置及遮罩布局硬伤
		if(fs){
			Box(modifier=Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){
				if(!Fyan.tv){
					// 手机设备：全屏时启动横屏重力感应锁定，退出时自动恢复原始竖屏状态
					DisposableEffect(Unit){
						act?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
						onDispose{act?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
					}
				}
				AndroidView(factory={ctx->
					PlayerView(ctx).apply{
						useController=true
						setOnClickListener{if(isControllerFullyVisible)hideController() else showController()}
					}
				},update={it.player=P;it.requestFocus()},modifier=Modifier.fillMaxSize())
				
				if(!Fyan.tv){
					// 手机全屏额外提供左上角极简关闭退出按钮
					Box(modifier=Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp).clip(CircleShape).background(c.m).clickable{fs=false},contentAlignment=Alignment.Center){
						BasicText("✕",style=f.p.copy(color=Color.White))
					}
				}
				BackHandler{fs=false}
			}
		}
	}
}
