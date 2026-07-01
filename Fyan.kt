package com.fyan

import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.layout.*
import android.graphics.Color as G

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
	@Composable
	fun mc(s:String):Modifier{//核心样式解析函数
		xc[s]?.let{return it}//命中缓存直接返回
		var m:Modifier=Modifier//初始化修饰符
		val a=s.split(" ")//分割指令集
		for(t in a){//遍历指令
			val i=t.indexOf(":")//预定位冒号
			val p=if(i>0)t.substring(0,i)else t//提取前缀
			val v=if(i>0)t.substring(i+1)else ""//提取参数
			m=when(p){//指令路由映射
				"⚄"->m.then(layout{m,c->layout(m.minIntrinsicWidth(c.maxHeight),m.maxIntrinsicHeight(c.maxHeight)){m.layout(0,0).place(0,0)}}.weight(if(v=="")0f else v.toFloat()))//权重占比扩展
				"⧓"->if(v.startsWith(">"))m.widthIn(min=v.substring(1).toInt().dp)else if(v.startsWith("<"))m.widthIn(max=v.substring(1).toInt().dp)else m.width(v.toInt().dp)//宽度约束
				"⧗"->if(v.startsWith(">"))m.heightIn(min=v.substring(1).toInt().dp)else if(v.startsWith("<"))m.heightIn(max=v.substring(1).toInt().dp)else m.height(v.toInt().dp)//高度约束
				"⧆"->v.split(".").let{if(it.size>1)m.size(it[0].toInt().dp,it[1].toInt().dp)else m.size(it[0].toInt().dp)}//尺寸大小
				"⧈"->if(v=="o")m.systemBarsPadding()else if(v=="n")m.navigationBarsPadding()else if(v=="i")m.imePadding()else if(v=="s")m.statusBarsPadding()else v.split(".").let{
					when(it.size){//边距策略
						1->m.padding(it[0].toInt().dp)//统一内边距
						2->m.padding(vertical=it[0].toInt().dp,horizontal=it[1].toInt().dp)//垂直与水平边距
						3->m.padding(top=it[0].toInt().dp,start=it[1].toInt().dp,end=it[1].toInt().dp,bottom=it[2].toInt().dp)//三边距
						else->m.padding(start=it[3].toInt().dp,top=it[0].toInt().dp,end=it[1].toInt().dp,bottom=it[2].toInt().dp)//四边距
					}
				}
				"⬤"->m.background(cr(v))//背景填充
				"⦼"->if(v=="x")m.clipToBounds()else m.clip(sh(v))//形状裁剪
				"☪"->m.alpha(v.toFloat())//透明度设置
				"☐"->v.split(":").let{if(it.size>2)m.border(it[0].toInt().dp,cr(it[1]),sh(it[2]))else m.border(it[0].toInt().dp,cr(it[1]))}//边框绘制
				"♾"->v.split(":").let{if(it.size>1)m.shadow(it[0].toInt().dp,spotColor=cr(it[1]),ambientColor=cr(it[1]))else m.shadow(it[0].toInt().dp)}//阴影绘制
				"⌬"->m.rotate(v.toFloat())//旋转角度
				"⏣"->v.split(":").let{if(it.size>1)m.scale(it[0].toFloat(),it[1].toFloat())else m.scale(it[0].toFloat())}//缩放比例
				"⇲"->v.split(":").let{m.offset(it[0].toInt().dp,it[1].toInt().dp)}//坐标偏移
				"⍐"->m.then(Modifier.wrapContentSize(Alignment.TopCenter))//顶部居中对齐
				"⍗"->m.then(Modifier.wrapContentSize(Alignment.BottomCenter))//底部居中对齐
				"⍇"->m.then(Modifier.wrapContentSize(Alignment.CenterStart))//居中左对齐
				"⍈"->m.then(Modifier.wrapContentSize(Alignment.CenterEnd))//居中右对齐
				"⍄"->m.horizontalScroll(rememberScrollState())//水平滚动
				"⍌"->m.verticalScroll(rememberScrollState())//垂直滚动
				"⍯"->m.aspectRatio(v.toFloat())//宽高比约束
				"⌻"->m.zIndex(v.toFloat())//Z轴层级
				"⌾"->if(v=="1")m.focusable(true)else m.focusable(false)//焦点响应
				"⍟"->m.animateContentSize()//尺寸过渡动画
				"⍰"->if(v=="w")m.wrapContentWidth()else if(v=="h")m.wrapContentHeight()else m.wrapContentSize()//内容包裹
				"⍹"->m.fillMaxWidth(if(v.length>0)("0."+v).toFloat()else 1f)//宽度铺满
				"⌽"->m.fillMaxSize(if(v.length>0)("0."+v).toFloat()else 1f)//全屏铺满
				"⍬"->m.fillMaxHeight(if(v.length>0)("0."+v).toFloat()else 1f)//高度铺满
				"☮"->m.clickable{}//点击事件占位
				else->m//无效指令忽略
			}
		}
		xc[s]=m//存入缓存
		return m//返回修饰链
	}
}
