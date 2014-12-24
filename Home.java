import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Matheus Stutzel on 25/10/2014.
 */
public class Home {
    public  JTextField textField1;
    public  JButton button1;
    public  JTabbedPane tabbedPane1;
    public  JButton iniciarButton;
    public  JComboBox comboBox1;
    public  JComboBox comboBox2;
    public  JTextField textField2;
    public  JTextField textField3;
    public  JTextField textField4;
    public  JTextField textField5;
    public  JTextField textField6;
    public  JTextField textField7;
    public  JButton cancelarButton;
    public  JButton confirmarButton;
    public  JTextArea asdasdTextArea;
    public  JList list1;
    public  JPanel RemoteSpeaker;
    public  JButton conectarAoServidorButton;
    private JButton atualizarButton;
    private JLabel activeLabel;
    private JLabel myIPLabel;
    private JLabel clientCont;
    public  DefaultListModel lista1;
    Thread client,server;
    TCPClient Client;
    TCPServer Server;
    private boolean Conectado,servidor;
    private static final String validIP="(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}";

    public Home() {
        setClient();
        setServidor();
    }



    private void setServidor() {
        activeLabel.setText("Não");
        servidor=false;
        String ip;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                    if(ip.matches(validIP))
                        myIPLabel.setText(myIPLabel.getText()+","+ip);
                }
            }
            myIPLabel.setText(myIPLabel.getText().replaceFirst(",", ""));
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        iniciarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(servidor){
                    Server.finish();
                    server.stop();
                    servidor=false;
                    iniciarButton.setText("Iniciar");
                }
                else{
                    Server=new TCPServer((String) comboBox2.getSelectedItem(),asdasdTextArea,clientCont);
                    server=new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Server.start();
                        }
                    });
                    server.start();
                    servidor=true;
                    iniciarButton.setText("Parar servidor");
                }
            }
        });
    }

    private void setClient() {
        DefaultComboBoxModel comb = new DefaultComboBoxModel();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            comb.addElement(info.toString());
        }
        comboBox1.setModel(comb);
        comboBox2.setModel(comb);
        lista1 = new DefaultListModel();
        list1.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                conectarAoServidorButton.setText("Conectar");
                conectarAoServidorButton.enable(true);
            }
        });
        list1.setModel(lista1);
        procuraServidores();
        conectarAoServidorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!Conectado) {
                    Client = new TCPClient((String) lista1.getElementAt(list1.getSelectedIndex())
                            , (String) comboBox1.getSelectedItem());
                    client = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Client.start();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    client.start();
                    conectarAoServidorButton.setText("Desconectar");
                    Conectado=true;
                }else{
                    Client.finish();
                    client.stop();
                    Conectado=false;
                    conectarAoServidorButton.setText("Conectar");
                }
            }
        });
        conectarAoServidorButton.enable(false);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String aux = textField1.getText().trim();
                if(aux.length()>0){
                    lista1.addElement(aux);
                    textField1.setText("");
                    list1.addNotify();
                }
            }
        });
        atualizarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                procuraServidores();
            }
        });
    }


    private void procuraServidores() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Find the server using UDP broadcast
                try {
                    //Open a random port to send the package
                    DatagramSocket c = new DatagramSocket();
                    c.setBroadcast(true);

                    byte[] sendData = "DISCOVER_FUIFSERVER_REQUEST".getBytes();

                    //Try the 255.255.255.255 first
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
                        c.send(sendPacket);
                        System.out.println(getClass().getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
                    } catch (Exception e) {
                    }

                    // Broadcast the message over all the network interfaces
                    Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                        if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                            continue; // Don't want to broadcast to the loopback interface
                        }

                        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                            InetAddress broadcast = interfaceAddress.getBroadcast();
                            if (broadcast == null) {
                                continue;
                            }

                            // Send the broadcast package!
                            try {
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                                c.send(sendPacket);
                            } catch (Exception e) {
                            }

                            System.out.println(getClass().getName() + ">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                        }
                    }

                    System.out.println(getClass().getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");
                    long agora = System.currentTimeMillis();
                    while(agora+60000>System.currentTimeMillis()) {
                        //Wait for a response
                        byte[] recvBuf = new byte[15000];
                        DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                        c.receive(receivePacket);

                        //We have a response
                        System.out.println(getClass().getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

                        //Check if the message is correct
                        String message = new String(receivePacket.getData()).trim();
                        if (message.equals("DISCOVER_FUIFSERVER_RESPONSE")) {
                            //DO SOMETHING WITH THE SERVER'S IP (for example, store it in your controller)
                            adicionaNalista(receivePacket.getAddress().toString().replaceAll("/",""));
                        }
                    }
                    //Close the port!
                    c.close();
                } catch (IOException ex) {
                    Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void adicionaNalista(String s) {
        boolean existe = false;
        for (int i = 0; i < lista1.size() && !existe; i++) {
            existe=lista1.get(i).equals(s);
        }
        if(!existe)lista1.addElement(s);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("RemoteSpeaker");
        frame.setContentPane(new Home().RemoteSpeaker);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}

