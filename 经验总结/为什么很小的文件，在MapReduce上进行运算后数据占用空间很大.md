# 问题说明

笔者在分析MapReduce数据倾斜问题时，生成了大概400MB的文件，但是通过运算后（reduce分别设置为1、5），整体磁盘空间占用了20G，占用空间如此之大，不免令人疑惑。



# 原因分析

## 1️⃣ 原始文件大小

`skew_data.txt`：

- 一亿条数字，每行是数字 + 换行符
- 假设平均每行 4 个字节数字 + 1 个换行符 = **5 字节/行**
- 一亿行 → 5 × 100,000,000 ≈ **500MB**

---

## 2️⃣ 运行后空间占用分析

### 1.Shuffle 和中间文件

- 当 MapReduce 作业执行时，Map 任务会把输出写到 **本地磁盘的 Map 输出目录**（默认在 `dfs.data.dir` 或 NodeManager 的 `local-dirs`）
- Map 输出会在 reduce 阶段被 shuffle 到对应的 Reduce 任务
- 因为你的数据严重倾斜（90% 是 key=500），一个 Reduce 任务需要处理大量数据
- 即便只有 1 个 Reduce，Map 输出文件仍然会保存 **每个 Map 的输出**
- Map 任务数通常取决于输入块大小（HDFS 默认 128MB/block）
- 所有 Map 输出累加可能比原始文件大很多：**压缩与重复写入增加了磁盘占用**

> 举例：
> - 1 亿行 → Map 输出 1GB 左右（无压缩）
> - Shuffle 临时文件 + Spill + Sort → 可能 10~12GB

### 2.Spill 文件

- Map 输出缓冲区默认大小有限（100MB 左右），缓冲区满后会 **spill 到本地磁盘**
- 你的 skew 数据太集中，单个 Reduce 对应 Map 输出很多，导致多次 spill
- 每次 spill 都会生成临时文件，**占用大量磁盘空间**

**Spill** 是指 **Map 任务在内存缓冲区满时，将部分中间数据写到本地磁盘的操作**。
换句话说：
- Map 输出会先写入 **内存缓冲区（Map Output Buffer）**
- 当缓冲区接近满了，Hadoop 会把数据 **溢写到本地磁盘**，这就是 **spill**

### 3.Reduce 输出

- Reduce 任务会从 shuffle 接收所有 Map 输出，写出最终文件
- 由于你的 key 极度集中（90% 是 500），对应 Reduce 输出可能很大
- 即便 Reduce 数量不同（1 或 5），中间文件大小也不会减少太多，磁盘占用仍高

## 3️⃣ 优化方案

### 1. 启用 Map 输出压缩

```xml
<property>
  <name>mapreduce.map.output.compress</name>
  <value>true</value>
</property>

<property>
  <name>mapreduce.map.output.compress.codec</name>
  <value>org.apache.hadoop.io.compress.SnappyCodec</value>
</property>

```

### 2. 增加 Map 缓冲区大小

```xml
<property>
  <name>mapreduce.task.io.sort.mb</name>
  <value>256</value>
</property>

```

### 3. **避免数据倾斜**

- 使用 **Combiner** 或自定义 **Partitioner** 分散热点 key
- 或者做 **随机前缀**，将大 key 拆分成多个 Reduce 处理
