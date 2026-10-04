# 🕊️ Geopelia_Chat — 感谢你的陪伴 愿我们再次相遇
<p align="center">
  <a href="https://github.com/Geopelia-Geo/Geopelia_Chat"><img alt="GitHub Repo" src="https://img.shields.io/badge/GitHub-Geopelia__Chat-ff69b4?logo=github"></a>
  <a href="https://github.com/Geopelia-Geo/Geopelia_Chat/blob/main/LICENSE"><img alt="License" src="https://img.shields.io/github/license/Geopelia-Geo/Geopelia_Chat?color=blue"></a>
  <a href="https://github.com/Geopelia-Geo/Geopelia_Chat/stargazers"><img alt="Stars" src="https://img.shields.io/github/stars/Geopelia-Geo/Geopelia_Chat?color=yellow"></a>
  <a href="https://github.com/Geopelia-Geo/Geopelia_Chat/network/members"><img alt="Forks" src="https://img.shields.io/github/forks/Geopelia-Geo/Geopelia_Chat?color=orange"></a>
  <a href="https://github.com/Geopelia-Geo/Geopelia_Chat/releases"><img alt="Release" src="https://img.shields.io/github/v/release/Geopelia-Geo/Geopelia_Chat?color=green"></a>
  <a href="https://github.com/Geopelia-Geo/Geopelia_Chat"><img alt="Last Commit" src="https://img.shields.io/github/last-commit/Geopelia-Geo/Geopelia_Chat?color=green"></a>
  <a href="https://developer.android.com/"><img alt="Android" src="https://img.shields.io/badge/Android-5.0+-3DDC84?logo=android&logoColor=white"></a>
  <a href="https://www.java.com/"><img alt="Java" src="https://img.shields.io/badge/Java-Pure-007396?logo=openjdk&logoColor=white"></a>
</p>
> **⚠️ 免责声明：本项目仅供学习交流使用，请勿用于商业用途。禁止倒卖！**

玩完 Phigros 大结局之后，感动得不行。
满脑子都是那个画面，睡不着，就想做点什么留下来。
于是就有了 Geopelia_Chat。

一个 AI 聊天应用。没什么宏大的目标，就是做完大结局之后想做点东西留个纪念。
咕咕咕。

---

## ✨ 特性

- ⚡ **最低支持 Android 5.0（API 21）** — 无原生库、纯 Java，32 位 / 64 位设备都能跑
- 🔌 **任意 OpenAI 兼容接口** — 可填写任意 Base URL + API Key + 模型名
- 🐋 **默认 DeepSeek** — 接口地址 `https://api.deepseek.com/v1`、模型 `deepseek-chat`、温度 1.0
- 💭 **Geopelia-Geo 特殊优化的系统提示词** — 调教过的喵
- 📦 **原生打包，无 WebView 要求**
- 🏠 **返回键回主屏幕** — 顶栏 ‹ 一键回到系统桌面
- 🖼️ **头像系统** — AI 头像用内置图；自己的头像可在设置页自定义；连续同一个人发的多条消息只显示一次头像
- 💬 **微信式多行发送** — 输入框内多行文本按行拆成多条独立消息逐条发送，间隔随机 0.3~0.7 秒
- 🎨 **自定义聊天背景图** — 设置页可从相册选图，自动采样压缩，可一键恢复默认
- 💾 **聊天记录记忆** — 自动持久化保存，重启 App 不丢失
- 🧠 **上下文条数可调** — 发送时附带最近 N 条历史，默认 30 条，设置里可改（0~200）
- ↩️ **撤回 / 回溯 / 删除** — 长按任意消息弹出菜单
  - 复制内容
  - 删除此条
  - 撤回此条及回复（仅用户消息，同时删掉对应的 AI 回答）
  - 回溯到此处（删除该消息之后的全部记录，从该位置继续对话）
- 🧹 **清空聊天记录** — 在「设置」页
- 🪶 **零第三方网络 SDK** — 仅用 Android 自带的 HttpURLConnection + org.json，体积小
- ✅ **已签名** — 可直接安装分发（release 版）

---

## 📱 快速开始

1. 去 [Releases](../../releases) 页面下载最新的 APK
2. 安装到安卓手机上（Android 5.0 及以上）
3. 打开就能用啦

> 源码在仓库里，想自己编译的可以自己 build。

### 首次使用

1. 打开 App，点顶栏 ☰ 打开设置
2. 只需填写 API Key（默认已填好 DeepSeek 地址 `https://api.deepseek.com/v1`、模型 `deepseek-chat`、温度 1.0）
3. 也可改成其他：

| 服务 | 接口地址 |
|------|----------|
| DeepSeek（默认） | `https://api.deepseek.com/v1` |
| ChatGPT | `https://api.openai.com/v1` |
| 通义 | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| 智谱 | `https://open.bigmodel.cn/api/paas/v4` |
| 本地 Ollama | `http://<电脑IP>:11434/v1` |

4. 上下文条数：默认 30，发送时附带最近 N 条聊天记录；设为 0 则不附带历史
5. （可选）在「外观」区：选择我的头像、选择聊天背景图、恢复默认
6. 保存后返回主界面即可对话
7. 顶栏 ‹ 返回键会直接回到手机主屏幕

---

## 💬 使用技巧

### 多行输入

输入时按回车换行，点发送后自动拆成多条独立消息逐条发出（间隔 0.3~0.7 秒）。
同一个人连续发的多条消息只显示一次头像。

### 连续快速发送

- 你发送一条消息后，**1 秒内**再发第二条，它不会马上发出去
- 第二条发完后 **1 秒内**再发第三条，也不会发
- 以此类推，一直等你停手
- **超过 1 秒没有新消息**，才会把你刚才连着发的那一堆**打包成一条**发出去

所以想一句一句发，就发完等 1 秒以上再发下一条喵。

### 表情

输入框左侧的笑脸按钮是矢量图标，点开会弹出黄脸 emoji 面板，点击表情即插入到输入框。

### 消息操作

长按任意消息可复制、删除、撤回或回溯；清空全部记录在设置页底部。

---

## 🌡️ 温度是个啥

温度越高，AI 越活跃、越放得开。
但是调太高会开始胡言乱语，前言不搭后语那种。

温度越低，AI 越安静、越沉稳，回答比较稳。
自己看着调喵，找到一个舒服的值就好。

**建议温度：1.1 ~ 1.2（针对 DeepSeek）**
其他提供商请看各自的官方文档，不同模型合适的值不一样喵。

---

## 🔌 关于 API

**目前只测试过 DeepSeek 的 API，其他 API 没试过。**
理论上应该也能用，毕竟是通用的 OpenAI 兼容接口格式，但没测过就是没测过喵。
想用别的 API 的自己试试，大概是好的，咕咕咕。

---

## 📋 兼容性

| 项目 | 说明 |
|------|------|
| 最低系统 | Android 5.0（API 21） |
| 架构 | 32 位 / 64 位全兼容 |
| 依赖 | 纯 Java，无原生库 |
| 渲染 | 无 WebView 要求 |

---

## 📄 许可

本项目遵循 **MIT 协议** 开源。
你可以自由地使用、修改、分发、学习参考。

### 但是喵（重要）

**禁止倒卖！禁止倒卖！禁止倒卖！**

- 不准拿这个项目去卖钱
- 不准打包成收费软件
- 不准拿去骗人说这是你自己做的
- 二次修改后请注明原项目出处

开源是分享，不是给你拿去赚钱的。
喜欢的话点个 Star 就好啦，咕咕咕。

---

## ⚠️ 声明

- 本项目与 Phigros 官方无关，仅为个人兴趣作品
- 仅供学习交流使用

---

<p align="center"><strong>咕咕咕～ 🕊️</strong></p>
