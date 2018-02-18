package messages;

public class RoundStartedMessage extends ProgramMessage{
	
	int gameNumber;
	int roundNumber;

	public RoundStartedMessage(int gameNumber,int roundNumber) {
		this.gameNumber = gameNumber;
		this.roundNumber = roundNumber;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}
	
	public int getRoundNumber(){
		return roundNumber;
	}

}
