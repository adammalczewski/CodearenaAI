package messages;

import gameControllers.GameSpeed;

public class ChangeGameSpeedMessage extends ProgramMessage {
	
	int gameNumber;
	GameSpeed speed;

	public ChangeGameSpeedMessage(int gameNumber,GameSpeed speed) {
		this.gameNumber = gameNumber;
		this.speed = speed;
	}
	
	public GameSpeed getSpeed(){
		return speed;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}

}
