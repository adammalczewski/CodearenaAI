package game;

import gameControllers.GameController;
import gameControllers.GameControllerListener;
import gameControllers.GameSpeed;

public class Game {
	
	GameEngine gameEngine;
	GameController gameController;
	
	GameCountdownListener countdownListener;
	GameControllerListener controllerListener;
	
	GameSpeed speed;
	
	GameType gameType;
	int gameNumber;
	
	int wins,losses,ties;
	
	String mainDesc;
	String secondaryDesc;
	
	boolean ended = false;

	public Game(GameType gameType,GameCountdownListener countdownListener
			,GameControllerListener controllerListener)
	{
		this.gameType = gameType;
		this.countdownListener = countdownListener;
		this.controllerListener = controllerListener;
		mainDesc = "";
		secondaryDesc = "";
		//TODO przeniesc to gdzie indziej, do jakichs stalych programu czy cos
		if (gameType == GameType.REPLAY) speed = GameSpeed.STEPS; 
		else speed = GameSpeed.NORMAL;
	}
	
	public void setGameSpeed(GameSpeed speed){
		this.speed = speed;
	}
	
	public GameSpeed getGameSpeed(){
		return speed;
	}
	
	public GameEngine getGameEngine(){
		return gameEngine;
	}
	
	public void setGameEngine(GameEngine gameEngine){
		this.gameEngine = gameEngine;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}
	
	public void setMainDesc(String mainDesc){
		this.mainDesc = mainDesc;
	}
	
	public void setSecondaryDesc(String secondaryDesc){
		this.secondaryDesc = secondaryDesc;
	}
	
	public void setWins(int wins){
		this.wins = wins;
	}
	
	public void setLosses(int losses){
		this.losses = losses;
	}
	
	public void setTies(int ties){
		this.ties = ties;
	}
	
	public int getWins(){
		return wins;
	}
	
	public int getLosses(){
		return losses;
	}
	
	public int getTies(){
		return ties;
	}

	public String getMainDesc(){
		return mainDesc;
	}
	
	public String getSecondaryDesc(){
		return secondaryDesc;
	}
	
	public GameType getGameType(){
		return gameType;
	}
	
	public synchronized void endGame(){
		ended = true;
		if (gameController != null){
			gameController.endGame();
			gameController = null;
			gameEngine = null;
		}
	}
	
	private void startGameIfNotEnded(int gameNumber){
		if (!ended){
			gameController.startGame(controllerListener,gameNumber);
		}
	}
	
	public void startGame(int gameNumber,final int secondsToWait){
		startGame(gameNumber,secondsToWait,gameController);
	}
	
	public void startGame(int gameNumber,final int secondsToWait,GameController gameController){
		
		this.gameController = gameController;
		this.gameNumber = gameNumber;
		
		//Tworzymy nowy wątek żeby nie zatrzymywać głównego wątku programu
		new Thread(() ->{
			
			int secondsLeft = secondsToWait;
		
			while (secondsLeft > 0){
				//Nie trzeba synchronizowac, najwyzej za sekunde zobaczy ze sie skonczyla
				if (ended) break;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				countdownListener.getTimeToGameStart(gameNumber, --secondsLeft);
			}
			
			startGameIfNotEnded(gameNumber);
		
		}).start();
		
	}
	
	public GameController getGameController(){
		return gameController;
	}

}
