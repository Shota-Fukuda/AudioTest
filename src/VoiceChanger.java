import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class VoiceChanger {

    private static final int CHANNELS = 1;
    private static final int RATE = 16000;
    private static final int CHUNK_SIZE = 1024;

    public static void main(String[] args) {
    	System.out.println("ねむい");
        int recordSeconds = 3;
        String pitchFile = "temp.pitch";
        String mcepFile = "temp.mcep";
        String rawFile = "temp.raw";
        String outputRawFile = "output.raw";

        try {
            AudioFormat audioFormat = new AudioFormat(RATE, 16, CHANNELS, true, false);
            DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

            Line.Info[] targetLineInfos = AudioSystem.getTargetLineInfo(targetInfo);
            if (targetLineInfos.length == 0) {
                System.err.println("No target lines available.");
                return;
            }
            TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(targetLineInfos[0]);

            targetLine.open(audioFormat);
            targetLine.start();

            System.out.println("*** Now recording ... (" + recordSeconds + " sec)");
            record(targetLine, rawFile, recordSeconds);

            // Extract pitch
            extractPitch(rawFile, pitchFile);

            System.out.println("*** Extracting mel cepstrum ...");
            extractMcep(rawFile, mcepFile);

            System.out.println("*** Modifying parameters ...");

            // Choose one modification method at a time
            // modifyPitch(0.3, pitchFile, mcepFile, outputRawFile);
            // modifySpeed(300, pitchFile, mcepFile, outputRawFile);
            // hoarseVoice(pitchFile, mcepFile, outputRawFile);
            //robotVoice(100, recordSeconds, mcepFile, outputRawFile);
             childVoice(pitchFile, mcepFile, outputRawFile);
            // deepVoice(pitchFile, mcepFile, outputRawFile);

            System.out.println("*** Playing modified voice!");
            play(outputRawFile);

        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }





    private static void record(TargetDataLine targetLine, String outputFile, int recordSeconds) throws IOException {
        AudioFormat audioFormat = targetLine.getFormat();
        AudioInputStream audioStream = new AudioInputStream(targetLine);

        // 録音開始時間
        long startTime = System.currentTimeMillis();

        // データを一時的に保存するバッファ
        byte[] buffer = new byte[targetLine.getBufferSize() / 5];

        // 録音を指定の秒数だけ行う
        while ((System.currentTimeMillis() - startTime) < recordSeconds * 1000) {
            int bytesRead = targetLine.read(buffer, 0, buffer.length);
            audioStream.read(buffer, 0, bytesRead);
        }

        // 録音を停止する
        targetLine.stop();
        targetLine.close();

        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, new java.io.File(outputFile));
    }




    private static void extractPitch(String inputFile, String outputFile) throws IOException {
        String[] command = {"x2x", "+sf", inputFile, "|", "pitch", "-a", "1", "-s", "16", "-p", "80", ">", outputFile};
        executeCommand(command);
    }

    private static void extractMcep(String inputFile, String outputFile) throws IOException {
        String[] command = {"x2x", "+sf", inputFile, "|", "frame", "-p", "80", "|", "window", "|", "mcep", "-m", "25", "-a", "0.42", ">", outputFile};
        executeCommand(command);
    }
    
    private static byte[] getModifiedAudioData(String pitchFile, String mcepFile, String outputRawFile) throws IOException {
        String[] command = {
                "SMILExtract", 
                "-C", "config/opensmile.conf",
                "-I", mcepFile,
                "-O", outputRawFile
        };
        executeCommand(command);

        // 変換された音声データを読み込む
        byte[] modifiedData;
        try (InputStream inputStream = new FileInputStream(outputRawFile)) {
            modifiedData = inputStream.readAllBytes();
        }

        return modifiedData;
    }

    private static void modifyPitch(double m, String pitchFile, String mcepFile, String outputFile) throws IOException {
    	
    	 String[] command = {"sopr", "-m", Double.toString(m), pitchFile, "|", "excite", "-p", "80", "|", "mlsadf", "-m", "25", "-a", "0.42", "-p", "80", mcepFile, "|", "clip", "-y", "-32000", "32000", "|", "x2x", "+fs", ">", outputFile};
         executeCommand(command);
    	
        // 変換後の音声データを取得
    	byte[] modifiedData = getModifiedAudioData(pitchFile, mcepFile, outputFile);

        // 音声データをファイルに書き込む
        AudioFormat outputAudioFormat = new AudioFormat(RATE, 16, CHANNELS, true, false);
        AudioInputStream outputAudioStream = new AudioInputStream(new ByteArrayInputStream(modifiedData), outputAudioFormat, modifiedData.length);
        AudioSystem.write(outputAudioStream, AudioFileFormat.Type.WAVE, new java.io.File(outputFile));
    }


    private static void modifySpeed(double frameShift, String pitchFile, String mcepFile, String outputFile) throws IOException {
    	String[] command = {"excite", "-p", Double.toString(frameShift), pitchFile, "|", "mlsadf", "-m", "25", "-a", "0.42", "-p", Double.toString(frameShift), mcepFile, "|", "clip", "-y", "-32000", "32000", "|", "x2x", "+fs", ">", outputFile};
        executeCommand(command);

        // 変換後の音声データを取得
        byte[] modifiedData = getModifiedAudioData(pitchFile, mcepFile, outputFile);

        // 音声データをファイルに書き込む
        AudioFormat outputAudioFormat = new AudioFormat(RATE, 16, CHANNELS, true, false);
        AudioInputStream outputAudioStream = new AudioInputStream(new ByteArrayInputStream(modifiedData), outputAudioFormat, modifiedData.length);
        AudioSystem.write(outputAudioStream, AudioFileFormat.Type.WAVE, new java.io.File(outputFile));
    }


    private static void hoarseVoice(String pitchFile, String mcepFile, String outputFile) throws IOException {
    	String[] command = {"sopr", "-m", "0", pitchFile, "|", "excite", "-p", "80", "|", "mlsadf", "-m", "25", "-a", "0.42", "-p", "80", mcepFile, "|", "clip", "-y", "-32000", "32000", "|", "x2x", "+fs", ">", outputFile};
        executeCommand(command);

        // 変換後の音声データを取得
        byte[] modifiedData = getModifiedAudioData(pitchFile, mcepFile, outputFile);

        // 音声データをファイルに書き込む
        AudioFormat outputAudioFormat = new AudioFormat(RATE, 16, CHANNELS, true, false);
        AudioInputStream outputAudioStream = new AudioInputStream(new ByteArrayInputStream(modifiedData), outputAudioFormat, modifiedData.length);
        AudioSystem.write(outputAudioStream, AudioFileFormat.Type.WAVE, new java.io.File(outputFile));
    }

    private static void childVoice(String pitchFile, String mcepFile, String outputFile) throws IOException {
    	
    	String[] command = {"sopr", "-m", "0.4", pitchFile, "|", "excite", "-p", "80", "|", "mlsadf", "-m", "25", "-a", "0.1", "-p", "80", mcepFile, "|", "clip", "-y", "-32000", "32000", "|", "x2x", "+fs", ">", outputFile};
        executeCommand(command);
        
        // 変換後の音声データを取得
        byte[] modifiedData = getModifiedAudioData(pitchFile, mcepFile, outputFile);

        // 音声データをファイルに書き込む
        AudioFormat outputAudioFormat = new AudioFormat(RATE, 16, CHANNELS, true, false);
        AudioInputStream outputAudioStream = new AudioInputStream(new ByteArrayInputStream(modifiedData), outputAudioFormat, modifiedData.length);
        AudioSystem.write(outputAudioStream, AudioFileFormat.Type.WAVE, new java.io.File(outputFile));
    }


    private static void deepVoice(String pitchFile, String mcepFile, String outputFile) throws IOException {
    	
    	 String[] command = {"sopr", "-m", "2.0", pitchFile, "|", "excite", "-p", "80", "|", "mlsadf", "-m", "25", "-a", "0.6", "-p", "80", mcepFile, "|", "clip", "-y", "-32000", "32000", "|", "x2x", "+fs", ">", outputFile};
         executeCommand(command);

        // 変換後の音声データを取得
        byte[] modifiedData = getModifiedAudioData(pitchFile, mcepFile, outputFile);

        // 音声データをファイルに書き込む
        AudioFormat outputAudioFormat = new AudioFormat(RATE, 16, CHANNELS, true, false);
        AudioInputStream outputAudioStream = new AudioInputStream(new ByteArrayInputStream(modifiedData), outputAudioFormat, modifiedData.length);
        AudioSystem.write(outputAudioStream, AudioFileFormat.Type.WAVE, new java.io.File(outputFile));
    }


    private static void executeCommand(String[] command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        try (InputStream inputStream = process.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Do something with the output if needed
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void play(String inputFile) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new java.io.File(inputFile));
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
            Thread.sleep(clip.getMicrosecondLength() / 1000);
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        
        
    }
    
}

