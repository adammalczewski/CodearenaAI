package messages;

public class WaitingForGameMessage extends ProgramMessage{

	String message;
	int gameNumber;
	
	public WaitingForGameMessage(int gameNumber, String message) {
		this.message = message;
		this.gameNumber = gameNumber;
	}
	
	public String getMessage(){
		return message;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}

}
