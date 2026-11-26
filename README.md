### 构建：.\gradlew.bat build

### 游戏版本：Forge 1.20.1 

#### 服务端需装，客户端需装。配置服务端和客户端不互通

#### 模组目前为原版mc加入了几个命令来控制记录开启以及关闭导出等操作：

客户端本地记录：
- /log start 记录名：开启记录
- /log end：结束并导出记录到游戏目录/lore_record/<记录名>/
- /log pause：暂停记录
- /log continue：继续暂停的记录
- /log book 记录名：给予玩家该记录书

服务端记录，权限OP：
- /log global start 记录名：开启记录
- /log global end：结束并导出记录到游戏目录/lore_record/global/<记录名>/
- /log global pause：暂停记录
- /log global continue：继续暂停的记录
- /log global book 记录名：给予玩家该记录书

#### 闲着就维护，~~几个月不提交就是坑了~~
