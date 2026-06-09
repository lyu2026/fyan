package com.fyan

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ── 全局网络客户端 ────────────────────────────────────────────────────────────
internal val HC=okhttp3.OkHttpClient.Builder()
	.connectTimeout(5,java.util.concurrent.TimeUnit.SECONDS)
	.readTimeout(5,java.util.concurrent.TimeUnit.SECONDS)
	.writeTimeout(5,java.util.concurrent.TimeUnit.SECONDS)
	.followRedirects(true) // 统一开启重定向，版本检测接口需要
	.build()

// 阻塞式GET，仅供Dispatchers.IO调用
internal fun SF(url:String):String{
	val r=okhttp3.Request.Builder().url(url).build()
	HC.newCall(r).execute().use{o->
		if(!o.isSuccessful)throw java.io.IOException("HTTP ${o.code}: $url")
		return o.body?.string()?:""
	}
}

// ── 本地持久化 ────────────────────────────────────────────────────────────────
object PR{ // SharedPreferences薄封装
	private lateinit var sp:android.content.SharedPreferences
	fun init(cx:Context){sp=cx.getSharedPreferences("fyan_prefs",Context.MODE_PRIVATE)}
	var lt:String // 最后激活的Tab id
		set(v){sp.edit().putString("last_tab",v).apply()}
		get()=sp.getString("last_tab","history")?:"history"
	var hs:String // 历史记录JSON序列化字符串
		set(v){sp.edit().putString("his_json",v).apply()}
		get()=sp.getString("his_json","[]")?:"[]"
}

// ── 数据模型 ──────────────────────────────────────────────────────────────────
data class NT(val id:String,val lb:String) // 导航Tab（id=路由/lb=标签）
data class FO(val id:String,val lb:String) // 过滤选项节点（id=classifyId/lb=显示名）
data class FG(val op:List<FO>) // 过滤选项分组（一组互斥选项）
data class VI(val id:String,val type:String,val tt:String,val pt:String,val sc:String,val ut:String) // 视频列表简略项
data class VD(val id:String,val type:String,val tt:String,val ds:String,val pt:String,val ep:List<String>,val et:List<String>) // 视频详情全量
data class RT(val id:String,val type:String,val tt:String,val pt:String,val pg:String="") // 历史记录条目（原FN.VT）

// ── 全局导航Tab配置 ───────────────────────────────────────────────────────────
val NAV_TABS=listOf(
	NT("history","历史记录"),NT("movie","电影"),NT("drama","电视剧"),
	NT("anime","动漫"),NT("variety","综艺"),NT("documentary","纪录片"),
	NT("news","新闻"),NT("yule","娱乐")
) // 顶部Tab全量定义，增减在此一处改动

// ── 日志全局状态 ──────────────────────────────────────────────────────────────
val LG=mutableStateListOf<String>() // 日志条目队列；格式：uuid.colorHex●时间 模块 ➜ 内容
var LF by mutableStateOf(false) // 日志面板折叠状态（true=折叠为横条）
var LY by mutableStateOf(0f) // 日志面板拖拽下移像素偏移

private val MH=android.os.Handler(android.os.Looper.getMainLooper()) // 复用Handler，避免重复构造

fun lg(m:String,o:String,c:Char='i'){ // 写入一条日志（m=模块/o=内容/c=级别 i信息 u用户 e错误 s系统 n成功 w警告）
	val t=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date())
	val x=when(c){'i'->"#2196F3";'u'->"#9C27B0";'e'->"#F44336";'s'->"#00BCD4";'n'->"#4CAF50";'w'->"#FF9800";else->"#9E9E9E"}
	val v="${java.util.UUID.randomUUID().toString().replace("-","")}.$x●$t $m ➜ $o"
	MH.post{LG.add(v)} // 切主线程，保证State更新安全
}
fun lc()=LG.clear() // 清空全部日志
fun lr(i:String)=LG.removeAll{it.startsWith(i)} // 按uuid前缀删除单条

// ── 历史记录 ──────────────────────────────────────────────────────────────────
val HI=mutableStateListOf<RT>() // 播放历史内存队列

fun sH(){ // 序列化历史到SP
	val j=org.json.JSONArray()
	HI.forEach{val o=JSONObject();o.put("id",it.id);o.put("type",it.type);o.put("tt",it.tt);o.put("pt",it.pt);o.put("pg",it.pg);j.put(o)}
	PR.hs=j.toString()
}
fun lH(){ // 从SP反序列化历史到内存
	HI.clear()
	runCatching{
		val j=org.json.JSONArray(PR.hs)
		for(i in 0 until j.length()){val o=j.getJSONObject(i);HI.add(RT(o.optString("id"),o.optString("type","1"),o.optString("tt"),o.optString("pt"),o.optString("pg")))}
	}
}
fun aH(im:RT){ // 追加/更新历史（同id去重置顶）
	HI.removeAll{it.id==im.id}
	HI.add(0,im)
	sH()
}
fun rH(id:String){HI.removeAll{it.id==id};sH()} // 删除单条并持久化
fun cH(){HI.clear();sH()} // 清空并持久化

// ── 集数播放源解析（P.kt共用，提取自P.kt私有rs()）──────────────────────────────
suspend fun rs(id:String,d:VD,ep:Int):String{ // 解析指定集数播放直链
	val ru=d.ep.getOrNull(ep)?:""
	return if(ru.isNotEmpty()&&!ru.startsWith("http",ignoreCase=true))fS(id,d.type,ru) else ru
}

// ── API常量 ───────────────────────────────────────────────────────────────────
private const val YF="https://api.iyf.tv/api" // 媒体数据源API根地址

suspend fun fG(id:String):List<FG>=withContext(Dispatchers.IO){ // 拉取分类过滤条件树
	runCatching{
		when{
			id=="news"->listOf(FG(listOf( // 新闻分类硬编码
				FO("国际","国际"),FO("国内","国内"),FO("华人资讯","华人资讯"),FO("财经","财经"),FO("军事","军事")
			)))
			else->{
				var j=JSONObject(SF("$YF/list/getfiltertagsdata?SecondaryCode=$id"))
				j=j.optJSONObject("data")?:return@runCatching emptyList()
				val s=j.optJSONArray("list")?:return@runCatching emptyList()
				buildList{
					for(i in 0 until s.length()){
						val x=s.getJSONObject(i)
						val z=x.optJSONArray("list")?:continue
						add(FG(buildList{
							for(r in 0 until z.length()){val o=z.getJSONObject(r);add(FO(o.optString("classifyId","0"),o.optString("classifyName","")))}
						}))
					}
				}
			}
		}
	}.getOrElse{e->lg("Tags",e.message?:"err",'e');emptyList()}
}

suspend fun fL(id:String,isStr:String,pg:Int,sz:Int=21):List<VI>=withContext(Dispatchers.IO){ // 分页拉取视频列表
	runCatching{
		var j=JSONObject(SF("$YF${if(id=="news")"/home/getrelativevideosbysub?titleid=$id&Tags=$isStr" else "/list/getconditionfilterdata?titleid=$id&ids=$isStr"}&page=$pg&size=$sz"))
		j=j.optJSONObject("data")?:return@runCatching emptyList()
		val s=j.optJSONArray("list")?:return@runCatching emptyList()
		buildList{
			for(i in 0 until s.length()){val x=s.getJSONObject(i);add(VI(x.optString("mediaKey",""),x.optString("videoType","1"),x.optString("title",""),x.optString("coverImgUrl",""),x.optString("score",""),x.optString("updateStatus","")))}
		}
	}.getOrElse{e->lg("List",e.message?:"err",'e');emptyList()}
}

suspend fun fD(id:String):VD?=withContext(Dispatchers.IO){ // 拉取视频完整详情
	runCatching{
		var o=JSONObject(SF("$YF/video/videodetails?mediaKey=$id"))
		o=o.optJSONObject("data")?:return@runCatching null
		o=o.optJSONObject("detailInfo")?:return@runCatching null
		val s=o.optJSONArray("episodes")
		val ts=mutableListOf<String>() // 各集显示标题
		val us=mutableListOf<String>() // 各集episodeId
		if(s!=null)for(i in 0 until s.length()){val v=s.getJSONObject(i);us.add(v.optString("episodeId",""));ts.add(v.optString("episodeTitle","${i+1}"))}
		VD(id=id,type=o.optString("videoType","1"),tt=o.optString("title","").ifEmpty{"未知"},ds=o.optString("introduce","").ifEmpty{"空空如也"},
			pt=o.optString("coverImgUrl","").ifEmpty{""}
				.let{if(it.isNotEmpty())"$it?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg" else it},
			ep=us,et=ts)
	}.getOrElse{e->lg("Detail",e.message?:"err",'e');null}
}

suspend fun fS(id:String,type:String,vid:String=""):String=withContext(Dispatchers.IO){ // 解析单集播放直链，优先免费源
	runCatching{
		val o=JSONObject(SF("$YF/video/getplaydata?mediaKey=$id&videoId=$vid&videoType=$type"))
		val s=o.optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
		var u=""
		for(i in 0 until s.length()){
			val x=s.optJSONObject(i)?:continue
			val mu=x.optString("mediaUrl","")
			if(mu.isEmpty())continue
			if(u.isEmpty())u=mu // 兜底：记录第一个非空
			if(!x.optBoolean("isVip",false)){u=mu;break} // 免费源立即中断
		}
		u
	}.getOrElse{e->lg("Source",e.message?:"err",'e');""}
}