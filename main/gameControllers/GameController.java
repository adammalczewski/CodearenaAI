package gameControllers;

public interface GameController {

	void setParameter(String parameter,String value);
	
	void startGame(GameControllerListener listener,int gameNumber);
	
	void endGame();
	
}
