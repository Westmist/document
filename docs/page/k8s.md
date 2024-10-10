### Kubernetes 简明教程

#### 1、使用 Kubeadm 安装 Kubernetes 集群 




#### 1、增加 k8s 源
```shell
# 使用 cat 命令将内容写入 kubernetes.repo 文件
cat <<EOF | sudo tee /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg https://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg
EOF
```
#### 2、安装 kubeadm 和 kubectl
```shell
yum install -y kubeadm kubectl
```
#### 3、在主节点初始化 k8s
```shell
kubeadm init --pod-network-cidr=10.244.0.0/16
```
