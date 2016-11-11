import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.text.SimpleDateFormat;
import java.lang.InterruptedException;

import processing.core.PApplet;

/**
 * Write logs for experiment trials. Ensures that the output stream is opened
 * for each line written to the output file so that the file cannot be corrupted
 * in the event of an early termination of the UI program
 */
public class FileWriter extends PApplet {
    FileOutputStream fos;
    WriterThread wt;
    OutputStreamWriter osw;
    File logFile;
    String sep = "/";
    /** Date formate for appending to log file names*/
    SimpleDateFormat logNameFormat = new SimpleDateFormat("yyyyMMddHHmmss");
  
    /**
     * Create a new FileWriter. Generates a new log file at a path defined by
     * the current time and the mouses name.
     *
     * @param pathname path to directory in which to store logfiles.
     * @param mouse    mouse name to identify each animal
     */
    public FileWriter(String pathname, String mouse) throws IOException {
        File directory = new File(pathname + sep + mouse);
    
        println(directory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        Date logDate = Calendar.getInstance().getTime();
        logFile = new File(pathname + sep + mouse + sep + mouse + "_" +
            logNameFormat.format(logDate) + ".tdml");
        println(logFile);
        //try {
            fos = new FileOutputStream(logFile, false);
        //} catch (IOException e) {
        //    println(e);
        //    println("error creating file");
        //}

        wt = new WriterThread(logFile);
        wt.start();
    }
  
    public File getFile() {
        return new File(logFile.getAbsolutePath());
    }

    /**
     * Write a line to the experiment's log file.
     * @param msg message to be written to the log file
     */
    public void write(String msg) {
        /*
        if (logFile != null) {
            try {
                fos = new FileOutputStream(logFile, true);
                osw = new OutputStreamWriter(fos);
 
                osw.write(
                    msg.replaceAll("[\r|\n]|\\s{2,}","") + "\n");
                osw.close();
                fos.close();
            } catch (IOException e) {
                System.out.println("Error writing to data file");
            }
        }
        */
        wt.queueMessage(msg);
    } 

    public void close() {
        wt.stop_thread();
    }

    public class WriterThread extends Thread {
        private boolean run;
        private LinkedList<String> writeQueue;
        private Thread t;
        private String messageBuffer;

        private FileOutputStream fos;
        private OutputStreamWriter osw;
        private File logFile;
        private String sep = "/";

        WriterThread(File logFile) {
            this.run = true;
            writeQueue = new LinkedList<String>();
            messageBuffer = null;
            this.logFile = logFile;
        }

        public void run() {
            messageBuffer = writeQueue.poll();
            while (this.run || messageBuffer != null) {
                while (messageBuffer != null) {
                    write(messageBuffer);
                    messageBuffer = writeQueue.poll();
                }
                
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
                messageBuffer = writeQueue.poll();
            }
        }

        public void start() {
            if (t == null) {
                t = new Thread(this, "writeThread " + System.nanoTime());
                t.start();
            }
        }

        public void stop_thread() {
            this.run = false;;
        }

        public void queueMessage(String msg) {
            writeQueue.add(msg);
        }

        public void write(String msg) {
            if (logFile != null) {
                try {
                    fos = new FileOutputStream(logFile, true);
                    osw = new OutputStreamWriter(fos);
     
                    osw.write(
                        msg.replaceAll("[\r|\n]|\\s{2,}","") + "\n");
                    osw.close();
                    fos.close();
                } catch (IOException e) {
                    System.out.println("Error writing to data file");
                }
            }
        }
    }
}
