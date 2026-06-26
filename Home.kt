package com.fyan

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 爱壹帆卡片配色
private val AG1=Color(0xFF1A2A6C) // 深靛蓝
private val AG2=Color(0xFFB21F1F) // 烈红
private val AG3=Color(0xFFFDBC00) // 金黄
private val AA=Color(0xFFFFE082) // 浅金强调

// 日记卡片配色
private val DG1=Color(0xFF0F2027) // 深墨
private val DG2=Color(0xFF203A43) // 深青
private val DG3=Color(0xFF2C5364) // 海蓝
private val DL=Color(0x40A5D6F5) // 淡蓝横线
private val DA=Color(0xFF7ECBF3) // 水蓝强调

private val CS=RoundedCornerShape(8.dp) // 统一卡片圆角形状

@Composable fun Home(){
	Column(modifier=Modifier.fillMaxSize().systemBarsPadding().padding(15.dp),
		verticalArrangement=Arrangement.spacedBy(18.dp)
	){AyfCard();DiaryCard()}
}

// 爱壹帆卡片：极光渐变 + 旋转光晕 + 流光扫线 + 散点粒子 + 弹跳点击
@Composable fun AyfCard(){
	val it=rememberInfiniteTransition(label="ayf")
	val hr by it.animateFloat(label="rot",initialValue=0f,targetValue=360f, // 光晕旋转，8s一圈
		animationSpec=infiniteRepeatable(animation=tween(8000,easing=LinearEasing)))
	val so by it.animateFloat(label="shimmer",initialValue=-1f,targetValue=2f, // 流光扫描位置
		animationSpec=infiniteRepeatable(animation=tween(2400,easing=FastOutSlowInEasing),repeatMode=RepeatMode.Restart))
	var pr by remember{mutableStateOf(false)} // 按压状态
	val sc by animateFloatAsState(label="sc",targetValue=if(pr)0.955f else 1f, // 弹跳缩放
		animationSpec=spring(dampingRatio=0.4f,stiffness=500f))
	// 粒子列表（编译期常量，避免 Composable 重复分配）
	val pts=remember{listOf(0.15f to 0.25f,0.42f to 0.18f,0.68f to 0.72f,0.85f to 0.35f,0.30f to 0.80f,0.55f to 0.55f)}
	val prs=remember{listOf(3.5f,2.0f,4.0f,2.5f,1.8f,3.0f)}

	Box(modifier=Modifier.fillMaxWidth().height(160.dp).scale(sc).clip(CS)
		.deepShadow(color=Color(0x55B21F1F),offsetY=14.dp,blur=28.dp,spread=(-4).dp)
		.pointerInput(Unit){detectTapGestures(onPress={pr=true;tryAwaitRelease();pr=false;Fyan.goto("ayf_home")})}
	){
		Canvas(modifier=Modifier.matchParentSize()){
			val(w,h)=size.width to size.height
			// 1. 三色斜向底层渐变
			drawRect(brush=Brush.linearGradient(listOf(AG1,AG2,AG3),start=Offset(0f,h),end=Offset(w,0f)))
			// 2. 旋转光晕椭圆（Screen 混合叠加）
			rotate(hr,pivot=Offset(w*0.7f,h*0.3f)){
				drawOval(brush=Brush.radialGradient(listOf(Color(0x60FFF8DC),Color.Transparent),
					center=Offset(w*0.7f,h*0.3f),radius=w*0.5f),
					topLeft=Offset(w*0.25f,-h*0.2f),size=Size(w*0.9f,h*0.9f),blendMode=BlendMode.Screen)
			}
			// 3. 流光扫描线（随 so 移动）
			val sx=w*so
			drawRect(brush=Brush.linearGradient(listOf(Color.Transparent,Color(0x25FFFFFF),Color(0x60FFFFFF),Color(0x25FFFFFF),Color.Transparent),
				start=Offset(sx-60f,0f),end=Offset(sx+60f,h)),blendMode=BlendMode.Screen)
			// 4. 确定性散点粒子（位置/半径固定，无随机抖动）
			pts.forEachIndexed{i,(px,py)->drawCircle(Color(0x80FFE082),radius=prs[i].dp.toPx(),center=Offset(w*px,h*py),blendMode=BlendMode.Screen)}
			// 5. 底部半透暗条（提升文字对比度）
			drawRect(brush=Brush.verticalGradient(listOf(Color.Transparent,Color(0x50000000)),startY=h*0.52f,endY=h))
			// 6. 金色渐隐边框描边
			drawRoundRect(brush=Brush.linearGradient(listOf(Color(0x00FDBC00),Color(0x80FDBC00),Color(0x00FDBC00))),
				size=size,cornerRadius=CornerRadius(8.dp.toPx()),style=Stroke(1.2.dp.toPx()))
		}
		Column(modifier=Modifier.matchParentSize().padding(horizontal=24.dp,vertical=20.dp),verticalArrangement=Arrangement.SpaceBetween){
			Box(modifier=Modifier.background(Brush.horizontalGradient(listOf(Color(0x40FFFFFF),Color(0x15FFFFFF))),RoundedCornerShape(8.dp)) // 频道标签磨砂胶囊
				.padding(horizontal=10.dp,vertical=4.dp)){
				BasicText("精选频道",style=TextStyle(color=AA,fontSize=11.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.5.sp))
			}
			Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
				BasicText("爱壹帆",style=TextStyle(color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold,letterSpacing=0.5.sp))
				Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
					BasicText("进入主页",style=TextStyle(color=Color(0xCCFFFFFF),fontSize=13.sp))
					Canvas(modifier=Modifier.size(14.dp,10.dp)){ // 箭头图标
						val p=Path().apply{
							moveTo(0f,size.height/2);lineTo(size.width*0.6f,size.height/2)
							moveTo(size.width*0.35f,0f);lineTo(size.width,size.height/2);lineTo(size.width*0.35f,size.height)
						}
						drawPath(p,Color(0xCCFFFFFF),style=Stroke(1.6.dp.toPx(),cap=StrokeCap.Round))
					}
				}
			}
		}
	}
}

// 日记卡片：深海蓝调 + 日记横线 + 装订线 + 折页角 + 脉冲光晕 + 光标闪烁
@Composable fun DiaryCard(){
	val it=rememberInfiniteTransition(label="diary")
	val gp by it.animateFloat(label="glow",initialValue=0.4f,targetValue=1f, // 光晕脉冲强度
		animationSpec=infiniteRepeatable(animation=tween(1800,easing=FastOutSlowInEasing),repeatMode=RepeatMode.Reverse))
	val cb by it.animateFloat(label="cursor",initialValue=1f,targetValue=0f, // 光标闪烁透明度
		animationSpec=infiniteRepeatable(animation=tween(600),repeatMode=RepeatMode.Reverse))
	var pr by remember{mutableStateOf(false)} // 按压状态
	val sc by animateFloatAsState(label="sc",targetValue=if(pr)0.955f else 1f,
		animationSpec=spring(dampingRatio=0.4f,stiffness=500f))

	Box(modifier=Modifier.fillMaxWidth().height(160.dp).scale(sc).clip(CS)
		.deepShadow(color=Color(0x552C5364),offsetY=14.dp,blur=28.dp,spread=(-4).dp)
		.pointerInput(Unit){detectTapGestures(onPress={pr=true;tryAwaitRelease();pr=false})} // todo: 日记功能待接入
	){
		Canvas(modifier=Modifier.matchParentSize()){
			val(w,h)=size.width to size.height
			// 1. 深海斜向底层渐变
			drawRect(brush=Brush.linearGradient(listOf(DG1,DG2,DG3),start=Offset(0f,0f),end=Offset(w,h)))
			// 2. 四条淡蓝横线（模拟日记本横格）
			val ls=h/5f
			for(i in 1..4)drawLine(DL,Offset(20.dp.toPx(),ls*i),Offset(w-20.dp.toPx(),ls*i),0.8.dp.toPx())
			// 3. 左侧红色装订线
			drawLine(Color(0x60E57373),Offset(48.dp.toPx(),0f),Offset(48.dp.toPx(),h),1.2.dp.toPx())
			// 4. 右下角折页三角（翻页质感）
			val fs=36.dp.toPx()
			drawPath(Path().apply{moveTo(w-fs,h);lineTo(w,h-fs);lineTo(w,h);close()},
				brush=Brush.linearGradient(listOf(Color(0xFF0F2027),Color(0xFF1a3a4a)),
					start=Offset(w-fs,h-fs),end=Offset(w,h)))
			drawLine(Color(0x557ECBF3),Offset(w-fs,h),Offset(w,h-fs),1.dp.toPx()) // 折页高光边
			// 5. 右上角脉冲光晕（随 gp 强度呼吸）
			drawCircle(brush=Brush.radialGradient(listOf(DA.copy(alpha=0.18f*gp),Color.Transparent),
				center=Offset(w*0.8f,0f),radius=w*0.55f),
				radius=w*0.55f,center=Offset(w*0.8f,0f),blendMode=BlendMode.Screen)
			// 6. 水蓝色渐隐边框描边
			drawRoundRect(brush=Brush.linearGradient(listOf(Color(0x007ECBF3),Color(0x607ECBF3),Color(0x007ECBF3))),
				size=size,cornerRadius=CornerRadius(8.dp.toPx()),style=Stroke(1.dp.toPx()))
		}
		Column(modifier=Modifier.matchParentSize().padding(start=62.dp,end=24.dp,top=22.dp,bottom=20.dp), // 左侧留出装订线区域
			verticalArrangement=Arrangement.SpaceBetween){
			Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
				Canvas(modifier=Modifier.size(18.dp)){ // Canvas 绘制钢笔图标
					val(cx,cy)=size.width/2 to size.height/2
					drawRoundRect(DA,Offset(cx-3.dp.toPx(),cy-7.dp.toPx()),Size(6.dp.toPx(),10.dp.toPx()),CornerRadius(2.dp.toPx())) // 笔杆
					drawPath(Path().apply{moveTo(cx-3.dp.toPx(),cy+3.dp.toPx());lineTo(cx+3.dp.toPx(),cy+3.dp.toPx());lineTo(cx,cy+8.dp.toPx());close()},DA) // 笔尖
				}
				BasicText("我的日记",style=TextStyle(color=DA,fontSize=12.sp,fontWeight=FontWeight.SemiBold,letterSpacing=2.sp))
			}
			Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
				Row(verticalAlignment=Alignment.CenterVertically){ // 标题 + 闪烁光标
					BasicText("今天也要好好记录",style=TextStyle(color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold))
					Spacer(modifier=Modifier.width(2.dp))
					Box(modifier=Modifier.width(2.dp).height(22.dp).background(DA.copy(alpha=cb))) // 光标闪烁
				}
				BasicText("记录生活的每一刻",style=TextStyle(color=Color(0x99FFFFFF),fontSize=13.sp))
			}
		}
	}
}

// 多层阴影 Modifier 扩展：逐层叠加模拟真实阴影衰减，步数固定 8 层
fun Modifier.deepShadow(color:Color=Color(0x44000000),offsetY:Dp=8.dp,blur:Dp=16.dp,spread:Dp=0.dp):Modifier=drawBehind{
	val(dp,sp,bp,r)=listOf(offsetY.toPx(),spread.toPx(),blur.toPx(),8.dp.toPx())
	for(i in 8 downTo 1){
		val frac=i/8f;val expand=sp+bp*frac
		drawRoundRect(color.copy(alpha=color.alpha*(1f-frac)*0.35f), // 越外层越淡
			topLeft=Offset(-expand,-expand+dp*frac),
			size=Size(size.width+expand*2,size.height+expand*2),
			cornerRadius=CornerRadius(r+expand))
	}
}