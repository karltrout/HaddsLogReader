/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package logreader;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 *
 * @author karlt_000
 */
public class Message {
   
    ArrayList<Tuple> fields;
    String source;
    String destination;
    String type;
    long timeStamp;
    int messageId;
    int messageBlock;
    
    Message() throws Exception{
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
    Message (int mb){
      this.messageId = mb;
      fields = new ArrayList<>();
    }
    
    public void setTimeStamp(long timeStamp){
        this.timeStamp = timeStamp;
    }
    
    public void setMessageBlock(int mb){
        this.messageBlock = mb;
    }
    
    public void addHeader(String source, String dest, String type){
        
        this.source = source;
        this.destination = dest;
        this.type = type;
        
    }
    
    public void addField(int fNum, String fVariation, byte[] fData) throws UnsupportedEncodingException {
        String fId = String.format("%d%s", fNum, fVariation);
        String fDataStr = valueCheck(fId, fData);
        fields.add(new Tuple(fId, fDataStr));
    }

    private String valueCheck(String fId, byte[] fData) throws UnsupportedEncodingException {
        String result = "N/A";
        switch (fId){
            case "170A":
                result = get32BitString(fData);
                break;
            case "173A":
                result = get32BitString(fData);
                break;
            case "177A":
                result = get32BitString(fData);
                break;
            case "316A":
                result =  new String(fData, "UTF-8");
                break;
            case "167A":
                result = String.format("%02x%02x", fData[0],fData[1]);
                break;
            default:
                result = new String(fData, "IBM-1047");
        }

        return result;

    }

    private String get32BitString(byte[] bytes) {
        long timeBytes = 0;
        for (int i = 0; i < 4; i++)
        {
            timeBytes = (timeBytes << 8) + (bytes[i] & 0xff);
        }
        return String.valueOf(timeBytes);
    }

    @Override
    public String toString(){
        String data = String.format("M:%d TS:%d MB:%d DST:%s SRC:%s TYP:%s", 
                this.messageId, 
                this.timeStamp,
                this.messageBlock,
                this.destination,
                this.source, 
                this.type);
        
        StringBuilder sb = new StringBuilder();
        sb.append(data);
        for (Tuple t : this.fields){
            sb.append(" ").append(t);
        }
        
        return sb.toString();
    }

    public static class Tuple<T, U> {
        public final T _1;
        public final U _2;
        public Tuple(T arg1, U arg2) {
            super();
            this._1 = arg1;
            this._2 = arg2;
        }
        @Override
        public String toString() {
            return String.format("%s:%s", _1, _2);
        }
    }
}