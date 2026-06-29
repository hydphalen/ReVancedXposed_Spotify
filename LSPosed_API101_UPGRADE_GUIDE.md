# LSPosed API 101+ 与 Legacy Xposed 升级指南

## 📋 项目概述

本项目已经完成了从 **Xposed API v82 (Legacy)** 到 **LSPosed libxposed API 101+** 的升级。

**主要特性：**
- ✅ **LSPosed v2.1.0+ 支持** (API 101+)
- ✅ **不丢弃 Legacy Xposed 兼容性**
- ✅ **自动检测运行环境**
- ✅ **两组 API 同时编译**
- ✅ **根据检测流程自动上下文切换**

## 版本信息

| 特性 | 值 |
|--------|------|
| LSPosed 版本 | v2.1.0+ |
| LSPosed API | 101+ |
| Legacy Xposed | v82 |
| 编译 SDK | 34 |
| 最低 SDK | 27 |
| 优化目标 API | 34 |
| Kotlin 版本 | 2.3.20 |
| AGP 版本 | 9.2.0 |

## 🎯 API 对比

| 特性 | Legacy Xposed (v82) | LSPosed API 101+ |
|------|---|---|
| 隐藏 API 访问 | ❌ 受限 | ✅ HiddenApiBypass |
| 优先级模式 | ❌ 无 | ✅ 支持 |
| 性能 | 基础 | ✅ 高效 |
| 安全性 | 较弱 | ✅ 增强 |
| 体积 | 不明 | ✅ 更小 |

## 📝 主要改动

### 1. **依赖更新**

```toml
# gradle/libs.versions.toml
[versions]
xposed = "82"          # Legacy API
lsposed = "1.10.2"     # LSPosed API 101+

[libraries]
# 两个 API 都支持
xposed = { group = "de.robv.android.xposed", name = "api", version.ref = "xposed" }
lsposed = { group = "org.lsposed.lsposed", name = "lsposed-api", version.ref = "lsposed" }
```

### 2. **Maven 仓库配置**

```kotlin
// settings.gradle.kts
maven(url = "https://api.xposed.info")         // Legacy
maven(url = "https://maven.lsposed.org/releases") // LSPosed
```

### 3. **LSPosed 兼容层**

新文件 `MainHookLSPosed.kt` 提供：
- ✅ 运行时检测 LSPosed 环境
- ✅ 自动初始化隐藏 API bypass
- ✅ 桃月预伦的 fallback 机制

### 4. **日志改进**

```kotlin
// MainHook.kt 日志输出示例
"ReVanced-Xposed: 检测到 LSPosed 环境"
"ReVanced-Xposed: LSPosed (API 101+) 已启用"
// 或
"ReVanced-Xposed: 使用 Legacy Xposed API (v82)"
```

## 🚀 构建指南

### 方案 1: 使用 Gradle Wrapper

```bash
# 清理构建缓存
./gradlew clean

# 下载依赖
./gradlew dependencies

# 构建发版本
./gradlew build

# 或源子构建
./gradlew assembleRelease
```

### 方案 2: 使用 Android Studio

1. 打开项目
2. Build -> Make Project
3. Build -> Build Bundle(s) / APK(s)

## ✅ 测试阶段

### 在 LSPosed Manager 中测试：

1. **安装模块**
   - 将 APK 安装了辅助应用
   - 在 LSPosed Manager 中启用

2. **检查日志**
   ```bash
   logcat | grep "ReVanced-Xposed"
   ```

3. **需要看到的日志：**
   - ✅ `检测到 LSPosed 环境`
   - ✅ `LSPosed (API 101+) 已启用`
   - ✅ 模块功能正常作用

## 🔧 故障排除

### 问题 1: 编译失败

**错误：**
```
Cannot resolve symbol 'org.lsposed.hiddenapibypass'
```

**解决：**
- 检查 `settings.gradle.kts` 是否包含 LSPosed Maven 仓库
- 程源性清理：`./gradlew clean`

### 问题 2: 加载失败

**错误：**
```
Failed to load module
```

**解决：**
- 检查 Logcat 中的错误信息
- 验证你正在 LSPosed 上运行（不是 Legacy Xposed）
- 确保 Spotify 已安装

### 问题 3: 模块不执行

**错误：**
```
Module disabled or not loaded
```

**解决：**
- 在 LSPosed Manager 中手动启用
- 程源性防护转不正常 - 检查 ProGuard 配置
- 检查 `AndroidManifest.xml` 中的 LSPosed 元数据

## 📚 文档参考

- [LSPosed 官方文档](https://lsposed.org/)
- [libxposed API 参考](https://github.com/LSPosed/LSPosed/wiki/API)
- [Xposed Framework 文档](https://api.xposed.info/)
- [HiddenApiBypass](https://github.com/LSPosed/HiddenApiBypass)

## 📝 注意事项

1. **两个 API 的不同之处：**
   - LSPosed 优先（运行时检测）
   - Legacy Xposed 作为 fallback

2. **性能池：**
   - LSPosed 性能更好
   - 推荐用户升级到 LSPosed v2.1.0+

3. **流程控制：**
   ```
   启动
   ↓
   MainHook() 创建
   ↓
   检测 LSPosed 或 Legacy
   ↓
   回调对应 API
   ↓
   Hook 方法
   ```

## 🎯 分支信息

- **分支名：** `libxposed-api101-upgrade`
- **基础分支：** `main`
- **遗旋也：** 保持 legacy 版本在 `main`

## ✅ 下一步

1. 测试 LSPosed v2.1.0+ 环境
2. 验证 Legacy Xposed 兼换
3. 提交 Pull Request
4. 合并到 `main` 分支

---

**最后更新：** 2026-06-29
**维护者：** ReVanced Xposed FE Team
