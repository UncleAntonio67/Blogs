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

通过`job.setPartitionerClass()`可以定义分区类
