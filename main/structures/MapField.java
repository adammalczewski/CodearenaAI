package structures;

public class MapField {

	public enum Background{
		GRASS,FOREST,CRYSTALFLOOR,SWAMP,STONE,VOID
	}
	
	public enum Object{
		STONE,DIAMOND,UNKNOWN
	}
	
	public enum Building{
		ALTAR
	}
	
	public Background background;
	public Object object;
	public Building building;
	public int buildingPlayer;
	public SeenUnit unit;
	
	public MapField(){
		background = null;
		object = null;
		building = null;
		unit = null;
	}
	
	public boolean crossableTerrain(){
		return (building == null && (background == Background.CRYSTALFLOOR || background == Background.FOREST
				|| background == Background.GRASS || background == Background.SWAMP));
	}
	
	public boolean crossable(){
		return (building == null && object == null && unit == null &&
				(background == Background.CRYSTALFLOOR || background == Background.FOREST
				|| background == Background.GRASS || background == Background.SWAMP));
	}
	
	public boolean crossableDisregardingObject(MapField.Object obj){
		return (building == null && (object == null || object == obj) && unit == null &&
				(background == Background.CRYSTALFLOOR || background == Background.FOREST
				|| background == Background.GRASS || background == Background.SWAMP));
	}
	
	public boolean crossableByObject(){
		return (building == null && object == null && unit == null &&
				(background == Background.CRYSTALFLOOR || background == Background.GRASS
				|| background == Background.SWAMP || background == Background.VOID));
	}
	
	public boolean crossableByObjectDisregardingObject(MapField.Object obj){
		return (building == null && (object == null || object == obj) && unit == null &&
				(background == Background.CRYSTALFLOOR || background == Background.GRASS
				|| background == Background.SWAMP || background == Background.VOID));
	}
	
	public boolean crossableByObject(MapField.Object obj){
		if (obj == MapField.Object.DIAMOND){
			return crossableByDiamond();
		} else return crossableByObject();
	}
	
	public boolean crossableByObjectDisregardingObject(MapField.Object obj,MapField.Object objD){
		if (obj == MapField.Object.DIAMOND){
			return crossableByDiamondDisregardingObject(objD);
		} else return crossableByObjectDisregardingObject(objD);
	}
	
	public boolean crossableByDiamond(){
		return ((building == null || building == Building.ALTAR) && object == null && unit == null &&
				(background == Background.CRYSTALFLOOR || background == Background.GRASS
				|| background == Background.SWAMP || background == Background.VOID));
	}
	
	public boolean crossableByDiamondDisregardingObject(MapField.Object obj){
		return ((building == null || building == Building.ALTAR) && (object == null || object == obj) && unit == null &&
				(background == Background.CRYSTALFLOOR || background == Background.GRASS
				|| background == Background.SWAMP || background == Background.VOID));
	}
	
}
