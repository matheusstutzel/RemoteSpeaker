import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

public class JavaSoundRecorder {
    /**Preferencias**/
    float sampleRate = 44100;
    int sampleSizeInBits = 16;
    int channels = 2;
    boolean signed = true;
    boolean bigEndian = false;
    int buffer_size = 1024;
    int porta = 6789;
    /****/

    private final boolean escolha;
    boolean on;
    Socket clientSocket;
    String ip;

    public JavaSoundRecorder(String ip,boolean escolha){
        this.ip=ip;
        this.escolha=escolha;
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


            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            // checks if system supports the data line
            if (!AudioSystem.isLineSupported(info)) {
                out("Line not supported");
                System.exit(0);
            }
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            if(escolha) {
                Scanner s = new Scanner(System.in);
                for (int i = 0; i < mixerInfos.length; i++) {
                    try {
                        line = (TargetDataLine) AudioSystem.getMixer(mixerInfos[i]).getLine(info);
                        out("Opcao " + i + ": " + mixerInfos[i]);
                    } catch (Exception e) {
                        //System.err.println(e);
                    }
                }
                try {
                    int i = s.nextInt();
                    line = (TargetDataLine) AudioSystem.getMixer(mixerInfos[i]).getLine(info);
                    out("Opcao " + i + ": " + mixerInfos[i]);
                } catch (Exception e) {
                    //System.err.println(e);
                }

            }else{
                try {
                    int i;
                    for (i = 0; i < mixerInfos.length; i++) {if(mixerInfos[i].toString().contains("Stereo Mix"))break;}
                    line = (TargetDataLine) AudioSystem.getMixer(mixerInfos[i]).getLine(info);
                } catch (Exception e) {
                    //System.err.println(e);
                }
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
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
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

    /**
     * Entry to run the program
     */
    public static void main(String[] args) {
        if(args.length!=2 || args[0].equals("help")){
            printHelp();
            System.exit(0);
        }
        else if(args[1].equals("0")) new JavaSoundRecorder(args[0],false).start();
        else if(args[1].equals("1")) new JavaSoundRecorder(args[0],true).start();
        else{
            out("Opcao desconhecida");
            printHelp();
            System.exit(0);
        }


    }

    private static void printHelp() {
        out("Digite o ip do computador de destino, por exemplo 192.168.0.100" +
                "\nEm seguida digite 1 para escolher e 0 para selecionar automaticamente a placa de som");
    }
    public static void out(String a){
        System.out.println(new SimpleDateFormat("HH:mm dd/MM/yyyy").format(System.currentTimeMillis()) + " - " + a);
    }
}
