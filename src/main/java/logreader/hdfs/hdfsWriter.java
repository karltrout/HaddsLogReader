package logreader.hdfs;

import logreader.Message;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

            JavaRDD<String> lines = sc.textFile("hdfs://localhost:9000/user/karltrout/HADDS/*/*/*/*/nas_*_flat.txt");
            //#Get departing flights from KJFK acids list
            //        rows = lines.map(lambda line: line.split(" ")).filter(lambda r: len(r) > 4 and str(r[5]) == 'TYP:DH' and str(r[13]) == '26A:KJFK')
            //acids = rows.map( lambda a: (a[7].split(':')[1], a))

            JavaRDD<String[]> rows = lines.map(line -> line.split(" ")).filter(
                    arr -> arr.length > 4
                            && String.valueOf(arr[5]).equals("TYP:DH")
                            && String.valueOf(arr[13]).equals("26A:KJFK") );

            JavaPairRDD<String,String[]> acids = rows.mapToPair(flt ->
            {
                String acid = flt[7].split(":")[1];
                return new Tuple2(acid,flt);
            });
            //long roCnt = rows.count();
            //System.out.println("There are "+roCnt+" DH messages from KJFK airport.");
           /* List<Tuple2<String, String[]>> acidList = acids.take(1);
            Tuple2<String, String[]> acid1 = acidList.get(0);
            System.out.println(">>> My First Acid is "+acid1._1+" with array values "+ Arrays.toString(acid1._2()));
*/

/*            # Organize TH rows
            from hadds import th2AMessageList
            throws = lines.map(lambda line: line.split(" ")).filter(lambda r: len(r) > 4 and str(r[5]) == 'TYP:TH').map(lambda th: th2AMessageList(th)).map( lambda th: (th[0][0].split(':')[1],th[0]) )*/

            JavaRDD<String[]> thMessagesRows = lines.map(line -> line.split(" ")).filter(
                    thRows -> thRows.length > 4
                            && String.valueOf(thRows[5]).equals("TYP:TH")
                            );
//                            .map( thRow -> {
//                                //th2MessageList(thRow)
//                                ArrayList<String[]> result = new ArrayList();
//                                int fieldId = 0;
//                                int lastFieldId = 0;
//                                for (String field : thRow){
//                                    if (field.startsWith("2A:")){
//                                        if (lastFieldId != 0)
//                                            result.add(Arrays.copyOfRange(thRow,lastFieldId,fieldId));
//
//                                        lastFieldId = fieldId;
//                                    }
//                                    fieldId++;
//                                }
//                                result.add(Arrays.copyOfRange(thRow,lastFieldId, thRow.length - 1));
//                                if (result.size() < 1) {
//                                    String[] none = {"2A:NONE"};
//                                    result.add(none);
//                                none}d

//                                return result.toArray(new String[result.size()]);

//            });

            List<String[]> messageArray = thMessagesRows.take(1);

            System.out.println(">>> First TH Message size is: "+messageArray.get(0).length);//+Arrays.toString(messageArray.get(0)));

        }catch (IOException ioe){

            System.out.println("Failed to open outstream");
            ioe.printStackTrace();
        }

    }
}
