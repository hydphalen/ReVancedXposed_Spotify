package io.github.chsbuffer.revancedxposed

import android.util.Log

/**
 * LSPosed 兼容层 (API 101+)
 * 用于 LSPosed v2.1.0 及以上版本
 * 提供环境检测和初始化功能
 */
object MainHookLSPosed {
    private const val TAG = "ReVanced-LSPosed"
    
    /**
     * 检测当前运行环境是否为 LSPosed
     * @return true 如果运行在 LSPosed 环境中
     */
    fun isRunningOnLSPosed(): Boolean {
        return try {
            // 尝试加载 LSPosed 特有的类
            Class.forName("org.lsposed.hiddenapibypass.HiddenApiBypass")
            Log.i(TAG, "检测到 LSPosed 环境")
            true
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "未检测到 LSPosed 环境，使用 Legacy Xposed API")
            false
        } catch (e: Exception) {
            Log.e(TAG, "LSPosed 检测失败: ${e.message}")
            false
        }
    }
    
    /**
     * 初始化 LSPosed 环境
     * 解锁隐藏 API 访问权限
     */
    fun initLSPosed() {
        try {
            if (!isRunningOnLSPosed()) {
                Log.d(TAG, "当前环境不是 LSPosed，跳过初始化")
                return
            }
            
            // 使用反射调用 HiddenApiBypass.addHiddenApiExemptions
            val hiddenApiBypassClass = Class.forName("org.lsposed.hiddenapibypass.HiddenApiBypass")
            val addExemptionsMethod = hiddenApiBypassClass.getMethod("addHiddenApiExemptions", String::class.java)
            addExemptionsMethod.invoke(null, "L") // "L" 表示全部
            
            Log.i(TAG, "LSPosed 环境初始化成功 (API 101+)")
        } catch (e: Exception) {
            Log.e(TAG, "LSPosed 初始化失败: ${e.message}", e)
        }
    }
    
    /**
     * 获取当前 Xposed 框架版本
     * @return 版本字符串 (LSPosed 或 Legacy)
     */
    fun getXposedFrameworkType(): String {
        return if (isRunningOnLSPosed()) {
            "LSPosed (API 101+)"
        } else {
            "Legacy Xposed (API 82)"
        }
    }
}
