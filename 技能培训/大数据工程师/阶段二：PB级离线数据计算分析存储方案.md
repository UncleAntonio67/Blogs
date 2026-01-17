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
  
    // 总数据量  
    private static final int TOTAL_COUNT = 100_000_000;  
    // 倾斜 key    private static final int SKEW_KEY = 500;  
    // 倾斜 key 数量  
    private static final int SKEW_COUNT = 9_000_000;  
  
    public static void main(String[] args) {  
        String outputFile = "skew_data.txt";  
  
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {  
  
            // 1. 写入 900 万条 500            for (int i = 0; i < SKEW_COUNT; i++) {  
                writer.write(String.valueOf(SKEW_KEY));  
                writer.newLine();  
            }  
  
            // 2. 写入剩余 100 万条（1-999，排除 500）  
            Random random = new Random();  
            int remaining = TOTAL_COUNT - SKEW_COUNT;  
  
            for (int i = 0; i < remaining; i++) {  
                int value;  
                do {  
                    value = random.nextInt(999) + 1; // 1 - 999  
                } while (value == SKEW_KEY);  
  
                writer.write(String.valueOf(value));  
                writer.newLine();  
            }  
  
            writer.flush();  
            System.out.println("数据生成完成，文件路径：" + outputFile);  
  
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
hadoop jar /export/data/db_hadoop-1.0-SNAPSHOT.jar com.imooc.mr.WordCountJobSkew /skew_data.txt /out10000 1
```

![[file-20260117181507365.png | 600]]
![[file-20260117183039458.png | 550]]

![[file-20260117183327880.png | 400]]

[[HDFS数据挂在根分区下，频繁提示空间不足如何处理]]

- 5个reduce的效果，相差不大
```shell
hadoop jar /export/data/db_hadoop-1.0-SNAPSHOT.jar com.imooc.mr.WordCountJobSkew /skew_data.txt /out10000 5
```

![[file-20260117232718717.png]]

[[为什么很小的文件，在MapReduce上进行运算后数据占用空间很大]]

4.编写通过增加打散数据，并编译打包
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
            //打散数据
                Random random = new Random();  
                key1 = "5" + "_" + random.nextInt(10);  
            }  
  
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