import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Scanner;

class TCPServer {
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
    AudioFormat getAudioFormat() {
        return new AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian);
    }
    static SourceDataLine line;

    public TCPServer(boolean escolha) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(porta);
        out("Servidor criado na porta "+porta);

        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        // checks if system supports the data line
        if (!AudioSystem.isLineSupported(info)) {
            out("Line not supported");
            System.exit(0);
        }
        if(escolha) {
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            Scanner s = new Scanner(System.in);
            for (int i = 0; i < mixerInfos.length; i++) {
                try {
                    line = (SourceDataLine) AudioSystem.getMixer(mixerInfos[i]).getLine(info);
                    out("Opcao " + i + ": " + mixerInfos[i]);
                } catch (Exception e) {
                    //System.err.println(e);
                }
            }
            try {
                int i = s.nextInt();
                line = (SourceDataLine) AudioSystem.getMixer(mixerInfos[i]).getLine(info);
                out("Opcao " + i + ": " + mixerInfos[i]);
            } catch (Exception e) {
                //System.err.println(e);
            }
            if (line == null) {
                out("Não deu certo");
                line = (SourceDataLine) AudioSystem.getLine(info);
            }
        }else {
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
                            } catch (SocketException e) {
                                out("Cliente desconectado " + info);
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                }
                ).start();
            }else{
                out("Erro na conexao com o cliente: "+serverSocket.getRemoteSocketAddress());
            }
        }
    }
    public static void main(String args[]) throws Exception {
        if(args.length!=1 || args[0].equals("help"))printHelp();
        else if(args[0].equals("0"))new TCPServer(false);
        else if(args[0].equals("1"))new TCPServer(true);
        else{
            out("Opcao desconhecida");
            printHelp();
        }
    }

    public static void out(String a){

        System.out.println(new SimpleDateFormat("HH:mm dd/MM/yyyy").format(System.currentTimeMillis())+" - "+a);
    }
    private static void printHelp() {
        out("Digite:" +
                "\n0 - Saida padrão" +
                "\n1 - Para escolher a saida");
    }
}