import java.util.LinkedList;
import java.util.ListIterator;

public class Tour
{
	private final LinkedList<Integer> artifactIDs;
	private ListIterator<Integer> li;
	
	
	Tour(LinkedList<Integer> artifactIDs)
	{
		this.artifactIDs = artifactIDs;
	}
	
	public void fromBeginning()
	{
		li = artifactIDs.listIterator();
	}
	
	public Integer getNext()
	{
		if (li.hasNext()) return li.next();
		return null;
	}
	
}
