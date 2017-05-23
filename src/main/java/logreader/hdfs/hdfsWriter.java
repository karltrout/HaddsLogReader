package logreader.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.*;
import java.nio.file.Paths;

/**
 * Created by karltrout on 5/14/17.
 */

public class hdfsWriter implements Runnable {


    public static final String FS_PARAM_NAME = "fs.defaultFS";

    public static void main (String[] args){
        Thread t = new Thread(new hdfsWriter());
        t.run();
    }

    @Override
    public void run() {
        String outputDirectory = "/user/karltrout/HADDS";
        String inputDirectory = "/Users/karltrout/Documents/Resources/text/test.fs";
        Path outputPath = new Path(outputDirectory);

        String core_site = "/usr/local/Cellar/hadoop/2.8.0/libexec/etc/hadoop/core-site.xml";
        String hdfs_site = "/usr/local/Cellar/hadoop/2.8.0/libexec/etc/hadoop/hdfs-site.xml";
        Configuration conf = new Configuration();
        conf.addResource(new Path(core_site));
        conf.addResource(new Path(hdfs_site));

        SparkConf sConf = new SparkConf()
                .setMaster("local[2]")
                .setAppName("Hadds Processing");

        System.out.println("configured filesystem = " + conf.get(FS_PARAM_NAME));

        try(FileSystem fs = FileSystem.get(conf)) {


            if (fs.exists(outputPath)) {
                System.err.println("output path exists");

                FileStatus[] files = fs.listStatus(outputPath);
                for (FileStatus file : files ){
                    System.out.println(file.getPath().getName());
                }
            }
            else return;

            JavaSparkContext sc = new JavaSparkContext(sConf);

            // OutputStream os = fs.create(outputPath);


            //try (InputStream is = new BufferedInputStream(new FileInputStream(inputDirectory))) {

            // IOUtils.copyBytes(is, os, conf);

            //}

        }catch (IOException ioe){

            System.out.println("Failed to open outstream");
            ioe.printStackTrace();
        }

    }
}
