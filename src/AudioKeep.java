
/* sample 4.2.2 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;


public class AudioKeep extends Thread {
	private TargetDataLine m_line;
	private AudioFileFormat.Type m_targetType;
	private AudioInputStream m_audioInputStream;
	private File m_outputFile;
	private boolean m_bRecording;

	public AudioKeep(TargetDataLine line, AudioFileFormat.Type targetType, File file) {
		m_line = line;
		m_audioInputStream = new AudioInputStream(line);
		m_targetType = targetType;
		m_outputFile = file;
	}

	public void startRecording() {
		m_line.start();
		super.start();
		m_bRecording = true;
	}

	public void stopRecording() {
		m_line.stop();
		m_line.close();
		m_bRecording = false;
	}

	public void run() {
		try {
			AudioSystem.write(m_audioInputStream, m_targetType, m_outputFile);
			System.out.println("録音が終了しました");
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public static void main(String[] args) {
		try {
			File outputFile = new File("./RecordAudio.wav");

			AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, // サンプリング周波数
					16,
					2,
					4, 44100.0F, false);

			DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
			TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);

			targetDataLine.open(audioFormat);
			AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;

			AudioKeep recorder = new AudioKeep(targetDataLine, targetType, outputFile);
			System.out.println("ENTERキーを押すと録音を開始します");
			byte[] b = new byte[2];
			System.in.read(b);

			recorder.startRecording();
			System.out.println("録音中...");
			System.out.println("ENTERキーを押すと録音を停止します");
			System.in.read(b);
			recorder.stopRecording();
			System.out.println("Record Stop");
			int StatCode = Send();
			System.out.println(StatCode);
			System.exit(0);
		} catch (Exception e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	private static final String EOL = "\r\n";

	public static int Send() throws IOException {
		String filename = "./RecordAudio.wav";
		String url = "サーバーのエンドポイント";
		try (FileInputStream file = new FileInputStream(filename)) {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			final String boundary = UUID.randomUUID().toString();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			try (OutputStream out = con.getOutputStream()) {
				out.write(("--" + boundary + EOL + "Content-Disposition: form-data; name=\"fname\"; " + "filename=\""
						+ filename + "\"" + EOL + "Content-Type: audio/mpeg" + EOL + EOL)
								.getBytes(StandardCharsets.UTF_8));
				byte[] buffer = new byte[128];
				int size = -1;
				while (-1 != (size = file.read(buffer))) {
					out.write(buffer, 0, size);
				}
				out.write((EOL + "--" + boundary + EOL + "Content-Disposition: form-data; name=\"Body[Title]\"" + EOL
						+ EOL + "Java投稿テスト音源" + EOL + "--" + boundary + EOL
						+ "Content-Disposition: form-data; name=\"Body[TimeStamp]\"" + EOL + EOL + "2021-08-30" + EOL
						+ "--" + boundary + EOL + "Content-Disposition: form-data; name=\"Body[EventID]\"" + EOL + EOL
						+ "2" + EOL + "--" + boundary + EOL + "Content-Disposition: form-data; name=\"Auth[Token]\""
						+ EOL + EOL + "(Token)" + EOL + "--" + boundary + EOL
						+ "Content-Disposition: form-data; name=\"Auth[UserID]\"" + EOL + EOL + "(UesrID)" + EOL + "--"
						+ boundary + EOL + "Content-Disposition: form-data; name=\"Method\"" + EOL + EOL + "addNewAudio"
						+ EOL + "--" + boundary + EOL + "Content-Disposition: form-data; name=\"Body[MusicID]\"" + EOL
						+ EOL + "0" + EOL).getBytes(StandardCharsets.UTF_8));
				out.write(("--" + boundary + "--" + EOL).getBytes(StandardCharsets.UTF_8));
				out.flush();
				System.err.println(con.getResponseMessage());
				InputStream is = con.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String s;
				while ((s = reader.readLine()) != null) {
					System.out.println(s);
				}
				return con.getResponseCode();
			} finally {

				con.disconnect();
			}
		}
	}

}