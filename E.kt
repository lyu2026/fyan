package com.fyan

import org.json.JSONObject
import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 全局共享OkHttpClient：统一超时配置+跟随重定向，cu()更新检测也复用此实例
internal val HC=okhttp3.OkHttpClient.Builder()
	.connectTimeout(5,java.util.concurrent.TimeUnit.SECONDS)
	.readTimeout(5,java.util.concurrent.TimeUnit.SECONDS)
	.writeTimeout(5,java.util.concurrent.TimeUnit.SECONDS) // 防止慢速上传阻塞
	.followRedirects(true) // 统一开启重定向跟随，版本检测接口需要
	.build()

// 封装阻塞式GET请求，仅供Dispatchers.IO内部调用
private fun SF(url:String):String{
	val r=okhttp3.Request.Builder().url(url).build()
	HC.newCall(r).execute().use{o->
		if(!o.isSuccessful)throw java.io.IOException("网络请求失败: $o")
		return o.body?.string()?:""
	}
}

data class NT(val id:String,val lb:String) // 顶部导航Tab常量结构（id=路由标识/lb=显示名）
val NAV_TABS=listOf(NT("history","历史记录"),NT("movie","电影"),NT("drama","电视剧"),NT("anime","动漫"),NT("variety","综艺"),NT("documentary","纪录片"),NT("news","新闻"),NT("yule","娱乐")) // 全局导航Tab配置表
data class FO(val id:String,val lb:String) // 过滤选项节点（id=classifyId/lb=显示标签）
data class FG(val op:List<FO>) // 过滤选项分组，包含一组互斥选项
data class VI(val id:String,val type:String,val tt:String,val pt:String,val sc:String,val ut:String) // 视频列表简略项（id/type=类型/tt=标题/pt=封面/sc=评分/ut=更新状态）
data class VD(val id:String,val tt:String,val ds:String,val pt:String,val ep:List<String>,val et:List<String>) // 视频详情全量模型（ep=各集episodeId列表/et=各集标题列表）

object PR{ // 本地SharedPreferences薄封装，持久化用户偏好
	private lateinit var sp:android.content.SharedPreferences // SP实例，由init()注入
	fun init(cx:Context){sp=cx.getSharedPreferences("fyan_prefs",Context.MODE_PRIVATE)} // 应用启动时初始化
	var lt:String // 最后访问的Tab id，下次启动时恢复
		set(v){sp.edit().putString("last_tab",v).apply()}
		get()=sp.getString("last_tab","history")?:"history" // 默认历史记录Tab
	var hs:String // 历史记录JSON序列化字符串
		set(v){sp.edit().putString("his_json",v).apply()}
		get()=sp.getString("his_json","[]")?:"[]" // 缺省空数组
}

private const val YF="https://api.iyf.tv/api" // 媒体数据源API根地址

suspend fun fG(id:String):List<FG> = withContext(Dispatchers.IO){ // 拉取指定分类的过滤条件树
	runCatching{
		when{
			id=="news"->buildList{
				val op=buildList{
					add(FO(id="国际",lb="国际"))
					add(FO(id="国内",lb="国内"))
					add(FO(id="华人资讯",lb="华人资讯"))
					add(FO(id="财经",lb="财经"))
					add(FO(id="军事",lb="军事"))
				}
				add(FG(op=op))
			}
			else->{
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
			}
		}
	}.getOrElse{e->FN.lg("Tags",e.message?:"err",'e');emptyList()}
}

suspend fun fL(id:String,isStr:String,pg:Int,sz:Int=21):List<VI> = withContext(Dispatchers.IO){ // 分页拉取视频列表（sz默认21，≥21则认为有下一页）
	runCatching{
		var j=JSONObject(SF("$YF${if(id=="news")"/home/getrelativevideosbysub?titleid=${id}&Tags=${isStr}"else"/list/getconditionfilterdata?titleid=${id}&ids=${isStr}"}&page=${pg}&size=${sz}"))
		j=j.optJSONObject("data")?:return@runCatching emptyList()
		val s=j.optJSONArray("list")?:return@runCatching emptyList()
		buildList{
			for(i in 0 until s.length()){
				val x=s.getJSONObject(i)
				add(VI(id=x.optString("mediaKey",""),type=x.optString("videoType","1"),tt=x.optString("title",""),pt=x.optString("coverImgUrl",""),sc=x.optString("score",""),ut=x.optString("updateStatus","")))
			}
		}
	}.getOrElse{e->FN.lg("List",e.message?:"err",'e');emptyList()}
}

suspend fun fD(id:String):VD?= withContext(Dispatchers.IO){ // 拉取视频完整详情（含集数列表）
	runCatching{
		var o=JSONObject(SF("$YF/video/videodetails?mediaKey=$id"))
		o=o.optJSONObject("data")?:return@runCatching null
		o=o.optJSONObject("detailInfo")?:return@runCatching null
		val s=o.optJSONArray("episodes")
		val ts=mutableListOf<String>() // 各集展示标题
		val us=mutableListOf<String>() // 各集episodeId（播放解析用）
		if(s!=null){
			for(i in 0 until s.length()){
				val v=s.getJSONObject(i)
				us.add(v.optString("episodeId",""))
				ts.add(v.optString("episodeTitle","${i+1}")) // 无标题则用序号
			}
		}
		VD(ep=us,et=ts,id=id,tt=o.optString("title","").takeIf{it.isNotEmpty()}?:"未知",ds=o.optString("introduce","").takeIf{it.isNotEmpty()}?:"空空如也",pt=o.optString("coverImgUrl","").takeIf{it.isNotEmpty()}?.let{"$it?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg"}?:"")
	}.getOrElse{e->FN.lg("Detail",e.message?:"err",'e');null}
}

suspend fun fS(id:String,type:String,vid:String=""):String= withContext(Dispatchers.IO){ // 解析单集播放直链：优先返回免费源，无则返回第一个非空源
	runCatching{
		val o=JSONObject(SF("$YF/video/getplaydata?mediaKey=$id&videoId=$vid&videoType=$type"))
		val s=o.optJSONObject("data")?.optJSONArray("list")?:return@runCatching ""
		var u=""
		// 单次遍历：先记录第一个非空兜底，遇到免费源立即中断
		for(i in 0 until s.length()){
			val x=s.optJSONObject(i)?:continue
			val mu=x.optString("mediaUrl","")
			if(mu.isEmpty())continue
			if(u.isEmpty())u=mu // 兜底：记录第一个非空源
			// isVip默认false（保守策略：字段缺失时假设免费）
			if(!x.optBoolean("isVip",false)){u=mu;break} // 找到免费源立即停止
		}
		u
	}.getOrElse{e->FN.lg("Source",e.message?:"err",'e');""}
}

fun sH(){ // 序列化历史记录到SP
	val j=org.json.JSONArray()
	FN.hi.forEach{val o=JSONObject();o.put("id",it.id);o.put("tt",it.tt);o.put("pt",it.pt);o.put("pg",it.pg);j.put(o)}
	PR.hs=j.toString()
}
fun lH(){ // 从SP反序列化历史记录到内存队列
	FN.hi.clear()
	runCatching{
		val j=org.json.JSONArray(PR.hs)
		for(i in 0 until j.length()){val o=j.getJSONObject(i);FN.hi.add(FN.VT(o.optString("id"),o.optString("tt"),o.optString("pt"),o.optString("pg")))}
	}
}
fun aH(im:FN.VT){ // 追加或更新历史记录（同id去重后置顶）
	FN.hi.removeAll{it.id==im.id} // 先移除旧记录（避免重复）
	FN.hi.add(0,im) // 置顶插入
	sH() // 立即持久化
}
fun rH(id:String){FN.hi.removeAll{it.id==id};sH()} // 删除单条历史记录并持久化
fun cH(){FN.hi.clear();sH()} // 清空所有历史记录并持久化