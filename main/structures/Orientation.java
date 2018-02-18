package structures;

public enum Orientation{
	NW(0),SW(4),W(5),NE(1),SE(3),E(2);
	private int value;
	
	private Orientation(int value){
		this.value = value;
	}
	
	public static Orientation fromInt(int value){
		switch (value){
		case 0:
			return NW;
		case 1:
			return NE;
		case 2:
			return E;
		case 3:
			return SE;
		case 4:
			return SW;
		case 5:
			return W;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	public int toInt(){
		switch (this){
		case E:
			return 2;
		case NE:
			return 1;
		case NW:
			return 0;
		case SE:
			return 3;
		case SW:
			return 4;
		case W:
			return 5;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	public Orientation reverse(){
		return fromInt((value+3)%6);
	}
	
	public Orientation rotateRight(){
		return fromInt((value+1)%6);
	}
	
	public Orientation rotateLeft(){
		return fromInt((value+5)%6);
	}
	
	public int rotationsRight(Orientation to){
		
		return ((to.value - value+6)%6);
		
	}
	
	public int rotationsLeft(Orientation to){
		
		return ((value-to.value+6)%6);
		
	}
	


}