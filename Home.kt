package com.fyan

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

//─────────────────────────────────────────────────────────────────────────────
//颜色系统（与外部 cc.bg 保持协调）
//─────────────────────────────────────────────────────────────────────────────
private val AyfGradientStart=Color(0xFF1A2A6C) //深靛蓝
private val AyfGradientMid=Color(0xFFB21F1F) //烈红
private val AyfGradientEnd=Color(0xFFFDBC00) //金黄
private val AyfAccent=Color(0xFFFFE082) //浅金

private val DiaryGradientTop=Color(0xFF0F2027) //深墨
private val DiaryGradientMid=Color(0xFF203A43) //深青
private val DiaryGradientBot=Color(0xFF2C5364) //海蓝
private val DiaryLine=Color(0x40A5D6F5) //淡蓝线
private val DiaryAccent=Color(0xFF7ECBF3) //水蓝

private val CardShape=RoundedCornerShape(8.dp)

//─────────────────────────────────────────────────────────────────────────────
//入口：两张卡片的容器
//─────────────────────────────────────────────────────────────────────────────
@Composable fun Home(){
	Column(modifier=Fya.mc("⌽ ⧈:s ⧈:15"),
		verticalArrangement=Arrangement.spacedBy(18.dp)
	){AyfCard();DiaryCard()}
}

//─────────────────────────────────────────────────────────────────────────────
//卡片 1：爱壹帆
//极光渐变+旋转光晕+压缩弹跳点击
//─────────────────────────────────────────────────────────────────────────────
@Composable fun AyfCard(){
	val textMeasurer=rememberTextMeasurer()

	//旋转光晕动画
	val iftr=rememberInfiniteTransition(label="ayf_halo")
	val hr by iftr.animateFloat(
		label="halo_rot",initialValue=0f,targetValue=360f,
		animationSpec=infiniteRepeatable(animation=tween(8000,easing=LinearEasing))
	)
	val so by iftr.animateFloat(
		label="shimmer",initialValue=-1f,targetValue=2f,
		animationSpec=infiniteRepeatable(animation=tween(2400,easing=FastOutSlowInEasing),repeatMode=RepeatMode.Restart)
	)

	//点击弹跳
	var pressed by remember{mutableStateOf(false)}
	val scale by animateFloatAsState(
		label="ayf_scale",targetValue=if(pressed) 0.955f else 1f,
		animationSpec=spring(dampingRatio=0.4f,stiffness=500f)
	)

	Box(modifier=Modifier.fillMaxWidth().height(160.dp).scale(scale).clip(CardShape)
		.deepShadow(color=Color(0x55B21F1F),offsetY=14.dp,blur=28.dp,spread=(-4).dp)
		.pointerInput(Unit){
				detectTapGestures(
					onPress={
						pressed=true;tryAwaitRelease()
						pressed=false;Fyan.goto("ayf_home")
					}
				)
			}
	){
		//背景 Canvas：极光渐变+旋转光晕+粒子
		Canvas(modifier=Modifier.matchParentSize()){
			//1.底层三色渐变
			drawRect(brush=Brush.linearGradient(
				colors=listOf(AyfGradientStart,AyfGradientMid,AyfGradientEnd),
				start=Offset(0f,size.height),end=Offset(size.width,0f)
			))
			//2.旋转光晕（大椭圆，叠加混合）
			rotate(hr,pivot=Offset(size.width*0.7f,size.height*0.3f)){
				drawOval(brush=Brush.radialGradient(
					colors=listOf(Color(0x60FFF8DC),Color.Transparent),
					center=Offset(size.width*0.7f,size.height*0.3f),
					radius=size.width*0.5f
				),topLeft=Offset(size.width*0.25f,-size.height*0.2f),
				size=Size(size.width*0.9f,size.height*0.9f),
				blendMode=BlendMode.Screen)
			}
			//3.流光扫描线
			val shimmerX=size.width*so
			drawRect(brush=Brush.linearGradient(
				colors=listOf(Color.Transparent,Color(0x25FFFFFF),Color(0x60FFFFFF),Color(0x25FFFFFF),Color.Transparent),
				start=Offset(shimmerX-60f,0f),end=Offset(shimmerX+60f,size.height)
			),blendMode=BlendMode.Screen)
			//4.散点粒子（静态，基于 size 确定性分布）
			val particles=listOf(
				Offset(size.width*0.15f,size.height*0.25f) to 3.5f,
				Offset(size.width*0.42f,size.height*0.18f) to 2.0f,
				Offset(size.width*0.68f,size.height*0.72f) to 4.0f,
				Offset(size.width*0.85f,size.height*0.35f) to 2.5f,
				Offset(size.width*0.30f,size.height*0.80f) to 1.8f,
				Offset(size.width*0.55f,size.height*0.55f) to 3.0f
			)
			particles.forEach{(pos,r)->
				drawCircle(color=Color(0x80FFE082),radius=r.dp.toPx(),center=pos,blendMode=BlendMode.Screen)
			}
			//5.底部磨砂条（内容分区）
			drawRect(brush=Brush.verticalGradient(
				colors=listOf(Color.Transparent,Color(0x50000000)),
				startY=size.height*0.52f,endY=size.height
			))
			//6.边缘金色描边
			drawRoundRect(brush=Brush.linearGradient(
				colors=listOf(Color(0x00FDBC00),Color(0x80FDBC00),Color(0x00FDBC00))
			),size=size,cornerRadius=CornerRadius(8.dp.toPx()),
			style=Stroke(width=1.2.dp.toPx()))
		}
		//文字内容层
		Column(modifier=Modifier.matchParentSize().padding(horizontal=24.dp,vertical=20.dp),verticalArrangement=Arrangement.SpaceBetween){
			Box( //标签
				modifier=Modifier.background(
					brush=Brush.horizontalGradient(
						listOf(Color(0x40FFFFFF),Color(0x15FFFFFF))
					),shape=RoundedCornerShape(8.dp)
				).padding(horizontal=10.dp,vertical=4.dp)
			){
				androidx.compose.foundation.text.BasicText(
					text="精选频道",style=TextStyle(
						color=AyfAccent,fontSize=11.sp,
						fontWeight=FontWeight.SemiBold,letterSpacing=1.5.sp
					)
				)
			}
			Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
				androidx.compose.foundation.text.BasicText(
					text="爱壹帆",style=TextStyle(
						color=Color.White,fontSize=28.sp,
						fontWeight=FontWeight.Bold,letterSpacing=0.5.sp
					)
				)
				Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
					androidx.compose.foundation.text.BasicText(
						text="进入主页",
						style=TextStyle(color=Color(0xCCFFFFFF),fontSize=13.sp)
					)
					//箭头
					Canvas(modifier=Modifier.size(14.dp,10.dp)){
						val p=Path().apply{
							moveTo(0f,size.height/2)
							lineTo(size.width*0.6f,size.height/2)
							moveTo(size.width*0.35f,0f)
							lineTo(size.width,size.height/2)
							lineTo(size.width*0.35f,size.height)
						}
						drawPath(p,Color(0xCCFFFFFF),style=Stroke(1.6.dp.toPx(),cap=StrokeCap.Round))
					}
				}
			}
		}
	}
}

//─────────────────────────────────────────────────────────────────────────────
//卡片 2：日记本
//深海蓝调+横线纹理+翻页折角+闪烁光点
//─────────────────────────────────────────────────────────────────────────────
@Composable fun DiaryCard(){
	val iftr=rememberInfiniteTransition(label="diary")
	val gp by iftr.animateFloat(
		initialValue=0.4f,targetValue=1f,animationSpec=infiniteRepeatable(
			animation=tween(1800,easing=FastOutSlowInEasing),repeatMode=RepeatMode.Reverse
		),label="glow"
	)
	val cb by iftr.animateFloat(
		initialValue=1f,targetValue=0f,animationSpec=infiniteRepeatable(
			animation=tween(600),repeatMode=RepeatMode.Reverse
		),label="cursor"
	)

	var pressed by remember{ mutableStateOf(false) }
	val scale by animateFloatAsState(
		targetValue=if(pressed) 0.955f else 1f,animationSpec=spring(dampingRatio=0.4f,stiffness=500f),label="diary_scale"
	)

	Box(
		modifier=Modifier.fillMaxWidth().height(160.dp).scale(scale).clip(CardShape).deepShadow(
			color=Color(0x552C5364),offsetY=14.dp,blur=28.dp,spread=(-4).dp
		).pointerInput(Unit){
			detectTapGestures(
				onPress={
					pressed=true;tryAwaitRelease()
					pressed=false;// todo ...
				}
			)
		}
	){
		Canvas(modifier=Modifier.matchParentSize()){
			//1.底层渐变
			drawRect(
				brush=Brush.linearGradient(
					colors=listOf(DiaryGradientTop,DiaryGradientMid,DiaryGradientBot),start=Offset(0f,0f),end=Offset(size.width,size.height)
				)
			)
			//2.横向日记线（4条，淡蓝）
			val lineStep=size.height/5f
			for(i in 1..4){
				val y=lineStep*i
				drawLine(
					color=DiaryLine,start=Offset(20.dp.toPx(),y),end=Offset(size.width-20.dp.toPx(),y),strokeWidth=0.8.dp.toPx()
				)
			}
			//3.左侧装订红线
			drawLine(
				color=Color(0x60E57373),start=Offset(48.dp.toPx(),0f),end=Offset(48.dp.toPx(),size.height),strokeWidth=1.2.dp.toPx()
			)
			//4.右下角折页（翻页感）
			val foldSize=36.dp.toPx()
			val foldPath=Path().apply{
				moveTo(size.width-foldSize,size.height)
				lineTo(size.width,size.height-foldSize)
				lineTo(size.width,size.height)
				close()
			}
			drawPath(
				path=foldPath,brush=Brush.linearGradient(
					colors=listOf(Color(0xFF0F2027),Color(0xFF1a3a4a)),start=Offset(size.width-foldSize,size.height-foldSize),end=Offset(size.width,size.height)
				)
			)
			//折页高光线
			drawLine(
				color=Color(0x557ECBF3),start=Offset(size.width-foldSize,size.height),end=Offset(size.width,size.height-foldSize),strokeWidth=1.dp.toPx()
			)
			//5.顶部光晕（脉冲）
			drawCircle(
				brush=Brush.radialGradient(
					colors=listOf(
						DiaryAccent.copy(alpha=0.18f*gp),Color.Transparent
					),center=Offset(size.width*0.8f,0f),radius=size.width*0.55f
				),radius=size.width*0.55f,center=Offset(size.width*0.8f,0f),blendMode=BlendMode.Screen
			)
			//6.蓝色描边
			drawRoundRect(
				brush=Brush.linearGradient(
					colors=listOf(
						Color(0x007ECBF3),Color(0x607ECBF3),Color(0x007ECBF3)
					)
				),size=size,cornerRadius=CornerRadius(8.dp.toPx()),style=Stroke(width=1.dp.toPx())
			)
		}
		//文字内容
		Column(
			modifier=Modifier.matchParentSize().padding(start=62.dp,end=24.dp,top=22.dp,bottom=20.dp),verticalArrangement=Arrangement.SpaceBetween
		){
			Row(
				verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)
			){
				//笔图标（Canvas 绘制）
				Canvas(modifier=Modifier.size(18.dp)){
					val cx=size.width/2;val cy=size.height/2
					//笔杆
					drawRoundRect(
						color=DiaryAccent,topLeft=Offset(cx-3.dp.toPx(),cy-7.dp.toPx()),size=Size(6.dp.toPx(),10.dp.toPx()),cornerRadius=CornerRadius(2.dp.toPx())
					)
					//笔尖
					val tip=Path().apply{
						moveTo(cx-3.dp.toPx(),cy+3.dp.toPx())
						lineTo(cx+3.dp.toPx(),cy+3.dp.toPx())
						lineTo(cx,cy+8.dp.toPx())
						close()
					}
					drawPath(tip,DiaryAccent)
				}
				androidx.compose.foundation.text.BasicText(
					text="我的日记",style=TextStyle(
						color=DiaryAccent,fontSize=12.sp,fontWeight=FontWeight.SemiBold,letterSpacing=2.sp
					)
				)
			}
			Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
				//模拟光标闪烁的"正在书写"状态
				Row(verticalAlignment=Alignment.CenterVertically){
					androidx.compose.foundation.text.BasicText(
						text="今天也要好好记录",style=TextStyle(
							color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold
						)
					)
					//光标
					Spacer(modifier=Modifier.width(2.dp))
					Box(
						modifier=Modifier.width(2.dp).height(22.dp).background(DiaryAccent.copy(alpha=cb))
					)
				}
				androidx.compose.foundation.text.BasicText(
					text="记录生活的每一刻",style=TextStyle(
						color=Color(0x99FFFFFF),fontSize=13.sp
					)
				)
			}
		}
	}
}

//─────────────────────────────────────────────────────────────────────────────
//工具：多层阴影（Modifier 扩展）
//─────────────────────────────────────────────────────────────────────────────
fun Modifier.deepShadow(
	color:Color=Color(0x44000000),
	offsetY:Dp=8.dp,blur:Dp=16.dp,
	spread:Dp=0.dp,shape:Shape=CardShape
):Modifier=this.drawBehind{
	val dp=offsetY.toPx()
	val sp=spread.toPx()
	val bp=blur.toPx()
	val r=8.dp.toPx()
	val steps=8
	for(i in steps downTo 1){
		val frac=i.toFloat()/steps
		val expand=sp+bp*frac
		val alpha=color.alpha*(1f-frac)*0.35f
		drawRoundRect(
			color=color.copy(alpha=alpha),
			topLeft=Offset(-expand,-expand+dp*frac),
			size=Size(size.width+expand*2,size.height+expand*2),
			cornerRadius=CornerRadius(r+expand)
		)
	}
}