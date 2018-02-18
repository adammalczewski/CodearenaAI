package messages;

public class GameErrorMessage extends ProgramMessage{

	String error;
	int gameNumber;
	
	public GameErrorMessage(int gameNumber, String error) {
		this.error = error;
		this.gameNumber = gameNumber;
	}
	
	public String getError(){
		return error;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}

}
