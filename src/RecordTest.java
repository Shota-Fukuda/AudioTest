import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
public class RecordTest implements Runnable {
  static final long RECORD_TIME = 100;  // 0.1 sec
  File wavFile;
  AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
  static final float SAMPLE_RATE = 44100;
  static final int SAMPLE_SIZE_IN_BITS = 16;
  static final int CHANNELS = 2;
  static final boolean SIGNED = true;
  static final boolean BIG_ENDIAN = true;
  TargetDataLine line;
  RecordTest(File file) throws Exception {
    AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
    wavFile = file;
    line = AudioSystem.getTargetDataLine(format);
    line.open(format);
  }
  void startRecording() {
    try {
      line.start();
      AudioInputStream ais = new AudioInputStream(line);
      AudioSystem.write(ais, fileType, wavFile);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
  void stopRecording() {
    line.stop();
    line.close();
  }
  public static void main(String[] args) throws Exception {
    final RecordTest recorder = new RecordTest(new File("C:/pleiades/2023-03/workspace/AudioTest/AudioTset/src/midi/RecordAudio.wav"));
    Thread stopper = new Thread(recorder);
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    System.out.print("enter to start recording >>");
    in.readLine();
    System.out.println("recording...");
    stopper.start();
    System.out.print("enter to stop recording >>");
    in.readLine();
    recorder.stopRecording();
    System.out.println("finished");
  }
  @Override
  public void run() {
    startRecording();
  }
  
}
