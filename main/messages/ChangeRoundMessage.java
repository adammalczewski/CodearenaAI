package messages;

public class ChangeRoundMessage extends ProgramMessage {
	
	public enum RoundChange{
		NEXT_ROUND,PREVIOUS_ROUND,NEXT_UNIT,PREVIOUS_UNIT,EXACT_ROUND,THE_SAME;
	}
	
	private RoundChange roundChange;
	private int round;
	private int gameNumber;

	public ChangeRoundMessage(int gameNumber,RoundChange roundChange,int round) {
		this.round = round;
		this.roundChange = roundChange;
		this.gameNumber = gameNumber;
	}
	
	public ChangeRoundMessage(int gameNumber,RoundChange roundChange) {
		this.roundChange = roundChange;
		this.gameNumber = gameNumber;
	}
	
	public int getRound(){
		return round;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}
	
	public RoundChange getRoundChange(){
		return roundChange;
	}
	
	public void setRoundChange(RoundChange roundChange){
		this.roundChange = roundChange;
	}

}
