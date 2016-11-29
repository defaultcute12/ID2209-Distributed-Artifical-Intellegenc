import java.io.Serializable;


@SuppressWarnings("serial")
public class AuctionItem implements Serializable {
	
	public static final String PAINTING = "p";
	public static final String SCULPTURE = "s";
	public static final String VASE = "v";
	
	public final int ID;
	public final String name;
	public final String type;
	
	public AuctionItem(int ID, String name, String type)
	{
		this.ID = ID;
		this.name = name;
		this.type = type;
	}
}
