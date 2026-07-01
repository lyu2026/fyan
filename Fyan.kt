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
	val xc=HashMap<String,Modifier>()//静态常驻缓存池
	//基础通用修饰符解析：适用于Box或不含特殊Scope的Modifier构建
	fun Modifier.mc(s:String):Modifier{
		xc[s]?.let{return it}//命中缓存直接返回
		var m=this//初始化修饰符链
		val a=s.split(" ")//分割指令集
		for(t in a){//遍历执行指令
			val i=t.indexOf(":")//寻找分隔符
			val p=if(i>0)t.substring(0,i)else t//指令前缀
			val v=if(i>0)t.substring(i+1)else ""//指令参数
			m=when(p){//根据指令路由映射
				"⧓"->if(v.startsWith(">"))m.widthIn(min=v.substring(1).toInt().dp)else if(v.startsWith("<"))m.widthIn(max=v.substring(1).toInt().dp)else m.width(v.toInt().dp)//宽度约束
				"⧗"->if(v.startsWith(">"))m.heightIn(min=v.substring(1).toInt().dp)else if(v.startsWith("<"))m.heightIn(max=v.substring(1).toInt().dp)else m.height(v.toInt().dp)//高度约束
				"⧆"->v.split(".").let{if(it.size>1)m.size(it[0].toInt().dp,it[1].toInt().dp)else m.size(it[0].toInt().dp)}//固定尺寸
				"⧈"->if(v=="o")m.systemBarsPadding()else if(v=="n")m.navigationBarsPadding()else if(v=="i")m.imePadding()else if(v=="s")m.statusBarsPadding()else v.split(".").let{
					when(it.size){//边距策略
						1->m.padding(it[0].toInt().dp)//四边统一
						2->m.padding(vertical=it[0].toInt().dp,horizontal=it[1].toInt().dp)//垂直水平
						3->m.padding(top=it[0].toInt().dp,start=it[1].toInt().dp,end=it[1].toInt().dp,bottom=it[2].toInt().dp)//复杂边距
						else->m.padding(start=it[3].toInt().dp,top=it[0].toInt().dp,end=it[1].toInt().dp,bottom=it[2].toInt().dp)//全独立
					}
				}
				"⬤"->m.background(cr(v))//背景色
				"⦼"->if(v=="x")m.clipToBounds()else m.clip(sh(v))//裁剪形状
				"☪"->m.alpha(v.toFloat())//不透明度
				"☐"->v.split(":").let{if(it.size>2)m.border(it[0].toInt().dp,cr(it[1]),sh(it[2]))else m.border(it[0].toInt().dp,cr(it[1]))}//边框
				"♾"->v.split(":").let{if(it.size>1)m.shadow(it[0].toInt().dp,spotColor=cr(it[1]),ambientColor=cr(it[1]))else m.shadow(it[0].toInt().dp)}//阴影
				"⌬"->m.rotate(v.toFloat())//旋转
				"⏣"->v.split(":").let{if(it.size>1)m.scale(it[0].toFloat(),it[1].toFloat())else m.scale(it[0].toFloat())}//缩放
				"⇲"->v.split(":").let{m.offset(it[0].toInt().dp,it[1].toInt().dp)}//偏移
				"⍄"->m.horizontalScroll(rememberScrollState())//横向滚动
				"⍌"->m.verticalScroll(rememberScrollState())//纵向滚动
				"⍯"->m.aspectRatio(v.toFloat())//宽高比
				"⌻"->m.zIndex(v.toFloat())//层级
				"⌾"->if(v=="1")m.focusable(true)else m.focusable(false)//焦点
				"⍟"->m.animateContentSize()//尺寸动画
				"⍰"->if(v=="w")m.wrapContentWidth()else if(v=="h")m.wrapContentHeight()else m.wrapContentSize()//内容包裹
				"⍹"->m.fillMaxWidth(if(v.length>0)("0."+v).toFloat()else 1f)//宽度铺满
				"⌽"->m.fillMaxSize(if(v.length>0)("0."+v).toFloat()else 1f)//大小铺满
				"⍬"->m.fillMaxHeight(if(v.length>0)("0."+v).toFloat()else 1f)//高度铺满
				"☮"->m.clickable{}//点击事件
				else->m//无效指令
			}
		}
		xc[s]=m//存入缓存
		return m//返回链
	}

	//RowScope作用域扩展：支持权重指令(⚄)
	fun RowScope.mc(s:String):Modifier=Modifier.mc(s).let{
		if(s.contains("⚄"))it.weight(s.split("⚄:")[1].split(" ")[0].toFloat())else it
	}
	//ColumnScope作用域扩展：支持权重指令(⚄)
	fun ColumnScope.mc(s:String):Modifier=Modifier.mc(s).let{
		if(s.contains("⚄"))it.weight(s.split("⚄:")[1].split(" ")[0].toFloat())else it
	}
	//BoxScope作用域扩展：支持对齐指令(⍐/⍗/⍇/⍈)
	fun BoxScope.mc(s:String):Modifier=Modifier.mc(s).let{
		when{
			s.contains("⍐")->it.align(Alignment.TopCenter)//顶部对齐
			s.contains("⍗")->it.align(Alignment.BottomCenter)//底部对齐
			s.contains("⍇")->it.align(Alignment.CenterStart)//左侧对齐
			s.contains("⍈")->it.align(Alignment.CenterEnd)//右侧对齐
			else->it//默认无对齐
		}
	}
}
