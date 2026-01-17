# 问题说明

在增加Reduce个数后，频繁提示磁盘空间不足。通过`df -h`定位，随着 HDFS 数据增大，根分区被写满，`df -h` 显示 100% 使用率。

# 操作步骤

假设新磁盘是 `/dev/sdb`，挂载点为 `/data`，Hadoop 用户为 `hadoop`。

---
## 1️⃣ 停止 Hadoop 服务

在每个节点执行：

`stop-dfs.sh stop-yarn.sh`

确认 Hadoop 服务已停止。

---
## 2️⃣ 清空原 HDFS 数据

> **注意：这个操作不可恢复，原 HDFS 数据将丢失**

```shell
sudo rm -rf /export/servers/hadoop-3.3.1/tmp/dfs/data/*
sudo rm -rf /export/servers/hadoop-3.3.1/tmp/dfs/name/*
sudo rm -rf /export/servers/hadoop-3.3.1/tmp/yarn/*
sudo rm -rf /export/servers/hadoop-3.3.1/tmp/mapreduce/*
```

确认根分区和旧目录空间释放：

`df -h /`

---
## 3️⃣ 准备新磁盘挂载 `/data`

```shell
# 创建分区（假设 /dev/sdb）
sudo fdisk /dev/sdb   # n → p → 1 → 默认起始/结束 → w

# 格式化为 XFS
sudo mkfs.xfs /dev/sdb1

# 创建挂载点
sudo mkdir -p /data

# 挂载
sudo mount /dev/sdb1 /data

# 设置开机自动挂载
echo '/dev/sdb1 /data xfs defaults 0 0' | sudo tee -a /etc/fstab

# 给 Hadoop 用户权限
sudo chown -R hadoop:hadoop /data

```

---
## 4️⃣ 创建 Hadoop 所需目录

```shell
sudo mkdir -p /data/dfs/data
sudo mkdir -p /data/dfs/name
sudo mkdir -p /data/yarn/local
sudo mkdir -p /data/mapreduce

sudo chown -R hadoop:hadoop /data

```

---

## 5️⃣ 修改 Hadoop 配置挂到新盘

#### `hdfs-site.xml`：

```xml
<property>
  <name>dfs.datanode.data.dir</name>
  <value>file:///data/dfs/data</value>
</property>

<property>
  <name>dfs.namenode.name.dir</name>
  <value>file:///data/dfs/name</value>
</property>

```

#### `yarn-site.xml`：

```xml
<property>
  <name>yarn.nodemanager.local-dirs</name>
  <value>/data/yarn/local</value>
</property>

<property>
  <name>mapreduce.cluster.local.dir</name>
  <value>/data/mapreduce</value>
</property>

```

---

## 6️⃣ 格式化 NameNode

因为已经清空 HDFS，需要重新初始化：

`hdfs namenode -format`

---

## 7️⃣ 启动 Hadoop 服务

`start-dfs.sh start-yarn.sh`

检查 HDFS 状态：

`hdfs dfsadmin -report hdfs dfs -ls /`

---

查看效果：
![[file-20260117225520619.png | 500]]
# 总结

- **挂在根分区的问题**
    - 根分区 `/` 主要用于系统文件、日志和应用程序，不适合存储大容量 Hadoop 数据。
    - 当 HDFS 写入数据超过可用空间时，系统和 Hadoop 服务都会受到影响。
        
- **HDFS 默认目录与磁盘空间绑定**
    - Hadoop 使用 `dfs.data.dir` 和 `dfs.name.dir` 配置的路径来存储数据。
    - 如果这些路径挂在根分区，一旦写满，就会导致 MapReduce 作业失败、MapTask 输出溢出、NameNode 进入安全模式。
        
- **安全模式触发机制**
    - NameNode 启动时会进入 Safe Mode，检查 DataNode 报告的块副本数是否满足最小副本比例。
    - 如果磁盘满或 DataNode 不可用，会一直停留在安全模式，无法写入或删除数据。


| 项目                    | 原先（根分区）                                   | 现在（独立磁盘 `/data`）                 | 说明                     |
| --------------------- | ----------------------------------------- | -------------------------------- | ---------------------- |
| HDFS 数据目录             | /export/servers/hadoop-3.3.1/tmp/dfs/data | /data/dfs/data                   | 独立磁盘存 HDFS 数据，根分区不再受占用 |
| NameNode 目录           | /export/servers/hadoop-3.3.1/tmp/dfs/name | /data/dfs/name                   | 元数据独立                  |
| YARN / MapReduce 临时目录 | 根分区                                       | /data/yarn/local、/data/mapreduce | 避免 spill 文件填满系统盘       |
| 根分区安全                 | 容易满                                       | 保持系统稳定                           | 系统与 Hadoop 数据隔离，提高可靠性  |
| 扩展能力                  | 受限于根分区大小                                  | 新磁盘可扩容                           | 可通过挂载更大磁盘增加存储容量        |

| 项目     | 根分区 `/`                                             | 独立挂载 `/data`                         |
| ------ | --------------------------------------------------- | ------------------------------------ |
| 空间用途   | 系统、日志、应用、缓存、Hadoop 数据都在同一块                          | 只有 Hadoop 数据，占满不会影响系统                |
| 系统敏感性  | 根分区满了可能导致系统进程、SSH、yum 等操作失败                         | 系统根分区剩余空间充足，操作系统稳定                   |
| 清理难度   | Hadoop 占满 `/` 时，要小心不要误删系统文件                         | `/data` 只放 Hadoop，可直接删除数据目录          |
| 安全模式影响 | Hadoop 写满根分区 → DataNode 无法正常报告块信息 → NameNode 长期安全模式 | 只要 `/data` 有空间，Hadoop 正常工作，安全模式可自动退出 |
| 容错性    | 写满 `/` → 系统可能无法启动或 Hadoop 报错                        | 写满 `/data` → 系统不受影响，只影响 Hadoop       |