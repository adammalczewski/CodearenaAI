package messages;

public class GameAboutToStartMessage extends ProgramMessage {
	
	int gameNumber;
	int secondsLeft;

	public GameAboutToStartMessage(int gameNumber,int secondsLeft) {
		this.gameNumber = gameNumber;
		this.secondsLeft = secondsLeft;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}
	
	public int getSecondsLeft(){
		return secondsLeft;
	}

}
