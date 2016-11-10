import jade.core.Agent;

/*
 * Curator Agent monitors the gallery/museum.
 * A gallery/museum contains detailed information of artifacts such as
 * id, name, creator, date of creation, place of creation, genre, etc.
 */
public class CuratorAgent extends Agent {
	
	public static final String AGENTTYPE = "Curator Agent";
	
	@Override
	protected void setup()
	{
		System.out.println("Setting up "+ AGENTTYPE + " " + getAID().getName());
	}
}
