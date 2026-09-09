# Story Map：HomeVideo — 家庭内网视频播放系统

**基于 PRD**：v2.0（2026-09-10）
**拆分粒度**：Sprint 级（1-3 天/Story）
**估算体系**：Fibonacci（1/2/3/5/8/13）
**团队构成**：1 名全栈开发（Java + Android）
**架构**：Java Spring Boot 服务端 + Android TV 客户端
**服务端**：s2（192.168.31.73），视频目录 `/srv/hometok-media`
**目标设备**：索尼 BRAVIA 4K VH22（Android TV 10，API 29）

---

## 需求全貌

**涉及角色**：
- 家庭管理者（配置者）—— 部署服务端、配置客户端连接
- 家庭观看者 —— 浏览视频、播放观看

**核心用户旅程**：
```
部署服务端 → 安装客户端 → 连接服务端 → 浏览文件夹 → 选择视频 → 播放 → 控制播放 → 续播/从历史续播
```

**拆分模式**：先服务端后客户端，按工作流步骤 + 按复杂度递进

---

## Epic 1：服务端基础

| 用户旅程阶段 | Story | 优先级 | Points |
|-------------|-------|--------|--------|
| 项目搭建 | S-01 | P0 | 3 |
| 文件浏览 API | S-02 | P0 | 5 |
| 视频流 + 字幕 + 缩略图 | S-03 | P0 | 8 |
| 播放历史 API | S-04 | P1 | 3 |

---

### S-01：初始化 Spring Boot 服务端项目

**角色**：作为开发者
**需求**：我希望搭建 Spring Boot 项目骨架并配置好基础依赖
**价值**：以便后续 API 开发有稳定的基础
**优先级**：P0
**Story Points**：3

**Acceptance Criteria**：
- [ ] Given 空项目目录, When 执行 `./gradlew bootJar`, Then 生成可运行的 jar 包
- [ ] Given jar 包运行在 s2 上, When 访问 `http://192.168.31.73:8080/api/health`, Then 返回 200 OK
- [ ] Given 配置文件指定视频目录, When 启动服务, Then 应用正确读取配置的视频根目录路径
- [ ] Given 服务运行中, When 查看日志, Then 无 ERROR 级别异常

**依赖**：无
**技术备注**：Java 17 + Spring Boot 3.x + Maven；配置 `homevideo.media-dir` 指向视频目录；H2 数据库文件模式；提供 systemd service 文件

---

### S-02：文件浏览与搜索 API

**角色**：作为客户端开发者
**需求**：我希望服务端提供文件浏览和搜索接口
**价值**：以便客户端能展示服务器上的视频文件列表
**优先级**：P0
**Story Points**：5

**Acceptance Criteria**：
- [ ] Given 视频目录下有子文件夹和视频文件, When 调用 `GET /api/files?path=/`, Then 返回文件夹列表和视频文件列表，视频文件包含文件名、大小、分辨率、修改时间
- [ ] Given 目录下有 .txt、.jpg 等非视频文件, When 调用文件浏览 API, Then 这些文件不出现在返回结果中
- [ ] Given 支持的格式包括 mp4/mkv/avi/mov/ts/flv/wmv/rmvb, When 目录中有这些格式的文件, Then 全部正确返回
- [ ] Given 目录超过 100 个文件, When 调用文件浏览 API, Then 默认分页返回 100 条，支持 page 和 size 参数
- [ ] Given 排序参数为 sort=name, When 调用 API, Then 结果按文件名升序排列
- [ ] Given 排序参数为 sort=modified&order=desc, When 调用 API, Then 结果按修改时间降序排列
- [ ] Given 视频目录下有文件名包含"家庭"的文件, When 调用 `GET /api/files/search?keyword=家庭`, Then 返回所有文件名匹配的结果，包含文件所在路径
- [ ] Given 搜索无匹配, When 调用搜索 API, Then 返回空列表

**依赖**：S-01
**技术备注**：递归读取本地文件系统；扩展名过滤；ffmpeg 提取分辨率（可异步缓存）；搜索用文件名 contains 匹配

---

### S-03：视频流传输、字幕匹配与缩略图

**角色**：作为客户端开发者
**需求**：我希望服务端提供视频流传输、字幕文件和缩略图接口
**价值**：以便客户端能播放视频、显示字幕和展示缩略图
**优先级**：P0
**Story Points**：8

**Acceptance Criteria**：
- [ ] Given 服务端有一个视频文件, When 调用 `GET /api/videos/stream/{id}`, Then 返回视频二进制流
- [ ] Given 客户端发送 Range 请求, When 请求 `Range: bytes=1000-2000`, Then 返回 206 Partial Content，仅包含请求的字节范围
- [ ] Given 视频文件超过 10GB, When 客户端 seek 到任意位置, Then 服务端在 1 秒内开始返回数据，无需加载整个文件
- [ ] Given 视频同目录有同名 .srt 文件, When 调用 `GET /api/videos/info/{id}`, Then 返回的字幕列表中包含该字幕文件
- [ ] Given 字幕文件编码为 GBK, When 客户端请求字幕内容, Then 服务端返回 UTF-8 编码的字幕文本
- [ ] Given 视频文件存在, When 调用 `GET /api/videos/thumbnail/{id}`, Then 返回 JPEG 缩略图（截取视频第 10% 处）
- [ ] Given 缩略图已生成过, When 再次请求同一视频缩略图, Then 直接返回缓存，不重复调用 ffmpeg
- [ ] Given 视频文件存在, When 调用 `GET /api/videos/info/{id}`, Then 返回视频时长、分辨率、编码格式、可用音轨列表

**依赖**：S-01
**技术备注**：Spring `ResourceRegion` 处理 Range 请求；ffmpeg 截取缩略图缓存到 `/tmp/homevideo-thumbnails/`；字幕编码检测用 juniversalchardet；视频信息通过 `ffprobe -print_format json` 提取

---

### S-04：播放历史 API

**角色**：作为客户端开发者
**需求**：我希望服务端提供播放历史的增删查接口
**价值**：以便客户端能展示最近观看记录并支持断点续播
**优先级**：P1
**Story Points**：3

**Acceptance Criteria**：
- [ ] Given 客户端提交播放记录, When 调用 `POST /api/history`, Then 记录保存成功，返回 200
- [ ] Given 已有 5 条播放记录, When 调用 `GET /api/history`, Then 返回按最后播放时间降序排列的 5 条记录
- [ ] Given 已有 50 条记录, When 新增第 51 条, Then 最早的记录被自动删除
- [ ] Given 客户端请求删除某条记录, When 调用 `DELETE /api/history/{videoId}`, Then 该记录被删除
- [ ] Given 客户端请求清除全部历史, When 调用 `DELETE /api/history`, Then 所有记录清空

**依赖**：S-01
**技术备注**：H2 数据库存储播放记录（videoId, position, duration, lastPlayed）；JPA Repository 操作

---

## Epic 2：客户端基础

| 用户旅程阶段 | Story | 优先级 | Points |
|-------------|-------|--------|--------|
| 项目搭建 | C-01 | P0 | 3 |
| 服务器连接 | C-02 | P0 | 5 |
| 配置持久化 | C-03 | P0 | 2 |
| 首次引导 | C-04 | P0 | 2 |

---

### C-01：初始化 Android TV 客户端项目

**角色**：作为开发者
**需求**：我希望搭建 Android TV 项目骨架并配置好基础依赖
**价值**：以便后续功能模块有稳定的开发基础
**优先级**：P0
**Story Points**：3

**Acceptance Criteria**：
- [ ] Given 空项目目录, When 执行 `./gradlew assembleDebug`, Then 生成可安装到 Android TV 模拟器的 APK
- [ ] Given App 运行在 BRAVIA 4K VH22 上, When 使用遥控器方向键, Then 焦点在 UI 组件间正确移动，无焦点丢失
- [ ] Given App 启动, When 查看 Android Manifest, Then 声明了 `android.software.leanback` 和 `android.permission.INTERNET`（仅局域网），未声明外网相关权限

**依赖**：无
**技术备注**：Kotlin + Jetpack Compose for TV；minSdk 29；预留 Retrofit、Media3 ExoPlayer 依赖；配置服务端默认地址 `http://192.168.31.73:8080`

---

### C-02：连接服务端

**角色**：作为家庭管理者
**需求**：我希望 App 能发现并连接到服务端
**价值**：以便访问服务端管理的视频文件
**优先级**：P0
**Story Points**：5

**Acceptance Criteria**：
- [ ] Given App 与服务端在同一局域网, When 点击「自动扫描」, Then 10 秒内展示发现的服务端（含 IP）
- [ ] Given 用户选择了服务端, When 点击连接, Then 调用 `/api/health` 验证连通性，成功则进入主界面
- [ ] Given 自动扫描未发现服务端, When 扫描超时, Then 显示"未发现服务端，可手动输入服务器地址"
- [ ] Given 用户选择手动输入, When 输入有效 IP 并连接, Then 成功连接到服务端
- [ ] Given 用户输入无法连接的 IP, When 点击连接, Then 5 秒后显示"无法连接到服务端，请确认服务端已启动"

**依赖**：C-01
**技术备注**：自动发现用子网广播或 mDNS（JmDNS）；手动输入适配遥控器数字键盘；连接验证调用 `/api/health`

---

### C-03：服务器连接配置持久化

**角色**：作为家庭管理者
**需求**：我希望服务端连接信息保存后，下次打开 App 自动重连
**价值**：以便家人打开 App 就能直接用
**优先级**：P0
**Story Points**：2

**Acceptance Criteria**：
- [ ] Given 已成功连接服务端, When 杀掉 App 后重新打开, Then 自动重连服务端，主界面直接展示文件列表
- [ ] Given 服务端处于停止状态, When 打开 App, Then 主界面顶部显示"服务端未连接"并提供重连按钮

**依赖**：C-02
**技术备注**：DataStore 存储服务端 IP；App 启动时异步验证连通性，不阻塞 UI

---

### C-04：首次启动连接引导

**角色**：作为家庭管理者
**需求**：我希望首次打开 App 时有简洁的引导帮我连接服务端
**价值**：以便 3 分钟内就能开始使用
**优先级**：P0
**Story Points**：2

**Acceptance Criteria**：
- [ ] Given App 首次启动（未配置服务端）, When 进入 App, Then 显示引导页，包含「自动扫描」和「手动输入」两个选项
- [ ] Given 引导流程中成功连接服务端, When 点击「完成」, Then 直接进入主界面（文件浏览）
- [ ] Given 引导页面展示, When 使用遥控器操作, Then 全程仅需方向键 + 确认键即可完成

**依赖**：C-02
**技术备注**：单 Activity 架构，引导页作为首个 Screen；连接成功后判断是否首次

---

## Epic 3：客户端视频浏览

| 用户旅程阶段 | Story | 优先级 | Points |
|-------------|-------|--------|--------|
| 文件浏览 | C-05 | P0 | 5 |
| 视频搜索 | C-06 | P1 | 3 |

---

### C-05：文件夹层级浏览与视频文件列表

**角色**：作为家庭观看者
**需求**：我希望打开 App 就能浏览服务端上的文件夹和视频文件
**价值**：以便像在文件管理器中一样快速找到想看的视频
**优先级**：P0
**Story Points**：5

**Acceptance Criteria**：
- [ ] Given 已连接服务端, When 打开 App, Then 主界面直接展示服务端视频根目录的内容（文件夹 + 视频文件混合）
- [ ] Given 目录中有子文件夹和视频文件, When 展示列表, Then 文件夹在前、视频文件在后，视频文件显示文件名、大小和分辨率
- [ ] Given 列表中有一个子文件夹, When 点击该文件夹, Then 进入下一级目录
- [ ] Given 列表中有一个视频文件, When 点击该视频, Then 进入播放
- [ ] Given 某文件夹内无视频文件, When 进入该文件夹, Then 显示"该文件夹中没有视频文件"
- [ ] Given 用户在某文件夹中滚动到了第 50 个文件, When 按返回键再重新进入, Then 列表滚动位置恢复到上次位置
- [ ] Given 浏览过程中服务端连接中断, When 自动重试失败, Then 显示"与服务端连接断开，请检查网络"

**依赖**：C-03
**技术备注**：调用服务端 `/api/files` 接口；`LazyColumn` + 分页（>100 文件时）；记住浏览位置用 DataStore

---

### C-06：按文件名搜索视频

**角色**：作为家庭观看者
**需求**：我希望通过关键字搜索视频文件名
**价值**：以便在大量视频中快速定位，无需逐级翻文件夹
**优先级**：P1
**Story Points**：3

**Acceptance Criteria**：
- [ ] Given 用户在主界面或文件列表界面, When 按遥控器「搜索」键, Then 顶部弹出搜索输入框并自动聚焦
- [ ] Given 搜索框已聚焦, When 通过遥控器输入关键字"家庭", Then 调用服务端搜索 API，实时展示匹配结果
- [ ] Given 搜索有 5 个匹配结果, When 查看结果列表, Then 显示 5 条结果，每条含文件名和所在文件夹路径
- [ ] Given 搜索无匹配, When 查看结果, Then 显示"没有找到匹配的视频"
- [ ] Given 搜索结果中有一条视频, When 点击该视频, Then 直接进入播放
- [ ] Given 搜索框已展开, When 按返回键, Then 关闭搜索框回到上一界面

**依赖**：C-05
**技术备注**：调用服务端 `/api/files/search` 接口；TV 端文字输入适配遥控器

---

## Epic 4：客户端视频播放

| 用户旅程阶段 | Story | 优先级 | Points |
|-------------|-------|--------|--------|
| 基础播放 | C-07 | P0 | 5 |
| 播放控制 | C-08 | P0 | 5 |
| 播放设置 | C-09 | P1 | 5 |
| 外挂字幕 | C-10 | P1 | 3 |

---

### C-07：基础视频播放（ExoPlayer 集成）

**角色**：作为家庭观看者
**需求**：我希望选中视频后能直接全屏播放
**价值**：以便立即看到视频内容
**优先级**：P0
**Story Points**：5

**Acceptance Criteria**：
- [ ] Given 文件列表中有一个 H.264 编码的 MP4 文件, When 点击该文件, Then 2 秒内进入全屏播放，画面和声音正常
- [ ] Given 文件列表中有一个 H.265 编码的 MKV 文件, When 点击该文件, Then 正常播放，画面和声音同步
- [ ] Given 文件列表中有一个 AVI 文件, When 点击该文件, Then 正常播放
- [ ] Given 视频编码不被支持, When 点击该文件, Then 显示"该视频格式暂不支持播放"并返回文件列表
- [ ] Given 视频来自服务端 HTTP 流, When 播放中, Then 播放流畅无频繁缓冲（内网环境）
- [ ] Given 视频正在播放, When 获取播放器状态, Then 能正确获取视频总时长和当前播放位置
- [ ] Given 视频有播放历史（上次播放到 30:00）, When 开始播放, Then 自动从 30:00 位置开始

**依赖**：C-05
**技术备注**：Media3 ExoPlayer + HTTP DataSource（Retrofit/OkHttp）；优先硬解，不支持时回退软解；播放前调用 `/api/videos/info` 获取视频信息和续播位置

---

### C-08：播放控制界面与遥控器操作

**角色**：作为家庭观看者
**需求**：我希望在播放时能通过遥控器方便地控制播放进度
**价值**：以便自由跳转到想看的片段
**优先级**：P0
**Story Points**：5

**Acceptance Criteria**：
- [ ] Given 视频正在播放, When 按遥控器「确认」键, Then 底部显示控制栏（进度条、当前时间/总时长），5 秒后自动隐藏
- [ ] Given 控制栏已显示, When 按方向键右, Then 快进 10 秒，进度条和时间更新
- [ ] Given 控制栏已显示, When 按方向键左, Then 快退 10 秒，进度条和时间更新
- [ ] Given 视频正在播放, When 长按方向键右, Then 以 3 倍速快进，屏幕显示速度指示
- [ ] Given 视频正在播放, When 长按方向键左, Then 以 3 倍速快退
- [ ] Given 控制栏已显示, When 按「返回」键, Then 控制栏隐藏
- [ ] Given 控制栏已隐藏, When 按「返回」键, Then 退出播放回到文件列表
- [ ] Given 视频播放到最后 30 秒, When 继续播放, Then 显示「重新播放」和「返回」快捷按钮
- [ ] Given 视频播放结束, When 不操作, Then 显示「重新播放」和「返回列表」选项

**依赖**：C-07
**技术备注**：`PlayerView` 自定义 ControlDispatcher；长按用 `KeyEvent.FLAG_LONG_PRESS` 或自定义定时器；控制栏动画用 Compose animation；播放结束时调用 `POST /api/history` 更新记录

---

### C-09：播放设置面板（音轨/字幕/速度）

**角色**：作为家庭观看者
**需求**：我希望在播放时能切换音轨、字幕和调节播放速度
**价值**：以便适应不同语言需求和观看偏好
**优先级**：P1
**Story Points**：5

**Acceptance Criteria**：
- [ ] Given 视频包含多条音轨, When 按「菜单」键打开设置面板, Then 显示可用音轨列表，点击可切换
- [ ] Given 视频已加载字幕, When 打开设置面板, Then 显示字幕轨道列表，可选择切换或关闭
- [ ] Given 设置面板中显示播放速度, When 选择 1.5x, Then 视频以 1.5 倍速播放，声音音调不变
- [ ] Given 可选速度为 0.5x / 1.0x / 1.25x / 1.5x / 2.0x, When 逐一切换, Then 每种速度正常，声音不变调
- [ ] Given 设置面板已打开, When 按「返回」键, Then 关闭面板继续播放

**依赖**：C-07
**技术备注**：ExoPlayer `TrackSelector` 切换音轨/字幕轨；`PlaybackParameters(speed)` 变速；Compose Dialog

---

### C-10：外挂字幕自动加载

**角色**：作为家庭观看者
**需求**：我希望播放视频时能自动加载字幕
**价值**：以便不需要手动查找和加载字幕
**优先级**：P1
**Story Points**：3

**Acceptance Criteria**：
- [ ] Given 视频有可用字幕（服务端 info 接口返回字幕列表）, When 开始播放, Then 自动加载第一个字幕并显示
- [ ] Given 服务端返回字幕内容, When 字幕编码为 UTF-8, Then 中文正确显示无乱码
- [ ] Given 服务端已将 GBK 字幕转为 UTF-8 返回, When 加载字幕, Then 正确显示
- [ ] Given 视频无可用字幕, When 播放, Then 正常播放不报错
- [ ] Given 字幕已加载, When 在设置面板中切换字幕, Then 从服务端请求对应字幕内容并切换显示

**依赖**：C-07
**技术备注**：播放前调用 `/api/videos/info` 获取字幕列表；字幕内容通过 `/api/videos/subtitles/{id}` 获取；ExoPlayer `SingleSampleMediaSource` 加载字幕

---

## Epic 5：客户端播放历史与设置

| 用户旅程阶段 | Story | 优先级 | Points |
|-------------|-------|--------|--------|
| 最近观看 | C-11 | P1 | 5 |
| 设置页面 | C-12 | P1 | 3 |

---

### C-11：最近观看记录与续播

**角色**：作为家庭观看者
**需求**：我希望在主界面看到最近播放过的视频列表
**价值**：以便快速继续上次没看完的视频
**优先级**：P1
**Story Points**：5

**Acceptance Criteria**：
- [ ] Given 已播放过 3 个视频（每个超过 30 秒）, When 打开 App, Then 主界面顶部展示「最近观看」，包含这 3 个视频
- [ ] Given 「最近观看」有记录, When 查看每条, Then 显示缩略图、文件名、上次播放位置和相对时间（如"昨天"）
- [ ] Given 点击「最近观看」中的记录, When 服务端在线, Then 从上次位置开始播放
- [ ] Given 某视频播放不足 30 秒后退出, When 查看「最近观看」, Then 不出现该视频
- [ ] Given 已有 10 条记录, When 播放第 11 个视频, Then 最早的记录不再显示在最近观看中
- [ ] Given 长按某条记录, When 选择「移除」, Then 调用服务端删除接口，该记录消失
- [ ] Given 某记录对应的视频已被删除, When 点击, Then 显示"该视频文件已不存在"
- [ ] Given 服务端离线, When 查看「最近观看」, Then 记录置灰显示"服务端未连接"

**依赖**：C-07
**技术备注**：调用服务端 `/api/history` 接口获取记录；缩略图从 `/api/videos/thumbnail/{id}` 加载并缓存；播放时定期 `POST /api/history` 更新位置

---

### C-12：设置页面

**角色**：作为家庭管理者
**需求**：我希望有一个设置页面来管理应用配置
**价值**：以便按需调整配置
**优先级**：P1
**Story Points**：3

**Acceptance Criteria**：
- [ ] Given 用户在主界面, When 导航到「设置」, Then 进入设置页面
- [ ] Given 设置页面展示, When 查看列表, Then 包含：服务器连接、默认播放速度、字幕大小、清除播放历史、关于
- [ ] Given 点击「服务器连接」, When 进入子页面, Then 显示当前服务端 IP，可修改 IP，可测试连接
- [ ] Given 修改「默认播放速度」为 1.25x, When 下次播放视频, Then 以 1.25x 开始
- [ ] Given 修改「字幕大小」为"大", When 下次播放有字幕视频, Then 字幕字号更大
- [ ] Given 点击「清除播放历史」并确认, When 操作完成, Then 调用服务端清除接口，历史记录清空
- [ ] Given 设置页面展示, When 遥控器操作, Then 全程方向键 + 确认键可完成

**依赖**：C-03, C-11
**技术备注**：DataStore 存储客户端设置项（播放速度、字幕大小）；服务器连接修改复用 C-02 的连接逻辑；清除历史调用 `DELETE /api/history`

---

## Sprint 规划建议

### Sprint 1（服务端 MVP + 客户端骨架）—— 23 Points，约 10 天

| Story | 标题 | Points | 依赖 |
|-------|------|--------|------|
| S-01 | 初始化 Spring Boot 服务端项目 | 3 | 无 |
| S-02 | 文件浏览与搜索 API | 5 | S-01 |
| S-03 | 视频流传输、字幕匹配与缩略图 | 8 | S-01 |
| C-01 | 初始化 Android TV 客户端项目 | 3 | 无 |
| C-02 | 连接服务端 | 5 | C-01 |
| C-03 | 服务器连接配置持久化 | 2 | C-02 |
| C-04 | 首次启动连接引导 | 2 | C-02 |

**交付目标**：服务端可运行，客户端可连接服务端。Sprint 内服务端和客户端可并行开发。

**关键路径**：
```
S-01 → S-02 → S-03（服务端）
C-01 → C-02 → C-03（客户端）
                ↗
          C-04
```

---

### Sprint 2（核心播放链路）—— 15 Points，约 8 天

| Story | 标题 | Points | 依赖 |
|-------|------|--------|------|
| C-05 | 文件夹层级浏览与视频文件列表 | 5 | C-03, S-02 |
| C-07 | 基础视频播放（ExoPlayer 集成） | 5 | C-05, S-03 |
| C-08 | 播放控制界面与遥控器操作 | 5 | C-07 |

**交付目标**：安装 → 连接服务端 → 浏览文件 → 播放视频 → 控制播放，完整闭环。

**关键路径**：
```
S-02 → C-05 → C-07 → C-08
              ↗
        S-03
```

---

### Sprint 3（体验增强）—— 17 Points，约 7 天

| Story | 标题 | Points | 依赖 |
|-------|------|--------|------|
| S-04 | 播放历史 API | 3 | S-01 |
| C-06 | 按文件名搜索视频 | 3 | C-05 |
| C-09 | 播放设置面板（音轨/字幕/速度） | 5 | C-07 |
| C-10 | 外挂字幕自动加载 | 3 | C-07 |
| C-11 | 最近观看记录与续播 | 5 | C-07, S-04 |
| C-12 | 设置页面 | 3 | C-03, C-11 |

**交付目标**：搜索、字幕、播放历史、设置全部就位，产品可交付家庭成员日常使用。

**可并行**：C-06、C-09、C-10 互不依赖；S-04 与客户端开发并行。

---

## 依赖关系总览

```
服务端：
S-01（Spring Boot 骨架）
├── S-02（文件浏览 API）
├── S-03（视频流 + 字幕 + 缩略图）
└── S-04（播放历史 API）

客户端：
C-01（Android TV 骨架）
└── C-02（连接服务端）
    ├── C-03（持久化）
    │   ├── C-05（文件浏览）← 依赖 S-02
    │   │   ├── C-06（搜索）
    │   │   └── C-07（播放）← 依赖 S-03
    │   │       ├── C-08（播放控制）
    │   │       ├── C-09（播放设置）
    │   │       ├── C-10（字幕）
    │   │       └── C-11（播放历史）← 依赖 S-04
    │   │           └── C-12（设置，也依赖 C-03）
    │   └── C-12（设置）
    └── C-04（首次引导）
```

---

## INVEST 校验结果

| Story | I | N | V | E | S | T | 结果 |
|-------|---|---|---|---|---|---|------|
| S-01 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| S-02 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| S-03 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| S-04 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-01 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-02 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-03 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-04 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-05 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-06 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-07 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-08 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-09 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-10 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-11 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |
| C-12 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 通过 |

**全部 16 个 Story 通过 INVEST 校验。**

---

## v1.1 → v2.0 变更说明

| 变更项 | v1.1（单 App 直连 SMB） | v2.0（前后端分离） | 影响 |
|--------|------------------------|-------------------|------|
| 架构 | 单 Android App，通过 smbj 直连 SMB | Java 服务端 + Android TV 客户端 | 新增服务端开发 |
| 协议 | SMB/CIFS (smbj) | HTTP REST API + 视频流 | 砍掉 smbj，新增 Retrofit |
| 文件访问 | 客户端通过 SMB 读取 | 服务端直接读本地文件系统 | 更高效，无 SMB 协议开销 |
| 播放历史 | 存储在客户端本地 | 存储在服务端（可多设备共享） | 需开发服务端持久化 |
| 部署 | 仅安装 APK | 部署服务端 jar + 安装 APK | 多一步服务端部署 |
| Story 数量 | 12 个 | 16 个（4 服务端 + 12 客户端） | 增加 4 个 |
| 总 Points | 44 | 55 | 增加 11 点 |
| 预估工期 | 20 天 | 25 天 | 增加 5 天（可并行缩短） |
