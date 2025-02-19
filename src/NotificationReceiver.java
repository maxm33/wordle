package src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationReceiver extends Thread {
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final ArrayList<String> received;
    private final MulticastSocket socket;

    public NotificationReceiver(ArrayList<String> list, MulticastSocket socket) {
        this.received = list;
        this.socket = socket;
    }

    @Override
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