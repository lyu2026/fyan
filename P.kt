package com.fyan

import kotlinx.coroutines.launch
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@Composable fun HS(nv:NavController){ // HS (HomeScreen) 应用首页主架组件
	val cc=FN.LC.current // 注入全局色彩配置
	var tb by remember{mutableStateOf(PR.lt)} // 维持首页主导页面分类游标
	Column(modifier="fs".css().background(cc.b)){ // 贯穿全屏的竖向主页列布局
		Row(modifier="fw h56 psb".css().background(cc.s).border(0.5.dp,cc.ov),verticalAlignment=Alignment.CenterVertically){ // 顶部横向滚动主分类频道栏
			Row(modifier="fh e1 sh".css(this)){ // 赋予横向滚动属性的包裹容器
				NAV_TABS.forEach{o-> // 迭代配置预设的各个频道标签
					val ac=o.id==tb // 结算当下选中状态
					Box(modifier="fh ph10".css().background(if(ac)cc.p.copy(alpha=0.15f) else androidx.compose.ui.graphics.Color.Transparent).clickable{tb=o.id;PR.lt=o.id;FN.lg("HomeTab","切换 → ${o.id}",'u')},contentAlignment=Alignment.Center){ // 触发分类重洗刷新的点击响应盒
						BasicText(o.lb,style=FN.TM.copy(color=if(ac)cc.p else cc.os.copy(alpha=0.7f),fontWeight=if(ac)androidx.compose.ui.text.font.FontWeight.W600 else androidx.compose.ui.text.font.FontWeight.W400)) // 各频道的文字显示
					}
				}
			}
		}
		Box(modifier="fw e1".css(this)){ // 核心视图容器内容动态更换区
			key(tb){ // 运用Tab唯一键强制重新计算渲染
				when(tb){ // 判断特定路由分类
					"history"->HI(nv,eb=true) // 调起首层嵌入版足迹记录大屏组件
					else->FS(nv,id=tb,eb=true) // 调起首层嵌入版特定视频列表分类大屏组件
				}
			}
		}
	}
}

@Composable fun HI(nv:NavController,eb:Boolean=false){ // HI (HistoryScreen) 历史浏览记录页面组件
	val cc=FN.LC.current // 取出环境配置色
	var cl by remember{mutableStateOf(false)} // 抹除足迹二次提示面板触发器
	var rk by remember{mutableStateOf<String?>(null)} // 选中准备废除的单体足迹记录主键
	val cf=LocalConfiguration.current // 提取出本机配置
	val cs=when{ // 按物理屏宽自动栅格适配多列
		cf.screenWidthDp>=840->5 // TV超大宽平板开五列
		cf.screenWidthDp>=600->4 // 折叠开屏常规平板开四列
		else->3 // 普通竖持小屏智能手机开三列
	}
	Column(modifier="fs".css().background(cc.b)){ // 足迹页面纵向排布根盒
		if(!eb){ // 若非嵌入模式（即独立路由大页）
			TB(tt="历史记录",ob={nv.popBackStack()},ed={IB(lb="🗑",oc={cl=true},modifier="fw36 fh36 c8".css())}) // 绘制配备标准清空功能把手的独立大顶部Bar
		}else{ // 归属于首页主屏之下的嵌入子页
			Row(modifier="fw h40 ph12".css(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){ // 绘制一个极简轻盈的标题行
				BasicText("历史记录",style=FN.TS.copy(color=cc.os)) // 足迹小标文案
				IB(lb="🗑",oc={cl=true},modifier="fw32 fh32 c8".css()) // 清空按钮
			}
		}
		if(FN.hi.isEmpty()){ // 全局记录为空的缺省判定
			Box(modifier="fs".css(),contentAlignment=Alignment.Center){BasicText("暂无观看记录",style=FN.BM.copy(color=cc.os.copy(alpha=0.4f)))} // 居中输出空视窗信息
		}else{ // 包含有效足迹历史记录
			LazyVerticalGrid(modifier="fw".css(),columns=GridCells.Fixed(cs),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){ // 惰性栅格流式网格
				gridItems(FN.hi,key={it.id}){o-> // 遍历足迹条目绑定唯一主键
					VC(tt=o.tt,pt=o.pt,sb=o.pg,modifier="fw".css(),oc={FN.lg("History","点击 ${o.id}",'u');nv.navigate("detail/${o.id}")},lp={rk=o.id}) // 挂载瀑布视频历史记录大卡片
				}
			}
		}
	}
	if(cl)CD(tt="确认清空所有历史记录？",od={cl=false},oc={cH();cl=false}) // 清除全部足迹大弹层
	rk?.let{ky->CD(tt="确认删除该条历史记录？",od={rk=null},oc={rH(ky);rk=null})} // 划走移除单条历史记录提示窗
}

@Composable fun FS(nv:NavController,id:String,eb:Boolean=false){ // FS (FilterScreen) 视频高级检索筛选大页面组件
	val cc=FN.LC.current // 绑定全局取色
	val tb=NAV_TABS.find{it.id==id} // 精准捕获所关联的分类常数字段
	var gs by remember{mutableStateOf<List<FG>>(emptyList())} // 本检索分类持有的筛选多属性组定义
	var ds by remember{mutableStateOf<List<String>>(emptyList())} // 对应各过滤属性组的具体选中项映射键数组
	var vs by remember{mutableStateOf<List<VI>>(emptyList())} // 流式无限加载追加的多媒体视频数据集合
	var lm by remember{mutableStateOf(false)} // 避免滚屏重入的分页追加防抖状态标记锁
	var hm by remember{mutableStateOf(true)} // 标志远端服务器后台是否仍存在残余待拉取的后置页面
	var pg by remember{mutableIntStateOf(1)} // 本地维护的分页游标深度计数值
	var ld by remember{mutableStateOf(true)} // 首次进场初始化拉取网格白屏骨架锁状态
	val ls=androidx.compose.foundation.lazy.grid.rememberLazyGridState() // 掌控惰性多列网格底层滚动逻辑的独立游标状态

	LaunchedEffect(id){ // 进场触发首次多重属性条件初始化的协程副作用
		ld=true // 封锁大视窗
		val s=fG(id) // 跨线程网络接口捕获当前的分类属性树数据
		gs=s // 存储数据
		ds=s.map{"0"} // 预备初始化填充零代码作为默认全选条件
		vs=fL(id,ds.joinToString(","),1) // 首刷调用拉取第1页的缩略信息
		pg=1 // 还原页码归属为1
		hm=vs.size>=21 // 首刷多于或等于标准21条代表极可能包含后续追加的分页
		ld=false // 解除首刷白屏大锁
		FN.lg("FilterScreen","tab=$id list=${s.size}",'i') // 记录操作路径事件
	}

	suspend fun rl(){ // 条件发生任何变更洗牌时调用的底层网格强制洗牌重洗逻辑
		ld=true // 临时封锁网格
		val nq=ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")} // 实时汇编提取选中的多段拼接检索键字符串
		vs=fL(id,nq,1) // 网络请求覆盖第1页洗牌结果
		pg=1 // 页游标归首
		hm=vs.size>=21 // 更新分页后续状态
		ld=false // 打开封面
	}

	val nr by remember{derivedStateOf{val lt=ls.layoutInfo;val lx=lt.visibleItemsInfo.lastOrNull()?.index?:-1;lx>=lt.totalItemsCount-3&&lt.totalItemsCount>0}} // 派生触底状态：最后可见项接近末尾则为true
	LaunchedEffect(nr){ // 监听派生触底状态变化触发增量分页副作用
		if(nr&&hm&&!lm&&!ld){ // 确认触底且有更多数据且无重入
			lm=true // 夯死触发防抖锁
			val nq=ds.joinToString(",").ifEmpty{gs.map{"0"}.joinToString(",")} // 获取最新的多参数过滤文本
			val nx=fL(id,nq,pg+1) // 分页网络请求追加获取pg+1深度块数据
			if(nx.isNotEmpty()){vs=vs+nx;pg++} // 若返回新块有效则内存数组追加并前进游标
			else hm=false // 彻底没有新内容了，销毁增量分页入口
			lm=false // 释放在线加载防抖标识锁
		}
	}

	val cf=LocalConfiguration.current // 设备状态抓取
	val cs=when{cf.screenWidthDp>=840->6;cf.screenWidthDp>=600->4;else->3} // 自适应大中小型设备列分摊排布
	Column(modifier="fs".css().background(cc.b)){ // 检索列表页纵向主布局
		if(!eb)TB(tt=tb?.lb?:"",ob={nv.popBackStack()}) // 在独立大页面模式之下绘制专属标准TBBar顶栏
		if(gs.isNotEmpty()){ // 若包含行数不为零的条件组
			Column(modifier="fw".css().background(cc.s).border(0.5.dp,cc.ov)){ // 多属性栏目垂直聚集大盒子
				gs.forEachIndexed{i,g-> // 迭代各条独立大检索属性行
					TR(fl=g.nm,tb=g.op.map{it.id to it.lb},sl=ds.getOrElse(i){"0"},on={ii-> // 挂载单条标签滑轨TR
						ds=ds.toMutableList().also{it[i]=ii} // 高速就地克隆更新对应行的细化属性选中代码
						kotlinx.coroutines.MainScope().launch{rl()} // 在就近主域调起清洗函数更新网络数据网格
					})
					if(i<gs.lastIndex)Box(modifier="fw h0.5".css().background(cc.ov)) // 行与行缝隙加浅灰描边细缝
				}
			}
		}
		Box(modifier="fw e1".css(this)){ // 核心流媒体瀑布网格画布盒子
			when{ // 选择分支显示不同形态中间态组件
				ld->CL("100","加载视频列表…") // 首加载状态展示CL缓冲组件
				vs.isEmpty()->Box(modifier="fs".css(),contentAlignment=Alignment.Center){BasicText("暂无视频",style=FN.BM.copy(color=cc.os.copy(alpha=0.4f)))} // 无结果提示空视图
				else->LazyVerticalGrid(state=ls,modifier="fw".css(),columns=GridCells.Fixed(cs),contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){ // 流式自适应多列视频卡片网格
					gridItems(vs,key={it.id}){o-> // 绑定卡片并指明媒体主键
						VC(pt=o.pt,tt=o.tt,modifier="fw".css(),oc={aH(FN.VT(o.id,o.tt,o.pt));nv.navigate("detail/${o.id}")},sb=listOfNotNull(o.sc.takeIf{it.isNotEmpty()},o.ut.takeIf{it.isNotEmpty()}).joinToString(" · ")) // 单张高保真视频展现卡片
					}
					if(lm)item{CL("加载更多…")} // 正在滑滚增量中在最后行外置一个微型数据解析缓冲圈
				}
			}
		}
	}
}

@Composable fun DS(nv:NavController,id:String){ // DS (DetailScreen) 独立媒体正片详情播放控制主大视窗组件
	val cc=FN.LC.current // 注入上下文色控主题
	val cf=LocalConfiguration.current // 读取本台手机或设备状态数据环境
	val tt=cf.screenWidthDp>=600 // 测度是否属于宽幅宽屏显示视口
	val lp=cf.orientation==Configuration.ORIENTATION_LANDSCAPE // 指示当下是否处理为宽横屏物理摆放姿态
	val tv=(cf.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION // 查看是否为电视安卓盒子大终端
	var o by remember{mutableStateOf<VD?>(null)} // 实例化存放本详情数据的内存响应式载体
	var ld by remember{mutableStateOf(true)} // 详情解析页初进入骨架白屏挂起状态状态开关
	var pg by remember{mutableIntStateOf(0)} // 正片剧集单集切换高亮物理指针

	LaunchedEffect(id){ // 进场拉取正片详情全量包数据的协程副作用
		ld=true // 降下屏幕大锁
		o=fD(id) // 网络模块调用远端大包拉取接口
		ld=false // 释放大屏等待组件
		FN.lg("DetailScreen","id=$id",'i') // 打入界面访问标记日志
		o?.let{aH(FN.VT(id=it.id,tt=it.tt,pt=it.pt,pg="第1集"))} // 如果详情有效则极速添加或覆盖保存一次当前看片足迹起点为第1集
	}

	Column(modifier="fs".css().background(cc.b)){ // 详情主垂直排布根框
		TB(tt=o?.tt?:"视频详情",ob={nv.popBackStack()}) // 组装详情页定制的顶栏
		when{ // 网络拉取逻辑多态判定切换
			ld->CL("200","加载视频详情…") // 骨架锁住则常驻加载状态组件
			o==null->Box(modifier="fs".css(),contentAlignment=Alignment.Center){BasicText("加载失败",style=FN.BM.copy(color=cc.os.copy(alpha=0.5f)))} // 解析失败输出损坏文案
			else->{ // 提取解包非空正片详情数据流
				val d=o!! // 解开强制绑定
				if(tv||(tt&&lp)){TV(d,pg){i->pg=i;aH(FN.VT(d.id,d.tt,d.pt,"第${i+1}集"))}} // 在大屏智能终端横幅下调用横向定制版TV大组件
				else{PL(d,pg){i->pg=i;aH(FN.VT(d.id,d.tt,d.pt,"第${i+1}集"))}} // 针对竖屏普通移动手机用户调用专属黄金竖列PL组件
			}
		}
	}
}

@Composable private fun TV(d:VD,ep:Int,oe:(Int)->Unit){ // TV 大视口终端或高横宽屏特定专属自适应双列播放框架组件
	val cc=FN.LC.current // 上下文取配色
	var u by remember(ep){mutableStateOf("")} // 独立声明托管当前具体解析得到的直连切片流媒体物理大链接URL
	LaunchedEffect(ep){ // 集数换集触发的切片直连流重解析副作用
		val ru=d.ep.getOrNull(ep)?:"" // 剥离出当前集数锁死持有的流口令密匙
		if(ru.isNotEmpty()&&!ru.startsWith("http",ignoreCase=true)){ // 鉴别是否属于需要二次转换的短哈希密匙口令
			u=fS(ru) // 网络功能模块解码返回真实的真实大网络公网流直连地址
			FN.lg("fetchVideoSource",u) // 日志记录下放的大切片流媒体直连物理地址
		}else u="" // 清空脏态
	}
	Row(modifier="fs".css()){ // 横向平分全屏的左右两段左右大双列布局
		Column(modifier="fh e2".css(this)){ // 左方主位：霸占两倍超大宽幅权重的核心放映大厅Column
			Box(modifier="fw".css().aspectRatio(16f/9f).background(androidx.compose.ui.graphics.Color.Black),contentAlignment=Alignment.Center){ // 16:9纯净黑底模拟放映机机位底盒
				if(u.isNotEmpty()){VP(pt=d.pt,sc=u)}else{BasicText("加载中...",style=FN.TS.copy(color=cc.os))} // 若直连链接非空则降落VP模拟播放器卡位画幅，否则原地挂起小字提示符
			}
			LazyRow(modifier="fw".css(),horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)){ // 下属横行自由水平滑滚的分选按钮单行导轨
				items(d.et.indices.toList()){i->EB(lb=d.et[i],ac=i==ep,modifier="fw60 fh32".css(),oc={oe(i)})} // 循环输出分集圆角选择切片钮
			}
		}
		Column(modifier="fh ph16 pv12 e1 sv".css(this)){ // 右方副位：分配单倍宽幅权重支持纵向自由翻滚的视频详细介绍长文本纸盒
			BasicText("简介",style=FN.TS.copy(color=cc.os.copy(alpha=0.5f))) // 分类灰度标题
			Spacer(modifier="h6".css()) // 纵向间衬
			BasicText(d.ds,style=FN.BM.copy(color=cc.os)) // 视频长幅详情介绍核心纯内容大文案
		}
	}
}

@Composable private fun PL(d:VD,ep:Int,oe:(Int)->Unit){ // PL 智能移动普通小手机垂直窄视口常规排版流组件
	val cc=FN.LC.current // 主题全局配色
	Column(modifier="fs sv".css()){ // 贯穿全高支持自如顺滑向下纵滚的页面大长垂直基座Column
		Box(modifier="fw".css().aspectRatio(16f/9f).background(androidx.compose.ui.graphics.Color.Black),contentAlignment=Alignment.Center){VP(pt=d.pt,sc=d.ep.getOrNull(ep)?:"")} // 贴紧两侧物理屏幕边缘的标准视界16比9大观影面视窗
		Column(modifier="fw ph14 pv12".css()){ // 介绍文案内容外框
			BasicText("简介",style=FN.TS.copy(color=cc.os.copy(alpha=0.5f))) // 提示分类暗字
			Spacer(modifier="h4".css()) // 缝隙留白
			BasicText(d.ds,style=FN.BM.copy(color=cc.os)) // 正文文本铺设
		}
		Box(modifier="fw h0.5 mh14".css().background(cc.ov)) // 细白横截割断线
		if(d.et.isNotEmpty()){ // 若解析剧集目录数组包含有意义内容
			Column(modifier="fw ph12 pv8".css()){ // 选正片集控制面板区
				BasicText("选集",style=FN.TS.copy(color=cc.os.copy(alpha=0.5f))) // 模块文本
				Spacer(modifier="h8".css()) // 边缝空白
				EG(tl=d.et,ct=ep,cs=5,os=oe) // 唤醒多行铺满非滑移的平面矩阵式集数网格大组件
			}
		}
		Spacer(modifier="h24".css()) // 垫底安全边界补齐留白盒
	}
}

@Composable private fun EG(tl:List<String>,ct:Int,cs:Int,os:(Int)->Unit){ // EG 专为防止纵向滚动冲突而使用Column+Row平铺排布的扁平剧集平面网格矩阵组件
	val rs=(tl.size+cs-1)/cs // 实行严格向上取整测算出所需排列出的总物理横行层数
	Column(verticalArrangement=Arrangement.spacedBy(6.dp)){ // 各行按等距6dp堆叠大列
		repeat(rs){r-> // 根据测算总行数逐层展开迭代
			Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ // 每行内部的按钮按水平横向间隙间隔排开
				repeat(cs){c-> // 顺应列数因子单行内部循环平铺
					val i=r*cs+c // 换算映射还原出一维长数组内真实的数据元素指针
					if(i<tl.size){EB(lb=tl[i],ac=i==ct,oc={os(i)},modifier="fw e1 fh34".css(this))} // 绘制单枚权重对半分发的块集数选区小组件
					else Spacer(modifier="e1".css(this)) // 一行内尾部残缺格子补齐同等空置视口物理权重，避免前置按钮因空位而产生大范围拉伸变形
				}
			}
		}
	}
}

@Composable private fun VP(pt:String,sc:String){ // VP 播放核心画幅前端高真界面模拟卡位占位组件
	Box(modifier="fs".css(),contentAlignment=Alignment.Center){ // 多层压栈覆盖盒子根底
		AsyncImage(model=pt,contentDescription="封面",contentScale=ContentScale.Fit,modifier="fs".css()) // 最底层完全平铺且保持比例不坏的视频大海报原图画布
		Box(modifier="fw56 fh56 c".css().background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.5f)),contentAlignment=Alignment.Center){BasicText("▶",style=FN.TL.copy(color=androidx.compose.ui.graphics.Color.White))} // 最上层半透明圆形遮罩点睛式居中观影操作指示方向标小箭头
	}
}
