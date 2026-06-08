package com.fyan

import android.content.res.Configuration
import android.graphics.Color as AC
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.UUID
import kotlin.math.roundToInt

object FN{ // 全局控制单例
	data class tc(val p:Color,val b:Color,val s:Color,val sv:Color,val o:Color,val os:Color,val ov:Color) // 主题色彩配置类
	fun cl(dk:Boolean=true)=if(dk) tc(p=Color(0xFF8AB4F8),b=Color(0xFF1C1C1E),s=Color(0xFF2C2C2E),sv=Color(0xFF3A3A3C),o=Color(0xFF938F99),os=Color(0xFFE6E1E5),ov=Color(0xFF49454F)) else tc(p=Color(0xFF1A73E8),b=Color(0xFFF5F5F5),s=Color(0xFFFFFFFF),sv=Color(0xFFE8EAF6),o=Color(0xFFCAC4D0),os=Color(0xFF1C1B1F),ov=Color(0xFFE7E0EC)) // 动静明暗色彩分发逻辑
	val LC=staticCompositionLocalOf{cl()} // 全局主题本地提供变量
	val TL get()=TextStyle(fontSize=22.sp,fontWeight=FontWeight.W600,lineHeight=28.sp) // 大号非标题字形
	val TM get()=TextStyle(fontSize=16.sp,fontWeight=FontWeight.W500,lineHeight=24.sp) // 中号导航字形
	val TS get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W500,lineHeight=20.sp) // 小号强调字形
	val BM get()=TextStyle(fontSize=14.sp,fontWeight=FontWeight.W400,lineHeight=20.sp) // 标准常规字形
	val BS get()=TextStyle(fontSize=12.sp,fontWeight=FontWeight.W400,lineHeight=16.sp) // 极小脚注字形
	val lg=mutableStateListOf<String>() // 控制台全局运行日志队列
	var lf by mutableStateOf(false) // 控制台是否折叠显示状态
	var lh by mutableStateOf(false) // 控制台是否全面隐藏状态
	var ly by mutableStateOf(0f) // 触屏拖拽实时位移量
	fun lg(m:String,o:String,c:Char='i'){ // 打印应用运行事件方法
		val t=java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) // 获取当下格式化时钟
		val x=when(c){'i'->"#2196F3";'u'->"#9C27B0";'e'->"#F44336";'s'->"#00BCD4";'n'->"#4CAF50";'w'->"#FF9800";else->"#9E9E9E"} // 分配日志级别的显色
		lg.add("${UUID.randomUUID().toString().replace("-","")}.$x●$t $m ➜ $o") // 压入带有独立键值的日志序列
	}
	fun lc()=lg.clear() // 彻底擦除当前运行时日志
	fun lr(i:String)=lg.removeAll{it.startsWith(i)} // 定向抹除单条对应的历史日志
	@Composable fun LP(){Column(modifier="fw".css(),verticalArrangement=Arrangement.Bottom){if(lf)LH()else LS()}} // 总控调度日志承载看板组件
	@Composable private fun LH(){Box(modifier="fw h5 pnb c2.5".css().background(Color(0x80808080)).clickable{lf=false;ly=0f},contentAlignment=Alignment.Center){BasicText("· · ·  日志  · · ·",style=BS.copy(color=Color.White.copy(alpha=0.7f)))}} // 简易窄条折叠指示组件
	@Composable private fun LS(){ // 宽幅展开日志详情组件
		val ls=rememberLazyListState() // 日志惰性栏目的状态游标
		LaunchedEffect(lg.lastOrNull()){if(lg.isNotEmpty())ls.animateScrollToItem(lg.size-1)} // 当有新日志产生时平滑触底
		val mh=LocalConfiguration.current.screenHeightDp.dp/3 // 动态划分设备屏幕视口的三分之一高度
		val tv=(LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK)==Configuration.UI_MODE_TYPE_TELEVISION // 检测是否运行于TV端
		Box(modifier="fw br6,6,0,0 b0.5,808080,0.70 h>${mh}".css().offset(y=ly.roundToInt().dp).background(Color(0xEB1C1C1E)).pointerInput(Unit){detectDragGestures(onDragEnd={if(ly>100f){lf=true;ly=0f} else ly=0f},onDrag={ch,d->ch.consume();if(ly+d.y>=0f)ly+=d.y})}){ // 核心手势拖拽外罩盒子，pnb移至LH统一处理
			Column(modifier="fw pv2 ph5 pnb".css()){ // 纵向排布容器，底部导航内边距移至此处
				Box(modifier="fw h16".css(),contentAlignment=Alignment.Center){Box(modifier="fw40 fh3 c2".css().background(Color(0x66808080)).clickable(enabled=tv){lf=true;ly=0f}.pointerInput(!tv){if(!tv)detectTapGestures(onTap={lf=true;ly=0f})})} // 顶部居中的拖曳手柄横条
				Row(modifier="fw ph4 pv2".css(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){ // 工具条横列布局
					BasicText("日志 · ${lg.size}条",style=BS.copy(color=Color(0xFF9E9E9E),fontFamily=FontFamily.Monospace)) // 指示当前条数
					Box(modifier="ph8 pv2 c4".css().background(Color(0x33F44336)).clickable{lc()}){BasicText("清空",style=BS.copy(color=Color(0xFFF44336)))} // 触发擦除日志按钮
				}
				LazyColumn(state=ls,modifier="fw pt4".css()){ // 日志条目流滚动组件
					items(lg){ey-> // 单条日志渲染循环
						val pt=ey.split("●",limit=2) // 拆分属性和文本
						val dt=pt[0].lastIndexOf('.') // 捕获色块切片终点
						val id=if(dt>0)pt[0].substring(0,dt) else pt[0] // 提取出事件原始ID
						val hx=if(dt>0)pt[0].substring(dt+1) else "#9E9E9E" // 拿到十六进制色彩
						val ec=try{Color(AC.parseColor(hx))}catch(_:Exception){Color(0xFF9E9E9E)} // 转换输出原生显色
						Row(modifier="fw pt1".css(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){ // 单条横向显示行
							BasicText(pt.getOrElse(1){""},modifier="e1 pe4".css(this),style=BS.copy(color=ec,lineHeight=1.2.em,fontFamily=FontFamily.Monospace)) // 内容核心块
							Box(modifier="fw24 fh24 c".css().clickable{lr(id)},contentAlignment=Alignment.Center){BasicText("✕",style=BS.copy(color=Color(0xFF9E9E9E)))} // 单一删除把手按钮
						}
						Box(modifier="fw h0.5".css().background(Color(0x1A808080))) // 行底浅色微缝线
					}
				}
			}
		}
	}
	var ht by mutableStateOf<String?>(null) // 记录主导航激活的标识键值
	val hi=mutableStateListOf<VT>() // 历史足迹全量内存观察队列
	data class VT(val id:String,val tt:String,val pt:String,val pg:String="") // 单个媒体播放记录足迹数据结构
}

@Composable
fun TB(tt:String="",ob:(()->Unit)?=null,ed:@Composable RowScope.()->Unit={}){ // TB (TopBar) 标题工具栏组件
	val cc=FN.LC.current // 取出当前环境主题色
	Row(modifier="fw h56 psb".css().background(cc.s).border(0.5.dp,cc.ov),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){ // 顶条水平根布局
		Row(modifier="ph12".css(),verticalAlignment=Alignment.CenterVertically){ // 左段标题组合行
			if(ob!=null){ // 若返回事件不为空
				IB(lb="←",modifier="fw36 fh36 c8".css().background(cc.sv),oc=ob) // 绘制通用返回按钮组件
				if(tt.isNotEmpty())Spacer(modifier="fw12".css()) // 左边距
			}
			if(tt.isNotEmpty())BasicText(tt,style=FN.TL.copy(color=cc.os),maxLines=1,overflow=TextOverflow.Ellipsis,modifier="e1".css(this)) // 主标题限单一单行显示
		}
		Row(verticalAlignment=Alignment.CenterVertically){ed()} // 右段自定义按钮行
	}
}

@Composable
fun IB(lb:String,modifier:Modifier,oc:()->Unit){ // IB (IconBtn) 图标轻量按钮组件
	val cc=FN.LC.current // 注入上下文主题色
	Box(modifier=modifier.clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(lb,style=TextStyle(fontSize=18.sp,color=cc.os))} // 交互包装盒
}

@Composable
fun CL(pt:String="0",tt:String="加载中…"){ // CL (LoadingCenter) 全屏居中数据等待缓冲组件
	val cc=FN.LC.current // 绑定全局色彩
	Box(modifier="fw pt$pt".css(),contentAlignment=Alignment.Center){ // 铺满宽度并居中对齐的外层箱盒子
		Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){ // 图标与文字横向同行排列
			BasicText("◌",style=TextStyle(fontSize=20.sp,color=cc.p)) // 转圈图标占位符
			BasicText(tt,style=FN.BS.copy(color=cc.os.copy(alpha=0.6f))) // 加载状态文字
		}
	}
}

@Composable
fun VC(pt:String,tt:String,sb:String,modifier:Modifier,oc:()->Unit,lp:(()->Unit)?=null){ // VC (VideoCard) 竖型瀑布卡片组件
	val cc=FN.LC.current // 引入主题配色
	Box(modifier=modifier.clip(RoundedCornerShape(10.dp)).background(cc.sv).then(if(lp!=null)Modifier.pointerInput(Unit){detectTapGestures(onTap={oc()},onLongPress={lp()})} else Modifier.clickable(onClick=oc))){ // 响应单击和长按的高级触碰外包装框
		Column{ // 主卡片纵向排版流
			Box(modifier="fw r3x4".css().background(cc.s)){AsyncImage(modifier="fs".css(),model=pt,contentDescription=tt,contentScale=ContentScale.Crop)} // 锁定3比4规格的海报大画布
			Column(modifier="fw ph8 pv6".css()){ // 文字辅助信息容器
				BasicText(tt,style=FN.BS.copy(color=cc.os),maxLines=2,overflow=TextOverflow.Ellipsis) // 双行标题截断文本
				if(sb.isNotEmpty()){ // 若更新提示不为空
					Spacer(modifier="h2".css()) // 顶距微调
					BasicText(sb,style=FN.BS.copy(color=cc.p),maxLines=1) // 副字展示
				}
			}
		}
	}
}

@Composable
fun CD(tt:String,ct:String="确认",at:String="取消",oc:()->Unit,od:()->Unit){ // CD (ConfirmDialog) 强交互二次确认悬浮弹窗组件
	val cc=FN.LC.current // 引入当下主题
	Box(modifier="fs".css().background(Color(0x80000000)),contentAlignment=Alignment.Center){ // 半透明黑色纯底大蒙层
		Column(modifier="fw300 ph20 pv20 c12".css().background(cc.s).border(0.5.dp,cc.ov,RoundedCornerShape(12.dp)),horizontalAlignment=Alignment.CenterHorizontally){ // 圆角容器主体大框
			BasicText(tt,style=FN.BM.copy(color=cc.os,textAlign=TextAlign.Center)) // 提示警示内容
			Spacer(modifier="h16".css()) // 高度间距
			Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){ // 下部横列双向按钮控制行
				Box(modifier="fw ph20 pv10 c8".css().background(cc.sv).clickable(onClick=od),contentAlignment=Alignment.Center){BasicText(at,style=FN.BM.copy(color=cc.os))} // 取消退出按钮
				Box(modifier="fw ph20 pv10 c8".css().background(cc.p.copy(alpha=0.15f)).clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(ct,style=FN.BM.copy(color=cc.p))} // 接受确认按钮
			}
		}
	}
}

@Composable
fun TR(fl:String,tb:List<Pair<String,String>>,sl:String,on:(String)->Unit){ // TR (FilterTabRow) 单行横向可滚动标签属性过滤轴组件
	val cc=FN.LC.current // 上下文取色
	Row(modifier="fw h28".css().background(cc.s),verticalAlignment=Alignment.CenterVertically){ // 外套主横行框架
		Row(modifier="fh sh e1".css(this),verticalAlignment=Alignment.CenterVertically){ // 支持横滚的内联横向列
			tb.forEach{(id,lb)-> // 拆包迭代各子细分项对
				val ac=id==sl // 测算激活标志
				Box(modifier="fh ph2".css().clickable{on(id)}.background(if(ac)cc.p.copy(alpha=0.15f) else Color.Transparent),contentAlignment=Alignment.Center){BasicText(" $lb ",style=FN.BS.copy(color=if(ac)cc.p else cc.os.copy(alpha=0.7f),fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))} // 变色微型单片过滤块
			}
		}
	}
}

@Composable fun EB(lb:String,ac:Boolean,modifier:Modifier,oc:()->Unit){ // EB (EpisodeBtn) 圆角多样式正片分集卡块按钮组件
	val cc=FN.LC.current // 提取主题颜色
	Box(modifier=modifier.clip(RoundedCornerShape(6.dp)).background(if(ac)cc.p else cc.sv).clickable(onClick=oc),contentAlignment=Alignment.Center){BasicText(lb,style=FN.BS.copy(color=if(ac)cc.b else cc.os,fontWeight=if(ac)FontWeight.W600 else FontWeight.W400))} // 分集交互背景圆角按钮盒子
}

class CW(val m:Modifier,val w:Float?=null,val a:Alignment?=null,val f:Boolean=true) // CW DSL修饰符样式编译中间打包实体类

@Composable fun pcss(s:String):CW{ // 高度精简CSS流式多属性解析翻译引擎方法
	var m:Modifier=Modifier;var w:Float?=null;var f=true;var a:Alignment?=null // 状态配置临时初始化
	for(it in s.split(" ")){ // 依照单一空格切片迭代
		val t=it.trim();if(t.isEmpty())continue // 滤过非法空格
		when(t[0]){ // 按照核心标识首字母划分分支
			'f'->when{ // fill填充流大组
				t=="fs"->m=m.fillMaxSize() // 填充全屏视口
				t.startsWith("fw")->{val v=t.drop(2);m=if(v.isEmpty())m.fillMaxWidth() else m.width(v.toDoubleOrNull()?.dp?:0.dp)} // 铺满宽或赋予特定宽
				t.startsWith("fh")->{val v=t.drop(2);m=if(v.isEmpty())m.fillMaxHeight() else m.height(v.toDoubleOrNull()?.dp?:0.dp)} // 铺满高或赋予特定高
			}
			'w'->{val v=t.drop(1) // 宽度特定修饰流
				m=when{ // 深度测算区间边界值
					v.toDoubleOrNull()!=null->m.width(v.toDouble().dp) // 普通固定宽
					v.startsWith(">")->m.widthIn(min=v.drop(1).toDoubleOrNull()?.dp?:0.dp) // 极低安全下限宽
					v.startsWith("<")->m.widthIn(max=v.drop(1).toDoubleOrNull()?.dp?:Dp.Unspecified) // 极端安全上限宽
					v.isEmpty()->m.fillMaxWidth() // 铺满
					else->m
				}
			}
			'h'->{val v=t.drop(1) // 高度特定修饰流
				m=when{ // 深度测算高度区间
					v.isEmpty()->m.fillMaxHeight() // 铺满高
					v.startsWith(">")->m.heightIn(min=v.drop(1).toDoubleOrNull()?.dp?:0.dp) // 极低下限高
					v.startsWith("<")->m.heightIn(max=v.drop(1).toDoubleOrNull()?.dp?:Dp.Unspecified) // 极限上限高
					else->m.height(v.toDoubleOrNull()?.dp?:0.dp) // 常规绝对高
				}
			}
			'p'->{val v=t.drop(1) // 间隔外边衬Padding大组
				when{ // 甄别安全区域和普通数值间衬
					v=="nb"->m=m.navigationBarsPadding() // 系统到底栏底间衬
					v=="sb"->m=m.statusBarsPadding() // 系统顶状态栏顶间衬
					v=="bs"->m=m.systemBarsPadding() // 双侧全屏系统边衬
					v=="im"->m=m.imePadding() // 输入法键盘弹出防遮底边衬
					else->{ // 常规物理纯数值内边距
						val n=v.dropWhile{!it.isDigit()&&it!='.'}.toDoubleOrNull()?.dp?:0.dp // 切片获取浮点数值dp
						m=when{ // 区分特定生效侧向
							v.startsWith("t")->m.padding(top=n) // 顶边距
							v.startsWith("b")->m.padding(bottom=n) // 底边距
							v.startsWith("s")->m.padding(start=n) // 起始侧
							v.startsWith("e")->m.padding(end=n) // 终了侧
							v.startsWith("v")->m.padding(vertical=n) // 纵轴上下双向
							v.startsWith("h")->m.padding(horizontal=n) // 横轴左右双向
							v.toDoubleOrNull()!=null->m.padding(n) // 四周一体打包
							else->m
						}
					}
				}
			}
			'z'->{val v=t.drop(1) // 位移Offset大组
				when{ // 分配极向坐标
					v.startsWith("x")->{val n=v.drop(1).toDoubleOrNull()?.dp?:0.dp;m=m.offset(x=n)} // 水平横轴轴偏移
					v.startsWith("y")->{val n=v.drop(1).toDoubleOrNull()?.dp?:0.dp;m=m.offset(y=n)} // 垂直纵轴轴偏移
				}
			}
			'g'->{val v=t.drop(1) // 背景显色及图形组装全能大组
				val hx=v.takeWhile{it.isLetterOrDigit()} // 抓取颜色十六进制代码
				val rt=v.drop(hx.length) // 割离获取裁剪标志
				val ap=if(rt.startsWith("."))rt.drop(1).toDoubleOrNull()?.toFloat()?:1f else 1f // 解析不透明度参数值
				val bs=Color(AC.parseColor("#$hx")) // 换回设备底层色
				val fc=if(ap!=1f)bs.copy(alpha=ap) else bs // 赋予透明度全新混合色
				m=when{ // 选择形状切除遮罩
					rt.endsWith("c")->m.clip(CircleShape).background(fc) // 圆形切
					rt.contains("r")->{val r=rt.dropWhile{!it.isDigit()}.toDoubleOrNull()?.dp?:0.dp;m.clip(RoundedCornerShape(r)).background(fc)} // 圆角特定像素切
					else->m.background(fc) // 默认不变切普通直角背景
				}
			}
			'b'->{val v=t.drop(1) // 独立描边或多角异度切边修饰组
				when{ // 判定切角还是描边线
					v.startsWith("r")->{ // 独立切边
						val r=v.drop(1).split(",").mapNotNull{it.toDoubleOrNull()?.dp} // 割出复合四边半径参数
						val sh=when(r.size){1->RoundedCornerShape(r[0]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)} // 动态构筑非对称异度形
						m=m.clip(sh) // 剪
					}
					else->{ // 真实外边框线条绘制
						val pt=v.split(",") // 切出长宽厚度及颜色
						val wd=pt.getOrNull(0)?.toDoubleOrNull()?.dp?:1.dp // 线的物理厚度
						val hx=pt.getOrNull(1)?:"" // 十六进制色串
						val ap=pt.getOrNull(2)?.toFloatOrNull()?:1f // 线的透明度
						val fc=if(hx.isNotEmpty()){try{Color(AC.parseColor("#$hx"))}catch(_:Exception){Color.Black}}else Color.Black // 换算线条配色
						val c2=if(ap!=1f)fc.copy(alpha=ap) else fc // 锁定饱合度
						m=m.border(wd,c2) // 上色外边描线条
					}
				}
			}
			'c'->{val v=t.drop(1) // 极简单项切边Clip组
				m=when{ // 根据参数拆解形体
					v.isEmpty()->m.clip(CircleShape) // 圆形切
					v.contains(",")->{ // 高阶任意多边变数切
						val r=v.split(",").mapNotNull{it.toDoubleOrNull()?.dp} // 切出各个倒角半径
						val sh=when(r.size){1->RoundedCornerShape(r[0]);2->RoundedCornerShape(r[0],r[1]);3->RoundedCornerShape(r[0],r[1],r[2]);4->RoundedCornerShape(r[0],r[1],r[2],r[3]);else->RoundedCornerShape(0.dp)} // 构建形体
						m.clip(sh) // 剪切
					}
					v.toDoubleOrNull()!=null->m.clip(RoundedCornerShape(v.toDouble().dp)) // 均等标准圆角倒边
					else->m.clip(CircleShape)
				}
			}
			'a'->{a=when(t.drop(1)){"c"->Alignment.Center;"cs"->Alignment.CenterStart;"ce"->Alignment.CenterEnd;"ts"->Alignment.TopStart;"tc"->Alignment.TopCenter;"te"->Alignment.TopEnd;"bs"->Alignment.BottomStart;"bc"->Alignment.BottomCenter;"be"->Alignment.BottomEnd;else->null}} // 特殊视口布局对齐锚点极速映射
			'e'->{val v=t.drop(1);w=v.filter{it!='f'}.toFloatOrNull()?:1f;f=!v.contains('f')} // 视口容器内权重分配
			's'->{when(t){"sh"->m=m.horizontalScroll(rememberScrollState());"sv"->m=m.verticalScroll(rememberScrollState())}} // 容器快速附赠物理滑滚能力
			'r'->{val v=t.drop(1) // 锁定画面纵横画幅比例大项
				if(v.contains("x")){ // 捕捉乘数分割键
					val pt=v.split("x") // 分离宽和高
					val ww=pt.getOrNull(0)?.toFloatOrNull()?:1f // 宽因子
					val hh=pt.getOrNull(1)?.toFloatOrNull()?:1f // 高目标
					if(hh!=0f)m=m.aspectRatio(ww/hh) // 锁定并固化比例
				}
			}
		}
	}
	return CW(m,w,a,f) // 打包返回翻译出来的逻辑结果
}
@Composable fun String.css()=pcss(this).m // 纯粹泛用单体扩展属性方法
@Composable fun String.css(s:ColumnScope)=pcss(this).let{with(s){var m=it.m;it.w?.let{w->m=m.weight(w,it.f)};m}} // 垂直主轴专精样式注入方法
@Composable fun String.css(s:RowScope)=pcss(this).let{with(s){var m=it.m;it.w?.let{w->m=m.weight(w,it.f)};(it.a as? Alignment.Vertical)?.let{v->m=m.align(v)};m}} // 水平横轴专精样式注入方法
@Composable fun String.css(s:BoxScope)=pcss(this).let{with(s){var m=it.m;it.a?.let{a->m=m.align(a)};m}} // 多层叠压箱层专精样式注入方法