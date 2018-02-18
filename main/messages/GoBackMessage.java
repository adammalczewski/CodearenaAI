package messages;

public class GoBackMessage  extends ProgramMessage {
	
	private String windowName;

	public GoBackMessage(String windowName) {
		this.windowName = windowName;
	}
	
	public String getWindowName(){
		return windowName;
	}

}
