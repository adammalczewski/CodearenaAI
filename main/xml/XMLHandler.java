package xml;

import game.GameListener;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import structures.ActionRejection;
import structures.GameStatus;
import structures.MapField;
import structures.Orientation;
import structures.SeenUnit;
import structures.ServerResponse;
import structures.Unit;
import structures.MapField.Background;
import structures.MapField.Building;
import structures.MapField.Object;
import structures.ServerResponse.ResponseID;

public class XMLHandler extends DefaultHandler{
	
	Node parents;
	GameListener listener;
	
	GameStatus gameStatus;
	int unitNumber;
	Orientation seesDirection;
	ActionRejection actionRejection;
	ServerResponse serverResponse;
	
	
	private class Node{
		public String name;
		public Node next;
	}
	
	public XMLHandler(GameListener listener){
		super();
		parents = null;
		this.listener = listener;
		gameStatus = null;
		actionRejection = null;
		serverResponse = null;
	}
	
	private void writeBadPositioningError(String blockName){
		System.out.println("Nieprawidłowe miejsce wystąpienia bloku "+blockName);
		System.out.println("Wczesniejsze bloki :");
		System.out.print(" ");
		for (Node node = parents;node != null;node = node.next){
			System.out.print(node.name+" <- ");
		}
		System.out.println();
	}
	
	private String getAttributeByName(Attributes attributes,String section,String name){
		int attribute = attributes.getIndex(name);
		if (attribute == -1){
			System.out.println("Error : XMLHandler.startElement : "+section+" - nie znaleziono atrybutu "+name);
			return null;
		}
		return attributes.getValue(attribute);
	}
		
	@Override
	public void startElement(String uri, String localName
			, String qName, Attributes attributes) throws SAXException {
		
		if (qName.equalsIgnoreCase("response")) {
			
			if (parents == null){ //Odpowiedz dotyczaca mozliwosci rozp. gry
				serverResponse = new ServerResponse();
				String responseDesc = getAttributeByName(attributes,"response","status");
				if (responseDesc.equalsIgnoreCase("WRONG_CREDENTIALS")){
					serverResponse.id = ResponseID.WRONG_CREDENTIALS;
				} else if (responseDesc.equalsIgnoreCase("BUSY")){
					serverResponse.id = ResponseID.BUSY;
				} else if (responseDesc.equalsIgnoreCase("GAME_READY")){
					serverResponse.id = ResponseID.GAME_READY;
				} else if (responseDesc.equalsIgnoreCase("WAITING_FOR_PLAYER")){
					serverResponse.id = ResponseID.WAITING_FOR_PLAYER;
				} else if (responseDesc.equalsIgnoreCase("CHECK_WWW")){
					serverResponse.id = ResponseID.CHECK_WWW;
				} else if (responseDesc.equalsIgnoreCase("GAME_CLOSED")){
					serverResponse.id = ResponseID.GAME_CLOSED;
				} else {
					System.out.println("Error : XMLHandler.startElement : response - nieprawidłowy status : " + responseDesc);
				}	
			} else {
				writeBadPositioningError("response");
			}
			
		} else if (qName.equalsIgnoreCase("game")){
			
			if (parents == null){ //Status Gry
				gameStatus = new GameStatus();
				gameStatus.result = "";
				int attribute = attributes.getIndex("result");
				if (attribute == -1){
					
				} else gameStatus.result = attributes.getValue(attribute);
			} else if (parents.name.equalsIgnoreCase("response") && parents.next == null){ // zaczynamy grę
				String gameIDString = getAttributeByName(attributes,"game","id");
				serverResponse.game = Integer.parseInt(gameIDString);
			} else writeBadPositioningError("game");
			
		} else if (qName.equalsIgnoreCase("general")){
			//nic nie robimy
		} else if (qName.equalsIgnoreCase("timeSec")){
			//nic nie robimy
		} else if (qName.equalsIgnoreCase("roundNum")){
			//nic nie robimy
		} else if (qName.equalsIgnoreCase("amountOfPoints")){
			//nic nie robimy
		} else if (qName.equalsIgnoreCase("units")){
			unitNumber = -1;
		} else if (qName.equalsIgnoreCase("unit")){
			
			if (parents == null || parents.next == null){
				writeBadPositioningError("unit");
			} else if (parents.name.equalsIgnoreCase("units") && parents.next.name.equalsIgnoreCase("game")){
				
				++unitNumber;
				gameStatus.units.add(new Unit());
				gameStatus.units.get(unitNumber).id = Integer.parseInt(getAttributeByName(attributes,"unit","id"));
				gameStatus.units.get(unitNumber).posX = Integer.parseInt(getAttributeByName(attributes,"unit","x"));
				gameStatus.units.get(unitNumber).posY = Integer.parseInt(getAttributeByName(attributes,"unit","y"));
				gameStatus.units.get(unitNumber).status = getAttributeByName(attributes,"unit","status");
				//Zrobić sprawdzanie poprawności
				gameStatus.units.get(unitNumber).action = getAttributeByName(attributes,"unit","action");
				//Zrobić sprawdzanie poprawności
				gameStatus.units.get(unitNumber).player = Integer.parseInt(getAttributeByName(attributes,"unit","player"));
				gameStatus.units.get(unitNumber).hp = Integer.parseInt(getAttributeByName(attributes,"unit","hp"));
				String orientationString = getAttributeByName(attributes,"unit","orientation");
				gameStatus.units.get(unitNumber).orientation = Orientation.valueOf(orientationString.toUpperCase());
				
			} else if (parents.name.equalsIgnoreCase("sees") && parents.next.name.equalsIgnoreCase("unit")
					&& parents.next.next.name.equalsIgnoreCase("units") && parents.next.next.next.name.equalsIgnoreCase("game")){
				
				gameStatus.units.get(unitNumber).sees.get(seesDirection).unit = new SeenUnit();
				gameStatus.units.get(unitNumber).sees.get(seesDirection).unit.player
					= Integer.parseInt(getAttributeByName(attributes,"unit","player"));
				gameStatus.units.get(unitNumber).sees.get(seesDirection).unit.hp
					= Integer.parseInt(getAttributeByName(attributes,"unit","hp"));
				String orientationString = getAttributeByName(attributes,"unit","orientation");
				gameStatus.units.get(unitNumber).sees.get(seesDirection).unit.orientation
					= Orientation.valueOf(orientationString.toUpperCase());
					
			} else  writeBadPositioningError("unit");
			
		} else if (qName.equalsIgnoreCase("sees")){
			
			if (parents == null || parents.next == null || parents.next.next == null){
				writeBadPositioningError("sees");
			} else if (parents.name.equalsIgnoreCase("unit") && parents.next.name.equalsIgnoreCase("units")
					&& parents.next.next.name.equalsIgnoreCase("game"))
			{
				String orientationString = getAttributeByName(attributes,"sees","direction");
				seesDirection = Orientation.valueOf(orientationString.toUpperCase());
				MapField mapField = new MapField();
				//jak nie dostaniemy background to znaczy ze jest void
				mapField.background = Background.VOID;
				gameStatus.units.get(unitNumber).sees.put(seesDirection, mapField);

			} else writeBadPositioningError("sees");
			
		} else if (qName.equalsIgnoreCase("background")){
			
			if (parents == null || parents.next == null || parents.next.next == null
					|| parents.next.next.next == null){
				writeBadPositioningError("background");
			} else if (parents.name.equalsIgnoreCase("sees") && parents.next.name.equalsIgnoreCase("unit")
				&& parents.next.next.name.equalsIgnoreCase("units") && parents.next.next.next.name.equalsIgnoreCase("game")){
				//nic nie robimy
			} else writeBadPositioningError("background");
			
		} else if (qName.equalsIgnoreCase("object")){
			
			if (parents == null || parents.next == null || parents.next.next == null
					|| parents.next.next.next == null){
				writeBadPositioningError("object");
			} else if (parents.name.equalsIgnoreCase("sees") && parents.next.name.equalsIgnoreCase("unit")
				&& parents.next.next.name.equalsIgnoreCase("units") && parents.next.next.next.name.equalsIgnoreCase("game")){
				
				
				
			} else writeBadPositioningError("object");
			
		} else if (qName.equalsIgnoreCase("building")){
			
			if (parents == null || parents.next == null || parents.next.next == null
					|| parents.next.next.next == null){
				writeBadPositioningError("building");
			} else if (parents.name.equalsIgnoreCase("sees") && parents.next.name.equalsIgnoreCase("unit")
				&& parents.next.next.name.equalsIgnoreCase("units") && parents.next.next.next.name.equalsIgnoreCase("game")){
				
				gameStatus.units.get(unitNumber).sees.get(seesDirection).buildingPlayer
				  = Integer.parseInt(getAttributeByName(attributes,"building","player"));
				
			} else writeBadPositioningError("building");
			
		} else if (qName.equalsIgnoreCase("error")){
			actionRejection = new ActionRejection();
			String IDString = getAttributeByName(attributes,"error","id");
			actionRejection.id = Integer.parseInt(IDString);
			actionRejection.idDescription = getAttributeByName(attributes,"error","description");
		} else if (qName.equalsIgnoreCase("ok")){
			//nic nie robimy
		} else {
			System.out.println("XMLHandler.startElement : Otrzymano element o nieznanej nazwie :");
			System.out.println(" qName : "+qName);
			System.out.print(" ");
			for (Node node = parents;node != null;node = node.next){
				System.out.print(node.name+" <- ");
			}
			System.out.println();
		}
		
		/*System.out.println("Start Element: "+qName);
		for (int i = 0;i < attributes.getLength();++i){
			System.out.println(" attribute "+attributes.getQName(i)+ " : "+attributes.getValue(i));
		}*/
		
		Node newNode = new Node();
		newNode.name = qName;
		newNode.next = parents;
		parents = newNode;

	}
	
	@Override
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		
		if (parents == null || !parents.name.equalsIgnoreCase(qName)) throw new SAXException("Błąd w parsowaniu - zakończył się element, który nie został zapisany");
		
		parents = parents.next;
		
		if (qName.equalsIgnoreCase("response")) {
			listener.getServerResponse(serverResponse);
			serverResponse = null;
		} else if (qName.equalsIgnoreCase("game")){
			if (parents == null){
				listener.getGameStatus(gameStatus);
				gameStatus = null;
			}
		} else if (qName.equalsIgnoreCase("general")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("timeSec")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("roundNum")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("amountOfPoints")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("units")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("unit")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("sees")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("background")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("object")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("building")){
			//Nic nie robimy
		} else if (qName.equalsIgnoreCase("error")){
			listener.getActionRejection(actionRejection);
			actionRejection = null;
		} else if (qName.equalsIgnoreCase("ok")){
			listener.getActionConfirmation();
		} else {
			System.out.println("XMLHandler.startElement : Otrzymano element o nieznanej nazwie :");
			System.out.println(" qName : "+qName);
			System.out.print(" ");
			for (Node node = parents;node != null;node = node.next){
				System.out.print(node.name+" <- ");
			}
			System.out.println();
		}
		
		//System.out.println("End Element: "+qName);
	 
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException{
		
		if (parents == null) throw new SAXException("Błąd w parsowaniu - dostalismy wartosc elementu, który nie został zapisany");
			
		String value = new String(ch,start,length);
		
		if (parents.name.equalsIgnoreCase("response")) {
			System.out.println("Niespodziewana wartosc w bloku response : "+value);
		} else if (parents.name.equalsIgnoreCase("game")){
			System.out.println("Niespodziewana wartosc w bloku game : "+value);
		} else if (parents.name.equalsIgnoreCase("general")){
			System.out.println("Niespodziewana wartosc w bloku general : "+value);
		} else if (parents.name.equalsIgnoreCase("timeSec")){
			gameStatus.timeElapsed = Double.parseDouble(value);
		} else if (parents.name.equalsIgnoreCase("roundNum")){
			gameStatus.round = Integer.parseInt(value);
		} else if (parents.name.equalsIgnoreCase("amountOfPoints")){
			gameStatus.points = Integer.parseInt(value);
		} else if (parents.name.equalsIgnoreCase("units")){
			System.out.println("Niespodziewana wartosc w bloku units : "+value);
		} else if (parents.name.equalsIgnoreCase("unit")){
			if (parents.next.name.equalsIgnoreCase("sees")){
				if (!value.equalsIgnoreCase("peasant")) 
					System.out.println("Niewlasciwa wartosc w bloku sees : "+value);
			} else System.out.println("Niespodziewana wartosc w bloku unit : "+value);
		} else if (parents.name.equalsIgnoreCase("sees")){
			System.out.println("Niespodziewana wartosc w bloku sees : "+value);
		} else if (parents.name.equalsIgnoreCase("background")){
			
			if (value.equalsIgnoreCase("green")){
				gameStatus.units.get(unitNumber).sees.get(seesDirection).background = Background.GRASS;
			} else if (value.equalsIgnoreCase("orange")){
				gameStatus.units.get(unitNumber).sees.get(seesDirection).background = Background.FOREST;
			} else if (value.equalsIgnoreCase("blue")){
				gameStatus.units.get(unitNumber).sees.get(seesDirection).background = Background.CRYSTALFLOOR;
			} else if (value.equalsIgnoreCase("red")){
				gameStatus.units.get(unitNumber).sees.get(seesDirection).background = Background.SWAMP;
			} else if (value.equalsIgnoreCase("stone")){
				gameStatus.units.get(unitNumber).sees.get(seesDirection).background = Background.STONE;
			} else if (value.equalsIgnoreCase("black") || value.equalsIgnoreCase("")){
				gameStatus.units.get(unitNumber).sees.get(seesDirection).background = Background.VOID;
			} else System.out.println("Nie rozpoznawany typ podłoża : "+value);
			
		} else if (parents.name.equalsIgnoreCase("object")){
			
			if (value.equalsIgnoreCase("stone")){
				gameStatus.units.get(unitNumber).sees.get(seesDirection).object = Object.STONE;
			} else if (value.equalsIgnoreCase("diamond")){
				gameStatus.units.get(unitNumber).sees.get(seesDirection).object = Object.DIAMOND;
			} else System.out.println("Nie rozpoznany typ obiektu : "+value);
			
		} else if (parents.name.equalsIgnoreCase("building")){
			
			if (value.equalsIgnoreCase("base")){
				gameStatus.units.get(unitNumber).sees.get(seesDirection).building = Building.ALTAR;
			} else System.out.println("Nie rozpoznany typ obiektu : "+value);
			
		} else if (parents.name.equalsIgnoreCase("error")){
			actionRejection.text = value;
		} else if (parents.name.equalsIgnoreCase("ok")){
			System.out.println("Niespodziewana wartosc w bloku ok : "+value);
		} else {
			System.out.println("XMLHandler.startElement : Otrzymano element o nieznanej nazwie :");
			System.out.println(" parents.name : "+parents.name);
			System.out.print(" ");
			for (Node node = parents;node != null;node = node.next){
				System.out.print(node.name+" <- ");
			}
			System.out.println();
		}
		
		//System.out.println("characters: "+new String(ch,start,length));
	}
	
}
