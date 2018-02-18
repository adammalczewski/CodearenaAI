package xml;

import structures.Action;
import structures.Action.ActionActionType;
import structures.Action.ActionRotationType;
import structures.Action.ActionType;

public class XMLCreator {

	public static String createActionMessage(Action action){
		
		StringBuilder result = new StringBuilder("");
		
		result.append("<unit id=\"");
		result.append(action.unitID);
		result.append("\"><go ");
		
		if (action.actionType == ActionType.ACTION){
			result.append("action=");
			if (action.actionActionType == ActionActionType.DRAG){
				result.append("\"drag\"");
			} else if (action.actionActionType == ActionActionType.DROP){
				result.append("\"drop\"");
			} else if (action.actionActionType == ActionActionType.HEAL){
				result.append("\"heal\"");
			} else if (action.actionActionType == ActionActionType.FIGHT){
				result.append("\"attack\"");
			} else {
				System.out.println("XMLCreator.createActionMessage - niewlasciwy typ akcji action");
			}
		} else if (action.actionType == ActionType.MOVE){
			result.append("direction=\""+action.actionMoveOrientation+"\"");
		} else if (action.actionType == ActionType.ROTATE){
			result.append("rotate=");
			if (action.actionRotationType == ActionRotationType.ROTATE_LEFT){
				result.append("\"rotateLeft\"");
			} else if (action.actionRotationType == ActionRotationType.ROTATE_RIGHT){
				result.append("\"rotateRight\"");
			} else {
				System.out.println("Error : XMLCreator.createActionMessage - niewlasciwy typ akcji rotate");
				
			}
		} else System.out.println("Error : XMLCreator.createActionMessage - niewlasciwy typ akcji");
		
		result.append("/></unit>");
		
		
		return result.toString();
	}
	
}
