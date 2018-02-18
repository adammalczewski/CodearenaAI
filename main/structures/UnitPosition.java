package structures;

public class UnitPosition{
	public int posX;
	public int posY;
	public Orientation orientation;
	
	public UnitPosition(int posX,int posY,Orientation o){
		this.posX = posX;
		this.posY = posY;
		this.orientation = o;
	}
	
	public void rotateLeft(){
		orientation = orientation.rotateLeft();
	}
	
	public void rotateRight(){
		orientation = orientation.rotateRight();
	}
	
	public void move(Orientation o,Node [][] map){
		int newPosX = map[posX][posY].edges.get(o).x;
		posY = map[posX][posY].edges.get(o).y;
		posX = newPosX;
	}
	
	@Override
	public String toString(){
		return "x = "+posX+" y = "+posY+ " o = "+orientation;
	}
	
	@Override
	public boolean equals(Object obj){
		UnitPosition pos = (UnitPosition)obj;
		return pos.posX == posX && pos.posY == posY && pos.orientation == orientation;
	}
	
}
