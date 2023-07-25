## 搭建一个基于 Vuepress 的笔记网站
### 1、背景
平时习惯使用 Markdown 记录一些笔记，但又想要有网页一样的展示效果  
刚好 vuepress 就满足这个需求，可以专注于 Markdown 写作  
搭配 Jenkins 使用即可实现更新 md 文件，后自动生成、部署网站
### 2、创建一个 VuePress 工程
::: tip 环境依赖
需要Node.js版本大于等于8.6  
推荐使用yarn包管理器  
将 VuePress 安装为本地依赖`yarn add -D vuepress`
:::
工程目录树如下
```shell
.
├── docs
│   ├── page
│   │   └── vuepress.md
│   ├── README.md               由此文件生成 index.html
│   └── .vuepress
│       ├── config.js           配置文件的入口文件，导航栏、网站标题等都在此配置
│       ├── dist                默认的构建结果目录
│       └── public              存放图片等静态资源
│           ├── images       
│           │   └── hero.png
│           └── logo.png
└── package.json                配置启动和构建脚本           
```
::: tip 路径管理
docs/.vuepress/public下的文件构建后将输出到dist的根目录  
例如：上述`hero.png`在Markdown文件中的引用路径为`/images/hero.png`  
引用Markdown文件时以docs为根目录  
例如：上述`vuepress.md`在Markdown文件中的引用路径为`/page/vuepress.md`  
:::
### 3、准备 Jenkins 环境
需要在 Jenkins 服务器上安装 NodeJs 环境  
1. 上传 nodejs 到 Jenkins 的数据卷  
2. 配置 Jenkins 的全局属性（类似环境变量，每次执行构建时都可以使用）  
在 Dashboard --> Manage Jenkins --> Configure System 下找到 Global properties  
value 项中追加如下内容（可以替换为实际的nodejs路径）:  
```profile
# 包含 node、npm 和 yarn 包管理器  
:/var/jenkins_home/maven/apache-maven-3.6.3/bin:/var/jenkins_home/nodejs/node-v12.20.1-linux-x64/bin`  
```
```profile
# 包含全局安装的 NodeJs 依赖，Vuepress 将安装在此路径  
`:/var/jenkins_home/nodejs/node-v12.20.1-linux-x64/node_modules/.bin`
```
配置如图所示：
![Jenkins Env Config](/images/jenkins_env_config.png)
3. 需要新建一个 Jenkins 任务全局安装 Vuepress 依赖，执行如何 shell  
```profile
# 全局安装 Vuepress
yarn add -g vuepress
``` 
4. 安装 SSH 插件
需要安转的 Jenkins 插件
* Publish Over SSH 用于传送文件以及 SSH 登陆执行 Shell
* Git 用于拉取代码
[插件使用详见](/page/jenkins.md)
5. 配置部署服凭证  
在 Dashboard --> Manage Jenkins --> Configure System 下找到 Publish over SSH   
配置部署服主机信息、上传秘钥文件、配置秘钥路径口令、配置远程工作目录  
::: warning 
`Remote Directory` 若未配置则会以登陆用户的用户目录作为根目录
:::
Jenkins 配置下图所示
![Jenkins SSH Config](/images/jenkins_ssh_config.png)
### 4、集成 CI/CD
创建 Pipeline 工程
::: details document.groovy
```groovy
pipeline {

    agent any

    parameters {
        booleanParam(name: 'restart_nginx', defaultValue: false, description: '重启容器')
    }

    environment {
        // 本地代码路径
        code_path = "server"
        code_repo = "https://github.com/xxxx/xxxx.git"
        // 代码分支
        code_branch = "master"
    }

    stages {

        stage('更新代码') {
            steps {
                checkout([$class           : 'GitSCM', branches: [[name: "${code_branch}"]],
                          extensions       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${code_path}"]],
                          userRemoteConfigs: [[url: "${code_repo}"]]])
            }
        }

        stage('构建打包') {
            steps {
                sh """
                   cd ${code_path}
                   yarn install
                   vuepress build docs
                """
            }
        }

        stage('传输文件') {
            steps {
                sshPublisher(
                        publishers: [sshPublisherDesc(
                                configName: 'centos',
                                transfers: [
                                        sshTransfer(
                                                removePrefix : "${code_path}/docs/.vuepress/dist",
                                                sourceFiles: "${code_path}/docs/.vuepress/dist/**",
                                                remoteDirectory: "/data/vuepress/"
                                        )
                                ],
                        )]
                )
            }
        }

        stage('重启容器') {
            when {
                expression { params.restart_nginx == true }
            }

            steps {
                sshPublisher(
                        publishers: [sshPublisherDesc(
                                configName: 'centos',
                                transfers: [
                                        sshTransfer(
                                                execCommand: "cd /data && docker-compose restart vuepress",
                                                sourceFiles: "",
                                                remoteDirectory: ""
                                        )], verbose: true)])
            }
        }
    }
}
```
:::
使用 nginx 镜像构建容器
::: details docker-compose.yml
```yml
version: "2"
services:
  # 文档
  vuepress:
    image: nginx:latest
    container_name: vuepress
    restart: always
    volumes:
      - /data/vuepress/:/usr/share/nginx/html/
    ports:
      - 8000:80
```
:::
### 参考文档
[Vuepress 官方文档](https://vuepress.vuejs.org/zh/)  
[NodeJs 下载地址](https://nodejs.org/dist/)  