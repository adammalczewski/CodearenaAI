package gameControllers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import replays.OnlineReplayRecorder;
import structures.ActionRejection;
import structures.GameStatus;
import structures.ServerResponse;
import game.GameEngine;
import game.SocketActionListener;
import messages.RoundStartedMessage;
import network.ClientListener;
import network.XMLClient;
import xml.XMLHandler;
import game.GameListener;

//TODO jak nie dostaje wiadomosci od serwera przez jakis czas, to uznaje ze mnie rozlaczyl i probuje jeszcze raz

public class InternetGameController implements GameController, GameListener, ClientListener {

	XMLClient client;
	XMLHandler xmlHandler;
	
	boolean gameStarted;
	int gameNumber;
	GameEngine game;
	
	GameControllerListener listener;
	
	OnlineReplayRecorder onlineReplayRecorder;
	
	GameResult gameResult;
	
	Thread closingConnectionThread;
	
	Map<String,String> parameters;
	
	public InternetGameController() {
		
		gameStarted = false;
		
		xmlHandler = new XMLHandler(this);
		
		onlineReplayRecorder = new OnlineReplayRecorder();
		
		parameters = new HashMap<String,String>();
		
	}

	@Override
	public void setParameter(String parameter, String value) {

		parameters.put(parameter, value);

	}

	@Override
	public void startGame(GameControllerListener listener,int gameNumber) {
		
		this.gameNumber = gameNumber;
		
		this.listener = listener;
		
		if (gameStarted) System.out.println("Error : InternetGameController.startGame - wywolano przed ukonczeniem poprzedniej gry");
		
		gameResult = null;

		if (client == null) client = new XMLClient("codearena.pl",7654,xmlHandler,this);
		
		if (!client.isConnected()){
			client.tryConnecting();
			if (!client.isConnected()){
				listener.gameError(gameNumber,"Nie udalo sie polaczyc z serwerem");
				return;
			}
		}
		
		if (parameters.containsKey("map")){
			client.sendMessage("<connect userid=\"133\" hashid=\"0473eeb0e8cb292ba1c0527c1383c226\" map=\""+parameters.get("map")+"\"/>");
		} else client.sendMessage("<connect userid=\"133\" hashid=\"0473eeb0e8cb292ba1c0527c1383c226\"/>");

	}

	@Override
	public void endGame() {

		game = null;
		if (client != null){
			client.closeConnection();
		}
		if (onlineReplayRecorder != null){
			onlineReplayRecorder.stopRecording();
		}
		if (closingConnectionThread != null){
			closingConnectionThread.interrupt();
			closingConnectionThread = null;
		}

	}

	@Override
	public void getServerResponse(ServerResponse response) {
		if (closingConnectionThread != null){
			closingConnectionThread.interrupt();
			closingConnectionThread = null;
		}
		//if (logEnabled()) System.out.println("Otrzymano odpowiedź od serwera : ");
		switch (response.id){
		case BUSY:
			System.out.println(new Date()+" Serwer jest zajęty");
			listener.waitingForGame(gameNumber,"Czekamy na grę, serwer jest zajęty ...");
			break;
		case CHECK_WWW:
			System.out.println("Trzeba zmienić ustawienia profilu na stronie");
			client.closeConnection();
			listener.gameError(gameNumber,"Trzeba zmienic ustawienia profilu na stronie");
			break;
		case GAME_READY:
			
			System.out.println(new Date() + " Mozna rozpocząć grę : numer gry - "+response.game);
			gameStarted = true;
			onlineReplayRecorder.startRecording("game "+response.game);
			SocketActionListener actionListener = new SocketActionListener(client);
			actionListener.setReplayManager(onlineReplayRecorder);
			game = new GameEngine(actionListener);

			listener.gameStarted(gameNumber,game);
			
			break;
		case WAITING_FOR_PLAYER:
			System.out.println(new Date()+" Czekamy na graczy (id gry = "+response.game+")");
			listener.waitingForGame(gameNumber,"Czekamy na graczy ...");
			
			closingConnectionThread = new Thread(() ->{
				boolean interrupted = false;
				try {
					Thread.sleep(600000);
				} catch (Exception e) {
					System.out.println("closingConnectingThread - interrupted");
					interrupted = true;
				}
				if (!interrupted){
					System.out.println(new Date()+" Czekalismy 10 minut - konczymy polaczenie");
					if (client != null){
						client.closeConnection();
						client = null;
						closingConnectionThread = null;
						startGame(listener,gameNumber);
					}

				}
				
			});
			closingConnectionThread.start();
			
			break;
		case WRONG_CREDENTIALS:
			System.out.println(new Date()+" Niewlaściwe dane logowania");
			client.closeConnection();
			listener.gameError(gameNumber,"Niewlasciwe dane logowania");
			break;
		case GAME_CLOSED:
			onlineReplayRecorder.stopRecording();
			gameStarted = false;
			game.showStatistics();
			listener.gameEnded(gameNumber, gameResult);
			break;
		default:
			System.out.println(new Date()+" Warning : InternetGameListener.getGameStatus - Otrzymano nieznana odpowiedz od serwera");
			break;
		}
	}

	@Override
	public void getActionConfirmation() {
		if (gameStarted){
			onlineReplayRecorder.recordServerMessage(client.lastMessage);
			game.getActionConfirmation();
		} else System.out.println("Nie rozpoczęto gry, a dostano potwierdzenie akcji");
	}

	@Override
	public void getActionRejection(ActionRejection actionRejection) {
		if (gameStarted){
			onlineReplayRecorder.recordServerMessage(client.lastMessage);
			game.getActionRejection();
		} else System.out.println("Nie rozpoczęto gry, a dostano odrzucenie akcji");
	}

	@Override
	public void getGameStatus(GameStatus gameStatus) {
		//System.out.println("Rozpoczela się runda nr "+gameStatus.round);
		//System.out.println(" punkty - "+gameStatus.points);
		//System.out.println(" czas - "+gameStatus.timeElapsed);
		//System.out.println("Mamy do dyspozycji "+gameStatus.units.size()+" jednostek");
		onlineReplayRecorder.recordServerMessage(client.lastMessage);
		if (gameStarted)
			if (gameStatus.result.equalsIgnoreCase("win")){
				gameResult = GameResult.WIN;
				game.setGameResult(gameResult);
			} else if (gameStatus.result.equalsIgnoreCase("loss")){
				gameResult = GameResult.LOSS;
				game.setGameResult(gameResult);
			} else if (gameStatus.result.equalsIgnoreCase("tie")){
				gameResult = GameResult.TIE;
				game.setGameResult(gameResult);
			} else {
				//TODO jako parametr ustawić sleepa
				/*try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}*/
				
				listener.giveCustomMessage(new RoundStartedMessage(gameNumber,gameStatus.round));
				try {
					Thread.sleep(200);
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				game.getGameStatus(gameStatus);
			}

		else {
			//FIXME Jak naprawia błąd to wyrzucic ten kawałek
			if (gameStatus.result.equalsIgnoreCase("loss")){
				//Serwer zakonczyl gre, trzeba wznowic
				System.out.println(new Date()+" Serwer zakończył grę, trzeba wznowić");
				client.closeConnection();
				client = null;
				startGame(listener,gameNumber);
			} else {
				
				System.out.println(new Date() +" Nie rozpoczęto gry, a dostano status gry");
				
			}
		}
	}

	@Override
	public void connectionClosed() {

		gameStarted = false;
		listener.gameError(gameNumber, "Stracono połączenie z serwerem");
		if (closingConnectionThread != null){
			closingConnectionThread.interrupt();
			closingConnectionThread = null;
		}

	}

}
