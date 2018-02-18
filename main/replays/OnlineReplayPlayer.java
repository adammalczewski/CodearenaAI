package replays;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import structures.Action;
import structures.Orientation;
import structures.Action.ActionActionType;
import structures.Action.ActionRotationType;
import structures.Action.ActionType;
import game.ActionListener;
import game.GameEngine;
import game.GameListener;

public class OnlineReplayPlayer implements ActionListener{

	GameListener listener;
	DefaultHandler handler;
	
	SAXParser saxParser;
	
	BufferedReader reader;
	
	boolean endOfStream = false;
	
	public OnlineReplayPlayer(GameListener listener,DefaultHandler handler) {
		this.listener = listener;
		this.handler = handler;
	}
	
	public void start(String fileName){
		FileInputStream fos;
		InputStreamReader streamReader;
		try {
			fos = new FileInputStream(fileName);
			streamReader  = new InputStreamReader(fos, "UTF-8");
			reader = new BufferedReader(streamReader);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		
		try {
			String line = reader.readLine();
			if (line == null) endOfStream = true;
			else {
				saxParser = factory.newSAXParser();
				saxParser.parse(new InputSource(new StringReader(line)), handler);
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void getAction(Action action){
		getAction(action,null);
	}

	//FIXME Wywalic stad gameEngine
	
	public void getAction(Action action,GameEngine gameEngine) {
		
		try {
			String line = reader.readLine();
			if (line == null) endOfStream = true;
			else {
				//FIXME wywalic nastepne linijki
				
				{
				
					
					boolean success = true;
					int pos;
					
					if ((pos = line.indexOf("id=\"")) != -1){
						
						int endPos = line.indexOf("\"",pos+4);
						
						if (endPos == -1) success = false;
						
						if (success){
							
							gameEngine.action.unitID = Integer.parseInt(line.substring(pos+4,endPos));
							
							if ((pos = line.indexOf("action=\"")) != -1){
								
								endPos = line.indexOf("\"",pos+8);
								
								if (endPos == -1) success = false;
								
								if (success){
									
									String actionString = line.substring(pos+8,endPos);
									
									if (actionString.equalsIgnoreCase("drag")){
										gameEngine.action.actionType = ActionType.ACTION;
										gameEngine.action.actionActionType = ActionActionType.DRAG;
									} else if (actionString.equalsIgnoreCase("drop")){
										gameEngine.action.actionType = ActionType.ACTION;
										gameEngine.action.actionActionType = ActionActionType.DROP;
									} else if (actionString.equalsIgnoreCase("heal")){
										gameEngine.action.actionType = ActionType.ACTION;
										gameEngine.action.actionActionType = ActionActionType.HEAL;
									}
									
								}
								
							} else if ((pos = line.indexOf("rotate=\"")) != -1){
								
								endPos = line.indexOf("\"",pos+8);
								
								if (endPos == -1) success = false;
								
								if (success){
									
									String actionString = line.substring(pos+8,endPos);
									
									if (actionString.equalsIgnoreCase("rotateLeft")){
										gameEngine.action.actionType = ActionType.ROTATE;
										gameEngine.action.actionRotationType = ActionRotationType.ROTATE_LEFT;
									} else if (actionString.equalsIgnoreCase("rotateRight")){
										gameEngine.action.actionType = ActionType.ROTATE;
										gameEngine.action.actionRotationType = ActionRotationType.ROTATE_RIGHT;
									} else {
										System.out.println("Error : Bad rotation : "+actionString);
										endOfStream = true;
									}
									
								}
								
							} else if ((pos = line.indexOf("direction=\"")) != -1){
								
								endPos = line.indexOf("\"",pos+11);
								
								if (endPos == -1) success = false;
								
								if (success){
									
									String orientationString = line.substring(pos+11,endPos);
									System.out.println("OrientationString = "+orientationString);
									Orientation o = Orientation.valueOf(orientationString);
									
									gameEngine.action.actionType = ActionType.MOVE;
									gameEngine.action.actionMoveOrientation = o;
									
								}
								
							}
						}
						
						if (!success){
							System.out.println("Error : Nie udalo sie sparsować wiadomości");
							endOfStream = true;
						}
						
					}
				
				}		
				
				/*
				if (!line.equals(XMLCreator.createActionMessage(action))){
					System.out.println("Niewlasciwa komenda. Zmieniono algorytm.");
					//TODO zmienic to, zrobic zeby jakis status w zmiennej ustawial sobie player
					System.exit(-1);
				}*/
				
				line = reader.readLine();
				System.out.println("Parsuje linie : "+line);
				if (line == null) endOfStream = true;
				else saxParser.parse(new InputSource(new StringReader(line)), handler);
				line = reader.readLine();
				System.out.println("Parsuje linie : "+line);
				if (line == null) endOfStream = true;
				else saxParser.parse(new InputSource(new StringReader(line)), handler);
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public boolean getEndOfStream(){
		return endOfStream;
	}

}
