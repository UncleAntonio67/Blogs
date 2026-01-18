#大数据 #虚拟机 #扩容

# 问题说明

在开展MapReduce数据倾斜实验时，运行报错提示磁盘空间不足**No space left on device**，需要进行虚拟机扩容，总结操作步骤如下：

```log
2026-01-17 16:50:01,010 INFO mapreduce.Job: Task Id : attempt_1768638883317_0001_m_000000_2, Status : FAILED [2026-01-17 16:49:59.317]Exception from container-launch. Container id: container_1768638883317_0001_01_000014 Exit code: 1 Exception message: /bin/mv: cannot move '/export/servers/hadoop-3.3.1/tmp/nm-local-dir/nmPrivate/application_1768638883317_0001/container_1768638883317_0001_01_000014/container_1768638883317_0001_01_000014.pid.exitcode.tmp' to '/export/servers/hadoop-3.3.1/tmp/nm-local-dir/nmPrivate/application_1768638883317_0001/container_1768638883317_0001_01_000014/container_1768638883317_0001_01_000014.pid.exitcode': No space left on device [2026-01-17 16:49:59.320]Container exited with a non-zero exit code 1. Error file: prelaunch.err. Last 4096 bytes of prelaunch.err : ln: failed to create symbolic link ‘job.xml’: No space left on device [2026-01-17 16:49:59.321]Container exited with a non-zero exit code 1. Error file: prelaunch.err. Last 4096 bytes of prelaunch.err : ln: failed to create symbolic link ‘job.xml’: No space left on device 2026-01-17 16:50:03,020 INFO mapreduce.Job: map 100% reduce 100% 2026-01-17 16:50:03,025 INFO mapreduce.Job: Job job_1768638883317_0001 failed with state FAILED due to: Task failed task_1768638883317_0001_m_000003 Job failed as tasks failed. failedMaps:1 failedReduces:0 killedMaps:0 killedReduces: 0 2026-01-17 16:50:03,071 INFO mapreduce.Job: Counters: 14 Job Counters Failed map tasks=9 Killed map tasks=3 Killed reduce tasks=1 Launched map tasks=12 Other local map tasks=8 Data-local map tasks=4 Total time spent by all maps in occupied slots (ms)=480982 Total time spent by all reduces in occupied slots (ms)=0 Total time spent by all map tasks (ms)=240491 Total vcore-milliseconds taken by all map tasks=240491 Total megabyte-milliseconds taken by all map tasks=246262784 Map-Reduce Framework CPU time spent (ms)=0 Physical memory (bytes) snapshot=0 Virtual memory (bytes) snapshot=0

```

# 操作步骤

## 1️⃣  确认磁盘使用情况

```shell
df -h
```

![[file-20260117174630977.png | 500]]

---
## 2️⃣虚拟机扩容

如有快照，须先删除快照，否则无法进行磁盘扩容，删除快照须在开机状态下，点快照管理进行删除。

磁盘扩容：
![[file-20260117174953783.png | 500]]

关机状态下，选择“硬盘”——>“扩展”，即可完成

开启虚拟机后，扩容不会立即生效，需要进行如下操作：
```shell
fdisk /dev/sda

Command (m for help): p       # 查看当前分区表
Command (m for help): d       # 删除分区
Partition number (1,2, default 2): 2   # 删除 sda2
Command (m for help): n       # 新建分区
Partition type: p             # 主分区
Partition number (2-4, default 2): 2  # 2号
First sector (保持原来的开始扇区): 2099200  # 或默认回车显示的值
Last sector (回车表示扩展到磁盘末尾): 回车
Command (m for help): t       # 修改类型
Partition number (2): 2
Hex code (type L to list codes): 8e   # Linux LVM
Command (m for help): w       # 写入并退出
#让内核识别新分区：
partprobe /dev/sda
#扩展 PV：
pvresize /dev/sda2
#扩展逻辑卷：
lvextend -l +100%FREE /dev/centos/root
#扩展文件系统（XFS）：
xfs_growfs /
#验证
df -h
```

查看效果：
![[file-20260117175359515.png | 500]]