// 顶层构建脚本：负责声明全局插件和对应版本
plugins {
    // 谷歌官方安卓应用构建核心插件 (AGP)
    id("com.android.application") version "8.4.1" apply false
    id("com.android.library") version "8.4.1" apply false
    
    // Kotlin 语言编译插件
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    
    // Jetpack Compose 最新的编译器插件（Kotlin 2.0+ 之后集成于此）
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}
