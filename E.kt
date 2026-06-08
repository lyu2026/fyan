package com.fyan

import org.json.JSONObject
import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 带5秒超时控制的复用客户端
private val HC=okhttp3.OkHttpClient.Builder()
	.connectTimeout(5,java.util.concurrent.TimeUnit.SECONDS)
	.readTimeout(5,java.util.concurrent.TimeUnit.SECONDS).build()

// 封装安全的阻塞式 GET 请求方法（供 Dispatchers.IO 内部复用）
private fun SF(url:String):String{
	val r=okhttp3.Request.Builder().url(url).build()
	HC.newCall(r).execute().use{o->
		if(!o.isSuccessful)throw java.io.IOException("网络请求失败: $response")
		return o.body?.string()?:""
	}
}

data class NT(val id:String,val lb:String) // NT (NavTab) 首页顶部频道常量结构
val NAV_TABS=listOf(NT("history","历史记录"),NT("movie","电影"),NT("drama","电视剧"),NT("anime","动漫"),NT("variety","综艺"),NT("documentary","纪录片"),NT("news","新闻"),NT("yule","娱乐")) // 分类常量全局配置序列
data class FO(val id:String,val lb:String) // FO (FilterOption) 细分检索小标签节点
data class FG(val op:List<FO>) // FG (FilterGroup) 多属性标签组结构
data class VI(val id:String,val tt:String,val pt:String,val sc:String,val ut:String) // VI (VideoListItem) 视频流展示简略项结构
data class VD(val id:String,val tt:String,val ds:String,val pt:String,val ep:List<String>,val et:List<String>) // VD (VideoDetail) 全量详情数据模型

object PR{ // PR (Prefs) 本地磁盘轻量键值SP数据持久化薄操作封装单例
	private lateinit var sp:android.content.SharedPreferences // 内部托管SP
	fun init(cx:Context){sp=cx.getSharedPreferences("fyan_prefs",Context.MODE_PRIVATE)} // 初始化取得磁盘引用
	var lt:String // 最终停留的主Tab记录
		set(v){sp.edit().putString("last_tab",v).apply()} // 就地磁盘同步持久化
		get()=sp.getString("last_tab","history")?:"history" // 默认返回足迹
}

private const val YF="https://api.iyf.tv/api" // 媒体数据源中央服务器根公网API

suspend fun fG(id:String):List<FG> = withContext(Dispatchers.IO){ // fG (fetchGroups) 异步拉取特定分类对应的全量过滤条件属性配置树
	runCatching{ // 开启防御性异常捕获
		var j=JSONObject(SF("$YF/list/getfiltertagsdata?SecondaryCode=$id")) // I/O阻塞拉取纯JSON文本
		j=j.optJSONObject("data")?:return@runCatching emptyList() // 安全解包data大包
		val s=j.optJSONArray("list")?:return@runCatching emptyList() // 安全解包list数据链
		buildList{ // 组装列表
			for(i in 0 until s.length()){ // 层级迭代条件组
				val x=s.getJSONObject(i) // 拿到当前的过滤大项
				val z=x.optJSONArray("list")?:continue // 割取内部包含的细分项
				val op=buildList{ // 迭代解包细分属性小分支
					for(r in 0 until z.length()){ // 小项大迭代
						val o=z.getJSONObject(r) // 提取出单个选项
						add(FO(id=o.optString("classifyId","0"),lb=o.optString("classifyName",""))) // 挂接FO配置节点
					}
				}
				add(FG(op=op)) // 归档大FG配置树
			}
		}
	}.getOrElse{e->FN.lg("Tags",e.message?:"err",'e');emptyList()} // 发生异常熔断返回零长度空集并打入错误日志
}

suspend fun fL(id:String,isStr:String,pg:Int,sz:Int=21):List<VI> = withContext(Dispatchers.IO){ // fL (fetchList) 携带复合拼接参数进行多态流式分页抓取视频列表结果集
	runCatching{ // 异常拦截防御
		var j=JSONObject(SF("$YF/list/getconditionfilterdata?titleid=${id}&ids=${isStr}&page=${pg}&size=${sz}")) // 高速拉取远端列表
		j=j.optJSONObject("data")?:return@runCatching emptyList() // 摘除data外衣
		val s=j.optJSONArray("list")?:return@runCatching emptyList() // 割出视频主体项长序列
		buildList{ // 安全生成可变队列
			for(i in 0 until s.length()){ // 迭代打包卡片模型
				val x=s.getJSONObject(i) // 从索引点提出媒体基本字典
				add(VI(id=x.optString("mediaKey",""),tt=x.optString("title",""),pt=x.optString("coverImgUrl",""),sc=x.optString("score",""),ut=x.optString("updateStatus",""))) // 组合VI视频项入库
			}
		}
	}.getOrElse{e->FN.lg("List",e.message?:"err",'e');emptyList()} // 捕获错误并归档
}

suspend fun fD(id:String):VD?= withContext(Dispatchers.IO){ // fD (fetchDetail) 携媒体主键主路由参数深度抓取正片详情全量包
	runCatching{ // 开启防御性捕获
		var o=JSONObject(SF("$YF/video/videodetails?mediaKey=$id")) // 基础请求拉取
		o=o.optJSONObject("data")?:return@runCatching null // 摘取data
		o=o.optJSONObject("detailInfo")?:return@runCatching null // 定位具体核心信息载体detailInfo字典
		val s=o.optJSONArray("episodes") // 割取出剧集列表大字段
		val ts=mutableListOf<String>() // 正向保存各集展示标语
		val us=mutableListOf<String>() // 存储集数跳转用唯一主键哈希口令
		if(s!=null){ // 判定剧集有效性
			for(i in 0 until s.length()){ // 迭代切分集数数据
				val v=s.getJSONObject(i) // 摸出当前分集
				us.add(v.optString("episodeId","")) // 注入Key
				ts.add(v.optString("episodeTitle","${i+1}")) // 填充标题名
			}
		}
		VD(ep=us,et=ts,id=id,tt=o.optString("title",""),ds=o.optString("introduce",""),pt=o.optString("coverImgUrl","")+"?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg") // 压印返回高定制宽画幅VD详情实体
	}.getOrElse{e->FN.lg("Detail",e.message?:"err",'e');null} // 崩溃降级丢回空壳引用并记日志
}

suspend fun fS(id:String,vid:String):String= withContext(Dispatchers.IO){ // fS (fetchSource) 高级解密正片特定单集哈希密钥转换回公网可播放物理直连URL大地址
	runCatching{ // 异常防护层
		val o=JSONObject(SF("$YF/video/getplaydata?mediaKey=$id&videoId=$vid")) // 网络拉回播放元源字典数据
		val s=o.optJSONObject("data")?.optJSONArray("list")?:return@runCatching "" // 斩获核心排布通道数据链
		var u:String="" // 直连视频大长URL承载暂存器
		for(i in 0 until s.length()){ // 条件大搜索
			val x=s.optJSONObject(i)?:continue // 取出当前条目
			val mu=x.optString("mediaUrl","") // 取出播放链接
			if(mu.isNotEmpty()&&!x.optBoolean("isVip",true)){u=mu;break} // 优先取非VIP且链接有效的免费源
		}
		if(u.isEmpty())for(i in 0 until s.length()){ // 若无免费源则降级回退取任意非空链接
			val mu=s.optJSONObject(i)?.optString("mediaUrl","")?:"" // 兜底取链接
			if(mu.isNotEmpty()){u=mu;break} // 取到即止
		}
		u // 奉还播放公网流直连地址
	}.getOrElse{e->FN.lg("Source",e.message?:"err",'e');""} // 遇挫落空吐空壳字串
}

fun aH(im:FN.VT){ // 历史记录内存更新追加方法
	FN.hi.removeAll{it.id==im.id} // 先行干掉当前内存数组中存在的旧有同媒体主键历史项（去重）
	FN.hi.add(0,im) // 将最新交互产生的新历史卡片足迹强行推入到队列第0位（时序最新排前）
}
fun rH(id:String){FN.hi.removeAll{it.id==id}} // 定向擦除单条特定的足迹卡片记录
fun cH()=FN.hi.clear() // 全面归零当前内存全量足迹队列
