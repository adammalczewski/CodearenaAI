package gui;

import game.GameEngine;
import gameControllers.GameResult;
import gameControllers.GameSpeed;

import java.awt.Point;

import messages.ChangeGameSpeedMessage;
import messages.ChangeRoundMessage;
import messages.ChangeRoundMessage.RoundChange;
import messages.GoBackMessage;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.RoundedRectangle;

import structures.MapField;
import structures.Node;
import structures.Unit;
import structures.MapField.Background;

public class GameWindow extends Window {
	
	private static boolean DRAW_COORDINATES = false;
	private static boolean SHOW_MOUSE_POSITION = false;
	private static boolean DRAW_NODE_VALUE = false;
	
	private static final Rectangle GAME_AREA_RECTANGLE = new Rectangle(0,0,1366,675);
	private static final Rectangle LOG_RECT = new Rectangle(40,40,1200,595);
	
	static Font countdownFont;
	static Font roundNumberFont;
	
	Integer secondsToStart;
	
	final int hexStringDiffX = 55;
	final int hexStringDiffY = 180;
	
	private GameEngine game;
	private int gameNumber;

	final static float STANDARD_ZOOM = 0.08f;
	
	final static int ODD_ROWS_DISTANCE_X = 600;
	final static int ROWS_DISTANCE_Y = 500;
	final static int ROWS_DISTANCE_X = -10;
	
	final static float MAP_BORDERX = 1; // w hexach
	final static float MAP_BORDERY = 1;
	
	Point selected;
	
	boolean spyOnActualUnit = false;
	
	int hexWidth = 0;
	int hexHeight = 0;
	
	Image grassImage;
	Image crystalFloorImage;
	Image blueAltarImage;
	Image greenAltarImage;
	Image stoneImage;
	Image diamondImage;
	Image forestImage;
	Image swampImage;
	Image stoneBackgroundImage;
	Image voidImage;
	Image golemImage;
	Image knightImage;
	Image unitMarkerImage;
	Image unknownImage;
	
	int cameraX = 0;
	int cameraY = 0;
	
	int zoom = 0;
	float zoomMultiplier = STANDARD_ZOOM;
	
	Button backButton;
	
	TextScrollableClip logClip;

	public GameWindow(GUIListener listener,String name) {
		super(listener,name);
		this.listener = listener;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(GameContainer container) throws SlickException{
		super.init(container);
		
		countdownFont = loadUnicodeFont("arial.ttf",300);
		roundNumberFont = loadUnicodeFont("arial.ttf",55);
		
		logClip = new TextScrollableClip(LOG_RECT);
		logClip.setWheelSpeed(600);
		
		compContainer.addComponent(logClip);
		
		
		grassImage = loadImage("grass.png");
		crystalFloorImage = loadImage("crystalFloor.png");
		blueAltarImage = loadImage("bluealtar.png");
		greenAltarImage = loadImage("greenaltar.png");
		stoneImage = loadImage("stone.png");
		diamondImage = loadImage("diamond.png");
		forestImage = loadImage("forest.png");
		swampImage = loadImage("swamp.png");
		stoneBackgroundImage = loadImage("stoneBackground.png");
		voidImage = loadImage("voidAlpha.png");
		golemImage = loadImage("golem.png");
		knightImage = loadImage("knight.png");
		unitMarkerImage = loadImage("unitMarker.png");
		unknownImage = loadImage("unknown.png");
		
		
		if (grassImage != null){
			hexWidth = grassImage.getWidth();
			hexHeight = grassImage.getHeight();
		} else {
			hexWidth = 40;
			hexHeight = 40;
		}
		cameraX = 5*hexWidth;
		cameraY = 5*ROWS_DISTANCE_Y;
		
		
		backButton = new Button(1160,695,205,50,"Go Back");
		backButton.setClickAction(() -> listener.getMessage(new GoBackMessage(name)));
		compContainer.addComponent(backButton);
		
	}

	@Override
	public void update(GameContainer container, int delta) {
		
		
		if (selected != null && game != null){
			for(Unit unit : game.units.values()){
				if (unit.posX == selected.x && unit.posY == selected.y && unit.log != null){
					String prevText = logClip.getText();
					if (prevText == null || !prevText.equals(unit.log)){
						logClip.setText(unit.log);
					}
				}
			}
		}
		
		boolean logVisible = false;
		if (selected != null){
			for(Unit unit : game.units.values()){
				if (unit.posX == selected.x && unit.posY == selected.y && unit.log != null){
					logVisible = true;
				}
			}
		}
		logClip.setVisible(logVisible);
		
		super.update(container, delta);
		
	}

	@Override
	public void render(GameContainer container, Graphics g) {
		
		boolean [][] coordinates = new boolean[GameEngine.MAX_MAPSIZEX][GameEngine.MAX_MAPSIZEY];
			
		float mouseX = screenToGameX(gc.getInput().getMouseX());
		float mouseY = screenToGameY(gc.getInput().getMouseY());
			
		if (SHOW_MOUSE_POSITION){
			
			g.drawString("mysz : x = "+mouseX+" y = "+mouseY, 500, 10);
			
		}
		
		Rectangle old2Clip = copyRectangle(g.getClip());
		
		Color oldColor = g.getColor();
		g.setColor(new Color(40,80,120));
		
		g.drawLine(0, 675, 1366, 675);
		g.drawLine(115, 675, 115,768);
		
		g.setColor(oldColor);
		
		if (game != null){
				
			Font oldFont = g.getFont();
			g.setFont(roundNumberFont);
			
			drawStringCentered(g,Integer.toString(game.getRoundNumber()),new Rectangle(0,675,115,93));
		
			g.setFont(oldFont);
		
		}
		
		g.setClip(GAME_AREA_RECTANGLE);
		
		if (game != null){
		
			Node [][] map = game.getMap();
			
			for (int y = 0;y < GameEngine.MAX_MAPSIZEY;++y)
				for (int x = 0;x < GameEngine.MAX_MAPSIZEX;++x){
			
					Image hexImage = null;
					Image backgroundExtraImage = null;
					int backgroundExtraDiffX = 0;
					int backgroundExtraDiffY = 0;
					Image buildingImage = null;
					int buildingDiffX = 0;
					int buildingDiffY = 0;
					Image objectImage = null;
					int objectDiffX = 0;
					int objectDiffY = 0;
					Image unitImage = null;
					int unitDiffX = 0;
					int unitDiffY = 0;
					int unitHealthGaugeDiffX = 0;
					int unitHealthGaugeDiffY = 0;
					int unitMarkerDiffX = 0;
					int unitMarkerDiffY = 0;
					float unitHealthPercents = 0.0f;
					boolean mouseOver = false;
					
					boolean markedUnit = false;
						
					mouseOver = overHex(mouseX,mouseY,x,y);
					
					
					//Sprawdzamy jednostki gracza
					for (Unit unit : game.units.values()){
						//FIXME zalozenie ze zycia kazda jednostka ma maksymalnie 100
						
						if (unit.posX == x && unit.posY == y){
							if (game.actualUnit != null && game.actualUnit == unit.id){
								markedUnit = true;
							}
							unitHealthPercents = unit.hp;
							if (unit.player == 1){
								unitImage = golemImage;
								unitDiffX = 90;
								unitDiffY = -385;
								unitHealthGaugeDiffX = 155;
								unitHealthGaugeDiffY = -460;
								unitMarkerDiffX = 275;
								unitMarkerDiffY = -1565;
							} else if (unit.player == 2){
								unitImage = knightImage;
								unitDiffX = -30;
								unitDiffY = -505;
								unitHealthGaugeDiffX = 170;
								unitHealthGaugeDiffY = -655;
								unitMarkerDiffX = 275;
								unitMarkerDiffY = -1700;
							}
						}
					}
					if (map[x][y].field != null){
						if (map[x][y].field.background == Background.GRASS){
							hexImage = grassImage;
						} else if (map[x][y].field.background == Background.CRYSTALFLOOR){
							hexImage = crystalFloorImage;
						} else if (map[x][y].field.background == Background.FOREST){
							hexImage = grassImage;
							backgroundExtraImage = forestImage;
							backgroundExtraDiffX = 180;
							backgroundExtraDiffY = -255;
						} else if (map[x][y].field.background == Background.SWAMP){
							hexImage = swampImage;
						} else if (map[x][y].field.background == Background.STONE){
							hexImage = grassImage;
							backgroundExtraImage = stoneBackgroundImage;
							backgroundExtraDiffX = 120;
							backgroundExtraDiffY = 90;
						} else if (map[x][y].field.background == Background.VOID){
							backgroundExtraImage = voidImage;
						}
						if (map[x][y].field.building == MapField.Building.ALTAR){
							hexImage = null;
							if (map[x][y].field.buildingPlayer == 1){
								buildingImage = greenAltarImage;
							} else if (map[x][y].field.buildingPlayer == 2){
								buildingImage = blueAltarImage;
							}
							buildingDiffX = 20;
							buildingDiffY = -180;
						}
						if (map[x][y].field.object == MapField.Object.STONE){
							objectImage = stoneImage;
							objectDiffX = 180;
							objectDiffY = 0;
						} else if (map[x][y].field.object == MapField.Object.DIAMOND){
							objectImage = diamondImage;
							objectDiffX = 375;
							objectDiffY = -140;
						} else if (map[x][y].field.object == MapField.Object.UNKNOWN){
							objectImage = unknownImage;
							objectDiffX = 305;
							objectDiffY = 0;
						}
						if (map[x][y].field.unit != null && game.getPlayerID() != null 
								&& map[x][y].field.unit.player != game.getPlayerID()){
							unitHealthPercents = map[x][y].field.unit.hp;
							if (map[x][y].field.unit.player == 1){
								unitImage = golemImage;
								unitDiffX = 90;
								unitDiffY = -385;
								unitHealthGaugeDiffX = 155;
								unitHealthGaugeDiffY = -460;
							} else if (map[x][y].field.unit.player == 2){
								unitImage = knightImage;
								unitDiffX = -30;
								unitDiffY = -505;
								unitHealthGaugeDiffX = 170;
								unitHealthGaugeDiffY = -655;
							} else {
								System.out.println("Warning : gui.render - dostalismy numer gracza"
										+ ", ktory nie odpowiada zadnemu obrazkowi postaci");
							}
						}
					}
					
					if (markedUnit && spyOnActualUnit){
						selected = new Point(x,y);
					}
					
					Color color = Color.white;
					if (selected != null && selected.x == x && selected.y == y) color = Color.blue;
					else if (mouseOver) color = new Color(65,105,225);
					
					if (hexImage != null) {
						drawImageOnHex(g,x,y,0,0,hexImage,color);
					}
					
					if (backgroundExtraImage != null){
						drawImageOnHex(g,x,y,backgroundExtraDiffX,backgroundExtraDiffY,backgroundExtraImage,color);
					}
						
					if (buildingImage != null) drawImageOnHex(g,x,y,buildingDiffX,buildingDiffY,buildingImage,color);

					if (objectImage != null) drawImageOnHex(g,x,y,objectDiffX,objectDiffY,objectImage,color);
						
					if (unitImage != null){
						
						drawImageOnHex(g,x,y,unitDiffX,unitDiffY,unitImage,color);
						
						g.setAntiAlias(true);
						
						RoundedRectangle hpGaugeRectBackground = new RoundedRectangle(hexToScreenX(x,y,unitHealthGaugeDiffX)
								,hexToScreenY(y,unitHealthGaugeDiffY),795*zoomMultiplier,125*zoomMultiplier,50*zoomMultiplier);
						
						RoundedRectangle hpGaugeRectInside = new RoundedRectangle(hexToScreenX(x,y,unitHealthGaugeDiffX)
								,hexToScreenY(y,unitHealthGaugeDiffY),795*zoomMultiplier,125*zoomMultiplier,50*zoomMultiplier);
						
						g.setColor(Color.black);
						g.fill(hpGaugeRectBackground);
						
						Rectangle oldClip = copyRectangle(g.getClip());
						g.setClip(joinClips(oldClip,new Rectangle(hexToScreenX(x,y,unitHealthGaugeDiffX),hexToScreenY(y,unitHealthGaugeDiffY)
								,800*zoomMultiplier*unitHealthPercents/100f,130*zoomMultiplier)));
						
						g.setColor(hpColor(unitHealthPercents));
						g.fill(hpGaugeRectInside);
						g.setClip(oldClip);
						
						g.setColor(Color.white);
						g.draw(hpGaugeRectBackground);
						
						g.setAntiAlias(false);
						
						if (markedUnit){
							drawImageOnHex(g,x,y,unitMarkerDiffX,unitMarkerDiffY,unitMarkerImage,color);
						}
						
					}

					if (hexImage != null || backgroundExtraImage != null || unitImage != null
							|| buildingImage != null){
						coordinates[x][y] = true;
					} else coordinates[x][y] = false;
					
				}
			
			for (int y = 0;y < GameEngine.MAX_MAPSIZEY;++y)
				for (int x = 0;x < GameEngine.MAX_MAPSIZEX;++x) if (coordinates[x][y]){
					if (DRAW_NODE_VALUE){
						if (map[x][y].value != null){
							Color oldColor2 = g.getColor();
							g.setColor(new Color(150,150,200));
							drawStringOnHex(g,x,y,map[x][y].value);
							g.setColor(oldColor2);
						}
					} else if (DRAW_COORDINATES){
						Color oldColor2 = g.getColor();
						g.setColor(new Color(150,150,200));
						drawStringOnHex(g,x,y,x+" "+y);
						g.setColor(oldColor2);
					}
				}

			
			if (game.getGameResult() != null && game.getGameResult() != GameResult.UNKNOWN){
				Font oldFont = g.getFont();
				g.setFont(countdownFont);
				drawStringCentered(g,game.getGameResult().toString(),GAME_AREA_RECTANGLE);
				g.setFont(oldFont);
			}
		
		} else if (secondsToStart != null){
			
			Font oldFont = g.getFont();
			g.setFont(countdownFont);
			drawStringCentered(g,secondsToStart.toString(),GAME_AREA_RECTANGLE);
			g.setFont(oldFont);
			
		}
		
		g.setClip(old2Clip);
		
		super.render(container, g);
		
	}

	@Override
	public void mouseWheelMoved(int change) {
		super.mouseWheelMoved(change);
		
		Input input = gc.getInput();
	
		float zoomMultiplierStart = zoomMultiplier;
		zoom += change;
		zoom = Math.max(-3000,zoom);
		zoom = Math.min(3000, zoom);
		zoomMultiplier = STANDARD_ZOOM*((zoom >= 0)?(zoom/500.0f+1f):(1.0f/(-zoom/500.0f+1f)));
		float zoomChange = zoomMultiplier/zoomMultiplierStart;
		if (zoomChange > 1.0f){
			
			cameraX += (int) (input.getMouseX()/zoomMultiplierStart*(1f-1f/zoomChange));
			cameraY += (int) (input.getMouseY()/zoomMultiplierStart*(1f-1f/zoomChange));
		} else {
			cameraX += (int) (gc.getWidth()/2f/zoomMultiplierStart*(1f-1f/zoomChange));
			cameraY += (int) (gc.getHeight()/2f/zoomMultiplierStart*(1f-1f/zoomChange));
		}
		
		cameraX = Math.min(cameraX,(int)((GameEngine.MAX_MAPSIZEX+MAP_BORDERX)*hexWidth-gc.getWidth()/zoomMultiplier));
		cameraX = Math.max(cameraX,(int)(-MAP_BORDERX*hexWidth));
		
		cameraY = Math.min(cameraY,(int)((GameEngine.MAX_MAPSIZEY+MAP_BORDERY)*ROWS_DISTANCE_Y-gc.getHeight()/zoomMultiplier));
		cameraY = Math.max(cameraY, (int)(-MAP_BORDERY*ROWS_DISTANCE_Y));
	
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		super.mouseDragged(oldx, oldy, newx, newy);
		
		Input input = gc.getInput();
		
		if (input.isMouseButtonDown(MOUSE_BUTTON_LEFT)){
		
			float zoomMultiplier = STANDARD_ZOOM*((zoom >= 0)?(zoom/500.0f+1f):(1.0f/(-zoom/500.0f+1f)));
			cameraX -= (newx-oldx)/zoomMultiplier;
			cameraY -= (newy-oldy)/zoomMultiplier;
			
			cameraX = Math.min(cameraX,(int)((GameEngine.MAX_MAPSIZEX+MAP_BORDERX)*hexWidth-gc.getWidth()/zoomMultiplier));
			cameraX = Math.max(cameraX,(int)(-MAP_BORDERX*hexWidth));
			
			cameraY = Math.min(cameraY,(int)((GameEngine.MAX_MAPSIZEY+MAP_BORDERY)*ROWS_DISTANCE_Y-gc.getHeight()/zoomMultiplier));
			cameraY = Math.max(cameraY, (int)(-MAP_BORDERY*ROWS_DISTANCE_Y));
		
		}
		
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		super.mousePressed(button, x, y);
		
		if (button == MOUSE_BUTTON_LEFT){
			
			selected = mouseSelectedHex();
			
		}
		
		
	}

	@Override
	public void keyPressed(int key, char c) {
		super.keyPressed(key, c);
		
		if (c == 'c'){
			DRAW_COORDINATES = !DRAW_COORDINATES;
		} else if (c == 'm'){
			SHOW_MOUSE_POSITION = !SHOW_MOUSE_POSITION;
		} else if (c == 'n'){
			listener.getMessage(new ChangeRoundMessage(gameNumber,RoundChange.NEXT_UNIT));
		} else if (c == '0'){
			listener.getMessage(new ChangeGameSpeedMessage(gameNumber,GameSpeed.STEPS));
		} else if (c == '1'){
			listener.getMessage(new ChangeGameSpeedMessage(gameNumber,GameSpeed.VERYSLOW));
		} else if (c == '2'){
			listener.getMessage(new ChangeGameSpeedMessage(gameNumber,GameSpeed.SLOW));
		} else if (c == '3'){
			listener.getMessage(new ChangeGameSpeedMessage(gameNumber,GameSpeed.NORMAL));
		} else if (c == '4'){
			listener.getMessage(new ChangeGameSpeedMessage(gameNumber,GameSpeed.FAST));
		} else if (c == '5'){
			listener.getMessage(new ChangeGameSpeedMessage(gameNumber,GameSpeed.FASTER));
		} else if (c == '6'){
			listener.getMessage(new ChangeGameSpeedMessage(gameNumber,GameSpeed.FASTEST));
		} else if (c == 's'){
			spyOnActualUnit = !spyOnActualUnit;
		} else if (c == 'v'){
			DRAW_NODE_VALUE = !DRAW_NODE_VALUE;
		}
		
	}
	
	synchronized public void setGameEngine(GameEngine game){
		this.game = game;
	}
	
	public float screenToGameX(int x){
		return x/zoomMultiplier+cameraX;
	}
	
	public float screenToGameY(int y){
		return y/zoomMultiplier+cameraY;
	}
	
	public float gameToScreenX(float x){
		return (x-cameraX)*zoomMultiplier;
	}
	
	public float gameToScreenY(float y){
		return (y-cameraY)*zoomMultiplier;
	}
	
	public float hexToGameX(int hexX,int hexY,float diffX){
		return diffX+((hexY%2==1)?ODD_ROWS_DISTANCE_X:0)+(hexWidth+ROWS_DISTANCE_X)*hexX;
	}
	
	public float hexToGameY(int hexY,float diffY){
		return diffY+(ROWS_DISTANCE_Y)*hexY;
	}
	
	public float hexToScreenX(int hexX,int hexY,int diffX){
		return (-cameraX+diffX+((hexY%2==1)?ODD_ROWS_DISTANCE_X:0)+(hexWidth+ROWS_DISTANCE_X)*hexX)*zoomMultiplier;
	}
	
	public float hexToScreenY(int hexY,int diffY){
		return (-cameraY+diffY+(ROWS_DISTANCE_Y)*hexY)*zoomMultiplier;
	}
	
	public float hexToScreenXEnd(int hexX,int hexY,int diffX,int width){
		return (-cameraX+diffX+((hexY%2==1)?ODD_ROWS_DISTANCE_X:0)+(hexWidth+ROWS_DISTANCE_X)*hexX+width)*zoomMultiplier;
	}
	
	public float hexToScreenYEnd(int hexY,int diffY,int height){
		return (-cameraY+diffY+(ROWS_DISTANCE_Y)*hexY+height)*zoomMultiplier;
	}
	
	public void drawImageOnHex(Graphics g,int hexX,int hexY,int diffX,int diffY,Image image){
		g.drawImage(image, hexToScreenX(hexX,hexY,diffX), hexToScreenY(hexY,diffY)
				, hexToScreenXEnd(hexX,hexY,diffX,image.getWidth())
				, hexToScreenYEnd(hexY,diffY,image.getHeight()), 0, 0, image.getWidth(), image.getHeight());
	}
	
	public void drawImageOnHex(Graphics g,int hexX,int hexY,int diffX,int diffY,Image image,Color color){
		g.drawImage(image, hexToScreenX(hexX,hexY,diffX), hexToScreenY(hexY,diffY)
				, hexToScreenXEnd(hexX,hexY,diffX,image.getWidth())
				, hexToScreenYEnd(hexY,diffY,image.getHeight()), 0, 0, image.getWidth(), image.getHeight(), color);
	}
	
	public void drawStringOnHex(Graphics g,int hexX,int hexY,String string){
		g.drawString(string,hexToScreenX(hexX,hexY,hexStringDiffX),hexToScreenY(hexY,hexStringDiffY));
	}
	
	public Color hpColor(float percents){
		if (percents < 10){
			return Color.red;
		} else if (percents < 30){
			return Color.pink;
		} else if (percents < 70){
			return Color.yellow;
		} else return Color.green;
	}
	
	public void drawLine(Graphics g,float a,float b){
		g.drawLine(gameToScreenX(0), gameToScreenY(b), gameToScreenX(100000), gameToScreenY(100000*a+b));
	}
	
	public boolean overHex(float x,float y,int hexX,int hexY){
		float posX = hexToGameX(hexX,hexY,0);
		float posY = hexToGameY(hexY,0);
		return x > posX && x < posX+1130 && y > (x-posX)/(-4.0f) + posY+225
				&& y > (x-posX)/(4.0f) +posY-90 && y < (x-posX)/4.0f+posY+540
				&& y < (x-posX)/(-4.0f) + posY+870;
	}
	
	public Point mouseSelectedHex(){
		Point result = null;
		float mouseX = screenToGameX(gc.getInput().getMouseX());
		float mouseY = screenToGameY(gc.getInput().getMouseY());
		for (int y = 0;y < GameEngine.MAX_MAPSIZEY && result == null;++y)
			for (int x = 0;x < GameEngine.MAX_MAPSIZEX && result == null;++x){
				if (overHex(mouseX,mouseY,x,y)) result = new Point(x,y);
			}
		return result;
	}
	
	public void setGameNumber(int gameNumber){
		this.gameNumber = gameNumber;
	}
	
	public int getGameNumber(){
		return gameNumber;
	}
	
	public void setSecondsToStart(Integer secondsToStart){
		this.secondsToStart = secondsToStart;
	}

}
