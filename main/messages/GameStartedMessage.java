package messages;

import game.GameEngine;

public class GameStartedMessage extends ProgramMessage{

	private GameEngine game;
	private int gameNumber;
	
	public GameStartedMessage(GameEngine game,int gameNumber) {
		this.game = game;
		this.gameNumber = gameNumber;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}
	
	public GameEngine getGameEngine(){
		return game;
	}

}
