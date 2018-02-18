package structures;

import java.util.ArrayList;

public class GameStatus {

	public double timeElapsed;
	public int round;
	public int points;
	public String result;
	
	public ArrayList<Unit> units;
	
	public GameStatus(){
		units = new ArrayList<Unit>();
	}
	
}
