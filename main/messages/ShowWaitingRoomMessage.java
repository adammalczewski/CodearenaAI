package messages;

import game.GameType;

public class ShowWaitingRoomMessage extends ProgramMessage{

	GameType gameType;
	
	public ShowWaitingRoomMessage(GameType gameType) {
		this.gameType = gameType;
	}
	
	public GameType getGameType(){
		return gameType;
	}

}
