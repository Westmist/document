pipeline {

    agent any

    parameters {
        choice(name: 'dockerDeploySer', choices: ['172.17.0.1'], description: 'docker部署服')
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
                sh """
                   cd ${code_path}/docs/.vuepress/dist
                   rsync -zr --delete-after ./ root@${dockerDeploySer}:/data/vuepress/
                """
            }
        }

        stage('重启容器') {
            when {
                expression { params.restart_nginx == true }
            }
            steps {
                sh """
                   cd /data
                   docker-compose up -d vuepress
                """
            }
        }

    }
}




