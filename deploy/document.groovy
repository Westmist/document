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