import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import static java.awt.image.ImageObserver.WIDTH;
import java.io.IOException;
import static java.lang.System.exit;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.layout.Border;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class Cliente extends javax.swing.JFrame {
    String IP_MULTICAST = "239.0.0.1";
    int PUERTO_MULTICAST_ENTRADA = 4001;
    int PUERTO_MULTICAST_SALIDA = 4000;
    int TAMANIO_BUFFER = 1024;
    private Enviar enviar = null;
    String nombre_usuario;
    Icon iconos[];
    static String mensajeInput = "";
    
    private class Escuchador extends SwingWorker<Void, String> {
        //El primer argumento es el tipo de retorno del hilo.
        //El segundo argumento es tipo del valor que va a ir cambiando en el proceso.

        //Este es el metodo que realiza la tarea 'pesada' o tardada, separada de la interfaz
        //Se invoca llamando al metodo execute().
        @Override
        public Void doInBackground() {
            MulticastSocket socket = null;
            InetAddress direccion = null;
                        
            try {
                socket = new MulticastSocket(PUERTO_MULTICAST_ENTRADA);
                direccion = InetAddress.getByName(IP_MULTICAST);
                socket.joinGroup(direccion);

                while (!isCancelled()) {
                    byte[] buf1 = new byte[TAMANIO_BUFFER];
                    byte[] buf2 = new byte[TAMANIO_BUFFER];
                    DatagramPacket packet1 = new DatagramPacket(buf1, buf1.length);
                    DatagramPacket packet2 = new DatagramPacket(buf2, buf2.length);

                    System.out.println("Recibiendo en: " + direccion);

                    socket.receive(packet1);
                    socket.receive(packet2);
                    
                    String mensaje = new String(packet1.getData()).trim();
                    String cadena_usuarios = new String(packet2.getData()).trim();

                    if(!(mensaje.contains("<privado>") && !mensaje.contains("<" + nombre_usuario + ">"))){
                        System.out.println("Mensaje Recibido: " + packet1.getAddress() + " " + packet1.getPort() + " " + mensaje);
                        System.out.println("Mensaje Recibido: " + packet2.getAddress() + " " + packet2.getPort() + " " + cadena_usuarios);
                        publish(mensaje,cadena_usuarios);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if(socket != null)
                    try{
                        socket.leaveGroup(direccion);
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                socket.close();
                socket = null;
            }

            return null;
        }

        //Este metodo es que va a ir actualizando la interfaz de acuerdo a lo que vaya realizando la tarea 'pesada'
        //Se invoca llamando al metodo publish(...);
        @Override
        protected void process(List<String> mensajes) {
            String mensaje = mensajes.get(0);
            String usuarios = mensajes.get(1);
            String[] usuarios_aux = usuarios.split("%");
            DefaultListModel modelo = new DefaultListModel();
            SimpleAttributeSet atributos = new SimpleAttributeSet();
            
            System.out.println("Mostrando: " + mensajes.size() + " messages");
            System.out.println("Insertar mensaje(" + mensaje + ")");
            
            try {
                if(mensaje.contains("<inicio>")){
                    JTA_Conversacion.getStyledDocument().insertString(JTA_Conversacion.getStyledDocument().getLength(), "El usuario " + mensaje.substring(8) + " ha entrado al chat\n", atributos);
                }
                else if(mensaje.contains("<salir>")){
                    JTA_Conversacion.getStyledDocument().insertString(JTA_Conversacion.getStyledDocument().getLength(), "El usuario " + mensaje.substring(7) + " ha salido del chat\n", atributos);
                }
                else{
                    StyleConstants.setForeground(atributos, Color.red);
                        
                    if(mensaje.contains("<msj>")){
                        mensaje = mensaje.substring(6);
                        JTA_Conversacion.getStyledDocument().insertString(JTA_Conversacion.getStyledDocument().getLength(), mensaje.substring(0, mensaje.indexOf(">")) + ": ", atributos);
                        mensaje = mensaje.substring(mensaje.indexOf(">") + 1);
                    }
                    else{
                        mensaje = mensaje.substring(10);
                        JTA_Conversacion.getStyledDocument().insertString(JTA_Conversacion.getStyledDocument().getLength(), mensaje.substring(0, mensaje.indexOf(">")) + " (Privado): ", atributos);
                        mensaje = mensaje.substring(mensaje.indexOf(">") + 1);
                        mensaje = mensaje.substring(mensaje.indexOf(">") + 1);
                    }
                    
                    StyleConstants.setForeground(atributos, Color.BLACK);
                    if(mensaje.contains("<zumbido>")){
                        zumbar();
                        JTA_Conversacion.getStyledDocument().insertString(JTA_Conversacion.getStyledDocument().getLength(), "ha enviado un zumbido xD", atributos);
                    }else{
                        String txtEmojis = mensaje.substring(1, mensaje.indexOf("}"));
                        System.out.println("Emojis: " + txtEmojis);
                        String[] arrayEmojis = txtEmojis.split("~");
                        mensaje = mensaje.substring(mensaje.indexOf("}") + 1);
                        for(int i = 0; i < mensaje.length(); i++){
                            int flag = 0;
                            for(int j = 0; j < arrayEmojis.length; j++){
                                flag = 0;
                                if(arrayEmojis[j].contains(",")){
                                    if(i == Integer.parseInt(arrayEmojis[j].split(",")[1])){
                                        JTA_Conversacion.setCaretPosition(JTA_Conversacion.getStyledDocument().getLength());
                                        JTA_Conversacion.insertIcon(iconos[Integer.parseInt(arrayEmojis[j].split(",")[0])]);
                                        flag = 1;
                                        break;
                                    }
                                }
                            }
                            if(flag == 1)
                                continue;
                            JTA_Conversacion.getStyledDocument().insertString(JTA_Conversacion.getStyledDocument().getLength(), mensaje.charAt(i)+"", atributos);
                        }
                    }
                    JTA_Conversacion.getStyledDocument().insertString(JTA_Conversacion.getStyledDocument().getLength(), "\n", atributos);
                }
            } catch (BadLocationException ex) {
                Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            for(String usuario : usuarios_aux){
                modelo.addElement(usuario);
            }
            
            JL_Usuarios.setModel(modelo);
        }
    }

    private class Enviar {
        private DatagramSocket socket = null;
        private InetAddress address = null;

        Enviar(String host) throws SocketException, UnknownHostException {
            this.socket = new DatagramSocket();
            this.address = InetAddress.getByName(host);
        }

        public void enviar(String message) throws IOException {
            byte[] bytes = message.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, this.address, PUERTO_MULTICAST_SALIDA);
            String data = new String(packet.getData(), 0, packet.getLength());

            System.out.println("Enviando: " + packet.getAddress() + " " + packet.getPort() + " " + data);

            this.socket.send(packet);

            System.out.println("Enviado OK");
        }

        public void close() {
            if(socket!=null)
                socket.close();
        }
    }

    public Cliente(String username, String host) {
        this.setTitle("QuéTranzapp - " + username);
        
        nombre_usuario = username;
        iconos = new ImageIcon[231];
        mensajeInput = "<msj><"+nombre_usuario+">{";
        for(int i = 1 ; i <= 230 ; i++){
            ImageIcon imageIcon = new ImageIcon("Emojis/" + i + ".png");
            Image image = imageIcon.getImage();
            imageIcon = new ImageIcon(image.getScaledInstance(25, 25, java.awt.Image.SCALE_SMOOTH));
            iconos[i] = imageIcon;
        }
        
        initComponents();
        
        JTF_Mensaje.requestFocus();
        
        new Escuchador().execute();
        
        try {
            enviar = new Enviar(host);
            enviar.enviar("<inicio>" + username );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jFrame1 = new javax.swing.JFrame();
        jScrollPane3 = new javax.swing.JScrollPane();
        JTF_Mensaje = new javax.swing.JTextField();
        JBtn_Enviar = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        JL_Usuarios = new javax.swing.JList();
        JRB_Mensaje_Grupal = new javax.swing.JRadioButton();
        JRB_Mensaje_Privado = new javax.swing.JRadioButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        JTA_Conversacion = new javax.swing.JTextPane();
        JLBL_Usuarios = new javax.swing.JLabel();
        JB_Salir = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        JTF_Mensaje.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JTF_MensajeActionPerformed(evt);
            }
        });
        JTF_Mensaje.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                JTF_MensajeKeyPressed(evt);
            }
        });

        JBtn_Enviar.setText(">");
        JBtn_Enviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JBtn_EnviarActionPerformed(evt);
            }
        });

        jScrollPane2.setViewportView(JL_Usuarios);

        buttonGroup1.add(JRB_Mensaje_Grupal);
        JRB_Mensaje_Grupal.setSelected(true);
        JRB_Mensaje_Grupal.setText("Mensaje Grupal");
        JRB_Mensaje_Grupal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JRB_Mensaje_GrupalActionPerformed(evt);
            }
        });

        buttonGroup1.add(JRB_Mensaje_Privado);
        JRB_Mensaje_Privado.setText("Mensaje Privado");
        JRB_Mensaje_Privado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JRB_Mensaje_PrivadoActionPerformed(evt);
            }
        });

        jScrollPane1.setViewportView(JTA_Conversacion);

        JLBL_Usuarios.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        JLBL_Usuarios.setText("Usuarios en línea:");

        JB_Salir.setText("Salir");
        JB_Salir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JB_SalirActionPerformed(evt);
            }
        });

        jButton1.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jButton1.setText("☺");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jButton2.setText("☼");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(JRB_Mensaje_Grupal)
                        .addComponent(JRB_Mensaje_Privado)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addComponent(JLBL_Usuarios, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(JB_Salir, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(29, 29, 29)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JTF_Mensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(JBtn_Enviar)))
                .addGap(31, 31, 31))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(39, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(JLBL_Usuarios, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(JRB_Mensaje_Grupal)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(JRB_Mensaje_Privado)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(JB_Salir)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(JBtn_Enviar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(1, 1, 1))
                    .addComponent(JTF_Mensaje)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(19, 19, 19))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void JRB_Mensaje_GrupalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JRB_Mensaje_GrupalActionPerformed
        mensajeInput = "<msj><" + nombre_usuario + ">{";
        JTF_Mensaje.setEnabled(true);
        JTF_Mensaje.requestFocus();
    }//GEN-LAST:event_JRB_Mensaje_GrupalActionPerformed

    private void JRB_Mensaje_PrivadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JRB_Mensaje_PrivadoActionPerformed
        if(JL_Usuarios.getSelectedValue() == null)
            JOptionPane.showMessageDialog(this, "Debes seleccionar primero a quien se lo quieres enviar");
        else if(JL_Usuarios.getSelectedValue().toString().equals(nombre_usuario))
            JOptionPane.showMessageDialog(this, "No puedes enviarte mensajes tu mismo ¬¬");
        else{
            mensajeInput = "<privado><" + nombre_usuario + "><" + JL_Usuarios.getSelectedValue().toString() + ">{";
            JTF_Mensaje.setEnabled(true);
        }
        
        JTF_Mensaje.requestFocus();
    }//GEN-LAST:event_JRB_Mensaje_PrivadoActionPerformed

    private void JTF_MensajeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JTF_MensajeActionPerformed
        JBtn_EnviarActionPerformed(evt);
    }//GEN-LAST:event_JTF_MensajeActionPerformed

    private void JB_SalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JB_SalirActionPerformed
        try {
            String message = "<salir>" + nombre_usuario;
            
            if ("".equals(message.trim()))
                return;
            
            enviar.enviar(message);
            
            exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(this, e.toString(), "Exception", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_JB_SalirActionPerformed

    private void JTF_MensajeKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_JTF_MensajeKeyPressed
        
    }//GEN-LAST:event_JTF_MensajeKeyPressed

    private void JBtn_EnviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JBtn_EnviarActionPerformed
        try {
            if(mensajeInput.contains("~"))
                mensajeInput = mensajeInput.substring(0, mensajeInput.length() - 1);
            String message = mensajeInput + "}"  + JTF_Mensaje.getText();
            JTF_Mensaje.setText("");
            if(mensajeInput.contains("msj"))
                mensajeInput = "<msj><"+nombre_usuario+">{";
            else if(mensajeInput.contains("privado"))
                //Hacer algo aquí
            if ("".equals(message.trim()))
                return;

            enviar.enviar(message);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(this, e.toString(), "Exception", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_JBtn_EnviarActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        //Cargar emojis        
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(210, panel.getHeight()));
        int w = 0;
        int h = 0;
        for(int i = 1; i <= 230; i++){
            JLabel aux = new JLabel();
            aux.setSize(25, 25);
            final int x = i;
            //aux.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
            aux.setIcon(iconos[i]);
            aux.setLocation(w, h);
            aux.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            aux.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(MouseEvent e){
                    mensajeInput += x+","+JTF_Mensaje.getText().length()+"~";
                    JTF_Mensaje.setText(JTF_Mensaje.getText()+"☺");
                    JTF_Mensaje.requestFocus();
                }
            });
            panel.add(aux);
            w += 25;
            if(w == 175){
                w = 0;
                h += 25;
            }
        }
        
        jFrame1 = new JFrame();
        jFrame1.setSize(new Dimension(430, 545));
        jFrame1.setLocation(657, 0);
        jFrame1.setResizable(false);
        jFrame1.add(panel);
        jFrame1.setVisible(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    public void zumbar() {
        Toolkit.getDefaultToolkit().beep();
        final Timer timer = new Timer(100, null);
        timer.addActionListener((e) -> {
            for(int i = 0; i < 40; i++){
                this.setLocation(this.getLocation().x - 10, this.getLocation().y);
                this.setLocation(this.getLocation().x + 10, this.getLocation().y);
                this.setLocation(this.getLocation().x + 10, this.getLocation().y);
                this.setLocation(this.getLocation().x - 10, this.getLocation().y);
                if(i == 4)
                    timer.stop();
            }            
        });
        timer.start();

    }
    
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            mensajeInput += "}<zumbido>";
            JBtn_EnviarActionPerformed(evt);
        } catch (Exception ex) {
            Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        final String username = getUsername(args);
        final String servidor = getServidor(args);
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Cliente.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Cliente.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Cliente.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Cliente.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                mensajeInput = "<msj><"+username+">{";
                new Cliente(username, servidor).setVisible(true);
            }
        });
    }
    
    private static String getUsername(String[] args) {
        if (args.length > 0)
            return args[0];
        
        String username = JOptionPane.showInputDialog(null, "Nombre de Usuario", "Ingresar nombre de usuario", JOptionPane.QUESTION_MESSAGE);
        
        if ( username != null && username.trim().length() > 0 )
            return username;
        
        return "Invitado";
    }
    
    private static String getServidor(String[] args) {
        if (args.length > 1)
            return args[1];
        
        return "localhost";
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton JB_Salir;
    private javax.swing.JButton JBtn_Enviar;
    private javax.swing.JLabel JLBL_Usuarios;
    private javax.swing.JList JL_Usuarios;
    private javax.swing.JRadioButton JRB_Mensaje_Grupal;
    private javax.swing.JRadioButton JRB_Mensaje_Privado;
    private javax.swing.JTextPane JTA_Conversacion;
    private javax.swing.JTextField JTF_Mensaje;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    // End of variables declaration//GEN-END:variables
}
