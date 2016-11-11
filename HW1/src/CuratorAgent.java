import jade.core.Agent;
import java.util.Hashtable;

/*
 * Curator Agent monitors the gallery/museum.
 * A gallery/museum contains detailed information of artifacts such as
 * id, name, creator, date of creation, place of creation, genre, etc.
 */
public class CuratorAgent extends Agent {
	
	public static final String AGENTTYPE = "Curator Agent";
	private Hashtable artifactCalatalogue;

	@Override
	protected void setup()
	{
		System.out.println("Hi am up "+ AGENTTYPE + " " + getAID().getName());

		//Register my service



		// respond to tour agent for build tour

		//respond to profiler giving detailed information about artifacts

		//agent terminating

	}

    protected void takeDown()
    {
        System.out.println("CuratorAgent Terminating"+getAID().getName());
    }
}
