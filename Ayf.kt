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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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

@Composable fun AyfHome(){ // 爱壹帆主页分类容器
	val s=listOf("history" to "历史记录","movie" to "电影","drama" to "剧集","anime" to "动漫","variety" to "综艺","documentary" to "纪录片") // 分类标签映射表
	val t by Fyan.cg("ayf_tab","history").collectAsState(initial="history") // 监听当前选中的激活标签
	val sc=rememberCoroutineScope() // 协程生命周期作用域
	val cc=Fyan.cc // 缓存全局主题单例，更改命名彻底规避局部作用域重名冲突
	val f=Fyan.ff // 缓存全局字体规格单例
	Fyan.log("路由","进入爱壹帆首页") // 记录页面导航日志
	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){ // 主视窗纵向背景底板
		Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(cc.cg).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){ // 分类标签栏横向滚动长卷
			s.forEach{o->Box(modifier=Modifier.fillMaxHeight().background(if(o.first==t)cc.primary.copy(alpha=0.15f)else Color.Transparent).clickable{ // 渲染分类项标签卡
				Fyan.log("爱壹帆","点击TAB: "+o.first,'u') // 记录用户交互点击
				sc.launch{Fyan.cs("ayf_tab",o.first)} // 异步持久化当前选中的分类
			},contentAlignment=Alignment.Center){
				BasicText("  ${o.second}  ",style=f.h4.copy(color=if(o.first==t)cc.primary else cc.c.copy(alpha=0.7f),fontWeight=if(o.first==t)FontWeight.Bold else FontWeight.Normal)) // 动态高亮文本字态
			}}
		}
		Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd)) // 导航栏底部极细分割线
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){ // 视图内容动态填充区
			key(t){when(t){"history"->AyfHistory();else->AyfList(id=t)}} // 使用专属Key隔离重构状态，防止交叉污染
		}
	}
}

@Composable fun AyfHistory(){ // 观看历史记录组件
	val cc=Fyan.cc // 缓存全局颜色方案单例
	val f=Fyan.ff // 缓存全局字体规格单例
	val cn=if(Fyan.sw>=840)5 else if(Fyan.sw>=600)4 else 3 // 依据屏幕宽度弹性计算历史网格列数
	var id by remember{mutableStateOf<String?>(null)} // 暂存长按待删条目的影视Key凭证
	var hc by remember{mutableStateOf(false)} // 全局清空历史确认弹窗控制器开关
	val sc=rememberCoroutineScope() // 获取协程异步任务分发句柄
	val rs by Fyan.cg("ayf_history","").collectAsState(initial="") // 响应式监听本地序列化历史文本流
	val vs=remember(rs){ // 缓存并增量解析历史记录文本行
		if(rs.isBlank())emptyList()else rs.lines().filter{it.isNotBlank()}.mapNotNull{
			val x=it.trim().split(Regex("\\s+"),6) // 空间正则高效切割六大核心维度字段
			if(x.size<6)null else mapOf("id" to x[0],"type" to x[1],"title" to x[2],"cover" to x[3],"ec" to x[4],"ps" to x[5]) // 字典化打包归档
		}
	}
	Fyan.log("路由","进入爱壹帆历史记录页") // 输出模块导航审计轨迹
	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){ // 历史视窗总纵向容器
		Row(modifier=Modifier.fillMaxWidth().height(30.dp).padding(start=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){ // 顶部标题工具操作行
			BasicText("记录清单",style=f.pb.copy(color=cc.c)) // 模块大标题展示
			Box(modifier=Modifier.size(28.dp).clickable{hc=true},contentAlignment=Alignment.Center){BasicText("🗑",style=f.h4.copy(color=cc.c))} // 全局一键清除图标热区
		}
		if(vs.isEmpty())Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ // 空历史占位缺省提示
			BasicText("暂无观看记录",style=f.p.copy(color=cc.c.copy(alpha=0.4f))) // 引导文本输出
		}else LazyVerticalGrid(modifier=Modifier.fillMaxWidth().weight(1f),columns=GridCells.Fixed(cn),contentPadding=PaddingValues(4.dp),verticalArrangement=Arrangement.spacedBy(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){ // 历史海报网格流
			items(vs,{it["id"]!!}){o-> // 绑定唯一影视凭证作Key增量渲染
				Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(cc.cg).pointerInput(o["id"]!!){ // 单体海报卡片槽位
					detectTapGestures(onTap={Fyan.goto("ayf_info/"+o["id"])},onLongPress={id=o["id"]}) // 复合绑定单击跳转续播与长按唤起单条删除
				},horizontalAlignment=Alignment.CenterHorizontally){
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(cc.ag),contentAlignment=Alignment.BottomCenter){ // 约束海报物理黄金宽高比例
						AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop) // 异步流式加载影视海报
						if(o["ec"]!="0")Box(modifier=Modifier.padding(4.dp).background(cc.m,RoundedCornerShape(1.dp)).padding(horizontal=4.dp,vertical=2.dp)){ // 上次足迹集数浮层
							BasicText("第${o["ec"]}集",style=f.ps.copy(color=Color.White)) // 集数文本输出
						}
					}
					BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=f.ps.copy(color=cc.c,textAlign=TextAlign.Center)) // 影视标题单行截断
				}
			}
		}
	}
	if(hc)Dialog(onDismissRequest={hc=false}){ // 全量毁灭清空的二次确认拦截弹窗
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(cc.cg).border(1.dp,cc.bd,RoundedCornerShape(3.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){ // 弹窗材质底座
			BasicText("清空历史记录？",style=f.h3.copy(color=cc.c)) // 核心风险说明文案
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){ // 操作交互行
				BasicText("取消",modifier=Modifier.clickable{Fyan.log("爱壹帆","取消清空历史记录",'s');hc=false}.padding(8.dp),style=f.p.copy(color=cc.c)) // 安全撤销返回
				BasicText("确定",modifier=Modifier.clickable{Fyan.log("爱壹帆","执行清空历史记录",'w');sc.launch{Fyan.cs("ayf_history","")};hc=false}.padding(8.dp),style=f.p.copy(color=cc.primary)) // 确立清空并清空本地存储文本
			}
		}
	}
	if(id!=null)Dialog(onDismissRequest={id=null}){ // 单条记录选择性抹除的二次确认拦截弹窗
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(cc.cg).border(1.dp,cc.bd,RoundedCornerShape(3.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){ // 弹窗材质底座
			BasicText("删除此记录？",style=f.h3.copy(color=cc.c)) // 精准风险说明文案
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){ // 行为操作控制行
				BasicText("取消",modifier=Modifier.clickable{Fyan.log("爱壹帆","取消删除历史记录，编号: $id",'e');id=null}.padding(8.dp),style=f.p.copy(color=cc.c)) // 撤回并释放锁定的影视ID指针
				BasicText("确定",modifier=Modifier.clickable{ // 确立执行移除
					val o=rs.lines().filter{it.isNotBlank()&&!it.startsWith("${id!!} ")}.joinToString("\n") // 正则流过滤剔除精准目标影视行
					Fyan.log("爱壹帆","执行删除历史记录，编号: $id",'d') // 输出底层调试信息
					sc.launch{Fyan.cs("ayf_history",o)};id=null // 持久化刷新数据并移除锁定
				}.padding(8.dp),style=f.p.copy(color=cc.primary))
			}
		}
	}
}

@Composable fun AyfList(id:String){ // 爱壹帆多维条件筛选列表组件
	val cc=Fyan.cc // 统一就地捕获色彩方案
	val f=Fyan.ff // 统一就地捕获字体规格
	var vs by remember{mutableStateOf(emptyList<Map<String,String>>())} // 当前展示流的视频多媒体数据集合
	var fs by remember{mutableStateOf(emptyList<List<Pair<String,String>>>())} // 多维综合过滤标签池矩阵
	val fc=remember{mutableStateMapOf<Int,String>()} // 多维度条件状态锁定映射字典
	var X by remember{mutableStateOf(true)} // 接口全局数据首屏骨架加载锁
	var pg by remember{mutableIntStateOf(1)} // 后台接口分页流页码累加器
	var hm by remember{mutableStateOf(true)} // 后台流式数据可拉取余量边界开关
	var lm by remember{mutableStateOf(false)} // 并发控制互斥锁：阻断高频触底冲击造成重复请求
	val gs=rememberLazyGridState() // 挂载网格专用滚动监听状态器
	val sc=rememberCoroutineScope() // 获取挂载协程轻量级分发域
	Fyan.log("路由","进入爱壹帆筛选列表页") // 输出页面跳转追踪埋点
	fun au(p:Int=1)="https://api.iyf.tv/api/list/getconditionfilterdata?titleid=$id&ids=${fc.entries.sortedBy{it.key}.joinToString(","){it.value}}&page=$p&size=21" // 表达式内嵌拼装高级筛选URL网络路径
	suspend fun fv(u:String):List<Map<String,String>> = withContext(Dispatchers.IO){ // 切入IO高能密集型池拉取远程多媒体序列
		runCatching<List<Map<String,String>>>{ // 稳健捕获网络和解析阶段的所有意外漏洞
			Fyan.log("爱壹帆","获取筛选视频列表，链接: $u",'d') // 记录物理网络层事件日志
			val j=JSONObject(Fyan.fetch(u)).optJSONObject("data")?:return@runCatching emptyList() // 截留并解析核心高阶响应对象
			val s=j.optJSONArray("list")?:return@runCatching emptyList() // 媒体子代行记录列表
			buildList{ // 高效映射打包成业务用轻量字典
				for(i in 0 until s.length()){ // 循环抓取实体
					val v=s.getJSONObject(i) // 提取独立JSON节点
					add(mapOf("id" to v.optString("mediaKey",""),"type" to v.optString("videoType","1"),"title" to v.optString("title",""),"cover" to v.optString("coverImgUrl",""),"score" to v.optString("score",""),"tip" to v.optString("updateStatus",""))) // 压入规范化映射集合
				}
			}
		}.getOrElse{emptyList()} // 出错时安全自我熔断返回空集合
	}
	LaunchedEffect(id){ // 分类变更引发的多维过滤器初始化及数据建立全生命周期
		fs=withContext(Dispatchers.IO){ // 无阻塞IO线程拉取筛选条件维度参数
			runCatching<List<List<Pair<String,String>>>>{ // 阻断意外解析漏洞
				Fyan.log("爱壹帆","获取筛选数据，TAB: $id",'s') // 输出筛选维度拉取事件日志
				val j=JSONObject(Fyan.fetch("https://api.iyf.tv/api/list/getfiltertagsdata?SecondaryCode=$id")).optJSONObject("data")?:return@runCatching emptyList() // 远程握手
				val s=j.optJSONArray("list")?:return@runCatching emptyList() // 解析列表
				buildList{ // 矩阵嵌套解包
					for(i in 0 until s.length()){ // 遍历轴级维度
						val z=s.getJSONObject(i).optJSONArray("list")?:continue // 抓取项级矩阵
						add(buildList{for(r in 0 until z.length()){val o=z.getJSONObject(r);add(o.optString("classifyId","0") to o.optString("classifyName",""))}}) // 二维展平解包对齐
					}
				}
			}.getOrElse{emptyList()} // 矩阵防崩溃备份回滚机制
		}
		fs.indices.forEach{i->fc[i]="0"} // 重置并将所有维度的条件全部初始化归零对齐
		pg=1;hm=true // 重置页码边界基础锚点
		vs=fv(au(1));X=false // 加载第一卷首屏流媒体并解除全局骨架屏加载锁
	}
	LaunchedEffect(gs){ // 滚动展板触底智能增量分页效应器
		snapshotFlow{gs.layoutInfo.visibleItemsInfo.lastOrNull()?.index}.collect{li-> // 滚动流高精密提取末尾可见单元索引
			if(li!=null&&li>=vs.size-Fyan.gc*2&&hm&&!lm&&!X){ // 触及预设安全水位线余量校验
				lm=true;val px=pg+1 // 并发卡扣加锁并递增当前页码计数器
				Fyan.log("爱壹帆","触底加载第${px}页",'d') // 记录追加更新事件
				val mr=fv(au(px)) // 高效调取网络接续分卷数据
				if(mr.isEmpty()){hm=false;Fyan.log("爱壹帆","已加载全部数据",'s')}else{vs=vs+mr;pg=px} // 决策融合追加或熔断边界控制
				lm=false // 释放并发加载护栏锁
			}
		}
	}
	Column(modifier=Modifier.fillMaxSize().background(cc.bg)){ // 综合筛选主视窗纵向总布局
		if(fs.isNotEmpty())Column(modifier=Modifier.fillMaxWidth().background(cc.cg)){ // 多维传送带过滤器容器排
			fs.forEachIndexed{i,g-> // 排队遍历各过滤维度长廊
				Row(modifier=Modifier.fillMaxWidth().height(20.dp).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){ // 单条维度横向自由流动长卷
					g.forEach{(fi,fn)-> // 铺装维度内包含的每个过滤器胶囊
						val at=fi==fc[i] // 甄别比对当前项是否被用户激活点击选中
						Box(modifier=Modifier.fillMaxHeight().background(if(at)cc.primary.copy(alpha=0.15f)else Color.Transparent).clickable{ // 过滤器点击事件
							fc[i]=fi;sc.launch{X=true;pg=1;hm=true;vs=fv(au(1));X=false} // 重调选中字典并强力异步推倒重建当前分类下的第一页视图数据
						},contentAlignment=Alignment.Center){BasicText("  $fn  ",style=f.ps.copy(color=if(at)cc.primary else cc.c.copy(alpha=0.7f),fontWeight=if(at)FontWeight.W600 else FontWeight.W400))} // 属性样式自适应
					}
				}
				Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd)) // 过滤器单廊底层精密间隔线
			}
		}
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){ // 核心流式影视多媒体网格展现大舞台
			when{ // 核心状态流分支控制树
				X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){BasicText("◌ 加载中…",style=f.pb.copy(color=cc.c.copy(alpha=0.4f)))} // 首屏就绪骨架屏状态
				vs.isEmpty()->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){BasicText("暂无视频",style=f.p.copy(color=cc.c.copy(alpha=0.4f)))} // 接口无数据真空缺省提示状态
				else->LazyVerticalGrid(state=gs,columns=GridCells.Fixed(Fyan.gc),contentPadding=PaddingValues(4.dp),verticalArrangement=Arrangement.spacedBy(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){ // 高性能多列响应式网格渲染核心
					items(vs,{it["id"]!!}){o-> // 绑定唯一影视凭证ID作高性能无缝Diff比对
						Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(cc.cg).clickable{ // 单媒体矩形立体海报卡
							Fyan.log("爱壹帆","进入详情页，编号: "+o["id"]);Fyan.goto("ayf_info/"+o["id"]) // 点击记录轨迹日志并全速下发路由总线跃迁指令
						},horizontalAlignment=Alignment.CenterHorizontally){
							Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(cc.ag)){ // 黄金海报框占比物理容器
								AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop) // 异步高敏加载影视封面大图
								if(!o["score"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.TopStart).padding(2.dp,6.dp).background(cc.m,RoundedCornerShape(10.dp)).padding(2.dp)){BasicText(o["score"]!!,style=f.ps.copy(color=cc.c))} // 左上角影视评分防反差浮动标志层
								if(!o["tip"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.BottomCenter).padding(2.dp,6.dp).background(cc.m,RoundedCornerShape(6.dp)).padding(2.dp)){BasicText(o["tip"]!!,style=f.ps.copy(color=cc.c))} // 底部更新状态/清晰度动态浮层
							}
							BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=f.ps.copy(color=cc.c,textAlign=TextAlign.Center)) // 视频多媒体官方中文译名
						}
					}
					if(lm)item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}){Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){BasicText("◌ 加载更多…",style=f.ps.copy(color=cc.c.copy(alpha=0.4f)))}} // 流追加衔接加载条组件
					if(!hm&&vs.isNotEmpty())item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}){Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){BasicText("꧁ 已加载全部 ꧂",style=f.ps.copy(color=cc.c.copy(alpha=0.3f)))}} // 终点流终结符尾饰
				}
			}
		}
	}
}

@Composable fun AyfInfo(id:String){ // 爱壹帆高级无缝续播影视详情组件
	val act=run{ // 鲁棒循环递归上下文穿透链：完全免疫任何高阶拦截层（如Hilt、深色切换器等）精准提取出底层物理 Activity 实例指针
		var ctx=LocalContext.current
		while(ctx is android.content.ContextWrapper){if(ctx is Activity)return@run ctx;ctx=ctx.baseContext};ctx as? Activity
	}
	var O by remember{mutableStateOf<Map<String,Any>?>(null)} // 后台下发的视频详情多维核心解压结构字典
	var X by remember{mutableStateOf(true)} // 详情视窗首屏异步全局数据骨架加载锁
	// 极致多端旋转保障：核心状态变量全部跨级指派为 rememberSaveable，横竖屏物理翻转重建时数据100%完美续存
	var ec by rememberSaveable{mutableIntStateOf(-1)} // 激活剧集锁定索引锚点
	var ep by rememberSaveable{mutableLongStateOf(0L)} // 物理音视频播放轴毫秒位置计数器
	var uc by rememberSaveable{mutableStateOf("")} // 物理底层多媒体流真实网络串流路径
	var pr by rememberSaveable{mutableStateOf(false)} // 用户确认激发视频起播的状态屏障控制闸
	var fs by rememberSaveable{mutableStateOf(false)} // 顶级无阻隔全屏图层平铺状态开关
	val cc=Fyan.cc // 顶层统一捕获色彩方案单例，彻底切断重组过程中多余的局部State状态对齐消耗
	val f=Fyan.ff // 顶层统一捕获文字规格单例，提升执行效率
	val sc=rememberCoroutineScope() // 本地轻量协程工厂句柄
	Fyan.log("路由","进入爱壹帆视频详情页") // 输出模块总线埋点日志
	LaunchedEffect(id){ // 入口级影视大元数据解析加载生命周期
		X=true;pr=false;uc="" // 重刷并清洗重置播放状态环境
		O=withContext(Dispatchers.IO){ // 全速转入手势无关的后台IO核心池执行网络吞吐
			runCatching<Map<String,Any>?>{ // 护栏拦截异常阻断潜在的解析崩盘漏洞
				val j=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/videodetails?mediaKey=$id")).optJSONObject("data")?.optJSONObject("detailInfo")?:return@runCatching null // 握手拉取大对象
				val x=j.optJSONArray("episodes") // 抽离关联集数行集合
				val s=mutableListOf<Map<String,String>>() // 初创选集矩阵
				if(x!=null)for(i in 0 until x.length()){val v=x.getJSONObject(i);s.add(mapOf("id" to v.optString("episodeId",""),"title" to v.optString("episodeTitle","${i+1}")))} // 归一化提取各集元数据
				mapOf("id" to id,"type" to j.optString("videoType","1"),"title" to j.optString("title","未知"),"brief" to j.optString("introduce","空空如也"),"cover" to j.optString("coverImgUrl",""),"s" to s) // 返回全量归档字典
			}.getOrElse{null}
		}
		X=false // 全局卸载首屏大骨架屏遮罩锁
		if(O!=null){ // 判定追溯本地足迹历史序列
			val h=Fyan.co("ayf_history","") // 读取持久化文本原始行
			val x=h.lines().firstOrNull{it.startsWith("$id ")} // 定向搜寻当前影视痕迹
			if(x.isNullOrBlank()){ec=0;ep=0L;Fyan.cs("ayf_history",(listOf("$id ${O!!["type"]} ${O!!["title"]} ${O!!["cover"]} 0 0")+h.lines().filter{it.isNotEmpty()}).joinToString("\n"))} // 处女航全新写入置顶初值
			else{val s=x.trim().split(Regex("\\s+"),6);ec=s.getOrElse(4){"0"}.toIntOrNull()?:0;ep=s.getOrElse(5){"0"}.toLongOrNull()?:0L} // 原地还原追溯上次离线时的集数和具体时间轴进度
		}
	}
	LaunchedEffect(ec){ // 切集与物理源分发解算生命周期
		if(O==null||ec==-1)return@LaunchedEffect // 容错护栏隔离
		pr=false;uc="" // 卸载清洗并关闭上一次残留的物理解码画布渲染管线
		val h=Fyan.co("ayf_history","") // 调取序列化历史根文本
		val x=h.lines().firstOrNull{it.startsWith("$id ")} // 检索目标影视行
		if(x.isNullOrBlank())ep=0L else{val s=x.trim().split(Regex("\\s+"),6);if(s.getOrElse(4){"0"}.toIntOrNull()?:0==ec)ep=s.getOrElse(5){"0"}.toLongOrNull()?:0L else ep=0L} // 校正同集进度续播或切集清零
		@Suppress("UNCHECKED_CAST") val ei=(O!!["s"] as List<Map<String,String>>).getOrNull(ec)?.get("id")?:"" // 获取关联剧集的密钥
		val url=withContext(Dispatchers.IO){ // 无阻塞IO池拉取底层音视频串流路径
			runCatching<String>{ // 阻断意外解析崩溃
				val s=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/getplaydata?mediaKey=$id&videoId=$ei&videoType=${O!!["type"]}")).optJSONObject("data")?.optJSONArray("list")?:return@runCatching "" // 数据解包
				var u="" // 临时串流路径缓冲区
				for(i in 0 until s.length()){val v=s.getJSONObject(i).optString("mediaUrl","");if(v.isNotEmpty()){u=v;break}};u // 命中输出首条高可用播放流
			}.getOrElse{""}
		}
		uc=url // 赋予物理源绑定起跑，启动媒体装填动作
		Fyan.cs("ayf_history",(listOf("$id ${O!!["type"]} ${O!!["title"]} ${O!!["cover"]} ${ec} ${ep}")+h.lines().filter{it.isNotEmpty()&&!it.startsWith("${id} ")}).joinToString("\n")) // 全新覆写最新记录并将足迹置顶排列
	}
	val P:ExoPlayer?=remember(uc){if(uc.isNotEmpty())ExoPlayer.Builder(Fyan.me).build() else null} // 响应串流变化构建解码器
	LaunchedEffect(P,uc){ // 媒体底层物理内核数据流注入与历史寻道连
		P?.apply{val f=if(uc.contains(".m3u8",true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory());setMediaSource(f.createMediaSource(MediaItem.fromUri(Uri.parse(uc))));if(ep>0L)seekTo(ep);prepare();playWhenReady=true} // 装弹起播开闸
	}
	LaunchedEffect(P){ // 物理播放进度时间轴脉冲对齐循环计数器
		while(true){if(P?.isPlaying==true)ep=P.currentPosition;kotlinx.coroutines.delay(1000)} // 当处于活动播放内核状态时，每秒轮询同步当前最新的ep指标
	}
	DisposableEffect(P){ // 底层硬件通道及显存刚性回收器
		onDispose{ // 组件树注销卸载现场抢救清理函数
			if(P!=null&&O!=null){ // 判空双向确认
				val cp=P.currentPosition // 精准捕获硬件核心临终前瞬时的时间轴进度位
				val t=O!!["type"];val tl=O!!["title"];val cv=O!!["cover"] // 解包历史记录要素
				kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch{val h=Fyan.co("ayf_history","");val s=listOf("$id $t $tl $cv $ec $cp")+h.lines().filter{it.isNotEmpty()&&!it.startsWith("$id ")};Fyan.cs("ayf_history",s.joinToString("\n"))} // 临终异步数据落盘
			}
			P?.release() // 彻底灭活并交还物理设备显存及解码核心通道
		}
	}
	@Suppress("UNCHECKED_CAST") val sl:List<Map<String,String>> = if(O!=null)O!!["s"] as List<Map<String,String>> else emptyList() // 安全解压并规避类型擦除的剧集列表
	Box(modifier=Modifier.fillMaxSize()){ // 物理详情主容器顶级底座
		Column(modifier=Modifier.fillMaxSize().background(cc.bg)){ // 视窗总纵向图板
			Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(cc.cg),verticalAlignment=Alignment.CenterVertically){ // 顶部标准标题控制条
				Box(modifier=Modifier.size(26.dp).clip(CircleShape).padding(3.dp).clickable{Fyan.nc.popBackStack()},contentAlignment=Alignment.Center){Image(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier=Modifier.size(20.dp),colorFilter=ColorFilter.tint(cc.c))} // 返回键
				BasicText((O?.get("title") as? String)?:"视频详情",modifier=Modifier.weight(1f).padding(start=3.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=f.h4.copy(color=cc.c)) // 长标题截断式精准排版
			}
			when{ // 核心详情视窗多模态状态分流控制器
				X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){BasicText("◌ 加载视频详情…",style=f.p.copy(color=cc.c.copy(alpha=0.5f)))} // 首屏接口等待加载模态
				O==null->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){BasicText("◑ 加载失败",style=f.p.copy(color=cc.c.copy(alpha=0.5f)));Box(modifier=Modifier.padding(top=8.dp).clip(RoundedCornerShape(4.dp)).background(cc.ag).clickable{X=true}.padding(horizontal=16.dp,vertical=6.dp),contentAlignment=Alignment.Center){BasicText("重试",style=f.p.copy(color=cc.primary))}}} // 容错重试交互入口
				Fyan.tv->Row(modifier=Modifier.fillMaxSize()){ // 电视大屏TV横向分栏布局视窗
					Column(modifier=Modifier.fillMaxHeight().weight(3f)){ // 左侧重量级影视物理播放控制区
						Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){ // 标准宽屏底框
							if(uc.isEmpty())BasicText("◉ 加载中...",style=f.ps.copy(color=cc.c.copy(alpha=0.5f))) // 分流等待物理串流解析
							else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){AsyncImage(model=O!!["cover"].toString()+"?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg",contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize());Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(cc.m),contentAlignment=Alignment.Center){BasicText("▶",style=f.h2.copy(color=Color.White))}} // 影视静态大封面起播热区
							else Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){AndroidView(factory={ctx->PlayerView(ctx).apply{useController=true}},update={it.player=if(fs)null else P;if(!fs)it.requestFocus()},modifier=Modifier.fillMaxSize())} // 全屏时动态切断小窗解码链路并精准投射遥控器物理焦点
						}
						if(sl.isNotEmpty())LazyRow(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(10.dp,8.dp)){items(sl.indices.toList()){k->Box(modifier=Modifier.width(50.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(k==ec)cc.primary else cc.x).clickable{if(ec!=k)ec=k},contentAlignment=Alignment.Center){BasicText(sl[k]["title"]!!,style=f.ps.copy(color=if(k==ec)Color.White else cc.c,fontWeight=if(k==ec)FontWeight.W600 else FontWeight.W400))}}} // TV 遥控器对齐集数导航横轴轨道
					}
					Column(modifier=Modifier.fillMaxHeight().padding(16.dp,12.dp).weight(1f).verticalScroll(rememberScrollState())){BasicText("视频简介",style=f.ps.copy(color=cc.c.copy(alpha=0.5f)));BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=6.dp),style=f.p.copy(color=cc.c))} // 右侧长简介展示
				}
				else->Column(modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ // 移动手机端竖屏布局视窗
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){ // 顶部物理容器
						if(uc.isEmpty())BasicText("◌ 加载中...",style=f.ps.copy(color=cc.c.copy(alpha=0.5f))) // 分流等待串流注入
						else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){AsyncImage(model=O!!["cover"],contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize());Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(cc.m),contentAlignment=Alignment.Center){BasicText("▶",style=f.h2.copy(color=Color.White))}} // 手机端点播触发标志热区
						else Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){AndroidView(factory={ctx->PlayerView(ctx).apply{useController=false}},update={it.player=if(fs)null else P},modifier=Modifier.fillMaxSize())} // 预览态基础投射展板
					}
					Column(modifier=Modifier.padding(14.dp,12.dp)){BasicText("视频简介",style=f.ps.copy(color=cc.c.copy(alpha=0.5f)));BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=4.dp),style=f.p.copy(color=cc.c))} // 图文排版文本
					Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal=14.dp).background(cc.bd)) // 基础线性分割线
					if(sl.isNotEmpty())Column(modifier=Modifier.padding(12.dp,8.dp)){ // 手机端紧凑矩阵自适应换行选集大本营
						BasicText("选集",style=f.ps.copy(color=cc.c.copy(alpha=0.5f))) // 选集字头说明
						Column(modifier=Modifier.padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){repeat((sl.size+4)/5){r->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){repeat(5){cIdx->val i=r*5+cIdx;if(i<sl.size)Box(modifier=Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(i==ec)cc.primary else cc.x).clickable{if(ec!=i)ec=i},contentAlignment=Alignment.Center){BasicText(sl[i]["title"]!!,style=f.ps.copy(color=if(i==ec)Color.White else cc.c,fontWeight=if(i==ec)FontWeight.W600 else FontWeight.W400))}else Box(modifier=Modifier.weight(1f))}}}} // 自适应闭包数字矩阵
					}
					Box(modifier=Modifier.height(24.dp)) // 最底边防内容粘连
				}
			}
		}
		if(fs){ // 全向全屏原生覆盖层：手机端配合 Saveable 指标，100% 豁免旋转 Activity 销毁重建引发的退出漏洞
			Box(modifier=Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){ // 全尺寸黑底巨幕底座
				if(!Fyan.tv){DisposableEffect(Unit){act?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;onDispose{act?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}}} // 手机全屏幕激发重力感应全横屏切换，退出全屏时自动恢复至系统标准竖屏状态
				AndroidView(factory={ctx->PlayerView(ctx).apply{useController=true;setOnClickListener{if(isControllerFullyVisible)hideController() else showController()}}},update={it.player=P;it.requestFocus()},modifier=Modifier.fillMaxSize()) // 投射完整功能系统控制台
				if(!Fyan.tv){Box(modifier=Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp).clip(CircleShape).background(cc.m).clickable{fs=false},contentAlignment=Alignment.Center){BasicText("✕",style=f.p.copy(color=Color.White))}} // 手机端提供左上角物理退出键
				BackHandler{fs=false} // 拦截返回键返回普通小窗图文详情页
			}
		}
	}
}
