package com.fyan

import java.net.URL
import org.json.JSONObject
import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ════════════════════════════════════════════════════════════════
// 数据模型
// ════════════════════════════════════════════════════════════════
// 首页顶栏 tab 定义
data class NavTab(val id:String,val label:String)
val NAV_TABS=listOf(
	NavTab("history","历史记录"),
	NavTab("movie","电影"),
	NavTab("drama","电视剧"),
	NavTab("anime","动漫"),
	NavTab("variety","综艺"),
	NavTab("documentary","纪录片"),
	NavTab("news","新闻"),
	NavTab("yule","娱乐"),
)
// 筛选 tag 数据
data class FilterOption(val id:String,val label:String)
data class FilterGroup(val name:String,val options:List<FilterOption>)
// 视频列表项
data class VideoListItem(
	val id:String,
	val title:String,
	val poster:String,
	val score:String,
	val update:String,
)
// 视频详情
data class VideoDetail(
	val id:String,
	val title:String,val desc:String,val poster:String,
	val episodes:List<String>,val episodeTitles:List<String>,
)

// ════════════════════════════════════════════════════════════════
// 本地缓存 —— SharedPreferences 薄封装
// ════════════════════════════════════════════════════════════════
object Prefs{
	private lateinit var sp:android.content.SharedPreferences
	fun init(c:Context){
		sp=c.getSharedPreferences("fyan_prefs",Context.MODE_PRIVATE)
	}
	var lastTab:String
		set(v){sp.edit().putString("last_tab",v).apply()}
		get()=sp.getString("last_tab","history")?:"history"
}

// ════════════════════════════════════════════════════════════════
// 网络请求
// ════════════════════════════════════════════════════════════════
private const val YF="https://api.iyf.tv/api"
// 获取筛选 tag 数据
suspend fun fetchFilterTags(id:String):List<FilterGroup>=withContext(Dispatchers.IO){
	runCatching{
		var j=JSONObject(URL("$YF/list/getfiltertagsdata?SecondaryCode=$id").readText())
		j=j.optJSONObject("data")?:return@runCatching emptyList()
		val s=j.optJSONArray("list")?:return@runCatching emptyList()
		buildList{
			for(i in 0 until s.length()){
				val x=s.getJSONObject(i)
				val name=x.optString("name","")
				val z=x.optJSONArray("list")?:continue
				val options=buildList{
					for(r in 0 until z.length()){
						val o=z.getJSONObject(r)
						add(FilterOption(
							id=o.optString("classifyId","0"),
							label=o.optString("classifyName",""),
						))
					}
				}
				add(FilterGroup(name=name,options=options))
			}
		}
	}.getOrElse{e->
		Fyan.log("拉取筛选数据",e.message?:"未知除外",'e')
		emptyList()
	}
}
// 分页获取筛选视频列表
suspend fun fetchVideoList(id:String,ids:String,page:Int,size:Int=21):List<VideoListItem>=withContext(Dispatchers.IO){
	runCatching{
		var j=JSONObject(URL("$YF/list/getconditionfilterdata?titleid=$id&ids=$ids&page=$page&size=$size").readText())
		j=j.optJSONObject("data")?:return@runCatching emptyList()
		val s=j.optJSONArray("list")?:return@runCatching emptyList()
		buildList{
			for(i in 0 until s.length()){
				val x=s.getJSONObject(i)
				add(VideoListItem(
					title=x.optString("title",""),
					score=x.optString("score",""),
					id=x.optString("mediaKey",""),
					poster=x.optString("coverImgUrl",""),
					update=x.optString("updateStatus",""),
				))
			}
		}
	}.getOrElse{e->
		Fyan.log("拉取视频列表数据",e.message?:"未知错误",'e')
		emptyList()
	}
}
// 获取视频详情
suspend fun fetchVideoDetail(id:String):VideoDetail?=withContext(Dispatchers.IO){
	runCatching{
		var o=JSONObject(URL("$YF/video/videodetails?mediaKey=$id").readText())
		o=o.optJSONObject("data")?:return@runCatching null
		o=o.optJSONObject("detailInfo")?:return@runCatching null
		val s=o.optJSONArray("episodes")
		val ts=mutableListOf<String>()
		val ks=mutableListOf<String>()
		if(s!=null){
			for(i in 0 until s.length()){
				val v=s.getJSONObject(i)
				ts.add(v.optString("episodeTitle","${i+1}"))
				ks.add(v.optString("episodeKey",""))
			}
		}
		VideoDetail(
			episodes=ks,episodeTitles=ts,
			id=id,title=o.optString("title",""),
			desc=o.optString("introduce",""),
			poster=o.optString("coverImgUrl",""),
		)
	}.getOrElse{e->
		Fyan.log("拉取视频详情",e.message?:"未知错误",'e')
		null
	}
}

// ════════════════════════════════════════════════════════════════
// 历史记录 —— 简单内存管理（可扩展持久化）
// ════════════════════════════════════════════════════════════════
// 添加或更新历史记录（最新在前）
fun addHistory(item:Fyan.VideoItem){
	Fyan.history.removeAll{it.id==item.id}
	Fyan.history.add(0,item)
}
fun removeHistory(id:String){
	Fyan.history.removeAll{it.id==id}
}
fun clearHistory()=Fyan.history.clear()
