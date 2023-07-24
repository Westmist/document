## 项目容器化
### 一、环境准备
#### 1、Docker 安装
卸载原有 docker
```shell
yum remove docker \
    docker-client \
    docker-client-latest \
    docker-common \
    docker-latest \
    docker-latest-logrotate \
    docker-logrotate \
    docker-engine
```
安装 docker
```shell
yum install -y yum-utils
# 增加 yum 软件源
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
# 安装 docker
yum install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
# 启动 docker 服务
systemctl start docker
```
#### 2、修改配置
docker守护进程配置文件默认路径为 `/etc/docker/daemon.json`, 如无文件可手动创建   
`data-root`配置为 docker 的根目录，具名卷目录默认在根目录的 volume 文件夹下   
`insecure-registries`配置可以忽略https安全策略限制，使用http请求拉取指定主机的镜像   
```
{
  "data-root": "/data/docker",
  "insecure-registries":["192.168.xxx.xxx:port"]
}
```
#### 3、Docker Compose 安装
```shell
# 将二进制文件安转到 /usr/local/bin/ 目录下
sudo curl -L https://github.com/docker/compose/releases/download/v2.19.1/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
# 提权
sudo chmod +x /usr/local/docker-compose
# 创建软链接
sudo  ln -s /usr/local/docker-compose /usr/bin/docker-compose
# 卸载时只需要删除二进制文件以及移除软链接
rm /usr/bin/docker-compose
rm /usr/local/docker-compose
```
#### 4、验证版本
![Docker](/images/docker_version.png)
### 二、项目容器化设计
* 需要进行容器化的项目为一个分区分服手游项目   
* 一组服务器由4个进程（游戏服-game、跨服场景服-scene、匹配服-match和战斗服-battle）组成   
* 策划配置表使用数据卷绑定，jar包、进程配置文件和启动脚本打入镜像中  
* 上述4个进程编排成一组服务，使用同一个桥接网路、同一个具名卷挂载日志文件   
::: tip 拆分成下述的两个 Jenkins 工程实现 CI/CD 流程
 1. 本地服主要步骤：代码MR -> 拉取代码和相关配置文件 -> 打包 -> 构建镜像 -> 上传镜像   
 2. DEV 服主要步骤：提交策划配置表 -> 配置表检验 -> 更新镜像 -> 重启镜像   
:::
本地服：即打出jar包后提交到svn以供策划本地启动使用   
只有代码有变更才会构建镜像和提交jar包到svn   
### 三、项目改造
1、在代码工程中增加 docker 相关配置文件   
::: details 目录树
```shell
docker                               目录树
├── bin                              存放各个进程启动脚本
│   ├── battle.sh
│   ├── game.sh
│   ├── match.sh
│   └── scene.sh
├── build                            镜像构建文件
│   ├── battle.Dockerfile
│   ├── game.Dockerfile
│   ├── match.Dockerfile
│   └── scene.Dockerfile
├── conf                             进程配置文件
│   ├── game
│   │   ├── config.properties
│   │   ├── gameds.properties
│   │   └── log4j2-game.xml
│   ├── match
│   │   ├── log4j2-match.xml
│   │   ├── matchcfg.properties
│   │   └── redisCfg.properties
│   ├── battle
│   │   ├── log4j2-battle.xml
│   │   └── battle.properties
│   └── scene
│       ├── log4j2-scene.xml
│       ├── mapcfg.properties
│       └── redisCfg.properties
└── docker-compose.yml               编排文件
```
:::
2、Dockerfile 和启动脚本
::: details 启动脚本-game.sh
```shell
ulimit -n 65535

# 服务名
SERVER_NAME=game
MAIN_CLASS=com.xx.game.GameBootstrap
CONF=/server/conf/config.properties

# 进程依赖
LIBS=/server/libs/*
CORE=/server/core/*

# 版本号
VERSION=0.0.1

# JVM配置
OPTS="
 -XX:MetaspaceSize=256m
 -XX:MaxMetaspaceSize=256m
 -XX:ReservedCodeCacheSize=128m
 -XX:+PrintCommandLineFlags
 -XX:-OmitStackTraceInFastThrow

 -Xmx5g
 -Xms5g

 -verbose:gc
 -XX:+PrintGCDateStamps
 -XX:+PrintGCDetails
 -XX:+PrintGCApplicationStoppedTime
 -Xloggc:./logs/sgc.log
 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heapdump.hprof

 -Dfile.encoding=UTF-8
 -Dgame.script.dev=true
 -Dgame.script.findSuperClassInterface=true

 -Dlog4j.configurationFile="./conf/log4j2-game.xml"
"

#启动游戏服务器
function startServer()
{
  nohup java -server ${OPTS} -cp ${LIBS}:${CORE} ${MAIN_CLASS} ${CONF} ${VERSION} ${SERVER_NAME}
  # 关联日志输出
  ln -sf /dev/stdout /server/logs/game.info.out
  ln -sf /dev/stderr /server/logs/game.error.out
}
startServer;
```
:::
::: details 基础镜像-base.Dockerfile
```shell
FROM centos:7.6.1810

ADD jdk-8u371-linux-x64.tar.gz /usr/java/

# 增加 Arthas
COPY --from=hengyunabc/arthas:latest /opt/arthas /opt/arthas

#增加 Netcat
RUN yum install -y nc

# 增加容器内中⽂支持
ENV LANG="en_US.UTF-8"

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# JAVA 环境变量
ENV JAVA_HOME /usr/java/jdk1.8.0_371
ENV PATH ${PATH}:${JAVA_HOME}/bin
```
:::
::: details 游戏服镜像-game.Dockerfile
```Dockerfile
FROM 192.168.xxx.xxx:port/linux-sh/centos7.6-jdk8:latest

RUN mkdir /server
WORKDIR /server

# 启动脚本
COPY ./docker/bin/game.sh ./bin/game.sh
# 配置文件
COPY ./docker/conf/game/ ./conf/
# core 和 lib 依赖
COPY ./game ./
EXPOSE 8848
ENTRYPOINT ["/bin/bash", "bin/game.sh"]
```
:::
::: details 编排文件-docker-compose.yml
```yaml
# 经过化名处理，mx代指项目名
version: "2"
services:
  mx-game:
    image: 192.168.xxx.xxx:port/studio-dev/mx-game
    restart: always
    container_name: mx_game
    cap_add:
      - SYS_PTRACE
      - SYS_ADMIN
    volumes:
      # 日志文件
      - logs:/server/logs/
      # 配置表
      - /data/server/mx/data/:/server/data/
    ports:
      # 客户端
      - 8848:8848
    networks:
      - mx-network
    healthcheck:
      test: [ "CMD", "nc", "-zv", "localhost", "8848" ]
      interval: 30s
      timeout: 2s
      retries: 5
  mx-scene:
    image: 192.168..xxx.xxx:port/studio-dev/mx-scene
    restart: always
    container_name: mx_scene
    cap_add:
      - SYS_PTRACE
      - SYS_ADMIN
    volumes:
      # 日志文件
      - logs:/server/logs/
      # 配置表
      - /data/server/mx/data/:/server/data/
    networks:
      - mx-network
    healthcheck:
      test: [ "CMD", "nc", "-zv", "localhost", "8858" ]
      interval: 20s
      timeout: 2s
      retries: 3
  mx-match:
    image: 192.168.xxx.xxx:port/studio-dev/mx-match
    restart: always
    container_name: mx_match
    cap_add:
      - SYS_PTRACE
      - SYS_ADMIN
    volumes:
      # 日志文件
      - logs:/server/logs/
    networks:
      - mx-network
    healthcheck:
      test: [ "CMD", "nc", "-zv", "localhost", "8868" ]
      interval: 10s
      timeout: 2s
      retries: 3
  mx-battle:
    image: 192.168.xxx.xxx:port/studio-dev/mx-battle
    restart: always
    container_name: mx_battle
    cap_add:
      - SYS_PTRACE
      - SYS_ADMIN
    volumes:
      # 日志文件
      - logs:/server/logs/
      # 配置表
      - /data/server/mx/data/:/server/data/
    networks:
      - mx-network
    healthcheck:
      test: [ "CMD", "nc", "-zv", "localhost", "8878" ]
      interval: 15s
      timeout: 2s
      retries: 3
volumes:
  logs:
networks:
  mx-network:
    driver: bridge
```
:::
4、Jenkins Pipeline 脚本
::: details 本地服-local.groovy
```groovy
pipeline {

    agent any

    environment {
        // 代码检出到 Jenkins workspace 下的路径
        code_path = "code"
        // 配置表检出到 Jenkins workspace 下的路径
        release_data_path = "release/data"
        // 构建结果(jar包) 将移动到该目录，然后提交到svn
        release_server_path = "release/server"

        // 远程代码仓库地址
        code_repo = "git@192.168.xxx.xxx:game_server/mx.git"
        // 代码分支
        code_branch = "develop"
        // 本地服svn地址
        data_repo = "svn://192.168.xxx.xxx/svn/mx"

        // 镜像名前缀
        image_prefix = "mx"
        // docker 构建上下文路径，代码工程下的目录
        image_context = "server/release"
        // docker 仓库
        docker_registry = "192.168.xxx.xxx:port/studio-dev"
    }


    stages {

        stage('资源准备') {
            failFast true
            parallel {
                stage('拉取代码库') {
                    steps {
                        echo '更新代码库'
                        checkout([$class           : 'GitSCM', branches: [[name: "${code_branch}"]],
                                  extensions       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${code_path}"]],
                                  userRemoteConfigs: [[url: "${code_repo}"]]])

                    }
                }

                stage('拉取svn资源') {
                    steps {
                        echo '更新数据表到游戏库'
                        checkout([$class                : 'SubversionSCM', additionalCredentials: [],
                                  excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '',
                                  filterChangelog       : false, ignoreDirPropChanges: false, includedRegions: '',
                                  locations             : [
                                          [cancelProcessOnExternalsFail: true, credentialsId: 'svn_suxun', depthOption: 'infinity',
                                           ignoreExternalsOption       : true, local: "${release_data_path}", remote: "${data_repo}/data"],
                                          [cancelProcessOnExternalsFail: true, credentialsId: 'svn_suxun', depthOption: 'infinity',
                                           ignoreExternalsOption       : true, local: "${release_server_path}", remote: "${data_repo}/server"]],
                                  quietOperation        : true, workspaceUpdater: [$class: 'CheckoutUpdater']])
                    }
                }
            }
        }

        stage('打包所有代码') {
            steps {
                sh """
                   cd ${code_path}
                   mvn clean package -DskipTests
                """
            }
        }

        stage('复制Jar包') {
            steps {
                sh """
                   rsync -r --exclude=docker ${code_path}/release/* ${release_server_path}
                """
            }
        }

        stage('提交Jar包') {
            steps {
                sh """
                   cd  ${release_server_path}
                   svn upgrade
                   svn add . --force 
                   svn commit -m '自动更新'
                """
            }
        }

        stage("构建镜像-docker") {
            failFast true
            parallel {
                stage('构建游戏服镜像') {
                    steps {
                        script {
                            env.game_image_name = "${image_prefix}" + "-game:latest"
                        }
                        sh '''
                           cd ${image_context}
                           docker build -f ./docker/build/game.Dockerfile -t ${game_image_name} .
                           docker tag ${game_image_name} ${docker_registry}/${game_image_name}
                           docker push ${docker_registry}/${game_image_name}
                        '''
                    }
                }

                stage('构建场景服镜像') {
                    steps {
                        script {
                            env.scene_image_name = "${image_prefix}" + "-scene:latest"
                        }
                        sh '''
                           cd ${image_context}
                           docker build -f ./docker/build/scene.Dockerfile -t ${scene_image_name} .
                           docker tag ${scene_image_name} ${docker_registry}/${scene_image_name}
                           docker push ${docker_registry}/${scene_image_name}
                        '''
                    }
                }


                stage('构建匹配服镜像') {
                    steps {
                        script {
                            env.match_image_name = "${image_prefix}" + "-match:latest"
                        }
                        sh '''
                           cd ${image_context}
                           docker build -f ./docker/build/match.Dockerfile -t ${match_image_name} .
                           docker tag ${match_image_name} ${docker_registry}/${match_image_name}
                           docker push ${docker_registry}/${match_image_name}
                        '''
                    }
                }

                stage('构建战斗服镜像') {
                    steps {
                        script {
                            env.battle_image_name = "${image_prefix}" + "-battle:latest"
                        }
                        echo '开始构建战斗服镜像'
                        sh '''
                           cd ${image_context}
                           docker build -f ./docker/build/battle.Dockerfile -t ${battle_image_name} .
                           docker tag ${battle_image_name} ${docker_registry}/${battle_image_name}
                           docker push ${docker_registry}/${battle_image_name}
                        '''
                    }
                }
            }
        }
    }
}
```
:::
::: details DEV-dev.groovy
```groovy
pipeline {
    agent any

    /**
     * 选择性参数
     */
    parameters {
        choice(name: 'dockerDeploySer', choices: ['192.168.xxx.xxx'], description: 'docker部署服')
    }

    environment {

        // 本地代码检出目录
        code_path = "code"
        // 本地配置表检出目录
        data_path = "data"

        // 远程代码仓库地址
        code_repo = "git@192.168.xxx.xxx:game_server/mx.git"
        // 代码分支
        code_branch = "develop"
        // 本地服svn地址
        data_repo = "svn://192.168.xxx.xxx/svn/mx"

        // docker 构建上下文路径
        image_context = "server/release"

        // docker部署地址
        deploy_path = "/data/server/mx"
    }


    stages {

        stage("更新资源") {
            failFast true

            parallel {
                stage('更新SCM库') {
                    steps {
                        echo '更新代码库'
                        checkout([$class           : 'GitSCM', branches: [[name: "${code_branch}"]],
                                  extensions       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${code_path}"]],
                                  userRemoteConfigs: [[url: "${code_repo}"]]])
                    }
                }
                stage('更新数据表') {
                    steps {
                        echo '更新数据表到游戏库'
                        checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '',
                                  excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false,
                                  ignoreDirPropChanges: false, includedRegions: '',
                                  locations: [[cancelProcessOnExternalsFail: true, credentialsId: 'svn_suxun',
                                               depthOption                 : 'infinity', ignoreExternalsOption: true, local: "${data_path}",
                                               remote                      : "${data_repo}"]], quietOperation: true,
                                  workspaceUpdater: [$class: 'CheckoutUpdater']])
                    }
                }
            }
        }


        stage('检验配置表') {
            steps {
                script {
                    sh """
                       cd ${code_path}
                       mvn test -q -pl com.xxx.xxx:game-server -am -Dtest.data.path=${env.WORKSPACE}/${data_path}/
                    """
                }
            }
        }

        stage('传输配置表-docker') {
            steps {
                echo '开始传输配置文件'
                sh """
                   rsync ${image_context}/docker/docker-compose.yml root@${dockerDeploySer}:${deploy_path}/
                   rsync -zr --delete-after --progress ${data_path}/ root@${dockerDeploySer}:${deploy_path}/data/
                """
            }
        }

        stage('重启服务器-docker') {
            steps {
                echo '重启容器'
                sh "ssh root@${dockerDeploySer} \"cd ${deploy_path} && docker-compose pull && docker-compose up -d\""
            }
        }
    }
}
```
:::
::: tip CI/CD
jenkins服务器上配置部署服的SSH私钥后即可实现全自动的 CI/CD 流程
将在 Pipeline 步骤中传输配置表和 compose 文件到部署服（如无目录将自动创建）
:::