# 一.拿来就用的企业级解决方案

## 1.剖析小文件问题与企业级解决方案

### 核心知识

- 小文件问题：Hadoop在小文件的处理上不但效率低下，而且十分消耗内存资源，解决方案是使用容器，包括SequenceFile和MapFile（排序后的SequenceFile）

| **特性**   | **SequenceFile**                   | **MapFile**                             |
| -------- | ---------------------------------- | --------------------------------------- |
| **定义**   | 由二进制键值对（Key-Value Pairs）组成的顺序记录文件。 | 已排序的 SequenceFile，带有索引以便快速查找。           |
| **组成结构** | 只有一个文件。                            | 包含一个文件夹，下有两个文件：`data`（数据）和 `index`（索引）。 |
| **排序要求** | **不要求**。记录按写入顺序存储。                 | **必须排序**。写入时 Key 必须按顺序排列。               |
| **检索方式** | 只能**顺序读取**。不支持随机访问。                | 支持**随机访问**。通过索引快速定位 Key。                |
| **可搜索性** | 差。找特定 Key 需要遍历整个文件。                | 强。适合需要频繁查找特定记录的场景。                      |
| **存储开销** | 较低。                                | 略高（因为需要额外存储索引文件）。                       |
| **典型用途** | 作为 MapReduce 阶段间的中间数据存储；合并小文件。     | 需要按 Key 快速查找数据的场景；作为简易的键值数据库。           |

如果要计算包括SequenceFile，通过`job.setInputFormatClass(SequenceFileInputFormat.class);`

### 实验操作

编写SequenceFile案例

```java
package com.imooc.mr;  
  
import org.apache.commons.io.FileUtils;  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.FileSystem;  
import org.apache.hadoop.fs.FileUtil;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.SequenceFile;  
import org.apache.hadoop.io.Text;  
import org.checkerframework.checker.units.qual.C;  
  
import java.io.File;  
import java.io.IOException;  
  
public class SmallFileSeq {  
  
    public static void main(String[] args) throws Exception {  
        write("D:\\Project\\Java\\db_hadoop\\test","/seqFile");  
        read("/seqFile");  
    }  
    /**  
     * 生成sequencefile文件  
     * @param inputDir  输入目录  
     * @param outputFile 输出文件  
     * @throws IOException  
     */    public static void write(String inputDir,String outputFile) throws IOException {  
        Configuration conf = new Configuration();  
        conf.set("fs.defaultFS","hdfs://node01:9000");  
  
        FileSystem fileSystem = FileSystem.get(conf);  
        fileSystem.delete(new Path(outputFile),true);  
  
        /*  
        三个元素，1是输出路径，2、3是kv  
         */        SequenceFile.Writer.Option[] opts = new SequenceFile.Writer.Option[]{  
                SequenceFile.Writer.file(new Path(outputFile)),  
                SequenceFile.Writer.keyClass(Text.class),  
                SequenceFile.Writer.valueClass(Text.class),  
  
        };  
  
        SequenceFile.Writer writer = SequenceFile.createWriter(conf,opts);  
  
        //指定需要压缩的文件目录  
        File inputDirPath = new File(inputDir);  
        if(inputDirPath.isDirectory()){  
            File[] files = inputDirPath.listFiles();  
            for (File file:files){  
                String content = FileUtils.readFileToString(file,"UTF-8");  
                String fileName = file.getName();  
                Text key = new Text(fileName);  
                Text value = new Text(content);  
                writer.append(key,value);  
            }  
        }  
        writer.close();  
  
    }  
  
    /**  
     * 读取文件  
     * @param inputFile 文件路径  
     * @throws Exception  
     */    private static void read(String inputFile) throws Exception{  
        Configuration conf = new Configuration();  
        conf.set("fs.defaultFS", "hdfs://node01:9000");  
        SequenceFile.Reader reader = new SequenceFile.Reader(conf, SequenceFile.Reader.file(new Path(inputFile)));  
        Text key = new Text();  
        Text value = new Text();  
        while(reader.next(key,value)){  
            System.out.print("文件名："+key.toString()+",");  
            System.out.println("文件内容："+value.toString()+",");  
        }  
        reader.close();  
    }  
}
```

运行效果：
![[file-20260115220938270.png]]

编写MapFile文件
```java
package com.imooc.mr;  
  
import org.apache.commons.io.FileUtils;  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.FileSystem;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.MapFile;  
import org.apache.hadoop.io.SequenceFile;  
import org.apache.hadoop.io.Text;  
  
import java.io.File;  
import java.io.IOException;  
  
/**  
 * 小文件解决方案之mapfile  
 */public class SmallFileMap {  
  
    public static void main(String[] args) throws Exception {  
        write("D:\\Project\\Java\\db_hadoop\\test","/mapFile");  
        read("/mapFile");  
    }  
  
    /**  
     * 生成mapfile文件  
     * @param inputDir  输入目录  
     * @param outputDir 输出目录  
     * @throws IOException  
     */    public static void write(String inputDir,String outputDir) throws IOException {  
        Configuration conf = new Configuration();  
        conf.set("fs.defaultFS","hdfs://node01:9000");  
  
        FileSystem fileSystem = FileSystem.get(conf);  
        fileSystem.delete(new Path(outputDir),true);  
  
        SequenceFile.Writer.Option[] opts = new SequenceFile.Writer.Option[]{  
                MapFile.Writer.keyClass(Text.class),  
                MapFile.Writer.valueClass(Text.class),  
  
        };  
        MapFile.Writer writer = new MapFile.Writer(conf,new Path(outputDir),opts);  
  
        //指定需要压缩的文件目录  
        File inputDirPath = new File(inputDir);  
        if(inputDirPath.isDirectory()){  
            File[] files = inputDirPath.listFiles();  
            for (File file:files){  
                String content = FileUtils.readFileToString(file,"UTF-8");  
                String fileName = file.getName();  
                Text key = new Text(fileName);  
                Text value = new Text(content);  
                writer.append(key,value);  
            }  
        }  
        writer.close();  
  
    }  
  
    /**  
     * 读取文件  
     * @param inputDir 文件路径  
     * @throws Exception  
     */    private static void read(String inputDir) throws Exception{  
        Configuration conf = new Configuration();  
        conf.set("fs.defaultFS", "hdfs://node01:9000");  
        MapFile.Reader reader = new MapFile.Reader(new Path(inputDir),conf);  
        Text key = new Text();  
        Text value = new Text();  
        while(reader.next(key,value)){  
            System.out.print("文件名："+key.toString()+",");  
            System.out.println("文件内容："+value.toString()+",");  
        }  
        reader.close();  
    }  
}
```

运行效果：
![[file-20260115223841674.png]]

编写WordCountJobSeq
```java
package com.imooc.mr;  
  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.LongWritable;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapreduce.Job;  
import org.apache.hadoop.mapreduce.Mapper;  
import org.apache.hadoop.mapreduce.Reducer;  
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;  
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
  
import java.io.IOException;  
  
/**  
 * 读取SeqenceFile文件  
 */  
public class WordCountJobSeq {  
  
    public static class MyMapper extends Mapper<Text, Text, Text, LongWritable> {  
        Logger logger = LoggerFactory.getLogger(MyMapper.class);  
        /**  
         * 实现map函数  
         * 接收<k1,v1>输出<k2，v2>  
         * @param key  
         * @param value  
         */  
        @Override  
        protected void map(Text key, Text value, Context context) throws IOException, InterruptedException {  
            logger.info("<k1,v1>=<"+key.toString() + "," + value.toString()+">");  
            String[] words = value.toString().split(" ");  
            for (String word : words) {  
                Text k2 = new Text(word);  
                LongWritable v2 = new LongWritable(1L);  
                context.write(k2, v2);  
            }  
        }  
    }  
  
    public static class MyReduce extends Reducer<Text, LongWritable, Text, LongWritable> {  
        Logger logger = LoggerFactory.getLogger(MyReduce.class);  
        /**  
         * 针对<k2，v2>累加求和转化为<k3，v3>  
         * @param key  
         * @param values  
         * @throws IOException  
         */        @Override  
        protected void reduce(Text key, Iterable<LongWritable> values, Reducer<Text, LongWritable, Text, LongWritable>.Context context) throws IOException, InterruptedException {  
            long sum = 0L;  
            for(LongWritable value :values){  
                logger.info("<k2,v2>=<"+key.toString() + "," + value.get()+">");  
                sum += value.get();  
            }  
            Text k3 = key;  
            LongWritable v3 = new LongWritable(sum);  
            logger.info("<k3,v3>=<"+k3.toString() + "," + v3.get()+">");  
            context.write(k3,v3);  
        }  
    }  
  
    /**  
     * 组装Jop  
     */    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {  
        if(args.length != 2){  
            System.exit(100);  
        }  
        Configuration conf = new Configuration();  
        Job job = Job.getInstance(conf);  
        job.setJarByClass(WordCountJobSeq.class);  
        //指定输入路径  
        FileInputFormat.setInputPaths(job,new Path(args[0]));  
        FileOutputFormat.setOutputPath(job,new Path(args[1]));  
  
        job.setMapperClass(MyMapper.class);  
        job.setMapOutputKeyClass(Text.class);  
        job.setMapOutputValueClass(LongWritable.class);  
  
        //设置输入数据处理类  
        job.setInputFormatClass(SequenceFileInputFormat.class);  
  
        job.setReducerClass(MyReduce.class);  
        job.setOutputKeyClass(Text.class);  
        job.setOutputValueClass(LongWritable.class);  
  
        job.waitForCompletion(true);  
    }  
}
```

运行效果：
![[file-20260115225311895.png | 400]]

## 2.数据倾斜问题与企业级解决方案

### 核心知识

- map任务的个数与block数量相关
- reduce任务的个数默认一个，可以通过`job.setPartitionerClass()`可以定义分区类，默认情况下是hash分区类，增加一般可以提升性能

数据倾斜问题：Reduce节点大部分执行完毕，但是有一个或者几个节点运行很慢，导致时间变得很长，具体表现是：Reduce阶段一直卡着不动，解决方案：
1、增加Reduce个数，不一定有用
2、把倾斜的数据打散

| **策略方案**               | **核心操作**                                     | **预期效果**                 | **实验结论**                                                                                                                      |
| ---------------------- | -------------------------------------------- | ------------------------ | ----------------------------------------------------------------------------------------------------------------------------- |
| **方案一：单纯增加 Reduce 个数** | 设置 `setNumReduceTasks(5)`                    | 希望通过更多 Reduce 任务并行处理。    | **基本无效**。因为 MapReduce 默认按 `key.hashCode() % numReducers` 分区。相同的 key "5" 无论如何都会被分配到同一个 Reduce 任务中，导致该任务负载过重（Long Tail），其余任务空转。 |
| **方案二：打散倾斜 Key（加盐处理）** | 在 Mapper 端为 key "5" 随机添加后缀，如 `5_0`, `5_1`... | 将原本集中的 key 物理上变成不同的 key。 | **效果显著**。倾斜的数据被均匀分配到了 5 个（或更多）不同的 Reduce 任务中。由于每个任务处理的数据量大幅减少，计算时间成倍缩短。                                                       |

```mathamatica
Map 阶段输出
┌────────┐
│ Map 1  │──┐
│ Map 2  │──┼──── user_1 的数据（50%）
│ Map 3  │──┘
│  ...   │
│ Map N  │
└────────┘

Shuffle（hash(user_id) % 200）
                    ↓
┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐
│ R0 │ R1 │ R2 │ R3 │ .. │R78 │R79 │R80 │ .. │R199│
│    │    │    │    │    │    │    │    │    │    │
│ ·  │ ·  │ ·  │ ·  │    │██████████│ ·  │ ·  │    │
│    │    │    │    │    │ user_1   │    │    │    │
│    │    │    │    │    │ 5000万   │    │    │    │
└────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘
```

### 实验操作

**数据倾斜问题案例**

1.生成倾斜文件，并上传：
```java
package com.imooc.utils;  
  
import java.io.BufferedWriter;  
import java.io.FileWriter;  
import java.io.IOException;  
import java.util.Random;  
  
public class SkewDataGenerator {  
  
    // 总数据量：1000 万  
    private static final int TOTAL_COUNT = 10_000_000;  
    // 倾斜 key    private static final int SKEW_KEY = 5;  
    // 倾斜 key 数量：900 万  
    private static final int SKEW_COUNT = 9_000_000;  
  
    public static void main(String[] args) {  
        String outputFile = "skew_data.txt";  
  
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {  
  
            // 1. 写入 900 万条 key=5            for (int i = 0; i < SKEW_COUNT; i++) {  
                writer.write(String.valueOf(SKEW_KEY));  
                writer.newLine();  
            }  
  
            // 2. 写入剩余 100 万条（1-10，排除 5）  
            Random random = new Random();  
            int remaining = TOTAL_COUNT - SKEW_COUNT;  
  
            for (int i = 0; i < remaining; i++) {  
                int value;  
                do {  
                    value = random.nextInt(10) + 1; // 1 - 10  
                } while (value == SKEW_KEY);  
  
                writer.write(String.valueOf(value));  
                writer.newLine();  
            }  
  
            writer.flush();  
            System.out.println("数据生成完成，总条数：" + TOTAL_COUNT + "，文件：" + outputFile);  
  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
}
```

```shell
hdfs dfs -put /export/data/skew_data.txt /
```

2.编写通过增加reduce个数的代码，并编译打包
```java
package com.imooc.mr;  
  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.LongWritable;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapreduce.Job;  
import org.apache.hadoop.mapreduce.Mapper;  
import org.apache.hadoop.mapreduce.Reducer;  
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
  
import java.io.IOException;  
  
/**  
 * 数据倾斜——增加reduce任务个数  
 */  
public class WordCountJobSkew {  
  
    public static class MyMapper extends Mapper<LongWritable, Text, Text, LongWritable> {  
        Logger logger = LoggerFactory.getLogger(MyMapper.class);  
        /**  
         * 实现map函数  
         * 接收<k1,v1>输出<k2，v2>  
         * @param key  
         * @param value  
         * @throws IOException  
         */        @Override  
        protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, LongWritable>.Context context) throws IOException, InterruptedException {  
            logger.info("<k1,v1>=<"+key.get() + "," + value.toString()+">");  
            String[] words = value.toString().split(" ");  
            for (String word : words) {  
                Text k2 = new Text(word);  
                LongWritable v2 = new LongWritable(1L);  
                context.write(k2, v2);  
            }  
        }  
    }  
  
    public static class MyReduce extends Reducer<Text, LongWritable, Text, LongWritable> {  
        Logger logger = LoggerFactory.getLogger(MyReduce.class);  
        /**  
         * 针对<k2，v2>累加求和转化为<k3，v3>  
         * @param key  
         * @param values  
         * @throws IOException  
         */        @Override  
        protected void reduce(Text key, Iterable<LongWritable> values, Reducer<Text, LongWritable, Text, LongWritable>.Context context) throws IOException, InterruptedException {  
            long sum = 0L;  
            for(LongWritable value :values){  
                logger.info("<k2,v2>=<"+key.toString() + "," + value.get()+">");  
                sum += value.get();  
                //模拟reduce的复杂计算消耗的时间  
                if(sum%2000 == 0){  
                    Thread.sleep(1);  
                }  
            }  
            Text k3 = key;  
            LongWritable v3 = new LongWritable(sum);  
            logger.info("<k3,v3>=<"+k3.toString() + "," + v3.get()+">");  
            context.write(k3,v3);  
        }  
    }  
  
    /**  
     * 组装Jop  
     */    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {  
        if(args.length != 3){  
            System.err.println(  
                    "Usage: WordCountJobSkew <input> <output> <numReducers>"  
            );  
            System.exit(100);  
        }  
        Configuration conf = new Configuration();  
        Job job = Job.getInstance(conf);  
        job.setJarByClass(WordCountJobSkew.class);  
        //指定输入路径  
        FileInputFormat.setInputPaths(job,new Path(args[0]));  
        FileOutputFormat.setOutputPath(job,new Path(args[1]));  
  
        job.setMapperClass(MyMapper.class);  
        job.setMapOutputKeyClass(Text.class);  
        job.setMapOutputValueClass(LongWritable.class);  
  
  
        job.setReducerClass(MyReduce.class);  
        job.setOutputKeyClass(Text.class);  
        job.setOutputValueClass(LongWritable.class);  
        //增加reduce个数  
        job.setNumReduceTasks(Integer.parseInt(args[2]));  
        job.waitForCompletion(true);  
    }  
}
```

```shell
mvn clean package -DskipTests
```

[[Ubuntu虚拟机磁盘空间不够，如何一键成功扩容]]

3.执行查看运行效果及时间

- 1个reduce的效果
```shell
hadoop jar /export/data/db_hadoop-1.0-SNAPSHOT.jar com.imooc.mr.WordCountJobSkew /skew_data.txt /out10000_1r 1
```

![[file-20260118110753852.png | 600]]

[[HDFS数据挂在根分区下，频繁提示空间不足如何处理]]

- 5个reduce的效果，相差不大
```shell
hadoop jar /export/data/db_hadoop-1.0-SNAPSHOT.jar com.imooc.mr.WordCountJobSkew /skew_data.txt /out10000_5r 5
```

![[file-20260118110814459.png | 600]]

[[为什么很小的文件，在MapReduce上进行运算后数据占用空间很大]]

4.编写通过增加打散数据，并编译打包上传
```java
package com.imooc.mr;  
  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.LongWritable;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapreduce.Job;  
import org.apache.hadoop.mapreduce.Mapper;  
import org.apache.hadoop.mapreduce.Reducer;  
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
  
import java.io.IOException;  
import java.util.Random;  
  
/**  
 * 数据倾斜——把倾斜的数据打散  
 */  
public class WordCountJobSkewRandkey {  
  
    public static class MyMapper extends Mapper<LongWritable, Text, Text, LongWritable> {  
        Logger logger = LoggerFactory.getLogger(MyMapper.class);  
        /**  
         * 实现map函数  
         * 接收<k1,v1>输出<k2，v2>  
         * @param key  
         * @param value  
         * @throws IOException  
         */        @Override  
        protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, LongWritable>.Context context) throws IOException, InterruptedException {  
            logger.info("<k1,v1>=<"+key.get() + "," + value.toString()+">");  
            String[] words = value.toString().split(" ");  
            String key1 = words[0];  
            if("5".equals(key1)){  
                Random random = new Random();  
                key1 = "5" + "_" + random.nextInt(10);  
            }  
  
            Text k2 = new Text(key1);  
            LongWritable v2 = new LongWritable(1L);  
            context.write(k2, v2);  
        }  
    }  
  
    public static class MyReduce extends Reducer<Text, LongWritable, Text, LongWritable> {  
        Logger logger = LoggerFactory.getLogger(MyReduce.class);  
        /**  
         * 针对<k2，v2>累加求和转化为<k3，v3>  
         * @param key  
         * @param values  
         * @throws IOException  
         */        @Override  
        protected void reduce(Text key, Iterable<LongWritable> values, Reducer<Text, LongWritable, Text, LongWritable>.Context context) throws IOException, InterruptedException {  
            long sum = 0L;  
            for(LongWritable value :values){  
                logger.info("<k2,v2>=<"+key.toString() + "," + value.get()+">");  
                sum += value.get();  
                //模拟reduce的复杂计算消耗的时间  
                if(sum%10000 == 0){  
                    Thread.sleep(1);  
                }  
            }  
            Text k3 = key;  
            LongWritable v3 = new LongWritable(sum);  
            logger.info("<k3,v3>=<"+k3.toString() + "," + v3.get()+">");  
            context.write(k3,v3);  
        }  
    }  
  
    /**  
     * 组装Jop  
     */    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {  
        if(args.length != 3){  
            System.err.println(  
                    "Usage: WordCountJobSkew <input> <output> <numReducers>"  
            );  
            System.exit(100);  
        }  
        Configuration conf = new Configuration();  
        Job job = Job.getInstance(conf);  
        job.setJarByClass(WordCountJobSkewRandkey.class);  
        //指定输入路径  
        FileInputFormat.setInputPaths(job,new Path(args[0]));  
        FileOutputFormat.setOutputPath(job,new Path(args[1]));  
  
        job.setMapperClass(MyMapper.class);  
        job.setMapOutputKeyClass(Text.class);  
        job.setMapOutputValueClass(LongWritable.class);  
  
  
        job.setReducerClass(MyReduce.class);  
        job.setOutputKeyClass(Text.class);  
        job.setOutputValueClass(LongWritable.class);  
        //增加reduce个数  
        job.setNumReduceTasks(Integer.parseInt(args[2]));  
        job.waitForCompletion(true);  
    }  
}
```

3.执行查看运行效果及时间

```shell
hadoop jar /export/data/db_hadoop-1.0-SNAPSHOT.jar com.imooc.mr.WordCountJobSkew /skew_data.txt /out10000_5rr 5
```

![[file-20260118111812539.png | 600]]

## 3.YARN实战

### 核心知识

yarn可以实现Hadoop集群的资源共享，不仅支持MapReduce，还支持Spark、Flink等

| **组件名称**                   | **角色** | **核心职责**                                                                                                                                             |
| -------------------------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ResourceManager (RM)**   | 集群主节点  | **全局资源管理器**。负责整个集群资源的监控、分配和调度。包含两个核心组件：<br><br>1. **Scheduler**: 纯调度器，根据容量/公平策略分配资源。<br>2. **Applications Manager (AsM)**: 负责接收作业提交、协商第一个 Container。 |
| **NodeManager (NM)**       | 节点从节点  | **单节点资源管理器**。负责管理本节点上的资源（CPU、内存）及 Container 的生命周期，并向 RM 定期汇报节点健康状况。                                                                                  |
| **ApplicationMaster (AM)** | 每个任务一个 | **作业管理者**。用户提交每个作业时都会启动一个 AM。负责向 RM 申请资源，并与 NM 协作启动任务，监控任务状态及容错。                                                                                     |
| **Container**              | 逻辑单位   | **资源抽象**。它是 YARN 资源的动态抽象，封装了节点上的多维资源（如 2GB 内存，1 核 CPU）。所有任务都在 Container 中运行。                                                                         |
- `yarn.nodemanager.resource.memory-mb`：单节点可分配的物理内存总量，默认是8GB
- `yarn.nodemanager.resource.cpu-vcores`：单节点可分配的虚拟CPU个数，默认是8

![[file-20260118120315563.png | 600]]

**YARN的调度器**

| **调度器类型**                      | **核心原理**                         | **图解特征**                                                    | **优点**                             | **缺点**                               | **适用场景**                       |
| ------------------------------ | -------------------------------- | ----------------------------------------------------------- | ---------------------------------- | ------------------------------------ | ------------------------------ |
| **FIFO Scheduler** (先进先出)      | 按照作业提交的顺序进行服务，先到的作业先分配资源。        | 只有一个队列，作业 1 占满全部资源后，作业 2 必须等待作业 1 完成才能开始。                   | 简单易懂，不需要额外配置。                      | 不适合共享集群。大作业会阻塞后续的小作业，资源利用率在单作业模式下受限。 | 个人测试或极简单的单任务环境。                |
| **Capacity Scheduler** (容量调度器) | 多队列结构，每个队列预设资源百分比，支持资源共享和限制。     | 存在 queue A 和 queue B。作业 1 在 A 中运行，作业 2 在 B 中运行，两者互不干扰，并行执行。 | 保证小作业的及时响应；支持资源借用（闲置资源可临时给其他队列使用）。 | 配置相对复杂；队列间有严格的比例限制。                  | 多租户共享集群（如企业生产环境），Hadoop 默认调度器。 |
| **Fair Scheduler** (公平调度器)     | 动态调整资源，使所有运行的作业随着时间推移能公平地共享集群资源。 | 作业 2 提交后，作业 1 会释放部分资源给作业 2，使两者在同一队列中“平分”资源。                 | 资源分配最灵活；自动平衡长短作业，不会出现某个作业长时间等待。    | 频繁的资源调整可能导致任务切换开销；早期的公平性计算较为复杂。      | 资源竞争激烈且作业类型多样化的中大型集群。          |

![[file-20260118120455171.png]]

默认是Capacity Scheduler
![[file-20260118120728283.png]]

### 实验操作

增加online队列和offline队列，向offline队列提交任务
1.更新capacity-scheduler.xml 配置文件
```xml
<configuration>
  <property>
    <name>yarn.scheduler.capacity.resource-calculator</name>
    <value>org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator</value>
    <description>
      The ResourceCalculator implementation to be used to compare 
      Resources in the scheduler.
      The default i.e. DefaultResourceCalculator only uses Memory while
      DominantResourceCalculator uses dominant-resource to compare 
      multi-dimensional resources such as Memory, CPU etc.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.queues</name>
    <value>default,online,offline</value>
    <description>
      The queues at the this level (root is the root queue).
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.default.capacity</name>
    <value>70</value>
    <description>Default queue target capacity.</description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.online.capacity</name>
    <value>10</value>
    <description>online queue target capacity.</description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.offline.capacity</name>
    <value>20</value>
    <description>offline queue target capacity.</description>
  </property>


  <property>
    <name>yarn.scheduler.capacity.root.default.maximum-capacity</name>
    <value>100</value>
    <description>
      The maximum capacity of the default queue. 
    </description>
  </property>
    <property>
    <name>yarn.scheduler.capacity.root.online.maximum-capacity</name>
    <value>10</value>
    <description>
      The maximum capacity of the online queue.
    </description>
  </property>
    <property>
    <name>yarn.scheduler.capacity.root.offline.maximum-capacity</name>
    <value>20</value>
    <description>
      The maximum capacity of the offline queue.
    </description>
  </property>

</configuration>

```

2.查看效果
![[file-20260118125111579.png | 600]]

3.编写指定queue文件，向offline提交任务

```java
package com.imooc.mr;  
  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.LongWritable;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapreduce.Job;  
import org.apache.hadoop.mapreduce.Mapper;  
import org.apache.hadoop.mapreduce.Reducer;  
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;  
import org.apache.hadoop.util.GenericOptionsParser;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
  
import java.io.IOException;  
  
/**  
 * 指定队列名称  
 */  
public class WordCountJobQueue {  
  
    public static class MyMapper extends Mapper<LongWritable, Text, Text, LongWritable> {  
        Logger logger = LoggerFactory.getLogger(MyMapper.class);  
        /**  
         * 实现map函数  
         * 接收<k1,v1>输出<k2，v2>  
         * @param key  
         * @param value  
         * @throws IOException  
         */        @Override  
        protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, LongWritable>.Context context) throws IOException, InterruptedException {  
            logger.info("<k1,v1>=<"+key.get() + "," + value.toString()+">");  
            String[] words = value.toString().split(" ");  
            for (String word : words) {  
                Text k2 = new Text(word);  
                LongWritable v2 = new LongWritable(1L);  
                context.write(k2, v2);  
            }  
        }  
    }  
  
    public static class MyReduce extends Reducer<Text, LongWritable, Text, LongWritable> {  
        Logger logger = LoggerFactory.getLogger(MyReduce.class);  
        /**  
         * 针对<k2，v2>累加求和转化为<k3，v3>  
         * @param key  
         * @param values  
         * @throws IOException  
         */        @Override  
        protected void reduce(Text key, Iterable<LongWritable> values, Reducer<Text, LongWritable, Text, LongWritable>.Context context) throws IOException, InterruptedException {  
            long sum = 0L;  
            for(LongWritable value :values){  
                logger.info("<k2,v2>=<"+key.toString() + "," + value.get()+">");  
                sum += value.get();  
            }  
            Text k3 = key;  
            LongWritable v3 = new LongWritable(sum);  
            logger.info("<k3,v3>=<"+k3.toString() + "," + v3.get()+">");  
            context.write(k3,v3);  
        }  
    }  
  
    /**  
     * 组装Jop  
     */    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {  
  
        Configuration conf = new Configuration();  
  
        //解析命令行中-D传过来的参数，添加到conf中  
        String[] remainingArgs = new GenericOptionsParser(conf, args).getRemainingArgs();  
  
        Job job = Job.getInstance(conf);  
        job.setJarByClass(WordCountJobQueue.class);  
        //指定输入路径  
        FileInputFormat.setInputPaths(job,new Path(remainingArgs[0]));  
        FileOutputFormat.setOutputPath(job,new Path(remainingArgs[1]));  
  
        job.setMapperClass(MyMapper.class);  
        job.setMapOutputKeyClass(Text.class);  
        job.setMapOutputValueClass(LongWritable.class);  
  
  
        job.setReducerClass(MyReduce.class);  
        job.setOutputKeyClass(Text.class);  
        job.setOutputValueClass(LongWritable.class);  
  
        job.waitForCompletion(true);  
    }  
}
```

```shell
hadoop jar /export/data/db_hadoop-1.0-SNAPSHOT.jar com.imooc.mr.WordCountJob -Dmapreduce.job.queuename=offline /wordcount_input.txt /out_queue
```
查看效果：
![[file-20260118130253572.png | 600]]
## 4.官方文档使用指北

**如何看官网**：
![[file-20260118135243466.png | 600]]

![[file-20260118135357601.png | 800]]

**在CDH和HDP中使用**

- 下载CDH/HDP解压，通过Vmware打开
- 进行相关配置，启动服务
- 开始相关操作，Namenode端口号是8020

# 二.Flume从0到高手一站式养成记