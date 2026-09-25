# SoccerMatch — Minecraft 足球比赛裁判插件

面向 **Paper 26.2 / Java 25** 的足球比赛管理插件。提供红黄牌判罚、计时、比分、上下半场、队内聊天、球门自动计分、裁判哨（右键暂停/继续）与站位杖（右键模板站位）功能。

---

## 一、环境要求

| 项目 | 要求 | 说明 |
|---|---|---|
| 服务端 | Paper 26.2 | Leaves / Purpur 等 Paper 衍生端同样可用 |
| 运行 Java | **25 或更高** | MC 26.2 的硬性要求，Java 21 无法启动 |
| 编译 JDK | **25** | Paper 26.2 的字节码版本为 Java 25 |
| 构建工具 | Maven 3.9+ | 或直接用 IDEA 内置 Maven |

> **依赖坐标提醒**：Paper 从 26.x 起改变了版本命名规则，**不再是** `26.2-R0.1-SNAPSHOT`，
> 正确写法是 `26.2.build.87-stable`。写错会直接拉不到依赖。

---

## 二、编译打包

### 用 IntelliJ IDEA（推荐）

1. `File → Open`，选中本目录的 `pom.xml`，以项目方式打开
2. `File → Project Structure → SDK`，选择 **JDK 25**
   如果列表里没有，点 `Download JDK`，Version 选 25，Vendor 选 Temurin
3. 右侧 Maven 面板 → `Lifecycle` → 双击 `package`
4. 产物在 `target/SoccerMatch.jar`

### 用命令行

```bash
mvn clean package
```

> **本机已配好便携工具链**：仓库里的 `.build-cache/` 已附带
> **JDK 25.0.4+7** 与 **Maven 3.9.16**，无需在本机另行安装。
> 直接用以下命令即可一键重编（Windows PowerShell）：
>
> ```powershell
> $env:JAVA_HOME = "e:/workbuddy1files/SoccerMatch/.build-cache/jdk-25.0.4+7"
> $env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
> .\.build-cache\apache-maven-3.9.16\bin\mvn -s .build-cache\settings.xml -B clean package
> ```
>
> 产物在 `target/SoccerMatch.jar`。

> **JDK 25 兼容性提醒**：`pom.xml` 已显式固定两处插件版本，
> 否则 Maven 3.9.16 默认携带的版本会在 JDK 25 下编译失败：
> - `maven-resources-plugin` 固定为 `3.3.1`（3.4.0 在 JDK 25 下 Guice 注入报错）
> - `maven-jar-plugin` 固定为 `3.4.1`（3.5.0 因 plexus-utils 4 的 `DirectoryScanner` 类名变更报缺失类）
> 如果你换用更高版本 Maven，可自行去掉这两处固定。

---

## 三、安装

1. 把 `SoccerMatch.jar` 放进服务器 `plugins/` 目录
2. 启动服务器，会自动生成 `plugins/SoccerMatch/config.yml`
3. 按需修改配置，然后 `/js reload` 生效（**不需要重启服务器**）

---

## 四、命令一览

裁判命令默认仅 **OP** 可用。若想给非 OP 的裁判开权限，授予 `soccer.referee` 即可。

### 判罚

| 命令 | 作用 |
|---|---|
| `/y <玩家ID>` | 出示黄牌，全服显示「警告 + 玩家名」 |
| `/y undo` | 撤销最近一张黄牌，全服显示「裁判撤销黄牌 + 玩家名」 |
| `/r <玩家ID>` | 出示红牌，全服显示「罚下 + 玩家名」并执行罚下处置 |

- 玩家名支持 **Tab 补全**
- 玩家不在线或名字打错时，**直接报错、不执行**
- 累计 **3 张黄牌自动罚下**（张数可在 `cards.yellow-to-red` 修改）
- 撤销会一并回滚罚下：补回队伍 tag、还原游戏模式、传送回罚下前的位置

### 比赛控制

| 命令 | 作用 | 非法状态下的提示 |
|---|---|---|
| `/js start` | 3-2-1 倒计时后开赛 | 已开始 → 「比赛进行中，不能重复开始」 |
| `/js stop` | 暂停计时 | 未开始 → 「比赛未开始」；已暂停 → 「已经处于暂停状态」 |
| `/js restart` | 继续计时 | 未开始 → 「比赛未开始」；**没暂停过 → 「比赛未暂停」** |
| `/js end` | 结束比赛并宣布结果 | 未开始 → 「比赛未开始」 |
| `/js goal <red\|blue> [球员]` | 进球计分 | 供球门命令方块调用 |
| `/js score <red\|blue> <分数>` | 直接修正比分 | 误判纠正用 |
| `/js reload` | 重载配置 | — |

> `/js restart` 的两种失败情形提示是**分开的**：从没开始过说「比赛未开始」，
> 正在跑但没暂停说「比赛未暂停」，不会混为一谈。

### 其他

| 命令 | 作用 |
|---|---|
| `/soccerstatus true` | 切到上半场 |
| `/soccerstatus false` | 切到下半场 |
| `/tc <内容>` | 队内聊天，仅同队队员与 OP 可见 |

### 裁判物品（右键触发）

除命令外，裁判还能通过右键**绑定物品**快速操作，省去打字：

| 物品 | 制作方式 | 右键效果 |
|---|---|---|
| **裁判哨** | 铁砧上将**纸**改名为「裁判哨」 | 暂停 ⇄ 继续比赛，完全等价 `/js stop` / `/js restart`，含相同状态提示 |
| **站位杖** | 铁砧上将**木棍**改名为「站位杖」 | 暂停期间使用：双方所有带 tag 球员按模板站位 |

**站位杖详细说明**：
- 以裁判右键时的位置为**发球点**，球员按模板偏移展开
- 偏移跟随裁判**面朝方向旋转**——裁判朝哪，阵型就朝哪展开
- 在线球员按**名字母序**填入固定槽位（默认 11 人），人少则空着、多余的不站位
- 站位后球员**立即冻结**，直到哨子或 `/js restart` 恢复比赛才解冻
- 物品名和材质均可在 `config.yml` 的 `referee-items` 节点修改

> 物品识别方式：**材质 + 显示名**匹配（不区分大小写）。
> 仅 OP 或拥有 `soccer.referee` 权限的玩家可使用。
> 内置 500ms 右键冷却防连点。

---

## 五、球门自动计分接线（重点）

你的方案是：球门下方埋绊线钩，硫磺怪（球）进门触发绊线 → 命令方块加分。

### 命令方块设置

在球门后方放一个命令方块，连接绊线钩的红石信号：

| 设置项 | 必须选择 |
|---|---|
| 方块类型 | **脉冲（Impulse）** |
| 条件性 | 无条件（Unconditional） |
| 红石 | **需要红石（Needs Redstone）** |

命令内容（**注意是谁的球门就给对方加分**）：

```
# 蓝队球门里的命令方块 → 红队得分
js goal red

# 红队球门里的命令方块 → 蓝队得分
js goal blue
```

### 三个必须注意的坑

**① 千万别用「循环（Repeat）」型命令方块**
循环型每 tick 执行一次，球只要停在绊线上，比分会在一秒内涨几十分。必须用**脉冲型**。

**② 插件已内置进球冷却，默认 3 秒**
绊线钩在球体滚动时会连续输出红石信号，即使用了脉冲型也可能连触发好几次。
插件会自动忽略冷却期内的重复信号，配置项为 `match.goal-cooldown`。
如果你的球门比较深、球会来回滚，可以把这个值调大到 5。

**③ 建议关掉命令方块的聊天回显**

```
/gamerule sendCommandFeedback false
/gamerule commandBlockOutput false
```

否则每次进球，所有 OP 的聊天栏都会刷一行命令方块日志。

---

## 六、分队方式

队伍完全基于**原版 tag**，插件不另外维护名单：

```
/tag <玩家> add red     # 加入红队
/tag <玩家> add blue    # 加入蓝队
/tag <玩家> remove red  # 退出红队
```

tag 名称可以在 `config.yml` 的 `teams.red.tag` / `teams.blue.tag` 里改。

没有任何队伍 tag 的玩家会被视为**观众**，不受冻结影响，也不能用 `/tc`。

---

## 七、配置要点

完整注释都写在 `config.yml` 里，这里只列最常改的几项。

### 时长与计分板

```yaml
match:
  duration: 600          # 每个半场秒数，600 = 10 分钟
  countdown: 3           # 开球 3-2-1 倒计时
  goal-cooldown: 3       # 进球冷却，防绊线钩重复计分

scoreboard:
  title: '&6&l⚽ 足球比赛'   # 侧边栏标题
  lines:                    # 侧边栏内容，可自由增删
    - '&f阶段: &e%half%'
    - '&f剩余: &a%time%'
    - '&c● 红队  &f%red%'
    - '&9● 蓝队  &f%blue%'
    - '&f你的队伍: %team%'
```

可用占位符：`%time%` `%half%` `%red%` `%blue%` `%team%` `%state%`

### 罚下传送点

```yaml
cards:
  yellow-to-red: 3       # 几张黄牌自动罚下
  send-off:
    world: 'world'       # 必须与服务器实际世界名完全一致，区分大小写
    x: 0.5
    y: 100.0
    z: 0.5
```

> 世界名写错是最常见的失败原因。插件会在控制台给出明确警告而不是静默失败。

### 冻结行为

```yaml
match:
  freeze-on-countdown: true   # 开球倒计时冻结球员，防抢跑
  freeze-on-pause: true       # 暂停期间冻结，防趁机占位
  freeze-exempt-op: true      # 裁判(OP)不被冻结，可自由走动执法
  freeze-players-only: true   # 只冻有队伍 tag 的球员，观众不受影响
```

冻结期间玩家**仍可自由转动视角**观察场上，只是不能移动和攻击。

### 站位模板

```yaml
positioning:
  max-slots: 11           # 每队槽位上限

  red:                     # 红队模板
    - x: 0.0              # 右侧偏移（正=裁判右手边）
      y: 0.0              # 垂直偏移（正=上方）
      z: -25.0            # 前方偏移（正=裁判前方，负=后方）
      yaw: 0.0            # 面朝偏转（0=同裁判朝向，180=面向裁判）
      pitch: 0.0          # 俯仰角（0=平视）
    # ... 继续添加更多槽位

  blue:                    # 蓝队模板（与红队镜像）
    - x: 0.0
      y: 0.0
      z: 25.0
      yaw: 180.0
      pitch: 0.0
    # ...
```

> **坐标系说明**：以裁判面朝方向为基准，`x`=右、`y`=上、`z`=前。
> 模板偏移会跟随裁判右键时的面朝方向**自动旋转**。
> 默认提供 4-4-2 阵型（11 人），可按实际场地自由修改。

---

## 八、一场比赛的完整流程

```
1. 分队
   /tag PlayerA add red
   /tag PlayerB add blue

2. 开赛（自动 3-2-1 倒计时，期间球员被冻结）
   /js start

3. 比赛中
   进球     → 球门命令方块自动执行 js goal red
   黄牌     → /y PlayerA
   出错了   → /y undo
   红牌     → /r PlayerA
   叫暂停   → /js stop  或  右键「裁判哨」
   继续     → /js restart  或  右键「裁判哨」
   出界发球 → 右键「站位杖」双方球员按模板站位

4. 中场
   /soccerstatus false        切到下半场，计时自动重置并暂停
   /js restart                下半场开球

5. 结束
   倒计时归零会自动结束并宣布胜方
   也可手动 /js end
```

---

## 九、已知边界

- 队伍固定为红、蓝两队。多队制需要改代码
- 出界检测、换人、点球大战未实现（按需求确认时的约定省略）
- 赛后统计报告未实现，队内聊天会同步一份到控制台日志便于人工审核
- 计分板为每位玩家独立生成，以便显示各自的所属队伍；在线人数极多（数百人）时会有一定开销

---

## 十、目录结构

```
SoccerMatch/
├── pom.xml
├── README.md
└── src/main/
    ├── java/mcsc/plugin/soccer/
    │   ├── SoccerPlugin.java          插件主类
    │   ├── config/SoccerConfig.java   配置封装
    │   ├── match/
    │   │   ├── MatchManager.java      状态机 / 计时 / 比分 / 半场
    │   │   ├── MatchState.java
    │   │   └── MatchHalf.java
    │   ├── card/
    │   │   ├── CardManager.java       红黄牌 / 三黄变红 / 撤销
    │   │   └── CardRecord.java
    │   ├── team/
    │   │   ├── TeamManager.java       tag 分队
    │   │   └── TeamSide.java
    │   ├── display/
    │   │   ├── ScoreboardDisplay.java 侧边栏
    │   │   ├── BossBarDisplay.java    计时条
    │   │   └── Placeholders.java
    │   ├── listener/
    │   │   ├── MatchListener.java      冻结与清理
    │   │   └── RefereeItemListener.java 裁判哨/站位杖右键监听
    │   ├── positioning/
    │   │   ├── PositioningManager.java  站位旋转+传送
    │   │   └── SlotOffset.java          槽位偏移记录
    │   ├── command/                    五个命令
    │   └── util/
    └── resources/
        ├── plugin.yml
        └── config.yml
```
