import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

public class Servidor extends Thread{
    protected MulticastSocket socket = null;
    String IP_MULTICAST = "239.0.0.1";
    int PUERTO_MULTICAST_ENTRADA = 4000;
    int PUERTO_MULTICAST_SALIDA = 4001;
    int PUERTO_AUX = 4002;
    int BUF_SIZE = 1024;
    
    public Servidor() throws SocketException, IOException {
        super("Servidor");
        this.socket = new MulticastSocket(PUERTO_MULTICAST_ENTRADA);
    }
    
    public void run() {
        byte[] buffer;
        DatagramPacket packet1, packet2;
        String mensaje;
        String usuarios = "";
        byte[] bytes;
        
        while (true) {
            try {
                buffer = new byte[BUF_SIZE];
                packet1 = new DatagramPacket(buffer, buffer.length);
                packet2 = new DatagramPacket(buffer, buffer.length);
                
                System.out.println("Recibiendo en: " + this.socket.getLocalAddress());
                
                socket.receive(packet1);
                
                mensaje = new String(packet1.getData(), 0, packet1.getLength());
                
                System.out.println("Mensaje Recibido: " + packet1.getAddress() + ":" + packet1.getPort() + ">>>" + mensaje);
                
                InetAddress group = InetAddress.getByName(IP_MULTICAST);
                packet1 = new DatagramPacket(buffer, buffer.length, group, PUERTO_MULTICAST_SALIDA);
                
                if(mensaje.contains("<inicio>"))
                    usuarios += mensaje.substring(mensaje.indexOf(">") + 1) + "%";
                
                if(mensaje.contains("<salir>"))
                    usuarios = usuarios.replace(mensaje.substring(7) + "%", "");
                
                bytes = usuarios.getBytes();
                packet2 = new DatagramPacket(bytes, bytes.length, group, PUERTO_MULTICAST_SALIDA);
                
                socket.send(packet1);
                socket.send(packet2);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        
        socket.close();
    }
    public static void main(String[] args) {
        System.out.println("Servidor Iniciado!!!");
        
        try {
            new Servidor().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}