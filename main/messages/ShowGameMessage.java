package messages;

public class ShowGameMessage extends ProgramMessage{
	
	private int gameNumber;

	public ShowGameMessage(int gameNumber) {
		this.gameNumber = gameNumber;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}

}
