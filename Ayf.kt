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


@Composable fun AyfHome(){ // 爱壹帆主页容器
	val s=listOf("history" to "历史记录","movie" to "电影","drama" to "剧集","anime" to "动漫","variety" to "综艺","documentary" to "纪录片") // 标签分类映射表
	val t by Fyan.cg("ayf_tab","history").collectAsState(initial="history") // 响应式读取当前激活标签
	val sc=rememberCoroutineScope() // 协程生命周期作用域
	val c=Fyan.cc // 统一缓存主题单例
	val f=Fyan.ff // 统一缓存字体单例
	Fyan.log("路由","进入爱壹帆首页") // 记录页面路由日志
	Column(modifier=Modifier.fillMaxSize().background(c.bg)){ // 主视窗纵向布局
		Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(c.cg).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){ // 横向滚动分类导航栏
			s.forEach{o->Box(modifier=Modifier.fillMaxHeight().background(if(o.first==t)c.primary.copy(alpha=0.15f)else Color.Transparent).clickable{ // 循环渲染导航标签项
				Fyan.log("爱壹帆","点击TAB: "+o.first,'u') // 打印用户交互日志
				sc.launch{Fyan.cs("ayf_tab",o.first)} // 异步持久化当前标签切换状态
			},contentAlignment=Alignment.Center){
				BasicText("  ${o.second}  ",style=f.h4.copy(color=if(o.first==t)c.primary else c.c.copy(alpha=0.7f),fontWeight=if(o.first==t)FontWeight.Bold else FontWeight.Normal)) // 标签文本样式自适应渲染
			}}
		}
		Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(c.bd)) // 导航栏底部细分割线
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){ // 内容动态填充区
			key(t){when(t){"history"->AyfHistory();else->AyfList(id=t)}} // 借助唯一键机制规避分类状态交叉污染
		}
	}
}


@Composable fun AyfHistory(){ // 观看历史记录页
	val c=Fyan.cc // 缓存全局颜色方案
	val f=Fyan.ff // 缓存全局字体规格
	val cn=if(Fyan.sw>=840)5 else if(Fyan.sw>=600)4 else 3 // 依据屏幕宽度弹性计算网格列数
	var id by remember{mutableStateOf<String?>(null)} // 暂存长按待删除的条目媒体凭证
	var hc by remember{mutableStateOf(false)} // 清空全部记录的弹窗交互开关
	val sc=rememberCoroutineScope() // 获取异步任务协程句柄
	val rs by Fyan.cg("ayf_history","").collectAsState(initial="") // 响应式监听本地历史原始数据流
	val vs=remember(rs){ // 缓存并增量解析历史格式行
		if(rs.isBlank())emptyList()else rs.lines().filter{it.isNotBlank()}.mapNotNull{
			val x=it.trim().split(Regex("\\s+"),6) // 空间正则切割字段
			if(x.size<6)null else mapOf("id" to x[0],"type" to x[1],"title" to x[2],"cover" to x[3],"ec" to x[4],"ps" to x[5]) // 字段字典化映射
		}
	}
	Fyan.log("路由","进入爱壹帆历史记录页") // 输出模块追踪日志
	Column(modifier=Modifier.fillMaxSize().background(c.bg)){ // 历史视窗总容器
		Row(modifier=Modifier.fillMaxWidth().height(30.dp).padding(start=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){ // 顶部标题工具操作行
			BasicText("记录清单",style=f.pb.copy(color=c.c)) // 模块标题文本渲染
			Box(modifier=Modifier.size(28.dp).clickable{hc=true},contentAlignment=Alignment.Center){BasicText("🗑",style=f.h4.copy(color=c.c))} // 全局清除图标触控区
		}
		if(vs.isEmpty())Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ // 空状态缺省提示
			BasicText("暂无观看记录",style=f.p.copy(color=c.c.copy(alpha=0.4f))) // 缺省态文本输出
		}else LazyVerticalGrid(modifier=Modifier.fillMaxWidth().weight(1f),columns=GridCells.Fixed(cn),contentPadding=PaddingValues(4.dp),verticalArrangement=Arrangement.spacedBy(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){ // 历史卡片网格布局
			items(vs,{it["id"]!!}){o-> // 绑定唯一身份标识凭证迭代渲染
				Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(c.cg).pointerInput(o["id"]!!){ // 单独海报卡片卡槽
					detectTapGestures(onTap={Fyan.goto("ayf_info/"+o["id"])},onLongPress={id=o["id"]}) // 复合绑定单击续播与长按唤起删除菜单
				},horizontalAlignment=Alignment.CenterHorizontally){
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(c.ag),contentAlignment=Alignment.BottomCenter){ // 封面比例裁剪容器
						AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop) // 异步高能加载影视海报
						if(o["ec"]!="0")Box(modifier=Modifier.padding(4.dp).background(c.m,RoundedCornerShape(1.dp)).padding(horizontal=4.dp,vertical=2.dp)){ // 角标渲染判空条件
							BasicText("第${o["ec"]}集",style=f.ps.copy(color=Color.White)) // 上次足迹定位角标
						}
					}
					BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=f.ps.copy(color=c.c,textAlign=TextAlign.Center)) // 影视标题截断展示
				}
			}
		}
	}
	if(hc)Dialog(onDismissRequest={hc=false}){ // 清空全量记录二次拦截弹窗
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(c.cg).border(1.dp,c.bd,RoundedCornerShape(3.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){ // 弹窗主体材质
			BasicText("清空历史记录？",style=f.h3.copy(color=c.c)) // 风险警示标题
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){ // 交互行为操作组
				BasicText("取消",modifier=Modifier.clickable{Fyan.log("爱壹帆","取消清空历史记录",'s');hc=false}.padding(8.dp),style=f.p.copy(color=c.c)) // 取消清空并回滚状态
				BasicText("确定",modifier=Modifier.clickable{Fyan.log("爱壹帆","执行清空历史记录",'w');sc.launch{Fyan.cs("ayf_history","")};hc=false}.padding(8.dp),style=f.p.copy(color=c.primary)) // 确立清空并异步重置本地存储
			}
		}
	}
	if(id!=null)Dialog(onDismissRequest={id=null}){ // 针对单条记录销毁的二次拦截弹窗
		Column(modifier=Modifier.fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(3.dp)).background(c.cg).border(1.dp,c.bd,RoundedCornerShape(3.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){ // 弹窗布局底板
			BasicText("删除此记录？",style=f.h3.copy(color=c.c)) // 删除警示内容
			Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){ // 弹窗确认执行组
				BasicText("取消",modifier=Modifier.clickable{Fyan.log("爱壹帆","取消删除历史记录，编号: $id",'e');id=null}.padding(8.dp),style=f.p.copy(color=c.c)) // 退出操作并释放锁定的id
				BasicText("确定",modifier=Modifier.clickable{ // 确认清除回调
					val o=rs.lines().filter{it.isNotBlank()&&!it.startsWith("${id!!} ")}.joinToString("\n") // 行匹配流过滤剔除目标数据
					Fyan.log("爱壹帆","执行删除历史记录，编号: $id",'d') // 输出底层调试轨迹
					sc.launch{Fyan.cs("ayf_history",o)};id=null // 异步落盘并解除指针暂存
				}.padding(8.dp),style=f.p.copy(color=c.primary))
			}
		}
	}
}

@Composable fun AyfList(id:String){ // 爱壹帆筛选视频列表
	val c=Fyan.cc // 缓存全局动态色彩架构
	val f=Fyan.ff // 缓存全局结构字体规格
	var vs by remember{mutableStateOf(emptyList<Map<String,String>>())} // 当前渲染页的数据集合
	var fs by remember{mutableStateOf(emptyList<List<Pair<String,String>>>())} // 多维综合筛选矩阵标签池
	val fc=remember{mutableStateMapOf<Int,String>()} // 多维度条件锁定映射字典
	var X by remember{mutableStateOf(true)} // 异步接口首屏全局加载锁
	var pg by remember{mutableIntStateOf(1)} // 内部滑阻追踪页码器
	var hm by remember{mutableStateOf(true)} // 边界判定：服务端可供流式拉取的余量开关
	var lm by remember{mutableStateOf(false)} // 互斥锁：避免并发性触底触点高频冲击重复发起
	val gs=rememberLazyGridState() // 挂载网格专属状态检测器
	val sc=rememberCoroutineScope() // 协程分发调度域
	Fyan.log("路由","进入爱壹帆筛选列表页") // 输出模块进入航向轨迹
	fun au(p:Int=1)="https://api.iyf.tv/api/list/getconditionfilterdata?titleid=$id&ids=${fc.entries.sortedBy{it.key}.joinToString(","){it.value}}&page=$p&size=21" // 单行内嵌拼装网络链接
	suspend fun fv(u:String):List<Map<String,String>> = withContext(Dispatchers.IO){ // 流式加载后台底层核心计算过程
		runCatching<List<Map<String,String>>>{ // 稳健承载解析流
			Fyan.log("爱壹帆","获取筛选视频列表，链接: $u",'d') // 审计远程网络流日志
			val j=JSONObject(Fyan.fetch(u)).optJSONObject("data")?:return@runCatching emptyList() // 安全读取顶层业务级对象
			val s=j.optJSONArray("list")?:return@runCatching emptyList() // 抽取核心多媒体清单明细
			buildList{ // 组装业务标准轻量字典
				for(i in 0 until s.length()){ // 指针轮询对象
					val v=s.getJSONObject(i) // 提取行记录
					add(mapOf("id" to v.optString("mediaKey",""),"type" to v.optString("videoType","1"),"title" to v.optString("title",""),"cover" to v.optString("coverImgUrl",""),"score" to v.optString("score",""),"tip" to v.optString("updateStatus",""))) // 压入规范化映射集合
				}
			}
		}.getOrElse{emptyList()} // 发生异常时全局熔断返回空集
	}
	LaunchedEffect(id){ // 伴随分类大凭证变更触发的多维筛选标签数据构建生命周期
		fs=withContext(Dispatchers.IO){ // 切入IO池进行无阻塞远程检索
			runCatching<List<List<Pair<String,String>>>>{ // 捕获不确定性解析漏洞
				Fyan.log("爱壹帆","获取筛选数据，TAB: $id",'s') // 记录业务线埋点轨迹
				val j=JSONObject(Fyan.fetch("https://api.iyf.tv/api/list/getfiltertagsdata?SecondaryCode=$id")).optJSONObject("data")?:return@runCatching emptyList() // 网络解析
				val s=j.optJSONArray("list")?:return@runCatching emptyList() // 遍历层
				buildList{ // 矩阵嵌套解包
					for(i in 0 until s.length()){ // 轮询大维度
						val z=s.getJSONObject(i).optJSONArray("list")?:continue // 抽取子代集合
						add(buildList{for(r in 0 until z.length()){val o=z.getJSONObject(r);add(o.optString("classifyId","0") to o.optString("classifyName",""))}}) // 二维解压对齐
					}
				}
			}.getOrElse{emptyList()} // 解析灾难备份回滚
		}
		fs.indices.forEach{i->fc[i]="0"} // 默认多维全部初始化对齐归零
		pg=1;hm=true // 重设分页初始锚点
		vs=fv(au(1));X=false // 拉取首屏多媒体对象并解除黑屏加载状态
	}
	LaunchedEffect(gs){ // 滚动视窗高精密边缘测算效应器
		snapshotFlow{gs.layoutInfo.visibleItemsInfo.lastOrNull()?.index}.collect{li-> // 实时响应捕获底部可见范围索引
			if(li!=null&&li>=vs.size-Fyan.gc*2&&hm&&!lm&&!X){ // 精密触发余量水位阈值校验
				lm=true;val px=pg+1 // 并发锁定并递增当前计数器页码
				Fyan.log("爱壹帆","触底加载第${px}页",'d') // 输出底层寻址事件
				val mr=fv(au(px)) // 获取后备网络接续分卷
				if(mr.isEmpty()){hm=false;Fyan.log("爱壹帆","已加载全部数据",'s')}else{vs=vs+mr;pg=px} // 融合或熔断边界控制
				lm=false // 释放加载护栏
			}
		}
	}
	Column(modifier=Modifier.fillMaxSize().background(c.bg)){ // 多维度总视图布局
		if(fs.isNotEmpty())Column(modifier=Modifier.fillMaxWidth().background(c.cg)){ // 多层筛选横向传送带操作群
			fs.forEachIndexed{i,g-> // 排队构建具体过滤大项
				Row(modifier=Modifier.fillMaxWidth().height(20.dp).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){ // 单条过滤器横向无限长卷
					g.forEach{(fi,fn)-> // 铺装当前行内的可点选项卡
						val at=fi==fc[i] // 精准匹配是否处于激活性高亮状态
						Box(modifier=Modifier.fillMaxHeight().background(if(at)c.primary.copy(alpha=0.15f)else Color.Transparent).clickable{ // 点击过滤器交互
							fc[i]=fi;sc.launch{X=true;pg=1;hm=true;vs=fv(au(1));X=false} // 重调映射并强力异步推倒重建数据视图
						},contentAlignment=Alignment.Center){BasicText("  $fn  ",style=f.ps.copy(color=if(at)c.primary else c.c.copy(alpha=0.7f),fontWeight=if(at)FontWeight.W600 else FontWeight.W400))} // 属性字态适应
					}
				}
				Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(c.bd)) // 单行过滤器边际细分割线
			}
		}
		Box(modifier=Modifier.fillMaxWidth().weight(1f)){ // 核心流多媒体网格动态展板
			when{ // 三态状态分支路由判定
				X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){BasicText("◌ 加载中…",style=f.pb.copy(color=c.c.copy(alpha=0.4f)))} // 首屏高光加载态
				vs.isEmpty()->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){BasicText("暂无视频",style=f.p.copy(color=c.c.copy(alpha=0.4f)))} // 接口空缺省展示态
				else->LazyVerticalGrid(state=gs,columns=GridCells.Fixed(Fyan.gc),contentPadding=PaddingValues(4.dp),verticalArrangement=Arrangement.spacedBy(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){ // 网格流主渲染引擎
					items(vs,{it["id"]!!}){o-> // 迭代实体单元映射
						Column(modifier=Modifier.clip(RoundedCornerShape(2.dp)).background(c.cg).clickable{ // 单个影片矩形单元卡
							Fyan.log("爱壹帆","进入详情页，编号: "+o["id"]);Fyan.goto("ayf_info/"+o["id"]) // 点击记录轨迹并全速下发路由跃迁
						},horizontalAlignment=Alignment.CenterHorizontally){
							Box(modifier=Modifier.fillMaxWidth().aspectRatio(0.7f).background(c.ag)){ // 固定缩略图物理高宽占比
								AsyncImage(model=o["cover"],contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop) // 弹性网络多媒体海报加载
								if(!o["score"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.TopStart).padding(2.dp,6.dp).background(c.m,RoundedCornerShape(10.dp)).padding(2.dp)){BasicText(o["score"]!!,style=f.ps.copy(color=c.c))} // 左上角影视评分浮层
								if(!o["tip"].isNullOrEmpty())Box(modifier=Modifier.align(Alignment.BottomCenter).padding(2.dp,6.dp).background(c.m,RoundedCornerShape(6.dp)).padding(2.dp)){BasicText(o["tip"]!!,style=f.ps.copy(color=c.c))} // 底部连载进度浮层
							}
							BasicText(o["title"]!!,modifier=Modifier.padding(4.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=f.ps.copy(color=c.c,textAlign=TextAlign.Center)) // 视频中文译名展示
						}
					}
					if(lm)item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}){Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){BasicText("◌ 加载更多…",style=f.ps.copy(color=c.c.copy(alpha=0.4f)))}} // 加载过渡组件
					if(!hm&&vs.isNotEmpty())item(span={androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)}){Box(modifier=Modifier.fillMaxWidth().padding(8.dp),contentAlignment=Alignment.Center){BasicText("꧁ 已加载全部 ꧂",style=f.ps.copy(color=c.c.copy(alpha=0.3f)))}} // 终点终结符
				}
			}
		}
	}
}

@Composable fun AyfInfo(id:String){ // 爱壹帆高级无缝续播详情组件
	val act=run{ // 鲁棒循环递归上下文穿透链：完美跨越任意包装器精准获取底层真实Activity对象实例
		var ctx=LocalContext.current
		while(ctx is android.content.ContextWrapper){if(ctx is Activity)return@run ctx;ctx=ctx.baseContext};ctx as? Activity
	}
	var O by remember{mutableStateOf<Map<String,Any>?>(null)} // 核心详情结构数据词典
	var X by remember{mutableStateOf(true)} // 详情加载中遮罩骨架屏标记
	var ec by rememberSaveable{mutableIntStateOf(-1)} // 选集定位索引：状态持久存活防销毁
	var ep by rememberSaveable{mutableLongStateOf(0L)} // 毫秒进度时间戳锚点：状态持久存活防销毁
	var uc by rememberSaveable{mutableStateOf("")} // 分发物理串流地址：状态持久存活防销毁
	var pr by rememberSaveable{mutableStateOf(false)} // 用户点按确认点播标志：状态持久存活防销毁
	var fs by rememberSaveable{mutableStateOf(false)} // 顶级全屏控制状态：状态持久存活防销毁
	val c=Fyan.cc // 顶层注入单例化色彩主干快照
	val f=Fyan.ff // 顶层注入单例化文字规格快照
	val sc=rememberCoroutineScope() // 本地轻量协程工厂
	Fyan.log("路由","进入爱壹帆视频详情页") // 输出寻址路径事件日志
	LaunchedEffect(id){ // 页面入口现场数据拉取生命周期
		X=true;pr=false;uc="" // 重初始化重置播放状态
		O=withContext(Dispatchers.IO){ // 切入后台常驻IO密集型线程池
			runCatching<Map<String,Any>?>{ // 阻断数据异常引起的崩溃流
				val j=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/videodetails?mediaKey=$id")).optJSONObject("data")?.optJSONObject("detailInfo")?:return@runCatching null // 拉取网络流
				val x=j.optJSONArray("episodes") // 抽离选集行列表
				val s=mutableListOf<Map<String,String>>() // 映射标准集数队列
				if(x!=null)for(i in 0 until x.length()){val v=x.getJSONObject(i);s.add(mapOf("id" to v.optString("episodeId",""),"title" to v.optString("episodeTitle","${i+1}")))} // 解析各分集元数据
				mapOf("id" to id,"type" to j.optString("videoType","1"),"title" to j.optString("title","未知"),"brief" to j.optString("introduce","空空如也"),"cover" to j.optString("coverImgUrl",""),"s" to s) // 返回全量归档包
			}.getOrElse{null}
		}
		X=false // 释放详情框架加载锁定
		if(O!=null){ // 判定是否存在就绪的历史同步现场
			val h=Fyan.co("ayf_history","") // 同步拦截原始记录全文本
			val x=h.lines().firstOrNull{it.startsWith("$id ")} // 扫描当前影片历史遗迹
			if(x.isNullOrBlank()){ec=0;ep=0L;Fyan.cs("ayf_history",(listOf("$id ${O!!["type"]} ${O!!["title"]} ${O!!["cover"]} 0 0")+h.lines().filter{it.isNotEmpty()}).joinToString("\n"))} // 无记录全新写入
			else{val s=x.trim().split(Regex("\\s+"),6);ec=s.getOrElse(4){"0"}.toIntOrNull()?:0;ep=s.getOrElse(5){"0"}.toLongOrNull()?:0L} // 原地满血还原选集与进度
		}
	}
	LaunchedEffect(ec){ // 极速动态换源解算周期
		if(O==null||ec==-1)return@LaunchedEffect // 业务置空护栏
		pr=false;uc="" // 清理并关闭上一次残留的渲染现场
		val h=Fyan.co("ayf_history","") // 调取本地最新序列化文本
		val x=h.lines().firstOrNull{it.startsWith("$id ")} // 定向锁定行
		if(x.isNullOrBlank())ep=0L else{val s=x.trim().split(Regex("\\s+"),6);if(s.getOrElse(4){"0"}.toIntOrNull()?:0==ec)ep=s.getOrElse(5){"0"}.toLongOrNull()?:0L else ep=0L} // 校验切集寻道赋值
		@Suppress("UNCHECKED_CAST") val ei=(O!!["s"] as List<Map<String,String>>).getOrNull(ec)?.get("id")?:"" // 获取具体解析指纹ID
		val url=withContext(Dispatchers.IO){ // 切入IO线程拉取网络视频真实流媒体路径
			runCatching<String>{ // 隔离捕获
				val s=JSONObject(Fyan.fetch("https://api.iyf.tv/api/video/getplaydata?mediaKey=$id&videoId=$ei&videoType=${O!!["type"]}")).optJSONObject("data")?.optJSONArray("list")?:return@runCatching "" // 解算地址池
				var u="" // 初创空缓冲区
				for(i in 0 until s.length()){val v=s.getJSONObject(i).optString("mediaUrl","");if(v.isNotEmpty()){u=v;break}};u // 命中并返回首条高可用物理串流
			}.getOrElse{""}
		}
		uc=url // 触发物理源绑定建立
		Fyan.cs("ayf_history",(listOf("$id ${O!!["type"]} ${O!!["title"]} ${O!!["cover"]} ${ec} ${ep}")+h.lines().filter{it.isNotEmpty()&&!it.startsWith("${id} ")}).joinToString("\n")) // 全新覆盖并重刷记录置顶
	}
	val P:ExoPlayer?=remember(uc){if(uc.isNotEmpty())ExoPlayer.Builder(Fyan.me).build() else null} // 创生底层解码播放引擎实例
	LaunchedEffect(P,uc){ // 物理内核初始化与极速寻道调度连
		P?.apply{val f=if(uc.contains(".m3u8",true))HlsMediaSource.Factory(DefaultHttpDataSource.Factory())else ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory());setMediaSource(f.createMediaSource(MediaItem.fromUri(Uri.parse(uc))));if(ep>0L)seekTo(ep);prepare();playWhenReady=true} // 装弹起播
	}
	LaunchedEffect(P){ // 高频时间轴脉冲对齐计数器
		while(true){if(P?.isPlaying==true)ep=P.currentPosition;kotlinx.coroutines.delay(1000)} // 当且仅当处于播放内核态时轮询对齐当前ep状态
	}
	DisposableEffect(P){ // 解码引擎资源刚性回收器
		onDispose{ // 释放渲染树生命周期毁伤现场
			if(P!=null&&O!=null){ // 判空确认
				val cp=P.currentPosition // 精准锁定当前解码器时间戳
				val t=O!!["type"];val tl=O!!["title"];val cv=O!!["cover"] // 解包持久化要素
				kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch{val h=Fyan.co("ayf_history","");val s=listOf("$id $t $tl $cv $ec $cp")+h.lines().filter{it.isNotEmpty()&&!it.startsWith("$id ")};Fyan.cs("ayf_history",s.joinToString("\n"))} // 临终落盘
			}
			P?.release() // 彻底销毁
		}
	}
	@Suppress("UNCHECKED_CAST") val sl:List<Map<String,String>> = if(O!=null)O!!["s"] as List<Map<String,String>> else emptyList() // 安全转换当前剧集列表
	Box(modifier=Modifier.fillMaxSize()){ // 详情顶级全平面容器
		Column(modifier=Modifier.fillMaxSize().background(c.bg)){ // 详情纵向骨架主体
			Row(modifier=Modifier.fillMaxWidth().height(38.dp).background(c.cg),verticalAlignment=Alignment.CenterVertically){ // 顶部工具行
				Box(modifier=Modifier.size(26.dp).clip(CircleShape).padding(3.dp).clickable{Fyan.nc.popBackStack()},contentAlignment=Alignment.Center){Image(painter=painterResource(R.drawable.arrow_back),contentDescription=null,modifier=Modifier.size(20.dp),colorFilter=ColorFilter.tint(c.c))} // 返回键
				BasicText((O?.get("title") as? String)?:"视频详情",modifier=Modifier.weight(1f).padding(start=3.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=f.h4.copy(color=c.c)) // 精准渲染多媒体长名称
			}
			when{ // 状态分流器
				X->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){BasicText("◌ 加载视频详情…",style=f.p.copy(color=c.c.copy(alpha=0.5f)))} // 加载态
				O==null->Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){BasicText("◑ 加载失败",style=f.p.copy(color=c.c.copy(alpha=0.5f)));Box(modifier=Modifier.padding(top=8.dp).clip(RoundedCornerShape(4.dp)).background(c.ag).clickable{X=true}.padding(horizontal=16.dp,vertical=6.dp),contentAlignment=Alignment.Center){BasicText("重试",style=f.p.copy(color=c.primary))}}} // 容错重试交互入口
				Fyan.tv->Row(modifier=Modifier.fillMaxSize()){ // TV 横向布局
					Column(modifier=Modifier.fillMaxHeight().weight(3f)){ // 左侧大重量视窗列
						Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){ // 比例基座
							if(uc.isEmpty())BasicText("◉ 加载中...",style=f.ps.copy(color=c.c.copy(alpha=0.5f))) // 分流未达加载态
							else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){AsyncImage(model=O!!["cover"].toString()+"?width=500&height=283&scale=both&mode=crop&anchor=topcenter&format=jpg",contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize());Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(c.m),contentAlignment=Alignment.Center){BasicText("▶",style=f.h2.copy(color=Color.White))}} // 静态展面
							else Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){AndroidView(factory={ctx->PlayerView(ctx).apply{useController=true}},update={it.player=if(fs)null else P;if(!fs)it.requestFocus()},modifier=Modifier.fillMaxSize())} // 全屏时动态断开连接以保证独占渲染
						}
						if(sl.isNotEmpty())LazyRow(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(10.dp,8.dp)){items(sl.indices.toList()){k->Box(modifier=Modifier.width(50.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(k==ec)c.primary else c.x).clickable{if(ec!=k)ec=k},contentAlignment=Alignment.Center){BasicText(sl[k]["title"]!!,style=f.ps.copy(color=if(k==ec)Color.White else c.c,fontWeight=if(k==ec)FontWeight.W600 else FontWeight.W400))}}} // TV 遥控器对齐轨道
					}
					Column(modifier=Modifier.fillMaxHeight().padding(16.dp,12.dp).weight(1f).verticalScroll(rememberScrollState())){BasicText("视频简介",style=f.ps.copy(color=c.c.copy(alpha=0.5f)));BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=6.dp),style=f.p.copy(color=c.c))} // 右边栏图文梗概
				}
				else->Column(modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ // 手机端布局
					Box(modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){ // 顶置卡框
						if(uc.isEmpty())BasicText("◌ 加载中...",style=f.ps.copy(color=c.c.copy(alpha=0.5f))) // 源流加载等待
						else if(!pr)Box(modifier=Modifier.fillMaxSize().clickable{pr=true},contentAlignment=Alignment.Center){AsyncImage(model=O!!["cover"],contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize());Box(modifier=Modifier.size(56.dp).clip(CircleShape).background(c.m),contentAlignment=Alignment.Center){BasicText("▶",style=f.h2.copy(color=Color.White))}} // 播放按钮
						else Box(modifier=Modifier.fillMaxSize().clickable{fs=true},contentAlignment=Alignment.Center){AndroidView(factory={ctx->PlayerView(ctx).apply{useController=false}},update={it.player=if(fs)null else P},modifier=Modifier.fillMaxSize())} // 预览投射展板
					}
					Column(modifier=Modifier.padding(14.dp,12.dp)){BasicText("视频简介",style=f.ps.copy(color=c.c.copy(alpha=0.5f)));BasicText(O!!["brief"] as String,modifier=Modifier.padding(top=4.dp),style=f.p.copy(color=c.c))} // 手机端文本说明
					Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal=14.dp).background(c.bd)) // 分割杠线
					if(sl.isNotEmpty())Column(modifier=Modifier.padding(12.dp,8.dp)){ // 网格换行选集大本营
						BasicText("选集",style=f.ps.copy(color=c.c.copy(alpha=0.5f))) // 选集字头
						Column(modifier=Modifier.padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){repeat((sl.size+4)/5){r->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){repeat(5){cIdx->val i=r*5+cIdx;if(i<sl.size)Box(modifier=Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if(i==ec)c.primary else c.x).clickable{if(ec!=i)ec=i},contentAlignment=Alignment.Center){BasicText(sl[i]["title"]!!,style=f.ps.copy(color=if(i==ec)Color.White else c.c,fontWeight=if(i==ec)FontWeight.W600 else FontWeight.W400))}else Box(modifier=Modifier.weight(1f))}}}} // 五列自适应循环矩阵
					}
					Box(modifier=Modifier.height(24.dp)) // 最底部留白护垫
				}
			}
		}
		if(fs){ // 全屏覆盖层：全屏时平铺屏幕，规避 Dialog 引起的旋转重建退出的缺陷
			Box(modifier=Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){ // 全尺寸巨幕底座
				if(!Fyan.tv){DisposableEffect(Unit){act?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;onDispose{act?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}}} // 手机端全屏激发重力感应横屏，退出恢复竖屏
				AndroidView(factory={ctx->PlayerView(ctx).apply{useController=true;setOnClickListener{if(isControllerFullyVisible)hideController() else showController()}}},update={it.player=P;it.requestFocus()},modifier=Modifier.fillMaxSize()) // 投射完整控制台，遥控器确定键切换显示隐藏
				if(!Fyan.tv){Box(modifier=Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp).clip(CircleShape).background(c.m).clickable{fs=false},contentAlignment=Alignment.Center){BasicText("✕",style=f.p.copy(color=Color.White))}} // 手机全屏返回退出热区
				BackHandler{fs=false} // 拦截返回键返回小窗简介页
			}
		}
	}
}
