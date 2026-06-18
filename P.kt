package com.fyan

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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

fun SF(u:String):String=java.net.URL(u).openStream().bufferedReader().use{it.readText()}

@Composable fun AyfHome(){
	val s=listOf("history" to "历史记录","movie" to "电影","drama" to "剧集","anime" to "动漫","variety" to "综艺","documentary" to "纪录片","news" to "新闻")
	val c by Fyan.cg("ayf_tab","history").collectAsState(initial="history")
	val sc=rememberCoroutineScope()
	Column(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg)){
		Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(Fyan.cc.cg).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
			s.forEach{o->Box(modifier=Modifier.fillMaxHeight().background(if(o.first==c)Fyan.cc.fc.copy(alpha=0.15f)else Color.Transparent).padding(horizontal=16.dp).clickable{sc.launch{Fyan.cs("ayf_tab",o.first)}},contentAlignment=Alignment.Center){
				BasicText(o.second,style=Fyan.ff.h4.copy(color=if(o.first==c)Fyan.cc.fc else Fyan.cc.c.copy(alpha=0.7f),fontWeight=if(o.first==c)FontWeight.Bold else FontWeight.Normal))
			}}
		}
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			key(c){when(c){"history"->AyfHistory();else->AyfList(id=c)}}
		}
	}
}

@Composable fun AyfHistory(){
	val cn=if(Fyan.sw>=840)5 else if(Fyan.sw>=600)4 else 3
	var id by remember{mutableStateOf<String?>(null)}
	var cc by remember{mutableStateOf(false)}
	val sc=rememberCoroutineScope()
	val rs by Fyan.cg("ayf_history","").collectAsState(initial="")
	val vs=remember(rs){
		if(rs.isBlank())emptyList()else rs.lines().filter{it.isNotBlank()}.mapNotNull{
			val x=it.trim().split(Regex("\\s+"),6)
			if(x.size<6)null else mapOf("id" to x[0],"type" to x[1],"title" to x[2],"cover" to x[3],"ec" to x[4],"ps" to x[5])
		}
	}
	Column(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg)){
		Row(modifier=Modifier.fillMaxWidth().height(30.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
			BasicText("记录清单",style=Fyan.ff.h3.copy(color=Fyan.cc.c))
			Box(modifier=Modifier.size(28.dp).clickable{cc=true},contentAlignment=Alignment.Center){BasicText("🗑",style=Fyan.ff.h4.copy(color=Fyan.cc.c))}
		}
		if(vs.isEmpty())Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
			BasicText("暂无观看记录",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.4f)))
		}else LazyVerticalGrid(modifier=Modifier.fillMaxWidth().weight(1f),columns=GridCells.Fixed(cn),contentPadding=PaddingValues(2.dp),verticalArrangement=Arrangement.spacedBy(2.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)){
			items(vs,{it["id"]!!}){o->
				Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).pointerInput(o["id"]!!){
					detectTapGestures(onTap={Fyan.goto("ayf_info/"+o["id"])},onLongPress={id=o["id"]})
				},horizontalAlignment=Alignment.CenterHorizontally){
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(Fyan.cc.ag),contentAlignment=Alignment.BottomCenter){
						AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
						if(o["ec"]!="0")Box(modifier=Modifier.padding(4.dp).background(Fyan.cc.m,RoundedCornerShape(1.dp)).padding(horizontal=4.dp,vertical=2.dp)){
							BasicText("第${o["ec"]}集",style=Fyan.ff.p12.copy(color=Color.White))
						}
					}
					BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.p12.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
				}
			}
		}
	}
	if(cc)Dialog(onDismissRequest={cc=false}){
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(Fyan.cc.cg).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
			BasicText("清空历史记录？",style=Fyan.ff.h4.copy(color=Fyan.cc.c))
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
				BasicText("取消",modifier=Modifier.clickable{cc=false}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				BasicText("确定",modifier=Modifier.clickable{sc.launch{Fyan.cs("ayf_history","")};cc=false}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.fc))
			}
		}
	}
	if(id!=null)Dialog(onDismissRequest={id=null}){
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(Fyan.cc.cg).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
			BasicText("删除此记录？",style=Fyan.ff.h4.copy(color=Fyan.cc.c))
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
				BasicText("取消",modifier=Modifier.clickable{id=null}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				BasicText("确定",modifier=Modifier.clickable{
					val o=rs.lines().filter{it.isNotBlank()&&!it.startsWith("${id!!} ")}.joinToString("\n")
					sc.launch{Fyan.cs("ayf_history",o)};id=null
				}.padding(8.dp),style=Fyan.ff.p.copy(color=Fyan.cc.fc))
			}
		}
	}
}

@Composable fun AyfList(id:String){
	var vs by remember{mutableStateOf(emptyList<Map<String,String>>())}
	var fs by remember{mutableStateOf(emptyList<List<Pair<String,String>>>())}
	val fc=remember{mutableStateMapOf<Int,String>()}
	var X by remember{mutableStateOf(true)}
	val sc=rememberCoroutineScope()

	fun au():String{
		var o=fc.entries.sortedBy{it.key}.joinToString(","){it.value}
		o=if(id=="news")"home/getrelativevideosbysub?titleid=$id&Tags=$o"else"list/getconditionfilterdata?titleid=$id&ids=$o"
		return "https://api.iyf.tv/api/$o&page=1&size=21"
	}

	suspend fun fv(u:String):List<Map<String,String>> = withContext(Dispatchers.IO){
		runCatching<List<Map<String,String>>>{
			val j=JSONObject(SF(u)).optJSONObject("data")?:return@runCatching emptyList()
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
				if(id=="news")listOf(listOf("国际" to "国际","国内" to "国内","华人资讯" to "华人资讯","财经" to "财经","军事" to "军事"))
				else{
					val j=JSONObject(SF("https://api.iyf.tv/api/list/getfiltertagsdata?SecondaryCode=$id")).optJSONObject("data")?:return@runCatching emptyList()
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
		fs.indices.forEach{i->fc[i]="0"}
		vs=fv(au());X=false
	}

	Column(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg)){
		if(fs.isNotEmpty())Column(modifier=Modifier.fillMaxWidth().background(Fyan.cc.cg)){
			fs.forEachIndexed{i,g->
				Row(modifier=Modifier.fillMaxWidth().height(24.dp).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
					g.forEach{(fi,fn)->
						val at=fi==fc[i]
						Box(modifier=Modifier.fillMaxHeight().background(if(at)Fyan.cc.fc.copy(alpha=0.15f)else Color.Transparent).clickable{
							fc[i]=fi;sc.launch{X=true;vs=fv(au());X=false}
						}.padding(horizontal=12.dp),contentAlignment=Alignment.Center){
							BasicText("  $fn  ",style=Fyan.ff.p12.copy(color=if(at)Fyan.cc.fc else Fyan.cc.c.copy(alpha=0.7f),fontWeight=if(at)FontWeight.W600 else FontWeight.W400))
						}
					}
				}
			}
		}
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){
			when{
				X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					BasicText("加载中…",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.4f)))
				}
				vs.isEmpty()->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
					BasicText("暂无视频",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.4f)))
				}
				else->LazyVerticalGrid(columns=GridCells.Fixed(Fyan.gc),contentPadding=PaddingValues(2.dp),
					verticalArrangement=Arrangement.spacedBy(2.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)){
					items(vs,{it["id"]!!}){o->
						Column(modifier=Modifier.padding(2.dp).clip(RoundedCornerShape(2.dp)).background(Fyan.cc.cg).clickable{
							if(id!="news")Fyan.goto("ayf_info/"+o["id"])
						},horizontalAlignment=Alignment.CenterHorizontally){
							Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(Fyan.cc.ag),contentAlignment=Alignment.BottomCenter){
								AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
								val bd=listOfNotNull(o["score"]?.takeIf{it.isNotEmpty()},o["tip"]?.takeIf{it.isNotEmpty()}).joinToString(" · ")
								if(bd.isNotEmpty())Box(modifier=Modifier.padding(2.dp).background(Fyan.cc.m,RoundedCornerShape(1.dp)).padding(4.dp,2.dp)){
									BasicText(bd,style=Fyan.ff.p12.copy(color=Color.White))
								}
							}
							BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.p12.copy(color=Fyan.cc.c,textAlign=TextAlign.Center))
						}
					}
				}
			}
		}
	}
}

@Composable fun AyfInfo(id:String){
	var O by remember{mutableStateOf<Map<String,Any>?>(null)}
	var X by remember{mutableStateOf(true)}
	var ec by remember{mutableIntStateOf(0)}
	var uc by remember{mutableStateOf("")}
	var pr by remember{mutableStateOf(false)}
	var fs by remember{mutableStateOf(false)}
	val hs by Fyan.cg("ayf_history","").collectAsState("")
	val sc=rememberCoroutineScope()

	LaunchedEffect(id){
		X=true;ec=0;pr=false;uc=""
		O=withContext(Dispatchers.IO){
			runCatching<Map<String,Any>?>{
				val j=JSONObject(SF("https://api.iyf.tv/api/video/videodetails?mediaKey=$id")).optJSONObject("data")?.optJSONObject("detailInfo")?:return@runCatching null
				val x=j.optJSONArray("episodes")
				val s=mutableListOf<Map<String,String>>()
				if(x!=null)for(i in 0 until x.length()){val v=x.getJSONObject(i);s.add(mapOf("id" to v.optString("episodeId",""),"title" to v.optString("episodeTitle","${i+1}")))}
				mapOf("id" to id,"type" to j.optString("videoType","1"),"title" to j.optString("title","未知"),"brief" to j.optString("introduce","空空如也"),"cover" to j.optString("coverImgUrl",""),"s" to s)
			}.getOrElse{null}
		}
		X=false
		if(O!=null){
			sc.launch{Fyan.cs("ayf_history",(listOf("$id ${O!!["type"]} ${O!!["title"]} ${O!!["cover"]} 0 0")+hs.lines().filter{it.isNotEmpty()&&!it.startsWith("$id ")}).joinToString("\n"))}
		}
	}

	LaunchedEffect(ec){
		if(O==null)return@LaunchedEffect
		pr=false;uc=""
		@Suppress("UNCHECKED_CAST") val ei=(O!!["s"] as List<Map<String,String>>).getOrNull(ec)?.get("id")?:""
		uc=withContext(Dispatchers.IO){
			runCatching<String>{
				val s=JSONObject(SF("https://api.iyf.tv/api/video/getplaydata?mediaKey=$id&videoId=$ei&videoType=${O!!["type"]}")).optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
				var u=""
				for(i in 0 until s.length()){val v=s.getJSONObject(i).optString("mediaUrl","");if(v.isNotEmpty()){u=v;break}};u
			}.getOrElse{""}
		}
	}

	val P:ExoPlayer?=remember(uc){
		if(uc.isNotEmpty())ExoPlayer.Builder(Fyan.me).build().apply{
			val f=if(uc.contains(".m3u8",true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
			setMediaSource(f.createMediaSource(MediaItem.fromUri(Uri.parse(uc))));prepare();playWhenReady=true
		}else null
	}
	DisposableEffect(P){onDispose{P?.release()}}

	@Suppress("UNCHECKED_CAST") val sl:List<Map<String,String>> = if(O!=null)O!!["s"] as List<Map<String,String>> else emptyList()

	Column(modifier=Modifier.fillMaxSize().background(Fyan.cc.bg)){
		Row(modifier=Modifier.fillMaxWidth().height(38.dp).padding(horizontal=2.dp).background(Fyan.cc.cg),verticalAlignment=Alignment.CenterVertically){
			Box(modifier=Modifier.size(32.dp).clip(CircleShape).clickable{Fyan.nc.popBackStack()},contentAlignment=Alignment.Center){
				Image(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier=Modifier.size(20.dp),colorFilter=ColorFilter.tint(Fyan.cc.c))
			}
			BasicText((O?.get("title") as? String)?:"视频详情",modifier=Modifier.padding(horizontal=6.dp).weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=Fyan.ff.h4.copy(color=Fyan.cc.c))
		}
		when{
			X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
				BasicText("加载视频详情…",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
			}
			O==null->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
				Column(horizontalAlignment=Alignment.CenterHorizontally){
					BasicText("加载失败",style=Fyan.ff.p.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					Box(modifier=Modifier.padding(top=8.dp).clip(RoundedCornerShape(4.dp)).background(Fyan.cc.hv).clickable{X=true}.padding(horizontal=16.dp,vertical=6.dp),contentAlignment=Alignment.Center){
						BasicText("重试",style=Fyan.ff.p.copy(color=Fyan.cc.fc))
					}
				}
			}
			Fyan.tv->Row(modifier=Modifier.fillMaxSize()){
				Column(modifier=Modifier.fillMaxHeight().weight(3f)){
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
						if(uc.isEmpty())BasicText("加载中...",style=Fyan.ff.p12.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
						else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){
							AsyncImage(model=O!!["cover"],contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
							Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(Fyan.cc.m),contentAlignment=Alignment.Center){BasicText("▶",style=Fyan.ff.h2.copy(color=Color.White))}
						}else AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=true;requestFocus()}},modifier=Modifier.fillMaxSize())
					}
					if(sl.isNotEmpty())LazyRow(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(10.dp,8.dp)){
						items(sl.indices.toList()){k->
							Box(modifier=Modifier.width(50.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(k==ec)Fyan.cc.fc else Fyan.cc.x).clickable{ec=k},contentAlignment=Alignment.Center){
								BasicText(sl[k]["title"]!!,style=Fyan.ff.p12.copy(color=if(k==ec)Color.White else Fyan.cc.c,fontWeight=if(k==ec)FontWeight.W600 else FontWeight.W400))
							}
						}
					}
				}
				Column(modifier=Modifier.fillMaxHeight().padding(16.dp,12.dp).weight(1f).verticalScroll(rememberScrollState())){
					BasicText("视频简介",style=Fyan.ff.p12.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=6.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				}
			}
			else->Column(modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
				Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
					if(uc.isEmpty())BasicText("加载中...",style=Fyan.ff.p12.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){
						AsyncImage(model=O!!["cover"],contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize())
						Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(Fyan.cc.m),contentAlignment=Alignment.Center){BasicText("▶",style=Fyan.ff.h2.copy(color=Color.White))}
					}else{
						Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){
							AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=false;requestFocus()}},modifier=Modifier.fillMaxSize())
						}
						if(fs)Dialog(onDismissRequest={fs=false},properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=true,dismissOnClickOutside=false)){
							val ctx=LocalContext.current
							DisposableEffect(Unit){
								(ctx as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
								onDispose{(ctx as? Activity)?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
							}
							Box(modifier=Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){
								AndroidView(factory={PlayerView(Fyan.me).apply{player=P;useController=true;requestFocus()}},modifier=Modifier.fillMaxSize())
								Box(modifier=Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp).clip(CircleShape).background(Fyan.cc.m).clickable{fs=false},contentAlignment=Alignment.Center){
									BasicText("✕",style=Fyan.ff.p.copy(color=Color.White))
								}
							}
						}
					}
				}
				Column(modifier=Modifier.padding(14.dp,12.dp)){
					BasicText("视频简介",style=Fyan.ff.p12.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=4.dp),style=Fyan.ff.p.copy(color=Fyan.cc.c))
				}
				Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal=14.dp).background(Fyan.cc.bd))
				if(sl.isNotEmpty())Column(modifier=Modifier.padding(12.dp,8.dp)){
					BasicText("选集",style=Fyan.ff.p12.copy(color=Fyan.cc.c.copy(alpha=0.5f)))
					Column(modifier=Modifier.padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
						repeat((sl.size+4)/5){r->
							Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
								repeat(5){c->
									val i=r*5+c
									if(i<sl.size)Box(modifier=Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(i==ec)Fyan.cc.fc else Fyan.cc.x).clickable{ec=i},contentAlignment=Alignment.Center){
										BasicText(sl[i]["title"]!!,style=Fyan.ff.p12.copy(color=if(i==ec)Color.White else Fyan.cc.c,fontWeight=if(i==ec)FontWeight.W600 else FontWeight.W400))
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