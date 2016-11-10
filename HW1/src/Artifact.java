import java.util.Date;

/*
 * id, name, creator, date of creation, place of creation, genre etc
 */
public class Artifact
{
	public final int ID;
	public final String name;
	public final Date creationDate;
	public final String creationPlace;
	public final String genre;
	
	Artifact(int ID, String name, Date creationDate, String creationPlace, String genre)
	{
		this.ID = ID;
		this.name = name;
		this.creationDate = creationDate;
		this.creationPlace = creationPlace;
		this.genre = genre;
	}
}
