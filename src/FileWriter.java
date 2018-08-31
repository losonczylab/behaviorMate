import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    public FileWriter(String pathname, String mouse, TrialListener tl) throws IOException {
        File directory = new File(pathname + sep + mouse);

        println(directory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        Date logDate = Calendar.getInstance().getTime();
        logFile = new File(pathname + sep + mouse + sep + mouse + "_" +
            logNameFormat.format(logDate) + ".tdml");
        println(logFile);
        //fos = new FileOutputStream(logFile, false);

        wt = new WriterThread(logFile, tl);
        wt.start();
    }

    public FileWriter(String filename) throws IOException {
        logFile = new File(filename);

        //fos = new FileOutputStream(logFile, false);
        wt = new WriterThread(logFile, null);
        wt.start();
        Date logDate = Calendar.getInstance().getTime();
        this.write(logNameFormat.format(logDate));
    }


    public File getFile() {
        return new File(logFile.getAbsolutePath());
    }

    /**
     * Write a line to the experiment's log file.
     * @param msg message to be written to the log file
     */
    public void write(String msg) {
        if (wt == null) {
            wt = new WriterThread(logFile, null);
            wt.start();
            wt.queueMessage(msg);
            wt.stop_thread();
            wt = null;
        } else {
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
    }

    public void close() {
        if (wt != null) {
            wt.stop_thread();
            wt = null;
        }
    }

    public class WriterThread extends Thread {
        private TrialListener tl;
        private boolean run;
        private ConcurrentLinkedQueue<String> writeQueue;
        private Thread t;
        private String messageBuffer;

        private FileOutputStream fos;
        private OutputStreamWriter osw;
        private File logFile;
        private String sep = "/";

        WriterThread(File logFile, TrialListener tl) {
            //Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            this.run = true;
            writeQueue = new ConcurrentLinkedQueue<String>();
            messageBuffer = null;
            this.logFile = logFile;
            this.tl = tl;
        }

        public void run() {
            messageBuffer = writeQueue.poll();
            while (this.run || messageBuffer != null) {
                if (messageBuffer != null) {
                    try {
                        fos = new FileOutputStream(logFile, true);
                        osw = new OutputStreamWriter(fos);
                    } catch (IOException e) {
                        System.out.println(e);
                        tl.exception("error opening log file");
                    }

                    for (int j = 0; ((messageBuffer != null) && (j < 10)); j++) {
                        try {
                            //write(messageBuffer);

                            osw.write(
                                messageBuffer.replaceAll("[\r|\n]|\\s{2,}","") + "\n");
                        } catch (IOException e) {
                            String alert = e.toString();
                            StackTraceElement[] elements = e.getStackTrace();
                            for (int i=0; ((i < 3) && (i < elements.length)); i++) {
                                alert += ("\n" + elements[i].toString());
                            }
                            if (tl != null) {
                                tl.exception(alert);
                            }
                            break;
                        }
                        messageBuffer = writeQueue.poll();
                    }

                    try {
                        osw.close();
                        fos.close();
                    } catch (IOException e) {
                        System.out.println(e);
                        tl.exception("error saving log file");
                    }
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

        public void write(String msg) throws IOException {
            if (logFile != null) {
                //fos = new FileOutputStream(logFile, true);
                ///osw = new OutputStreamWriter(fos);

                osw.write(
                    msg.replaceAll("[\r|\n]|\\s{2,}","") + "\n");
                //osw.close();
                //fos.close();
            }
        }
    }
}
