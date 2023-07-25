## Ansible
### 1、简介
![Ansible](/images/ansible.png)   
Ansible是一种自动化工具，用于配置管理、应用部署和任务自动化。它可以帮助您在大规模的计算机系统上自动执行各种任务，包括配置管理、应用程序部署、编排和协调操作等   
> 主要文件  
> 1、inventory.ini 默认文件为 hosts 定义被控端列表   
> 2、playbook.yaml 执行剧本定义任务和待执行的被控端组
### 2、安装 Ansible （只需要主控端安装）
```shell
# 新增 epel-release 第三方套件来源
yum install -y epel-release
# 安装 Ansible
yum install -y ansible
```

> 如果未找到 Ansible 包则执行如下操作
```shell
# 备份 repo 文件
cp /etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo.bak
```
```ini
# CentOS-Base.repo 文件中添加 Ansible 源
[ansible]
name=Ansible
baseurl=https://releases.ansible.com/ansible/rpm/release/epel-7-x86_64/
gpgcheck=1
gpgkey=https://releases.ansible.com/keys/ansible_release.pub
enabled=1
```
```shell
# 更新 yum 源
yum clean all
# 安装 Ansible
yum install ansible

# 查看 Ansible 版本信息
ansible --version
```
![Ansible_version](/images/ansible_version.png)
### 3、配置 hosts (被控端清单)
> 默认的 hosts 文件在 ansible.cfg 同级目录下
> 主控端与被控端配置 SSH 免密登陆
```ini
# hosts 文件中增加如下配置
[app]
192.168.11.128
192.168.11.129
``` 
> 测试主控端与被控端的连接性
```shell
ansible all -m ping
```
![Ansible_ping](/images/ansible_ping.png)
### 4、使用 ansible-playbook 执行多任务
> 编写 Playbook (Ansible 剧本) hello-world.yaml
```yaml
# app 组下的被控端将执行下述的三个任务
# 1、使用 debug 模块打印 Hello, World!
# 2、使用 shell 模块获取 docker 版本并将输出结果注册到`docker_version_output`变量中
# 3、使用 debug 模块输出变量`docker_version_output`
---
- name: Print Hello World and Docker Version
  hosts: app # hosts 中配置的组
  gather_facts: false

  tasks:
    - name: Print Message
      debug:
        msg: "Hello, World!"

    - name: Get Docker Version
      shell: docker --version
      register: docker_version_output

    - name: Print Docker Version
      debug:
        var: docker_version_output.stdout_lines
```
```shell
# 执行 Playbook 默认使用 host
ansible-playbook hello-world.yaml
# 指定 inventory
ansible-playbook hello-world.yaml -i inventory.ini
```
执行结果
![Ansible_playbook](/images/ansible_playbook.png)
### 参考文档
[Ansible 官方文档](https://docs.ansible.com/ansible/latest/getting_started/index.html)