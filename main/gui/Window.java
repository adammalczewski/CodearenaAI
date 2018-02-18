package gui;

import gui.Window.Component.EventHookType;
import gui.Window.Component.EventType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

import logs.LogDisplayer;
import logs.LogMessage;
import logs.Logger;
import logs.Logging;

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
import org.newdawn.slick.geom.Point;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.RoundedRectangle;

import messages.MinimalizeMessage;


//TODO zrobić okienko komunikatów (bedzie mialo krzyżyk do zamykania - bedzie to guzik z napisem "x" w środku)
//TODO zrobić żeby ComponentContainer też był komponentem
public class Window {
	
	static final int MOUSE_BUTTON_LEFT = 0;
	static final int MOUSE_BUTTON_RIGHT = 1;
	static final int MOUSE_BUTTON_MIDDLE = 2;
	
	int parameterX,parameterY;

	boolean showFPS = false;
	boolean showParameters = false;
	
	GameContainer gc;
	ComponentContainer compContainer;
	
	GUIListener listener;
	
	String name;

	Logger logger = Logging.getLogger(Window.class.getName(),"gui");
	static Logger staticLogger = Logging.getLogger(Window.class.getName(),"gui");

	public Window(GUIListener listener,String name) {
		this.name = name;
		this.listener = listener;
	}
	
	public void init(GameContainer container) throws SlickException{
		gc = container;
		
		compContainer = new ComponentContainer(container);
		compContainer.addComponent(new Button(1300,10,30,30,"-",() -> listener.getMessage(new MinimalizeMessage())));
		
	}
	
	public void update(GameContainer container, int delta){
		
		compContainer.update(delta);
		
	}
	
	public void render(GameContainer container, Graphics g){
		
		compContainer.render(container, g);
		
		if (showParameters){
			g.drawString("parameterX : "+parameterX,1195,10);
			g.drawString("parameterY : "+parameterY,1195,25);
		}
		
	}
	
	public void mouseWheelMoved(int change){
		
	}
	
	public void mouseDragged(int oldx, int oldy, int newx, int newy){
		
	}
	
	public void mousePressed(int button, int x, int y){
		
	}
	
	public void show(){
		
	}
	
	public void keyPressed(int key, char c){
		if (c == 'x'){
			parameterX += 5;
		} else if (c == 'z'){
			parameterX -= 5;
		} else if (c == 't'){
			parameterY -= 5;
		} else if (c == 'y'){
			parameterY += 5;
		} else if (c == 'f'){
			showFPS = !showFPS;
			gc.setShowFPS(showFPS);
		} else if (c == 'q'){
			showParameters = !showParameters;
		}
	}
	
	public static Rectangle joinClips(Rectangle clip1,Rectangle clip2){
		return new Rectangle(Math.max(clip1.getX(),clip2.getX())
			,Math.max(clip1.getY(),clip2.getY())
			,Math.min(clip1.getMaxX(),clip2.getMaxX())-Math.max(clip1.getX(),clip2.getX())
			,Math.min(clip1.getMaxY(),clip2.getMaxY())-Math.max(clip1.getY(),clip2.getY()));
	}
	
	public static Rectangle copyRectangle(Rectangle rect){
		return (rect == null)?null:new Rectangle(rect.getX(),rect.getY(),rect.getWidth(),rect.getHeight());
	}
	
	public static void drawStringCentered(Graphics g,String string,Rectangle rect){
		
		Font font = g.getFont();
		
		float fontWidth = font.getWidth(string);
		float fontHeight = font.getHeight(string);
		
		g.drawString(string, rect.getX()+(rect.getWidth()-fontWidth)/2f
				, rect.getY()+(rect.getHeight()-fontHeight)/2f);
	}
	
	public static void drawStringRightAligned(Graphics g,String string, float topRightX,float topRightY){
		
		Font font = g.getFont();
		
		float fontWidth = font.getWidth(string);
		
		g.drawString(string, topRightX-fontWidth, topRightY);
		
	}
	
    public static void drawStringMultiline(Graphics g, String text, int x, int y) {
    	String prevLine = null;
    	if (text != null){
	        for (String line : text.split("\n")){
	            g.drawString(line, x, y += ((prevLine == null)?0:((prevLine.equals(""))?g.getFont().getHeight("|"):g.getFont().getHeight(prevLine))));
	            prevLine = line;
	        }
    	}
    }
    
    public static void drawStringMultiline(Graphics g, String [] text,int [] heights,int x, int y){
    	for (int i = 0;i < text.length;++i){
    		g.drawString(text[i],x,y += heights[i]);
    	}
    }
    
    public static Image loadImage(String fileName) throws SlickException{
    	Image result = null;
    	try {
    		result = new Image(fileName);
    	} catch (RuntimeException e){
    		staticLogger.error("Nie znaleziono obrazka : "+fileName);
    	}
    	return result;
    }
    
    public static Font loadUnicodeFont(String fileName,int size) throws SlickException{
		
    	Font result = null;
    	
    	try {
	    	result = new UnicodeFont(fileName, size, false, false);
			
			((UnicodeFont)result).addAsciiGlyphs();
			((UnicodeFont)result).getEffects().add(new ColorEffect());
			((UnicodeFont)result).loadGlyphs();
			
			return result;
    	} catch (RuntimeException e){
    		staticLogger.error("Nie znaleziono pliku czcionki : "+fileName);
    		result = new TrueTypeFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, size),true);
    	}
    	
    	return result;
    }
    
    public static void drawStringMultiline(Graphics g, String [] text,int [] heights,int from,int to,int x, int y){
    	for (int i = 0;i < from && i < text.length;++i){
    		y += heights[i];
    	}
    	for (int i = from;i < text.length && i < to;++i){
    		g.drawString(text[i],x,y);
    		y += heights[i];
    	}
    }
    
    public static void drawStringMultiline(Graphics g, ArrayList<String> text,ArrayList<Integer> heights,int from,int to,int x, int y){
    	for (int i = 0;i < from && i < text.size();++i){
    		y += heights.get(i);
    	}
    	for (int i = from;i < text.size() && i < to;++i){
    		g.drawString(text.get(i),x,y);
    		y += heights.get(i);
    	}
    }
	
	public static class Button extends Component{
		
		Logger logger = Logging.getLogger(Button.class.getName(),"gui");
		
		static interface VoidWorker{
			void work();
		}
		
		static final Font newFont = new TrueTypeFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 33),true);
		
		VoidWorker worker = null;
		
		String name;
		
		public Button(float x,float y,float width,float height,String name){
			super(new Rectangle(x,y,width,height));

			this.name = name;
			
			eventHooks.put(EventType.MOUSE_PRESS, EnumSet.of(EventHookType.BLOCKS,EventHookType.GETS));
	
		}
		
		public Button(float x,float y,float width,float height,String name, VoidWorker worker){
			super(new Rectangle(x,y,width,height));

			this.name = name;
			this.worker = worker;
			
			eventHooks.put(EventType.MOUSE_PRESS, EnumSet.of(EventHookType.BLOCKS,EventHookType.GETS));
	
		}
		
		public void setClickAction(VoidWorker worker){
			this.worker = worker;
		}
		
		@Override
		public void render(GameContainer gc,Graphics g){
			
			Input input = gc.getInput();
			int x = input.getMouseX();
			int y = input.getMouseY();
			
			Color oldColor = g.getColor();
			
			if (rect.contains(x,y) && enabled) g.setColor(new Color(80,140,200));
			else g.setColor(new Color(50,50,50));
			
			Font oldFont = g.getFont();
			g.setFont(newFont);
			
			float fontWidth = newFont.getWidth(name);
			float fontHeight = newFont.getHeight(name);
			
			g.drawString(name, rect.getMinX()+(rect.getWidth()-fontWidth)/2f
					, rect.getMinY()+(rect.getHeight()-fontHeight)/2f);
			
			g.setColor(oldColor);
			g.setFont(oldFont);
			
		}
		
		@Override
		public void mousePressed(int button,int x,int y){
			
			System.out.println("Fine : mousePressed on button "+name);
			
			if (button == MOUSE_BUTTON_LEFT){
				
				if (worker != null){
					System.out.println("logger : "+logger);
					logger.info("Nacisnięto guzik o nazwie "+name+". Wykonujemy akcję.");
					worker.work();

				}
				
			}
			
		}
		
	}
	
	public static class NumberedButton extends Component{
		
		Logger logger = Logging.getLogger(NumberedButton.class.getName(),"gui");
		
		static interface Worker{
			void work(NumberedButton button);
		}
		
		static final Font numberFont = new TrueTypeFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 35),true);
		static final Font mainDescFont = new TrueTypeFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 22),true);
		static final Font secondaryDescFont = new TrueTypeFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14),true);
		
		int number;
		
		String savedDesc = "";
		String mainDesc = "";
		String secondaryDesc = "";
		
		boolean selected = false;
		
		Worker worker = null;
		
		public NumberedButton(float x,float y,float width,float height,int number){
			super(new Rectangle(x,y,width,height));
			this.number = number;

			eventHooks.put(EventType.MOUSE_PRESS, EnumSet.of(EventHookType.BLOCKS,EventHookType.GETS));
		}
		
		public NumberedButton(float x,float y,float width,float height,int number,Worker worker){
			super(new Rectangle(x,y,width,height));
			this.number = number;
			this.worker = worker;
			
			eventHooks.put(EventType.MOUSE_PRESS, EnumSet.of(EventHookType.BLOCKS,EventHookType.GETS));
			
		}
		
		public void setClickAction(Worker worker){
			this.worker = worker;
		}
		
		public void setMainDesc(String mainDesc){
			this.mainDesc = mainDesc;
		}
		
		public void setSecondaryDesc(String secondaryDesc){
			this.secondaryDesc = secondaryDesc;
		}
		
		public void setSavedDesc(String savedDesc){
			this.savedDesc = savedDesc;
		}
		
		public String getSavedDesc(){
			return savedDesc;
		}
		
		@Override
		public void render(GameContainer gc,Graphics g){
			
			Color oldColor = g.getColor();
			Font oldFont = g.getFont();
			
			int mouseX = gc.getInput().getMouseX();
			int mouseY;
			if (translate != null) mouseY = (int)(gc.getInput().getMouseY()-translate.getY());
			else mouseY = gc.getInput().getMouseY();
			boolean mouseOver = rect.contains(mouseX,mouseY);
			
			if (selected) g.setColor(new Color(150,200,220));
			else if (mouseOver) g.setColor(new Color(80,140,200));
			else g.setColor(new Color(50,50,50));
			g.setFont(numberFont);
			
			RoundedRectangle wholeButtonRect = new RoundedRectangle(rect.getMinX(),rect.getMinY()
					,rect.getWidth(),rect.getHeight(),30);
			g.draw(wholeButtonRect);
			
			RoundedRectangle numberRect = new RoundedRectangle(rect.getMinX(),rect.getMinY(),65
					,rect.getHeight(),30);
			g.draw(numberRect);
			
			if (selected) g.setColor(new Color(170,230,250));
			else if (mouseOver) g.setColor(new Color(100,160,230));
			else g.setColor(new Color(0,20,60));
			
			drawStringCentered(g,Integer.toString(number),new Rectangle(rect.getMinX(),rect.getMinY(),65
					,rect.getHeight()));
			
			if (selected) g.setColor(new Color(150,200,220));
			else if (mouseOver) g.setColor(new Color(80,140,200));
			else g.setColor(new Color(50,50,50));
			
			g.setFont(mainDescFont);
			
			g.drawString(mainDesc,rect.getMinX()+80,rect.getMinY()+5);
			
			g.setFont(secondaryDescFont);
			drawStringRightAligned(g,secondaryDesc,rect.getMaxX()-30,rect.getMaxY()*0.68f);
			
			g.setColor(oldColor);
			g.setFont(oldFont);
			
		}
		
		@Override
		public void mousePressed(int button,int x,int y){
			
			if (button == MOUSE_BUTTON_LEFT){
				
				if (worker != null){
				
					logger.info("Zostal nacisniety numerowany guzik o numerze "+number+". Wykonujemy akcję.");
					worker.work(this);

				}
				
			}
			
		}
		
		public void setSelected(boolean selected){
			this.selected = selected;
		}
		
	}
	
	public static class Component{
		
		protected Rectangle rect;
		
		protected Point translate = null;
		private Rectangle clip = null;
		
		private boolean visible = true;
		protected boolean enabled = true;
		
		enum EventType{
			MOUSE_PRESS,KEY_PRESS, MOUSE_DRAG, WHEEL_MOVE
		}
		
		enum EventHookType{
			GETS,GETS_ALWAYS,BLOCKS,BLOCKS_ALWAYS
		}
		
		public EnumMap<EventType,EnumSet<EventHookType>> eventHooks 
			= new EnumMap<EventType,EnumSet<EventHookType>>(EventType.class);
		
		private Component(Rectangle rect){
			this.rect = rect;
		}
		
		public void setTranslate(Point trans){
			translate = trans;
		}
		
		public void setClip(Rectangle clip){
			this.clip = clip;
		}
		
		public void setVisible(boolean visible){
			this.visible = visible;
		}
		
		public void setEnabled(boolean enabled){
			this.enabled = enabled;
		}

		public boolean blocksEventIfDoesntGet(){
			return true;
		}

		public boolean blocksEventIfGets(){
			return true;
		}
		
		public Rectangle getRect(){
			return rect;
		}
		
		public void mouseWheelMoved(int change){
			
		}
		
		public void mouseDragged(int oldx,int oldy, int newx, int newy){
			
		}
		
		public void mousePressed(int button, int x, int y){
			
		}
		
		public void keyPressed(int key, char c){
			
		}
		
		public void update(int timeElapsed){
			
		}
		
		public void render(GameContainer gc, Graphics g){
			
		}
		
	}
	
	public static class ComponentContainer{
		
		private LinkedList<Component> components = new LinkedList<Component>();
		private GameContainer gc;
		
		public ComponentContainer(GameContainer gc){
			System.out.println("gc : "+gc);
			this.gc = gc;
		}
		
		public void addComponent(Component comp){
			components.add(comp);
		}
		
		public void removeComponent(Component comp){
			components.remove(comp);
		}
		
		private interface Worker{
			void work(Component c);
		}
		
		//zwraca true jezeli blokuje dane zdarzenie
		private boolean broadcastEvent(int mouseX,int mouseY, EventType type, Worker worker){
			
			//FIXME BLOCKS_ALWAYS nie blokuje teraz tych komponentow ktore byly sprawdzane wczesniej
			
			boolean blocked = false;
			
			System.out.println("Fine : broadcastEvent type = "+type);
			
			for (Component c : components){
				boolean done = false;
				boolean blocks = false;
				boolean wantsToBlock = false;
				boolean gets = false;
				
				System.out.println("Finest : Przetwarzamy komponent");
				
				if (c.eventHooks.get(type) != null){
					EnumSet<EventHookType> set = c.eventHooks.get(type);
					
					System.out.println("Finest : Znalezlismy enumSet");
					
					if (set.contains(EventHookType.BLOCKS_ALWAYS)){
						blocks = true;
					}
					if (set.contains(EventHookType.GETS_ALWAYS)){
						worker.work(c);
						done = true;
					}
					if (set.contains(EventHookType.BLOCKS)){
						wantsToBlock = true;
					}
					if (set.contains(EventHookType.GETS)){
						System.out.println("Finest : Gets");
						gets = true;
					}
					
				}
				
				float relativeMouseX = mouseX;
				float relativeMouseY = mouseY;
				
				if (c.translate != null){
					relativeMouseX -= c.translate.getX();
					relativeMouseY -= c.translate.getY();
				}
				
				if (!blocked && c.visible && c.getRect().contains(relativeMouseX,relativeMouseY) 
						&& (c.clip == null || c.clip.contains(relativeMouseX,relativeMouseY))){
					System.out.println("Finest : Komponent jest widoczny i myszka znajduje sie w nim.");

					if (c.enabled && gets){
					
						if (!done) worker.work(c);
						if (wantsToBlock && c.blocksEventIfGets()) blocks = true;

					} else {
					
						if (wantsToBlock && c.blocksEventIfDoesntGet()) blocks = true;
						
					}
				}
				
				if (blocks) blocked = true;
				
			}
			
			return blocked;
			
		}
		
		//zwraca true jezeli blokuje dane zdarzenie
		public boolean mouseWheelMoved(int change){
			
			Input input = gc.getInput();
			int x = input.getMouseX();
			int y = input.getMouseY();
			
			return broadcastEvent(x,y,EventType.WHEEL_MOVE,(Component c) -> c.mouseWheelMoved(change));
			
		}
		
		//zwraca true jezeli blokuje dane zdarzenie
		public boolean mouseDragged(int oldx,int oldy, int newx, int newy){
			
			return broadcastEvent(oldx,oldy,EventType.MOUSE_DRAG,(Component c) -> c.mouseDragged(oldx,oldy,newx,newy));
			
		}
		
		//zwraca true jezeli blokuje dane zdarzenie
		public boolean mousePressed(int button, int x, int y){
			
			return broadcastEvent(x,y,EventType.MOUSE_PRESS,(Component c) -> c.mousePressed(button, x, y));
			
		}
		
		//zwraca true jezeli blokuje dane zdarzenie
		public boolean keyPressed(int key, char c){
			
			Input input = gc.getInput();
			int x = input.getMouseX();
			int y = input.getMouseY();
			
			return broadcastEvent(x,y,EventType.KEY_PRESS,(Component comp) -> comp.keyPressed(key, c));
			
		}
		
		public void update(int timeElapsed){
			
			for (Component c : components){
				c.update(timeElapsed);
			}
			
		}
		
		public void render(GameContainer gc, Graphics g){
			
			Iterator<Component> it = components.descendingIterator();
			
			while (it.hasNext()){
				Component c = it.next();
				if (c.visible){
					g.pushTransform();
					Rectangle oldClip = copyRectangle(g.getClip());
					if (c.clip != null) g.setClip(c.clip);
					if (c.translate != null) g.translate(c.translate.getX(),c.translate.getY());
					
					c.render(gc, g);
					g.setClip(oldClip);
					g.popTransform();
				}
			}
			
		}
		
	}
	
	public static class TextScrollableClip extends Component{
		
		static Font logFont = null;
		
		float wheelSpeed;
		
		String plainText;
		
		ArrayList<String> text = new ArrayList<String>();
		ArrayList<Integer> heights = new ArrayList<Integer>();
		int height = 0;
		int width = 0;
		
		float textTranslate,v;
		
		public TextScrollableClip(Rectangle rect) throws SlickException{
			super(rect);
			
			if (logFont == null){
				logFont = loadUnicodeFont("arial.ttf",15);
			}
			
			textTranslate = 0;
			v = 0;
			
			eventHooks.put(EventType.WHEEL_MOVE, EnumSet.of(EventHookType.BLOCKS,EventHookType.GETS));
		
			wheelSpeed = 600;
			
		}
		
		public void setWheelSpeed(float wheelSpeed){
			this.wheelSpeed = wheelSpeed;
		}
		
		public void setText(String text){
			
			this.text.clear();
			String [] arr = text.split("\n");
			for (String line : arr){
				this.text.add(line);
			}
			plainText = text;
			
			heights.clear();
			
			height = 0;
			width = 0;
			for (int i = 0;i < arr.length;++i){
				if (arr[i].equals("")){
					heights.add(logFont.getHeight("|"));
				} else heights.add(logFont.getHeight(arr[i]));
				height += heights.get(i);
				width = Math.max(width,logFont.getWidth(arr[i]));
			}
			
		}
		
		/* NOWE */
		public void appendText(String text){
			
			if (this.text.size() == 0) setText(text);
			else {
			
				int currentPos = this.text.size();
				
				String [] arr = text.split("\n");
				for (String line : arr){
					this.text.add(line);
				}
				
				plainText += "\n"+text;
	
				int height = 0;
				int width = 0;
	
				for (int i = currentPos;i < this.text.size();++i){
					if (this.text.get(i).equals("")){
						heights.add(logFont.getHeight("|"));
					} else heights.add(logFont.getHeight(this.text.get(i)));
					height += heights.get(i);
					width = Math.max(width,logFont.getWidth(this.text.get(i)));
				}
	
				this.height += height;
				this.width = Math.max(this.width,width);
				
			}

			
		}
		/* ... */
		
		public String getText(){
			return plainText; 
		}
		
		@Override
		public void update(int timeElapsed){
			
			//Zmiana polozenia ze wzgledu na predkosc
			textTranslate += v*timeElapsed/1000f;
			System.out.println("textTranslate : "+textTranslate);
			
			//Nakładanie ograniczeń na położenie
			textTranslate = Math.max(rect.getHeight()-height,textTranslate);
			textTranslate = Math.min(0,textTranslate);
			
			//Przychamowanie prędkości
			v = Math.signum(v)*Math.max(0,(Math.abs(v)-(1300)*timeElapsed/1000f));
			
		}
		
		@Override
		public void render(GameContainer gc, Graphics g){
			
			if (text.size() > 0){
			
				float mouseX = gc.getInput().getMouseX();
				float mouseY = gc.getInput().getMouseY();
				
				Font oldFont = g.getFont();
				Rectangle oldClip = Window.copyRectangle(g.getClip());
				g.setClip(rect);
				g.setFont(logFont);
				
				if (getRealRect().contains(mouseX,mouseY)){
					Color oldColor = g.getColor();
					g.setColor(new Color(100,150,200,20));
					g.fill(getRealRect());
					g.setColor(oldColor);
				}
				g.pushTransform();
				g.translate(0,textTranslate);
				
				float pos = textTranslate+rect.getMinY();
				int textPos = 0;
				int from = 0;
				while (pos < rect.getMinY() && textPos < text.size()){
					pos += heights.get(textPos++);
				}
				from = Math.max(0, textPos-1);
				while (pos <= rect.getMaxY() && textPos < text.size()){
					pos += heights.get(textPos++);
				}
				int to = textPos;
				
				drawStringMultiline(g, text, heights,from,to, (int)rect.getMinX(), (int)rect.getMinY());
				g.setFont(oldFont);
				g.setClip(oldClip);
				g.popTransform();
			
			}
		}
		
		@Override
		public void mouseWheelMoved(int change){

			v = Math.signum(change)*(wheelSpeed);
			
		}
		
		@Override
		public Rectangle getRect(){
			return getRealRect();
		}
		
		public Rectangle getRealRect(){
			return new Rectangle(rect.getMinX(),rect.getMinY(),Math.min(width, rect.getWidth()),Math.min(height, rect.getHeight()));
		}
		
	}

	/*TODO  Przy kazdym polu napisać, które funkcje są odpowiedzialne za ustawianie tych pól */
	public static class ScrollableLogClip extends Component{
		
		//TODO Zrobić funkcję appendMessage, która będzie dodawać wiele wiadomości
		//TODO Zrobić tak, żeby funkcja setLog w przypadku duzej ilosc danych uruchamiala nowy watek
		//TODO a w trakcie dzialania watku clip bedzie wyswietlał napis "Loading ...", appendMessage bedzie dodawało
		//TODO wiadomosci do kolejki watku, a setLog przerywało wątek
		//TODO Funkcja appendMessage tez powinna w przypadku dużej ilości danych uruchamiać nowy wątek, z tym że
		//TODO wtedy na gorze okna bedzie wyswietlany malym drukiem napis "Loading ..." i trzeba to jakos zsynchronizowac z update
		//TODO czyli update powinno dzialac normalnie z render, a appendMessage bedzie sobie zapisywal to co przerobi
		//TODO do jakiejs swojej kolejki, a pozniej jak skonczy to laczy sie z glownym watkiem i wtedy tą swoją kolejke
		//TODO uaktualnia tak, zeby pasowala do calosci i podpina ją do glownej listy. Zeby zawsze byla gwarancja zakonczenia
		//TODO takiego watku trzeba zrobic jakis limit na ilosc wiadomosci w kolejce appendMessage. Przy przekroczeniu limitu
		//TODO bedzie zglaszany error i kolejne wiadomosci beda sie gromadzily gdzie indziej, a appendMessage skonczy
		//TODO swoje wykonanie i pozniej zabierze wiadomosci z nowej kolejki i zacznie nowy wątek

		//TODO wyswietlac pasek przewijania z prawej strony
		//TODO wyswietlac w jakiej funkcji jestesmy aktualnie
		//TODO zrobic na gorze pasek za ktory bedziemy mogli zlapac okienko i przeniesc
		//TODO bedzie mozna wybrac tam tez minimalny level logów, filtrowac logi
		//TODO oznaczanie linijki, aby mozna bylo sie do niej wrocic w kazdym momencie
		
		//TODO Zrobic mozliwosc ustawiania glebokosci stosu w przypadku filtru z jednym watkiem
		//TODO Wtedy przy dodawaniu logu automatycznie bedzie on ukrywany jak glebokosc bedzie wieksza niz ustawiona
		
		//TODO Zrobic mozliwosc ukrycia wszystkich poczatkow i koncow funkcji w przypadku filtru z wieloma watkami
		
		//TODO Przy pustych linijkach wyswietlamy tylko pusta linie, bez zadnych czasow, poziomow czy kategorii
		
		//TODO Przy funkcji powinno być napisane ile wiadomości jest ukrytych z powodu danego poziomu filtru
		
		//TODO Zwinieta funkcja blokuje tylko wiadomosci, które mają mniejszy lub równy poziom istotności

		final float ANIMATION_TIME = 500f; // w milisekundach
		final int TIME_MAX_LEVEL = 10000;

		final float TAB_WIDTH = 30;

		final boolean ANIMATION = true;
		final boolean TABS = true;
		
		class LogEntry{
			private LogMessage msg;
			private float height;
			private float width;
			private int tabs;

			//czy log po zakonczeniu animacji bedzie wyswietlany
			private boolean visible;

			//w jakim stopniu jest wyswietlany
			private float visibilityLvl;
			
			//schowane z powodu schowania funkcji w ktorej sie znajduje log
			//nawet jak przez filtr nie przechodza wiadomosci to powinny miec ustawione to pole
			private boolean hidden;

			//log jest poczatkiem funkcji, ktora jest schowana
			private boolean functionHiding;
			
			//drugi koniec funkcji, którą zaczyna albo kończy ten wpis
			private int secondFunctionEnd;

			private int lastUpdate;

			public float getHeight(){
			
				if (visibilityLvl == 0.0f) return 0.0f;
				else return 1.0f+Math.min((height-1.0f)*visibilityLvl/100.0f,height-1.0f);

			}

			//na podstawie visibilityLvl dostosowuje transparentnosc, a kolor zależy od msg.lvl
			public Color getColor(){
				
				switch (msg.level){
				case ERROR:
					return new Color(200,0,0,visibilityLvl/100.0f*255.0f);
				case FATAL:
					return new Color(150,20,20,visibilityLvl/100.0f*255.0f);
				case FINE:
					return new Color(255,255,255,visibilityLvl/100.0f*255.0f);
				case FINEST:
					return new Color(255,255,255,visibilityLvl/100.0f*255.0f);
				case INFO:
					return new Color(100,140,200,visibilityLvl/100.0f*255.0f);
				case SEVERE:
					return new Color(150,20,20,visibilityLvl/100.0f*255.0f);
				case WARNING:
					return new Color(255,255,0,visibilityLvl/100.0f*255.0f);
				default:
					return new Color(255,255,255,visibilityLvl/100.0f*255.0f);
				
				}
			
				

			}

		}
		

		interface LogMessageFilter{
		
			boolean filter(LogMessage msg);
		
		}
		
		class FunctionMarker{
			
			public int startLineNumber; //gdzie znajduje sie otwierajaca linijka funkcji
			public int endLineNumber;   //gdzie znajduje sie zamykaja linijka funkcji
			public boolean startMarker; //czy to jest znacznik na poczatku czy na koncu
			public Rectangle rect;      //obszar który zajmuje znacznik
			
		}
		
		//Czyścimy to tylko w funkcji render, jak kliknieto myszą na funkcje której nie ma to trudno
		//TODO trzeba parować ze sobą początek i koniec funkcji, żeby funckcja render nie musiała szukać drugiego
		//TODO mając jednego
		LinkedList<FunctionMarker> functionMarkers = new LinkedList<FunctionMarker>();

		Logger logger = Logging.getLogger(ScrollableLogClip.class.getName(),"gui");
		
		static Font logFont = null;
		
		LogDisplayer logDisplayer = Logging.complexLogDisplayer();

		boolean blocksEvent = false;

		float wheelSpeed;
		
		ArrayList<LogEntry> log = new ArrayList<LogEntry>();

		//tylko te które są faktycznie widoczne (visibilityLvl > 0.0f)
		ArrayList<Integer> blocksStarts = new ArrayList<Integer>();
		ArrayList<Integer> blocksEnds = new ArrayList<Integer>();

		//te które będą widoczne
		ArrayList<Integer> readyToShowBlocksStarts = new ArrayList<Integer>();
		ArrayList<Integer> readyToShowBlocksEnds = new ArrayList<Integer>();

		float translateText = 0;
		float v = 0;

		int lastUpdate = 0;

		Integer firstLine = null;
		float firstLinePosY = 0;

		LogMessageFilter actualFilter = (LogMessage msg) -> true;
		//jezeli oneThreadFilter == true to znaczy, ze mamy jeden watek i mozemy rozdzielac poszczegolne funkcje
		boolean oneThreadFilter = false;

		//numery linijek, ktore zaczynaja funkcje bedace w stosie funkcji aktualnie
		Stack<Integer> stackTrace = new Stack<Integer>();
		
		@SuppressWarnings("unchecked")
		public ScrollableLogClip(Rectangle rect) throws SlickException{
			super(rect);
			
			if (logFont == null){
				logFont = loadUnicodeFont("arial.ttf", 15);
			}
			
			eventHooks.put(EventType.WHEEL_MOVE, EnumSet.of(EventHookType.BLOCKS,EventHookType.GETS));
			eventHooks.put(EventType.MOUSE_PRESS, EnumSet.of(EventHookType.BLOCKS,EventHookType.GETS));
			
			wheelSpeed = 600;

		}
		
		public void setWheelSpeed(float wheelSpeed){
			this.wheelSpeed = wheelSpeed;
		}

		//Złozoność O(liczba_znaków)
		public void setLog(Iterable<LogMessage> collection){
		
			log.clear();
			blocksStarts.clear();
			blocksEnds.clear();
			translateText = 0;

			wheelSpeed = 0.0f;
			firstLinePosY = 0.0f;

			boolean previousVisible = false;
			firstLine = null;
			lastUpdate = 0;

			int currentTabs = 0;
			int currentLine = 0;

			stackTrace.clear();

			for (LogMessage msg : collection){
			
				LogEntry newLogEntry = new LogEntry();
				if (msg.msg.equals("")){
					newLogEntry.height = logFont.getHeight("|");
				} else newLogEntry.height = logFont.getHeight(msg.msg);
				newLogEntry.width = logFont.getWidth(msg.msg);
				newLogEntry.visible = actualFilter.filter(msg);
				newLogEntry.visibilityLvl = 0.0f;
				newLogEntry.lastUpdate = 0;
				newLogEntry.msg = msg;

				if (oneThreadFilter){

					if (msg.category.equalsIgnoreCase("Stack Trace")){
						if (msg.msg.startsWith("~")){
							if (stackTrace.size() > 0){
								if (msg.msg.substring(1).equalsIgnoreCase(log.get(stackTrace.peek()).msg.msg)){
									stackTrace.pop();
								} else {
									logger.warning("Dostalismy do logu tylko koniec funkcji "+msg.msg.substring(1));
								}
							} else {
								logger.warning("Dostalismy do logu tylko koniec funkcji "+msg.msg.substring(1));
							}
							--currentTabs;
							newLogEntry.tabs = currentTabs;
						} else {
							stackTrace.push(currentLine);
							newLogEntry.tabs = currentTabs;
							++currentTabs;
						}
					} else newLogEntry.tabs = currentTabs;

				}

				newLogEntry.hidden = false;
				newLogEntry.functionHiding = false;

				log.add(newLogEntry);

				if (newLogEntry.visible){
					if (!previousVisible){
						readyToShowBlocksStarts.add(currentLine);
					}
				} else {
					if (previousVisible){
						readyToShowBlocksEnds.add(currentLine);
					}
				}

				previousVisible = newLogEntry.visible;

				++currentLine;
			
			}

			if (previousVisible){
				readyToShowBlocksEnds.add(currentLine-1);
			}

			if (readyToShowBlocksStarts.size() != readyToShowBlocksEnds.size()){
				logger.error("Pod koniec funkcji setLog nie jest spełniona równość"
					+ " readyToShowBlocksStarts.size() == readyToShowBlocksEnds.size()");
			}

		}
		
		public void setLogDisplayer(LogDisplayer logDisplayer){
			this.logDisplayer = logDisplayer;
		}
		
		public void appendMessage(LogMessage msg){

			LogEntry newLogEntry = new LogEntry();

			newLogEntry.msg = msg;

			if (msg.msg.equals("")){
				newLogEntry.height = logFont.getHeight("|");
			} else newLogEntry.height = logFont.getHeight(msg.msg);
			newLogEntry.width = logFont.getWidth(msg.msg);

			newLogEntry.functionHiding = false;


			boolean afterPrevious = true;
			//Ustawiamy tabs,hidden,stackTrace
			if (oneThreadFilter){

				if (msg.category.equalsIgnoreCase("Stack Trace")){
					if (msg.msg.startsWith("~")){
						if (stackTrace.size() > 0){
							if (msg.msg.substring(1).equalsIgnoreCase(log.get(stackTrace.peek()).msg.msg)){
								LogEntry functionStart = log.get(stackTrace.pop());
								newLogEntry.tabs = functionStart.tabs;
								newLogEntry.hidden = functionStart.hidden;
								afterPrevious = false;
							} else {
								logger.warning("Dostalismy do logu tylko koniec funkcji "+msg.msg.substring(1));
							}
						} else {
							logger.warning("Dostalismy do logu tylko koniec funkcji "+msg.msg.substring(1));
						}
					} else {
						LogEntry functionStart = log.get(stackTrace.peek());
						newLogEntry.tabs = functionStart.tabs+1;
						newLogEntry.hidden = (functionStart.hidden || functionStart.functionHiding);
						afterPrevious = false;
						stackTrace.push(log.size());
					}
				} else if (stackTrace.size() > 0){
						LogEntry functionStart = log.get(stackTrace.peek());
						newLogEntry.tabs = functionStart.tabs+1;
						newLogEntry.hidden = (functionStart.hidden || functionStart.functionHiding);
						afterPrevious = false;
				}

			}

			if (afterPrevious){
				if (log.size() <= 0){
					newLogEntry.tabs = 0;
					newLogEntry.hidden = false;
				} else {
					LogEntry previousEntry = log.get(log.size()-1);
					newLogEntry.tabs = previousEntry.tabs;
					newLogEntry.hidden = previousEntry.hidden;
				}
			}

			newLogEntry.visible = actualFilter.filter(msg) && !newLogEntry.hidden;
			newLogEntry.lastUpdate = lastUpdate;

			//Jak jest widoczny, to najpierw musi byc animacja
			newLogEntry.visibilityLvl = 0.0f;

			if (newLogEntry.visible){
				if (log.size() == 0 || !log.get(log.size()-1).visible || log.get(log.size()-1).visibilityLvl > 0.0f){
					readyToShowBlocksStarts.add(log.size());
					readyToShowBlocksEnds.add(log.size());
				} else {
					readyToShowBlocksEnds.set(readyToShowBlocksEnds.size()-1,log.size());
				}
			}

			log.add(newLogEntry);
			
		}

		public void filter(LogMessageFilter filter,boolean oneThreadFilter){
			
			this.oneThreadFilter = oneThreadFilter;
			actualFilter = filter;
			
			/* Update sobie poradzi jak firstLine będzie ustawiony na linijkę która jest niewidoczna */
			/* Tak samo render */
			
			// Więc firstLine zostawiamy na tej linijce na której jest teraz, i pozycje tak samo */
			/* Tylko wyszukiwanie bloków nie wyszukuje tych bloków co są na granicach, więc trzeba
			 * sprawdzić jak firstLine nie będzie widoczny, to ustawić firstLine na pierwszy element
			 * pierwszego bloku widocznego lub bloku który będzie miał się pojawić
			 */
			
			/* Wobec tego filter ma za zadanie tylko przejść po wszystkich elementach, sprawdzić ich widoczność
			 * i ustawić visibility, visibilityLvlg, bloki widocznosci , readyToShow
			 * hidden powinno być już ustawione w setLog, appendLog i innych, functionHiding tez
			 */
			
			/*
			
			blocksStarts.clear();
			blocksEnds.clear();
			translateText = 0;

			wheelSpeed = 0.0f;
			firstLinePosY = 0.0f;

			boolean previousVisible = false;
			firstLine = null;
			lastUpdate = 0;

			int currentTabs = 0;
			int currentLine = 0;

			stackTrace.clear();

			for (LogMessage msg : collection){
			
				LogEntry newLogEntry = new LogEntry();
				if (msg.msg.equals("")){
					newLogEntry.height = logFont.getHeight("|");
				} else newLogEntry.height = logFont.getHeight(msg.msg);
				newLogEntry.width = logFont.getWidth(msg.msg);
				newLogEntry.visible = actualFilter.filter(msg);
				newLogEntry.visibilityLvl = 0.0f;
				newLogEntry.lastUpdate = 0;
				newLogEntry.msg = msg;

				if (oneThreadFilter){

					if (msg.category.equalsIgnoreCase("Stack Trace")){
						if (msg.msg.startsWith("~")){
							if (stackTrace.size() > 0){
								if (msg.msg.substring(1).equalsIgnoreCase(log.get(stackTrace.peek()).msg.msg)){
									stackTrace.pop();
								} else {
									logger.warning("Dostalismy do logu tylko koniec funkcji "+msg.msg.substring(1));
								}
							} else {
								logger.warning("Dostalismy do logu tylko koniec funkcji "+msg.msg.substring(1));
							}
							--currentTabs;
							newLogEntry.tabs = currentTabs;
						} else {
							stackTrace.push(currentLine);
							newLogEntry.tabs = currentTabs;
							++currentTabs;
						}
					} else newLogEntry.tabs = currentTabs;

				}

				newLogEntry.hidden = false;
				newLogEntry.functionHiding = false;

				log.add(newLogEntry);

				if (newLogEntry.visible){
					if (!previousVisible){
						readyToShowBlocksStarts.add(currentLine);
					}
				} else {
					if (previousVisible){
						readyToShowBlocksEnds.add(currentLine);
					}
				}

				previousVisible = newLogEntry.visible;

				++currentLine;
			
			}

			if (previousVisible){
				readyToShowBlocksEnds.add(currentLine-1);
			}

			if (readyToShowBlocksStarts.size() != readyToShowBlocksEnds.size()){
				logger.error("Pod koniec funkcji setLog nie jest spełniona równość"
					+ " readyToShowBlocksStarts.size() == readyToShowBlocksEnds.size()");
			}
			
			*/

		}

		/* Sprawdzić najpierw czy to działa, a jak nie będzie działać to zmienić tak jak jest w tym FIXME */
		/* I wtedy sprawdzić mniej więcej co jest źle, chyba że nie będzie to takie łatwe - w pierwszych logach nie będzie */
		/* Jak będzie działać to też zmienić */
		@Override
		public void update(int timeElapsed){
			
			class LinePos{
				
				public LinePos(int lineNumber,float linePos){
					this.lineNumber = lineNumber;
					this.linePos = linePos;
				}
				
				public int lineNumber;
				public float linePos;
			}
			
			if (timeElapsed <= 0) return;

			lastUpdate = (lastUpdate+timeElapsed)%TIME_MAX_LEVEL;

			//FIXME usunąć to przypisanie
			float beginTranslate = translateText;
			
			//FIXME translateText powinno być zmienną lokalną
			//Zmiana polozenia ze wzgledu na predkosc
			translateText += v*timeElapsed/1000f;
			
			//Nak�adanie ograniczeń na położenie
			//FIXME usunac to ograniczenie. translateText moze być ujemne jak i dodatnie
			translateText = Math.min(0,translateText);
			
			//Przychamowanie prędkości
			v = Math.signum(v)*Math.max(0,(Math.abs(v)-(1300)*timeElapsed/1000f));
			
			logger.fine("translateText mamy na poczatku rowne : "+translateText);

			if (firstLine == null){

				//Sprawdzamy poprawność danych
				//Jezeli nie ma pierwszej linijki na ekranie to znaczy ze nie ma zadnego bloku widocznego
				if (blocksStarts.size() != 0 || blocksEnds.size() != 0){
					logger.error("Są jakieś widoczne bloki, a nie ma spe�nionego warunku blocksStarts.size() == 0 || blocksEnds.size() == 0");
				}

				if (readyToShowBlocksStarts.size() == 0){
					return;
				} else {
					firstLine = readyToShowBlocksStarts.get(0);
					firstLinePosY = 0;
				}
			}

			//FIXME usunąć to, nie będzie żadnej delty, samo translateText to delta
			float delta = translateText-beginTranslate;

			Integer actualVisibleBlock = null;
			Integer actualReadyToShowBlock = null;
			float actualPos = firstLinePosY+translateText;
			int actualLine = firstLine;

			//FIXME tutaj zrobić translateText zamiast delta
			//Sprawdzamy poprawność danych
			if (delta < 0.0f){ //Idziemy do przodu
				
				//Jezeli Tekst poleciał do góry to actualPos powinien być <= 0
				if (actualPos > 0.0f){
					logger.error("Przewinieto tekst do góry, a linijka która była pierwszą do rysowania jest ci�gle na dodatniej pozycji : "+actualPos);
					return;
				}

			} else if (delta != 0.0f){
				
				//Jezeli tekst poleciał w dół to actualPos powinien być >= 0
				if (actualPos < 0.0f){
					logger.error("Przewinięto tekst w dół, a linijka która była pierwszą do rysowania jest na ujemnej pozycji : "+actualPos);
					return;
				}

			}
			
			LinkedList<LinePos> positions = new LinkedList<LinePos>();

			float maxPos = rect.getHeight();
			Integer newFirstLine = null;
			float newFirstLinePos = 0;
			boolean crossedZero = false;
			
			logger.finest("maksymalna pozycja : "+maxPos);

			/* Idziemy do ostatniej widocznej na ekranie linijki */
			while (actualPos <= maxPos && actualLine < log.size()){
				
				LogEntry actualEntry = log.get(actualLine);

				//Przegladamy wszystkie logi znajdujace sie w aktualnym widocznym bloku
				while (actualEntry.visibilityLvl > 0.0f && actualLine < log.size() && actualPos <= maxPos){
						

					logger.finest(" Przechodzimy przez linijke nr "+actualLine+" (widoczn�) - actualPos : "+actualPos);
					if (!crossedZero && actualPos > 0.0f){
						//Znalezlismy pierwszy na pozycji dodatniej
						logger.finest("Znalezlismy pierwsza linijke na pozycji dodatniej.");
						crossedZero = true;
						if (positions.size() == 0){
							logger.finest("Idąc w tą stronę nie znajdziemy pierwszej linijki.");
						} else {
							newFirstLine = positions.get(positions.size()-1).lineNumber;
							newFirstLinePos = positions.get(positions.size()-1).linePos;
							logger.finest("Ostatnia linijka powinna być pierwszą, jeżeli mamy dobre przesunięcie  : "+newFirstLine);
						}
					}
					
					/* skoro przechodzimy po nich to trzeba od razu zupdatowa� ich widoczno�� */
					int timeChange = lastUpdate-actualEntry.lastUpdate;
					actualEntry.lastUpdate = lastUpdate;
					if (timeChange < 0) timeChange += TIME_MAX_LEVEL;

					if (actualEntry.visible) actualEntry.visibilityLvl = Math.min(actualEntry.visibilityLvl + timeChange/ANIMATION_TIME*100f,100f);
					else actualEntry.visibilityLvl = Math.max(0, actualEntry.visibilityLvl - timeChange/ANIMATION_TIME*100f);

					float lineHeight = actualEntry.getHeight();

					positions.add(new LinePos(actualLine,actualPos));
					
					++actualLine;
					actualPos += lineHeight;

					//TODO trzeba tu jeszcze uaktualni� bloki

					if (actualLine < log.size()) actualEntry = log.get(actualLine);

				}

				//Przegl�damy wszystkie logi znajduj�ce si� w aktualnym bloku gotowym do pokazania si�
				while (actualEntry.visibilityLvl == 0.0f && actualLine < log.size() && actualPos <= maxPos){
					
					logger.finest(" Przechodzimy przez linijke nr "+actualLine+" (readyToShow) - actualPos : "+actualPos);
					if (!crossedZero && actualPos > 0.0f){
						//Znalezlismy pierwszy na pozycji dodatniej
						logger.finest("Znalezlismy pierwsza linijke na pozycji dodatniej.");
						crossedZero = true;
						if (positions.size() == 0){
							logger.finest("Idąc w tą stronę nie znajdziemy pierwszej linijki.");
						} else {
							newFirstLine = positions.get(positions.size()-1).lineNumber;
							newFirstLinePos = positions.get(positions.size()-1).linePos;
							logger.finest("Ostatnia linijka powinna być pierwszą, jeżeli mamy dobre przesunięcie  : "+newFirstLine);
						}
					}
					
					/* skoro przechodzimy po nich to trzeba od razu zupdatowa� ich widoczno�� */
					int timeChange = lastUpdate-actualEntry.lastUpdate;
					actualEntry.lastUpdate = lastUpdate;
					if (timeChange < 0) timeChange += TIME_MAX_LEVEL;

					if (actualEntry.visible) actualEntry.visibilityLvl = Math.min(actualEntry.visibilityLvl + timeChange/ANIMATION_TIME*100f,100f);
					else actualEntry.visibilityLvl = Math.max(0, actualEntry.visibilityLvl - timeChange/ANIMATION_TIME*100f);

					float lineHeight = actualEntry.getHeight();
					
					positions.add(new LinePos(actualLine,actualPos));

					++actualLine;
					actualPos += lineHeight;

					//TODO trzeba tu jeszcze uaktualni� bloki

					if (actualLine < log.size()) actualEntry = log.get(actualLine);

				}

				//Jak doszlismy do niewidocznego bloku to trzeba znalezc kolejny widoczny lub gotowy do pokazania si� blok
				if (actualLine < log.size() && actualPos <= maxPos){
						
					actualVisibleBlock = findFirstBlockAfter(actualLine);
					actualReadyToShowBlock = findFirstReadyToShowBlockAfter(actualLine);

					if (actualVisibleBlock != null && actualReadyToShowBlock != null){
						actualLine = Math.min(blocksStarts.get(actualVisibleBlock),readyToShowBlocksStarts.get(actualReadyToShowBlock));
					} else if (actualVisibleBlock != null){
						actualLine = blocksStarts.get(actualVisibleBlock);
					} else if (actualReadyToShowBlock != null){
						actualLine = readyToShowBlocksStarts.get(actualReadyToShowBlock);
					} else actualLine = log.size();

				}

			}

			//Najwyższa pozycja do jakiej doszliśmy
			float endPos = actualPos;
			logger.finest("Najwyższa pozycja do jakiej doszliśmy : "+endPos);
			
			//FIXME Tu powinniśmy zaktualizować pozycje
			
			//FIXME Mamy opcje : minPos >= newFirstLinePos i newFirstLine != null -> znaczy wszystko jest dobrze
			//FIXME i przesunięcie tekstu było poprawne (ustawiamy firstLine na newFirstLine)
			//FIXME 2. newFirstLine == null -> nie znalezlismy pierwszej linijki, czyli rolka przejechalismy z tekstem
			//FIXME na dodatnią pozycje i musimy się wrócić
			//FIXME 3. newFirstLine != null, ale minPos < newFirstLinePos -> czyli i tak musimy się wrócić, przejechaliśmy
			//FIXME z tekstem do tyłu ale teraz mamy za mało tekstu z przodu
			//FIXME W obu przypadkach !! jeżeli endPos < rect.getHeight() !! musimy zaktualizować pozycje 
			//FIXME -> endPos musi miec wartość -> rect.getHeight()
			//FIXME czyli rect.getHeight()-endPos to jest wartość o którą musimy zmienić wszystkie pozycje
			
			//FIXME mamy newFirstLinePos, positions[...], actualPos i firstLinePosY
			//FIXME positions jest nam do niczego nie potrzebne,newFirstLinePos jest potrzebne tylko do pierwszej czesci
			//FIXME actualPos już też nie jest potrzebne, bo i tak firstLinePosY jest większe od 0, a od firstLine
			//FIXME w góre wszystko sprawdziliśmy, więc aktualizujemy tylko firstLinePosY i idziemy do tyłu
			//FIXME actualPos będzie równy na początku firstLinePosY
			
			//FIXME Jak zaktualizujemy pozycje, będziemy mogli iść do tyłu i dostaniemy jedno z dwóch
			//FIXME 1. Przejdziemy przez zero -> dolna granica tekstu jest dobrze ustawiona, więc można ustawić
			//FIXME pierwszą linijkę przed zerem jako pierwszą linijkę do rysowania
			//FIXME 2. Nie przejdziemy przez zero -> znaczy, że tekst jest za bardzo przesunięty w dół, trzeba
			//FIXME zaktualizować po raz ostatni pozycje -> będziemy mieli aktualną pozycję w actualPos,
			//FIXME musimy każdą pozycję zmienić o -actualPos (znaczy w sumie nie musimy, bo mamy już pierwszą linijkę
			//FIXME - będzie ona w lastLine i wpisujemy lastLine w firstLine, a firstLinePosY ustawiamy na 0
			//FIXME Jak lastLine będzie nullem
			

			
			//Najnizsza pozycja do jakiej musimy dojsc
			float minPos = endPos-rect.getHeight();
			
			//Trzeba wprowadzić ograniczenia na translateText, tak aby nie było możliwe przewinięcie tekstu za bardzo do góry
			if (minPos < 0.0f) translateText -= minPos;
			translateText = Math.min(0,translateText);
			
			if (minPos < 0.0f) logger.finest("Ograniczylismy translateText do : "+translateText);
			
			actualVisibleBlock = null;
			actualReadyToShowBlock = null;
			
			if (newFirstLine != null){
				logger.finest("aktualna pozycje sciagamy z wybranej linijki.");
				actualPos = newFirstLinePos;
				actualLine = newFirstLine-1;
			} else {
				logger.finest("Szukamy wczesniejszej linijki przed tą od ktorej zaczeliśmy.");
				actualLine = firstLine-1;
				actualPos = firstLinePosY+translateText;
			}
				
			Integer lastLine = null;
			float lastLinePos = 0;

			logger.fine("Idziemy w górę");
			logger.fine("Zaczynamy od pozycji : "+actualPos);
			logger.fine("minPos : "+minPos);
			/*Idziemy w g�r� do pierwszej linijki, kt�ra powinna si� wy�wietli� na ekranie*/
			while (actualLine < log.size() && actualLine >= 0 && (actualPos > 0.0f || actualPos > minPos)){
			
				LogEntry actualEntry = log.get(actualLine);

				//Przegladamy wszystkie logi znajdujace sie w aktualnym widocznym bloku
				while (actualEntry.visibilityLvl > 0.0f && actualLine >= 0 && (actualPos > 0.0f || actualPos > minPos)){
						
					logger.finest(" Przechodzimy przez linijke nr "+actualLine+" (widoczną) - actualPos : "+actualPos);
					
					/* skoro przechodzimy po nich to trzeba od razu zupdatowa� ich widoczno�� */
					int timeChange = lastUpdate-actualEntry.lastUpdate;
					actualEntry.lastUpdate = lastUpdate;
					if (timeChange < 0) timeChange += TIME_MAX_LEVEL;

					if (actualEntry.visible) actualEntry.visibilityLvl = Math.min(actualEntry.visibilityLvl + timeChange/ANIMATION_TIME*100f,100f);
					else actualEntry.visibilityLvl = Math.max(0, actualEntry.visibilityLvl - timeChange/ANIMATION_TIME*100f);

					float lineHeight = actualEntry.getHeight();
					
					lastLine = actualLine;
					lastLinePos = actualPos;

					--actualLine;
					actualPos -= lineHeight;

					//TODO trzeba tu jeszcze uaktualni� bloki

					if (actualLine >= 0) actualEntry = log.get(actualLine);

				}

				//Przegl�damy wszystkie logi znajduj�ce si� w aktualnym bloku gotowym do pokazania si�
				while (actualEntry.visibilityLvl == 0.0f && actualLine >= 0 && (actualPos > 0.0f || actualPos > minPos)){

					logger.finest(" Przechodzimy przez linijke nr "+actualLine+" (readyToShow) - actualPos : "+actualPos);
					
					int timeChange = lastUpdate-actualEntry.lastUpdate;
					actualEntry.lastUpdate = lastUpdate;
					if (timeChange < 0) timeChange += TIME_MAX_LEVEL;
					
					if (actualEntry.visible) actualEntry.visibilityLvl = Math.min(actualEntry.visibilityLvl + timeChange/ANIMATION_TIME*100f,100f);
					else actualEntry.visibilityLvl = Math.max(0, actualEntry.visibilityLvl - timeChange/ANIMATION_TIME*100f);

					float lineHeight = actualEntry.getHeight();

					lastLine = actualLine;
					lastLinePos = actualPos;
					
					--actualLine;
					actualPos -= lineHeight;

					//TODO trzeba tu jeszcze uaktualni� bloki

					if (actualLine >= 0) actualEntry = log.get(actualLine);

				}

				//Jak doszlismy do niewidocznego bloku to trzeba znalezc kolejny widoczny lub gotowy do pokazania si� blok
				if (actualLine >= 0 && (actualPos > 0.0f || actualPos > minPos)){
						
					actualVisibleBlock = findLastBlockBefore(actualLine);
					actualReadyToShowBlock = findLastReadyToShowBlockBefore(actualLine);

					if (actualVisibleBlock != null && actualReadyToShowBlock != null){
						actualLine = Math.max(blocksStarts.get(actualVisibleBlock),readyToShowBlocksStarts.get(actualReadyToShowBlock));
					} else if (actualVisibleBlock != null){
						actualLine = blocksStarts.get(actualVisibleBlock);
					} else if (actualReadyToShowBlock != null){
						actualLine = readyToShowBlocksStarts.get(actualReadyToShowBlock);
					} else actualLine = -1;

				}

			}
			
			logger.finest("actualPos : "+actualPos);

			if (lastLine != null){
				logger.finest("Ustawiamy jako pierwsza linijke na ktora sie natknelismy idac w gore.");
				logger.finest("Ustawiamy newFirstLine na "+lastLine+" (y = "+lastLinePos+")");
				newFirstLine = lastLine;
				newFirstLinePos = lastLinePos;
			}
			
			if (newFirstLine != null){
				firstLine = newFirstLine;
				firstLinePosY = newFirstLinePos-translateText;
				logger.finest("Ustawiamy firstLine na : "+firstLine+" (y = "+firstLinePosY+")");
			} else {
				logger.finest("Trzeba znalezc pierwsza linijke. Idziemy po positions.");
				float translateDiff;
				if (minPos < 0.0f){
					logger.finest("Zmienilismy translateText o "+minPos);
					translateDiff = -minPos;
				} else {
					logger.finest("Nie zmienialismy translateText.");
					translateDiff = -minPos;
				}
				
				boolean found = false;
				for (int i = 0;i < positions.size() && !found;++i){
					if (positions.get(i).linePos+translateDiff > 0.0f){
						logger.finest("W linijce nr "+positions.get(i).lineNumber+ " przekroczylismy 0.");
						if (i == 0){
							logger.error("Pierwsza linijka zaczyna sie od pozycji wiekszej od zera.");
							firstLine = positions.get(i).lineNumber;
							firstLinePosY = positions.get(i).linePos+translateDiff-translateText;
						} else {
							firstLine = positions.get(i-1).lineNumber;
							firstLinePosY = positions.get(i-1).linePos+translateDiff-translateText;
							logger.fine("Ustawiamy poprzedni� linijk� jako pierwsz�.");
							logger.fine("firstLine : "+firstLine+", firstLinePos : "+firstLinePosY);
						}
						found = true;
					}
				}
				if (!found){
					if (lastLine == null){
						logger.fine("Nie znalezlismy pierwszej linijki. Ustawiamy j� na null.");
						firstLine = null;
					} else {
						logger.fine("Nie udalo sie przeszukanie wiec pewnie mamy tylko jedna linijke. T� co odwiedzilismy.");
						logger.fine("Ustawiamy j� jako pierwsz�");
						firstLine = lastLine;
						firstLinePosY = lastLinePos+translateDiff-translateText;
					}
				}
				
			}
			
			logger.finest("translateText : "+translateText);
		}
		
		@Override
		public void render(GameContainer gc, Graphics g){

			if (firstLine == null) return;

			Font oldFont = g.getFont();
			Color oldColor = g.getColor();
			Rectangle oldClip = Window.copyRectangle(g.getClip());
			g.setClip(rect);
			
			g.setFont(logFont);

			//float mouseX = gc.getInput().getMouseX()-translate.getX();
			//float mouseY = gc.getInput().getMouseY()-translate.getY();

			Integer actualBlock = null; //nie trzeba liczyc na poczatku, skoro moze nie biedziemy tego potrzebować
			float actualPos = firstLinePosY+translateText+rect.getMinY();
			int actualLine = firstLine;
			
			logger.fine("zaczynamy rysowac. Jestesmy na pozycji : "+actualPos);
			logger.fine("firstLinePosY : "+firstLinePosY);
			logger.fine("translateText : "+translateText);
			logger.fine("rect.getMinY() : "+rect.getMinY());
			logger.fine("firstLine : "+firstLine);

			while (actualLine < log.size() && actualPos < rect.getMaxY()){
			
				LogEntry actualEntry = log.get(actualLine);

				while (actualEntry.visibilityLvl > 0.0f && actualLine < log.size() && actualPos < rect.getMaxY()){
				
					float lineHeight = actualEntry.getHeight();

					g.setColor(actualEntry.getColor());
					g.pushTransform();
					g.translate(rect.getMinX()+((TABS)?TAB_WIDTH*actualEntry.tabs:0), actualPos);
					g.scale(1.0f,actualEntry.getHeight()/actualEntry.height);

    				g.drawString(logDisplayer.display(actualEntry.msg),0,0);
    				logger.fine("Rysujemy linijke na pozycji : "+actualPos);

    				g.popTransform();
    				
    				if (oneThreadFilter){
    					if (actualEntry.msg.category.equals(LogMessage.CATEGORY_STACK_TRACE)){
    						//TODO Mozna dodawa� te pola do jakiejs listy, zeby mozna bylo je sprawdzic przy kliknieciu
    						if (actualEntry.msg.msg.startsWith("~")){
    							//Zakonczenie funkcji
    							g.setColor(new Color(50,50,220,255));
    							g.drawString("(fold)", rect.getMinX()+((TABS)?TAB_WIDTH*actualEntry.tabs:0)+actualEntry.width+10, actualPos);
    							g.setColor(new Color(50,50,50,255));
    							FunctionMarker newMarker = new FunctionMarker();
    							newMarker.rect = new Rectangle(rect.getMinX()+((TABS)?TAB_WIDTH*actualEntry.tabs:0)+actualEntry.width+10
    									, actualPos,logFont.getWidth("(fold)"),logFont.getHeight("(fold)"));
    							g.fill(newMarker.rect);
    						} else {
    							//Poczatek funkcji
    							g.setColor(new Color(50,50,220,255));
    							g.drawString("(fold)", rect.getMinX()+((TABS)?TAB_WIDTH*actualEntry.tabs:0)+actualEntry.width+10, actualPos);
    							g.setColor(new Color(50,50,50,255));
    							FunctionMarker newMarker = new FunctionMarker();
    							newMarker.rect = new Rectangle(rect.getMinX()+((TABS)?TAB_WIDTH*actualEntry.tabs:0)+actualEntry.width+10
    									, actualPos,logFont.getWidth("(fold)"),logFont.getHeight("(fold)"));
    							g.fill(newMarker.rect);
    						}
    					}
    				}

					

					++actualLine;
					actualPos += lineHeight;

					if (actualLine < log.size()) actualEntry = log.get(actualLine);

				}

				//Jezeli doszlismy do niewidocznego bloku to trzeba znalezc kolejny widoczny blok i ustawic numer linijki na jego poczatek
				if (actualLine < log.size() && actualPos < rect.getMaxY()){
				
					if (actualBlock == null) actualBlock = findFirstBlockAfter(actualLine);
					else ++actualBlock;

				} 

				if (actualBlock != null && actualBlock < blocksStarts.size()) actualLine = blocksStarts.get(actualBlock);
				else actualLine = log.size();
			
			}
			g.setFont(oldFont);
			g.setClip(oldClip);
			g.setColor(oldColor);

			/*
			if (text != null){
				
				if (getRealRect().contains(mouseX,mouseY)){
					g.setColor(new Color(100,150,200,20));
					g.fill(getRealRect());
				}
			
			}*/
		}
		
		@Override
		public void mouseWheelMoved(int change){

			v = Math.signum(change)*(wheelSpeed);
			blocksEvent = true;
			
		}

		@Override
		public void mousePressed(int button,int x,int y){
		
			blocksEvent = false;
		
		}

		@Override
		public boolean blocksEventIfDoesntGet(){
			return false;
		}

		@Override
		public boolean blocksEventIfGets(){
			return blocksEvent;
		}
		
		@Override
		public Rectangle getRect(){
			return getRealRect();
		}

		//Wyszukuje nastepny blok po danej linijce. Omija pierwszy blok
		private Integer findFirstBlockAfter(int lineNumber){
		
			if (lineNumber >= log.size()-1 || blocksStarts.size() == 0) return null;

			int begin = 1;
			int end = blocksStarts.size();
			int pos;
			boolean found = false;
			do{
				pos = (begin+end)/2;
				if (blocksStarts.get(pos) <= lineNumber){
					begin = pos+1;
				} else if (blocksStarts.get(pos-1) > lineNumber){
					end = pos;
				} else {
					found = true;
				}
			} while(begin < end && !found);
			if (!found){
				return null;
			} else return pos;

		}

		//Wyszukuje poprzedni blok przed dana linijka. Omija ostatni blok
		private Integer findLastBlockBefore(int lineNumber){
		
			if (lineNumber <= 0 || blocksEnds.size() == 0) return null;

			int begin = 0;
			int end = blocksEnds.size()-1;
			int pos;
			boolean found = false;
			do {
				pos = (begin+end)/2;
				if (blocksEnds.get(pos) >= lineNumber){
					end = pos;
				} else if (blocksEnds.get(pos+1) < lineNumber){
					begin = pos+1;
				} else found = true;
			} while (begin < end && !found);
			if (found) return pos;
			else return null;

		}

		//Wyszukuje nastepny blok po danej linijce. Omija pierwszy blok
		private Integer findFirstReadyToShowBlockAfter(int lineNumber){
	
			if (lineNumber >= log.size()-1 || readyToShowBlocksStarts.size() == 0) return null;

			int begin = 1;
			int end = readyToShowBlocksStarts.size();
			int pos;
			boolean found = false;
			do{
				pos = (begin+end)/2;
				if (readyToShowBlocksStarts.get(pos) <= lineNumber){
					begin = pos+1;
				} else if (readyToShowBlocksStarts.get(pos-1) > lineNumber){
					end = pos;
				} else {
					found = true;
				}
			} while(begin < end && !found);
			if (!found){
				return null;
			} else return pos;

		}

		//Wyszukuje poprzedni blok przed dana linijka. Omija ostatni blok
		private Integer findLastReadyToShowBlockBefore(int lineNumber){
	
			if (lineNumber <= 0 || readyToShowBlocksEnds.size() == 0) return null;

			int begin = 0;
			int end = readyToShowBlocksEnds.size()-1;
			int pos;
			boolean found = false;
			do {
				pos = (begin+end)/2;
				if (readyToShowBlocksEnds.get(pos) >= lineNumber){
					end = pos;
				} else if (readyToShowBlocksEnds.get(pos+1) < lineNumber){
					begin = pos+1;
				} else found = true;
			} while (begin < end && !found);
			if (found) return pos;
			else return null;

		}
		
		public Rectangle getRealRect(){

			if (firstLine == null) return new Rectangle(rect.getMinX(),rect.getMinY(),0,0);

			Integer actualBlock = null; //nie trzeba liczyc na poczatku, skoro moze nie biedziemy tego potrzebować
			float actualPos = firstLinePosY+translateText+rect.getMinY();
			int actualLine = firstLine;

			float width = 0;

			while (actualLine < log.size() && actualPos < rect.getMaxY()){
			
				LogEntry actualEntry = log.get(actualLine);

				while (actualEntry.visibilityLvl > 0.0f && actualLine < log.size() && actualPos < rect.getMaxY()){
				
					float lineHeight = actualEntry.getHeight();
					width = Math.max(width,actualEntry.width);

					++actualLine;
					actualPos += lineHeight;

					if (actualLine < log.size()) actualEntry = log.get(actualLine);

				}

				//przechodzimy po tych logach co są jeszcze niewidoczne, ale beda sie pojawiać
				while (actualEntry.visible && actualLine < log.size()) ++actualLine;
				
				//Jezeli doszlismy do niewidocznego bloku to trzeba znalezc kolejny widoczny blok i ustawic numer linijki na jego poczatek
				if (actualLine < log.size() && actualPos < rect.getMaxY()){
				
					if (actualBlock == null) actualBlock = findFirstBlockAfter(actualLine);
					else ++actualBlock;

				} 

				if (actualBlock != null && actualBlock < blocksStarts.size()) actualLine = blocksStarts.get(actualBlock);
				else actualLine = log.size();
			
			}

			return new Rectangle(rect.getMinX(),rect.getMinY(),width,Math.min(actualPos-rect.getMinY(),rect.getHeight()));
		}
		
	}
}
