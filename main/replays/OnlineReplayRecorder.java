package replays;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class OnlineReplayRecorder {
	
	boolean recording = false;
	OutputStreamWriter writer;

	public OnlineReplayRecorder() {
		
	}
	
	public void startRecording(String fileName){
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fileName+".nrl");
			writer  = new OutputStreamWriter(fos, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
		
		recording = true;
	}
	
	public void recordAction(String action){
		if (recording){
			try {
				writer.write(action+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void recordServerMessage(String gameStatus){
		if (recording){
			try {
				writer.write(gameStatus+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stopRecording(){
		recording = false;
		if (writer != null){
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
