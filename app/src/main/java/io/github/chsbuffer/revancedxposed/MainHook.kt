package io.github.chsbuffer.revancedxposed

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import app.revanced.extension.shared.Utils
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.revancedxposed.spotify.AdBlockHook
import io.github.chsbuffer.revancedxposed.spotify.RoundyUIHook
import io.github.chsbuffer.revancedxposed.spotify.SettingsSheet
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook
import io.github.chsbuffer.revancedxposed.spotify.ThemeHook
import androidx.core.view.isNotEmpty

/**
 * 主 Hook 类 - 支持 LSPosed (API 101+) 和 Legacy Xposed
 * 
 * 该类实现了两个 Xposed 接口以确保广泛的兼容性：
 * - IXposedHookLoadPackage: 在应用加载时的钩子
 * - IXposedHookZygoteInit: Zygote 进程初始化钩子
 */
class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        private const val TAG = "ReVanced-Xposed"
    }

    lateinit var startupParam: StartupParam
    lateinit var lpparam: LoadPackageParam
    lateinit var app: Application
    var targetPackageName: String? = null
    
    val hooksByPackage = mapOf(
        "com.spotify.music" to { SpotifyHook(app, lpparam) },
    )

    init {
        // 在初始化时检测并初始化 LSPosed 环境
        try {
            if (MainHookLSPosed.isRunningOnLSPosed()) {
                Log.i(TAG, "检测到 LSPosed 环境，初始化 LSPosed 兼容层")
                MainHookLSPosed.initLSPosed()
                XposedBridge.log("$TAG: LSPosed (API 101+) 已启用")
            } else {
                XposedBridge.log("$TAG: 使用 Legacy Xposed API (v82)")
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG: 初始化时出错: ${e.message}")
        }
    }

    fun shouldHook(packageName: String): Boolean {
        if (!hooksByPackage.containsKey(packageName)) return false
        if (targetPackageName == null) targetPackageName = packageName
        return targetPackageName == packageName
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (!lpparam.isFirstApplication) return
        if (!shouldHook(lpparam.packageName)) return
        this.lpparam = lpparam

        // --- 长按个人资料图标触发器 ---
        XposedHelpers.findAndHookMethod(
            "android.app.Activity",
            lpparam.classLoader,
            "onPostCreate",
            android.os.Bundle::class.java,
            object : XC_MethodHook() {
                @SuppressLint("DiscouragedApi")
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    if (!activity.javaClass.name.contains("MainActivity")) return

                    // Spotify 以异步方式加载头像，等待视图布局完成
                    val decorView = activity.window.decorView as ViewGroup
                    decorView.viewTreeObserver.addOnGlobalLayoutListener {
                        // 尝试通过常见 ID 查找头像
                        val avatarIds = listOf(
                            "profile_button", "profile_image", "avatar",
                            "user_avatar", "faceview", "faceheader_image"
                        )
                        var found = false

                        for (idName in avatarIds) {
                            val resId = activity.resources.getIdentifier(idName, "id", activity.packageName)
                            if (resId != 0) {
                                val avatarView = activity.findViewById<View>(resId)
                                if (avatarView != null && !found) {
                                    setModLongClickListener(avatarView, activity)
                                    found = true
                                }
                            }
                        }

                        // 如果未找到，递归搜索左上角的 ImageView
                        if (!found) {
                            findAvatarRecursive(decorView, activity)
                        }
                    }
                }
            }
        )

        inContext(lpparam) { app ->
            this.app = app

            // 加载偏好设置
            val prefs = app.getSharedPreferences("spotify_prefs", 0)

            if (isReVancedPatched(lpparam)) {
                Utils.showToastLong("ReVanced Xposed FE 模块与已修补的应用不兼容")
                return@inContext
            }
            
            val frameworkType = MainHookLSPosed.getXposedFrameworkType()
            Utils.showToastLong("ReVanced Xposed FE 初始化中... ($frameworkType)")

            // --- 高级功能模块 ---
            try {
                if (prefs.getBoolean("enable_premium", true)) {
                    hooksByPackage[lpparam.packageName]?.invoke()?.Hook()
                    Log.d(TAG, "高级功能已启用")
                }
            } catch (e: Exception) {
                XposedBridge.log("$TAG: 高级功能失败 - ${e.message}")
            }

            // --- 广告拦截模块 ---
            try {
                if (prefs.getBoolean("enable_adblock", true)) {
                    AdBlockHook(lpparam).hook()
                    XposedBridge.log("$TAG: 广告拦截已启用")
                }
            } catch (e: Exception) {
                XposedBridge.log("$TAG: 广告拦截失败 - ${e.message}")
            }

            // --- Monet 主题模块 ---
            try {
                if (prefs.getBoolean("enable_monet", true)) {
                    ThemeHook(app, lpparam).hook()
                    Log.d(TAG, "Monet 主题已启用")
                }
            } catch (e: Exception) {
                XposedBridge.log("$TAG: Monet 主题失败 - ${e.message}")
            }

            // --- 圆角 UI 模块 ---
            try {
                if (prefs.getBoolean("enable_round_ui", true)) {
                    RoundyUIHook(lpparam).hook()
                    Log.d(TAG, "圆角 UI 已启用")
                }
            } catch (e: Exception) {
                XposedBridge.log("$TAG: 圆角 UI 失败 - ${e.message}")
            }
        }
    }

    private fun setModLongClickListener(view: View, activity: Activity) {
        if (view.tag == "mod_hooked") return
        view.tag = "mod_hooked"

        view.setOnLongClickListener {
            val realView = if (it is ViewGroup && it.isNotEmpty()) {
                it.getChildAt(0)
            } else {
                it
            }

            it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            SettingsSheet.show(activity, realView)
            true
        }
    }

    private fun findAvatarRecursive(view: View, activity: Activity) {
        if (view is ImageView || view.contentDescription?.toString()?.contains("Profilo", true) == true) {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            // 头像通常在顶部 150px 和左侧 150px 内
            if (location[0] < 150 && location[1] < 200 && view.width > 0) {
                setModLongClickListener(view, activity)
                return
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findAvatarRecursive(view.getChildAt(i), activity)
            }
        }
    }

    private fun isReVancedPatched(lpparam: LoadPackageParam): Boolean {
        return runCatching {
            lpparam.classLoader.loadClass("app.revanced.extension.shared.Utils")
        }.isSuccess || runCatching {
            lpparam.classLoader.loadClass("app.revanced.extension.shared.utils.Utils")
        }.isSuccess || runCatching {
            lpparam.classLoader.loadClass("app.revanced.integrations.shared.Utils")
        }.isSuccess || runCatching {
            lpparam.classLoader.loadClass("app.revanced.integrations.shared.utils.Utils")
        }.isSuccess
    }

    override fun initZygote(startupParam: StartupParam) {
        this.startupParam = startupParam
        XposedInit = startupParam
    }
}

/**
 * 应用上下文辅助函数
 * 在应用的 onCreate 时获取 Application 对象
 */
fun inContext(lpparam: LoadPackageParam, f: (Application) -> Unit) {
    val appClazz = XposedHelpers.findClass(lpparam.appInfo.className, lpparam.classLoader)
    XposedBridge.hookMethod(appClazz.getMethod("onCreate"), object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val app = param.thisObject as Application
            Utils.setContext(app)
            f(app)
        }
    })
}
