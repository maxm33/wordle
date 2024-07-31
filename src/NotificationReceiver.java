package src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationReceiver extends Thread {
    private AtomicBoolean active = new AtomicBoolean(true);
    private ArrayList<String> received;
    private MulticastSocket socket;

    public NotificationReceiver(ArrayList<String> list, MulticastSocket socket) {
        this.received = list;
        this.socket = socket;
    }

    public void run() {
        while (active.get()) {
            try {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                this.socket.receive(packet);
                String message = new String(packet.getData(), StandardCharsets.UTF_8);
                this.received.add(message);
            } catch (SocketTimeoutException so) {
            } catch (IOException e) {
            }
        }
    }

    public void stop_1() {
        active.set(false);
    }
}