package logreader;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import static logreader.LogReader.getShort;

/**
 * Created by karl trout on 5/10/17.
 * This class is meant to parse a single HADDS log file and write it to a text file.
 * The new format is single line per message ascii file, space delimited key value pairs separated by a ':'.
 */
public class HaddsLogFileParser implements Runnable {

    private AtomicInteger filesCounter = null;
    private AtomicLong counter = null;
    private Path path;
    private Path outPath;
    private int byteCnt = 0;
    private int mId = 0;
    private boolean notTesting = true;
    private Map<Integer, Message> messages = new TreeMap<>();

    private Path outDirectory = null;

    /**
     * @param aFilePath the HADDS log file to parse
     * @throws IOException if the File to read or to write is not available a IOException is thrown.
     */
    HaddsLogFileParser(Path aFilePath,  Path outputDirectory) throws IOException {
        this.path = aFilePath;
        this.outDirectory = outputDirectory;
        setOutputFile();
    }

    HaddsLogFileParser(Path fileToParse, Path outputDirectory, AtomicLong counter, AtomicInteger filesRead) throws IOException {
        this.path = fileToParse;
        this.outDirectory = outputDirectory;
        this.filesCounter = filesRead;
        this.counter = counter;
        setOutputFile();
    }

    private void setOutputFile() throws IOException {
        if(outDirectory == null) throw new IOException("Please Set a Directory for Output");
        String outFileName = this.path.getFileName().toString().replace(".log", "_flat.txt");
        if (Files.notExists(outDirectory)){
            Files.createDirectories(outDirectory);
        }
        this.outPath=this.outDirectory.resolve(outFileName);
    }

    @Override
    public void run() {
        try {

            byte[] fileBytes = Files.readAllBytes(this.path);
            //int fileSize = fileBytes.length;

            if(path.getFileName().toString().contains("_01.log")){
                //we are a nas_01.log, we have an extra timestamp data to parse
                parseNasTimeSourceHeader(getBytes(4, fileBytes));
            }

            int byteTracker = 0;
            while (this.byteCnt < fileBytes.length){

                mId++;
                Message message = new Message(mId);
                setTimeAndBlock(getBytes(12, fileBytes), message);

                getMessage(fileBytes,message);
                messages.put(mId,message);

                if(this.counter != null){ this.counter.addAndGet(this.byteCnt - byteTracker);}
                byteTracker = this.byteCnt;

            }

            if (this.filesCounter != null ) { this.filesCounter.incrementAndGet();}

            if(notTesting) {
                try (FileWriter fw = new FileWriter(outPath.toFile())) {

                    for (Message m : messages.values()) {
                        fw.write(m.toString());
                        fw.write("\n");
                    }

                    fw.flush();
                    fw.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] getBytes(int numberOfBytes, byte[] fromArray){

        byte[] bytes = Arrays.copyOfRange(fromArray, this.byteCnt, this.byteCnt+numberOfBytes);
        this.byteCnt = this.byteCnt + numberOfBytes;
        return bytes;

    }

    private static void setTimeAndBlock(byte[] bytes, Message message)
            throws UnsupportedEncodingException {

        long timeBytes = 0;
        for (int i = 0; i < 4; i++)
        {
            timeBytes = (timeBytes << 8) + (bytes[i] & 0xff);
        }
        /*
         * these next 6 bytes are as of yet unknown
         * 05/10/2016
         */
        long unkn = 0;
        for (int i = 4; i < 10; i++)
        {
            unkn = (unkn << 8) + (bytes[i] & 0xff);
        }

        int blkNum = getShort(bytes[10], bytes[11]);

        message.setTimeStamp(timeBytes);
        message.setMessageBlock(blkNum);

    }

    private void parseNasTimeSourceHeader(byte[] bytes )
            throws UnsupportedEncodingException {

        long timeBytes = 0;
        for (int i = 0; i < 4; i++)
        {
            timeBytes = (timeBytes << 8) + (bytes[i] & 0xff);
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeBytes*1000);

        System.out.println(":: NAS File 01 Date: "+cal.getTime());

    }

    private void getMessage(byte[] bytes, Message message) {

        byte[] dst = getBytes(8, bytes);

        byte[] src = getBytes(8, bytes);
        int sz =((bytes[this.byteCnt++] & 0xff) << 8) | (bytes[this.byteCnt++] & 0xff);

        byte[] type = getBytes(2, bytes);

        try {

            String dest = new String(dst, "IBM-1047");
            String source = new String(src, "IBM-1047");
            String typ = new String(type, "IBM-1047");

            message.addHeader(source, dest, typ);

            int fieldByteSize = sz - 20 ; // header is 20 fileBytes long
            getFields(getBytes(fieldByteSize, bytes), message);

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LogReader.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void getFields(byte[] bytes, Message message) throws UnsupportedEncodingException {
        int dataSize = bytes.length;
        int b = 0;
        while (dataSize > 0){
            int fieldSize =((bytes[b++] & 0xff) << 8) | (bytes[b++] & 0xff);

            int fnum =((bytes[b++] & 0xff) << 8) | (bytes[b++] & 0xff);

            String fvar = new String(Arrays.copyOfRange(bytes, b++, b), "IBM-1047");

            byte[] fdata = Arrays.copyOfRange(bytes, b, b+fieldSize);

            message.addField(fnum, fvar, fdata);
            b=b+fieldSize;
            if (b == dataSize){
                break ;
            }
        }
    }

    public void setTesting(boolean testing) {
        notTesting = !testing;
    }

}
