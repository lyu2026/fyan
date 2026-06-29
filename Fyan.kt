package com.fyan

import androidx.compose.ui.* // 导入UI组件
import androidx.compose.ui.unit.* // 导入单位
import androidx.compose.ui.draw.* // 导入绘制
import androidx.compose.ui.graphics.* // 导入图形
import androidx.compose.foundation.* // 导入基础扩展
import androidx.compose.foundation.shape.* // 导入形状
import androidx.compose.foundation.layout.* // 导入布局
import android.graphics.Color as G // 导入原生颜色

object Fya{

	fun cr(s:String):Color{ // 解析颜色
		val x=s.split(".") // 分割颜色透明度
		val o=Color(G.parseColor("#"+x[0])) // 解析颜色
		return if(x.size>1)o.copy(alpha=("0."+x[1]).toFloat())else o
	}
	fun sh(s:String):Shape{ // 解析形状
		if(s=="c")return CircleShape // 圆形
		val x=s.split(".") // 分割四角
		return when(x.size){
			1->RoundedCornerShape(x[0].toInt().dp) // 统一
			2->RoundedCornerShape(x[0].toInt().dp,x[0].toInt().dp,x[1].toInt().dp,x[1].toInt().dp) // 上下对称
			else->RoundedCornerShape(x[0].toInt().dp,x[1].toInt().dp,x[2].toInt().dp,x[3].toInt().dp) // 四角
		}
	}

	val xc=HashMap<String,Modifier>() // 样式缓存
	fun m(s:String):Modifier{
		xc[s]?.let{return it} // 命中缓存
		var m=Modifier // 初始化
		val a=s.split(" ") // 分割指令
		for(t in a){ // 遍历指令
			if(t.startsWith("fw"))m=m.fillMaxWidth(if(t.length>2)("0"+t.substring(2)).toFloat()else 1f) // 宽占比
			else if(t.startsWith("fs"))m=m.fillMaxSize(if(t.length>2)("0"+t.substring(2)).toFloat()else 1f) // 全占比
			else if(t.startsWith("fh"))m=m.fillMaxHeight(if(t.length>2)("0"+t.substring(2)).toFloat()else 1f) // 高占比
			else if(t.startsWith("g:"))m=m.weight(t.substring(3).toFloat()) // 权重
			else if(t.startsWith("w:")){ // 宽度相关
				val v=t.substring(2)
				m=if(v.startsWith(">"))m.widthIn(min=v.substring(1).toInt().dp)
				else if(v.startsWith("<"))m.widthIn(max=v.substring(1).toInt().dp)
				else m.width(v.toInt().dp)
			}
			else if(t.startsWith("h:")){ // 高度相关
				val v=t.substring(2)
				m=if(v.startsWith(">"))m.heightIn(min=v.substring(1).toInt().dp)
				else if(v.startsWith("<"))m.heightIn(max=v.substring(1).toInt().dp)
				else m.height(v.toInt().dp)
			}
			else if(t.startsWith("p:")){ // 边距相关
				val v=t.substring(2)
				if(v=="ss")m=m.systemBarsPadding()
				else if(v=="nv")m=m.navigationBarsPadding()
				else{
					val x=v.split(".")
					m=when(x.size){
						1->m.padding(x[0].toInt().dp)
						2->m.padding(vertical=x[0].toInt().dp,horizontal=x[1].toInt().dp)
						else->m.padding(start=x[3].toInt().dp,top=x[0].toInt().dp,end=x[1].toInt().dp,bottom=x[2].toInt().dp)
					}
				}
			}
			else if(t.startsWith("ph:"))m=m.padding(horizontal=t.substring(3).toInt().dp) // 水平边距
			else if(t.startsWith("pv:"))m=m.padding(vertical=t.substring(3).toInt().dp) // 垂直边距
			else if(t.startsWith("pt:"))m=m.padding(top=t.substring(3).toInt().dp) // 上边距
			else if(t.startsWith("pb:"))m=m.padding(bottom=t.substring(3).toInt().dp) // 下边距
			else if(t.startsWith("ps:"))m=m.padding(start=t.substring(3).toInt().dp) // 开始边距
			else if(t.startsWith("pe:"))m=m.padding(end=t.substring(3).toInt().dp) // 结束边距
			else if(t.startsWith("bg:"))m=m.background(cr(t.substring(3))) // 背景
			else if(t.startsWith("c:")){ // 裁剪
				val v=t.substring(2)
				m=if(v=="x")m.clipToBounds()else m.clip(sh(v))
			}
			else if(t.startsWith("op:"))m=m.alpha(t.substring(3).toFloat()) // 透明度
			else if(t.startsWith("bd:")){ // 边框
				val x=t.substring(3).split(":")
				m=if(x.size>2)m.border(x[0].toInt().dp,cr(x[1]),sh(x[2]))else m.border(x[0].toInt().dp,cr(x[1]))
			}
			else if(t.startsWith("sd:")){ // 阴影
				val x=t.substring(3).split(":")
				m=if(x.size>1)m.shadow(x[0].toInt().dp,spotColor=cr(x[1]),ambientColor=cr(x[1]))else m.shadow(x[0].toInt().dp)
			}
			// 旋转
			else if(t.startsWith("ro:"))m=m.rotate(t.substring(3).toFloat())
			// 缩放
			else if(t.startsWith("sc:"))m=m.scale(t.substring(3).toFloat())
			else if(t.startsWith("xy:")){ // 偏移
				val x=t.substring(3).split(":")
				m=m.offset(x[0].toInt().dp,x[1].toInt().dp)
			}
			else if(t=="cc")m=m.clickable{} // 点击
			else if(t.startsWith("ag:")){ // 对齐
				val v=t.substring(3)
				m=m.align(when(v){
					"c"->Alignment.Center
					"t"->Alignment.Top
					"b"->Alignment.Bottom
					"l"->Alignment.Start
					"r"->Alignment.End
					else->Alignment.Center
				})
			}
			// 滚动
			else if(t.startsWith("sr:"))m=if(t.substring(3)=="v")m.verticalScroll(rememberScrollState())else m.horizontalScroll(rememberScrollState())
			// 宽高比
			else if(t.startsWith("ar:"))m=m.aspectRatio(t.substring(3).toFloat())
			// 层级
			else if(t.startsWith("zx:"))m=m.zIndex(t.substring(3).toFloat())
		}
		xc[s]=m // 缓存
		return m
	}
}
