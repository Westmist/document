pipeline {

    agent any

    parameters {
        booleanParam(name: 'restart_nginx', defaultValue: false, description: '重启容器')
    }

    environment {
        // compose 文件
        compose_path = "/home/compose/vuepress-docker-compose.yaml"
        // 远程代码仓库地址
        code_repo = "https://github.com/Westmist/document.git"
        // 代码分支
        code_branch = "master"
    }

    

    stages {

        stage('更新代码') {
            steps {
                script {
                    // 设置 Git 的代理，宿主机的 docker0 网卡地址
                    sh 'git config --global http.proxy http://172.17.0.1:7890'
                    sh 'git config --global https.proxy http://172.17.0.1:7890'
           
                    // 执行 Git Checkout
                    checkout([$class           : 'GitSCM', branches: [[name: "${code_branch}"]],
                    extensions       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: ""]],
                    userRemoteConfigs: [[url: "${code_repo}"]]])
                }
            }
        }

        stage('构建打包') {
            // 使用容器代理构建过程
            agent {
                docker { 
                    image 'vuepress-build'
                    // 复用 Jenkins 节点的工作空间
                    reuseNode true
                }
            }

            steps {
                sh """
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
                                                removePrefix : "docs/.vuepress/dist",
                                                sourceFiles: "docs/.vuepress/dist/**",
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
                                                execCommand: "docker-compose -f ${compose_path} restart vuepress",
                                                sourceFiles: "",
                                                remoteDirectory: ""
                                        )], verbose: true)])
            }
        }
    }
}