import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

/**
 * A sample program is to demonstrate how to record sound in Java
 * author: www.codejava.net
 */
public class TCPClient {
    /**Preferencias**/
    float sampleRate = 44100;
    int sampleSizeInBits = 16;
    int channels = 2;
    boolean signed = true;
    boolean bigEndian = false;
    int buffer_size = 1024;
    int porta = 6789;
    /****/
    boolean on;
    Socket clientSocket;
    String ip;
    String mix;

    public TCPClient(String ip,String mix){
        this.ip=ip;
        this.mix=mix;
    }
    // the line from which audio data is captured
    TargetDataLine line;

    /**
     * Defines an audio format
     */
    AudioFormat getAudioFormat() {
        return new AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian);
    }


    void start() {
        try {
            clientSocket = new Socket(ip, porta);

            out("Conectado ao servidor " + ip + ":"+porta);
            out("Recebendo configurações");
            DataInputStream fromServer = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            sampleRate= fromServer.readFloat();
            sampleSizeInBits = fromServer.readInt();
            channels = fromServer.readInt();
            signed = fromServer.readBoolean();
            bigEndian = fromServer.readBoolean();
            buffer_size = fromServer.readInt();

            out(sampleRate+"");
            out(sampleSizeInBits+"");
            out(channels+"");
            out(signed+"");
            out(bigEndian+"");
            out(buffer_size+"");

            out.writeBoolean(true);

            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            // checks if system supports the data line
            if (!AudioSystem.isLineSupported(info)) {
                out("Line not supported");
                System.exit(0);
            }
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

            try {
                int i;
                for (i = 0; i < mixerInfos.length; i++) {if(mixerInfos[i].toString().contains(mix))break;}
                line = (TargetDataLine) AudioSystem.getMixer(mixerInfos[i]).getLine(info);
            } catch (Exception e) {
                //System.err.println(e);
            }

            if (line == null) {
                line = (TargetDataLine) AudioSystem.getLine(info);
            }
            out("Capturando pela linha: " + line.getLineInfo());
            line.open(format);
            line.start();   // start capturing

            out("Start capturing...");



            byte buf[] = new byte[buffer_size];
            int bytesIn;
            on=true;
            while(on) {
                bytesIn = line.read(buf, 0, buf.length);
                out.write(buf, 0, bytesIn);
                //System.out.println("Fez :) "+bytesIn);
            }
        } catch (SocketException e){
            out("Erro de conexao com servidor, tente novamente");
            System.exit(-1);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Closes the target data line to finish capturing and recording
     */
    void finish() {
        line.stop();
        line.close();
        on=false;
        try{
            clientSocket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        out("Finished");
    }


    private static void printHelp() {
        out("Digite o ip do computador de destino, por exemplo 192.168.0.100" +
                "\nEm seguida digite 1 para escolher e 0 para selecionar automaticamente a placa de som");
    }
    public static void out(String a){
        System.out.println(new SimpleDateFormat("HH:mm dd/MM/yyyy").format(System.currentTimeMillis()) + " - " + a);
    }
}