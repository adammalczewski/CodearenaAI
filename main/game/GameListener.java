package game;


import structures.ActionRejection;
import structures.GameStatus;
import structures.ServerResponse;

public interface GameListener {

	public void getServerResponse(ServerResponse response);
	
	public void getActionConfirmation();
	
	public void getActionRejection(ActionRejection actionRejection);
	
	public void getGameStatus(GameStatus gameStatus);
	
}
