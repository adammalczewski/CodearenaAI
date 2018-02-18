package gameControllers;

import game.ActionListener;
import game.GameEngine;
import game.GameListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import messages.ChangeRoundMessage;
import messages.ChangeRoundMessage.RoundChange;
import messages.RoundStartedMessage;
import replays.OnlineReplayPlayer;
import structures.Action;
import structures.ActionRejection;
import structures.GameStatus;
import structures.ServerResponse;
import xml.XMLCreator;
import xml.XMLHandler;


public class OnlineReplayController implements GameController{
	
	boolean gameStarted;
	int gameNumber;
	
	GameControllerListener listener;
	
	GameResult gameResult;
	
	HandlerThread handler;
	Thread handlerThread;
	
	Map<String,String> parameters;
	
	private class HandlerThread implements Runnable,GameListener,ActionListener {
		
		final static int FASTEST_SLEEP = 10;
		final static int FASTER_SLEEP = 50;
		final static int FAST_SLEEP = 110;
		final static int NORMAL_SLEEP = 400;
		final static int SLOW_SLEEP = 1200;
		final static int VERY_SLOW_SLEEP = 4000;
		
		XMLHandler xmlHandler;
		
		OnlineReplayPlayer player;
		
		String fileName;
		
		int roundNumber;
		
		GameSpeed speed;
		BlockingQueue<ChangeRoundMessage> actions;
		
		Action gameAction;
		
		boolean gameStarted = false;
		
		GameEngine gameEngine;
		
		GameResult gameResult;
		
		boolean interrupted = false;
		
		public HandlerThread(String fileName,GameSpeed speed){
			
			xmlHandler = new XMLHandler(this);
			
			player = new OnlineReplayPlayer(this,xmlHandler);
			
			this.fileName = fileName;
			
			actions = new LinkedBlockingQueue<ChangeRoundMessage>();
			
			roundNumber = 0;
			
			this.speed = speed;
			
		}
		
		//zwraca false jak nastapilo przerwanie
		public boolean sleep(int miliseconds){
			try {
				Thread.sleep(miliseconds);
			} catch (InterruptedException e) {
				return false;
			}
			return true;
		}
		


		@Override
		public void run() {
			
			gameEngine = new GameEngine(this);
			
			gameStarted = true;
			
			listener.gameStarted(gameNumber, gameEngine);

			player.start(fileName);
			
			while (!player.getEndOfStream() && gameResult == null){
				

			
				if (gameAction == null){
					System.out.println("Nie przyznano akcji :"+XMLCreator.createActionMessage(gameAction));
					listener.gameEnded(gameNumber, GameResult.LOSS);
					return;
				}
				
				//TODO tutaj w zaleznosci od szybkosci gry robimy cos
				switch (speed){
				case FAST:
					if (!sleep(FAST_SLEEP)) interrupted = true;
					break;
				case FASTER:
					if (!sleep(FASTER_SLEEP)) interrupted = true;
					break;
				case FASTEST:
					if (!sleep(FASTEST_SLEEP)) interrupted = true;
					break;
				case NORMAL:
					if (!sleep(NORMAL_SLEEP)) interrupted = true;
					break;
				case REAL_TIME:
					break;
				case SLOW:
					if (!sleep(SLOW_SLEEP)) interrupted = true;
					break;
				case STEPS:
					while (actions.size() == 0 && !interrupted){
						if (!sleep(50)) interrupted = true;
					}
					if (!interrupted){
						ChangeRoundMessage msg = actions.remove();
						switch (msg.getRoundChange()){
						case EXACT_ROUND:
							break;
						case NEXT_ROUND:
							break;
						case NEXT_UNIT:
							//po prostu przechodzimy dalej
							break;
						case PREVIOUS_ROUND:
							break;
						case PREVIOUS_UNIT:
							break;
						default:
							System.out.println("Nieznany typ ChangeRoundMessage");
						}
					}
					break;
				case VERYSLOW:
					if (!sleep(VERY_SLOW_SLEEP)) interrupted = true;
					break;
				default:
					break;
				
				}
				
				if (interrupted) break;
				
				player.getAction(gameAction,gameEngine);
				
			}
			
			if (gameResult != null && !interrupted){
				System.out.println("Gra sie skonczyla, wynik = "+gameResult);
				listener.gameEnded(gameNumber, gameResult);
			} else if (!interrupted){
				System.out.println("Gra sie skonczyla, wynik = UNKNOWN");
				gameEngine.setGameResult(GameResult.UNKNOWN);
				listener.gameEnded(gameNumber, GameResult.UNKNOWN);
			}
			
			//TODO w zaleznosci od gameResult
			//TODO jak jest gameResult w funkcji ponizej przyjete to od razu powinien dawac ten komunikat
			//listener.gameEnded(gameNumber, GameResult.LOSS);
			
		}

		@Override
		public void getServerResponse(ServerResponse response) {
			
			
		}

		@Override
		public void getActionConfirmation() {
			
			if (gameStarted)
				gameEngine.getActionConfirmation();
			else System.out.println("Nie rozpoczęto gry, a dostano potwierdzenie akcji");
			
		}

		@Override
		public void getActionRejection(ActionRejection actionRejection) {
			
			if (gameStarted){
				gameEngine.getActionRejection();
			} else System.out.println("Nie rozpoczęto gry, a dostano odrzucenie akcji");
			
		}

		@Override
		public void getGameStatus(GameStatus gameStatus) {
			
			if (!gameStarted){
				System.out.println("dostano status gry a jescze nie zaczeto");
				
				//System.exit(-1);
			} else {
				if (gameStatus.result.equalsIgnoreCase("win")){
					gameResult = GameResult.WIN;
					gameEngine.setGameResult(gameResult);
				} else if (gameStatus.result.equalsIgnoreCase("loss")){
					gameResult = GameResult.LOSS;
					gameEngine.setGameResult(gameResult);
				} else if (gameStatus.result.equalsIgnoreCase("tie")){
					gameResult = GameResult.TIE;
					gameEngine.setGameResult(gameResult);
				} else {
					roundNumber = gameStatus.round;
					listener.giveCustomMessage(new RoundStartedMessage(gameNumber,roundNumber));
					gameEngine.getGameStatus(gameStatus);
				}
			}
			
		}
		
		public BlockingQueue<ChangeRoundMessage> getActions(){
			return actions;
		}

		@Override
		public void getAction(Action action) {

			gameAction = action;
			
		}

		public void setGameSpeed(GameSpeed speed) {

			this.speed = speed;
			actions.add(new ChangeRoundMessage(gameNumber,RoundChange.THE_SAME));
			
		}
		
		
	}
	
	public OnlineReplayController() {
		
		gameStarted = false;
		
		parameters = new HashMap<String,String>();
		
	}

	@Override
	public void setParameter(String parameter, String value) {
		
		System.out.println("Otrzymano parametr : "+parameter);
		
		parameters.put(parameter, value);

		if (parameter.equalsIgnoreCase("Game Speed")){
			GameSpeed speed = GameSpeed.valueOf(value);
			if (handler != null) handler.setGameSpeed(speed);
		}

	}

	@Override
	public void startGame(GameControllerListener listener,int gameNumber) {
		
		this.gameNumber = gameNumber;
		
		this.listener = listener;
		
		if (gameStarted) System.out.println("Error : InternetGameController.startGame - wywolano przed ukonczeniem poprzedniej gry");
		
		gameResult = null;
		
		GameSpeed speed = null;
		
		if (parameters.containsKey("Game Speed")){
			speed = GameSpeed.valueOf(parameters.get("Game Speed"));
		}
		
		if (parameters.containsKey("file")){
			handler = new HandlerThread(parameters.get("file"),(speed != null)?speed:GameSpeed.STEPS);
			handlerThread = new Thread(handler);
			handlerThread.start();
		} else {
			System.out.println("OnlineReplayController : Nie podano pliku z replayem");
			listener.gameError(gameNumber, "Nie podano pliku z replayem");
		}
		


	}
	
	public void sendChangeRoundMessage(ChangeRoundMessage msg){
		handler.getActions().add(msg);
	}

	@Override
	public void endGame() {

		if (handlerThread != null){
			handlerThread.interrupt();
		}

	}

}
