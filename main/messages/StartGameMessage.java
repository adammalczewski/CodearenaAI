package messages;

import game.GameType;

public class StartGameMessage extends ProgramMessage {
	
	GameType gameType;
	String map;

	public StartGameMessage(GameType gameType,String map) {
		this.gameType = gameType;
		this.map = map;
	}
	
	public GameType getGameType(){
		return gameType;
	}
	
	public String getMap(){
		return map;
	}

}
