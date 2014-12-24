import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Scanner;


/**
 * http://michieldemey.be/blog/network-discovery-using-udp-broadcast/
 * http://stackoverflow.com/questions/14542226/get-clients-on-the-network-discover-the-server
 * http://www.gemstone.com/docs/html/gemfire/6.0.0/SystemAdministratorsGuide/discovery_communication.5.5.html
 */
class TCPServer {
    private static JTextArea out;
    private final JLabel clientCont;
    String mix;
    /**Preferencias**/
    float sampleRate = 44100;
    int sampleSizeInBits = 16;
    int channels = 2;
    boolean signed = true;
    boolean bigEndian = false;
    int buffer_size = 1024;
    int porta = 6789;
    /****/
    static Socket serverSocket;
    private Thread discover;
    private DiscoveryThread discovery;
    private ServerSocket welcomeSocket;

    AudioFormat getAudioFormat() {
        return new AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian);
    }
    static SourceDataLine line;

    public void start(){
        try{
            welcomeSocket = new ServerSocket(porta);
            out("Servidor criado na porta "+porta);
            discovery=DiscoveryThread.getInstance();
            discover=new Thread(discovery);
            discover.start();
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            // checks if system supports the data line
            if (!AudioSystem.isLineSupported(info)) {
                out("Line not supported");
                System.exit(0);
            }
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

            try {
                int i;
                for (i = 0; i < mixerInfos.length; i++) {if(mixerInfos[i].toString().contains(mix))break;}
                line = (SourceDataLine) AudioSystem.getMixer(mixerInfos[i]).getLine(info);
            } catch (Exception e) {
                //System.err.println(e);
            }

            if (line == null) {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }
            out("Reproduzindo pela linha: " + line.getLineInfo());
            line.open(format);
            line.start();
            while (true) {
                serverSocket = welcomeSocket.accept();
                out("Cliente conectado: "+serverSocket.getRemoteSocketAddress());
                out("Enviando configurações");
                DataOutputStream toClient = new DataOutputStream(serverSocket.getOutputStream());
                toClient.writeFloat(sampleRate);
                toClient.writeInt(sampleSizeInBits);
                toClient.writeInt(channels);
                toClient.writeBoolean(signed);
                toClient.writeBoolean(bigEndian);
                toClient.writeInt(buffer_size);
                if(new DataInputStream(serverSocket.getInputStream()).readBoolean()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            out("Conexão bem sucedida");
                            maisClient();
                            byte buff[] = new byte[buffer_size];
                            String info = serverSocket.getRemoteSocketAddress() + "";
                            Socket connectionSocket = serverSocket;
                            while (true)
                                try {
                                    DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());

                                    inFromClient.readFully(buff);
                                    //int aux = inFromClient.read(buff);
                                    line.write(buff, 0, buff.length);
                                    Arrays.fill(buff, (byte) 0);
                                }catch (IOException e) {
                                    out("Cliente desconectado " + info);
                                    menosClient();

                                    e.printStackTrace();
                                    break;
                                }
                        }
                    }
                    ).start();
                }else{
                    out("Erro na conexao com o cliente: "+serverSocket.getRemoteSocketAddress());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void menosClient() {
        clientCont.setText((Integer.parseInt(clientCont.getText())-1)+"");
    }

    private void maisClient() {
        clientCont.setText((Integer.parseInt(clientCont.getText())+1)+"");
    }

    public TCPServer(String mix,JTextArea out,JLabel cont) {
        TCPServer.out=out;
        this.mix=mix;
        clientCont=cont;
    }

    public void finish(){
        line.stop();
        line.close();
        discovery.stop();
        try {
            discover.join();
            out("Terminou a thread discovery");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try{
            welcomeSocket.close();
            serverSocket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        out("Finished");
    }
    public static void out(String a){
        out.append(a+"\n");
        System.out.println(new SimpleDateFormat("HH:mm dd/MM/yyyy").format(System.currentTimeMillis()) + " - " + a);
    }
    private static void printHelp() {
        out("Digite:" +
                "\n0 - Saida padrão" +
                "\n1 - Para escolher a saida");
    }
}