package gui;

import java.util.concurrent.ConcurrentLinkedQueue;

import logs.LogMessage;
import logs.LogMessage.LogLevel;
import logs.Logging;
import messages.GoBackMessage;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

public class LogsWindow extends Window {
	
	TextScrollableClip logClip;
	ScrollableLogClip logClip2;
	
	private ConcurrentLinkedQueue<LogMessage> log = new ConcurrentLinkedQueue<LogMessage>();

	public LogsWindow(GUIListener listener,String name) {
		super(listener, name);
		this.listener = listener;
	}

	@Override
	public void init(GameContainer container) throws SlickException {
		super.init(container);
		
		compContainer.addComponent(new Button(1160,695,205,50,"Go Back"
				,() -> listener.getMessage(new GoBackMessage(name))));
		
		compContainer.addComponent(new Button(900,695,205,50,"Add Log"
				,() -> Logging.getLogger("nic").setCategory("nic"))); 
		
		final int NEW_CLIP_WIDTH = 160;
		
		logClip = new TextScrollableClip(new Rectangle(NEW_CLIP_WIDTH+50,60,1300-NEW_CLIP_WIDTH,680));
		logClip2 = new ScrollableLogClip(new Rectangle(10,60,NEW_CLIP_WIDTH,680));
		
		for (int i = 0;i < 50;++i){
			logClip2.appendMessage(new LogMessage("Przykladowy log "+i,"test",LogLevel.INFO));
		}
		
		logClip2.update(400);
		logClip2.mouseWheelMoved(-500);
		
		compContainer.addComponent(logClip);
		compContainer.addComponent(logClip2);

	}

	int counter = 0;
	
	@Override
	public void update(GameContainer container, int delta) {
		super.update(container, delta);

		while (!log.isEmpty()){
			LogMessage msg = log.remove();
			if (++counter < 2000) logClip.appendText(msg.threadName + " - "+msg.level + " ["+msg.category+"] : "+msg.msg);
			//logClip2.appendMessage(msg);
		}
		
	}

	@Override
	public void render(GameContainer container, Graphics g) {
		

		
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
	
	public void addLogMessage(LogMessage msg){
		log.add(msg);
		//logString += msg.level + " : "+msg.msg + "\n";
	}
	
}
