package com.fyan

import androidx.compose.ui.*//导入UI组件
import androidx.compose.ui.unit.*//导入单位
import androidx.compose.ui.draw.*//导入绘制
import androidx.compose.ui.graphics.*//导入图形
import androidx.compose.foundation.*//导入基础扩展
import androidx.compose.foundation.shape.*//导入形状
import androidx.compose.foundation.layout.*//导入布局
import androidx.compose.runtime.*//导入状态处理
import androidx.compose.animation.*//导入动画库
import androidx.compose.ui.semantics.*//导入语义测试
import androidx.compose.ui.focus.*//导入焦点控制
import androidx.compose.ui.layout.*//导入布局标识
import android.graphics.Color as G//导入原生颜色

object Fya{
	//颜色解析：将"RRGGBB.透明度"格式字符串转为Compose Color，透明度可选
	fun cr(s:String):Color{
		val x=s.split(".")//分割颜色值与透明度
		val o=Color(G.parseColor("#"+x[0]))//解析基础颜色
		return if(x.size>1)o.copy(alpha=("0."+x[1]).toFloat())else o//返回混合透明度颜色
	}
	//形状解析：支持圆形(c)、矩形(r)、圆角矩形(单值/两值/四值)
	fun sh(s:String):Shape{
		if(s=="c")return CircleShape//正圆形
		if(s=="r")return RectangleShape//直角矩形
		val x=s.split(".")//分割多边角参数
		return when(x.size){//匹配参数数量
			1->RoundedCornerShape(x[0].toInt().dp)//统一四角圆角
			2->RoundedCornerShape(x[0].toInt().dp,x[0].toInt().dp,x[1].toInt().dp,x[1].toInt().dp)//上下对称圆角
			else->RoundedCornerShape(x[0].toInt().dp,x[1].toInt().dp,x[2].toInt().dp,x[3].toInt().dp)//四角独立圆角
		}
	}
	val xc=HashMap<String,Modifier>()//静态常驻缓存池，避免重复构建相同修饰符链
	@Composable//支持remember生成内部状态，但此处仅作标记
	fun mc(s:String):Modifier{//核心样式解析函数，将字符串指令转为Modifier链
		xc[s]?.let{return it}//命中缓存直接返回实例
		var m:Modifier=Modifier//初始化基础修饰符链
		val a=s.split(" ")//分割全量指令集，每条指令以空格分隔
		for(t in a){//遍历执行每条指令
			val i=t.indexOf(":")//预定位冒号分割位，用于分离指令前缀与参数
			val p=if(i>0)t.substring(0,i)else t//提取前缀指令标识
			val v=if(i>0)t.substring(i+1)else ""//提取指令附加参数值
			m=when(p){//按指令类型进行极速路由映射
				"⚄"->m.weight(if(v=="")0F else v.toFloat())//权重占比扩展，需在Row或ColumnScope内使用
				"⧓"->if(v.startsWith(">"))m.widthIn(min=v.substring(1).toInt().dp)else if(v.startsWith("<"))m.widthIn(max=v.substring(1).toInt().dp)else m.width(v.toInt().dp)//宽度及极大极小值约束
				"⧗"->if(v.startsWith(">"))m.heightIn(min=v.substring(1).toInt().dp)else if(v.startsWith("<"))m.heightIn(max=v.substring(1).toInt().dp)else m.height(v.toInt().dp)//高度及极大极小值约束
				"⧆"->v.split(".").let{if(it.size>1)m.size(it[0].toInt().dp,it[1].toInt().dp)else m.size(it[0].toInt().dp)}//统一正方尺寸或宽高独立尺寸
				"⧈"->if(v=="o")m.systemBarsPadding()else if(v=="n")m.navigationBarsPadding()else if(v=="i")m.imePadding()else if(v=="s")m.statusBarsPadding()else v.split(".").let{
					when(it.size){//利用换行替代分号，根据不同参数数量决定边距策略
						1->m.padding(it[0].toInt().dp)//四边统一边距
						2->m.padding(vertical=it[0].toInt().dp,horizontal=it[1].toInt().dp)//上下与左右独立边距
						3->m.padding(top=it[0].toInt().dp,start=it[1].toInt().dp,end=it[1].toInt().dp,bottom=it[2].toInt().dp)//顶部底部独立，左右统一
						else->m.padding(start=it[3].toInt().dp,top=it[0].toInt().dp,end=it[1].toInt().dp,bottom=it[2].toInt().dp)//四边完全独立定制边距
					}
				}
				"⬤"->m.background(cr(v))//背景纯色或透明混合色填充
				"⦼"->if(v=="x")m.clipToBounds()else m.clip(sh(v))//超出边界硬裁剪或按形状遮罩裁剪
				"☪"->m.alpha(v.toFloat())//视图整体不透明度控制
				"☐"->v.split(":").let{if(it.size>2)m.border(it[0].toInt().dp,cr(it[1]),sh(it[2]))else m.border(it[0].toInt().dp,cr(it[1]))}//描边粗细与色彩及可选形状约束
				"♾"->v.split(":").let{if(it.size>1)m.shadow(it[0].toInt().dp,spotColor=cr(it[1]),ambientColor=cr(it[1]))else m.shadow(it[0].toInt().dp)}//Z轴阴影高度及双重光源色彩
				"⌬"->m.rotate(v.toFloat())//Z轴中心点几何旋转角度
				"⏣"->v.split(":").let{if(it.size>1)m.scale(it[0].toFloat(),it[1].toFloat())else m.scale(it[0].toFloat())}//整体等比缩放或XY轴形变缩放
				"⇲"->v.split(":").let{m.offset(it[0].toInt().dp,it[1].toInt().dp)}//坐标系相对偏移修改
				"⍐"->m.align(Alignment.TopCenter)//顶部居中对齐
				"⍗"->m.align(Alignment.BottomCenter)//底部居中对齐
				"⍇"->m.align(Alignment.CenterStart)//居中靠左对齐
				"⍈"->m.align(Alignment.CenterEnd)//居中靠右对齐
				"⍄"->m.horizontalScroll(rememberScrollState())//水平滚动状态拦截
				"⍌"->m.verticalScroll(rememberScrollState())//垂直滚动状态拦截
				"⍯"->m.aspectRatio(v.toFloat())//强制宽高比约束
				"⌻"->m.zIndex(v.toFloat())//Z轴层级穿透修改
				"⌾"->if(v=="1")m.focusable(true)else m.focusable(false)//焦点捕获与响应能力控制
				"⍟"->m.animateContentSize()//组件内容尺寸变化时自动产生过渡动画
				"⍰"->if(v=="w")m.wrapContentWidth()else if(v=="h")m.wrapContentHeight()else m.wrapContentSize()//解除强制铺满以包裹内容为主
				"⍹"->m.fillMaxWidth(if(v.length>0)("0."+v).toFloat()else 1f)//X轴宽占比满级或部分填充
				"⌽"->m.fillMaxSize(if(v.length>0)("0."+v).toFloat()else 1f)//XY轴全屏宽铺满或部分占比填充
				"⍬"->m.fillMaxHeight(if(v.length>0)("0."+v).toFloat()else 1f)//Y轴高占比满级或部分填充
				"☮"->m.clickable{}//注册点击事件拦截水波纹占位
				else->m//自动抛弃无法解析的无效指令修饰符
			}
		}
		xc[s]=m//复合参数解析完毕写入缓存池
		return m//弹出构建好的修饰链实例
	}
}