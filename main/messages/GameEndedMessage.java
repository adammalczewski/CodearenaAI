package messages;

import gameControllers.GameResult;

public class GameEndedMessage extends ProgramMessage {

	int gameNumber;
	GameResult gameResult;
	
	public GameEndedMessage(int gameNumber,GameResult gameResult) {
		this.gameResult = gameResult;
		this.gameNumber = gameNumber;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}
	
	public GameResult getGameResult(){
		return gameResult;
	}

}
