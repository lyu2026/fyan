package com.fyan

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

object Fyan{ // 全局底层核心基座系统
	lateinit var nc:NavHostController // 全局导航总控制器引用指针
	lateinit var me:Context // 全局常驻应用唯一ApplicationContext
	var tv=false // 电视TV设备标识硬件过滤器
	var vs=0L // 安装包内部版本指纹
	var vn="" // 安装包公开版本代号

	@Suppress("DEPRECATION") fun init(a:Context){ // 全局初始硬化装载过程
		me=a.applicationContext // 截留上下文防止泄漏
		tv=(me.resources.configuration.uiMode and 15)==4 // 提取系统位掩码确立TV硬件属性
		val p=me.packageManager.getPackageInfo(me.packageName,0) // 获取应用包底档
		vs=PackageInfoCompat.getLongVersionCode(p) // 解算长代码版本
		vn=p.versionName?:"0" // 提取版本文书名称
	}

	fun goto(o:String)=nc.navigate(o) // 全向轻量导航跃迁封装器
	fun fetch(u:String):String=java.net.URL(u).openStream().bufferedReader().use{it.readText()} // 原生同步读写吞吐网络流
	val sw:Int get()=me.resources.configuration.screenWidthDp // 屏幕实时工作宽度dp
	val sh:Int get()=me.resources.configuration.screenHeightDp // 屏幕实时工作高度dp
	val gc:Int get()=if(sw>=840)6 else if(sw>=600)4 else 3 // 多端自适应网格弹性系数分级器

	object ff{ // 系统底层集中管理文字排版与样式集
		val h1=TextStyle(fontSize=24.sp,fontWeight=FontWeight.Bold) // 顶级粗标题
		val h2=TextStyle(fontSize=20.sp,fontWeight=FontWeight.Bold) // 次级次标题
		val h3=TextStyle(fontSize=18.sp,fontWeight=FontWeight.Bold) // 菜单二级提示词
		val h4=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Bold) // 功能导航条短语
		val p=TextStyle(fontSize=14.sp,fontWeight=FontWeight.Normal) // 全局基础正文排版
		val ps=TextStyle(fontSize=12.sp,fontWeight=FontWeight.Normal) // 次要注解微型字样
		val pb=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Normal) // 加大正文行距字样
		val gr=TextStyle(fontSize=10.sp,lineHeight=1.06.em,fontWeight=FontWeight.Normal,fontFamily=FontFamily.Monospace) // 控制台等宽代码字体
	}

	class CC(o:Boolean){ // 精准反向色彩调配及自适应板
		val w=Color.White // 全光纯白色
		val fc=if(o)Color(0xFF5C8EBE)else Color(0xFF90CAF9) // 电视端遥控聚焦高亮方案
		val ps=if(o)Color(0xFF6A6A6A)else Color(0xFF9E9E9E) // 系统触摸波纹下压反馈色
		val m=if(o)Color(0xDD000000)else Color(0xDDFFFFFF) // 控制台遮罩雾面基色
		val bg=if(o)Color(0xFF000000)else Color(0xFFFFFFFF) // 画布视窗根骨底色
		val cg=if(o)Color(0xFF222222)else Color(0xFFDDDDDD) // 中度承载卡片骨架色
		val ag=if(o)Color(0xFF333333)else Color(0xFFCCCCCC) // 未加载图素占位填充灰
		val c=if(o)Color(0xFFFFFFFF)else Color(0xFF000000) // 主反差字体色
		val bd=if(o)Color(0xFF444444)else Color(0xFFBBBBBB) // 细窄边缘线条色
		val x=if(o)Color(0xFF555555)else Color(0xFFCCCCCC) // 集数默认背景色
		val info=if(o)Color(0xFF2196F3)else Color(0xFF1565C0) // 常规信息指示蓝
		val error=if(o)Color(0xFFF44336)else Color(0xFFF44336) // 警示故障红
		val warn=if(o)Color(0xFFFF9800)else Color(0xFFFF9800) // 阻断预警橙
		val debug=if(o)Color(0xFFCE93D8)else Color(0xFF6A1B9A) // 跟踪调用高密紫
		val success=if(o)Color(0xFF4CAF50)else Color(0xFF2E7D32) // 健康安全绿
		val primary=if(o)Color(0xFF66AFFF)else Color(0xFF0066FF) // 品牌唯一主视觉色
	}
	private val cd=CC(true);private val cl=CC(false) // 提前锁死生成亮暗两极常驻单例
	val cc:CC @Composable get()=if(isSystemInDarkTheme())cd else cl // 挂载响应重组无开销返回极性单例对象

	private class Idn(private val s:InteractionSource,private val f:Color,private val p:Color):Modifier.Node(),DrawModifierNode{ // 多维焦点跟踪绘图加速器
		private var jf=false;private var jp=false // 高频物理状态锁
		override fun onAttach(){ // 组件树成功锚接事件
			super.onAttach()
			coroutineScope.launch{ // 启动单向流管道监听
				var fc=0;var pc=0 // 缓存内部局部计数器
				s.interactions.collect{i-> // 管道高密收集
					when(i){
						is FocusInteraction.Focus->fc++
						is FocusInteraction.Unfocus->fc--
						is PressInteraction.Press->pc++
						is PressInteraction.Release,is PressInteraction.Cancel->pc--
					}
					val nf=fc>0;val np=pc>0 // 计算复合状态
					if(jf!=nf||jp!=np){jf=nf;jp=np;invalidateDraw()} // 发生裂变向图形驱动申请重刷画布区
				}
			}
		}
		override fun ContentDrawScope.draw(){if(jp)drawRect(p)else if(jf)drawRect(f);drawContent()} // 图形渲染引擎最终输出覆盖层
	}
	class Idf(private val f:Color,private val p:Color):IndicationNodeFactory{ // 适配现代BOM总线的无遗留微变工厂
		override fun create(s:InteractionSource):DelegatableNode=Idn(s,f,p) // 动态吐出加速单元
		override fun equals(other:Any?):Boolean=other is Idf&&f==other.f&&p==other.p // 高效同质等效性校正
		override fun hashCode():Int=31*f.hashCode()+p.hashCode() // 唯一散列冲突规避
	}

	private val Context.ds by preferencesDataStore("fyan") // 建立进程级全局共享唯一本地落盘数据库
	@Suppress("UNCHECKED_CAST") private fun <T> String.cype(v:T)=(when(v){ // 本地强类型自适应键指派映射器
		is ByteArray->byteArrayPreferencesKey(this)
		is Boolean->booleanPreferencesKey(this)
		is Set<*>->stringSetPreferencesKey(this)
		is Double->doublePreferencesKey(this)
		is String->stringPreferencesKey(this)
		is Float->floatPreferencesKey(this)
		is Long->longPreferencesKey(this)
		is Int->intPreferencesKey(this)
		else->null
	}as?Preferences.Key<T>)
	fun <T> cg(k:String,d:T)=me.ds.data.map{p->k.cype(d)?.let{p[it]}?:d} // 零死锁异步无缝跟踪数据响应流
	suspend fun <T> cs(k:String,v:T)=me.ds.edit{p->k.cype(v)?.let{p[it]=v}} // 原子化挂起式覆写持久化函数
	suspend fun cx(k:String)=me.ds.edit{p->p.asMap().keys.firstOrNull{it.name==k}?.let{p.remove(it)}} // 行匹配式全清或移除目标键方法
	suspend fun <T> co(k:String,d:T)=cg(k,d).first() // 刚性同步截取单次当前值常驻快照

	private val gs=mutableStateListOf<String>() // 控制台全局内存常驻高频刷新缓冲区
	private var gn by mutableStateOf(false) // 交互面板折叠开关
	private var gy by mutableStateOf(0f) // 面板纯像素位移物理距离跟踪器
	private val gh=Handler(Looper.getMainLooper()) // 指向最高优先级主线程循环管道
	private fun gc()=gs.clear() // 极速擦除内存缓冲区
	private fun gx(i:String)=gs.removeAll{it.startsWith(i)} // 利用流水ID精准灭活过滤日志行
	fun log(m:String,o:String,c:Char='i'){ // 集中式高性能入队审计器
		val t=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date()) // 测算毫秒钟表文书格式
		val v=UUID.randomUUID().toString().replace("-","")+".${c}●$t $m ➜ $o" // 高压固化结构
		gh.post{gs.add(v)} // 压入调度队列尾部
	}
	@Composable fun Record(){if(gn||gs.isEmpty())RX()else RO()} // 视图顶层多态调度主干分流热区
	@Composable private fun RX(){ // 压缩手势复位条组件
		val c=Fyan.cc
		Box(modifier=Modifier.fillMaxWidth(0.7f).height(6.dp).padding(bottom=2.dp).clip(RoundedCornerShape(2.dp)).background(c.cg).clickable{gn=false;gy=0f},contentAlignment=Alignment.Center){} // 复原轻触响应区
	}
	@Composable private fun RO(){ // 全尺寸信息量大控制台面板结构
		val s=rememberLazyListState() // 锁定滚动追踪器
		val c=Fyan.cc;val f=Fyan.ff // 双路顶级单例注入快照
		val dy=androidx.compose.ui.platform.LocalDensity.current.density // 测算屏幕真实的物理栅格化密度系数
		LaunchedEffect(gs.size){if(gs.isNotEmpty())s.animateScrollToItem(gs.size-1)} // 尾部自对齐追随滚动逻辑
		Box(modifier=Modifier.fillMaxWidth().heightIn(max=(Fyan.sh/3).dp).navigationBarsPadding().offset{androidx.compose.ui.unit.IntOffset(0,gy.roundToInt())}.pointerInput(Unit){ // 控制台悬浮基础垫片。优化：offset变身高效Lambda彻底瓦解触控过程引发的重组
			detectDragGestures(onDragEnd={if(gy>40*dy){gn=true;gy=0f}else gy=0f},onDrag={ch,o->ch.consume();if(gy+o.y>=0f)gy+=o.y}) // 测算像素偏移量及复位隐藏边界逻辑
		}){
			Box(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=4.dp,topEnd=4.dp)).background(c.m).drawWithContent{ // 雾面底层容器板
				drawContent() // 绘制原生布局文本
				val (w,r)=0.5.dp.toPx() to 4.dp.toPx() // 算绘边线半径参数
				drawPath(path=Path().apply{moveTo(0f,size.height);lineTo(0f,r);arcTo(Rect(0f,0f,r*2,r*2),180f,90f,false);lineTo(size.width-r,0f);arcTo(Rect(size.width-r*2,0f,size.width,r*2),270f,90f,false);lineTo(size.width,size.height)},color=c.bd,style=Stroke(w,cap=StrokeCap.Round,join=StrokeJoin.Round)) // 手绘合围精致外边线
			}){
				Column(modifier=Modifier.fillMaxWidth().padding(4.dp)){ // 内容纵向紧凑板
					Box(modifier=Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Box(modifier=Modifier.fillMaxWidth(0.25f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.ag).padding(top=2.dp).clickable(enabled=tv){gn=true;gy=0f})} // 拖拽手势指示条
					Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){BasicText("日志 · ${gs.size}条",style=f.ps.copy(color=c.c));Box(modifier=Modifier.clickable{gc()}){BasicText("清空",style=f.ps.copy(color=Color(0xFFF44336)))}} // 仪表统计及擦除开关
					LazyColumn(state=s,modifier=Modifier.fillMaxWidth().padding(bottom=6.dp)){ // 控制台输出滚轨
						items(gs){o-> // 高密度迭代单条日志流
							val x=o.split("●",limit=2);val z=x[0].lastIndexOf('.') // 解析格式化字段
							val id=if(z>0)x[0].substring(0,z)else x[0];val cx=if(z>0)x[0].substring(z+1)else"i" // 提纯流水特征
							val clr=when(cx){"i"->c.info;"e"->c.error;"w"->c.warn;"s"->c.success;"d"->c.debug;else->c.c} // 级别字色自适应映射
							Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(c.bd)) // 极细单条分割杠
							Row(modifier=Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){BasicText(x.getOrElse(1){"...."},modifier=Modifier.weight(1f).padding(end=4.dp),style=f.gr.copy(color=clr));Box(modifier=Modifier.size(14.dp).clickable{gx(id)},contentAlignment=Alignment.Center){BasicText("╳",style=f.ps.copy(color=c.c))}} // 单行详细日志与独立手动删行锚点
						}
					}
				}
			}
		}
	}

	var br:BroadcastReceiver?=null // 后台静默升级句柄引用
	fun up(v:String,u:String){ // 升级安装发布底层总线
		val r=DownloadManager.Request(Uri.parse(u)).apply{setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE);setTitle("应用更新");setDescription("后台下载中...");setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);setMimeType("application/vnd.android.package-archive");setDestinationInExternalFilesDir(Fyan.me,"Download","fyan_$v.apk")} // 指派升级包配置
		val m=Fyan.me.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager // 系统原生文件引擎
		val id=m.enqueue(r) // 排队推入底层任务链
		Toast.makeText(Fyan.me,"检测到新版，已开启后台静默下载",Toast.LENGTH_SHORT).show() // 用户气泡弱反馈提示
		br?.let{Fyan.me.unregisterReceiver(it)} // 销毁废旧拦截实体
		br=object:BroadcastReceiver(){ // 重新挂载硬件收发管路
			override fun onReceive(c:Context,i:Intent){ // 下载全面完成硬件通知接收
				if(i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1L)!=id)return // 精密防串身份码校正
				val f=File(Fyan.me.getExternalFilesDir("Download"),"fyan_$v.apk") // 获取物理包
				if(!f.exists())return // 落盘完整性判定
				val uri=if(Build.VERSION.SDK_INT>=24)FileProvider.getUriForFile(Fyan.me,"${Fyan.me.packageName}.fileprovider",f)else Uri.fromFile(f) // 沙盒穿透机制提取高内聚URI指纹
				Fyan.me.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,"application/vnd.android.package-archive");addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)}) // 全向轰开原生系统的APK应用安装向导
			}
		}
		Fyan.me.registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) // 绑定中断广播接收
	}
}




class O:ComponentActivity(){ // 应用顶层唯一承载物理窗口容器
	override fun onCreate(s:Bundle?){ // 窗体物理激活初创起点
		super.onCreate(s) // 移交基类装载
		Fyan.init(this) // 全局公共架构初始化
		setContent{ // 切入声明式编排树
			val c=Fyan.cc;val f=Fyan.ff // 双重单例快照就地捕获
			CompositionLocalProvider(LocalIndication provides remember(c){Fyan.Idf(c.fc,c.ps)}){ // 全局水波纹高亮拦截器
				Fyan.nc=rememberNavController() // 初始化并绑定全局路由环
				var ex by remember{mutableStateOf(false)} // 拦截退出程序标志交互开关
				BackHandler(enabled=!ex){ex=true} // 首次按返回抛出二次确认
				if(ex)Dialog(onDismissRequest={ex=false}){ // 销毁整个App主线程的确认警示弹窗
					Column(modifier=Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(12.dp)).background(c.cg).border(1.dp,c.bd,RoundedCornerShape(12.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally){ // 弹窗磨砂面板底座
						BasicText("系统提醒",style=f.h2.copy(color=c.c)) // 退出警示字头
						BasicText("确定退出应用程序吗？",style=f.p.copy(color=c.c.copy(alpha=0.6f))) // 细化描述说明
						Row(modifier=Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){ // 确认执行行
							Box(modifier=Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(6.dp)).background(c.ag).clickable{ex=false},contentAlignment=Alignment.Center){BasicText("取消",style=f.p.copy(color=c.c))} // 撤回取消消退
							Box(modifier=Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(6.dp)).background(c.primary).clickable{Process.killProcess(Process.myPid())},contentAlignment=Alignment.Center){BasicText("确定",style=f.p.copy(color=Color.White))} // 执意退出则发送强硬自毁信号清洗底层物理进程
						}
					}
				}
				Box(modifier=Modifier.fillMaxSize().background(c.bg)){ // 声明式页面基础布局主舞台
					NavHost(navController=Fyan.nc,startDestination="ayf_home"){ // 导航状态机
						composable("ayf_home"){AyfHome()} // 指配首页目的地
						composable("ayf_info/{id}"){b->AyfInfo(b.arguments?.getString("id")?:"")} // 指配详情页目的地
					}
					Box(modifier=Modifier.align(Alignment.BottomCenter)){Fyan.Record()} // 在屏幕底部常驻浮置审计控制台面板组件
				}
			}
		}
	}
	override fun onDestroy(){super.onDestroy();Fyan.br?.let{unregisterReceiver(it);Fyan.br=null}} // 当物理销毁事件触发时，刚性解绑接收器，杜绝可能产生的上下文悬挂内存泄漏
}
