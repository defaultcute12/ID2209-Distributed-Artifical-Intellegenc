import jade.core.Agent;


public class CuratorAgent extends Agent {
	@Override
	protected void setup()
	{
		System.out.println("Curator alive!");
		doClone(here(), (getAID().getLocalName() + "-Copy"));
	}
	
	@Override
	protected void beforeClone()
	{
		System.out.println(getAID().getLocalName() + " cloning locally at " + here().getName());
	}

}
