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
	var vs=0L // 安装包内部版本指纹长整型
	var vn="" // 安装包公开版本文书代号

	@Suppress("DEPRECATION") fun init(a:Context){ // 全局初始硬化装载过程
		me=a.applicationContext // 截留上下文防止造成Activity物理泄漏
		tv=(me.resources.configuration.uiMode and 15)==4 // 提取系统位掩码确立TV硬件属性
		val p=me.packageManager.getPackageInfo(me.packageName,0) // 获取应用原始包底档
		vs=PackageInfoCompat.getLongVersionCode(p) // 兼容性解算28以下的版本代码
		vn=p.versionName?:"0" // 提取公布的版本文书
	}

	fun goto(o:String)=nc.navigate(o) // 全向轻量统一路由跳转跃迁器
	fun fetch(u:String):String=java.net.URL(u).openStream().bufferedReader().use{it.readText()} // IO线程流同步读写网络文本流
	val sw:Int get()=me.resources.configuration.screenWidthDp // 屏幕工作宽度dp
	val sh:Int get()=me.resources.configuration.screenHeightDp // 屏幕工作高度dp
	val gc:Int get()=if(sw>=840)6 else if(sw>=600)4 else 3 // 多端响应式网格弹性系数分级器

	object ff{ // 全局文字排版与样式集
		val h1=TextStyle(fontSize=24.sp,fontWeight=FontWeight.Bold) // 顶级大粗标题
		val h2=TextStyle(fontSize=20.sp,fontWeight=FontWeight.Bold) // 次级页面标题
		val h3=TextStyle(fontSize=18.sp,fontWeight=FontWeight.Bold) // 弹窗交互提示标题
		val h4=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Bold) // 顶部导航条小短语
		val p=TextStyle(fontSize=14.sp,fontWeight=FontWeight.Normal) // 全局基础正文样式
		val ps=TextStyle(fontSize=12.sp,fontWeight=FontWeight.Normal) // 微型小角标简介描述
		val pb=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Normal) // 加大版对话框文案描述
		val gr=TextStyle(fontSize=10.sp,lineHeight=1.06.em,fontWeight=FontWeight.Normal,fontFamily=FontFamily.Monospace) // 高密控制台专用等宽代码字体
	}

	class CC(o:Boolean){ // 精准反向极性色彩调配及自适应板
		val w=Color.White // 纯净高光白色
		val fc=if(o)Color(0xFF5C8EBE)else Color(0xFF90CAF9) // 电视端遥控聚焦识别亮蓝高亮
		val ps=if(o)Color(0xFF6A6A6A)else Color(0xFF9E9E9E) // 触摸指示下压感明确灰
		val m=if(o)Color(0xDD000000)else Color(0xDDFFFFFF) // 日志控制台雾面背景基色
		val bg=if(o)Color(0xFF000000)else Color(0xFFFFFFFF) // 画布视窗根骨背景底色
		val cg=if(o)Color(0xFF222222)else Color(0xFFDDDDDD) // 中度承载物理卡片架构色
		val ag=if(o)Color(0xFF333333)else Color(0xFFCCCCCC) // 骨架屏封面未就绪预填灰
		val c=if(o)Color(0xFFFFFFFF)else Color(0xFF000000) // 主反差通用字体色
		val bd=if(o)Color(0xFF444444)else Color(0xFFBBBBBB) // 细窄边缘轮廓线条色
		val x=if(o)Color(0xFF555555)else Color(0xFFCCCCCC) // 剧集未激活按键基色
		val info=if(o)Color(0xFF2196F3)else Color(0xFF1565C0) // 日志：常规通知蓝
		val error=if(o)Color(0xFFF44336)else Color(0xFFF44336) // 日志：警示故障红
		val warn=if(o)Color(0xFFFF9800)else Color(0xFFFF9800) // 日志：预防拦截橙
		val debug=if(o)Color(0xFFCE93D8)else Color(0xFF6A1B9A) // 日志：高密跟踪紫
		val success=if(o)Color(0xFF4CAF50)else Color(0xFF2E7D32) // 日志：健康放行绿
		val primary=if(o)Color(0xFF66AFFF)else Color(0xFF0066FF) // 唯一核心视效高亮着色
	}
	private val cd=CC(true);private val cl=CC(false) // 提前固化常驻亮暗两极色彩单例
	val cc:CC @Composable get()=if(isSystemInDarkTheme())cd else cl // 挂载响应重组无损返回非空极性单例对象

	private class Idn(private val s:InteractionSource,private val f:Color,private val p:Color):Modifier.Node(),DrawModifierNode{ // 高级多维焦点物理跟踪绘图节点
		private var jf=false;private var jp=false // 内部事件物理排重锁
		override fun onAttach(){ // 组件树成功挂载事件生命周期
			super.onAttach()
			coroutineScope.launch{ // 启动单向流异步监听管道
				var fc=0;var pc=0 // 初始化状态累加值
				s.interactions.collect{i-> // 管道高密收集核心状态行为
					when(i){
						is FocusInteraction.Focus->fc++
						is FocusInteraction.Unfocus->fc--
						is PressInteraction.Press->pc++
						is PressInteraction.Release,is PressInteraction.Cancel->pc--
					}
					val nf=fc>0;val np=pc>0 // 测算复合原子开关
					if(jf!=nf||jp!=np){jf=nf;jp=np;invalidateDraw()} // 向引擎内核申请重刷画布
				}
			}
		}
		override fun ContentDrawScope.draw(){if(jp)drawRect(p)else if(jf)drawRect(f);drawContent()} // 图形核心树画布输出
	}
	class Idf(private val f:Color,private val p:Color):IndicationNodeFactory{ // 适配现代BOM层级的指示器工厂
		override fun create(s:InteractionSource):DelegatableNode=Idn(s,f,p) // 指派交付绘图代理
		override fun equals(other:Any?):Boolean=other is Idf&&f==other.f&&p==other.p // 判定同质等效
		override fun hashCode():Int=31*f.hashCode()+p.hashCode() // 生成唯一散列防冲突
	}

	private val Context.ds by preferencesDataStore("fyan") // 创生唯一的进程级强持久化互斥底盘落盘数据库
	@Suppress("UNCHECKED_CAST") private fun <T> String.cype(v:T)=(when(v){ // 本地强类型自适应键智能指派序列化分配器
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
	fun <T> cg(k:String,d:T)=me.ds.data.map{p->k.cype(d)?.let{p[it]}?:d} // 无死锁异步无缝并发数据追踪流
	suspend fun <T> cs(k:String,v:T)=me.ds.edit{p->k.cype(v)?.let{p[it]=v}} // 原子态写保护异步覆写持久化函数
	suspend fun cx(k:String)=me.ds.edit{p->p.asMap().keys.firstOrNull{it.name==k}?.let{p.remove(it)}} // 行匹配全擦除或断开目标序列名
	suspend fun <T> co(k:String,d:T)=cg(k,d).first() // 原子挂起同步捕获单次当前内存副本常驻快照

	private val gs=mutableStateListOf<String>() // 控制台全局高频刷新缓冲区内存序列
	private var gn by mutableStateOf(false) // 控制台抽屉折叠开关状态
	private var gy by mutableStateOf(0f) // 面板纯像素级别拖拽绝对物理位移跟踪器
	private val gh=Handler(Looper.getMainLooper()) // 指向最高优先级原生UI主线程信息管道循环
	private fun gc()=gs.clear() // 全速洗空控制台内存池
	private fun gx(i:String)=gs.removeAll{it.startsWith(i)} // 精密流水ID匹配强行灭活过滤日志行
	fun log(m:String,o:String,c:Char='i'){ // 顶层集约化高性能入队审计排队器
		val t=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date()) // 构建精细时钟标记文书
		val v=UUID.randomUUID().toString().replace("-","")+".${c}●$t $m ➜ $o" // 统一打包指纹行结构
		gh.post{gs.add(v)} // 推入UI刷新环尾部
	}
	@Composable fun Record(){if(gn||gs.isEmpty())RX()else RO()} // 控制台状态多模态调度主干分流热区
	@Composable private fun RX(){ // 极度压缩缩略形态下的控制台胶囊长条
		val cc=Fyan.cc
		Box(modifier=Modifier.fillMaxWidth(0.7f).height(6.dp).padding(bottom=2.dp).clip(RoundedCornerShape(2.dp)).background(cc.cg).clickable{gn=false;gy=0f},contentAlignment=Alignment.Center){} // 一键复位唤醒控制台触控区
	}
	@Composable private fun RO(){ // 全尺寸明细信息量控制台仪表盘结构
		val s=rememberLazyListState() // 滚轴滚动对齐状态锁定器
		val cc=Fyan.cc;val f=Fyan.ff // 双路顶级单例对齐注入快照，不产生任何混淆
		val dy=androidx.compose.ui.platform.LocalDensity.current.density // 测算物理屏幕的栅格像素密度系数
		LaunchedEffect(gs.size){if(gs.isNotEmpty())s.animateScrollToItem(gs.size-1)} // 尾部实时追随对齐
		Box(modifier=Modifier.fillMaxWidth().heightIn(max=(Fyan.sh/3).dp).navigationBarsPadding().offset{androidx.compose.ui.unit.IntOffset(0,gy.roundToInt())}.pointerInput(Unit){ // 控制台悬浮大垫片。优化：offset转为Lambda传纯像素彻底瓦解由于拖动造成的高频重组卡顿
			detectDragGestures(onDragEnd={if(gy>40*dy){gn=true;gy=0f}else gy=0f},onDrag={ch,o->ch.consume();if(gy+o.y>=0f)gy+=o.y}) // 测算绝对像素物理位移及触底折叠机制
		}){
			Box(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=4.dp,topEnd=4.dp)).background(cc.m).drawWithContent{ // 雾面底层承载板
				drawContent() // 渲染核心子代内容
				val (w,r)=0.5.dp.toPx() to 4.dp.toPx() // 计算细边缘像素高宽
				drawPath(path=Path().apply{moveTo(0f,size.height);lineTo(0f,r);arcTo(Rect(0f,0f,r*2,r*2),180f,90f,false);lineTo(size.width-r,0f);arcTo(Rect(size.width-r*2,0f,size.width,r*2),270f,90f,false);lineTo(size.width,size.height)},color=cc.bd,style=Stroke(w,cap=StrokeCap.Round,join=StrokeJoin.Round)) // 纯画笔细腻手绘无缝合围框线
			}){
				Column(modifier=Modifier.fillMaxWidth().padding(4.dp)){ // 内容高压收合紧凑板
					Box(modifier=Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Box(modifier=Modifier.fillMaxWidth(0.25f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(cc.ag).padding(top=2.dp).clickable(enabled=tv){gn=true;gy=0f})} // 抽屉横条把手指示卡
					Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){BasicText("日志 · ${gs.size}条",style=f.ps.copy(color=cc.c));Box(modifier=Modifier.clickable{gc()}){BasicText("清空",style=f.ps.copy(color=Color(0xFFF44336)))}} // 仪表盘实时容量统计与清空按钮热区
					LazyColumn(state=s,modifier=Modifier.fillMaxWidth().padding(bottom=6.dp)){ // 控制台流式输出长轨
						items(gs){o-> // 高能增量遍历单条审计流水
							val x=o.split("●",limit=2);val z=x[0].lastIndexOf('.') // 解析物理结构字段
							val id=if(z>0)x[0].substring(0,z)else x[0];val cx=if(z>0)x[0].substring(z+1)else"i" // 提纯独立事件流水特征码
							val clr=when(cx){"i"->cc.info;"e"->cc.error;"w"->cc.warn;"s"->cc.success;"d"->cc.debug;else->cc.c} // 指派级别颜色映射
							Box(modifier=Modifier.fillMaxWidth().height(0.5.dp).background(cc.bd)) // 单行最细分割横梁线
							Row(modifier=Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.SpaceBetween){BasicText(x.getOrElse(1){"...."},modifier=Modifier.weight(1f).padding(end=4.dp),style=f.gr.copy(color=clr));Box(modifier=Modifier.size(14.dp).clickable{gx(id)},contentAlignment=Alignment.Center){BasicText("╳",style=f.ps.copy(color=cc.c))}} // 单行详细日志文案输出及独立删行控制锚点
						}
					}
				}
			}
		}
	}

	var br:BroadcastReceiver?=null // 静默热更新系统硬件监听器引用句柄
	fun up(v:String,u:String){ // 远程更新安装发布核心底座总线
		val r=DownloadManager.Request(Uri.parse(u)).apply{setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE);setTitle("应用更新");setDescription("后台下载中...");setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);setMimeType("application/vnd.android.package-archive");setDestinationInExternalFilesDir(Fyan.me,"Download","fyan_$v.apk")} // 指派系统任务配置参数
		val m=Fyan.me.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager // 调用系统原生文件接收中心
		val id=m.enqueue(r) // 推入下载长轨任务队列
		Toast.makeText(Fyan.me,"检测到新版，已开启后台静默下载",Toast.LENGTH_SHORT).show() // 弹出全局气泡反馈提示
		br?.let{Fyan.me.unregisterReceiver(it)} // 安全释放残留监听器
		br=object:BroadcastReceiver(){ // 重连注册
			override fun onReceive(c:Context,i:Intent){ // 全量数据收取完毕中断响应
				if(i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1L)!=id)return // 防串身份标志校验
				val f=File(Fyan.me.getExternalFilesDir("Download"),"fyan_$v.apk") // 定位物理文件底档
				if(!f.exists())return // 稳健性检测
				val uri=if(Build.VERSION.SDK_INT>=24)FileProvider.getUriForFile(Fyan.me,"${Fyan.me.packageName}.fileprovider",f)else Uri.fromFile(f) // 沙盒安全穿透，提取向导合规URI
				Fyan.me.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,"application/vnd.android.package-archive");addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)}) // 唤醒物理系统的APK软件包安装引导程序
			}
		}
		Fyan.me.registerReceiver(br,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) // 启动系统级中断总线绑定
	}
}




class O:ComponentActivity(){ // 应用顶层唯一物理窗口承载容器
	override fun onCreate(s:Bundle?){ // 窗体物理激活初创起点
		super.onCreate(s) // 移交基类装载
		Fyan.init(this) // 全局公共架构初始化
		setContent{ // 切入声明式编排树
			val cc=Fyan.cc;val f=Fyan.ff // 样式首位就地单例捕获
			CompositionLocalProvider(LocalIndication provides remember(cc){Fyan.Idf(cc.fc,cc.ps)}){ // 全局水波纹高亮拦截器
				Fyan.nc=rememberNavController() // 初始化并绑定全局路由环
				var ex by remember{mutableStateOf(false)} // 拦截退出程序标志交互开关
				BackHandler(enabled=!ex){ex=true} // 首次按返回抛出二次确认
				if(ex)Dialog(onDismissRequest={ex=false}){ // 销毁整个App主线程的确认警示弹窗
					Column(modifier=Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(12.dp)).background(cc.cg).border(1.dp,cc.bd,RoundedCornerShape(12.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally){ // 弹窗磨砂面板底座
						BasicText("系统提醒",style=f.h2.copy(color=cc.c)) // 退出警示字头
						BasicText("确定退出应用程序吗？",style=f.p.copy(color=cc.c.copy(alpha=0.6f))) // 细化描述说明
						Row(modifier=Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){ // 确认执行行
							Box(modifier=Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(6.dp)).background(cc.ag).clickable{ex=false},contentAlignment=Alignment.Center){BasicText("取消",style=f.p.copy(color=cc.c))} // 撤回取消消退
							Box(modifier=Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(6.dp)).background(cc.primary).clickable{Process.killProcess(Process.myPid())},contentAlignment=Alignment.Center){BasicText("确定",style=f.p.copy(color=Color.White))} // 执意退出则发送强硬自毁信号清洗底层物理进程
						}
					}
				}
				Box(modifier=Modifier.fillMaxSize().background(cc.bg)){ // 声明式页面基础布局主舞台
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
