package structures;

import java.util.ArrayList;
import java.util.EnumMap;

public class Unit {
	
	public static class FightParams{
		ArrayList<Unit> units;
		int attackPosX;
		int attackPosY;
		int actionsUsed;
		int fightsUsed;
		int hpTaken;
	}

	public int id;
	public int posX;
	public int posY;
	public int level;
	public Integer startPosX;
	public Integer startPosY;
	
	public int attacked;
	
	public boolean freeUnit;
	public boolean inFight;
	public FightParams fightParams;
	
	public int hp;
	public String status;
	public String action;
	public Orientation orientation;
	public int player;
	public boolean once = true;
	public String log;
	public String priorityAction;
	public boolean standingOnNull = false;
	public Node reservedObjectNode = null;
	public boolean checkHealing = false;
	
	public int unitTurn;
	
	public EnumMap<Orientation,MapField> sees;
	
	public Unit(){
		sees = new EnumMap<Orientation,MapField>(Orientation.class);
	}
	
	public UnitPosition getPos(){
		return new UnitPosition(posX,posY,orientation);
	}
	
	
}
