package gui;

import game.Game;
import game.GameType;

import java.io.File;
import java.util.ArrayList;

import messages.*;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.geom.Point;
import org.newdawn.slick.geom.Rectangle;

public class WaitingRoomWindow extends Window {
	
	static final int MAX_BUTTONS = 50;
	
	static final Rectangle GAME_BUTTONS_CLIP = new Rectangle(60,115,470,575);
	static final Rectangle MAP_NAMES_CLIP = new Rectangle(890,115,470,575);
	
	static Font mapsFont;
	
	float gameButtonsTranslate = 0;
	float gameButtonsV = 0;
	
	float mapNamesTranslate = 0;
	float mapNamesV = 0;
	
	String waitingString = "";
	float waitingStringChange = 0;
	
	static Font titleFont;
	
	String title;
	GameType gameType;
	
	String message = "";
	
	String mapNames = "";
	String selectedMap = null;
	
	Button stopButton,showButton,startButton;
	
	NumberedButton [] gameButtons;
	ArrayList<Game> games;
	
	Integer selectedGame;
	
	Image image;

	public WaitingRoomWindow(GUIListener listener,String name) {
		super(listener, name);
		this.listener = listener;
		
		games = new ArrayList<Game>();
	}
	
	@Override
	public void show(){
		if (gameType == GameType.CAMPAIGN){
			selectedMap = "Random map";
		} else selectedMap = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(GameContainer container) throws SlickException {
		super.init(container);
		
		titleFont = new TrueTypeFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 50),true);
		
		mapsFont = loadUnicodeFont("arial.ttf", 15);
		
		compContainer.addComponent(new Button(1160,695,205,50,"Go Back"
				,() -> listener.getMessage(new GoBackMessage("waitingRoom"))));
		
		compContainer.addComponent(startButton = new Button(0,695,205,50,"Start Game"
				,() -> listener.getMessage(new StartGameMessage(gameType,selectedMap))));
		
		compContainer.addComponent(showButton = new Button(205+parameterX,695,205,50,"Show Game"
				,() -> {
					synchronized (this){
						if (selectedGame < games.size()){
							listener.getMessage(new ShowGameMessage(games.get(selectedGame).getGameNumber()));
						} else {
							selectedGame = null;
						}
					}
				}));
				
		compContainer.addComponent(stopButton = new Button (410+parameterX*2,695,205,50,"Stop Game"
				,() -> {
					synchronized (this){
						if (selectedGame < games.size()){
							listener.getMessage(new StopGameMessage(games.get(selectedGame).getGameNumber()));
						} else {
							selectedGame = null;
						}
					}
				}));

		
		gameButtons = new NumberedButton[MAX_BUTTONS];
		
		for (int i = 0;i < MAX_BUTTONS;++i){
			gameButtons[i] = new NumberedButton(65,120+90*i,400,60,i+1);
			gameButtons[i].setClip(GAME_BUTTONS_CLIP);
			gameButtons[i].setClickAction((NumberedButton button) -> {
				if (selectedGame != null){
					gameButtons[selectedGame].setSelected(false);
				}
				selectedGame = button.number-1;
				button.setSelected(true);
			});
			compContainer.addComponent(gameButtons[i]);
		}
		
		image = loadImage("knight.png");
		
	}

	@Override
	public void update(GameContainer container, int delta) {
		super.update(container, delta);
		
		/* Mapy */
		
		mapNames = "";
		
		if (gameType == GameType.REPLAY){
		
			File dir = new File("D:\\Programowanie\\Luna Workspace\\CodeArenaWithGUI");
			File [] directoryListing = dir.listFiles();
			
			if (directoryListing != null){
				for (File file : directoryListing){
					if (file.getName().endsWith(".nrl") || file.getName().endsWith(".frl")){
						mapNames += file.getName() + "\n";
					}
				}
			} else {
				System.out.println("Niewlasciwy katalog replay'ow");
			}
		
		} else if (gameType == GameType.CAMPAIGN){
			mapNames += "Random map\n";
			mapNames += "campaign_easy\n";
			mapNames += "campaign_distance\n";
			mapNames += "campaign_swamp\n";
			mapNames += "campaign_2_units\n";
			mapNames += "campaign_stones\n";
			mapNames += "campaign_cosmos\n";
			mapNames += "solo_hard\n";
			mapNames += "3_players\n";
			mapNames += "bottleneck\n";
			mapNames += "complex_map\n";
			mapNames += "extreme\n";
		}
		
		int textHeight = 0;
		for (String line : mapNames.split("\n")){
			textHeight += mapsFont.getHeight(line);
		}
		
		/* ... */
		
		//Zmiana polozenia ze wzgledu na predkosc
		gameButtonsTranslate += gameButtonsV*delta/1000f;
		mapNamesTranslate += mapNamesV*delta/1000f;
		
		//Nakładanie ograniczeń na położenie
		gameButtonsTranslate = Math.max(-90*(games.size()-1)-65+575, gameButtonsTranslate);
		gameButtonsTranslate = Math.min(0,gameButtonsTranslate);
		mapNamesTranslate = Math.max(575-textHeight,mapNamesTranslate);
		mapNamesTranslate = Math.min(0,mapNamesTranslate);
		
		//Przychamowanie prędkości
		gameButtonsV = Math.signum(gameButtonsV)*Math.max(0,(Math.abs(gameButtonsV)-(1300)*delta/1000f));
		mapNamesV = Math.signum(mapNamesV)*Math.max(0,(Math.abs(mapNamesV)-(1300)*delta/1000f));
		
		if (waitingStringChange <= 0){
			waitingStringChange = 600;
			switch (waitingString){
			case "":
				waitingString = ".";
				break;
			case ".":
				waitingString = "..";
				break;
			case "..":
				waitingString = "...";
				break;
			case "...":
				waitingString = "";
				break;
			}
		}
		
		waitingStringChange -= delta;
		
		if (selectedMap != null || gameType == GameType.MULTIPLAYER){
			startButton.setEnabled(true);
		} else startButton.setEnabled(false);
		
		int gamesNumber = 0;
		
		synchronized (this){
			
			for (int i = 0;i < games.size();++i){
				gameButtons[i].setMainDesc(games.get(i).getMainDesc().replace("<...>", waitingString));
				gameButtons[i].setSecondaryDesc(games.get(i).getSecondaryDesc());
			}
			
			gamesNumber = games.size();
		
		}
		
		for (int i = 0;i < MAX_BUTTONS;++i){
			gameButtons[i].setVisible(i < gamesNumber);
		}
		
		if (selectedGame != null) {
			
			if (selectedGame < gamesNumber){
				showButton.setVisible(true);
				stopButton.setVisible(true);
			} else {
				showButton.setVisible(false);
				stopButton.setVisible(false);
				gameButtons[selectedGame].setSelected(false);
				selectedGame = null;
			}
		
		} else {
			showButton.setVisible(false);
			stopButton.setVisible(false);
		}
		
		for (int i = 0;i < games.size();++i){
			gameButtons[i].setTranslate(new Point(0,gameButtonsTranslate));
		}

	}

	@Override
	public void render(GameContainer container, Graphics g) {
		
		Font oldFont = g.getFont();
		
		int mouseX = container.getInput().getMouseX();
		int mouseY = container.getInput().getMouseY();
		
		/* Tytuł okna */
		
		g.setFont(titleFont);
		Color oldColor = g.getColor();
		g.setColor(new Color(60,70,60));
		
		g.drawString(title, 40, 5);
		
		if (image != null){
			g.drawImage(image, 440, 105,440+image.getWidth()/2f,105+image.getHeight()/2f
					,0,0,image.getWidth(),image.getHeight());
		}
		
		
		g.setColor(oldColor);

		g.setAntiAlias(true);
		
		Rectangle oldClip = copyRectangle(g.getClip());

		/* Mapy */
		
		g.setClip(MAP_NAMES_CLIP);
		g.translate(0,mapNamesTranslate);
		
		g.setFont(mapsFont);
		
		int currentPos = 115;
		int height = 0;
		
		for (String line : mapNames.split("\n")){
			height = mapsFont.getHeight(line);
			boolean selected = (selectedMap != null && selectedMap.equals(line));
			boolean mouseOver = false;
			if (MAP_NAMES_CLIP.contains(mouseX,mouseY)){
				if (mouseY-mapNamesTranslate > currentPos && mouseY-mapNamesTranslate <= currentPos+height){
					mouseOver = true;
				}
			}
			oldColor = g.getColor();
			if (selected) g.setColor(new Color(150,200,220));
			else if (mouseOver) g.setColor(new Color(80,140,200));
			else g.setColor(new Color(50,50,50));
			g.drawString(line, 895, currentPos);
			g.setColor(oldColor);
			currentPos += height;
		}
		
		//drawStringMultiline(g, mapNames, 895, 115);
		
		g.resetTransform();
		
		/* Ustawiamy stare przyciecie */
		
		g.setClip(oldClip);
		
		/* Ustawiamy starą czcionkę */
		
		g.setFont(oldFont);
		
		g.setAntiAlias(false);

		super.render(container, g);

	}

	@Override
	public void mouseWheelMoved(int change) {
		super.mouseWheelMoved(change);
		
		float mouseX = gc.getInput().getMouseX();
		float mouseY = gc.getInput().getMouseY();
		
		if (GAME_BUTTONS_CLIP.contains(mouseX, mouseY)){
			gameButtonsV = Math.signum(change)*(800);
		} else if (MAP_NAMES_CLIP.contains(mouseX,mouseY)){
			mapNamesV = Math.signum(change)*(400);
		}
		
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		super.mouseDragged(oldx, oldy, newx, newy);

	}

	@Override
	public void mousePressed(int button, int x, int y) {
		super.mousePressed(button, x, y);
		
		/* Mapy */
		
		if (selectedGame != null) gameButtons[selectedGame].setSelected(false);
		selectedGame = null;
		
		int currentPos = 115;
		int height = 0;
		
		for (String line : mapNames.split("\n")){
			height = mapsFont.getHeight(line);
			if (MAP_NAMES_CLIP.contains(x,y)){
				if (y-mapNamesTranslate > currentPos && y-mapNamesTranslate <= currentPos+height){
					selectedMap = line;
					break;
				}
			}
			currentPos += height;
		}
		
	}

	@Override
	public void keyPressed(int key, char c) {
		super.keyPressed(key, c);

	}
	
	public void setMessage(String message){
		this.message = message;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public void setGameType(GameType gameType){
		this.gameType = gameType;
	}
	
	public GameType getGameType(){
		return gameType;
	}
	
	public synchronized void addGame(Game game){
		games.add(game);
	}
	
	public synchronized void removeGame(Game game){
		games.remove(game);
	}
	
	public void clearSelection(){
		selectedGame = null;
	}
	
	public void clearGames(){
		games.clear();
	}

}
