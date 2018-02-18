package structures;

import java.util.EnumMap;
import java.util.LinkedList;

public class Node{
	
	public EnumMap<Orientation,Node> edges;
	public MapField field;
	public int x;
	public int y;
	public boolean marked = false;
	public boolean reservedObject = false;
	public String value;
	
	public LinkedList<Integer> taken;
	
	public Node(){
		edges = new EnumMap<Orientation,Node>(Orientation.class);
		taken = new LinkedList<Integer>();
	}
	
}