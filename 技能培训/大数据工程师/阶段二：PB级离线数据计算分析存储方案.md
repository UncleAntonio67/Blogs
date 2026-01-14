# 一.拿来就用的企业级解决方案

## 1.剖析小文件问题与企业级解决方案

### 核心知识

- 小文件问题：Hadoop在小文件的处理上不但效率低下，而且十分消耗内存资源，解决方案是使用容器，包括SequenceFile和

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
  
import java.io.File;  
import java.io.IOException;  
  
public class SmallFileSeq {  
  
    public static void main(String[] args) throws IOException {  
        write("D:\\Project\\Java\\db_hadoop\\test","/seqFile");  
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
}
```
