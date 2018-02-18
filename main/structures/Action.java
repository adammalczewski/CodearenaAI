package structures;

import xml.XMLCreator;

public class Action {

	static public enum ActionType{
		ACTION,ROTATE,MOVE
	}
	
	static public enum ActionActionType{
		DRAG,DROP,HEAL,FIGHT
	}
	
	static public enum ActionRotationType{
		ROTATE_LEFT,ROTATE_RIGHT
	}
	
	public ActionType actionType;
	public ActionActionType actionActionType;
	public ActionRotationType actionRotationType;
	public Orientation actionMoveOrientation;
	public int unitID;
	
	@Override
	public String toString(){
		return XMLCreator.createActionMessage(this);
	}
	
}
