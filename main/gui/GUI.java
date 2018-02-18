package gui;

import java.util.HashMap;
import java.util.Map;

import game.Program;

import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

public class GUI extends BasicGame {
	
	public boolean initialized = false;
	
	public boolean beforeInit = true;
	
	
	public boolean exit = false;
	GameContainer gc;
	
	Program program;
	
	Map<String,Window> windows;
	String currentWindow = null;
	
	public GUI(Program program,String title) {
		super(title);
		this.program = program;
		
		windows = new HashMap<String,Window>();
		
	}
	
	@Override
	public void init(GameContainer container) throws SlickException {
		
		container.setAlwaysRender(true);
		container.setTargetFrameRate(24);
		container.setShowFPS(false);
		
		beforeInit = false;
		gc = container;
		for (Window window : windows.values()){
			window.init(gc);
		}
	}
	
	@Override
	public void update(GameContainer container, int delta)
			throws SlickException {
		
		if (exit){
			System.out.println("exit");
			container.exit();
			System.exit(0);
		}
		
		if (currentWindow != null) windows.get(currentWindow).update(container, delta);
	}

	@Override
	synchronized public void render(GameContainer container, Graphics g)
			throws SlickException {
		
		//Stworzylismy okienko
		initialized = true;
		
		if (currentWindow != null) windows.get(currentWindow).render(container, g);

	}
	
	@Override
	public void mouseWheelMoved(int change){

		if (currentWindow != null) {
			
			Window window = windows.get(currentWindow);
			
			if (!window.compContainer.mouseWheelMoved(change)){
				window.mouseWheelMoved(change);
			}
		
		}
		
	}
	
	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy){

		if (currentWindow != null) {
			
			Window window = windows.get(currentWindow);
			
			if (!window.compContainer.mouseDragged(oldx, oldy, newx, newy)){
				window.mouseDragged(oldx, oldy, newx, newy);
			}
		
		}
		
	}
	
	public void mousePressed(int button, int x, int y){

		if (currentWindow != null){
			
			Window window = windows.get(currentWindow);
			
			if (!window.compContainer.mousePressed(button, x, y)){
				window.mousePressed(button, x, y);
			}
			
		}
		
	}
	
	@Override
	public void keyPressed(int key, char c){
		
		if (currentWindow != null){
			
			Window window = windows.get(currentWindow);
			
			if (!window.compContainer.keyPressed(key, c)){
				window.keyPressed(key, c);
			}
		}
		
	}
	
	public boolean addWindow(String name,Window window){
		if (beforeInit){
			if (windows.containsKey(name)){
				return false;
			} else {
				windows.put(name,window);
				return true;
			}
		} else return false;
	}
	
	public boolean switchTo(String windowName){
		if (windows.containsKey(windowName)){
			windows.get(windowName).show();
			currentWindow = windowName;
			return true;
		} else return false;
	}
	
	public String getCurrentWindow(){
		return currentWindow;
	}

}
