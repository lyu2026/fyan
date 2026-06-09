package com.fyan

import org.json.JSONObject
import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 带超时控制的复用客户端（补加writeTimeout防止慢速上传阻塞）
private val HC=okhttp3.OkHttpClient.Builder()
	.connectTimeout(5,java.util.concurrent.TimeUnit.SECONDS)
	.readTimeout(5,java.util.concurrent.TimeUnit.SECONDS)
	.writeTimeout(5,java.util.concurrent.TimeUnit.SECONDS).build()

// 封装安全的阻塞式 GET 请求（供 Dispatchers.IO 内部复用）
private fun SF(url:String):String{
	val r=okhttp3.Request.Builder().url(url).build()
	HC.newCall(r).execute().use{o->
		if(!o.isSuccessful)throw java.io.IOException("网络请求失败: $o")
		return o.body?.string()?:""
	}
}

data class NT(val id:String,val lb:String) // NT (NavTab) 顶部频道常量结构
val NAV_TABS=listOf(NT("history","历史记录"),NT("movie","电影"),NT("drama","电视剧"),NT("anime","动漫"),NT("variety","综艺"),NT("documentary","纪录片"),NT("news","新闻"),NT("yule","娱乐")) // 分类常量全局配置序列
data class FO(val id:String,val lb:String) // FO (FilterOption) 细分检索标签节点
data class FG(val op:List<FO>) // FG (FilterGroup) 多属性标签组结构
data class VI(val id:String,val tt:String,val pt:String,val sc:String,val ut:String) // VI (VideoListItem) 视频流展示简略项
data class VD(val id:String,val tt:String,val ds:String,val pt:String,val ep:List<String>,val et:List<String>) // VD (VideoDetail) 全量详情数据模型

object PR{ // PR (Prefs) 本地SP键值持久化薄封装单例
	private lateinit var sp:android.content.SharedPreferences // 内部托管SP
	fun init(cx:Context){sp=cx.getSharedPreferences("fyan_prefs",Context.MODE_PRIVATE)} // 初始化取SP引用
	var lt:String // 最终停留的主Tab
		set(v){sp.edit().putString("last_tab",v).apply()}
		get()=sp.getString("last_tab","history")?:"history" // 默认足迹页
	var hs:String // 历史足迹JSON序列化字符串
		set(v){sp.edit().putString("his_json",v).apply()}
		get()=sp.getString("his_json","[]")?:"[]" // 缺省空数组
}

private const val YF="https://api.iyf.tv/api" // 媒体数据源API根地址

suspend fun fG(id:String):List<FG> = withContext(Dispatchers.IO){ // fG (fetchGroups) 拉取分类过滤条件树
	runCatching{
		var j=JSONObject(SF("$YF/list/getfiltertagsdata?SecondaryCode=$id"))
		j=j.optJSONObject("data")?:return@runCatching emptyList()
		val s=j.optJSONArray("list")?:return@runCatching emptyList()
		buildList{
			for(i in 0 until s.length()){
				val x=s.getJSONObject(i)
				val z=x.optJSONArray("list")?:continue
				val op=buildList{
					for(r in 0 until z.length()){
						val o=z.getJSONObject(r)
						add(FO(id=o.optString("classifyId","0"),lb=o.optString("classifyName","")))
					}
				}
				add(FG(op=op))
			}
		}
	}.getOrElse{e->FN.lg("Tags",e.message?:"err",'e');emptyList()}
}

suspend fun fL(id:String,isStr:String,pg:Int,sz:Int=21):List<VI> = withContext(Dispatchers.IO){ // fL (fetchList) 分页拉取视频列表
	runCatching{
		var j=JSONObject(SF("$YF/list/getconditionfilterdata?titleid=${id}&ids=${isStr}&page=${pg}&size=${sz}"))
		j=j.optJSONObject("data")?:return@runCatching emptyList()
		val s=j.optJSONArray("list")?:return@runCatching emptyList()
		buildList{
			for(i in 0 until s.length()){
				val x=s.getJSONObject(i)
				add(VI(id=x.optString("mediaKey",""),tt=x.optString("title",""),pt=x.optString("coverImgUrl",""),sc=x.optString("score",""),ut=x.optString("updateStatus","")))
			}
		}
	}.getOrElse{e->FN.lg("List",e.message?:"err",'e');emptyList()}
}

suspend fun fD(id:String):VD?= withContext(Dispatchers.IO){ // fD (fetchDetail) 深度拉取正片详情全量包
	runCatching{
		var o=JSONObject(SF("$YF/video/videodetails?mediaKey=$id"))
		o=o.optJSONObject("data")?:return@runCatching null
		o=o.optJSONObject("detailInfo")?:return@runCatching null
		val s=o.optJSONArray("episodes")
		val ts=mutableListOf<String>() // 各集展示标题
		val us=mutableListOf<String>() // 各集唯一ID
		if(s!=null){
			for(i in 0 until s.length()){
				val v=s.getJSONObject(i)
				us.add(v.optString("episodeId",""))
				ts.add(v.optString("episodeTitle","${i+1}"))
			}
		}
		VD(ep=us,et=ts,id=id,tt=o.optString("title","").takeIf{it.isNotEmpty()}?:"未知",ds=o.optString("introduce","").takeIf{it.isNotEmpty()}?:"空空如也",pt=o.optString("coverImgUrl","").takeIf{it.isNotEmpty()}?.let{"$it?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg"}?:"")
	}.getOrElse{e->FN.lg("Detail",e.message?:"err",'e');null}
}

suspend fun fS(id:String,vid:String):String= withContext(Dispatchers.IO){ // fS (fetchSource) 解析单集播放直链URL
	runCatching{
		val o=JSONObject(SF("$YF/video/getplaydata?mediaKey=$id&videoId=$vid"))
		val s=o.optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
		var u=""
		// 合并两轮循环：第一优先免费源，无则任意非空源（减少重复迭代）
		for(i in 0 until s.length()){
			val x=s.optJSONObject(i)?:continue
			val mu=x.optString("mediaUrl","")
			if(mu.isEmpty())continue
			if(u.isEmpty())u=mu // 兜底先记录第一个非空
			if(!x.optBoolean("isVip",true)){u=mu;break} // 找到免费源立即停止
		}
		u
	}.getOrElse{e->FN.lg("Source",e.message?:"err",'e');""}
}

fun sH(){ // sH (saveHistory) 序列化历史记录到SP
	val j=org.json.JSONArray()
	FN.hi.forEach{val o=JSONObject();o.put("id",it.id);o.put("tt",it.tt);o.put("pt",it.pt);o.put("pg",it.pg);j.put(o)}
	PR.hs=j.toString()
}
fun lH(){ // lH (loadHistory) 从SP反序列化历史记录
	FN.hi.clear()
	runCatching{
		val j=org.json.JSONArray(PR.hs)
		for(i in 0 until j.length()){val o=j.getJSONObject(i);FN.hi.add(FN.VT(o.optString("id"),o.optString("tt"),o.optString("pt"),o.optString("pg")))}
	}
}
fun aH(im:FN.VT){ // 追加或更新历史记录（去重后置顶）
	FN.hi.removeAll{it.id==im.id} // 移除旧记录
	FN.hi.add(0,im) // 置顶
	sH()
}
fun rH(id:String){FN.hi.removeAll{it.id==id};sH()} // 删除单条历史记录
fun cH(){FN.hi.clear();sH()} // 清空所有历史记录