import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import processing.core.PApplet;
import processing.data.JSONObject;


class UdpClient extends PApplet {
    SocketAddress arduinoAddress;
    DatagramPacket incomingUdp;
    DatagramSocket udpSocket;
    ReceiveThread rt;
    String address;
    FileWriter mL;

    public UdpClient(int arduinoPort, int receivePort) throws IOException {
        String ip = "127.0.0.1";
        arduinoAddress = new InetSocketAddress("127.0.0.1", arduinoPort);
        this.address = ip + ":" + receivePort;

        try {
            udpSocket = new DatagramSocket(receivePort);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("error connecting to " + this.address);
        }

        mL = new FileWriter("logs/" + ip + "." + receivePort + ".log");
        rt = new ReceiveThread(udpSocket, receivePort, this.mL);
        rt.start();
    }

    public UdpClient(String ip, int arduinoPort, int receivePort) throws IOException {
        arduinoAddress = new InetSocketAddress(ip,arduinoPort);
        this.address = ip + ":" + receivePort;

        try {
            udpSocket = new DatagramSocket(receivePort);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("error connecting to " + this.address);
        }

        mL = new FileWriter("logs/" + ip + "." + receivePort + ".log");
        rt = new ReceiveThread(udpSocket, receivePort, this.mL);
        rt.start();
    }

    public UdpClient(String ip, int arduinoPort) throws IOException {
        arduinoAddress = new InetSocketAddress(ip,arduinoPort);
        this.address = ip;

        try {
            udpSocket = new DatagramSocket(null);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("error connecting to " + this.address);
        }
    }


    void sendMessage(String message) {
      message = message.replaceAll("[\r|\n|\\s]", "");

      try {
          this.mL.write("[SEND] " + message);
          byte[] sendData = message.getBytes("UTF-8");
          DatagramPacket sendPacket = new DatagramPacket(sendData, 0,
            sendData.length, arduinoAddress);
          udpSocket.send(sendPacket);
      } catch (IOException e) {
        println("error sending to " + this.address + ": " + message);
        println(e);
      }
    }

    boolean receiveMessage(JSONBuffer json) {
        String message = this.rt.poll();
        if (message != null) {
            json.json = new JSONObject();
            try {
                json.json.setJSONObject(this.address, JSONObject.parse(message));
            } catch (RuntimeException e) {
                return false;
            }

            return true;
        }

        return false;
    }

    public class ReceiveThread extends Thread {
        private Thread t;
        private DatagramSocket sock;
        private DatagramPacket incomingUdp;
        private ConcurrentLinkedQueue<String> messageQueue;
        //private byte[] receiveData;
        private boolean run;
        private boolean debug;
        private int receivePort;
        private FileWriter mL;

        ReceiveThread(DatagramSocket sock, int receivePort, FileWriter mL) {
            this.run = true;
            this.debug = false;
            this.mL = mL;
            this.receivePort = receivePort;
            this.sock = sock;
            try {
                this.sock.setSoTimeout(50);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
            messageQueue = new ConcurrentLinkedQueue<String>();
        }

        public String poll() {
            return messageQueue.poll();
        }

        public void run() {
            while (this.run) {
                byte[] receiveData = new byte[1024];
                incomingUdp = new DatagramPacket(receiveData, receiveData.length);
                try {
                    sock.receive(incomingUdp);
                } catch (IOException e) {
                    continue;
                }

                String message = new String(
                    incomingUdp.getData(), 0, incomingUdp.getLength());
                this.mL.write("[RECEIVE] " + message);
                messageQueue.add(message);
            }
            this.sock.close();
        }

        public void start() {
            if (t == null) {
                t = new Thread (this, "name " + System.nanoTime());
                t.start();
            }
        }

        public void stop_thread() {
            this.run = false;
        }
    }

    void flushBuffer() {
        JSONBuffer json = new JSONBuffer();
        while (receiveMessage(json)) {}
    }

    void closeSocket() {
        if (rt != null) {
            rt.stop_thread();
        } else {
            udpSocket.close();
        }
    }
}
