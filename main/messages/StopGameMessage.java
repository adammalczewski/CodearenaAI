package messages;

public class StopGameMessage extends ProgramMessage{
	
	private int gameNumber;

	public StopGameMessage(int gameNumber) {
		this.gameNumber = gameNumber;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}

}
