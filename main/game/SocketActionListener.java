package game;

import replays.OnlineReplayRecorder;
import structures.Action;
import xml.XMLCreator;
import network.XMLClient;

public class SocketActionListener implements ActionListener {
	
	private XMLClient client;
	private OnlineReplayRecorder replayRecorder;

	public SocketActionListener(XMLClient client) {
		this.client = client;
	}

	@Override
	public void getAction(Action action) {

		String actionString = XMLCreator.createActionMessage(action);
		if (replayRecorder != null) replayRecorder.recordAction(actionString);
		client.sendMessage(actionString);
		
	}
	
	public void setReplayManager(OnlineReplayRecorder replayManager){
		this.replayRecorder = replayManager;
	}
	
	

}
