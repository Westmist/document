pipeline {

    agent any

    parameters {
        booleanParam(name: 'restart_nginx', defaultValue: false, description: '重启容器')
    }

    environment {
        // 本地代码路径
        code_path = "server"
        // 远程代码仓库地址
//        code_repo = "git@github.com:Westmist/document.git"
        code_repo = "https://github.com/Westmist/document.git"
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
                                                sourceFiles: "${code_path}/docs/.vuepress/dist/",
                                                remoteDirectory: '/data/vuepress/'
                                        )
                                ],
                        )]
                )
            }
        }

        stages {
            stage('重启容器') {
                when {
                    expression { params.restart_nginx == true }
                }
                steps {
                    // 使用 "Publish Over SSH" 插件传输文件到远程主机，并执行远程 Shell 命令并返回输出
                    script {
                        def result = sshPublisher(
                                publishers: [sshPublisherDesc(
                                        configName: 'centos',
                                        transfers: [
                                                sshTransfer(
                                                        execCommand: 'cd /data && docker-compose up -d vuepress',
                                                        returnStdout: true
                                                )
                                        ],
                                )]
                        )
                        echo "Shell 命令执行结果：${result}"
                    }
                }
            }
        }

    }
}




