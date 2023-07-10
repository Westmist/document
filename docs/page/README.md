## 快速启动
远程仓库地址：  
代码：http://192.168.4.32:8081/zssq/server  
csv配置表：https://192.168.5.142/svn/hero/data/miracle-data

vm参数：  
-XX:MetaspaceSize=256m  
-XX:MaxMetaspaceSize=256m  
-XX:ReservedCodeCacheSize=128m  
-XX:+PrintCommandLineFlags  
-Xmx2048m  
-Dgame.script.dev=true  
-XX:-OmitStackTraceInFastThrow  
-XX:ReservedCodeCacheSize=120m  
-Dgame.script.dev=true  
-Dlog4j.configurationFile=./conf/log4j2.xml  
-Dgame.script.findSuperClassInterface=true    

程序参数：./conf/config.properties 1.0.0  

入口模块：game-all  
启动类：com.sh.game.Startup

日志配置文件：log4j2.xml  
程序配置文件: config.properties  
连接池配置文件：gameds.properties  
版本号：1.0.0  

config.properties中的主要参数  
serverId: 服务器id(约定值，客户端选择区服列表时可在客户端控制台内查看)  
platformId：平台id(可自定义)  
remoteAddress：跨服地址（dev跨服ip: 192.168.10.97:9311）   
configDataPath: csv配置文件路径  
openTime：开服日期格式为 YY-MM-DD hh:mm:ss 可自行修改  
gameDbConfigPath: 关联游戏数据库连接池配置文件路径（gameds.properties）  

## 自动建表
项目启动时将扫描包含注解`@PersistDaoTemplate(PersistDao.class)`的类作为存储类
根据存储类自动创建数据表  
具体的DLL语句由对应的`PersistDao`类的子类决定  
只有数据库中不存在的数据表才会被自动建表创建，表名为存储类父类的`PersistPrefix`注解的`value`+存储类名(驼峰转下划线)
  







## 脚本引擎


## 定时任务
### 服务器心跳
### 玩家心跳

## 配置表处理

## 游戏运行监控
### JVM和在线人数
### 日志和ESK

## 功能开发
### 协议生成
### 模块划分

## 任务系统

## 条件校验

## 活动系统

## 副本系统

## 游戏Al

## 线程模型

### 线程间通信

## 项目构建与部署


## 游戏后台（http服务器）


## 强制规范
<!-- // TODO 存储类命名规范 -->

