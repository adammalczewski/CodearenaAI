package structures;

public class ServerResponse {
	
	public enum ResponseID{
		WRONG_CREDENTIALS,BUSY,GAME_READY,WAITING_FOR_PLAYER,CHECK_WWW,GAME_CLOSED
	}
	
	public ResponseID id;
	public int game;
}
