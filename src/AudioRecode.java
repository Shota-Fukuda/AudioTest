
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class AudioRecode extends JFrame{
	private JLabel lb;
    private JTextField tf;

    public static void main(String[] args)
    {
    	AudioRecode hl=new AudioRecode();
    }
    public AudioRecode()
    {
        super("録音再生ソフト");

        lb=new JLabel("録音時間を入力してください[秒]");
        tf=new JTextField();

        add(lb,BorderLayout.NORTH);
        add(tf,BorderLayout.SOUTH);

        tf.addActionListener(new SampleActionListener());

        this.setSize(400,100);
        this.setVisible(true);
    }
    class SampleActionListener implements ActionListener
    {

        //テキストフィールドでEnterを押されたさいの処理
        //1秒、入力された周波数の音を流す
        public void actionPerformed(ActionEvent e)
        {
            //テキストの値が正常か調べる
            JTextField tmp=(JTextField)e.getSource();
            int a;
            try{
                a=Integer.parseInt(tmp.getText());
            }catch(Exception er){
                System.out.println("error:"+er);
                return;
            }
            try{
                //録音する*************************************************************
                System.out.println("");
                //44100Hz,8bit,モノラル,値は正負型,リトルエンディアン
                AudioFormat linear=new AudioFormat(44100,8,1,true,false);
                DataLine.Info info=new DataLine.Info(TargetDataLine.class,linear);
                TargetDataLine target=(TargetDataLine)AudioSystem.getLine(info);
                target.open(linear);
                target.start();
                AudioInputStream stream=new AudioInputStream(target);

                //データの取得
                byte[] data=new byte[44100*8/8*1*a];
                stream.read(data,0,data.length);

                //終了
                target.stop();
                target.close();

                //再生する*************************************************************
                System.out.println("再生開始");
                AudioFormat frmt=new AudioFormat(44100,8,1,true,false);
                    //1秒あたりのサンプル数は44100個、
                    //音のビット数は8bit=256、
                    //チャネル数は1、
                    //値は正負型、
                    //bigEndianはfalse=リトルエンディアン
                DataLine.Info info2=new DataLine.Info(Clip.class,frmt);
                Clip clip=(Clip)AudioSystem.getLine(info2);
                clip.open(frmt,data,0,data.length);
                //再生を開始する
                clip.start();
                //再生が終わるまでループする
                while(clip.isRunning()){
                    Thread.sleep(100);
                }

                System.out.println("終了");
            }catch(Exception er){
                System.out.println("error:"+er);
            }
        }
    }
}