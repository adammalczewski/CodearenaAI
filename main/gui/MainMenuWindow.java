package gui;

import game.GameType;

import messages.*;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class MainMenuWindow extends Window {
	
	static final int BUTTONS_LEFT = 110;
	static final int BUTTONS_TOP = 55;
	static final int BUTTONS_DISTANCE_Y = 90;
	
	Image golemImage;

	public MainMenuWindow(GUIListener listener,String name) {
		super(listener, name);
		this.listener = listener;
	}

	@Override
	public void init(GameContainer container) throws SlickException {
		super.init(container);
		
		golemImage = loadImage("golem.png");
		
		compContainer.addComponent(new Button(BUTTONS_LEFT,BUTTONS_TOP,205,50,"Campaign"
				,() -> listener.getMessage(new ShowWaitingRoomMessage(GameType.CAMPAIGN))));
		
		compContainer.addComponent(new Button(BUTTONS_LEFT,BUTTONS_TOP+BUTTONS_DISTANCE_Y,205,50,"Single Player"
				,() ->listener.getMessage(new ShowWaitingRoomMessage(GameType.SINGLE_PLAYER))));
		
		compContainer.addComponent(new Button(BUTTONS_LEFT,BUTTONS_TOP+BUTTONS_DISTANCE_Y*2,205,50,"Multiplayer"
				,() -> listener.getMessage(new ShowWaitingRoomMessage(GameType.MULTIPLAYER))));
		
		compContainer.addComponent(new Button(BUTTONS_LEFT,BUTTONS_TOP+BUTTONS_DISTANCE_Y*3,205,50,"Replays"
				,() -> listener.getMessage(new ShowWaitingRoomMessage(GameType.REPLAY))));
		
		compContainer.addComponent(new Button(BUTTONS_LEFT,BUTTONS_TOP+BUTTONS_DISTANCE_Y*4,205,50,"Options"
				,() -> listener.getMessage(new ShowOptionsMessage())));
		
		compContainer.addComponent(new Button(BUTTONS_LEFT,BUTTONS_TOP+BUTTONS_DISTANCE_Y*5,205,50,"Logs"
				,() -> listener.getMessage(new ShowLogsMessage())));
		
		compContainer.addComponent(new Button(BUTTONS_LEFT,BUTTONS_TOP+BUTTONS_DISTANCE_Y*6,205,50,"Exit"
				,() -> listener.getMessage(new ExitMessage())));

	}

	@Override
	public void update(GameContainer container, int delta) {
		super.update(container, delta);

	}

	@Override
	public void render(GameContainer container, Graphics g) {
		
		
		if (golemImage != null){
			g.drawImage(golemImage, 690, 105,690+golemImage.getWidth()/2f
					,105+golemImage.getHeight()/2f,0,0,golemImage.getWidth(),golemImage.getHeight());
		}
		
		super.render(container, g);

	}

	@Override
	public void mouseWheelMoved(int change) {
		super.mouseWheelMoved(change);

	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		super.mouseDragged(oldx, oldy, newx, newy);

	}

	@Override
	public void mousePressed(int button, int x, int y) {
		super.mousePressed(button, x, y);

	}

	@Override
	public void keyPressed(int key, char c) {
		super.keyPressed(key, c);
		
	}

}
