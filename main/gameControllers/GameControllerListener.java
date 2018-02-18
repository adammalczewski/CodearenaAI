package gameControllers;

import messages.ProgramMessage;
import game.GameEngine;

public interface GameControllerListener {

	void gameStarted(int gameNumber, GameEngine game);
	
	void gameEnded(int gameNumber, GameResult gameResult);
	
	void giveCustomMessage(ProgramMessage message);
	
	void waitingForGame(int gameNumber, String message);
	
	//Przy wywolaniu tej funkcji, gra albo się zakończyła, albo w ogóle nie mogła zacząć
	void gameError(int gameNumber, String message);
	
}
