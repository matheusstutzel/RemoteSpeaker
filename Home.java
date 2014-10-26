import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    public  DefaultListModel lista1;
    Thread client;
    TCPClient Client;
    private boolean Conectado;

    public Home() {

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
                conectarAoServidorButton.setText("Conectar ao servidor: " + lista1.getElementAt(list1.getSelectedIndex()));
                conectarAoServidorButton.enable(true);
            }
        });
        list1.setModel(lista1);
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
}

