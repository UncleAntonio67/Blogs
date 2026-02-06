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

## 1.极速入门Flume

### 核心知识

Flume是一个高可用、高可靠，分布式的海量日志采集、聚合和传输的系统，不需要写一行代码

![[file-20260119214234144.png | 600]]

1. 它有一个简单、灵活的基于流的数据流结构
2. 具有负载均衡机制和故障转移机制
3. 一个简单可扩展的数据模型

高级应用场景：
- 多个agent之间可以联通

![[file-20260119214816324.png | 600]]

- 多个agent汇聚
![[file-20260119214924876.png]]

**三个核心组件**：

| **组件名称**    | **核心作用 (Role)**                                             | **常见类型 (Types)**                                    | **常用场景与特点**                                                                                        |
| ----------- | ----------------------------------------------------------- | --------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| **Source**  | **数据采集**：负责接收外部源的数据，并将数据封装成 Flume 事件（Event）传递给 Channel。     | Avro, Exec, Spooling Directory, Kafka, NetCat, HTTP | **Exec**: 实时监控文件输出（如 `tail -f`）。<br>**Spooling Dir**: 监控目录下新增的文件。<br>**Kafka**: 从 Kafka 消息队列中读取数据。 |
| **Channel** | **中转存储**：位于 Source 和 Sink 之间的临时缓冲区，起到了解耦和削峰填谷的作用。           | Memory Channel, File Channel, Kafka Channel         | **Memory**: 速度极快，但断电数据会丢失。<br>**File**: 数据写在磁盘，可靠性高但速度慢。<br>**Kafka**: 利用 Kafka 集群做缓冲，安全性极高。       |
| **Sink**    | **数据下沉**：从 Channel 中取出数据，并将其发送到目的地（如 HDFS）或下一个 Flume Agent。 | HDFS, Logger, Avro, Kafka, HBase, Solr              | **HDFS**: 最常用的目的地，存入大数据集群。<br>**Logger**: 打印到控制台，多用于调试。<br>**Avro**: 发送给另一个 Flume Agent，用于多级级联。    |

下载安装及配置：https://flume.apache.org/

### 实验操作

解压，更改配置

## 2.极速上手Flume使用

### 核心知识

操作手册：https://flume.apache.org/releases/content/1.11.0/FlumeUserGuide.html

1.在 Flume 的 `conf` 目录下（或自定义路径），添加配置文件
2.进入 Flume 的安装目录，执行以下命令启动服务。
3.保留启动 Flume 的终端窗口，另外打开一个**新的终端窗口**，使用 `telnet` 连接刚才配置的端口。

案例一：采集文件内容上传到HDFS
案例二：采集网站日志上传至HDFS
![[file-20260131150333891.png | 600]]

### 实验操作

1.编辑配置文件
```config
# example.conf: A single-node Flume configuration

# Name the components on this agent
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# Describe/configure the source
a1.sources.r1.type = netcat
# a1.sources.r1.bind = localhost
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 44444

# Describe the sink
a1.sinks.k1.type = logger

# Use a channel which buffers events in memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# Bind the source and sink to the channel
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

2.启动Agent：
```shell
bin/flume-ng agent --conf conf --conf-file example.conf --name a1 -Dflume.root.logger=INFO,console
```

3.启动telnet发送消息查看效果
```shell
telnet localhost 44444
```

![[file-20260119231710607.png | 500]]

4.后台运行
```shell
nohup bin/flume-ng agent --conf conf --conf-file example.conf &
```

5.查看日志
```shell
tail -10 flume.log
```

**案例1：采集文件内容上传到HDFS**

1.配置文件
```yaml
# Name the components on this agent
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# Describe/configure the source
a1.sources.r1.type = spooldir
a1.sources.r1.spoolDir = /data/log/StudentDir


# Describe the sink
a1.sinks.k1.type = hdfs
a1.sinks.k1.hdfs.path = hdfs://192.168.148.100:9000/flume/StudentDir
a1.sinks.k1.hdfs.filePrefix = stu-
a1.sinks.k1.hdfs.fileType = DataStream 
a1.sinks.k1.hdfs.writeFormat = Text 
a1.sinks.k1.hdfs.rollInterval = 3600
a1.sinks.k1.hdfs.rollSize = 134217728
a1.sinks.k1.hdfs.rollCount = 0

# Use a channel which buffers events in memory
a1.channels.c1.type = file
a1.channels.c1.checkpointDir = /export/servers/flume-1.11.0/StudentDir/checkpoint
a1.channels.c1.dataDirs = /export/servers/flume-1.11.0/StudentDir/data

# Bind the source and sink to the channel
a1.sources.r1.channels = c1
```

2.在对应目录下生成测试数据
``` shell
# 创建目录
mkdir -p /data/log/StudentDir

# 生成测试数据
jack 12 male
jessic 13 female
tom 30 male
```

3.启动hdfs后启动Agent
```shell
bin/flume-ng agent --conf conf --conf-file file-to-hdfs.conf --name a1 -Dflume.root.logger=INFO,console
```

4.查看hdfs文件：
```shell
hdfs dfs -cat /flume/StudentDir/stu-.1769841769976.tmp
```
![[file-20260131145814937.png]]

5.查看channel
![[file-20260131150118562.png| 500]]

**案例2：采集网站内容上传到HDFS**

1.在node02和node03配置安装flume
```shell
scp -rq flume-1.11.0/ node02:/export/servers/
scp -rq flume-1.11.0/ node03:/export/servers/
```

node01配置：
```conf
# agent-avro-hdfs.conf: 运行在 Node01 (192.168.148.100)

# Name the components
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# ------------------------------------------------
# 1. Source: 必须是 Avro，用来接收 Node02/03 发来的数据
# ------------------------------------------------
a1.sources.r1.type = avro
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 45454

# ------------------------------------------------
# 2. Sink: 这里才是真正写入 HDFS 的地方
# ------------------------------------------------
a1.sinks.k1.type = hdfs
# 确认你的 HDFS NameNode 地址是 9000 还是 9820/8020
a1.sinks.k1.hdfs.path = hdfs://192.168.148.100:9000/access/%Y%m%d
a1.sinks.k1.hdfs.filePrefix = access-
# 解决小文件问题，积攒够了再写
a1.sinks.k1.hdfs.rollInterval = 3600
a1.sinks.k1.hdfs.rollSize = 134217728
a1.sinks.k1.hdfs.rollCount = 0

# 关键设置：生成目录需要时间
a1.sinks.k1.hdfs.fileType = DataStream
a1.sinks.k1.hdfs.writeFormat = Text
a1.sinks.k1.hdfs.useLocalTimeStamp = true

# ------------------------------------------------
# 3. Channel
# ------------------------------------------------
a1.channels.c1.type = memory
a1.channels.c1.capacity = 10000
a1.channels.c1.transactionCapacity = 1000

# Bind
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```
node02、node03配置：
```conf
#example.conf: A single-node Flume configuration

# Name the components on this agent
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# Describe/configure the source
a1.sources.r1.type = exec
a1.sources.r1.command = tail -F /data/log/access.log


# Describe the sink
a1.sinks.k1.type = avro
a1.sinks.k1.hostname = 192.168.148.100
a1.sinks.k1.port = 45454

# Use a channel which buffers events in memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 10000
a1.channels.c1.transactionCapacity = 100

# Bind the source and sink to the channel
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

2.创建数据目录，生成测试数据
```shell
#!/bin/bash

# 定义日志文件路径
LOG_DIR="/data/log"
LOG_FILE="${LOG_DIR}/access.log"

# 检查日志目录是否存在，如果不存在则创建（这是比原图优化的地方）
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

# 循环向文件中生成数据
# while true 比 while [ "1" = "1" ] 更通用
while true
do
    # 获取当前时间戳
    curr_time=$(date +%s)
    
    # 获取当前主机名
    name=$(hostname)
    
    # 将 "主机名_时间戳" 追加写入到日志文件
    echo "${name}_${curr_time}" >> "$LOG_FILE"
    
    # 暂停1秒
    sleep 1
done
```

3.先启动node01，再启动node02、node03
```shell
bin/flume-ng agent --conf conf --conf-file avro-to-hdfs-node01.conf --name a1 -Dflume.root.logger=INFO,console

agent --conf conf --conf-file file-to-avro-node02.conf --name a1 -Dflume.root.logger=INFO,console

agent --conf conf --conf-file file-to-avro-node03.conf --name a1 -Dflume.root.logger=INFO,console
```

4.查看效果
![[file-20260131170108667.png | 600]]

## 3.Flume高级组件

### 核心知识 

| **组件名称**                                | **定义**                                       | **核心作用 (通俗解释)**                                                       | **典型应用场景**                                                                                                                       |
| --------------------------------------- | -------------------------------------------- | --------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| **Source Interceptors**<br>(Source 拦截器) | Source 可以指定一个或者多个拦截器，**按先后顺序**依次对采集到的数据进行处理。 | **“流水线加工”**<br>在数据进入 Channel 之前，像过安检一样，对数据进行过滤、修改或打标签。支持链式处理（A处理完给B）。 | 1. **ETL 清洗**：过滤掉不符合 JSON 格式的脏数据。<br>2. **脱敏**：将身份证号中间几位替换为 `*`。<br>3. **打标签**：给数据加上 `timestamp` 或 `host` 头信息。                   |
| **Channel Selectors**<br>(Channel 选择器)  | Source 发往**多个 Channel** 的策略设置。               | **“数据分发员”**<br>决定数据是“复制”给所有下游，还是根据条件“分流”给特定下游。                        | 1. **Replicating (复制)**：一份日志存 HDFS 做离线分析，一份发 Kafka 做实时计算。<br>2. **Multiplexing (多路复用)**：按地区分流，美国的日志走 Channel A，中国的日志走 Channel B。 |
| **Sink Processors**<br>(Sink 处理器)       | **Sink 发送数据的策略设置**。                          | **“发送策略组”**<br>通常用于将多个 Sink 编成一个组（Sink Group），实现**负载均衡**或**故障转移**。    | 1. **Failover (故障转移)**：首选 Sink 挂了（如网络断了），自动切换到备用 Sink 发送。<br>2. **Load Balancing (负载均衡)**：把数据轮询发给多个 Sink，分摊压力，提高吞吐量。             |
Event 是 Flume 传输数据的**基本单位**，也是事务的基本单位。在文本文件中，通常**一行记录就是一个 Event**。
Event 里有 **header** 和 **body**；header 类型为 `Map<String, String>`。
我们可以在 Source 中增加 header 的 `<key, value>`，在 Channel 和 Sink 中使用 header 中的值。

Event 的结构示意图：
```json
Event = {
    // Header: 只有 Flume 组件看，用于路由、分类、重命名
    "headers": {
        "timestamp": "1678888888000",  // 拦截器加的时间戳
        "host": "web-server-01",       // 采集机器的主机名
        "log_type": "access_log"       // 自定义的标签
    },

    // Body: 真正要保存的数据
    "body": "2024-01-31 18:00:00 GET /index.html 200 OK"
}
```

| **拦截器名称**              | **修改的位置**        | **核心功能** | **典型场景**              |
| ---------------------- | ---------------- | -------- | --------------------- |
| **Timestamp**          | Header           | 加时间戳     | 配合 HDFS Sink 生成时间目录   |
| **Host**               | Header           | 加机器名     | 区分数据来源机器              |
| **Static**             | Header           | 加固定标签    | 标记数据类型 (如 web vs app) |
| **Search and Replace** | **Body**         | 修改内容     | 敏感数据脱敏、清洗垃圾字符         |
| **Regex Extractor**    | Header (来源自Body) | 提取内容变标签  | 提取状态码(404/500)用于分流报警  |
**案例1：Source Interceptors：多类型上传到hdfs**
**案例2：Channel Selectors：多channel之Replicating Channel Selector**
![[file-20260202231255635.png | 600]]

**案例三：多channel之Multiplexing Channel Selector**：
![[file-20260202232551517.png]]
**案例4：Load balancing Sink Processor**
![[file-20260203211015687.png]]

**案例5：Failover Sink Processor**

![[file-20260203222058872.png]]

### 实验操作

**案例一：多类型上传到hdfs**：
hdfs://192.168.148.100: 9000/moreType/20200101/videoInfo
hdfs://192.168.148.100:9000/moreType/20200101/userInfo
hdfs://192.168.148.100: 9000/moreType/20200101/giftRecord

Exec Source ->Search and Replace Interceptor -> Regex Extractor Interceptor -> File Channel ->HDFS Sink

1.创建配置文件
```conf
# Name the components
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# ------------------------------------------------
# 1. Source
# ------------------------------------------------
a1.sources.r1.type = exec
a1.sources.r1.command = tail -F /data/log/moreType.log

# ------------------------------------------------
# Interceptors: 核心修复区 (注意双反斜杠)
# ------------------------------------------------
a1.sources.r1.interceptors = i1 i2 i3 i4

# --- i1: video_info ---
a1.sources.r1.interceptors.i1.type = search_replace
# [修复] 使用 \\s* 才能正确匹配空格
a1.sources.r1.interceptors.i1.searchPattern = "type":\\s*"video_info"
a1.sources.r1.interceptors.i1.replaceString = "type":"videoInfo"

# --- i2: user_info ---
a1.sources.r1.interceptors.i2.type = search_replace
a1.sources.r1.interceptors.i2.searchPattern = "type":\\s*"user_info"
a1.sources.r1.interceptors.i2.replaceString = "type":"userInfo"

# --- i3: gift_record ---
a1.sources.r1.interceptors.i3.type = search_replace
a1.sources.r1.interceptors.i3.searchPattern = "type":\\s*"gift_record"
a1.sources.r1.interceptors.i3.replaceString = "type":"giftInfo"

# --- i4: 提取 logType ---
a1.sources.r1.interceptors.i4.type = regex_extractor
# [关键修复] 必须用 \\s 和 \\w，否则正则无效！
a1.sources.r1.interceptors.i4.regex = "type":\\s*"(\\w+)"
a1.sources.r1.interceptors.i4.serializers = s1
a1.sources.r1.interceptors.i4.serializers.s1.name = logType

# ------------------------------------------------
# 2. Sink
# ------------------------------------------------
a1.sinks.k1.type = hdfs
# 只有 regex 匹配成功，这里的 logType 才有值
a1.sinks.k1.hdfs.path = hdfs://192.168.148.100:9000/moreType/%Y%m%d/%{logType}

a1.sinks.k1.hdfs.rollInterval = 3600
a1.sinks.k1.hdfs.rollSize = 134217728
a1.sinks.k1.hdfs.rollCount = 0

a1.sinks.k1.hdfs.fileType = DataStream
a1.sinks.k1.hdfs.writeFormat = Text
a1.sinks.k1.hdfs.useLocalTimeStamp = true

a1.sinks.k1.hdfs.filePrefix = data
a1.sinks.k1.hdfs.fileSuffix = .log

# ------------------------------------------------
# 3. Channel
# ------------------------------------------------
a1.channels.c1.type = file
a1.channels.c1.checkpointDir = /export/servers/flume-1.11.0/data/moreType/checkpoint
a1.channels.c1.dataDirs = /mnt/flume/data

# Bind
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1

```

2.生成测试数据
```bash
echo '{"id":"1","type":"video_info"}' >> /data/log/moreType.log
echo '{"uid":"2","type":"user_info"}' >> /data/log/moreType.log
echo '{"id":"3","type":"gift_record"}' >> /data/log/moreType.log
```

3.启动agent，查看效果
```bash
bin/flume-ng agent --conf conf --conf-file file-to-hdfs-moreType.conf --name a1 -Dflume.root.logger=INFO,console
```
![[file-20260131193959227.png]]


**案例二：多channel之Replicating Channel Selector**：

1.创建配置文件
```conf
# Name the components
a1.sources = r1
a1.sinks = k1 k2
a1.channels = c1 c2

# ------------------------------------------------
# 1. Source
# ------------------------------------------------
a1.sources.r1.type = netcat
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 44444

#默认是replicating
a1.sources.r1.selector.type = replicating

# ------------------------------------------------
# 2. Sink
# ------------------------------------------------
a1.sinks.k1.type = logger


a1.sinks.k2.type = hdfs
# 只有 regex 匹配成功，这里的 logType 才有值
a1.sinks.k2.hdfs.path = hdfs://192.168.148.100:9000/replication

a1.sinks.k2.hdfs.rollInterval = 3600
a1.sinks.k2.hdfs.rollSize = 134217728
a1.sinks.k2.hdfs.rollCount = 0

a1.sinks.k2.hdfs.fileType = DataStream
a1.sinks.k2.hdfs.writeFormat = Text
a1.sinks.k2.hdfs.useLocalTimeStamp = true

a1.sinks.k2.hdfs.filePrefix = data
a1.sinks.k2.hdfs.fileSuffix = .log

# ------------------------------------------------
# 3. Channel
# ------------------------------------------------
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

a1.channels.c2.type = memory
a1.channels.c2.capacity = 1000
a1.channels.c2.transactionCapacity = 100

# Bind
a1.sources.r1.channels = c1 c2
a1.sinks.k1.channel = c1
a1.sinks.k2.channel = c2
```

2.启动agent，查看效果
```bash
bin/flume-ng agent --conf conf --conf-file tcp-to-replicating.conf --name a1 -Dflume.root.logger=INFO,console
```
![[file-20260202232317965.png]]
![[file-20260202234337791.png]]


**案例三：多channel之Multiplexing Channel Selector**：

1.创建配置文件
```conf
# Name the components
a1.sources = r1
a1.sinks = k1 k2
a1.channels = c1 c2

# ------------------------------------------------
# 1. Source: Netcat (监听端口)
# ------------------------------------------------
a1.sources.r1.type = netcat
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 44444

# --- Interceptor: 正则提取 city ---
# [修复] al -> a1
a1.sources.r1.interceptors = i1
# [修复] al -> a1, regex extractor -> regex_extractor
a1.sources.r1.interceptors.i1.type = regex_extractor
# [优化] 加上 \\s* 允许冒号后有空格，增强兼容性
a1.sources.r1.interceptors.i1.regex = "city":\\s*"(\\w+)"
a1.sources.r1.interceptors.i1.serializers = s1
a1.sources.r1.interceptors.i1.serializers.s1.name = city

# --- Selector: Multiplexing (分流) ---
# [修复] al -> a1
a1.sources.r1.selector.type = multiplexing
a1.sources.r1.selector.header = city
# 逻辑：如果是 bj -> 走 c1 (Logger 打印到屏幕)
a1.sources.r1.selector.mapping.bj = c1
# 逻辑：其他城市 -> 走 c2 (存入 HDFS)
a1.sources.r1.selector.default = c2

# ------------------------------------------------
# 2. Sinks
# ------------------------------------------------
# Sink k1: Logger (控制台输出)
a1.sinks.k1.type = logger

# Sink k2: HDFS (存文件)
a1.sinks.k2.type = hdfs
a1.sinks.k2.hdfs.path = hdfs://192.168.148.100:9000/multiplexing
# 只有 path 里有时间占位符(%Y%m%d)时，useLocalTimeStamp 才是必须的
# 但开着也没坏处
a1.sinks.k2.hdfs.useLocalTimeStamp = true

# 滚动策略
a1.sinks.k2.hdfs.rollInterval = 3600
a1.sinks.k2.hdfs.rollSize = 134217728
a1.sinks.k2.hdfs.rollCount = 0

a1.sinks.k2.hdfs.fileType = DataStream
a1.sinks.k2.hdfs.writeFormat = Text
a1.sinks.k2.hdfs.filePrefix = data
a1.sinks.k2.hdfs.fileSuffix = .log

# ------------------------------------------------
# 3. Channels
# ------------------------------------------------
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

a1.channels.c2.type = memory
a1.channels.c2.capacity = 1000
a1.channels.c2.transactionCapacity = 100

# Bind
a1.sources.r1.channels = c1 c2
a1.sinks.k1.channel = c1
a1.sinks.k2.channel = c2
```

2.准备测试数据
```json
{"name":"jack","age":19,"city":"bj"}
{"name":"tom","age":25,"city":"sh"}
{"name":"lucy","age":22,"city":"gz"}
{"name":"mike","age":30,"city":"sz"}
{"name":"alice","age":18,"city":"bj"}
{"name":"bob","age":45,"city":"sh"}
{"name":"john","age":33,"city":"gz"}
```

3.启动并查看配置
```bash
bin/flume-ng agent --conf conf --conf-file tcp-to-multiplexing.conf --name a1 -Dflume.root.logger=INFO,console
```
![[file-20260203210720026.png | 300]]
![[file-20260203210638650.png]]



**案例4：多channel之Multiplexing Channel Selector**：

1.创建配置文件
node01：
```conf
# Name the components
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# ------------------------------------------------
# 1. Source: Netcat (监听端口)
# ------------------------------------------------
a1.sources.r1.type = avro
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 41414

# ------------------------------------------------
# 2. Sinks
# ------------------------------------------------
a1.sinks.k1.type = hdfs
a1.sinks.k1.hdfs.path = hdfs://192.168.148.100:9000/loadbalance

a1.sinks.k1.hdfs.rollInterval = 3600
a1.sinks.k1.hdfs.rollSize = 134217728
a1.sinks.k1.hdfs.rollCount = 0

a1.sinks.k1.hdfs.fileType = DataStream
a1.sinks.k1.hdfs.writeFormat = Text
a1.sinks.k1.hdfs.useLocalTimeStamp = true

a1.sinks.k1.hdfs.filePrefix = data110
a1.sinks.k1.hdfs.fileSuffix = .log


# ------------------------------------------------
# 3. Channels
# ------------------------------------------------
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# Bind
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```
node02：
```conf
# Name the components
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# ------------------------------------------------
# 1. Source: Netcat (监听端口)
# ------------------------------------------------
a1.sources.r1.type = avro
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 41414

# ------------------------------------------------
# 2. Sinks
# ------------------------------------------------
a1.sinks.k1.type = hdfs
a1.sinks.k1.hdfs.path = hdfs://192.168.148.100:9000/loadbalance

a1.sinks.k1.hdfs.rollInterval = 3600
a1.sinks.k1.hdfs.rollSize = 134217728
a1.sinks.k1.hdfs.rollCount = 0

a1.sinks.k1.hdfs.fileType = DataStream
a1.sinks.k1.hdfs.writeFormat = Text
a1.sinks.k1.hdfs.useLocalTimeStamp = true

a1.sinks.k1.hdfs.filePrefix = data110
a1.sinks.k1.hdfs.fileSuffix = .log


# ------------------------------------------------
# 3. Channels
# ------------------------------------------------
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# Bind
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

node03：
```conf
# Name the components
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# ------------------------------------------------
# 1. Source: Netcat (监听端口)
# ------------------------------------------------
a1.sources.r1.type = avro
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 41414

# ------------------------------------------------
# 2. Sinks
# ------------------------------------------------
a1.sinks.k1.type = hdfs
a1.sinks.k1.hdfs.path = hdfs://192.168.148.100:9000/loadbalance

a1.sinks.k1.hdfs.rollInterval = 3600
a1.sinks.k1.hdfs.rollSize = 134217728
a1.sinks.k1.hdfs.rollCount = 0

a1.sinks.k1.hdfs.fileType = DataStream
a1.sinks.k1.hdfs.writeFormat = Text
a1.sinks.k1.hdfs.useLocalTimeStamp = true

a1.sinks.k1.hdfs.filePrefix = data120
a1.sinks.k1.hdfs.fileSuffix = .log


# ------------------------------------------------
# 3. Channels
# ------------------------------------------------
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# Bind
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

2.启动并查看效果，先启动02、03，再启动01
![[file-20260203222000932.png | 300]]
![[file-20260203221944656.png]]

**案例5：多channel之Multiplexing Channel Selector**：

1.创建配置文件
node01:
```conf
# Name the components
a1.sources = r1
a1.sinks = k1 k2
a1.channels = c1

# ------------------------------------------------
# 1. Source: Netcat (监听端口)
# ------------------------------------------------
a1.sources.r1.type = netcat
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 44444


# ------------------------------------------------
# 2. Sinks
# ------------------------------------------------
a1.sinks.k1.type = avro
a1.sinks.k1.hostname = 192.168.148.110
a1.sinks.k1.port = 41414
a1.sinks.k1.batch-size = 1

a1.sinks.k2.type = avro
a1.sinks.k2.hostname = 192.168.148.120
a1.sinks.k2.port = 41414
a1.sinks.k2.batch-size = 1

a1.sinkgroups = g1
a1.sinkgroups.g1.sinks = k1 k2
a1.sinkgroups.g1.processor.type = failover
a1.sinkgroups.g1.processor.priority.k1 = 5
a1.sinkgroups.g1.processor.priority.k2 = 10
a1.sinkgroups.g1.processor.maxpenalty = 10000
# ------------------------------------------------
# 3. Channels
# ------------------------------------------------
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# Bind
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
a1.sinks.k2.channel = c1
```
node02:
```conf
# Name the components
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# ------------------------------------------------
# 1. Source: Netcat (监听端口)
# ------------------------------------------------
a1.sources.r1.type = avro
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 41414

# ------------------------------------------------
# 2. Sinks
# ------------------------------------------------
a1.sinks.k1.type = hdfs
a1.sinks.k1.hdfs.path = hdfs://192.168.148.100:9000/failover

a1.sinks.k1.hdfs.rollInterval = 3600
a1.sinks.k1.hdfs.rollSize = 134217728
a1.sinks.k1.hdfs.rollCount = 0

a1.sinks.k1.hdfs.fileType = DataStream
a1.sinks.k1.hdfs.writeFormat = Text
a1.sinks.k1.hdfs.useLocalTimeStamp = true

a1.sinks.k1.hdfs.filePrefix = data110
a1.sinks.k1.hdfs.fileSuffix = .log


# ------------------------------------------------
# 3. Channels
# ------------------------------------------------
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# Bind
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```
node02:
```conf
# Name the components
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# ------------------------------------------------
# 1. Source: Netcat (监听端口)
# ------------------------------------------------
a1.sources.r1.type = avro
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 41414

# ------------------------------------------------
# 2. Sinks
# ------------------------------------------------
a1.sinks.k1.type = hdfs
a1.sinks.k1.hdfs.path = hdfs://192.168.148.100:9000/failover

a1.sinks.k1.hdfs.rollInterval = 3600
a1.sinks.k1.hdfs.rollSize = 134217728
a1.sinks.k1.hdfs.rollCount = 0

a1.sinks.k1.hdfs.fileType = DataStream
a1.sinks.k1.hdfs.writeFormat = Text
a1.sinks.k1.hdfs.useLocalTimeStamp = true

a1.sinks.k1.hdfs.filePrefix = data120
a1.sinks.k1.hdfs.fileSuffix = .log


# ------------------------------------------------
# 3. Channels
# ------------------------------------------------
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# Bind
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

2.启动并查看效果，先启动02、03，再启动01
![[file-20260203223245588.png | 300]]
![[file-20260203223307360.png]]


## 4.Flume出神入化

### 核心知识

![[file-20260203223445644.png | 500]]
**flume自定义组件**可参考官方开发文档

**Flume优化**：

| **优化项**       | **核心建议 (图片原文)**                  | **适用场景 / 解决的问题**                                                                                              | **具体操作方式**                                  | **配置/命令示例**                                                                                                                  |
| ------------- | -------------------------------- | ------------------------------------------------------------------------------------------------------------- | ------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| **1. 调整内存大小** | 建议设置 **1G~2G**，太小的话会导致频繁 GC。     | **场景：** 高吞吐量、使用 Memory Channel、或者日志量非常大时。<br>**解决：** 防止 JVM 频繁进行垃圾回收导致传输卡顿，或直接报 OOM (内存溢出) 崩溃。                | 修改 `conf/flume-env.sh` 文件中的 `JAVA_OPTS` 参数。 | **修改内容：**<br>`export JAVA_OPTS="-Xms2048m -Xmx2048m -Dcom.sun.management.jmxremote"`<br>_(建议 Xms 和 Xmx 设为一样大)_               |
| **2. 区分日志文件** | 启动**多个 Flume 进程**时，建议修改配置区分日志文件。 | **场景：** 单台服务器上运行了多个 Agent (如一个采 Web 日志，一个采 App 日志)。<br>**解决：** 防止所有进程把日志写到同一个 `flume.log` 里，导致日志错乱、覆盖，无法排查问题。 | 在**启动命令**中添加 `-D` 参数动态指定日志文件名。              | **启动命令追加参数：**<br>`bin/flume-ng agent ...`<br><br>`-Dflume.log.file=my-app-log.log`<br><br>`-Dflume.root.logger=INFO,LOGFILE` |
**Flume进程监控**：
通过shell脚本实现Flume进程监控及自动重启

### 实验操作

1.编写相关脚本：
monlist.conf
```bash
example=startExample.sh
```
startExample.sh
```bash
#!/bin/bash

# [注意] 请修改为你实际的 Flume 安装路径
flume_path=/data/soft/apache-flume-1.9.0-bin

# 启动命令
nohup ${flume_path}/bin/flume-ng agent --name a1 --conf ${flume_path}/conf/ --conf-file ${flume_path}/conf/example.conf &
```
monlist.sh
```bash
#!/bin/bash

monlist=`cat monlist.conf`
echo "start check"
for item in ${monlist}
do
    # 1. 设置字段分隔符为等号 (=)
    OLD_IFS=$IFS
    IFS="="
    # 2. 把一行内容转成数组 (例如 example=startExample.sh)
    arr=($item)
    # 3. 获取等号左边的内容 (关键词，用于 grep)
    name=${arr[0]}
    # 4. 获取等号右边的内容 (重启脚本)
    script=${arr[1]}
    # 恢复默认分隔符 (非常重要，否则下一次循环会出错)
    IFS=$OLD_IFS
    echo "time is:" `date +"%Y-%m-%d %H:%M:%S"` " check "$name
    # 5. 核心判断：利用 jps -m 查看进程，过滤关键词，统计行数
    # 如果行数为 0，说明进程挂了
    if [ `jps -m | grep $name | wc -l` -eq 0 ]
    then
        # 发短信或者邮件告警 (这里仅打印日志)
        echo `date +"%Y-%m-%d %H:%M:%S"` $name "is none"   
        # 6. 执行重启脚本 (-x 显示执行过程)
        sh -x ./$script
    fi
done
```

2.执行验证效果：
![[file-20260203231134619.png | 300]]
![[file-20260203231113632.png]]

# 三.数据仓库Hive从入门到小牛

## 1.快速了解Hive
Hive是建立在Hadoop上的数据仓库的基础架构，提供了一系统工具进行ETL，定义了HQL操作数据。

|**核心概念**|**详细说明**|**关键特性/底层原理**|
|---|---|---|
|**定义与定位**|建立在 Hadoop 之上的**数据仓库**基础构架。|提供了一系列的工具，可以进行数据的**提取、转化、加载 (ETL)**。|
|**查询语言 (HQL)**|定义了简单的类 SQL 查询语言，称为 **HQL**。|允许熟悉 SQL 的用户直接查询 Hadoop 中的数据，降低了学习门槛。|
|**运行机制**|包含 SQL 解析引擎，将 SQL 语句**编译/翻译成 MapReduce (MR) Job**。|最终任务是在 Hadoop 中执行的，本质上是把 SQL 逻辑转化为 MapReduce 程序运行。|
|**数据存储**|Hive 的数据存储基于 Hadoop 的 **HDFS**。|Hive **没有专门的数据存储格式**，它直接利用 HDFS 的文件系统。|
|**支持的文件格式**|默认可以直接加载文本文件 (**TextFile**)。|还支持其他格式，如 **SequenceFile**、**RCFile** 等。|

系统架构：
![[file-20260204230722330.png]]

`select *`一般不走mapreduce。

**Hive计算引擎的变迁：**

| **引擎名称**                         | **核心机制**                                             | **运算速度**                          | **特点与原理**                                                                                  | **优缺点总结**                                                                | **现状/适用场景**                                                                           |
| -------------------------------- | ---------------------------------------------------- | --------------------------------- | ------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------- |
| **MapReduce**<br>(第一代)           | **磁盘 I/O**<br><br>  <br><br>(Disk-based)             | **慢**<br><br>  <br><br>(分钟/小时级)   | **原理：** 将 SQL 翻译成 Map 和 Reduce 两个阶段。中间结果必须写入 HDFS 磁盘，导致大量 I/O。<br>**特点：** 笨重，但极其稳定。        | **✅ 优点：** 稳定性极高，适合超大规模批处理，即使内存不足也能跑。<br>**❌ 缺点：** 速度最慢，延迟高，不适合交互式查询。     | **已淘汰/维护中**<br><br>  <br><br>Hive 2.x 中默认为它，但在 Hive 3.0+ 中已被官方标记为**过时 (Deprecated)**。 |
| **Tez**<br><br>  <br><br>(第二代)   | **DAG 作业**<br><br>  <br><br>(Directed Acyclic Graph) | **中等/快**<br><br>  <br><br>(秒/分钟级) | **原理：** 将多个 MapReduce 任务合并成一个 DAG（有向无环图）作业，减少了中间结果写磁盘的次数。<br><br>**特点：** 专门为 Hive 优化，容器重用。 | **✅ 优点：** 比 MR 快很多，资源利用率高。<br><br>**❌ 缺点：** 社区生态不如 Spark 丰富，主要只服务于 Hive。 | **主流 (Hortonworks 系)**<br><br>  <br><br>在 HDP 发行版中是默认引擎，适合既要稳定又要一定速度的场景。              |
| **Spark**<br><br>  <br><br>(第三代) | **内存计算**<br><br>  <br><br>(In-Memory)                | **极快**<br><br>  <br><br>(毫秒/秒级)   | **原理：** Hive on Spark。利用 Spark 的 RDD 内存计算模型，减少磁盘 I/O，支持 DAG。<br><br>**特点：** 吞吐量大，迭代计算能力强。  | **✅ 优点：** 速度最快，利用内存优势，适合迭代计算和即席查询。<br><br>**❌ 缺点：** 吃内存，对资源配置要求高，调优相对复杂。 | **主流 (Cloudera 系/通用)**<br><br>  <br><br>目前生产环境的主流选择，适合追求高性能的数仓计算。                     |

**Metastore:**

| **核心概念**      | **定义与作用**                                                                | **包含的具体内容**                                                                                                               | **存储引擎对比**                                                                                                                                                                           |
| ------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Metastore** | **Hive 元数据的集中存放地**。<br><br>  <br><br>它是 Hive 能够将非结构化的 HDFS 文件映射为结构化表的关键。 | **元数据 (Metadata)** 包括：<br>1. **表的名字**<br>2. **表的列 (Columns)**<br>3. **分区 (Partitions) 及其属性**<br>4. **表的数据所在目录** (HDFS 路径) | **1. 内嵌 Derby (默认):**<br>Hive 默认使用内嵌的 Derby 数据库。<br>_缺点_：单用户模式，只能一个客户端连接，会在当前目录生成 `derby.log`。<br><br>**2. 外置 MySQL (推荐):**<br>生产环境推荐使用 **MySQL** 作为外置存储引擎。<br>_优点_：支持多用户并发连接，元数据共享。 |
|               |                                                                          |                                                                                                                           |                                                                                                                                                                                      |
## 2.数据库与数据仓库

### 核心知识

|**对比项**|**HIVE**|**MySQL**|
|---|---|---|
|**数据存储位置**|HDFS|本地磁盘|
|**数据格式**|用户定义|系统决定|
|**数据更新**|不支持|支持|
|**索引**|有，但较弱|有|
|**执行**|MapReduce|Executor|
|**执行延迟**|高|低|
|**可扩展性**|高|低|
|**数据规模**|大 _(正常情况下)_|小|

|**对比维度**|**数据库 (Database)**|**数据仓库 (Data Warehouse)**|
|---|---|---|
|**核心简称**|**OLTP** (联机事务处理)|**OLAP** (联机分析处理)|
|**主要目标**|处理日常业务操作，保证数据录入的准确和快速。|处理数据分析，支持管理决策，挖掘历史趋势。|
|**典型代表**|MySQL, PostgreSQL, Oracle, SQL Server|**Hive**, Teradata, Snowflake, Redshift, ClickHouse|
|**数据时效性**|**当前值** (Current)。只保留最新状态，旧数据通常会被覆盖或归档。|**历史数据** (Historical)。存储过去 5-10 年的数据快照，不轻易删除。|
|**数据模型**|**高度范式化 (3NF)**。尽量减少数据冗余，避免更新异常。|**反范式化 (维度建模)**。星型模型/雪花模型，允许冗余以换取查询速度。|
|**操作类型**|**CRUD** (增删改查)。频繁的小数据量读写，并发高。|**只读/批量写入**。主要是复杂的 SELECT 查询，极少进行单行 UPDATE/DELETE。|
|**数据量级**|**GB 级别**。数据量受单机磁盘限制，过大会影响业务性能。|**TB/PB 级别**。基于 HDFS 或云存储，天生支持海量存储。|
|**响应速度**|**毫秒级**。要求实时响应，不能让用户等。|**秒级/分钟/小时级**。容忍度较高，因为处理的数据量巨大。|
|**并发用户**|**成千上万**。所有的终端用户、App、网页都在连接。|**几十/几百人**。主要是数据分析师、高管、报表系统。|
|**底层引擎**|针对行存储优化 (Row-oriented)，方便查单条记录。|通常针对列存储优化 (Column-oriented)，方便做聚合统计 (Sum, Avg)。|

### 实验操作

1.修改配置文件hive-site.xml 、core-site.xml
```xml
<configuration>
    <property>
        <name>javax.jdo.option.ConnectionURL</name>
        <value>jdbc:mysql://mysqlIp:3306/hive?serverTimezone=Asia/Shanghai</value>
    </property>

    <property>
        <name>javax.jdo.option.ConnectionDriverName</name>
        <value>com.mysql.cj.jdbc.Driver</value>
    </property>

    <property>
        <name>javax.jdo.option.ConnectionUserName</name>
        <value>root</value>
    </property>

    <property>
        <name>javax.jdo.option.ConnectionPassword</name>
        <value>你的密码</value>
    </property>
        <property>
        <name>hive.querylog.location</name>
        <value>/data/hive_repo/querylog</value>
    </property>
    <property>
        <name>hive.exec.local.scratchdir</name>
        <value>/data/hive_repo/scratchdir</value>
    </property>

    <property>
        <name>hive.downloaded.resources.dir</name>
        <value>/data/hive_repo/resources</value>
    </property>
</configuration>
```

```xml
<property>
    <name>hadoop.proxyuser.root.hosts</name>
    <value>*</value>
</property>
<property>
    <name>hadoop.proxyuser.root.groups</name>
    <value>*</value>
</property>
```

2.创建数据库用户并授权
```sql
-- 1. 创建用户（如果不存在）
CREATE USER 'root'@'%' IDENTIFIED BY '111111';

-- 2. 授权
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

3.上传mysql driver到lib目录下，进行初始化
```bash
bin/schematool -dbType mysql -initSchema -verbose
```
![[file-20260205234409401.png | 300]]
