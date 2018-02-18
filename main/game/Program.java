package game;

import logs.Logger;
import logs.Logging;
import messages.*;
import gameControllers.GameControllerListener;
import gameControllers.GameResult;
import gameControllers.InternetGameController;
import gameControllers.OnlineReplayController;
import gui.GUI;
import gui.GameWindow;
import gui.LogsWindow;
import gui.MainMenuWindow;
import gui.GUIListener;
import gui.WaitingRoomWindow;

import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFrame;

import messages.ProgramMessage;

import org.newdawn.slick.CanvasGameContainer;
import org.newdawn.slick.SlickException;

import structures.Unit;

//TODO nad przyciskami trybów w menu głównym powinny się pokazywać małe liczby pokazujące ile gier
//TODO jest uruchomionych
//TODO wyswietlac toasty gdy zacznie sie nowa gra multiplayer

public class Program implements GameControllerListener,GUIListener,GameCountdownListener {
	
	final static int GAME_COUNTDOWN = 3;
	final static boolean GAME_AFTER_GAME = true;
	
	Logger logger = Logging.getLogger(Program.class.getName());
	
	/* OPCJE */
	
	static boolean CREATE_WINDOW;
	
	enum DisplayType{
		FULLSCREEN,WINDOW,FULLSCREENPOPUP;
	}
	
	static DisplayType displayType;
	
	/* INTERFEJS GRAFICZNY */
	
	JFrame frame;
	
	GUI gui;
	
	GameWindow gameWindow;
	WaitingRoomWindow waitingRoomWindow;
	MainMenuWindow mainMenuWindow;
	LogsWindow logsWindow;
	
	/* ZMIENNE ZWIAZANE Z GRAMI */
	
	int wins = 0;
	int losses = 0;
	int ties = 0;
	
	ArrayList<Game> games;
	
	int currentGameNumber;
	
	BlockingQueue<ProgramMessage> messages;
	
	/* ... */
	
	public static void main(String [] args){
		
		Program.CREATE_WINDOW = true;
		Program.displayType = DisplayType.FULLSCREENPOPUP;
		
		Program program = new Program();
		
		program.start();
		
	}
	
	public Program(){
		
		gui = new GUI(this,"Codearena");
		
		messages = new LinkedBlockingQueue<ProgramMessage>();
		
		games = new ArrayList<Game>();
		
		currentGameNumber = 0;
		
		logger.setCategory("program messages");
	
	}

	public void start(){
		
		new Thread(() ->{
			processMessages();
		}).start();
		
		if (CREATE_WINDOW){
			
			logger.info("Tworzymy okno programu.");
			
			gameWindow = new GameWindow(this,"game");
			mainMenuWindow = new MainMenuWindow(this,"waitingRoom");
			waitingRoomWindow = new WaitingRoomWindow(this,"mainMenu");
			logsWindow = new LogsWindow(this,"logs");
				
			gui.addWindow("game", gameWindow);
			gui.addWindow("waitingRoom",waitingRoomWindow);
			gui.addWindow("mainMenu",mainMenuWindow);
			gui.addWindow("logs",logsWindow);
			
			gui.switchTo("mainMenu");
		
			createWindow();
			
		}
		
	}
	
	public boolean processMessage(ProgramMessage msg){
		if (msg instanceof ChangeRoundMessage){
			ChangeRoundMessage message = (ChangeRoundMessage)msg;
			Game game = null;
			for (int i = 0;i < games.size() && game == null;++i){
				if (games.get(i).getGameNumber() == message.getGameNumber()){
					game = games.get(i);
				}
			}
			if (game != null && game.getGameType() == GameType.REPLAY){
				((OnlineReplayController)game.getGameController()).sendChangeRoundMessage(message);
			}
		} else if (msg instanceof ExitMessage){
			return false;
		} else if (msg instanceof GameAboutToStartMessage){
			GameAboutToStartMessage message = (GameAboutToStartMessage)msg;
			Game game = null;
			for (int i = 0;i < games.size() && game == null;++i){
				if (games.get(i).getGameNumber() == message.getGameNumber()){
					game = games.get(i);
				}
			}
			if (game != null){
				game.setMainDesc("Game starting in "+message.getSecondsLeft()+" seconds");
				if (gui.getCurrentWindow().equals("game") && gameWindow.getGameNumber() == message.getGameNumber()){
					gameWindow.setSecondsToStart(message.getSecondsLeft());
				}
			}
		} else if (msg instanceof GameEndedMessage){
			GameEndedMessage message = (GameEndedMessage)msg;
			Game game = null;
			for (int i = 0;i < games.size() && game == null;++i){
				if (games.get(i).getGameNumber() == message.getGameNumber()){
					switch (games.get(i).getGameType()){
					case CAMPAIGN:
						switch (message.getGameResult()){
						case LOSS:
							games.get(i).setLosses(games.get(i).getLosses()+1);
							break;
						case TIE:
							games.get(i).setTies(games.get(i).getTies()+1);
							break;
						case WIN:
							games.get(i).setWins(games.get(i).getWins()+1);
							break;
						default:
							break;
						
						}
						games.get(i).setSecondaryDesc("Wins : "+games.get(i).getWins()+"  Losses : "
								+games.get(i).getLosses()+" Ties : "+games.get(i).getTies());
						games.get(i).startGame(currentGameNumber++,GAME_COUNTDOWN);
						games.get(i).setMainDesc("Game starting in "+GAME_COUNTDOWN+" seconds");
						break;
					case MULTIPLAYER:
						switch (message.getGameResult()){
						case LOSS:
							games.get(i).setLosses(games.get(i).getLosses()+1);
							break;
						case TIE:
							games.get(i).setTies(games.get(i).getTies()+1);
							break;
						case WIN:
							games.get(i).setWins(games.get(i).getWins()+1);
							break;
						default:
							break;
						
						}
						if (games.get(i).getLosses() < 3){
							games.get(i).setSecondaryDesc("Wins : "+games.get(i).getWins()+"  Losses : "
								+games.get(i).getLosses()+" Ties : "+games.get(i).getTies());
							games.get(i).startGame(currentGameNumber++,GAME_COUNTDOWN);
							games.get(i).setMainDesc("Game starting in "+GAME_COUNTDOWN+" seconds");
						}
						break;
					case REPLAY:
						game = games.get(i);
						break;
					case SINGLE_PLAYER:
						break;
					default:
						break;
					}
					
				}
			}
			
			if (game != null && game.getGameType() == GameType.REPLAY){
				games.remove(game);
				if (gui.getCurrentWindow().equals("waitingRoom") && waitingRoomWindow.getGameType()
						== GameType.REPLAY){
					waitingRoomWindow.removeGame(game);
				}
			}
		} else if (msg instanceof GameErrorMessage){
			
		} else if (msg instanceof GameStartedMessage){
			GameStartedMessage message = (GameStartedMessage)msg;
			for (int i = 0;i < games.size();++i){
				if (games.get(i).getGameNumber() == message.getGameNumber()){
					games.get(i).setGameEngine(message.getGameEngine());
					games.get(i).setMainDesc("Game started");
				}
			}
			if (gui.getCurrentWindow().equals("game") && gameWindow.getGameNumber() == message.getGameNumber()){
				gameWindow.setGameEngine(message.getGameEngine());
			}
		} else if (msg instanceof GoBackMessage){
			GoBackMessage message = (GoBackMessage)msg;
			if (message.getWindowName().equalsIgnoreCase("waitingRoom")
					|| message.getWindowName().equalsIgnoreCase("logs")
					|| message.getWindowName().equalsIgnoreCase("options")){
				gui.switchTo("mainMenu");
			} else if (message.getWindowName().equalsIgnoreCase("game")){
				waitingRoomWindow.clearGames();
				for (int i = 0;i < games.size();++i){
					if (games.get(i).getGameType() == waitingRoomWindow.getGameType()){
						waitingRoomWindow.addGame(games.get(i));
					}
				}
				gui.switchTo("waitingRoom");
			}
		} else if (msg instanceof ShowGameMessage){
			ShowGameMessage message = (ShowGameMessage)msg;
			Game game = null;
			for (int i = 0;i < games.size() && game == null;++i){
				if (games.get(i).getGameNumber() == message.getGameNumber()){
					game = games.get(i);
				}
			}
			if (game != null){
				gameWindow.setGameEngine(game.getGameEngine());
				gameWindow.setGameNumber(game.getGameNumber());
				gameWindow.setSecondsToStart(null);
				gui.switchTo("game");
			}
		} else if (msg instanceof ChangeGameSpeedMessage){
			ChangeGameSpeedMessage message = (ChangeGameSpeedMessage)msg;
			Game game = null;
			for (int i = 0;i < games.size() && game == null;++i){
				if (games.get(i).getGameNumber() == message.getGameNumber()){
					game = games.get(i);
				}
			}
			logger.info("Otrzymano komunikat ChangeGameSpeed");
			if (game != null){
				if (game.getGameType() == GameType.REPLAY){
					logger.fine("To jest replay");
					game.setGameSpeed(message.getSpeed());
					game.getGameController().setParameter("Game Speed", message.getSpeed().toString());
				}
			}
		} else if (msg instanceof ShowOptionsMessage){
			
		} else if (msg instanceof ShowWaitingRoomMessage){
			ShowWaitingRoomMessage message = (ShowWaitingRoomMessage) msg;
			switch (message.getGameType()){
			case CAMPAIGN:
				waitingRoomWindow.setTitle("Campaign");
				waitingRoomWindow.setGameType(GameType.CAMPAIGN);
				break;
			case MULTIPLAYER:
				waitingRoomWindow.setTitle("Multiplayer");
				waitingRoomWindow.setGameType(GameType.MULTIPLAYER);
				break;
			case REPLAY:
				waitingRoomWindow.setTitle("Replays");
				waitingRoomWindow.setGameType(GameType.REPLAY);
				break;
			case SINGLE_PLAYER:
				waitingRoomWindow.setTitle("Single player");
				waitingRoomWindow.setGameType(GameType.SINGLE_PLAYER);
				break;
			default:
				break;
			}
			waitingRoomWindow.clearGames();
			for (int i = 0;i < games.size();++i){
				if (games.get(i).getGameType() == waitingRoomWindow.getGameType()){
					waitingRoomWindow.addGame(games.get(i));
				}
			}
			gui.switchTo("waitingRoom");
		} else if (msg instanceof StartGameMessage){
			StartGameMessage message = (StartGameMessage) msg;
			Game game = new Game(message.getGameType(),this,this);
			switch (message.getGameType()){
			case CAMPAIGN:
				InternetGameController internetController = new InternetGameController();
				internetController.setParameter("map", message.getMap());
				game.startGame(currentGameNumber++,GAME_COUNTDOWN,internetController);
				games.add(game);
				game.setMainDesc("Game starting in "+GAME_COUNTDOWN+" seconds");
				game.setSecondaryDesc("Wins : 0  Losses : 0  Ties : 0");
				waitingRoomWindow.addGame(game);
				break;
			case MULTIPLAYER:
				internetController = new InternetGameController();
				internetController.setParameter("map", message.getMap());
				game.startGame(currentGameNumber++,GAME_COUNTDOWN,new InternetGameController());
				games.add(game);
				game.setMainDesc("Game starting in "+GAME_COUNTDOWN+" seconds");
				game.setSecondaryDesc("Wins : 0  Losses : 0  Ties : 0");
				waitingRoomWindow.addGame(game);
				break;
			case REPLAY:
				String map = message.getMap();
				if (map.endsWith(".nrl")){
					//online replay
					games.add(game);
					waitingRoomWindow.addGame(game);
					OnlineReplayController ORController = new OnlineReplayController();
					ORController.setParameter("file", message.getMap());
					game.startGame(currentGameNumber++,1, ORController);
					game.setMainDesc("Game starting in 1 second");
				} else if (map.endsWith(".frl")){
					//offline replay
				} else {
					logger.warning(" StartGameMessage : Niewlasciwe rozszerzenie mapy : "+map);
				}
				break;
			case SINGLE_PLAYER:
				game = null;
				break;
			default:
				logger.error("Program.processMessage - Niewlasciwy typ gry");
			}
		} else if (msg instanceof StopGameMessage){
			StopGameMessage message = (StopGameMessage)msg;
			//TODO jezeli jest gra multiplayer to nie powinna być przerywana w trakcie
			//TODO tak samo exit nie powinno działać jak jest gra multiplayer grana
			Game game = null;
			for (int i = 0;i < games.size() && game == null;++i){
				if (games.get(i).getGameNumber() == message.getGameNumber()){
					game = games.get(i);
				}
			}
			if (game != null){
				games.remove(game);
				game.endGame();
				if (gui.getCurrentWindow() == "waitingRoom"){
					waitingRoomWindow.removeGame(game);
				} else if (gui.getCurrentWindow() == "game"){
					//TODO wyswietlamy w okienku komunikat o koncu gry
					//TODO powinna byc jakas opcja zeby przechodzil po koncu gry do waitingRoom
					//TODO i opcja zeby wchodzil do nowo rozpoczętej gry od razu
				}
			}
		} else if (msg instanceof WaitingForGameMessage){
			WaitingForGameMessage message = (WaitingForGameMessage)msg;
			for (int i = 0;i < games.size();++i){
				if (games.get(i).getGameNumber() == message.getGameNumber()){
					games.get(i).setMainDesc(message.getMessage());
				}
			}
		} else if (msg instanceof RoundStartedMessage){
			RoundStartedMessage message = (RoundStartedMessage)msg;
			for (int i = 0;i < games.size();++i){
				if (games.get(i).getGameNumber() == message.getGameNumber()){
					games.get(i).setMainDesc("Game started (round "+message.getRoundNumber()+")");
				}
			}
		} else if (msg instanceof ShowLogsMessage){
			gui.switchTo("logs");
		} else if (msg instanceof MinimalizeMessage){
			
			frame.setState(Frame.ICONIFIED);
			
		}
			
		return true;
	}
	
	public void processMessages(){
		
		boolean exitProgram = false;
		
		while (!exitProgram){
			
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			while (messages.size() > 0){
				if (!processMessage(messages.remove())) exitProgram = true;
			}
			
			if (logsWindow != null){
				while (!Logging.isEmpty()){
					logsWindow.addLogMessage(Logging.getMessage());
				}
			}
			
		}
		
		logger.info("Zakończył się główny wątek programu.");
		
		System.exit(0);
		
	}
	
	public void createWindow(){
		try {
			
			frame = new JFrame();
			
			CanvasGameContainer container = new CanvasGameContainer(gui);
			
			frame.setUndecorated(true);
			frame.setAlwaysOnTop(false);
			frame.setVisible(true);
			frame.add(container);
			frame.setSize(1366,768);
			
			frame.addWindowListener(new WindowListener(){

				@Override
				public void windowActivated(WindowEvent arg0) {
					
				}

				@Override
				public void windowClosed(WindowEvent arg0) {

				}

				@Override
				public void windowClosing(WindowEvent arg0) {
					//TODO tu powinno być sprawdzenie czy gry są uruchomione
					//TODO powinny one się wykonywać już wtedy bez okienka
					gui.exit = true;
					frame.setVisible(false);
					frame.repaint();
					getMessage(new ExitMessage());
				}

				@Override
				public void windowDeactivated(WindowEvent arg0) {
					
				}

				@Override
				public void windowDeiconified(WindowEvent arg0) {
					
				}

				@Override
				public void windowIconified(WindowEvent arg0) {
					
				}

				@Override
				public void windowOpened(WindowEvent arg0) {
		
				}
				
			});
			
			/*AppGameContainer container = new AppGameContainer(gui);
			container.setAlwaysRender(true);
			container.setTargetFrameRate(24);
			container.setShowFPS(false);
			if (FULLSCREEN){
				container.setDisplayMode(1366, 768, true);
			} else {
				container.setDisplayMode(800, 600, false);
				
			}*/
			container.start();
		} catch (SlickException e){
			e.printStackTrace();
		}
	}

	@Override
	public void gameStarted(int gameNumber,GameEngine game) {

		messages.add(new GameStartedMessage(game,gameNumber));
		
	}
	
	@Override
	public void gameEnded(int gameNumber, GameResult gameResult) {
		
		messages.add(new GameEndedMessage(gameNumber,gameResult));

	}
	
	@Override
	public void waitingForGame(int gameNumber, String message) {

		messages.add(new WaitingForGameMessage(gameNumber,message));
		
	}
	
	@Override
	public void gameError(int gameNumber, String message) {

		messages.add(new GameErrorMessage(gameNumber,message));

	}
	
	public String getMessage(String message){
		return "Wygrano : "+wins+"\nPrzegrano : "+losses+"\n"+message;
	}
	
	@Override
	public void getMessage(ProgramMessage message) {

		messages.add(message);
		
	}
	
	@Override
	public void getTimeToGameStart(int gameNumber, int timeLeft) {

		messages.add(new GameAboutToStartMessage(gameNumber,timeLeft));
		
	}
	@Override
	public void giveCustomMessage(ProgramMessage message) {
		getMessage(message);
	}
	
}



