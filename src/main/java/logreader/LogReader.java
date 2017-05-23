/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package logreader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Application to Start
 * @author karlt_000
 */
public class LogReader {

    private static final Path outDirectory = Paths.get("/Users/karltrout/Documents/Resources/text/HADDS/zse_test/");
    private static Path rootDirectory = Paths.get("/Users/karltrout/Documents/Resources/hadds/zse/zsehadds2/2017/20170514/");
    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) {

        List<Path> files = new ArrayList<>();
        int maxDepth = 10;

        String pattern = "nas_[0-9]{2}\\.log";
        Number totalBytesCollected = 0;
        Number proccessedBytes = 0;

        long startTime = Calendar.getInstance().getTimeInMillis();

        try (Stream<Path> stream = Files.walk(rootDirectory, maxDepth)) {
            files = stream
                    .filter(path -> path.getFileName().toString().matches(pattern))
                    .collect(toList());
            totalBytesCollected =
                    files.stream().mapToDouble(
                            path -> {
                                try {
                                    return Double.valueOf(Files.size(path)) / 1024 / 1024;
                                } catch (IOException ioe) {
                                    System.out.println("::Warning:: Missing File: " + path.toString());
                                    return 0;
                                }
                            }).sum();

            System.out.println(String.format("Total file size to process: %.2f Mb", totalBytesCollected.doubleValue()));

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        runTest(totalBytesCollected, files);

        long stopTime = Calendar.getInstance().getTimeInMillis();
        Number time = (stopTime - startTime)/1000;
        System.out.println("taskTime(secs) :"+time);
    }

    private static void runSingleFileTest() {
        Path aFilePath = Paths.get("/Users/karltrout/Documents/Resources/hadds/zdc/zdchadds1/2017/20170320/nas_01.log");

        try {

            Thread t = new Thread(new HaddsLogFileParser(aFilePath, outDirectory));
            t.run();

        } catch (IOException e) {
            System.out.println("error Accessing File : Error -> "+e.getMessage());
        }
    }

    private static void runTest(Number totalBytesCollected, List<Path> files) {

        List<Future<?>> futures = new ArrayList<Future<?>>();

        ExecutorService executorService = Executors.newFixedThreadPool(4); //newSingleThreadExecutor();
        AtomicLong totalBytes = new AtomicLong(0);
        AtomicInteger totalFilesRead = new AtomicInteger(0);
        LocalTime start = LocalTime.now();

        for(Path p : files){

            try {

               // String outFileName = p.getFileName().toString().replace(".log", "_flat.txt");
                Path covertedFileDirectory = outDirectory.resolve(rootDirectory.relativize(p)).getParent();

                HaddsLogFileParser hLogParser = new HaddsLogFileParser(p,covertedFileDirectory, totalBytes, totalFilesRead);
                hLogParser.setTesting(false);
                futures.add(executorService.submit(hLogParser));

            }catch (IOException ioe){
                ioe.printStackTrace();
            }

        }
        boolean threadsNotCompleted = true;

        long lastRead = 0; // tracks the total to date of bytes last read.
        long currentRead = 0; // difference from last read to current accumulator;
        int seconds = 0;

        try{
            while(threadsNotCompleted) {

                Thread.sleep(1000);
                seconds++;
                currentRead = totalBytes.get() - lastRead;
                lastRead = lastRead+ currentRead;
                System.out.println(
                        String.format("Running for : %d Secs. Mbs read: %.2f of %.2f from %d file(s) | Mb/sec. : %.2f",
                                seconds,
                                Double.valueOf(lastRead)/1024/1024,
                                totalBytesCollected.doubleValue(),
                                totalFilesRead.get(),
                                Double.valueOf(currentRead)/1024/1024));

                threadsNotCompleted = false;

                for (Future future : futures) {
                    if (!future.isDone()){
                        threadsNotCompleted = true;
                        continue;
                    }
                }
            }

            executorService.shutdown();

        } catch (InterruptedException ie){
            ie.printStackTrace();
        }

        Duration duration = Duration.between(start, LocalTime.now());
        System.out.println("Total Duration to read files: "+duration);

    }

    static int getShort(byte a,  byte b){
        return ((a & 0xff) << 8) | (b & 0xff);
    }

}
