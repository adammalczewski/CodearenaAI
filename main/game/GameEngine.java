package game;

import xml.XMLCreator;
import gameControllers.GameResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import logs.LogMessage;
import logs.Logger;
import logs.Logging;
import structures.Action;
import structures.GameStatus;
import structures.HealthLoss;
import structures.MapField;
import structures.Node;
import structures.Orientation;
import structures.SeenUnit;
import structures.Unit;
import structures.Action.ActionActionType;
import structures.Action.ActionRotationType;
import structures.Action.ActionType;
import structures.MapField.Background;
import structures.MapField.Building;
import structures.UnitPosition;

//TODO Powinny byc funkcje ktore beda przenosic diamenty w taki sposob by nie mozna bylo 
//TODO ich odebrac (tylem), i moze inne obiekty tez, bo wtedy wiemy czy ktos jest za nami czy nie

//TODO Zmienic funkcje wyszukujace zeby mozna bylo im dodac funkcje ustalającą do których
//TODO wierzchołków wchodzimy zamiast crossable()

public class GameEngine {
	
	public int log = 0;
	public String logString = "";
	
	Logger logger;
	
	public final static int MAX_MAPSIZEX = 100;
	public final static int MAX_MAPSIZEY = 100;
	
	private Node [][] map;
	
	boolean healed = false;
	
	//FIXME zmienic z powrotem na private
	public Action action = null;
	
	boolean actionPerformed = true;
	
	private Integer playerID;
	private int points;
	private Integer roundNumber;
	
	private int unitTurn = 0;
	
	private int diamondsStolen = 0;
	
	private ActionListener actionListener;
	
	public Map<Integer,Unit> units = new ConcurrentHashMap<Integer,Unit>();
	public Integer actualUnit;
	
	GameResult gameResult;
	
	private LinkedList<ValueNode> taken = new LinkedList<ValueNode>();
	
	private interface NodeFilter{
		boolean test(Node node);
	}
	
	private interface NodeFilterWithObject{
		boolean test(Node node,MapField.Object obj);
	}
	
	private interface NodeJudge{
		
		//nadaje wierzcholkowi wartość punktową (-1 oznacza że wierzcholek sie nie nadaje)
		Integer test(Node node,int distance);
		
	}
	
	private class ValueNode{
		
		public Node node;
		public int value;
		
		public ValueNode(Node node, int value){
			this.node = node;
			this.value = value;
		}
		
	}
	
	public Node [][] getMap(){
		return map;
	}
	
	public Integer getPlayerID(){
		return playerID;
	}
	
	public void setAction(Action action){
		if (this.action == null) this.action = action;
		else {
			logger.warning("Jakaś funkcja chciała dodać nową akcję");
		}
	}

	public GameEngine(ActionListener actionListener){
		
		this.actionListener = actionListener;
		
		roundNumber = 0;
		
		map = new Node[1000][1000];
		
		for (int x = 0;x < MAX_MAPSIZEX;++x)
			for (int y = 0;y < MAX_MAPSIZEY;++y){
				map[x][y] = new Node();
				map[x][y].x = x;
				map[x][y].y = y;
			}
		
		for (int x = 0;x < MAX_MAPSIZEX;++x)
			for (int y = 0;y < MAX_MAPSIZEY;++y){
				
				//Dodajemy krawedz NW
				map[x][y].edges.put(Orientation.NW,(y == 0 || (y%2==0 && x==0))
						?null:((y%2==0)?map[x-1][y-1]:map[x][y-1]));
				
				//Dodajemy krawędź NE
				map[x][y].edges.put(Orientation.NE,(y == 0 || (y%2==1 && x == MAX_MAPSIZEX-1))
						?null:((y%2==0)?map[x][y-1]:map[x+1][y-1]));
				
				//Dodajemy krawędź W
				map[x][y].edges.put(Orientation.W,(x == 0)?null:map[x-1][y]);
				
				//Dodajemy krawędź E
				map[x][y].edges.put(Orientation.E,(x == MAX_MAPSIZEX-1)?null:map[x+1][y]);
				
				//Dodajemy krawędź SW
				map[x][y].edges.put(Orientation.SW,(y == MAX_MAPSIZEY-1 || (y%2==0 && x==0))
						?null:((y%2==0)?map[x-1][y+1]:map[x][y+1]));
				
				//Dodajemy krawędź SE
				map[x][y].edges.put(Orientation.SE,(y == MAX_MAPSIZEY-1 || (y%2==1 && x == MAX_MAPSIZEX-1))
						?null:((y%2==0)?map[x][y+1]:map[x+1][y+1]));
				
			}
		
		logger = Logging.getLogger(GameEngine.class.getName());
		logger.setCategory("game");
		
	}
	
	//Find - znajduje pole spełniające dany warunek
	//Find near - znajduje pole obok pola spełniającego podany warunek
	//FindCrossable - znajduje pole spelniajace dany warunek, musi byc mozliwosc dojscia na nie

	//TODO dodac funkcje ktora bedzie jednoczesnie kilka filtrów przyjmowała, i będzie zwracała
	//TODO wiele wyników
	//znajduje wszystkie wierzcholki ktore sa w zasiegu przechodzace przez podany filtr
	public LinkedList<Node> findAll(int startPosX,int startPosY,NodeFilter filter){
		
		LinkedList<Node> result = new LinkedList<Node>();
		
		Queue<Node> queue = new LinkedList<Node>();
		
		boolean [][] visited = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		
		for (int xx = 0;xx < MAX_MAPSIZEX;++xx)
			for (int yy = 0;yy < MAX_MAPSIZEY;++yy)
				visited[xx][yy] = false;
		
		visited[startPosX][startPosY] = true;
		queue.add(map[startPosX][startPosY]);
		
		if (filter.test(map[startPosX][startPosY]))
			result.add(map[startPosX][startPosY]);
		
		while (queue.size() > 0){
			
			Node actualNode = queue.remove();
			
			for (Orientation o : Orientation.values()) if (actualNode.edges.get(o) != null){
				Node nextNode = actualNode.edges.get(o);
				if (!visited[nextNode.x][nextNode.y]){
					visited[nextNode.x][nextNode.y] = true;
					if (filter.test(nextNode)){
						result.add(nextNode);
					}
					if (nextNode.field != null && nextNode.field.crossable()){
						queue.add(actualNode.edges.get(o));
					}
				}
			}
			
		}
		
		return result;
		
	}
	
	//znajduje wszystkie obiekty podanego typu, do których można dojść idąc od podanej pozycji
	//uwzględnia występujące na drodze przeszkody w postaci innych obiektów, jednostek
	public LinkedList<Node> findAllObjects(int startPosX,int startPosY,Object object){
		
		return findAll(startPosX,startPosY,(Node n) -> n.field != null && n.field.object == object);
		
	}
	
	//znajduje wszystkie budynki podanego typu, do których można dojść idąc od podanej pozycji
	//uwzględnia występujące po drodze przeszkody w postaci innych obiektów, jednostek
	public LinkedList<Node> findAllBuildings(int startPosX,int startPosY,Building building){
		
		return findAll(startPosX,startPosY,(Node n) -> n.field != null && n.field.building == building);
		
	}
	
	//znajduje wszystkie budynki danego gracza podanego typu, do których można dojść idąc od podanej pozycji
	//uwzględnia występujące po drodze przeszkody w postaci innych obiektów, jednostek
	public LinkedList<Node> findAllPlayerBuildings(int startPosX,int startPosY,int playerID,Building building){
		
		return findAll(startPosX,startPosY,(Node n) ->
			n.field != null && n.field.building == building && n.field.buildingPlayer == playerID);
		
	}
	
	//Zwraca wszystkie pola spelniajaca zadane kryterium, w kolejnosci od najblizszego do najdalszego
	//odleglosc jest wyznaczana na podstawie ilosci zycia straconego na dojscie obok nich
	public LinkedList<Node> findHPClosestAll(int startPosX,int startPosY,NodeFilter filter){
		
		LinkedList<Node> result = new LinkedList<Node>();
		Queue<ValueNode> queue = new LinkedList<ValueNode>();
		
		boolean [][] visited = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		
		for (int x = 0;x < MAX_MAPSIZEX;++x)
			for (int y = 0;y < MAX_MAPSIZEY;++y)
				visited[x][y] = false;
		
		queue.add(new ValueNode(map[startPosX][startPosY],1));
		logger.finest("Dodajemy pole x ="+startPosX+ " y = "+startPosY);
		visited[startPosX][startPosY] = true;
		
		if (filter.test(map[startPosX][startPosY]))
			result.add(map[startPosX][startPosY]);
		
		while (queue.size() > 0){
			ValueNode actualValueNode = queue.remove();
			--actualValueNode.value;
			if (actualValueNode.value == 0){
				
				Node actualNode = actualValueNode.node;
				
				for (Orientation o : Orientation.values()) if (actualNode.edges.get(o) != null){
					Node nextNode = actualNode.edges.get(o);
					if (!visited[nextNode.x][nextNode.y]){
						visited[actualNode.edges.get(o).x][actualNode.edges.get(o).y] = true;
						if (filter.test(actualNode.edges.get(o))){
							result.add(actualNode.edges.get(o));
						}
						if (nextNode.field != null && actualNode.edges.get(o).field.crossable()){
							if (nextNode.field.background == Background.SWAMP){
								queue.add(new ValueNode(nextNode,2));
							} else queue.add(new ValueNode(nextNode,1));
						}
					}
				}
					
			} else queue.add(actualValueNode);
		}
		
		return result;
		
	}
	
	//znajduje wszystkie obiekty podanego typu, do których można dojść idąc od podanej pozycji
	//uwzględnia występujące na drodze przeszkody w postaci innych obiektów, jednostek
	public LinkedList<Node> findHPClosestAllObjects(int startPosX,int startPosY,Object object){
		
		return findHPClosestAll(startPosX,startPosY,(Node n) -> n.field != null && n.field.object == object);
		
	}
	
	//znajduje wszystkie budynki podanego typu, do których można dojść idąc od podanej pozycji
	//uwzględnia występujące po drodze przeszkody w postaci innych obiektów, jednostek
	public LinkedList<Node> findHPClosestAllBuildings(int startPosX,int startPosY,Building building){
		
		return findHPClosestAll(startPosX,startPosY,(Node n) -> n.field != null && n.field.building == building);
		
	}
	
	//znajduje wszystkie budynki danego gracza podanego typu, do których można dojść idąc od podanej pozycji
	//uwzględnia występujące po drodze przeszkody w postaci innych obiektów, jednostek
	public LinkedList<Node> findHPClosestAllPlayerBuildings(int startPosX,int startPosY,int playerID,Building building){
		
		return findHPClosestAll(startPosX,startPosY,(Node n) ->
			n.field != null && n.field.building == building && n.field.buildingPlayer == playerID);
		
	}
	
	public Orientation near(int x,int y,NodeFilter filter){
		Orientation found = null;
		for (Orientation o : Orientation.values()){
			if (found != null) break;
			if (map[x][y].edges.get(o) != null && filter.test(map[x][y].edges.get(o)))
				found = o;
		}
		return found;
	}
	
	public Orientation nearObject(int x,int y,Object object){

		return near(x,y,(Node n) -> n.field != null && n.field.object == object);
		
	}
	
	public Orientation nearBuilding(int x,int y,Building building){

		return near(x,y,(Node n) -> n.field != null && n.field.building == building);
		
	}
	
	public Orientation nearPlayerBuilding(int x,int y,int playerID,Building building){

		return near(x,y,(Node n) -> n.field != null && n.field.building == building
				&& n.field.buildingPlayer == playerID);

	}
	
	public Orientation nearPosition(int startPosX,int startPosY,int endPosX,int endPosY){
		
		return near(startPosX,startPosY,(Node n) -> n.x == endPosX && n.y == endPosY);
		
	}
	
	public Orientation nearNull(int x,int y){

		return near(x,y,(Node n) -> n.field == null);
		
	}
	
	//Szuka pola spełniającego podane kryterium, takiego na które dojście zabierze jak najmniej
	//życia
	public Queue<Orientation> roadToHPClosest(int startPosX,int startPosY,NodeFilter filter){
		
		Queue<Orientation> result = new LinkedList<Orientation>();
		Queue<ValueNode> queue = new LinkedList<ValueNode>();
		
		boolean [][] visited = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		Orientation [][] cameGoing = new Orientation[MAX_MAPSIZEX][MAX_MAPSIZEY];
		
		for (int x = 0;x < MAX_MAPSIZEX;++x)
			for (int y = 0;y < MAX_MAPSIZEY;++y)
				visited[x][y] = false;
		
		queue.add(new ValueNode(map[startPosX][startPosY],1));
		logger.finest("Dodajemy pole x ="+startPosX+ " y = "+startPosY);
		visited[startPosX][startPosY] = true;
		
		Node endNode = null;
		
		while (queue.size() > 0 && endNode == null){
			ValueNode actualValueNode = queue.remove();
			--actualValueNode.value;
			if (actualValueNode.value == 0){
				
				Node actualNode = actualValueNode.node;
				
				logger.finest("Wchodze na x = "+actualNode.x+" y = "+actualNode.y);
				if (filter.test(actualNode)){
					logger.finest("Znalazlem to co chcialem na pozycji x = "
							+actualNode.x+" y = "+actualNode.y);
					endNode = actualNode;
				}
				
				if (endNode == null){
					
					for (Orientation o : Orientation.values()) if (actualNode.edges.get(o) != null){
						Node nextNode = actualNode.edges.get(o);
						if (nextNode.field != null && !visited[nextNode.x][nextNode.y]
								&& nextNode.field.crossable()){
							logger.finest("dodalem x = "+nextNode.x+" y = "+nextNode.y);
							visited[nextNode.x][nextNode.y] = true;
							cameGoing[nextNode.x][nextNode.y] = o;
							if (nextNode.field.background == Background.SWAMP) 
								queue.add(new ValueNode(nextNode,2));
							else queue.add(new ValueNode(nextNode,1));
							
						}
					}
					
				}
			} else queue.add(actualValueNode);
		}
		
		Stack<Orientation> resultBackwards = new Stack<Orientation>();
		
		if (endNode != null){
			
			Node pos = endNode;
			
			while (pos.x != startPosX || pos.y != startPosY){
				
				resultBackwards.add(cameGoing[pos.x][pos.y]);
				Orientation direction = cameGoing[pos.x][pos.y].reverse();
				pos = map[pos.x][pos.y].edges.get(direction);
				
			}
			
		}
		
		while (resultBackwards.size() > 0){
			
			result.add(resultBackwards.pop());
			
		}
		
		return result;
		
		
	}
	
	public Queue<Orientation> roadToNearHPClosestNull(int startPosX,int startPosY){
		
		return roadToHPClosest(startPosX,startPosY,(Node n) -> nearNull(startPosX,startPosY) != null);
		
	}
	
	public Queue<Orientation> roadToNearHPClosestObject(int startPosX,int startPosY,Object object){
		
		return roadToHPClosest(startPosX,startPosY,
				(Node n) -> nearObject(startPosX,startPosY,object) != null);
				
	}
	
	public Queue<Orientation> roadToNearHPClosestBuilding(int startPosX,int startPosY,Building building){
		
		return roadToHPClosest(startPosX,startPosY,
				(Node n) -> nearBuilding(startPosX,startPosY,building) != null);
				
	}
	
	public Queue<Orientation> roadToNearHPClosestPlayerBuilding(int startPosX,int startPosY,int playerID,Building building){
		
		return roadToHPClosest(startPosX,startPosY,
				(Node n) -> nearPlayerBuilding(startPosX,startPosY,playerID,building) != null);
				
	}
	
	public Queue<Orientation> roadToNearHPClosestPosition(int startPosX,int startPosY,int endPosX,int endPosY){
		logger.finer("roadToNearHPClosestPosition : idziemy do x = "+endPosX+" y = "+endPosY);
		return roadToHPClosest(startPosX,startPosY,(Node n) -> nearPosition(n.x,n.y
				,endPosX,endPosY) != null);
	}
	
	private Queue<Orientation> roadToHPClosestPosition(int startPosX,int startPosY,int endPosX,int endPosY){
		logger.finer("roadToHPClosestPosition : idziemy do x = "+endPosX+" y = "+endPosY);
		return roadToHPClosest(startPosX,startPosY,(Node n) ->
			n.x == endPosX && n.y == endPosY);
		
	}
	
	private Queue<Orientation> findBestCrossable(int startPosX,int startPosY,NodeJudge judge){

		//Zmienic zeby value zalezalo od pola na ktorym stoi
		
		Queue<Orientation> result = new LinkedList<Orientation>();
		Queue<ValueNode> queue = new LinkedList<ValueNode>();
		
		boolean [][] visited = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		Orientation [][] cameGoing = new Orientation[MAX_MAPSIZEX][MAX_MAPSIZEY];
		
		for (int xx = 0;xx < MAX_MAPSIZEX;++xx)
			for (int yy = 0;yy < MAX_MAPSIZEY;++yy)
				visited[xx][yy] = false;
		
		queue.add(new ValueNode(map[startPosX][startPosY],1));
		visited[startPosX][startPosY] = true;
		
		LinkedList<Node> bestNodes = new LinkedList<Node>();
		int bestPoints = -Integer.MIN_VALUE;	
		
		int distance = 0;
		int distNodes = 1;//ile jest wierzcholkow o podanej odleglosci
		
		while (queue.size() > 0){
			ValueNode actualValueNode = queue.remove();
			--actualValueNode.value;
			if (actualValueNode.value == 0){
				
				Node actualNode = actualValueNode.node;
				
				//println("Wchodze na x = "+actualNode.x+" y = "+actualNode.y);
				
				Integer points = judge.test(actualNode,distance);
				if (points != null){
					if (points > bestPoints){
						bestPoints = points;
						bestNodes.clear();
						bestNodes.add(actualNode);
					} else if (points == bestPoints){
						bestNodes.add(actualNode);
					}
				}
				
				for (Orientation o : Orientation.values()) if (actualNode.edges.get(o) != null){
					Node nextNode = actualNode.edges.get(o);
					if (nextNode.field != null && !visited[nextNode.x][nextNode.y]
							&& nextNode.field.crossable()){
						//println("dodalem x = "+nextNode.x+" y = "+nextNode.y);
						visited[nextNode.x][nextNode.y] = true;
						cameGoing[nextNode.x][nextNode.y] = o; 
						if (nextNode.field.background == Background.SWAMP) 
							queue.add(new ValueNode(nextNode,2));
						else queue.add(new ValueNode(nextNode,1));
					}
				}
					
			} else queue.add(actualValueNode);
			
			if (--distNodes == 0){
				//jezeli sie skonczyly wierzcholki o zapamietanej odleglosci
				distNodes = queue.size();
				++distance;
			}
			
		}
		
		
		Stack<Orientation> resultBackwards = new Stack<Orientation>();
		
		if (bestNodes.size() > 0){
			
			Random random = new Random();
			
			Node pos = bestNodes.get(random.nextInt(bestNodes.size()));
			
			while (pos.x != startPosX || pos.y != startPosY){
				
				resultBackwards.add(cameGoing[pos.x][pos.y]);
				Orientation direction = cameGoing[pos.x][pos.y].reverse();
				pos = map[pos.x][pos.y].edges.get(direction);
				
			}
			
		}
		
		while (resultBackwards.size() > 0){
			
			result.add(resultBackwards.pop());
			
		}
		
		return result;
		
	}
	
	private Queue<Orientation> findBestExploreField(int startPosX,int startPosY){
		
		return findBestCrossable(startPosX,startPosY,(Node n,int distance) -> {
			
			int nulls = 0;
			int unknowns = 0;
			
			for (Orientation o : Orientation.values()) if (n.edges.get(o) != null){
				if (n.edges.get(o).field == null){
					++nulls;
				} else if (n.edges.get(o).field.object == MapField.Object.UNKNOWN){
					++unknowns;
				}
			}
			
			if (nulls > 0 || unknowns > 0){
				
				int crystalFloorBonus = 0;
				if (n.field != null && n.field.background == Background.CRYSTALFLOOR)
					crystalFloorBonus = distance+2;
				n.value = Integer.toString(nulls + unknowns - 2*distance + crystalFloorBonus);
				return nulls + unknowns - 2*distance + crystalFloorBonus;
			} else {
				n.value = null;
				return null;
			}
			
		});
		
	}
	
	private Queue<Orientation> findBestUnknownObjectField(int startPosX,int startPosY){
		
		return findBestCrossable(startPosX,startPosY,(Node n,int distance) -> {
			
			int unknowns = 0;
			
			for (Orientation o : Orientation.values()) if (n.edges.get(o) != null){
				if (n.edges.get(o).field != null && n.edges.get(o).field.object == MapField.Object.UNKNOWN){
					++unknowns;
				}
			}
			
			if (unknowns > 0){
				
				int crystalFloorBonus = 0;
				if (n.field != null && n.field.background == Background.CRYSTALFLOOR)
					crystalFloorBonus = distance+2;
				n.value = Integer.toString(unknowns - 2*distance + crystalFloorBonus);
				return unknowns - 2*distance + crystalFloorBonus;
			} else {
				n.value = null;
				return null;
			}
			
		});
		
	}
	
	private Queue<Orientation> findBestNewFieldForAltar(int posX,int posY,int startPosX,int startPosY){
		
		return findBestCrossable(posX,posY,(Node n,int distance) -> {
			
			int nulls = 0;
			
			for (Orientation o : Orientation.values()) if (n.edges.get(o) != null
					&& n.edges.get(o).field == null){
				++nulls;
			}
			
			Integer startPosDistance = nodesDistance(map[startPosX][startPosY],n);
			
			if (nulls > 0 && startPosDistance != null){
				n.value = Integer.toString(nulls - 2*distance - (int)(2.5f*startPosDistance));
				return nulls - 2*distance - (int)(2.5f*startPosDistance);
			} else {
				n.value = null;
				return null;
			}
			
		});
		
	}
	
	private int nodesDistance(Node n1,Node n2){
		
		//Zmienic zeby value zalezalo od pola na ktorym stoi
		
		Integer result = null;
		Queue<Node> queue = new LinkedList<Node>();
		
		boolean [][] visited = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		
		for (int xx = 0;xx < MAX_MAPSIZEX;++xx)
			for (int yy = 0;yy < MAX_MAPSIZEY;++yy)
				visited[xx][yy] = false;
		
		queue.add(n1);
		visited[n1.x][n1.y] = true;
		
		int distance = 0;
		int distNodes = 1;//ile jest wierzcholkow o podanej odleglosci
		
		while (queue.size() > 0 && result == null){
			Node actualNode = queue.remove();
			
			//println("Wchodze na x = "+actualNode.x+" y = "+actualNode.y);
			
			if (actualNode == n2){
				result = distance;
			}
			
			for (Orientation o : Orientation.values()) if (actualNode.edges.get(o) != null){
				Node nextNode = actualNode.edges.get(o);
				if (nextNode.field != null && !visited[nextNode.x][nextNode.y]){
					//println("dodalem x = "+nextNode.x+" y = "+nextNode.y);
					visited[nextNode.x][nextNode.y] = true;
					queue.add(nextNode);
				}
			}
			
			if (--distNodes == 0){
				//jezeli sie skonczyly wierzcholki o zapamietanej odleglosci
				distNodes = queue.size();
				++distance;
			}
			
		}
		
		return result;
		
	}
	
	public int countNodes(int x,int y){
		
		int result = 1;
		
		Queue<Node> queue = new LinkedList<Node>();
		
		boolean [][] visited = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		
		for (int xx = 0;xx < MAX_MAPSIZEX;++xx)
			for (int yy = 0;yy < MAX_MAPSIZEY;++yy)
				visited[xx][yy] = false;
		
		queue.add(map[x][y]);
		visited[x][y] = true;
		
		while (queue.size() > 0){
			Node actualNode = queue.remove();
			
			for (Orientation o : Orientation.values()) if (actualNode.edges.get(o) != null){
				Node nextNode = actualNode.edges.get(o);
				if (nextNode.field != null && !visited[nextNode.x][nextNode.y]){
					visited[nextNode.x][nextNode.y] = true;
					queue.add(nextNode);
					++result;
				}
			}
		}
		
		
		return result;
		
	}
	
	public NewActionChain dropIfDragging(Unit unit,AtomicBoolean success){
		
		if (unit.action.equalsIgnoreCase("dragging")){
			logger.fine("dropIfDragging - wykryto ze niesiemy obiekt");
			Action action = new Action();
			action.actionType = ActionType.ACTION;
			action.actionActionType = ActionActionType.DROP;
			NewActionChain result = new NewActionChain();
			result.actions.add(action);
			return result;
		} else {
			
			if (success != null) success.set(false);
			return new NewActionChain();
			
		}
		
	}
	
	public NewActionChain newExplore(Unit unit,AtomicBoolean success){
		
		Queue<Orientation> road = findBestExploreField(unit.posX,unit.posY);
		
		if (road.size() != 0){
			
			return new NewActionChain(unit,road);
		
		} else {
			
			if (success != null) success.set(false);
			return new NewActionChain(unit);
			
		}
		
	}
	
	public NewActionChain newExploreUnknowns(Unit unit,AtomicBoolean success){
		
		Queue<Orientation> road = findBestUnknownObjectField(unit.posX,unit.posY);
		
		if (road.size() != 0){
			
			return new NewActionChain(unit,road);
		
		} else {
			
			if (success != null) success.set(false);
			return new NewActionChain(unit);
			
		}
		
	}
	
	public NewActionChain newExploreForAltar(Unit unit,AtomicBoolean success){
		
		Queue<Orientation> road = findBestNewFieldForAltar(unit.posX,unit.posY,unit.startPosX,unit.startPosY);
		
		if (road.size() != 0){
			
			return new NewActionChain(unit,road);
		
		} else {
			
			if (success != null) success.set(false);
			return new NewActionChain(unit);
			
		}
		
	}
	
	public NewActionChain newMoveTo(Unit unit,int destX,int destY,AtomicBoolean success){
		
		if (unit.posX == destX && unit.posY == destY){
			return new NewActionChain(unit);
		}
		
		Queue<Orientation> road = roadToHPClosestPosition(unit.posX,unit.posY,destX,destY);
		
		logger.finer("newMoveTo - Nasza droga ma dlugosc : "+road.size());
		
		if (road.size() != 0){
		
			return new NewActionChain(unit,road);
		
		} else {
			
			if (success != null) success.set(false);
			return new NewActionChain(unit);
			
		}
		
	}
	
	public NewActionChain newMoveNear(Unit unit,int destX,int destY,AtomicBoolean success){
		
		if (nearPosition(unit.posX, unit.posY, destX, destY) != null){
			return new NewActionChain(unit);
		}
		
		Queue<Orientation> road = new LinkedList<Orientation>();
		
		road = roadToNearHPClosestPosition(unit.posX,unit.posY,destX,destY);	
		
		if (road.size() != 0){
		
			return new NewActionChain(unit,road);
		
		} else {
			
			if (success != null) success.set(false);
			return new NewActionChain(unit);
		
		}
		
	}

	public Action newRotateLeft(){
		Action result = new Action();
		result.actionType = ActionType.ROTATE;
		result.actionRotationType = ActionRotationType.ROTATE_LEFT;
		return result;
	}
	
	public Action newRotateRight(){
		Action result = new Action();
		result.actionType = ActionType.ROTATE;
		result.actionRotationType = ActionRotationType.ROTATE_RIGHT;
		return result;
	}
	
	public NewActionChain newRotate(Orientation from,Orientation to){
		if (from == to) return new NewActionChain();
		int rotationsLeft = from.rotationsLeft(to);
		int rotationsRight = from.rotationsRight(to);
		NewActionChain result = new NewActionChain();
		if (rotationsLeft < rotationsRight){
			for (int i = 0;i < rotationsLeft;++i){
				result.add(newRotateLeft());
			}
		} else {
			for (int i = 0;i < rotationsRight;++i){
				result.add(newRotateRight());
			}
		}
		return result;
	}
	
	public void move(int unitID,Orientation orientation){
		Action action = new Action();
		action.actionType = ActionType.MOVE;
		action.actionMoveOrientation = orientation;
		action.unitID = unitID;
		setAction(action);
	}
	
	public Action newMove(Orientation orientation){
		Action action = new Action();
		action.actionType = ActionType.MOVE;
		action.actionMoveOrientation = orientation;
		return action;
	}
	
	public Action newDrag(){
		Action action = new Action();
		action.actionType = ActionType.ACTION;
		action.actionActionType = ActionActionType.DRAG;
		return action;
	}
	
	public Action newDrop(){
		Action action = new Action();
		action.actionType = ActionType.ACTION;
		action.actionActionType = ActionActionType.DROP;
		return action;
	}
	
	public Action newHeal(){
		Action action = new Action();
		action.actionType = ActionType.ACTION;
		action.actionActionType = ActionActionType.HEAL;
		return action;
	}
	
	public Action fight(){
		Action action = new Action();
		action.actionType = ActionType.ACTION;
		action.actionActionType = ActionActionType.FIGHT;
		return action;
	}
	
	class NewActionChain{
		//Poczatkowe polozenie, zeby mozna bylo sie upewnic czy jednostka moze rozpoczac ruch
		UnitPosition startPos;
		
		private Queue<Action> actions;
		
		public NewActionChain(){
			actions = new LinkedList<Action>();
		}
		
		public NewActionChain(Unit unit){
			actions = new LinkedList<Action>();
			startPos = unit.getPos();
		}
		
		public NewActionChain(int startX,int startY,Orientation startOrientation){
			actions = new LinkedList<Action>();
			startPos = new UnitPosition(startX,startY,startOrientation);
		}
		
		public NewActionChain(Unit unit,Queue<Orientation> moves){
			actions = new LinkedList<Action>();
			startPos = unit.getPos();
			for (Orientation o : moves){
				Action action = new Action();
				action.actionType = ActionType.MOVE;
				action.actionMoveOrientation = o;
				actions.add(action);
			}
		}
		
		public void add(Action action){
			actions.add(action);
		}
		
		public void add(NewActionChain actionChain){
			if (startPos == null){
				startPos = actionChain.startPos;
			}
			for (Action action : actionChain.actions){
				actions.add(action);
			}
		}
		
		public boolean takeNodes(int roundNumber){
			if (startPos == null) return false;
			UnitPosition pos = new UnitPosition(startPos.posX,startPos.posY,startPos.orientation);
			for (Action action : actions){
				switch (action.actionType){
				case ACTION:
					break;
				case MOVE:
					pos.move(action.actionMoveOrientation,map);
					break;
				case ROTATE:
					switch (action.actionRotationType){
					case ROTATE_LEFT:
						pos.rotateLeft();
						break;
					case ROTATE_RIGHT:
						pos.rotateRight();
						break;
					default:
						break;
					
					}
					break;
				default:
					
				}
				taken.add(new ValueNode(map[pos.posX][pos.posY],roundNumber));
				++roundNumber;
			}
			
			return true;
			
		}
		
		public UnitPosition calculateEndPosition(){
			UnitPosition pos = new UnitPosition(startPos.posX,startPos.posY,startPos.orientation);
			for (Action action : actions){
				switch (action.actionType){
				case ACTION:
					break;
				case MOVE:
					pos.move(action.actionMoveOrientation,map);
					break;
				case ROTATE:
					switch (action.actionRotationType){
					case ROTATE_LEFT:
						pos.rotateLeft();
						break;
					case ROTATE_RIGHT:
						pos.rotateRight();
						break;
					default:
						break;
					
					}
					break;
				default:
					logger.error("NewActionChain.calculateEndPosition : Niewlasciwa akcja");
				}
			}
			return pos;
		}
		
		public HealthLoss calculateHealthLoss(){
			
			int min = 0;
			int max = 0;
			
			UnitPosition pos = new UnitPosition(startPos.posX,startPos.posY,startPos.orientation);
			for (Action action : actions){
				switch (action.actionType){
				case ACTION:
					break;
				case MOVE:
					pos.move(action.actionMoveOrientation,map);
					break;
				case ROTATE:
					switch (action.actionRotationType){
					case ROTATE_LEFT:
						pos.rotateLeft();
						break;
					case ROTATE_RIGHT:
						pos.rotateRight();
						break;
					default:
						break;
					
					}
					break;
				default:
					logger.error("NewActionChain.calculateHealthLoss : Niewlasciwa akcja");
				}
				MapField field = map[pos.posX][pos.posY].field;
				if (field == null || field.background == null){
					min += 1;
					max += 2;
				} else switch (field.background){
				case CRYSTALFLOOR:
					min += 1;
					max += 1;
					break;
				case FOREST:
					min += 1;
					max += 1;
					break;
				case GRASS:
					min += 1;
					max += 1;
					break;
				case STONE:
					min += 1;
					max += 1;
					break;
				case SWAMP:
					min += 2;
					max += 2;
					break;
				case VOID:
					logger.info("NewActionChain.calculateHealthLoss : jednostka stoi na voidzie");
					break;
				default:
					logger.error("NewActionChain.calculateHealthLoss : niewlasciwy typ terenu : "+field.background);
					
				}
			}
			
			return new HealthLoss(min,max);
		}
		
		//Zwraca true gdy konczy akcje
		public boolean executeAction(Unit unit){
			logger.fine("Wykonujemy akcje z NewActionChain");
			if (actions.size() <= 0){
				logger.fine("NewChainAction.executeAction - Nie mozna wykonac akcji, poniewaz nie ma zadnej");
				return true;
			} else {
				Action action = actions.remove();
				action.unitID = unit.id;
				setAction(action);
				if (actions.size() > 0){
					return false;
				} else return true;
			}
		}
		
		public LinkedList<Node> crossedNodes(NodeFilter filter){
			LinkedList<Node> result = new LinkedList<Node>();
			
			if (startPos == null) return result;
			
			UnitPosition pos = new UnitPosition(startPos.posX,startPos.posY,startPos.orientation);
			if (filter.test(map[startPos.posX][startPos.posY]))
				result.add(map[startPos.posX][startPos.posY]);
			for (Action action : actions){
				switch (action.actionType){
				case ACTION:
					break;
				case MOVE:
					pos.move(action.actionMoveOrientation,map);
					break;
				case ROTATE:
					switch (action.actionRotationType){
					case ROTATE_LEFT:
						pos.rotateLeft();
						break;
					case ROTATE_RIGHT:
						pos.rotateRight();
						break;
					default:
						break;
					
					}
					break;
				default:
					logger.error("NewActionChain.calculateHealthLoss : Niewlasciwa akcja");
				}
				if (filter.test(map[pos.posX][pos.posY]))
					result.add(map[pos.posX][pos.posY]);
				
			}
			
			return result;
			
		}
		
		public LinkedList<Node> crossedNodesDragging(NodeFilter filter){
			
			logger.fine("crossedNodesDragging",LogMessage.CATEGORY_STACK_TRACE);
			
			LinkedList<Node> result = new LinkedList<Node>();
			
			if (startPos != null){
			
				UnitPosition pos = new UnitPosition(startPos.posX,startPos.posY,startPos.orientation);
				if (filter.test(map[startPos.posX][startPos.posY]))
					result.add(map[startPos.posX][startPos.posY]);
				if (filter.test(map[startPos.posX][startPos.posY].edges.get(startPos.orientation)))
					result.add(map[startPos.posX][startPos.posY].edges.get(startPos.orientation));
				for (Action action : actions){
					switch (action.actionType){
					case ACTION:
						break;
					case MOVE:
						logger.finest("Akcja move (o = "+action.actionMoveOrientation);
						pos.move(action.actionMoveOrientation,map);
						break;
					case ROTATE:
						switch (action.actionRotationType){
						case ROTATE_LEFT:
							logger.finest("Akcja rotateLeft");
							pos.rotateLeft();
							break;
						case ROTATE_RIGHT:
							logger.finest("Akcja rotateRight");
							pos.rotateRight();
							break;
						default:
							break;
						
						}
						break;
					default:
						logger.error("NewActionChain.calculateHealthLoss : Niewlasciwa akcja");
					}
					if (filter.test(map[pos.posX][pos.posY]))
						result.add(map[pos.posX][pos.posY]);
					if (filter.test(map[pos.posX][pos.posY].edges.get(pos.orientation)))
						result.add(map[pos.posX][pos.posY].edges.get(pos.orientation));
					
				}
			
			}
			
			logger.fine("crossedNodesDragging",LogMessage.CATEGORY_STACK_TRACE);
			
			return result;
			
		}
		
		public Integer inWhichTurnFirstNodeIsCrossed(NodeFilter filter){
			
			logger.fine("inWhichTurnFirstNodeIsCrossed",LogMessage.CATEGORY_STACK_TRACE);
			
			Integer result = null;
			
			if (startPos != null && result == null){
			
				int turnNumber = 1;
				
				UnitPosition pos = new UnitPosition(startPos.posX,startPos.posY,startPos.orientation);
				if (filter.test(map[startPos.posX][startPos.posY]))
					result = turnNumber;
				if (filter.test(map[startPos.posX][startPos.posY].edges.get(startPos.orientation)))
					result = turnNumber;
				for (Action action : actions) if (result == null){
					switch (action.actionType){
					case ACTION:
						break;
					case MOVE:
						logger.finest("Akcja move (o = "+action.actionMoveOrientation);
						pos.move(action.actionMoveOrientation,map);
						break;
					case ROTATE:
						switch (action.actionRotationType){
						case ROTATE_LEFT:
							logger.finest("Akcja rotateLeft");
							pos.rotateLeft();
							break;
						case ROTATE_RIGHT:
							logger.finest("Akcja rotateRight");
							pos.rotateRight();
							break;
						default:
							break;
						
						}
						break;
					default:
						logger.error("NewActionChain.calculateHealthLoss : Niewlasciwa akcja");
					}
					turnNumber++;
					if (filter.test(map[pos.posX][pos.posY]))
						result = turnNumber;
					if (filter.test(map[pos.posX][pos.posY].edges.get(pos.orientation)))
						result = turnNumber;
					
				}
			
			}
			
			logger.fine("~inWhichTurnFirstNodeIsCrossed",LogMessage.CATEGORY_STACK_TRACE);
			
			return result;
		}
		
		@Override
		public String toString(){
			String result = "";
			if (startPos != null){
				result += "Zaczynamy od "+startPos+"\n";
			} else result += "Nie mamy poczatkowej pozycji\n";
			for (Action action : actions){
				result += " "+action+"\n";
			}
			if (startPos != null){
				UnitPosition endPosition = calculateEndPosition();
				result += "Konczymy na "+endPosition;
			}
			return result;
		}
		
	}
	
	//Zapisuje do kolejki drogę jaką trzeba przenieść obiekt z pozycji posX,posY
	//na miejsce które przepuści metoda test obiektu filter, zwraca uwage na przeszkody
	//w postaci innych obiektów lub jednostek
	//ta funkcja dodatkowo posiada parametr crossableFilter i objCrossableFilter
	//dzieki czemu mozna ustalac przez jakie pola mozna przechodzic i przez jakie nie
	//TODO krawedzie powinny byc tez pomiedzy wierzcholkami sasiadujacymi z obiektem
	//TODO o ile obiekt nie znajduje sie na voidzie
	public NewActionChain newDragObjectBackwards(int posX,int posY,NodeFilter filter,Orientation orientation
			,AtomicBoolean success,NodeFilter crossableFilter,NodeFilterWithObject objCrossableFilter){
		class OrientationNode{
			public int posX;
			public int posY;
			public Orientation orientation;
			public OrientationNode [] edges;
			public int edgesCount;
			boolean visited;
			
			//Skąd się dostaliśmy do tego wierzchołka
			public OrientationNode from;
			
			public OrientationNode(){
				edges = new OrientationNode[8];
				edgesCount = 0;
				visited = false;
			}
			
		};
		
		class OrientationNodes{
			public EnumMap<Orientation,OrientationNode> nodes;
			
			public OrientationNodes(){
				nodes = new EnumMap<Orientation,OrientationNode>(Orientation.class);
			}
			
		};
		
		boolean [][] queued = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		boolean [][] visited = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		OrientationNodes [][] nodes = new OrientationNodes[MAX_MAPSIZEX][MAX_MAPSIZEY];
		
		for (int x = 0;x < MAX_MAPSIZEX;++x)
			for (int y = 0;y < MAX_MAPSIZEY;++y){
				queued[x][y] = false;
				visited[x][y] = false;
			}
		
		logger.finer("dragObject : x = "+posX+" y = "+posY+" orien. ="+((orientation==null)?"dowolna":orientation));
		
		Node objectNode = map[posX][posY];
		
		Queue<Node> queue = new LinkedList<Node>();
		
		//dodajemy do kolejki pole z obiektem i pola obok obiektu (te po których można chodzić)
		
		//TODO zrobic cos z tym co w nastepnej linijce
		//i te miejsce na ktore wskazuje orientacja przekazana do metody
		for (Orientation o : Orientation.values()) if (crossableFilter.test(objectNode.edges.get(o))
				|| (orientation != null && orientation == o)){
			
			queued[objectNode.edges.get(o).x][objectNode.edges.get(o).y] = true;
			logger.finest(" Dodajemy do kolejki wierzcholek x = "
					+objectNode.edges.get(o).x+" y = "+objectNode.edges.get(o).y);
			queue.add(objectNode.edges.get(o));
					
		}
				
		//To pole dodajemy tylko wtedy gdy mozna po nim chodzic
		if (map[posX][posY].field.crossableTerrain()){
			queued[posX][posY] = true;
			queue.add(objectNode);
		}
		
		logger.finer("CreatingGraph",LogMessage.CATEGORY_STACK_TRACE);
		
		while (queue.size() > 0){

			Node actualNode = queue.remove();
			
			//println("Sprawdzamy wierzcholek x = "+actualNode.x+" y = "+actualNode.y);
			
			visited[actualNode.x][actualNode.y] = true;
			nodes[actualNode.x][actualNode.y] = new OrientationNodes();
			
			logger.finest(" Wyjmujemy x = "+actualNode.x+ " y = "+actualNode.y);
			
			//Sprawdzamy wszystkie orientacje postaci stojacej na tym polu z obiektem
			//Trzeba sprawdzic we wszystkich kierunkach czy obiekt moze stac obok tego pola
			for (Orientation o : Orientation.values()){
				if (objCrossableFilter.test(actualNode.edges.get(o), objectNode.field.object)
								|| actualNode.edges.get(o) == objectNode){
					OrientationNode oNode = new OrientationNode();
					oNode.orientation = o;
					oNode.posX = actualNode.x;
					oNode.posY = actualNode.y;
					logger.finest("  Dodajemy oNode : x = "+oNode.posX+" y = "+oNode.posY + " orient. = "
								+oNode.orientation);
					nodes[actualNode.x][actualNode.y].nodes.put(o, oNode);
				}
			}
			
			//Łączymy te które nie są nullami
			for (Orientation o : Orientation.values()){
				if (nodes[actualNode.x][actualNode.y].nodes.get(o) != null){
					OrientationNode oNode = nodes[actualNode.x][actualNode.y].nodes.get(o);
					Orientation right = o.rotateRight();
					Orientation left = o.rotateLeft();
					
					//Jak istnieje lewy to łączymy
					if (nodes[actualNode.x][actualNode.y].nodes.get(left) != null){
						oNode.edges[oNode.edgesCount++] = nodes[actualNode.x][actualNode.y].nodes.get(left);
					}
					
					//Jak istnieje prawy to łączymy
					if (nodes[actualNode.x][actualNode.y].nodes.get(right) != null){
						oNode.edges[oNode.edgesCount++] = nodes[actualNode.x][actualNode.y].nodes.get(right);
					}
					
				}
			}
			
			//Sprawdzamy pola obok
			logger.finest(" Sprawdzamy pola obok");
			for (Orientation o : Orientation.values()){
				if (actualNode.edges.get(o) != null && ((actualNode.edges.get(o).field != null 
						&& (crossableFilter.test(actualNode.edges.get(o)) || actualNode.edges.get(o) == objectNode)) 
						|| (actualNode == objectNode && orientation == o)))
				{
					if (!queued[actualNode.edges.get(o).x][actualNode.edges.get(o).y]){
						//Trzeba dodac wierzcholek do kolejki, jak mozna przez niego przechodzic
						logger.finest("  "+o+" - zakolejkowany");
						if (crossableFilter.test(actualNode.edges.get(o))){
							queued[actualNode.edges.get(o).x][actualNode.edges.get(o).y] = true;
							queue.add(actualNode.edges.get(o));
							//println("Dodajemy do kolejki wierzcholek x = "+actualNode.edges.get(o).x
							//		+" y = "+actualNode.edges.get(o).y);
						}
					} else if (visited[actualNode.edges.get(o).x][actualNode.edges.get(o).y]){
						//Wierzcholek zostal juz dodany, dodajemy wiazania
						logger.finest("  "+o+" - odwiedzony");
						for (Orientation o2: Orientation.values()){
							//Musza sie zgadzac orientacje w jednym i drugim
							OrientationNode oNode1 = nodes[actualNode.x][actualNode.y].nodes.get(o2);
							OrientationNode oNode2 = nodes[actualNode.edges.get(o).x][actualNode.edges.get(o).y].nodes.get(o2);
							if (oNode1 != null && oNode2 != null){
								logger.finest("  o2 = "+o2+", o = "+o);
								//Tworzymy wiązanie, ale tak zeby chodzic tylem
								if (o == o2 || o == o2.rotateRight() || o == o2.rotateLeft()){
									//idziemy do tyłu idąc od oNode2 do oNode1
									logger.finest("  dodaje polaczenie od oNode2 do oNode1");
									oNode2.edges[oNode2.edgesCount++] = oNode1;
								} else {
									//idziemy do tyłu idąc od oNode1 do oNode2
									logger.finest("  dodaje polaczenie od oNode1 do oNode2");
									oNode1.edges[oNode1.edgesCount++] = oNode2;
								}
								
							}
						}
					}
				}
			}
			
		}
		
		logger.finer("~CreatingGraph",LogMessage.CATEGORY_STACK_TRACE);
		
		//println("dragObject - przechodzimy po grafie");
		
		//Stworzyliśmy graf, teraz trzeba przejść po wierzchołkach
		
		Queue<OrientationNode> oQueue = new LinkedList<OrientationNode>();
		
		//Dodajemy do kolejki wszystkie polozenia początkowe
		if (orientation == null){
			for (Orientation o: Orientation.values()) if (objectNode.edges.get(o) != null && objectNode.edges.get(o).field != null
					&& objectNode.edges.get(o).field.crossable()){
				//println("Sprawdzamy strone "+orientationToString(intToOrientation(i)));
				int startX = objectNode.edges.get(o).x;
				int startY = objectNode.edges.get(o).y;
				Orientation startOrientation = o.reverse();
				OrientationNode startNode = nodes[startX][startY].nodes.get(startOrientation);
				if (startNode != null){
					//println("Dodaje polozenie początkowe x = "+startNode.posX+ " y = "+startNode.posY
					//		+" orientation = "+orientationToString(startNode.orientation));
					startNode.from = null;
					oQueue.add(startNode);
					startNode.visited = true;
				}
			}
		} else {
			int startX = objectNode.edges.get(orientation).x;
			int startY = objectNode.edges.get(orientation).y;
			Orientation startOrientation = orientation.reverse();
			OrientationNode startNode = nodes[startX][startY].nodes.get(startOrientation);
			if (startNode != null){
				logger.finest("Dodaje polozenie początkowe x = "+startNode.posX+ " y = "+startNode.posY
						+" orientation = "+startNode.orientation);
				startNode.from = null;
				oQueue.add(startNode);
				startNode.visited = true;
			}
		}
		
		OrientationNode destinationNode = null;
		
		//TODO powinny OrientationNode byc zalezne od tego na jakim terenie sie znajduja
		//TODO mialyby wtedy jakas wartosc zalezna od ilosci zycia utraconego po wejsciu na nie
		while (oQueue.size() > 0 && destinationNode == null){
			
			OrientationNode actualNode = oQueue.remove();
			
			logger.finest("doszlismy do wierzcholka x = "+actualNode.posX+ " y = "+actualNode.posY
					+" orientation = "+actualNode.orientation);
			
			//Sprawdzamy czy doszlismy do celu
			Node objectDestNode = map[actualNode.posX][actualNode.posY]
					.edges.get(actualNode.orientation);
			//println("objectDestNode x = "+objectDestNode.x+" y = "+objectDestNode.y);
			if (filter.test(objectDestNode)){
				destinationNode = actualNode; 
				break;
			}
			
			//Dodajemy do kolejki kolejne wierzchołki
			//println("actualNode :"+actualNode);
			for (int i = 0;i < actualNode.edgesCount;++i){
				//println("edges[i] : "+actualNode.edges[i]);
				if (!actualNode.edges[i].visited){
				actualNode.edges[i].visited = true;
				actualNode.edges[i].from = actualNode;
				oQueue.add(actualNode.edges[i]);
				//println("Dodalismy kolejne polozenia x = "+actualNode.edges.get(o).posX+ " y = "+actualNode.edges.get(o).posY
				//		+" orientation = "+orientationToString(actualNode.edges.get(o).orientation));
			}
			}
			
		}
		
		NewActionChain result = null;
		
		if (destinationNode != null){
			//println("Doszlismy do celu");
			
			result = new NewActionChain();
			
			OrientationNode pos = destinationNode;
			
			Stack<Action> actionsBackwards = new Stack<Action>();
			
			while (pos != null){
				
				if (pos.from != null){
					if (pos.from.orientation == pos.orientation){
						//roznią się położeniem
						Action action = new Action();
						action.actionType = ActionType.MOVE;
						action.actionMoveOrientation = whichDirection(pos.from.posX,pos.from.posY
								,pos.posX,pos.posY);
						actionsBackwards.add(action);
					} else {
						//różnią się orientacją
						if (pos.from.orientation.rotateRight() == pos.orientation){
							//obrót w prawo
							Action action = new Action();
							action.actionType = ActionType.ROTATE;
							action.actionRotationType = ActionRotationType.ROTATE_RIGHT;
							actionsBackwards.add(action);
						} else if (pos.from.orientation.rotateLeft() == pos.orientation){
							//obrót w lewo
							Action action = new Action();
							action.actionType = ActionType.ROTATE;
							action.actionRotationType = ActionRotationType.ROTATE_LEFT;
							actionsBackwards.add(action);
						} else {
							//println("GameEngine.dragObject - błąd implementacji");
							logger.error("GameEngine.dragObject - blad implementacji");
						}
					}
				} else {
					result.startPos = new UnitPosition(pos.posX,pos.posY,pos.orientation);
				}
				
				pos = pos.from;
			}
			
			while (actionsBackwards.size() > 0){
				result.actions.add(actionsBackwards.pop());
			}
		}
		
		//println("DragObject - koniec");
		
		if (result == null){
			
			if (success != null) success.set(false);
			result = new NewActionChain();
			
		}
		
		return result;
		
	}
	
	//Zapisuje do kolejki drogę jaką trzeba przenieść obiekt z pozycji posX,posY
	//na miejsce które przepuści metoda test obiektu filter, zwraca uwage na przeszkody
	//w postaci innych obiektów lub jednostek
	//ta funkcja dodatkowo posiada parametr crossableFilter i objCrossableFilter
	//dzieki czemu mozna ustalac przez jakie pola mozna przechodzic i przez jakie nie
	//TODO krawedzie powinny byc tez pomiedzy wierzcholkami sasiadujacymi z obiektem
	//TODO o ile obiekt nie znajduje sie na voidzie
	public NewActionChain newDragObject(int posX,int posY,NodeFilter filter,Orientation orientation
			,AtomicBoolean success,NodeFilter crossableFilter,NodeFilterWithObject objCrossableFilter){
		class OrientationNode{
			public int posX;
			public int posY;
			public Orientation orientation;
			public OrientationNode [] edges;
			public int edgesCount;
			boolean visited;
			
			//Skąd się dostaliśmy do tego wierzchołka
			public OrientationNode from;
			
			public OrientationNode(){
				edges = new OrientationNode[8];
				edgesCount = 0;
				visited = false;
			}
			
		};
		
		class OrientationNodes{
			public EnumMap<Orientation,OrientationNode> nodes;
			
			public OrientationNodes(){
				nodes = new EnumMap<Orientation,OrientationNode>(Orientation.class);
			}
			
		};
		
		boolean [][] queued = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		boolean [][] visited = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		OrientationNodes [][] nodes = new OrientationNodes[MAX_MAPSIZEX][MAX_MAPSIZEY];
		
		for (int x = 0;x < MAX_MAPSIZEX;++x)
			for (int y = 0;y < MAX_MAPSIZEY;++y){
				queued[x][y] = false;
				visited[x][y] = false;
			}
		
		logger.finer("dragObject : x = "+posX+" y = "+posY+" orien. ="+((orientation==null)?"dowolna":orientation));
		
		Node objectNode = map[posX][posY];
		
		Queue<Node> queue = new LinkedList<Node>();
		
		logger.finer("CreatingGraph",LogMessage.CATEGORY_STACK_TRACE);
		
		//dodajemy do kolejki pole z obiektem i pola obok obiektu (te po których można chodzić)
		
		//TODO zrobic cos z tym co w nastepnej linijce
		//i te miejsce na ktore wskazuje orientacja przekazana do metody
		for (Orientation o : Orientation.values()) if (crossableFilter.test(objectNode.edges.get(o))
				|| (orientation != null && orientation == o)){
			
			queued[objectNode.edges.get(o).x][objectNode.edges.get(o).y] = true;
			logger.finest(" Dodajemy do kolejki wierzcholek x = "
					+objectNode.edges.get(o).x+" y = "+objectNode.edges.get(o).y);
			queue.add(objectNode.edges.get(o));
					
		}
				
		//To pole dodajemy tylko wtedy gdy mozna po nim chodzic
		if (map[posX][posY].field.crossableTerrain()){
			queued[posX][posY] = true;
			queue.add(objectNode);
		}
		
		while (queue.size() > 0){
			
			Node actualNode = queue.remove();
			
			//println("Sprawdzamy wierzcholek x = "+actualNode.x+" y = "+actualNode.y);
			
			visited[actualNode.x][actualNode.y] = true;
			nodes[actualNode.x][actualNode.y] = new OrientationNodes();
			
			logger.finest(" Wyjmujemy x = "+actualNode.x+ " y = "+actualNode.y);
			
			//Sprawdzamy wszystkie orientacje postaci stojacej na tym polu z obiektem
			//Trzeba sprawdzic we wszystkich kierunkach czy obiekt moze stac obok tego pola
			for (Orientation o : Orientation.values()){
				if (objCrossableFilter.test(actualNode.edges.get(o), objectNode.field.object)
								|| actualNode.edges.get(o) == objectNode){
					OrientationNode oNode = new OrientationNode();
					oNode.orientation = o;
					oNode.posX = actualNode.x;
					oNode.posY = actualNode.y;
					logger.finest("  Dodajemy oNode : x = "+oNode.posX+" y = "+oNode.posY + " orient. = "
								+oNode.orientation);
					nodes[actualNode.x][actualNode.y].nodes.put(o, oNode);
				}
			}
			
			//Łączymy te które nie są nullami
			for (Orientation o : Orientation.values()){
				if (nodes[actualNode.x][actualNode.y].nodes.get(o) != null){
					OrientationNode oNode = nodes[actualNode.x][actualNode.y].nodes.get(o);
					Orientation right = o.rotateRight();
					Orientation left = o.rotateLeft();
					
					//Jak istnieje lewy to łączymy
					if (nodes[actualNode.x][actualNode.y].nodes.get(left) != null){
						oNode.edges[oNode.edgesCount++] = nodes[actualNode.x][actualNode.y].nodes.get(left);
					}
					
					//Jak istnieje prawy to łączymy
					if (nodes[actualNode.x][actualNode.y].nodes.get(right) != null){
						oNode.edges[oNode.edgesCount++] = nodes[actualNode.x][actualNode.y].nodes.get(right);
					}
					
				}
			}
			
			//Sprawdzamy pola obok
			logger.finest(" Sprawdzamy pola obok");
			for (Orientation o : Orientation.values()){
				logger.finest("  "+o);
				if (actualNode.edges.get(o) != null && ((actualNode.edges.get(o).field != null 
						&& (crossableFilter.test(actualNode.edges.get(o)) || actualNode.edges.get(o) == objectNode)) 
						|| (actualNode == objectNode && orientation == o)))
				{
					if (!queued[actualNode.edges.get(o).x][actualNode.edges.get(o).y]){
						//Trzeba dodac wierzcholek do kolejki, jak mozna przez niego przechodzic
						logger.finest("  "+o+" - zakolejkowany");
						if (crossableFilter.test(actualNode.edges.get(o))){
							queued[actualNode.edges.get(o).x][actualNode.edges.get(o).y] = true;
							queue.add(actualNode.edges.get(o));
							//println("Dodajemy do kolejki wierzcholek x = "+actualNode.edges.get(o).x
							//		+" y = "+actualNode.edges.get(o).y);
						}
					} else if (visited[actualNode.edges.get(o).x][actualNode.edges.get(o).y]){
						//Wierzcholek zostal juz dodany, dodajemy dwustronne wiazania
						logger.finest("  "+o+" - odwiedzony");
						for (Orientation o2: Orientation.values()){
							//Musza sie zgadzac orientacje w jednym i drugim
							OrientationNode oNode1 = nodes[actualNode.x][actualNode.y].nodes.get(o2);
							OrientationNode oNode2 = nodes[actualNode.edges.get(o).x][actualNode.edges.get(o).y].nodes.get(o2);
							if (oNode1 != null && oNode2 != null){
								//Tworzymy wiązanie
								oNode1.edges[oNode1.edgesCount++] = oNode2;
								oNode2.edges[oNode2.edgesCount++] = oNode1;
							}
						}
					}
				}
				logger.finest();
			}
			
		}
		
		logger.finer("CreatingGraph",LogMessage.CATEGORY_STACK_TRACE);
		
		//println("dragObject - przechodzimy po grafie");
		
		//Stworzyliśmy graf, teraz trzeba przejść po wierzchołkach
		
		Queue<OrientationNode> oQueue = new LinkedList<OrientationNode>();
		
		//Dodajemy do kolejki wszystkie polozenia początkowe
		if (orientation == null){
			for (Orientation o: Orientation.values()) if (objectNode.edges.get(o) != null && objectNode.edges.get(o).field != null
					&& objectNode.edges.get(o).field.crossable()){
				//println("Sprawdzamy strone "+orientationToString(intToOrientation(i)));
				int startX = objectNode.edges.get(o).x;
				int startY = objectNode.edges.get(o).y;
				Orientation startOrientation = o.reverse();
				OrientationNode startNode = nodes[startX][startY].nodes.get(startOrientation);
				if (startNode != null){
					//println("Dodaje polozenie początkowe x = "+startNode.posX+ " y = "+startNode.posY
					//		+" orientation = "+orientationToString(startNode.orientation));
					startNode.from = null;
					oQueue.add(startNode);
					startNode.visited = true;
				}
			}
		} else {
			int startX = objectNode.edges.get(orientation).x;
			int startY = objectNode.edges.get(orientation).y;
			Orientation startOrientation = orientation.reverse();
			OrientationNode startNode = nodes[startX][startY].nodes.get(startOrientation);
			if (startNode != null){
				logger.finest("Dodaje polozenie początkowe x = "+startNode.posX+ " y = "+startNode.posY
						+" orientation = "+startNode.orientation);
				startNode.from = null;
				oQueue.add(startNode);
				startNode.visited = true;
			}
		}
		
		OrientationNode destinationNode = null;
		
		//TODO powinny OrientationNode byc zalezne od tego na jakim terenie sie znajduja
		//TODO mialyby wtedy jakas wartosc zalezna od ilosci zycia utraconego po wejsciu na nie
		while (oQueue.size() > 0 && destinationNode == null){
			
			OrientationNode actualNode = oQueue.remove();
			
			logger.finest("doszlismy do wierzcholka x = "+actualNode.posX+ " y = "+actualNode.posY
					+" orientation = "+actualNode.orientation);
			
			//Sprawdzamy czy doszlismy do celu
			Node objectDestNode = map[actualNode.posX][actualNode.posY]
					.edges.get(actualNode.orientation);
			//println("objectDestNode x = "+objectDestNode.x+" y = "+objectDestNode.y);
			if (filter.test(objectDestNode)){
				destinationNode = actualNode; 
				break;
			}
			
			//Dodajemy do kolejki kolejne wierzchołki
			//println("actualNode :"+actualNode);
			for (int i = 0;i < actualNode.edgesCount;++i){
				//println("edges[i] : "+actualNode.edges[i]);
				if (!actualNode.edges[i].visited){
				actualNode.edges[i].visited = true;
				actualNode.edges[i].from = actualNode;
				oQueue.add(actualNode.edges[i]);
				//println("Dodalismy kolejne polozenia x = "+actualNode.edges.get(o).posX+ " y = "+actualNode.edges.get(o).posY
				//		+" orientation = "+orientationToString(actualNode.edges.get(o).orientation));
			}
			}
			
		}
		
		NewActionChain result = null;
		
		if (destinationNode != null){
			//println("Doszlismy do celu");
			
			result = new NewActionChain();
			
			OrientationNode pos = destinationNode;
			
			Stack<Action> actionsBackwards = new Stack<Action>();
			
			while (pos != null){
				
				if (pos.from != null){
					if (pos.from.orientation == pos.orientation){
						//roznią się położeniem
						Action action = new Action();
						action.actionType = ActionType.MOVE;
						action.actionMoveOrientation = whichDirection(pos.from.posX,pos.from.posY
								,pos.posX,pos.posY);
						actionsBackwards.add(action);
					} else {
						//różnią się orientacją
						if (pos.from.orientation.rotateRight() == pos.orientation){
							//obrót w prawo
							Action action = new Action();
							action.actionType = ActionType.ROTATE;
							action.actionRotationType = ActionRotationType.ROTATE_RIGHT;
							actionsBackwards.add(action);
						} else if (pos.from.orientation.rotateLeft() == pos.orientation){
							//obrót w lewo
							Action action = new Action();
							action.actionType = ActionType.ROTATE;
							action.actionRotationType = ActionRotationType.ROTATE_LEFT;
							actionsBackwards.add(action);
						} else {
							//println("GameEngine.dragObject - błąd implementacji");
							logger.error("GameEngine.dragObject - błąd implementacji");
						}
					}
				} else {
					result.startPos = new UnitPosition(pos.posX,pos.posY,pos.orientation);
				}
				
				pos = pos.from;
			}
			
			while (actionsBackwards.size() > 0){
				result.actions.add(actionsBackwards.pop());
			}
		}
		
		//println("DragObject - koniec");
		
		if (result == null){
			
			if (success != null) success.set(false);
			result = new NewActionChain();
			
		}
		
		
		return result;
		
	}
	
	//Zapisuje do kolejki drogę jaką trzeba przenieść obiekt z pozycji posX,posY
	//na miejsce które przepuści metoda test obiektu filter, zwraca uwage na przeszkody
	//w postaci innych obiektów lub jednostek
	//TODO krawedzie powinny byc tez pomiedzy wierzcholkami sasiadujacymi z obiektem
	//TODO o ile obiekt nie znajduje sie na voidzie
	public NewActionChain newDragObject(int posX,int posY,NodeFilter filter,Orientation orientation
			,AtomicBoolean success){
		class OrientationNode{
			public int posX;
			public int posY;
			public Orientation orientation;
			public OrientationNode [] edges;
			public int edgesCount;
			boolean visited;
			
			//Skąd się dostaliśmy do tego wierzchołka
			public OrientationNode from;
			
			public OrientationNode(){
				edges = new OrientationNode[8];
				edgesCount = 0;
				visited = false;
			}
			
		};
		
		class OrientationNodes{
			public EnumMap<Orientation,OrientationNode> nodes;
			
			public OrientationNodes(){
				nodes = new EnumMap<Orientation,OrientationNode>(Orientation.class);
			}
			
		};
		
		boolean [][] queued = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		boolean [][] visited = new boolean[MAX_MAPSIZEX][MAX_MAPSIZEY];
		OrientationNodes [][] nodes = new OrientationNodes[MAX_MAPSIZEX][MAX_MAPSIZEY];
		
		for (int x = 0;x < MAX_MAPSIZEX;++x)
			for (int y = 0;y < MAX_MAPSIZEY;++y){
				queued[x][y] = false;
				visited[x][y] = false;
			}
		
		logger.finer("dragObject : x = "+posX+" y = "+posY+" orien. ="+((orientation==null)?"dowolna":orientation));
		
		logger.finer("CreatingGraph",LogMessage.CATEGORY_STACK_TRACE);
		
		Node objectNode = map[posX][posY];
		
		Queue<Node> queue = new LinkedList<Node>();
		
		//dodajemy do kolejki pole z obiektem i pola obok obiektu (te po których można chodzić)
		
		//TODO zrobic cos z tym co w nastepnej linijce
		//i te miejsce na ktore wskazuje orientacja przekazana do metody
		for (Orientation o : Orientation.values()) if ((objectNode.edges.get(o) != null
				&& objectNode.edges.get(o).field != null && objectNode.edges.get(o).field.crossable())
				|| (orientation != null && orientation == o)){
			
			queued[objectNode.edges.get(o).x][objectNode.edges.get(o).y] = true;
			logger.finest(" Dodajemy do kolejki wierzcholek x = "
					+objectNode.edges.get(o).x+" y = "+objectNode.edges.get(o).y);
			queue.add(objectNode.edges.get(o));
					
		}
				
		//To pole dodajemy tylko wtedy gdy mozna po nim chodzic
		if (map[posX][posY].field.crossableTerrain()){
			queued[posX][posY] = true;
			queue.add(objectNode);
		}
		
		while (queue.size() > 0){
			
			Node actualNode = queue.remove();
			
			//println("Sprawdzamy wierzcholek x = "+actualNode.x+" y = "+actualNode.y);
			
			visited[actualNode.x][actualNode.y] = true;
			nodes[actualNode.x][actualNode.y] = new OrientationNodes();
			
			logger.finest(" Wyjmujemy x = "+actualNode.x+ " y = "+actualNode.y);
			
			//Sprawdzamy wszystkie orientacje postaci stojacej na tym polu z obiektem
			//Trzeba sprawdzic we wszystkich kierunkach czy obiekt moze stac obok tego pola
			for (Orientation o : Orientation.values()){
				if ((actualNode.edges.get(o) != null && actualNode.edges.get(o).field != null
						&& actualNode.edges.get(o).field.crossableByObject(objectNode.field.object))
								|| actualNode.edges.get(o) == objectNode){
					OrientationNode oNode = new OrientationNode();
					oNode.orientation = o;
					oNode.posX = actualNode.x;
					oNode.posY = actualNode.y;
					logger.finest("  Dodajemy oNode : x = "+oNode.posX+" y = "+oNode.posY + " orient. = "
								+oNode.orientation);
					nodes[actualNode.x][actualNode.y].nodes.put(o, oNode);
				}
			}
			
			//Łączymy te które nie są nullami
			for (Orientation o : Orientation.values()){
				if (nodes[actualNode.x][actualNode.y].nodes.get(o) != null){
					OrientationNode oNode = nodes[actualNode.x][actualNode.y].nodes.get(o);
					Orientation right = o.rotateRight();
					Orientation left = o.rotateLeft();
					
					//Jak istnieje lewy to łączymy
					if (nodes[actualNode.x][actualNode.y].nodes.get(left) != null){
						oNode.edges[oNode.edgesCount++] = nodes[actualNode.x][actualNode.y].nodes.get(left);
					}
					
					//Jak istnieje prawy to łączymy
					if (nodes[actualNode.x][actualNode.y].nodes.get(right) != null){
						oNode.edges[oNode.edgesCount++] = nodes[actualNode.x][actualNode.y].nodes.get(right);
					}
					
				}
			}
			
			//Sprawdzamy pola obok
			logger.finest(" Sprawdzamy pola obok");
			for (Orientation o : Orientation.values()){
				if (actualNode.edges.get(o) != null && ((actualNode.edges.get(o).field != null 
						&& (actualNode.edges.get(o).field.crossable() || actualNode.edges.get(o) == objectNode)) 
						|| (actualNode == objectNode && orientation == o)))
				{
					if (!queued[actualNode.edges.get(o).x][actualNode.edges.get(o).y]){
						//Trzeba dodac wierzcholek do kolejki, jak mozna przez niego przechodzic
						logger.finest("  "+o+" - zakolejkowany");
						if (actualNode.edges.get(o).field.crossable()){
							queued[actualNode.edges.get(o).x][actualNode.edges.get(o).y] = true;
							queue.add(actualNode.edges.get(o));
							//println("Dodajemy do kolejki wierzcholek x = "+actualNode.edges.get(o).x
							//		+" y = "+actualNode.edges.get(o).y);
						}
					} else if (visited[actualNode.edges.get(o).x][actualNode.edges.get(o).y]){
						//Wierzcholek zostal juz dodany, dodajemy dwustronne wiazania
						logger.finest("  "+o+" - odwiedzony");
						for (Orientation o2: Orientation.values()){
							//Musza sie zgadzac orientacje w jednym i drugim
							OrientationNode oNode1 = nodes[actualNode.x][actualNode.y].nodes.get(o2);
							OrientationNode oNode2 = nodes[actualNode.edges.get(o).x][actualNode.edges.get(o).y].nodes.get(o2);
							if (oNode1 != null && oNode2 != null){
								//Tworzymy wiązanie
								oNode1.edges[oNode1.edgesCount++] = oNode2;
								oNode2.edges[oNode2.edgesCount++] = oNode1;
							}
						}
					}
				}
				logger.finest();
			}
			
		}
		
		logger.finer("~CreatingGraph",LogMessage.CATEGORY_STACK_TRACE);
		
		//println("dragObject - przechodzimy po grafie");
		
		//Stworzyliśmy graf, teraz trzeba przejść po wierzchołkach
		
		Queue<OrientationNode> oQueue = new LinkedList<OrientationNode>();
		
		//Dodajemy do kolejki wszystkie polozenia początkowe
		if (orientation == null){
			for (Orientation o: Orientation.values()) if (objectNode.edges.get(o) != null && objectNode.edges.get(o).field != null
					&& objectNode.edges.get(o).field.crossable()){
				//println("Sprawdzamy strone "+orientationToString(intToOrientation(i)));
				int startX = objectNode.edges.get(o).x;
				int startY = objectNode.edges.get(o).y;
				Orientation startOrientation = o.reverse();
				OrientationNode startNode = nodes[startX][startY].nodes.get(startOrientation);
				if (startNode != null){
					//println("Dodaje polozenie początkowe x = "+startNode.posX+ " y = "+startNode.posY
					//		+" orientation = "+orientationToString(startNode.orientation));
					startNode.from = null;
					oQueue.add(startNode);
					startNode.visited = true;
				}
			}
		} else {
			int startX = objectNode.edges.get(orientation).x;
			int startY = objectNode.edges.get(orientation).y;
			Orientation startOrientation = orientation.reverse();
			OrientationNode startNode = nodes[startX][startY].nodes.get(startOrientation);
			if (startNode != null){
				logger.finest("Dodaje polozenie początkowe x = "+startNode.posX+ " y = "+startNode.posY
						+" orientation = "+startNode.orientation);
				startNode.from = null;
				oQueue.add(startNode);
				startNode.visited = true;
			}
		}
		
		OrientationNode destinationNode = null;
		
		//TODO powinny OrientationNode byc zalezne od tego na jakim terenie sie znajduja
		//TODO mialyby wtedy jakas wartosc zalezna od ilosci zycia utraconego po wejsciu na nie
		while (oQueue.size() > 0 && destinationNode == null){
			
			OrientationNode actualNode = oQueue.remove();
			
			logger.finest("doszlismy do wierzcholka x = "+actualNode.posX+ " y = "+actualNode.posY
					+" orientation = "+actualNode.orientation);
			
			//Sprawdzamy czy doszlismy do celu
			Node objectDestNode = map[actualNode.posX][actualNode.posY]
					.edges.get(actualNode.orientation);
			//println("objectDestNode x = "+objectDestNode.x+" y = "+objectDestNode.y);
			if (filter.test(objectDestNode)){
				destinationNode = actualNode; 
				break;
			}
			
			//Dodajemy do kolejki kolejne wierzchołki
			//println("actualNode :"+actualNode);
			for (int i = 0;i < actualNode.edgesCount;++i){
				//println("edges[i] : "+actualNode.edges[i]);
				if (!actualNode.edges[i].visited){
				actualNode.edges[i].visited = true;
				actualNode.edges[i].from = actualNode;
				oQueue.add(actualNode.edges[i]);
				//println("Dodalismy kolejne polozenia x = "+actualNode.edges.get(o).posX+ " y = "+actualNode.edges.get(o).posY
				//		+" orientation = "+orientationToString(actualNode.edges.get(o).orientation));
			}
			}
			
		}
		
		NewActionChain result = null;
		
		if (destinationNode != null){
			//println("Doszlismy do celu");
			
			result = new NewActionChain();
			
			OrientationNode pos = destinationNode;
			
			Stack<Action> actionsBackwards = new Stack<Action>();
			
			while (pos != null){
				
				if (pos.from != null){
					if (pos.from.orientation == pos.orientation){
						//roznią się położeniem
						Action action = new Action();
						action.actionType = ActionType.MOVE;
						action.actionMoveOrientation = whichDirection(pos.from.posX,pos.from.posY
								,pos.posX,pos.posY);
						actionsBackwards.add(action);
					} else {
						//różnią się orientacją
						if (pos.from.orientation.rotateRight() == pos.orientation){
							//obrót w prawo
							Action action = new Action();
							action.actionType = ActionType.ROTATE;
							action.actionRotationType = ActionRotationType.ROTATE_RIGHT;
							actionsBackwards.add(action);
						} else if (pos.from.orientation.rotateLeft() == pos.orientation){
							//obrót w lewo
							Action action = new Action();
							action.actionType = ActionType.ROTATE;
							action.actionRotationType = ActionRotationType.ROTATE_LEFT;
							actionsBackwards.add(action);
						} else {
							//println("GameEngine.dragObject - błąd implementacji");
							logger.error("GameEngine.dragObject - błąd implementacji");
						}
					}
				} else {
					result.startPos = new UnitPosition(pos.posX,pos.posY,pos.orientation);
				}
				
				pos = pos.from;
			}
			
			while (actionsBackwards.size() > 0){
				result.actions.add(actionsBackwards.pop());
			}
		}
		
		//println("DragObject - koniec");
		
		if (result == null){
			
			if (success != null) success.set(false);
			result = new NewActionChain();
			
		}
		
		return result;
		
	}
	
	public Orientation whichDirection(int fromX,int fromY,int toX,int toY){
		
		boolean found = false;
		Orientation orientation = null;
		for (Orientation o : Orientation.values()){
			if (found) break;
			if (map[fromX][fromY].edges.get(o).x == toX && map[fromX][fromY].edges.get(o).y == toY){
				found = true;
				orientation = o;
			}
		}
		
		if (!found){
			logger.severe("GameEngine.whichDirection - nieprawidłowe parametry fromX = "+fromX+" fromY = "+fromY
					+" toX = "+toX+" toY = "+toY);
			logger.severe(new Exception().getStackTrace().toString());
			return Orientation.E;
		} else return orientation;
		
	}
	
	public NewActionChain newDragAnyBackwards(Unit unit,LinkedList<Node> objects
			,NodeFilter where,AtomicBoolean success,NodeFilter crossableFilter,NodeFilterWithObject objCrossableFilter){

		logger.fine("newDragAnyBackwards",LogMessage.CATEGORY_STACK_TRACE);
		
		int posX = unit.posX;
		int posY = unit.posY;
		
		NewActionChain actionChain = new NewActionChain(unit);
		
		AtomicBoolean successTemp = new AtomicBoolean(false);
		
		if (unit.action.equalsIgnoreCase("dragging")){
			Node objectNode = map[posX][posY].edges.get(unit.orientation);
			boolean found = false;
			//Sprawdzamy czy ten obiekt nalezy do przekazanych do funkcji
			for (Node node : objects){
				if (objectNode == node){
					found = true;
					break;
				}
			}
			if (found){
				//jezeli juz trzymamy obiekt to wyszukujemy trase nie wymagającą puszczenia go
				logger.fine("probujemy dojsc do celu nie puszczajac obiektu");
				successTemp.set(true);
				actionChain = newDragObjectBackwards(map[posX][posY].edges.get(unit.orientation).x
						,map[posX][posY].edges.get(unit.orientation).y,where
						,unit.orientation.reverse(),successTemp,crossableFilter,objCrossableFilter);
			}
		}
		for (int i = 0;i < objects.size() && !successTemp.get();++i){
			logger.fine("Probujemy przeniesc obiekt z pozycji x = "+objects.get(i).x
					+" y = "+objects.get(i).y);
			Orientation o = null;
			if ((o = nearPosition(objects.get(i).x,objects.get(i).y,posX,posY)) != null){
				logger.fine("Jestesmy obok obiektu, wiec powinnismy najpierw sprobowac chwycic go od tej strony");
				//Jestesmy obok obiektu, wiec powinnismy go chwytac od tej strony
				successTemp.set(true);
				actionChain = newDragObjectBackwards(objects.get(i).x,objects.get(i).y,where,o,successTemp,crossableFilter,objCrossableFilter);
				if (!successTemp.get()){
					logger.fine("Jak nie mozna z tej strony, to próbujemy z innej");
				}
			}
			if (!successTemp.get()){
				logger.fine("Probujemy chwycic obiekt z innej strony");
				successTemp.set(true);
				actionChain = newDragObjectBackwards(objects.get(i).x,objects.get(i).y,where,null,successTemp,crossableFilter,objCrossableFilter);
			}
		}
		
		if (!successTemp.get()){
			logger.fine("Nie udalo sie wykonac dragAny");
			if (success != null){
				success.set(false);
			}
		}
		
		logger.fine("~newDragAnyBackwards",LogMessage.CATEGORY_STACK_TRACE);
		
		return actionChain;
		
	}
	
	public NewActionChain newFullDragAnyThroughNulls(Unit unit,LinkedList<Node> diamonds
			,NodeFilter where,AtomicBoolean success,NodeFilter crossableFilter,NodeFilterWithObject objCrossableFilter){
		
		logger.fine("newFullDragAnyThroughNulls",LogMessage.CATEGORY_STACK_TRACE);
		
		boolean backwards = false;
		boolean forwards = false;
		AtomicBoolean successTemp = new AtomicBoolean(true);
		
		NewActionChain fullDragActionBackwards = null;
		NewActionChain fullDragActionForwards = null;
		
		NewActionChain dragActionChainBackwards = newDragAnyBackwards(unit,diamonds,
				(Node n) -> n.field != null && n.field.building == Building.ALTAR
				&& n.field.buildingPlayer == playerID,successTemp, (Node n) -> (n != null && n.field == null) || crossableFilter.test(n)
					,(Node n,MapField.Object obj) -> (n != null && n.field == null) || objCrossableFilter.test(n,obj));
		
		if (successTemp.get()){
			
			fullDragActionBackwards = constructFullDragActionChain(unit,dragActionChainBackwards,successTemp);
			
			if (successTemp.get()){
				backwards = true;
			}
			
		}
		
		successTemp.set(true);
		NewActionChain dragActionChainForwards = newDragAny(unit,diamonds,
				(Node n) -> n.field != null && n.field.building == Building.ALTAR
				&& n.field.buildingPlayer == playerID,success,crossableFilter
					,objCrossableFilter);
		
		if (successTemp.get()){
			
			fullDragActionForwards = constructFullDragActionChain(unit,dragActionChainForwards,successTemp);
			
			if (successTemp.get()){
				forwards = true;
			}
			
		}
		
		NewActionChain result = new NewActionChain(unit);
		
		if (forwards && backwards){
			HealthLoss forwardLoss = fullDragActionForwards.calculateHealthLoss();
			HealthLoss backwardLoss = fullDragActionBackwards.calculateHealthLoss();
			if (forwardLoss.max+forwardLoss.min < backwardLoss.max+backwardLoss.min){
				//lepiej isc tylem przez nulle
				result = dragActionChainForwards;
			} else {
				result = dragActionChainBackwards;
			}
		} else if (forwards){
			result = dragActionChainForwards;
		} else if (backwards){
			result = dragActionChainBackwards;
		} else {
			success.set(false);
		}
		
		logger.fine("newFullDragAnyThroughNulls",LogMessage.CATEGORY_STACK_TRACE);
		
		return result;
		
	}
	
	public NewActionChain newDragAny(Unit unit,LinkedList<Node> objects
			,NodeFilter where,AtomicBoolean success,NodeFilter crossableFilter,NodeFilterWithObject objCrossableFilter){

		logger.fine("newDragAny",LogMessage.CATEGORY_STACK_TRACE);
		
		int posX = unit.posX;
		int posY = unit.posY;
		
		NewActionChain actionChain = new NewActionChain(unit);
		
		AtomicBoolean successTemp = new AtomicBoolean(false);
		
		if (unit.action.equalsIgnoreCase("dragging")){
			Node objectNode = map[posX][posY].edges.get(unit.orientation);
			boolean found = false;
			//Sprawdzamy czy ten obiekt nalezy do przekazanych do funkcji
			for (Node node : objects){
				if (objectNode == node){
					found = true;
					break;
				}
			}
			if (found){
				//jezeli juz trzymamy obiekt to wyszukujemy trase nie wymagającą puszczenia go
				logger.fine("probujemy dojsc do celu nie puszczajac obiektu");
				successTemp.set(true);
				actionChain = newDragObject(map[posX][posY].edges.get(unit.orientation).x
						,map[posX][posY].edges.get(unit.orientation).y,where
						,unit.orientation.reverse(),successTemp,crossableFilter,objCrossableFilter);
			}
		}
		
		for (int i = 0;i < objects.size() && !successTemp.get();++i){
			logger.fine("Probujemy przeniesc obiekt z pozycji x = "+objects.get(i).x
					+" y = "+objects.get(i).y);
			Orientation o = null;
			if ((o = nearPosition(objects.get(i).x,objects.get(i).y,posX,posY)) != null){
				logger.fine("Jestesmy obok obiektu, wiec powinnismy najpierw sprobowac chwycic go od tej strony");
				//Jestesmy obok obiektu, wiec powinnismy go chwytac od tej strony
				successTemp.set(true);
				actionChain = newDragObject(objects.get(i).x,objects.get(i).y,where,o,successTemp,crossableFilter,objCrossableFilter);
				if (!successTemp.get()){
					logger.fine("Jak nie mozna z tej strony, to próbujemy z innej");
				}
			}
			if (!successTemp.get()){
				logger.fine("Probujemy chwycic obiekt z innej strony");
				successTemp.set(true);
				actionChain = newDragObject(objects.get(i).x,objects.get(i).y,where,null,successTemp,crossableFilter,objCrossableFilter);
			}
		}
		
		if (!successTemp.get()){
			logger.fine("Nie udalo sie wykonac dragAny");
			if (success != null){
				success.set(false);
			}
			actionChain = new NewActionChain(unit);
		}
		
		logger.fine("~newDragAny",LogMessage.CATEGORY_STACK_TRACE);
		
		return actionChain;
		
	}
	
	public NewActionChain newDragAny(Unit unit,LinkedList<Node> objects,NodeFilter where
			,AtomicBoolean success){
		
		logger.fine("newDragAny",LogMessage.CATEGORY_STACK_TRACE);

		int posX = unit.posX;
		int posY = unit.posY;
		
		NewActionChain actionChain = new NewActionChain(unit);
		
		AtomicBoolean successTemp = new AtomicBoolean(false);
		
		if (unit.action.equalsIgnoreCase("dragging")){
			Node objectNode = map[posX][posY].edges.get(unit.orientation);
			boolean found = false;
			//Sprawdzamy czy ten obiekt nalezy do przekazanych do funkcji
			for (Node node : objects){
				if (objectNode == node){
					found = true;
					break;
				}
			}
			if (found){
				//jezeli juz trzymamy obiekt to wyszukujemy trase nie wymagającą puszczenia go
				logger.fine("probujemy dojsc do celu nie puszczajac obiektu");
				successTemp.set(true);
				actionChain = newDragObject(map[posX][posY].edges.get(unit.orientation).x
						,map[posX][posY].edges.get(unit.orientation).y,where
						,unit.orientation.reverse(),successTemp);
			}
		}
		
		for (int i = 0;i < objects.size() && !successTemp.get();++i){
			logger.fine("Probujemy przeniesc obiekt z pozycji x = "+objects.get(i).x
					+" y = "+objects.get(i).y);
			Orientation o = null;
			if ((o = nearPosition(objects.get(i).x,objects.get(i).y,posX,posY)) != null){
				logger.fine("Jestesmy obok obiektu, wiec powinnismy najpierw sprobowac chwycic go od tej strony");
				//Jestesmy obok obiektu, wiec powinnismy go chwytac od tej strony
				successTemp.set(true);
				actionChain = newDragObject(objects.get(i).x,objects.get(i).y,where,o,successTemp);
				if (!successTemp.get()){
					logger.fine("Jak nie mozna z tej strony, to próbujemy z innej");
				}
			}
			if (!successTemp.get()){
				logger.fine("Probujemy chwycic obiekt z innej strony");
				successTemp.set(true);
				actionChain = newDragObject(objects.get(i).x,objects.get(i).y,where,null,successTemp);
			}
		}
		
		if (!successTemp.get()){
			logger.fine("Nie udalo sie wykonac dragAny");
			if (success != null){
				success.set(false);
			}
			actionChain = new NewActionChain(unit);
		}
		
		logger.fine("~newDragAny",LogMessage.CATEGORY_STACK_TRACE);
		
		return actionChain;
		
	}
	
	public NewActionChain constructFullDragActionChain(Unit unit,NewActionChain dragActionChain,AtomicBoolean success){
		
		logger.finer("constructFullDragActionChain",LogMessage.CATEGORY_STACK_TRACE);
		
		boolean dragging = false;
		
		NewActionChain dropActionChain = new NewActionChain();
		if (!dragActionChain.startPos.equals(unit.getPos())){
			dropActionChain = dropIfDragging(unit,null);
			logger.finer("dropActionChain : ");
			logger.finer(dropActionChain.toString());
		} else if (unit.action.equalsIgnoreCase("dragging")){
			dragging = true;
		}
		
		NewActionChain moveActionChain = newMoveTo(unit,dragActionChain.startPos.posX
				,dragActionChain.startPos.posY,success);
		logger.finer("moveActionChain : ");
		logger.finer(moveActionChain.toString());
		
		NewActionChain rotateActionChain = newRotate(unit.orientation,dragActionChain.startPos.orientation);
		
		logger.finer("rotateActionChain : ");
		logger.finer(rotateActionChain.toString());
		
		if (success.get()){
		
			NewActionChain actionChain = new NewActionChain(unit);
			actionChain.add(dropActionChain);
			actionChain.add(moveActionChain);
			actionChain.add(rotateActionChain);
			if (!dragging) actionChain.add(newDrag());
			actionChain.add(dragActionChain);
			//TODO Przed tym mozemy sie juz uleczyc
			actionChain.add(newDrop());
			
			logger.finer("~constructFullDragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			
			return actionChain;
		
		} else {
			
			logger.finer("~constructFullDragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			return new NewActionChain(unit);
			
		}
		
	}
	
	public NewActionChain newDragAnyFirstNearNull(Unit unit,LinkedList<Node> objects,NodeFilter where
			,AtomicBoolean success){
		
		int posX = unit.posX;
		int posY = unit.posY;
		
		NewActionChain actionChain = new NewActionChain(unit);
		
		for (int i = 0;i < objects.size();++i){
			logger.finest("x = "+objects.get(i).x+" y = "+objects.get(i).y);
		}
		
		AtomicBoolean successTemp = new AtomicBoolean(false);
		
		if (unit.action.equalsIgnoreCase("dragging")){
			Node objectNode = map[posX][posY].edges.get(unit.orientation);
			boolean found = false;
			//Sprawdzamy czy ten obiekt nalezy do przekazanych do funkcji
			for (Node node : objects){
				if (objectNode == node){
					found = true;
					break;
				}
			}
			//jezeli juz trzymamy obiekt to wyszukujemy trase nie wymagającą puszczenia go
			if (found){
				successTemp.set(true);
				actionChain = newDragObject(map[posX][posY].edges.get(unit.orientation).x
						,map[posX][posY].edges.get(unit.orientation).y,where
						,unit.orientation.reverse(),successTemp);
			}
		}
		
		//Sprawdzamy czy nie ma za jakimis kamieniami nulli
		for (int i = 0;i < objects.size() && !successTemp.get();++i){
			if (nearNull(objects.get(i).x,objects.get(i).y) != null){
				//Są jakieś nulle za kamieniami
				Orientation o = null;
				successTemp.set(true);
				if ((o = nearPosition(objects.get(i).x,objects.get(i).y,posX,posY)) != null){
					//Jestesmy obok kamienia, wiec powinnismy go chwytac od tej strony
					actionChain = newDragObject(objects.get(i).x,objects.get(i).y,where,o,successTemp);
				} else {
					actionChain = newDragObject(objects.get(i).x,objects.get(i).y,where,null,successTemp);
				}
			}
		}
		
		for (int i = 0;i < objects.size() && actionChain == null;++i){
			Orientation o = null;
			successTemp.set(true);
			if ((o = nearPosition(objects.get(i).x,objects.get(i).y,posX,posY)) != null){
				//Jestesmy obok kamienia, wiec powinnismy go chwytac od tej strony
				actionChain = newDragObject(objects.get(i).x,objects.get(i).y,where,o,successTemp);
			} else {
				actionChain = newDragObject(objects.get(i).x,objects.get(i).y,where,null,successTemp);
			}
		}
		
		if (!successTemp.get()){
			if (success != null) success.set(false);
		}
		
		return actionChain;
		
	}
	
	Integer firstUnitID = null;
	
	//TODO jak nic sie nie zmienilo to robimy ta sama akcje
	//Action
	
	/* wersja 1.0 */
	public void primaryUnit(GameStatus gameStatus){
		
		Unit unit = units.get(gameStatus.units.get(0).id);
		
		int posX = unit.posX;
		int posY = unit.posY;
		
		logger.info("Akcja priorytetowa : "+unit.priorityAction);
		
		//TODO Pozniej usunac to, jednostki nie powinny sobie nawzajem przeszkadzac
		//Umieszczamy inne jednostki na planszy zeby nie przeszkadzaly nam
		for (Unit otherUnit : units.values()) if (unit.id != gameStatus.units.get(0).id){
			if (map[otherUnit.posX][otherUnit.posY].field != null){
				map[otherUnit.posX][otherUnit.posY].field.unit = new SeenUnit();
				map[otherUnit.posX][otherUnit.posY].field.unit.orientation = otherUnit.orientation;
				map[otherUnit.posX][otherUnit.posY].field.unit.hp = otherUnit.hp;
				map[otherUnit.posX][otherUnit.posY].field.unit.player = otherUnit.player;
			}
		}
		
		LinkedList<Node> stones = findHPClosestAllObjects(posX,posY,MapField.Object.STONE);
		
		LinkedList<Node> diamonds = findHPClosestAllObjects(posX,posY,MapField.Object.DIAMOND);
		
		LinkedList<Node> altars = findHPClosestAllPlayerBuildings(posX,posY,unit.player,Building.ALTAR);
		
		//Tutaj bedzie zapisana akcja do wykonania
		NewActionChain actionChain = null;
		
		if (unit.checkHealing){
			if (unit.hp < 100 && gameStatus.points > 0){
				logger.info("Leczymy sie jak juz jestesmy kolo oltarza.");
				actionChain = new NewActionChain(unit);
				actionChain.add(newHeal());
			}
			unit.checkHealing = false;
		}
		
		//TODO trzeba sprawdzac w ile ruchow dojdziemy do oltarza trzymajac diament, wtedy przy leczeniu
		//TODO brac mniejsza wartosc
		//TODO to bedzie akcja o najwiekszym priorytecie tutaj
		boolean goingToAltar = false;
		
		if ((actionChain == null || actionChain.actions.size() == 0) && diamonds.size() > 0){
			//Nie trzymamy diamentu ale mamy dostep do oltarza i do diamentu
			logger.info("Mogę dojść do diamentu. Sprawdzam czy moge zaniesc jakis do oltarza");
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain dragActionChain = newDragAny(unit,diamonds,
					(Node n) -> n.field != null && n.field.building == Building.ALTAR
					&& n.field.buildingPlayer == playerID,success);
			
			logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			logger.fine("dragActionChain : ");
			logger.fine(dragActionChain.toString());
			logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			
			if (!success.get()){
				logger.info("Nie mozna przerzucic zadnego diamentu do oltarza. Sprawdzamy czy to wina kamieni");
				
				dragActionChain = newDragAny(unit,diamonds,
					(Node n) -> n.field != null && n.field.building == Building.ALTAR
					&& n.field.buildingPlayer == playerID,success,(Node n) -> n != null && n.field != null
					&& n.field.crossableDisregardingObject(MapField.Object.STONE)
					,(Node n,MapField.Object obj) -> n != null && n.field != null 
					&& n.field.crossableByObjectDisregardingObject(obj,MapField.Object.STONE));
				
				logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
				logger.fine("dragActionChain : ");
				logger.fine(dragActionChain.toString());
				logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
				
				LinkedList<Node> path = dragActionChain.crossedNodesDragging((Node n) -> true);
				LinkedList<Node> stonesToThrowOut = dragActionChain.crossedNodesDragging((Node n) -> n != null 
						&& n.field != null && n.field.object == MapField.Object.STONE);
				
				logger.info();

				logger.fine();
				logger.fine("Musimy oczyscic ścieżkę : ");
				logger.fine("Path",LogMessage.CATEGORY_STACK_TRACE);
				for (Node n : path){
					logger.fine(" x = "+n.x+" y = "+n.y);
				}
				logger.fine("~Path",LogMessage.CATEGORY_STACK_TRACE);
				
				logger.fine();
				logger.fine("Sa na niej kamienie : ");
				logger.fine("StonesToThrowOut",LogMessage.CATEGORY_STACK_TRACE);
				for (Node n : stonesToThrowOut){
					logger.fine(" x = "+n.x+" y = "+n.y);
				}
				logger.fine("~StonesToThrowOut",LogMessage.CATEGORY_STACK_TRACE);
				
				for (Node n : path){
					n.marked = true;
				}
				
				
				
				success.set(true);
				dragActionChain = newDragAny(unit,stonesToThrowOut,
						(Node n) -> n.marked == false,success);
						
				for (Node n : path){
					n.marked = false;
				}
				
				if (success.get()){
					
					actionChain = constructFullDragActionChain(unit,dragActionChain,success);
					
					if (success.get()){
						logger.info("Mozna wywalic kamien tak, aby wyjac diament.");
					}
					
				} else {
					logger.info("Nie udalo sie wywalic kamienia.");
				}
				
				
			} else {
				//Udalo sie akcja mozemy ustawic actionChain
				
				actionChain = constructFullDragActionChain(unit,dragActionChain,success);
				
				if (success.get()){
					//Mozna przeniesc diament do oltarza
					logger.info("Mozna przeniesc diament do oltara.");
					goingToAltar = true;
					
					if (actionChain.actions.size() == 1){
						//Zostalo nam tylko dropowanie, trzeba wlaczyc checkHealing
						unit.checkHealing = true;
					}
					
				}
				
			}
					
			//jak konczymy to trzeba ustawic checkHealing na true
			
		}
		
		if (actionChain == null || actionChain.actions.size() == 0){
			//Nie mamy diamentow, ktore moglibysmy wrzucic do oltarza, wiec przeszukujemy plansze
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain exploreActionChain = newExplore(unit,success);
			
			if (success.get()){
				//Sprawdzamy czy nie trzymamy przedmiotu
				logger.info("Explorujemy mape.");
				
				boolean dragging = false;
				if (unit.action.equalsIgnoreCase("dragging")){
					logger.info("Ale najpierw musimy wyrzucic przedmiot");
					dragging = true;
				}
				
				actionChain = new NewActionChain();
				if (dragging){
					actionChain.add(newDrop());
				}
				actionChain.add(exploreActionChain);
				
			}
			
		}
		
		//Wywalanie kamieni z drogi
		if (actionChain == null || actionChain.actions.size() == 0){
			logger.info();
			logger.info("Probujemy wywalic kamienie z drogi.");
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain dragActionChain = newDragAnyFirstNearNull(unit,stones,
					(Node n) -> n.field != null && n.field.background == Background.VOID,success);
			
			logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			logger.fine("dragActionChain : ");
			logger.fine(dragActionChain.toString());
			logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			
			if (success.get()){
				
				actionChain = constructFullDragActionChain(unit,dragActionChain,success);
				
				if (success.get()){
					logger.info("Uda sie wywalic z drogi kamien.");
				} else {
					logger.info("Nie udalo sie nam skonstruowac drogi pozwalajacej wywalic kamien.");
					logger.fine("Mamy tylko : ");
					logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
					logger.fine("dragActionChain : ");
					logger.fine(dragActionChain.toString());
					logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
				}
				
			} else {
				logger.info("Nie mozna zadnego kamienia wywalic.");
			}
			
		}
		
		//Wywalanie diamentów do voida
		if (actionChain == null || actionChain.actions.size() == 0){
			logger.info();
			logger.info("Probujemy wywalic diamenty do voida.");
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain dragActionChain = newDragAnyFirstNearNull(unit,diamonds,
					(Node n) -> n.field != null && n.field.background == Background.VOID,success);
			logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			logger.fine("dragActionChain : ");
			logger.fine(dragActionChain.toString());
			logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			
			if (success.get()){
				
				actionChain = constructFullDragActionChain(unit,dragActionChain,success);
				
				if (success.get()){
					logger.info("Uda sie wywalic diament do voida.");
				}
				
			}
			
		}
		
		//Sprawdzamy czy idac do oltarza z inna akcja, dojdziemy wystarczajaco wczesnie, aby sie uleczyc
		if (goingToAltar && actionChain != null && actionChain.actions.size() > 0){
			HealthLoss healthLoss = actionChain.calculateHealthLoss();
			
			if (healthLoss.min > unit.hp){
				//Nie mamy szans by sie uleczyc
				goingToAltar = false;
			} else if (healthLoss.max + 4 > unit.hp){
				//Powinnismy skonczyc akcje i isc sie uleczyc 
				goingToAltar = false;
			}
			
		}
		
		//Wchodzimy tutaj tylko jak mamy jakas akcje do wykonania, jak nie mamy to nie ma sensu tracic punktow
		if (altars.size() > 0 && actionChain != null && actionChain.actions.size() > 0 &&
				gameStatus.points > 0 && !goingToAltar){
			
			logger.info("\n-- LECZENIE -- ");
			
			logger.info("Jeszcze sie nie leczylismy. Mamy dojscie do oltarza i punkty. Sprawdzamy czy wracać");
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain dropActionChain = dropIfDragging(unit,null);
			
			NewActionChain moveNearActionChain = newMoveNear(unit,altars.get(0).x,altars.get(0).y,success);
			
			UnitPosition endPosition = moveNearActionChain.calculateEndPosition();
			
			NewActionChain rotateActionChain = newRotate(unit.orientation,
					whichDirection(endPosition.posX,endPosition.posY,altars.get(0).x,altars.get(0).y));
			
			if (success.get()){
				//Jak sie udalo znalezc dobry ciag akcji
			
				NewActionChain healActionChain = new NewActionChain(unit);
				healActionChain.add(dropActionChain);
				healActionChain.add(moveNearActionChain);
				healActionChain.add(rotateActionChain);
				
				
				//Liczymy ile zycia nam zajmie dojscie (bez heala, bo liczy sie po kazdej akcji)
				HealthLoss healthLoss = healActionChain.calculateHealthLoss();
				
				healActionChain.add(newHeal());
				
				logger.info("Idac do ołtarza stracimy min. "+healthLoss.min+" max. "+healthLoss.max+" zycia."
						+ " Mamy aktualnie "+unit.hp+" zycia.");
				
				if (healthLoss.min >= unit.hp){
					logger.info("Nie mamy szans na uleczenie.");
				} else if (healthLoss.max +5 >= unit.hp){
					logger.info("Najwyzsza pora zeby wrocic i sie uleczyc. Anulujemy inne akcje.");
					actionChain = healActionChain;
					
					if (actionChain.actions.size() == 1){
						logger.fine("Zostala nam ostatnia akcja. Ustawiamy unit.once na false");
						unit.once = false;
					}
					
				}
			
			}
			
			//Obracamy sie w lewo
			if (actionChain == null || actionChain.actions.size() == 0){
				
				actionChain = new NewActionChain(unit);
				actionChain.add(newRotateLeft());
				
			}
			
			logger.info();
			
		}
		
		if (actionChain != null && actionChain.actions.size() > 0){
			logger.info("Aktualny ciag akcji do wykonania : ");
			logger.info(actionChain.toString());
			//Uzywamy akcji
			actionChain.executeAction(unit);
		}
		
	}
	
/* wersja 2.0 */
public void primaryTeamUnit(GameStatus gameStatus){
		
		Unit unit = units.get(gameStatus.units.get(0).id);
		
		int posX = unit.posX;
		int posY = unit.posY;
		
		logger.info("");
		logger.info("Mam numer tury : "+unit.unitTurn);
		if (unit.unitTurn != unitTurn){
			logger.error("Error : Nie zgadza sie numer tury jednostki");
			logger.error("Jednostka ma numer tury : "+unit.unitTurn);
			logger.error("A jest tura : "+unitTurn);
			//Trzeba usunac jednostki ktore sa pomiedzy unitTurn, a unit.unitTurn
			int unitsCount = units.size();
			if (unit.unitTurn < 0 || unit.unitTurn >= unitsCount){
				logger.severe("Cos jest w ogole nie tak z numeracja jednostek. Mamy "+unitsCount+" jednostek.");
			} else {
				while (unitTurn != unit.unitTurn){
					//usuwamy jednostke z unitTurn
					Unit unitToDelete = null;
					for (Unit currentUnit : units.values()){
						if (currentUnit.unitTurn == unitTurn){
							unitToDelete = currentUnit;
							break;
						}
					}
					if (unitToDelete != null){
						logger.info("Usuwamy jednostke nr "+unitToDelete.id+". Widocznie zginela.");
						units.remove(unitToDelete.id);
					} else {
						logger.error("Cos jest nie tak, nie wykrylismy jednostki o numerze tury : "+unitTurn);
					}
					unitTurn = (unitTurn+1)%unitsCount;
				}
			}
		}
		
		logger.info("TakenNodes",LogMessage.CATEGORY_STACK_TRACE);
		logger.info("");
		if (taken.isEmpty()){
			logger.info("Nie ma zadnych zajetych pol na mapie. Chyba jestem pierwszy.");
		} else {
			logger.info("Sa zajete pola na mapie.");
			logger.fine("Zajete pola na mapie : ");
			for (ValueNode n : taken){
				logger.fine(" x = "+n.node.x+" y = "+n.node.y+" tura = "+n.value);
			}
		}
		logger.info("~TakenNodes",LogMessage.CATEGORY_STACK_TRACE);
		
		if (unit.reservedObjectNode != null){
			unit.reservedObjectNode.reservedObject = false;
			unit.reservedObjectNode = null;
		}
		
		LinkedList<Node> reservedObjects = new LinkedList<Node>();
		for (int x = 0;x < MAX_MAPSIZEX;++x)
			for (int y = 0;y < MAX_MAPSIZEY;++y)
				if (map[x][y].reservedObject){
					reservedObjects.add(map[x][y]);
				}

		logger.info("");
		logger.info("Zarezerwowane obiekty : ");
		for (Node n : reservedObjects){
			logger.info(" x = "+n.x+" y = "+n.y);
		}
		
		logger.info();
		logger.info("Akcja priorytetowa : "+unit.priorityAction);
		
		LinkedList<Node> stones = findHPClosestAllObjects(posX,posY,MapField.Object.STONE);
		
		//LinkedList<Node> diamonds = findHPClosestAllObjects(posX,posY,MapField.Object.DIAMOND);
		LinkedList<Node> diamonds = findHPClosestAll(posX, posY, (Node n) -> n != null && n.field != null
				&& n.field.object == MapField.Object.DIAMOND && !n.reservedObject);
		
		LinkedList<Node> altars = findHPClosestAllPlayerBuildings(posX,posY,unit.player,Building.ALTAR);
		
		//Tutaj bedzie zapisana akcja do wykonania
		NewActionChain actionChain = null;
		//Opis słowny wykonywanej akcji
		String actionString = null;
		
		if (unit.checkHealing){
			if (unit.hp < 70 && gameStatus.points > 0){
				logger.info("Leczymy sie jak juz jestesmy kolo oltarza.");
				actionChain = new NewActionChain(unit);
				actionChain.add(newHeal());
			}
			unit.checkHealing = false;
		}
		
		//TODO trzeba sprawdzac w ile ruchow dojdziemy do oltarza trzymajac diament, wtedy przy leczeniu
		//TODO brac mniejsza wartosc
		//TODO to bedzie akcja o najwiekszym priorytecie tutaj
		//boolean goingToAltar = false;
		
		//Czy sie da przerzucic normalnie diament do oltarza
		if ((actionChain == null || actionChain.actions.size() == 0) && diamonds.size() > 0){
			//Nie trzymamy diamentu ale mamy dostep do oltarza i do diamentu
			logger.info("Mogę dojść do diamentu. Sprawdzam czy moge zaniesc jakis do oltarza");
			
			AtomicBoolean success = new AtomicBoolean(true);
			NewActionChain dragActionChain = newDragAny(unit,diamonds,
					(Node n) -> n.field != null && n.field.building == Building.ALTAR
					&& n.field.buildingPlayer == playerID,success,(Node n) -> n != null & n.field != null
					&& n.field.crossable()
						,(Node n,MapField.Object obj) -> n != null & n.field != null
							&& (n.field.crossableByObject(obj) || 
								(n.field.building == Building.ALTAR && n.field.buildingPlayer == playerID
							&& n.field.object == MapField.Object.DIAMOND && n.reservedObject)));
			
			logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			logger.fine("dragActionChain : ");
			logger.fine(dragActionChain.toString());
			logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			
			if (success.get()) {
				//Udalo sie akcja mozemy ustawic actionChain
				
				actionChain = constructFullDragActionChain(unit,dragActionChain,success);
				
				if (success.get()){
					//Mozna przeniesc diament do oltarza
					logger.info("Mozna przeniesc diament do oltarza.");
					
					UnitPosition nearDiamond = dragActionChain.startPos;
					Node diamondNode = map[nearDiamond.posX][nearDiamond.posY].edges.get(nearDiamond.orientation);
					logger.info("Mozna przeniesc diament z pozycji x = "+diamondNode.x+" y = "+diamondNode.y+".");
					logger.info("REZERWUJE x = "+diamondNode.x+" y = "+diamondNode.y);
					diamondNode.reservedObject = true;
					unit.reservedObjectNode = diamondNode;
					
					actionString = "Zanosimy do ołtarza diament z x = "+diamondNode.x+" y = "+diamondNode.y;
					
					if (actionChain.actions.size() == 1){
						//Zostalo nam tylko dropowanie, trzeba wlaczyc checkHealing
						unit.checkHealing = true;
					}
					
				}
				
			}
			
		}
		
		//Czy sie da przerzucic diament do oltarza, pomijajac inne nasze jednostki
		if ((actionChain == null || actionChain.actions.size() == 0) && diamonds.size() > 0){
				
			//Usuwamy jednostki z mapy wraz z diamentami, które niosą
			LinkedList<Node> wasDiamond = new LinkedList<Node>();
			for (Unit otherUnit : units.values()) if (otherUnit != unit){
				Node unitNode = map[otherUnit.posX][otherUnit.posY];
				if (unitNode.field != null) unitNode.field.unit = null;
				if (otherUnit.action.equalsIgnoreCase("dragging")){
					Node objectNode = map[otherUnit.posX][otherUnit.posY].edges.get(otherUnit.orientation);
					if (objectNode.field != null && objectNode.field.object == MapField.Object.DIAMOND){
						logger.fine("Jednostka "+otherUnit.id+" trzymala diament. Usuwamy go na chwile.");
						wasDiamond.add(objectNode);
						objectNode.field.object = null;
					}
				}
			}
			
			AtomicBoolean success = new AtomicBoolean(true);
			NewActionChain dragActionChain = newDragAny(unit,diamonds,
					(Node n) -> n.field != null && n.field.building == Building.ALTAR
					&& n.field.buildingPlayer == playerID,success,(Node n) -> n != null & n.field != null
					&& n.field.crossable()
						,(Node n,MapField.Object obj) -> n != null & n.field != null
							&& (n.field.crossableByObject(obj) || 
								(n.field.building == Building.ALTAR && n.field.buildingPlayer == playerID
							&& n.field.object == MapField.Object.DIAMOND && n.reservedObject)));
			
			if (success.get()){

				
				
				success.set(true);
				actionChain = constructFullDragActionChain(unit,dragActionChain,success);
				
				if (success.get()){
					logger.info("Moznaby bylo przeniesc diament, gdyby nie moje jednostki");
					
					logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
					logger.fine("Moglbym to zrobic akcjami : ");
					logger.fine(dragActionChain.toString());		
					logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
					
					//Sprawdzamy po ilu turach zaczna nam przeszkadzac jednostki
					LinkedList<Node> disputableNodes = new LinkedList<Node>();
					for (Node node : wasDiamond) disputableNodes.add(node);
					for (Unit otherUnit : units.values()) if (otherUnit != unit) 
						disputableNodes.add(map[otherUnit.posX][otherUnit.posY]);
					
					Integer whichTurn = dragActionChain.inWhichTurnFirstNodeIsCrossed((Node n) -> disputableNodes.contains(n));
					if (whichTurn != null){
						logger.info("Po "+(whichTurn-1)+" turach zacznie nam przeszkadzac jednostka");
						
						if (whichTurn > 5){
							logger.info("Powinniśmy wykonać akcje");

							UnitPosition nearDiamond = dragActionChain.startPos;
							Node diamondNode = map[nearDiamond.posX][nearDiamond.posY].edges.get(nearDiamond.orientation);
							logger.info("Mozna przeniesc diament z pozycji x = "+diamondNode.x+" y = "+diamondNode.y+".");
							logger.info("REZERWUJE x = "+diamondNode.x+" y = "+diamondNode.y);
							diamondNode.reservedObject = true;
							unit.reservedObjectNode = diamondNode;
							
						} else actionChain = null;
						
					} else {
						logger.info("Nie mozna wyliczyc po ilu turach zacznie nam przeszkadzac jednostka");
						actionChain = null;
					}
				
				} else actionChain = null;
				
			} else {
				logger.info("Bez innych jednostek też nie można by było przenieść diamentu.");
			}
			
			//Wstawiamy jednostki z powrotem na mape
			for (Unit otherUnit : units.values()) if (otherUnit != unit){
				if (map[otherUnit.posX][otherUnit.posY].field != null){
					map[otherUnit.posX][otherUnit.posY].field.unit = new SeenUnit();
					map[otherUnit.posX][otherUnit.posY].field.unit.orientation = otherUnit.orientation;
					map[otherUnit.posX][otherUnit.posY].field.unit.hp = otherUnit.hp;
					map[otherUnit.posX][otherUnit.posY].field.unit.player = otherUnit.player;
				}
			}
			//Wstawiamy diamenty z powrotem na mape
			for (Node node : wasDiamond){
				node.field.object = MapField.Object.DIAMOND;
			}
				
		}
		
		//Czy sie da przerzucic diament do oltarza pomijając kamienie
		if ((actionChain == null || actionChain.actions.size() == 0) && diamonds.size() > 0){
			logger.info("Nie mozna przerzucic zadnego diamentu do oltarza. Sprawdzamy czy to wina kamieni");
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain dragActionChain = newDragAny(unit,diamonds,
				(Node n) -> n.field != null && n.field.building == Building.ALTAR
				&& n.field.buildingPlayer == playerID,success,(Node n) -> n != null && n.field != null
				&& n.field.crossableDisregardingObject(MapField.Object.STONE)
				,(Node n,MapField.Object obj) -> n != null && n.field != null 
				&& n.field.crossableByObjectDisregardingObject(obj,MapField.Object.STONE));
			
			if (success.get()){
				
				logger.info("Jakby nie bylo kamieni to mozna by bylo przeniesc diament");
			
				logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
				logger.fine();
				logger.fine("Mozna zaniesc diament do ołtarza za pomoca komend :");
				logger.fine(dragActionChain.toString());
				logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
				
				LinkedList<Node> path = dragActionChain.crossedNodesDragging((Node n) -> true);
				LinkedList<Node> stonesToThrowOut = dragActionChain.crossedNodesDragging((Node n) -> n != null 
						&& n.field != null && n.field.object == MapField.Object.STONE);
				
				logger.fine("PathToClean",LogMessage.CATEGORY_STACK_TRACE);
				logger.fine();
				logger.fine("Musimy oczyscic sciezke :");
				for (Node n : path){
					logger.fine(" x = "+n.x+" y = "+n.y);
				}
				logger.fine("~PathToClean",LogMessage.CATEGORY_STACK_TRACE);
				
				logger.info("Musimy się pozbyć kamieni.");
				logger.fine("StonesToThrowOut",LogMessage.CATEGORY_STACK_TRACE);
				for (Node n : stonesToThrowOut){
					logger.fine(" x = "+n.x+" y = "+n.y);
				}
				logger.fine("~StonesToThrowOut",LogMessage.CATEGORY_STACK_TRACE);
				
				for (Node n : path){
					n.marked = true;
				}
				
				success.set(true);
				dragActionChain = newDragAny(unit,stonesToThrowOut,(Node n) -> n.marked == false,success);
						
				for (Node n : path){
					n.marked = false;
				}
				
				if (success.get()){
					
					actionChain = constructFullDragActionChain(unit,dragActionChain,success);
					
					if (success.get()){
						logger.info("Mozna wywalic kamien tak, aby wyjac diament.");
						
						UnitPosition nearStone = dragActionChain.startPos;
						Node stoneNode = map[nearStone.posX][nearStone.posY].edges.get(nearStone.orientation);
						
						actionString = "Wywalamy na bok kamien x = "+stoneNode.x+" y = "+stoneNode.y;
					}
					
				} else {
					logger.info("Nie udalo sie wywalic kamienia.");
				}
		
			}
			
		}
		
		//Atak
		if (actionChain == null || actionChain.actions.size() == 0){
			
			Node frontNode = map[unit.posX][unit.posY].edges.get(unit.orientation);
			if (frontNode.field != null && frontNode.field.unit != null && frontNode.field.unit.player != playerID){
				actionChain = new NewActionChain(unit);
				actionChain.add(fight());
			}
			
		}
		
		
		//Explorowanie w poszukiwaniu ołtarza
		if ((actionChain == null || actionChain.actions.size() == 0) && altars.size() == 0){
			
			boolean found = false;
			//Sprawdzamy czy aby na pewno nie ma ołtarza nigdzie na mapie
			for (int x = 0;x < MAX_MAPSIZEX && !found;++x)
				for (int y = 0;y < MAX_MAPSIZEY && !found;++y)
					if (map[x][y] != null && map[x][y].field != null 
						&& map[x][y].field.building == Building.ALTAR
						&& map[x][y].field.buildingPlayer == playerID){
							found = true;
					}
			
			if (!found){
				//Nie ma ołtarza
				logger.info("Nie znaleziono na mapie ołtarza. Trzeba explorować w poszukiwaniu ołtarza.");
				
				if (unit.startPosX != null && unit.startPosY != null){
					logger.info("Startowa pozycja jednostki to : x = "+unit.startPosX+" y = "+unit.startPosY);
					
					AtomicBoolean success = new AtomicBoolean(true);
					NewActionChain exploreActionChain = newExploreForAltar(unit,success);
					
					if (success.get()){
						logger.info("Mozna explorowac mape w poszukiwaniu ołtarza.");
						actionChain = exploreActionChain;
					} else {
						logger.info("Nie mozna explorowac w poszukiwaniu ołtarza.");
					}
					
				} else {
					logger.info("Nie mamy startowej pozycji jednostki");
				}
				
			} else {
				//Jest oltarz, ale nie mozna do niego dojsc. Prawdopodobnie zablokowany przez unknowny
				
				AtomicBoolean success = new AtomicBoolean(true);
				NewActionChain exploreActionChain = newExploreUnknowns(unit,success);
				
				if (success.get()){
					logger.info("Mozna sprawdzac unknowny na mapie.");
					actionChain = exploreActionChain;
				} else {
					logger.info("Nie ma zadnych unknownow do przebadania.");
				}
				
			}
			
			
		}
		
		//Zwykle explorowanie mapy
		if (actionChain == null || actionChain.actions.size() == 0){
			//Nie mamy diamentow, ktore moglibysmy wrzucic do oltarza, wiec przeszukujemy plansze
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain exploreActionChain = newExplore(unit,success);
			
			if (success.get()){
				//Sprawdzamy czy nie trzymamy przedmiotu
				logger.info("Explorujemy mape.");
				
				boolean dragging = false;
				if (unit.action.equalsIgnoreCase("dragging")){
					logger.info("Ale najpierw musimy wyrzucic przedmiot");
					dragging = true;
				}
				
				actionChain = new NewActionChain();
				if (dragging){
					actionChain.add(newDrop());
				}
				actionChain.add(exploreActionChain);
				
				UnitPosition endPosition = exploreActionChain.calculateEndPosition();
				actionString = "Explorujemy mape idąc na pozycje x = "+endPosition.posX+" y = "+endPosition.posY;
				
			}
			
		}
		
		//Wywalanie kamieni z drogi
		if (actionChain == null || actionChain.actions.size() == 0){
			logger.info();
			logger.info("Probujemy wywalic kamienie z drogi.");
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain dragActionChain = newDragAnyFirstNearNull(unit,stones,
					(Node n) -> n.field != null && n.field.background == Background.VOID,success);
			
			logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			logger.fine("dragActionChain : ");
			logger.fine(dragActionChain.toString());
			logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			
			if (success.get()){
				
				actionChain = constructFullDragActionChain(unit,dragActionChain,success);
				
				if (success.get()){
					logger.info("Uda sie wywalic z drogi kamien.");
					
					UnitPosition nearStone = dragActionChain.startPos;
					Node stoneNode = map[nearStone.posX][nearStone.posY].edges.get(nearStone.orientation);
					
					actionString = "Wywalamy do próżni kamien x = "+stoneNode.x+" y = "+stoneNode.y;
					
				} else {
					logger.info("Nie udalo sie nam skonstruowac drogi pozwalajacej wywalic kamien.");
					logger.info("Mamy tylko : ");
					logger.info(dragActionChain.toString());
				}
				
			} else {
				logger.info("Nie mozna zadnego kamienia wywalic.");
			}
			
		}
		
		//Wywalanie diamentów do voida
		if (actionChain == null || actionChain.actions.size() == 0){
			logger.info();
			logger.info("Probujemy wywalic diamenty do voida.");
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain dragActionChain = newDragAnyFirstNearNull(unit,diamonds,
					(Node n) -> n.field != null && n.field.background == Background.VOID,success);
			
			logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			logger.fine("dragActionChain : ");
			logger.fine(dragActionChain.toString());
			logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			
			if (success.get()){
				
				actionChain = constructFullDragActionChain(unit,dragActionChain,success);
				
				if (success.get()){
					logger.info("Uda sie wywalic diament do voida.");
					
					UnitPosition nearDiamond = dragActionChain.startPos;
					Node diamondNode = map[nearDiamond.posX][nearDiamond.posY].edges.get(nearDiamond.orientation);
					
					actionString = "Wywalamy do próżni diament x = "+diamondNode.x+" y = "+diamondNode.y;
					
				}
				
			}
			
		}
		
		//Wchodzimy tutaj tylko jak mamy jakas akcje do wykonania, jak nie mamy to nie ma sensu tracic punktow
		if (altars.size() > 0 && actionChain != null && actionChain.actions.size() > 0 &&
				gameStatus.points > 0){
			
			logger.info("\n-- LECZENIE -- ");
			
			logger.info("Jeszcze sie nie leczylismy. Mamy dojscie do oltarza i punkty. Sprawdzamy czy wracać");
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain dropActionChain = dropIfDragging(unit,null);
			
			NewActionChain moveNearActionChain = newMoveNear(unit,altars.get(0).x,altars.get(0).y,success);
			
			UnitPosition endPosition = moveNearActionChain.calculateEndPosition();
			
			NewActionChain rotateActionChain = newRotate(unit.orientation,
					whichDirection(endPosition.posX,endPosition.posY,altars.get(0).x,altars.get(0).y));
			
			if (success.get()){
				//Jak sie udalo znalezc dobry ciag akcji
			
				NewActionChain healActionChain = new NewActionChain(unit);
				healActionChain.add(dropActionChain);
				healActionChain.add(moveNearActionChain);
				healActionChain.add(rotateActionChain);
				
				
				//Liczymy ile zycia nam zajmie dojscie (bez heala, bo liczy sie po kazdej akcji)
				HealthLoss healthLoss = healActionChain.calculateHealthLoss();
				
				healActionChain.add(newHeal());
				
				logger.info("Idac do ołtarza stracimy min. "+healthLoss.min+" max. "+healthLoss.max+" zycia."
						+ " Mamy aktualnie "+unit.hp+" zycia.");
				
				if (healthLoss.min >= unit.hp){
					logger.info("Nie mamy szans na uleczenie.");
				} else if (healthLoss.max +5 >= unit.hp){
					logger.info("Najwyzsza pora zeby wrocic i sie uleczyc. Anulujemy inne akcje.");
					actionChain = healActionChain;
					
					actionString = "Idziemy się uleczyć";
					
					if (actionChain.actions.size() == 1){
						logger.fine("Zostala nam ostatnia akcja. Ustawiamy unit.once na false");
						actionString = "Leczymy się";
						unit.once = false;
					}
					
				}
			
			}
			
			logger.info();
			
		}
		
		//Obracamy sie w lewo
		if (actionChain == null || actionChain.actions.size() == 0){
			
			actionChain = new NewActionChain(unit);
			actionChain.add(newRotateLeft());
			
			actionString = "Obracamy się bez pojęcia";
			
		}
		
		if (actionChain != null && actionChain.actions.size() > 0){
			
			if (actionString != null){
				logger.info();
				logger.info("-- AKCJA --");
				logger.info(actionString);
			}
			
			logger.info();
			logger.info("Aktualny ciag akcji do wykonania : ");
			logger.info(actionChain.toString());
			
			if (!actionChain.takeNodes(gameStatus.round)){
				logger.info("Error : primaryTeamUnit - Nie mozna pobrac pol, poniewaz pozycja startowa nie"
						+" zostala zainicjalizowana");
			}
			
			//Uzywamy akcji
			actionChain.executeAction(unit);
		}
		
	}




/* wersja 3.0 */
public void primaryBackwardTeamUnit(GameStatus gameStatus){
	
	Unit unit = units.get(gameStatus.units.get(0).id);
	
	int posX = unit.posX;
	int posY = unit.posY;
	
	logger.info();
	logger.info("Mam numer tury : "+unit.unitTurn);
	if (unit.unitTurn != unitTurn){
		System.out.println("Error : Nie zgadza sie numer tury jednostki");
		System.out.println("Jednostka ma numer tury : "+unit.unitTurn);
		System.out.println("A jest tura : "+unitTurn);
		//Trzeba usunac jednostki ktore sa pomiedzy unitTurn, a unit.unitTurn
		int unitsCount = units.size();
		if (unit.unitTurn < 0 || unit.unitTurn >= unitsCount){
			System.out.println("Co jest w ogole nie tak z numeracja jednostek. Mamy "+unitsCount+" jednostek.");
		} else {
			while (unitTurn != unit.unitTurn){
				//usuwamy jednostke z unitTurn
				Unit unitToDelete = null;
				for (Unit currentUnit : units.values()){
					if (currentUnit.unitTurn == unitTurn){
						unitToDelete = currentUnit;
						break;
					}
				}
				if (unitToDelete != null){
					System.out.println("Usuwamy jednostke nr "+unitToDelete.id+". Widocznie zginela.");
					units.remove(unitToDelete.id);
					Node unitNode = map[unitToDelete.posX][unitToDelete.posY];
					if (unitNode.field == null) unitNode.field = new MapField();
					unitNode.field.object = MapField.Object.DIAMOND;
				} else {
					System.out.println("Error : Cos jest nie tak, nie wykrylismy jednostki o numerze tury : "+unitTurn);
				}
				unitTurn = (unitTurn)%unitsCount+1;
			}
		}
	}
	/*
	println();
	if (taken.isEmpty()){
		println("Nie ma zadnych zajetych pol na mapie. Chyba jestem pierwszy.");
	} else {
		println("Zajete pola na mapie : ");
		for (ValueNode n : taken){
			println(" x = "+n.node.x+" y = "+n.node.y+" tura = "+n.value);
		}
	}*/
	
	if (unit.reservedObjectNode != null){
		unit.reservedObjectNode.reservedObject = false;
		unit.reservedObjectNode = null;
	}
	
	LinkedList<Node> reservedObjects = new LinkedList<Node>();
	for (int x = 0;x < MAX_MAPSIZEX;++x)
		for (int y = 0;y < MAX_MAPSIZEY;++y)
			if (map[x][y].reservedObject){
				reservedObjects.add(map[x][y]);
			}
	
	logger.fine();
	logger.fine("ReservedObjects",LogMessage.CATEGORY_STACK_TRACE);
	for (Node n : reservedObjects){
		logger.fine(" x = "+n.x+" y = "+n.y);
	}
	logger.fine("~ReservedObjects",LogMessage.CATEGORY_STACK_TRACE);
	
	logger.info();
	logger.info("Akcja priorytetowa : "+unit.priorityAction);
	
	LinkedList<Node> stones = findHPClosestAllObjects(posX,posY,MapField.Object.STONE);
	
	//LinkedList<Node> diamonds = findHPClosestAllObjects(posX,posY,MapField.Object.DIAMOND);
	LinkedList<Node> diamonds = findHPClosestAll(posX, posY, (Node n) -> n != null && n.field != null
			&& n.field.object == MapField.Object.DIAMOND && !n.reservedObject);
	
	LinkedList<Node> altars = findHPClosestAllPlayerBuildings(posX,posY,unit.player,Building.ALTAR);
	
	//Tutaj bedzie zapisana akcja do wykonania
	NewActionChain actionChain = null;
	//Opis słowny wykonywanej akcji
	String actionString = null;
	
	if (unit.checkHealing){
		if (unit.hp < 70 && gameStatus.points > 0){
			logger.info("Leczymy sie jak juz jestesmy kolo oltarza.");
			actionChain = new NewActionChain(unit);
			actionChain.add(newHeal());
		}
		unit.checkHealing = false;
	}
	
	//TODO trzeba sprawdzac w ile ruchow dojdziemy do oltarza trzymajac diament, wtedy przy leczeniu
	//TODO brac mniejsza wartosc
	//TODO to bedzie akcja o najwiekszym priorytecie tutaj
	//boolean goingToAltar = false;
	
	//Czy sie da przerzucic normalnie diament do oltarza
	if ((actionChain == null || actionChain.actions.size() == 0) && diamonds.size() > 0){
		//Nie trzymamy diamentu ale mamy dostep do oltarza i do diamentu
		logger.info("Mogę dojść do diamentu. Sprawdzam czy moge zaniesc jakis do oltarza");
		
		AtomicBoolean success = new AtomicBoolean(true);
		NewActionChain dragActionChain = newFullDragAnyThroughNulls(unit,diamonds,
				(Node n) -> n.field != null && n.field.building == Building.ALTAR
				&& n.field.buildingPlayer == playerID,success,(Node n) -> n != null && n.field != null
				&& n.field.crossable()
					,(Node n,MapField.Object obj) -> n != null && n.field != null
						&& (n.field.crossableByObject(obj) || 
							(n.field.building == Building.ALTAR && n.field.buildingPlayer == playerID
						&& n.field.object == MapField.Object.DIAMOND && n.reservedObject)));
		
		logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
		logger.fine("dragActionChain : ");
		logger.fine(dragActionChain.toString());
		logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
		
		if (success.get()) {
			//Mozna przeniesc diament do oltarza
			logger.info("Mozna przeniesc diament do oltarza.");
			
			UnitPosition nearDiamond = dragActionChain.startPos;
			Node diamondNode = map[nearDiamond.posX][nearDiamond.posY].edges.get(nearDiamond.orientation);
			logger.info("Mozna przeniesc diament z pozycji x = "+diamondNode.x+" y = "+diamondNode.y+".");
			logger.info("REZERWUJE x = "+diamondNode.x+" y = "+diamondNode.y);
			diamondNode.reservedObject = true;
			unit.reservedObjectNode = diamondNode;
			
			actionChain = constructFullDragActionChain(unit,dragActionChain,success);
			
			actionString = "Zanosimy do ołtarza diament z x = "+diamondNode.x+" y = "+diamondNode.y;
			
			if (actionChain.actions.size() == 1){
				//Zostalo nam tylko dropowanie, trzeba wlaczyc checkHealing
				unit.checkHealing = true;
			}
			
		}
		
	}
	
	//Jestesmy atakowani
	if (unit.attacked > 0 && (actionChain == null || actionChain.actions.size() == 0)){
		int enemiesCount = 0;//Liczba wrogów, stojących obok nas
		int directEnemiesCount = 0;//Wrogowie, którzy są ustawieni w naszą stronę
		
	}
	
	//Czy sie da przerzucic diament do oltarza, pomijajac inne nasze jednostki
	if ((actionChain == null || actionChain.actions.size() == 0) && diamonds.size() > 0){
			
		//Usuwamy jednostki z mapy wraz z diamentami, które niosą
		LinkedList<Node> wasDiamond = new LinkedList<Node>();
		for (Unit otherUnit : units.values()) if (otherUnit != unit){
			Node unitNode = map[otherUnit.posX][otherUnit.posY];
			if (unitNode.field != null) unitNode.field.unit = null;
			if (otherUnit.action.equalsIgnoreCase("dragging")){
				Node objectNode = map[otherUnit.posX][otherUnit.posY].edges.get(otherUnit.orientation);
				if (objectNode.field != null && objectNode.field.object == MapField.Object.DIAMOND){
					logger.fine("Jednostka "+otherUnit.id+" trzymala diament. Usuwamy go na chwile.");
					wasDiamond.add(objectNode);
					objectNode.field.object = null;
				}
			}
		}
		
		AtomicBoolean success = new AtomicBoolean(true);
		NewActionChain dragActionChain = newFullDragAnyThroughNulls(unit,diamonds,
				(Node n) -> n.field != null && n.field.building == Building.ALTAR
				&& n.field.buildingPlayer == playerID,success,(Node n) -> n != null & n.field != null
				&& n.field.crossable()
					,(Node n,MapField.Object obj) -> n != null & n.field != null
						&& (n.field.crossableByObject(obj) || 
							(n.field.building == Building.ALTAR && n.field.buildingPlayer == playerID
						&& n.field.object == MapField.Object.DIAMOND && n.reservedObject)));
		
		if (success.get()){
			
			dragActionChain = constructFullDragActionChain(unit,dragActionChain,success);
			
			logger.info("Moznaby bylo przeniesc diament, gdyby nie moje jednostki");
			
			logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			logger.fine("dragActionChain : ");
			logger.fine(dragActionChain.toString());
			logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			
			//Sprawdzamy po ilu turach zaczna nam przeszkadzac jednostki
			LinkedList<Node> disputableNodes = new LinkedList<Node>();
			for (Node node : wasDiamond) disputableNodes.add(node);
			for (Unit otherUnit : units.values()) if (otherUnit != unit) 
				disputableNodes.add(map[otherUnit.posX][otherUnit.posY]);
			
			Integer whichTurn = dragActionChain.inWhichTurnFirstNodeIsCrossed((Node n) -> disputableNodes.contains(n));
			if (whichTurn != null){
				logger.info("Po "+(whichTurn-1)+" turach zacznie nam przeszkadzac jednostka");
				
				if (whichTurn > 5){
					logger.info("Powinniśmy wykonać akcje");

					UnitPosition nearDiamond = dragActionChain.startPos;
					Node diamondNode = map[nearDiamond.posX][nearDiamond.posY].edges.get(nearDiamond.orientation);
					logger.info("Mozna przeniesc diament z pozycji x = "+diamondNode.x+" y = "+diamondNode.y+".");
					logger.info("REZERWUJE x = "+diamondNode.x+" y = "+diamondNode.y);
					diamondNode.reservedObject = true;
					unit.reservedObjectNode = diamondNode;
					
				} else actionChain = null;
				
			} else {
				logger.info("Nie mozna wyliczyc po ilu turach zacznie nam przeszkadzac jednostka");
				actionChain = null;
			}
			
		} else {
			logger.info("Bez innych jednostek też nie można by było przenieść diamentu.");
		}
		
		//Wstawiamy jednostki z powrotem na mape
		for (Unit otherUnit : units.values()) if (otherUnit != unit){
			if (map[otherUnit.posX][otherUnit.posY].field != null){
				map[otherUnit.posX][otherUnit.posY].field.unit = new SeenUnit();
				map[otherUnit.posX][otherUnit.posY].field.unit.orientation = otherUnit.orientation;
				map[otherUnit.posX][otherUnit.posY].field.unit.hp = otherUnit.hp;
				map[otherUnit.posX][otherUnit.posY].field.unit.player = otherUnit.player;
			}
		}
		//Wstawiamy diamenty z powrotem na mape
		for (Node node : wasDiamond){
			node.field.object = MapField.Object.DIAMOND;
		}
			
	}
	
	//Czy sie da przerzucic diament do oltarza pomijając kamienie
	if ((actionChain == null || actionChain.actions.size() == 0) && diamonds.size() > 0){
		logger.info("Nie mozna przerzucic zadnego diamentu do oltarza. Sprawdzamy czy to wina kamieni");
		
		AtomicBoolean success = new AtomicBoolean(true);
		
		NewActionChain dragActionChain = newDragAny(unit,diamonds,
			(Node n) -> n.field != null && n.field.building == Building.ALTAR
			&& n.field.buildingPlayer == playerID,success,(Node n) -> n != null && n.field != null
			&& n.field.crossableDisregardingObject(MapField.Object.STONE)
			,(Node n,MapField.Object obj) -> n != null && n.field != null 
			&& n.field.crossableByObjectDisregardingObject(obj,MapField.Object.STONE));
		
		dragActionChain = constructFullDragActionChain(unit,dragActionChain,success);
		
		if (success.get()){
			
			logger.info("Jakby nie bylo kamieni to mozna by bylo przeniesc diament");
		
			logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			logger.fine("dragActionChain : ");
			logger.fine(dragActionChain.toString());
			logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			
			LinkedList<Node> path = dragActionChain.crossedNodesDragging((Node n) -> true);
			LinkedList<Node> stonesToThrowOut = dragActionChain.crossedNodesDragging((Node n) -> n != null 
					&& n.field != null && n.field.object == MapField.Object.STONE);
			
			
			logger.fine();
			logger.fine("Musimy oczyscic ścieżkę : ");
			logger.fine("Path",LogMessage.CATEGORY_STACK_TRACE);
			for (Node n : path){
				logger.fine(" x = "+n.x+" y = "+n.y);
			}
			logger.fine("~Path",LogMessage.CATEGORY_STACK_TRACE);
			
			logger.fine();
			logger.fine("Sa na niej kamienie : ");
			logger.fine("StonesToThrowOut",LogMessage.CATEGORY_STACK_TRACE);
			for (Node n : stonesToThrowOut){
				logger.fine(" x = "+n.x+" y = "+n.y);
			}
			logger.fine("~StonesToThrowOut",LogMessage.CATEGORY_STACK_TRACE);
			
			for (Node n : path){
				n.marked = true;
			}
			
			
			
			success.set(true);
			dragActionChain = newDragAny(unit,stonesToThrowOut,(Node n) -> n.marked == false,success);
					
			for (Node n : path){
				n.marked = false;
			}
			
			if (success.get()){
				
				actionChain = constructFullDragActionChain(unit,dragActionChain,success);
				
				if (success.get()){
					logger.info("Mozna wywalic kamien tak, aby wyjac diament.");
					
					UnitPosition nearStone = dragActionChain.startPos;
					Node stoneNode = map[nearStone.posX][nearStone.posY].edges.get(nearStone.orientation);
					
					actionString = "Wywalamy na bok kamien x = "+stoneNode.x+" y = "+stoneNode.y;
				}
				
			} else {
				logger.info("Nie udalo sie wywalic kamienia.");
			}
	
		}
		
	}
	
	//Atak
	if (actionChain == null || actionChain.actions.size() == 0){
		
		Node frontNode = map[unit.posX][unit.posY].edges.get(unit.orientation);
		if (frontNode.field != null && frontNode.field.unit != null && frontNode.field.unit.player != playerID){
			actionChain = new NewActionChain(unit);
			actionChain.add(fight());
		}
		
	}
	
	
	//Explorowanie w poszukiwaniu ołtarza
	if ((actionChain == null || actionChain.actions.size() == 0) && altars.size() == 0){
		
		boolean found = false;
		//Sprawdzamy czy aby na pewno nie ma ołtarza nigdzie na mapie
		for (int x = 0;x < MAX_MAPSIZEX && !found;++x)
			for (int y = 0;y < MAX_MAPSIZEY && !found;++y)
				if (map[x][y] != null && map[x][y].field != null 
					&& map[x][y].field.building == Building.ALTAR
					&& map[x][y].field.buildingPlayer == playerID){
						found = true;
				}
		
		if (!found){
			//Nie ma ołtarza
			logger.info("Nie znaleziono na mapie ołtarza. Trzeba explorować w poszukiwaniu ołtarza.");
			
			if (unit.startPosX != null && unit.startPosY != null){
				logger.info("Startowa pozycja jednostki to : x = "+unit.startPosX+" y = "+unit.startPosY);
				
				AtomicBoolean success = new AtomicBoolean(true);
				NewActionChain exploreActionChain = newExploreForAltar(unit,success);
				
				if (success.get()){
					logger.info("Mozna explorowac mape w poszukiwaniu ołtarza.");
					actionChain = exploreActionChain;
				} else {
					logger.info("Nie mozna explorowac w poszukiwaniu ołtarza.");
				}
				
			} else {
				logger.info("Nie mamy startowej pozycji jednostki");
			}
			
		} else {
			//Jest oltarz, ale nie mozna do niego dojsc. Prawdopodobnie zablokowany przez unknowny
			
			AtomicBoolean success = new AtomicBoolean(true);
			NewActionChain exploreActionChain = newExploreUnknowns(unit,success);
			
			if (success.get()){
				logger.info("Mozna sprawdzac unknowny na mapie.");
				actionChain = exploreActionChain;
			} else {
				logger.info("Nie ma zadnych unknownow do przebadania.");
			}
			
		}
		
		
	}
	
	//Zwykle explorowanie mapy
	if (actionChain == null || actionChain.actions.size() == 0){
		//Nie mamy diamentow, ktore moglibysmy wrzucic do oltarza, wiec przeszukujemy plansze
		
		AtomicBoolean success = new AtomicBoolean(true);
		
		NewActionChain exploreActionChain = newExplore(unit,success);
		
		if (success.get()){
			//Sprawdzamy czy nie trzymamy przedmiotu
			logger.info("Explorujemy mape.");
			
			boolean dragging = false;
			if (unit.action.equalsIgnoreCase("dragging")){
				logger.info("Ale najpierw musimy wyrzucic przedmiot");
				dragging = true;
			}
			
			actionChain = new NewActionChain();
			if (dragging){
				actionChain.add(newDrop());
			}
			actionChain.add(exploreActionChain);
			
			UnitPosition endPosition = exploreActionChain.calculateEndPosition();
			actionString = "Explorujemy mape idąc na pozycje x = "+endPosition.posX+" y = "+endPosition.posY;
			
		}
		
	}
	
	//Wywalanie kamieni z drogi
	if (actionChain == null || actionChain.actions.size() == 0){
		logger.info();
		logger.info("Probujemy wywalic kamienie z drogi.");
		
		AtomicBoolean success = new AtomicBoolean(true);
		
		NewActionChain dragActionChain = newDragAnyFirstNearNull(unit,stones,
				(Node n) -> n.field != null && n.field.background == Background.VOID,success);

		logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
		logger.fine("dragActionChain : ");
		logger.fine(dragActionChain.toString());
		logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
		
		if (success.get()){
			
			actionChain = constructFullDragActionChain(unit,dragActionChain,success);
			
			if (success.get()){
				logger.info("Uda sie wywalic z drogi kamien.");
				
				UnitPosition nearStone = dragActionChain.startPos;
				Node stoneNode = map[nearStone.posX][nearStone.posY].edges.get(nearStone.orientation);
				
				actionString = "Wywalamy do próżni kamien x = "+stoneNode.x+" y = "+stoneNode.y;
				
			} else {
				logger.info("Nie udalo sie nam skonstruowac drogi pozwalajacej wywalic kamien.");
				logger.fine("Mamy tylko : ");
				logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
				logger.fine("dragActionChain : ");
				logger.fine(dragActionChain.toString());
				logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
			}
			
		} else {
			logger.info("Nie mozna zadnego kamienia wywalic.");
		}
		
	}
	
	//Wywalanie diamentów do voida
	if (actionChain == null || actionChain.actions.size() == 0){
		logger.info();
		logger.info("Probujemy wywalic diamenty do voida.");
		
		AtomicBoolean success = new AtomicBoolean(true);
		
		NewActionChain dragActionChain = newDragAnyFirstNearNull(unit,diamonds,
				(Node n) -> n.field != null && n.field.background == Background.VOID,success);

		logger.fine("DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
		logger.fine("dragActionChain : ");
		logger.fine(dragActionChain.toString());
		logger.fine("~DragActionChain",LogMessage.CATEGORY_STACK_TRACE);
		
		if (success.get()){
			
			actionChain = constructFullDragActionChain(unit,dragActionChain,success);
			
			if (success.get()){
				logger.info("Uda sie wywalic diament do voida.");
				
				UnitPosition nearDiamond = dragActionChain.startPos;
				Node diamondNode = map[nearDiamond.posX][nearDiamond.posY].edges.get(nearDiamond.orientation);
				
				actionString = "Wywalamy do próżni diament x = "+diamondNode.x+" y = "+diamondNode.y;
				
			}
			
		}
		
	}
	
	//Wchodzimy tutaj tylko jak mamy jakas akcje do wykonania, jak nie mamy to nie ma sensu tracic punktow
	if (altars.size() > 0 && actionChain != null && actionChain.actions.size() > 0 &&
			gameStatus.points > 0){
		
		logger.info("\n-- LECZENIE -- ");
		
		logger.info("Jeszcze sie nie leczylismy. Mamy dojscie do oltarza i punkty. Sprawdzamy czy wracać");
		
		AtomicBoolean success = new AtomicBoolean(true);
		
		NewActionChain dropActionChain = dropIfDragging(unit,null);
		
		NewActionChain moveNearActionChain = newMoveNear(unit,altars.get(0).x,altars.get(0).y,success);
		
		UnitPosition endPosition = moveNearActionChain.calculateEndPosition();
		
		NewActionChain rotateActionChain = newRotate(unit.orientation,
				whichDirection(endPosition.posX,endPosition.posY,altars.get(0).x,altars.get(0).y));
		
		if (success.get()){
			//Jak sie udalo znalezc dobry ciag akcji
		
			NewActionChain healActionChain = new NewActionChain(unit);
			healActionChain.add(dropActionChain);
			healActionChain.add(moveNearActionChain);
			healActionChain.add(rotateActionChain);
			
			
			//Liczymy ile zycia nam zajmie dojscie (bez heala, bo liczy sie po kazdej akcji)
			HealthLoss healthLoss = healActionChain.calculateHealthLoss();
			
			healActionChain.add(newHeal());
			
			logger.info("Idac do ołtarza stracimy min. "+healthLoss.min+" max. "+healthLoss.max+" zycia."
					+ " Mamy aktualnie "+unit.hp+" zycia.");
			
			if (healthLoss.min >= unit.hp){
				logger.info("Nie mamy szans na uleczenie.");
			} else if (healthLoss.max +5 >= unit.hp){
				logger.info("Najwyzsza pora zeby wrocic i sie uleczyc. Anulujemy inne akcje.");
				actionChain = healActionChain;
				
				actionString = "Idziemy się uleczyć";
				
				if (actionChain.actions.size() == 1){
					logger.fine("Zostala nam ostatnia akcja. Ustawiamy unit.once na false");
					actionString = "Leczymy się";
					unit.once = false;
				}
				
			}
		
		}
		
		logger.info();
		
	}
	
	//Obracamy sie w lewo
	if (actionChain == null || actionChain.actions.size() == 0){
		
		actionChain = new NewActionChain(unit);
		actionChain.add(newRotateLeft());
		
		actionString = "Obracamy się bez pojęcia";
		
	}
	
	if (actionChain != null && actionChain.actions.size() > 0){
		
		if (actionString != null){
			logger.info();
			logger.info("-- AKCJA --");
			logger.info(actionString);
		}
		
		logger.info();
		logger.info("Aktualny ciag akcji do wykonania : ");
		logger.info(actionChain.toString());
		
		if (!actionChain.takeNodes(gameStatus.round)){
			logger.error("primaryTeamUnit - Nie mozna pobrac pol, poniewaz pozycja startowa nie"
					+" zostala zainicjalizowana");
		}
		
		//Uzywamy akcji
		actionChain.executeAction(unit);
	}
	
}
	
	
	
	public void secondaryUnit(GameStatus gameStatus){
		
		Unit unit = units.get(gameStatus.units.get(0).id);
		
		int posX = unit.posX;
		int posY = unit.posY;
		
		logger.info("Akcja priorytetowa : "+unit.priorityAction);
		
		LinkedList<Node> stones = findHPClosestAllObjects(posX,posY,MapField.Object.STONE);

		LinkedList<Node> altars = findHPClosestAllPlayerBuildings(posX,posY,unit.player,Building.ALTAR);
		
		//Tutaj bedzie zapisana akcja do wykonania
		NewActionChain actionChain = null;
		
		if (unit.priorityAction != null){
			if (unit.priorityAction.equals("block enemy base")){
				
				LinkedList<Node> stonesNotInPosition = new LinkedList<Node>();
				
				for (Node stone : stones){
					if (nearPlayerBuilding(stone.x, stone.y, (playerID==1)?2:1, Building.ALTAR) == null){
						stonesNotInPosition.add(stone);
					}
				}
				
				actionChain = newDragAnyFirstNearNull(unit,stonesNotInPosition
						,(Node n) -> n.field != null && nearPlayerBuilding(n.x,n.y,(playerID==1)?2:1,Building.ALTAR) != null,null);
				
			}
		}
		
		//explorujemy mape
		if (actionChain == null || actionChain.actions.size() == 0){
		
			actionChain = newExplore(unit,null);
			
		}
		
		//Rzucamy kamienie pod baze przeciwnika
		if (actionChain == null || actionChain.actions.size() == 0){
			
			LinkedList<Node> stonesNotInPosition = new LinkedList<Node>();
			
			for (Node stone : stones){
				if (nearPlayerBuilding(stone.x, stone.y, (playerID==1)?2:1, Building.ALTAR) == null){
					stonesNotInPosition.add(stone);
				}
			}
			
			actionChain = newDragAnyFirstNearNull(unit,stonesNotInPosition
					,(Node n) -> n.field != null && nearPlayerBuilding(n.x,n.y,(playerID==1)?2:1,Building.ALTAR) != null,null);
			
		}
		
		//Rzucamy kamienie do próżni
		if (actionChain == null || actionChain.actions.size() == 0){
			
			logger.info("Nie mozna ruszyc zadnego kamienia do ołtarza przeciwnika, ani explorowac");

			actionChain = newDragAnyFirstNearNull(unit,stones
					,(Node n) -> n.field != null && n.field.background == Background.VOID,null);
			
		}

		//Wchodzimy tutaj tylko jak mamy jakas akcje do wykonania, jak nie mamy to nie ma sensu tracic punktow
		if (altars.size() > 0 && unit.once && actionChain != null && actionChain.actions.size() > 0 &&
				gameStatus.points > 0){
			
			logger.info("\n-- LECZENIE -- ");
			
			logger.info("Jeszcze sie nie leczylismy. Mamy dojscie do oltarza i punkty. Sprawdzamy czy wracać");
			
			AtomicBoolean success = new AtomicBoolean(true);
			
			NewActionChain dropActionChain = dropIfDragging(unit,null);
			
			NewActionChain moveNearActionChain = newMoveNear(unit,altars.get(0).x,altars.get(0).y,success);
			
			UnitPosition endPosition = moveNearActionChain.calculateEndPosition();
			
			NewActionChain rotateActionChain = newRotate(unit.orientation,
					whichDirection(endPosition.posX,endPosition.posY,altars.get(0).x,altars.get(0).y));
			
			if (success.get()){
				//Jak sie udalo znalezc dobry ciag akcji
			
				NewActionChain healActionChain = new NewActionChain();
				healActionChain.add(dropActionChain);
				healActionChain.add(moveNearActionChain);
				healActionChain.add(rotateActionChain);
				
				
				//Liczymy ile zycia nam zajmie dojscie (bez heala, bo liczy sie po kazdej akcji)
				HealthLoss healthLoss = healActionChain.calculateHealthLoss();
				
				healActionChain.add(newHeal());
				
				logger.info("Idac do ołtarza stracimy min. "+healthLoss.min+" max. "+healthLoss.max+" zycia."
						+ " Mamy aktualnie "+unit.hp+" zycia.");
				
				if (healthLoss.min >= unit.hp){
					logger.info("Nie mamy szans na uleczenie.");
				} else if (healthLoss.max +5 >= unit.hp){
					logger.info("Najwyzsza pora zeby wrocic i sie uleczyc. Anulujemy inne akcje.");
					actionChain = healActionChain;
					
					if (actionChain.actions.size() == 1){
						logger.fine("Zostala nam ostatnia akcja. Ustawiamy unit.once na false");
						unit.once = false;
					}
					
				}
			
			}
			
			logger.info();
			
		}
		
		if (actionChain != null && actionChain.actions.size() > 0){
			logger.info("Aktualny ciag akcji do wykonania : ");
			logger.info(actionChain.toString());
			//Uzywamy akcji
			actionChain.executeAction(unit);
		}
		
	}

	public void AIMain(GameStatus gameStatus){
		
		//TODO Pozniej usunac to, jednostki nie powinny sobie nawzajem przeszkadzac
		//Umieszczamy inne jednostki na planszy zeby nie przeszkadzaly nam
		for (Unit otherUnit : units.values()) if (otherUnit.id != gameStatus.units.get(0).id){
			if (map[otherUnit.posX][otherUnit.posY].field != null){
				map[otherUnit.posX][otherUnit.posY].field.unit = new SeenUnit();
				map[otherUnit.posX][otherUnit.posY].field.unit.orientation = otherUnit.orientation;
				map[otherUnit.posX][otherUnit.posY].field.unit.hp = otherUnit.hp;
				map[otherUnit.posX][otherUnit.posY].field.unit.player = otherUnit.player;
			}
		}
		
		primaryBackwardTeamUnit(gameStatus);
		
	}
	
	private void updateUnitsHealth(){
		
		for (Unit unit : units.values()){
			//FIXME zalozenie ze tylko na bagnie tracimy 2hp/ture
			if (map[unit.posX][unit.posY].field != null 
					&& map[unit.posX][unit.posY].field.background == Background.SWAMP)
			{
				//tracimy 2hp/ture
				unit.hp -= 2;
				logger.finer("Jednostka "+unit.id+ " traci 2 zycia. Ma teraz "+unit.hp+" zycia");
			} else if (map[unit.posX][unit.posY].field != null){
				unit.hp--;
				logger.finer("Jednostka "+unit.id+ " traci 1 zycia. Ma teraz "+unit.hp+" zycia");
			} else {
				logger.finer("Jednostka "+unit.id+" stoi na nullu, więc nie wiemy ile straciła życia");
				unit.standingOnNull = true;
				//Jezeli pole jest nullem to nic nie robimy, bo chcemy sie dowiedziec na podstawie
				//tego co zwroci nam serwer na czym stoi
			}
			if (unit.hp <= 0){
				//Jezeli jednostka zginela, wyrzucamy ja z listy i ustawiamy krysztal na jej pozycji
				
				int unitTurn = unit.unitTurn;
				units.remove(unit.id);
				
				//Zmieniamy numer tury pozostalym jednostkom
				for (Unit otherUnit : units.values()){
					if (otherUnit.unitTurn > unitTurn) --otherUnit.unitTurn;
				}
				
				//Czyscimy rezerwacje obiektu
				if (unit.reservedObjectNode != null){
					unit.reservedObjectNode.reservedObject = false;
					unit.reservedObjectNode = null;
				}
				
				map[unit.posX][unit.posY].field.object = MapField.Object.DIAMOND;
				
				//Jezeli trzymala przedmiot nad voidem to niszczymy ten przedmiot
				//TODO albo nie bo nie wiadomo czy ktos inny nie trzyma go\
				//TODO trzeba sprawdzic przynajmniej wszystkie moje jednostki czy jakas go nie trzyma
			}
		}
		
	}
	
	public void checkUnitParameters(Unit unit){
		
		if (unit.posX != units.get(unit.id).posX || unit.posY != units.get(unit.id).posY){
			//Nieprawidlowe polozenie jednostki
			logger.error("game.getGameStatus : Jednostka ma nieprawidlowe polozenie");
			logger.error("powinno byc x = "+units.get(unit.id).posX+" y = "+units.get(unit.id).posY+", a jest "
					+" x = "+unit.posX+ " y = "+unit.posY);
			units.get(unit.id).posX = unit.posX;
			units.get(unit.id).posY = unit.posY;
		}
		
		if (map[unit.posX][unit.posY].field == null){
			//Nie wiemy co to za pole musimy wyciagnac informacje na podstawie utraty zycia
			if (units.get(unit.id).hp == unit.hp+2){
				//stoimy na bagnie
				//FIXME Zakladamy tutaj ze tylko na bagnie sie traci 2hp/sek
				map[unit.posX][unit.posY].field = new MapField();
				
			} else if (units.get(unit.id).hp == unit.hp+1){
				//stoimy gdzies indziej
			} else {
				//Nie powinno być takiego pola
				logger.error("game.checkUnitParameters : stracilismy dziwna ilosc zycia (moze nas uderzono) : "
						+(units.get(unit.id).hp - unit.hp));
			}
			//Updatujemy zycie
			units.get(unit.id).hp = unit.hp;
		} else {
			//wiemy co to za pole i odjelismy wczesniej odpowiednia wartosc od zycia
			if (units.get(unit.id).hp != unit.hp){
				if (!units.get(unit.id).standingOnNull){
					logger.warning("Warning - game.checkUnitParameters : stracilismy dziwna ilosc zycia (moze nas uderzono) : "
							+(units.get(unit.id).hp - unit.hp));
					units.get(unit.id).hp = unit.hp;
				} else {
					logger.finer("Stalismy na nullu, wiec nie wiadomo bylo jaka ilosc zycia stracilismy. Teraz poprawiamy.");
					units.get(unit.id).hp = unit.hp;
				}
			}
			units.get(unit.id).standingOnNull = false;
		}
		
		if (units.get(unit.id).orientation != unit.orientation){
			//Nieprawidlowa orientacja
			logger.error("game.getGameStatus : Jednostka ma nieprawidłową orientacje : "+unit.orientation);
			units.get(unit.id).orientation = unit.orientation;
		}
		if (!units.get(unit.id).action.equalsIgnoreCase(unit.action)){
			if (unit.action.equalsIgnoreCase("") && units.get(unit.id).action.equalsIgnoreCase("dragging")){
				//Zabrał nam ktoś diament
				logger.fine("Zabral nam ktos obiekt");
				units.get(unit.id).action = "";
				++diamondsStolen;
			} else {
				logger.error("game.getGameStatus : Jednostka wykonuje nieprawidlowa akcje : "+unit.action);
				units.get(unit.id).action = unit.action;
			}
		}
		
		if (unit.player != playerID){
			//Nie zgadza sie ID playera
			logger.error("game.getGameStatus : Nie zgadza sie numer playera jednostki id ="+unit.player);
		}
		
	}
	
	public void getGameStatus(GameStatus gameStatus){
		
		//Czyscimy logi
		logString = "";
		
		//Ustawiamy aktualna jednostke
		actualUnit = gameStatus.units.get(0).id;
		
		if (actionPerformed == false){
			logger.severe("Wyslano nowy status gry a jeszcze nie wykonano poprzedniej akcji");
		}
		
		//Sprawdzamy playerID
		if (playerID == null){
			playerID = gameStatus.units.get(0).player;
			points = gameStatus.points;
			logger.info("Jestesmy graczem nr "+playerID+" (points = "+points+")");
		} else {
			//Sprawdzamy czy podali poprawne
			if (gameStatus.units.get(0).player != playerID){
				logger.error("game.getGameStatus - Jednostka ma niewlasciwe ID gracza : "
						+gameStatus.units.get(0).player);
			}
		}
		
		if (points != gameStatus.points){
			//Jezeli mamy niewlasciwa ilosc punktow wypisujemy tylko blad i poprawiamy
			logger.error("game.getGameStatus - Niewlasciwa ilosc punktow.");
			logger.error(" wyliczone : "+points+", podane przez serwer : "+gameStatus.points);
			points = gameStatus.points;
		}
		
		//Sprawdzamy numer rundy
		if (roundNumber == null){
			if (gameStatus.round != 1){
				logger.error("GameEngine.getGameStatus - Pierwsza tura nie ma numeru 1");
			}
			roundNumber = gameStatus.round-1;
		}
		
		boolean wrongTurn = false;
		if (gameStatus.round == roundNumber+1){
			
			++roundNumber;
			//Poczatek kolejnej rundy, robimy odstep
			logger.info();
			logger.info();
			logger.info(new Date() + " runda nr "+roundNumber+" punkty : "
					+gameStatus.points+" czas : "+String.format("%.2f", gameStatus.timeElapsed));
			logger.info(new Date() + " runda nr "+roundNumber+" punkty : "
					+gameStatus.points+" czas : "+String.format("%.2f", gameStatus.timeElapsed));
			
			//Czyscimy rezerwacje pol
			taken.clear();
			if (units.size() != 0){
				
				unitTurn = (unitTurn)%units.size()+1;
				
				if (unitTurn != 1){
					if (units.get(gameStatus.units.get(0).id).unitTurn == 1){
						System.out.println("Error : Chyba zginela jednostka, ktora nie miala zginac");
						int unitsCount = units.size();
						if (unitsCount <= 1){
							System.out.println("Co jest w ogole nie tak z numeracja jednostek. Mamy "+unitsCount+" jednostek.");
						} else {
							while (unitTurn != 1){
								//usuwamy jednostke z unitTurn
								Unit unitToDelete = null;
								for (Unit currentUnit : units.values()){
									if (currentUnit.unitTurn == unitTurn){
										unitToDelete = currentUnit;
										break;
									}
								}
								if (unitToDelete != null){
									System.out.println("Usuwamy jednostke nr "+unitToDelete.id+". Widocznie zginela.");
									units.remove(unitToDelete.id);
								} else {
									System.out.println("Error : Cos jest nie tak, nie wykrylismy jednostki o numerze tury : "+unitTurn);
								}
								unitTurn = (unitTurn)%unitsCount+1;
							}
						}
					} else {
						System.out.println("Error : Chyba zginela nam pierwsza jednostka");
					}
				}
				
			} else unitTurn = 1;
			
			updateUnitsHealth();
			
		} else if (gameStatus.round == roundNumber){
			++unitTurn;
			//Jest ciagle ta sama runda
		} else {
			logger.error("Poprzednia tura to : "+roundNumber+", a teraz mamy : "+gameStatus.round);
			roundNumber = gameStatus.round;
			wrongTurn = true;
		}
		
		//Przekształcamy współrzędne +6 do każdej (musi być parzysta liczba zeby 
		// nie zmienic parzystosci)
		for (int i = 0;i < gameStatus.units.size();++i){
			gameStatus.units.get(i).posX += 6;
			gameStatus.units.get(i).posY += 6;
		}
		
		//Usuwamy przeciwnikow, pewnie sa i tak na innej pozycji juz
		for (int x = 0;x < MAX_MAPSIZEX;++x)
			for (int y = 0;y < MAX_MAPSIZEY;++y)
				if (map[x][y].field != null && map[x][y].field.unit != null){
					map[x][y].field.unit = null;
				}
		
		//Dodajemy nowe pola, które widzą postacie
		for (int unit = 0;unit < gameStatus.units.size();++unit){
			for (Orientation o : Orientation.values()){
				//println("Widzimy w kierunku "+orientationToString(vis.direction)
				//		+ " pole = "+((vis.field==null)?"null":"nie null"));
				map[gameStatus.units.get(unit).posX][gameStatus.units.get(unit).posY]
						.edges.get(o).field = gameStatus.units.get(unit).sees.get(o);
			}
		}
		
		//Wypisujemy kogo tura teraz jest
		logger.info();
		logger.info("Teraz jest tura jednostki "+gameStatus.units.get(0).id
				+" (hp = "+gameStatus.units.get(0).hp+") : ");
		
		//Gdzie jestesmy
		logger.fine("Jednostka jest na pozycji x = "
				+gameStatus.units.get(0).posX+" y = "+gameStatus.units.get(0).posY);

		if (units.containsKey(gameStatus.units.get(0).id)){
			logger.finer("Jednostka juz sie znajduje w liście jednostek");

			if (!wrongTurn){
				//Jezeli nie mylimy sie w rundach to jednostka powinna miec takie parametry jak sadzimy
				checkUnitParameters(gameStatus.units.get(0));
			}
			
		} else {
			//Zapisujemy jednostke
			logger.info("Zapisujemy jednostke");
			int lastUnitTurn = 0;
			for (Unit unit : units.values()){
				if (unit.unitTurn > lastUnitTurn) lastUnitTurn = unit.unitTurn;
			}
			logger.info("Przydzielamy jednostce nowy numer tury : "+(lastUnitTurn+1));
			gameStatus.units.get(0).unitTurn = lastUnitTurn + 1;
			
			//Zapisujemy jej startowa pozycje
			gameStatus.units.get(0).startPosX = gameStatus.units.get(0).posX;
			gameStatus.units.get(0).startPosY = gameStatus.units.get(0).posY;
			
			gameStatus.units.get(0).inFight = false;
			gameStatus.units.get(0).freeUnit = false;
			
			units.put(gameStatus.units.get(0).id, gameStatus.units.get(0));

		}
		
		//ustawiamy action na null
		action = null;
		
		AIMain(gameStatus);
		
		if (action != null){
			logger.info("GameEngine.getGameStatus - Probujemy wykonac akcje : "+XMLCreator.createActionMessage(action));
		} else {
			NewActionChain actionChain = new NewActionChain(gameStatus.units.get(0));
			actionChain.add(newRotateLeft());
			logger.warning("GameEngine.getGameStatus - Nie mamy akcji, robimy : "+XMLCreator.createActionMessage(action));
		}
		
		actionListener.getAction(action);
		
		actionPerformed = false;
		
		//Zapisujemy log
		units.get(gameStatus.units.get(0).id).log = logString;
		
	}
	
	public void getActionConfirmation(){
		
		logger.info(new Date() + " Wykonano poprawnie akcje");
		//Poprawiamy polozenie jednostki
		Unit unit = units.get(action.unitID);
		
		if (action.actionType == ActionType.MOVE){
			logger.fine("wykonaną akcją był ruch");
			if (unit.action.equalsIgnoreCase("dragging")){
				logger.finer("Jednostka miala action=dragging");
				Node objectNode = map[unit.posX][unit.posY].edges.get(unit.orientation);
				MapField.Object object = objectNode.field.object;
				objectNode.field.object = null;
				boolean reservedObject = objectNode.reservedObject;
				objectNode.reservedObject = false;
				logger.finer("Trzymala obiekt : "+object);
				if (map[unit.posX][unit.posY].edges.get(action.actionMoveOrientation).
					edges.get(unit.orientation).field != null)
				{
					Node nextObjectNode = objectNode.edges.get(action.actionMoveOrientation);
					logger.finer("Obiekt przemiescil sie na pole x = "+nextObjectNode.x +
							" y = "+nextObjectNode.y);
					nextObjectNode.field.object = object;
					nextObjectNode.reservedObject = reservedObject;
					if (reservedObject){
						unit.reservedObjectNode = nextObjectNode;
					}
				} else {
					//jezeli pole na ktore stawiamy obiekt jest nullem
					Node nextObjectNode = objectNode.edges.get(action.actionMoveOrientation);
					logger.finer("Obiekt przemiescil sie na pole x = "+nextObjectNode.x +
							" y = "+nextObjectNode.y);
					nextObjectNode.field = new MapField();
					nextObjectNode.field.object = object;
					nextObjectNode.reservedObject = reservedObject;
					if (reservedObject){
						unit.reservedObjectNode = nextObjectNode;
					}
				}
			}
			unit.posX = map[unit.posX][unit.posY].edges.get(action.actionMoveOrientation).x;
			unit.posY = map[unit.posX][unit.posY].edges.get(action.actionMoveOrientation).y;
		} else if (action.actionType == ActionType.ACTION){
			switch (action.actionActionType){
			case DRAG:
				unit.action = "dragging";
				break;
			case DROP:
				unit.action = "";
				if (map[unit.posX][unit.posY].edges.get(unit.orientation).field != null
						&& map[unit.posX][unit.posY].edges.get(unit.orientation).field.building == Building.ALTAR
						&& map[unit.posX][unit.posY].edges.get(unit.orientation).field.buildingPlayer == playerID
						&& map[unit.posX][unit.posY].edges.get(unit.orientation).field.object == MapField.Object.DIAMOND)
				{
					//Jezeli wrzucamy diament do oltarza
					points += 100;
					//I usuwamy ten diament
					Node objectNode = map[unit.posX][unit.posY].edges.get(unit.orientation);
					objectNode.field.object = null;
					objectNode.reservedObject = false;
					unit.reservedObjectNode = null;
				}
				break;
			case HEAL:
				//FIXME tutaj robimy zalozenie ze jednostka ma 100 zycia maksymalnie
				int healed = Math.min(points,100-unit.hp);
				points -= healed;
				unit.hp += healed;
				logger.fine("Uleczylismy jednostke "+unit.id+". Ma teraz "+unit.hp+" zycia.");
				break;
			default:
				logger.error("GameEngine.getActionConfirmation - Nieznany typ akcji w akcji - "+action.actionActionType);
				break;
			}
		} else if (action.actionType == ActionType.ROTATE){
			if (action.actionRotationType == ActionRotationType.ROTATE_LEFT){
				if (unit.action.equalsIgnoreCase("dragging")){
					Node objectNode = map[unit.posX][unit.posY].edges.get(unit.orientation);
					MapField.Object object = objectNode.field.object;
					objectNode.field.object = null;
					boolean reservedObject = objectNode.reservedObject;
					objectNode.reservedObject = false;
					Node nextObjectNode = map[unit.posX][unit.posY].edges.get(unit.orientation.rotateLeft());
					nextObjectNode.field.object = object;
					nextObjectNode.reservedObject = reservedObject;
					if (reservedObject){
						unit.reservedObjectNode = nextObjectNode;
					}
				}
				unit.orientation = unit.orientation.rotateLeft();
			} else {
				if (unit.action.equalsIgnoreCase("dragging")){
					Node objectNode = map[unit.posX][unit.posY].edges.get(unit.orientation);
					MapField.Object object = objectNode.field.object;
					objectNode.field.object = null;
					boolean reservedObject = objectNode.reservedObject;
					objectNode.reservedObject = false;
					Node nextObjectNode = map[unit.posX][unit.posY].edges.get(unit.orientation.rotateRight());
					nextObjectNode.field.object = object;
					nextObjectNode.reservedObject = reservedObject;
					if (reservedObject){
						unit.reservedObjectNode = nextObjectNode;
					}
				}
				unit.orientation = unit.orientation.rotateRight();
			}
		} else {
			logger.error("GameEngine.getActionConfirmation - Nieznany typ akcji - "+action.actionType);
		}
		actionPerformed = true;
		
	}
	
	public void showStatistics(){
		logger.info();
		logger.info("Statystyki : ");
		logger.info(" Zabrano nam "+diamondsStolen+" diamentów.");
		logger.info();
	}
	
	public void getActionRejection(){
		//TODO tutaj powinnismy dodac znak zapytania na mape gry
		Unit unit = units.get(action.unitID);
		logger.info(new Date() + " Nie udalo się poprawnie wykonać akcji");
		if (action.actionType == ActionType.MOVE){
			logger.fine("Byl to ruch");
			if (unit.action.equalsIgnoreCase("dragging")){
				logger.fine("Trzymalismy przy tym przedmiot");
				if (action.actionMoveOrientation == unit.orientation
						|| action.actionMoveOrientation == unit.orientation.rotateLeft()
						|| action.actionMoveOrientation == unit.orientation.rotateRight()){
					logger.fine("Nie udalo sie przepchnac przedmiotu, bo cos stoi na drodze. Wstawiamy Object.UNKNOWN");
					Node obstacleField = map[unit.posX][unit.posY].edges.get(unit.orientation).edges.get(action.actionMoveOrientation);
					if (obstacleField.field != null){
						obstacleField.field.object = MapField.Object.UNKNOWN;
					}
				}
			}
		} else if (action.actionType == ActionType.ACTION){
			switch (action.actionActionType){
			case DRAG:
				logger.info("Byla to akcja drag");
				break;
			case DROP:
				logger.info("byla to akcja drop");
				break;
			case HEAL:
				logger.info("Byla to akcja heal");
				break;
			default:
				logger.error("GameEngine.getActionRejection - Nieznany typ akcji w akcji - "+action.actionActionType);
				break;
			}
		} else if (action.actionType == ActionType.ROTATE){
			logger.info("Byla to akcja rotate");
		} else {
			logger.error("GameEngine.getActionConfirmation - Nieznany typ akcji - "+action.actionType);
		}
		actionPerformed = true;
	}
	
	public int getRoundNumber(){
		return roundNumber;
	}
	
	public void setGameResult(GameResult gameResult){
		this.gameResult = gameResult;
	}
	
	public GameResult getGameResult(){
		return gameResult;
	}
	
}
