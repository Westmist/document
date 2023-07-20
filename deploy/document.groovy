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
                                                sourceFiles: "${code_path}/docs/.vuepress/dist/",
                                                remoteDirectory: "/data/vuepress/"
                                        )
                                ],
                        )]
                )

                sshPublisher(
                        publishers: [sshPublisherDesc(
                                configName: 'centos',
                                transfers: [
                                        sshTransfer(
                                                cleanRemote: true, excludes: '',
                                                execCommand: '',
                                                noDefaultExcludes: false, patternSeparator: '[, ]+',
                                                remoteDirectory: '/data/vuepress/',
                                                sourceFiles: '${code_path}/docs/.vuepress/dist/')],
                                useWorkspaceInPromotion: false, verbose: true)])
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
                                        sshTransfer(cleanRemote: false, excludes: '',
                                                execCommand: 'cd /data && docker-compose restart vuepress',
                                                noDefaultExcludes: false, patternSeparator: '[, ]+',
                                                removePrefix: '', sourceFiles: '')], usePromotionTimestamp: false,
                                useWorkspaceInPromotion: false, verbose: true)])
            }
        }
    }
}