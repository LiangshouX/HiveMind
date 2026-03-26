## 沙箱镜像

#### 选项1：拉取所有镜像（推荐）

| Image                | Purpose            | When to Use                                                   |
|----------------------|--------------------|---------------------------------------------------------------|
| **Base Image**       | Python代码执行，shell命令 | 基本工具执行必需                                                      |
| **GUI Image**        | 计算机操作              | 当你需要图形操作页面时                                                   |
| **Filesystem Image** | 文件系统操作             | 当您需要文件读取/写入/管理时                                               |
| **Browser Image**    | Web浏览器自动化          | 当您需要网络爬取或浏览器控制时                                               |
| **Mobile Image**     | 移动端操作              | 当您需要操作移动端设备时                                                  |
| **Training Image**   | 训练和评估智能体           | 当你需要在某些基准数据集上训练和评估智能体时 （详情请参考 [训练用沙箱](training_sandbox.md)  ） |

> **镜像来源：阿里云容器镜像服务**
>
> 所有Docker镜像都托管在阿里云容器镜像服务(ACR)上，以在全球范围内实现可获取和可靠性。镜像从ACR拉取后使用标准名称重命名，以与AgentScope
> Runtime无缝集成。

```shell
# 基础镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest agentscope/runtime-sandbox-base:latest

# GUI镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest agentscope/runtime-sandbox-gui:latest

# 文件系统镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest agentscope/runtime-sandbox-filesystem:latest

# 浏览器镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest agentscope/runtime-sandbox-browser:latest

# 移动端镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-mobile:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-mobile:latest agentscope/runtime-sandbox-mobile:latest
```