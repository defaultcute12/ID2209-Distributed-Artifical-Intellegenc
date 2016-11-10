import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/*
 * Contains list of related items (based on user’s interest, age, etc.).
 * Retrieves the information about artifacts in the gallery/museum
 * and builds a virtual tour (upon the request) for profiler agent.
 * 
 * Tour Guide agent interacts with Curator Agent in order to build the virtual tour.
 */
public class TourGuideAgent extends Agent {
	
	public static final String AGENTTYPE = "Tour Guide Agent";
	public static final String SERVICETYPE = "TourGuide";
	private final String SERVICENAME = "JadeTourGuide";
	
	private boolean isRegistered = false;
	
	@Override
	protected void setup()
	{
		System.out.println("Setting up " + AGENTTYPE + " " + getAID().getName());
		register();
	}
	
	@Override
	protected void takeDown()
	{
		if (isRegistered) deregister();
	}
	
	public boolean isRegistered()
	{
		return isRegistered;
	}
	
	/*
	 * Service Registration with the Directory Facilitator
	 */
	private void register()
	{
		DFAgentDescription agentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();
		
		agentDescription.setName(getAID());						// set Agent name
		serviceDescription.setName(SERVICENAME);				// set Service name
		serviceDescription.setType(SERVICETYPE);				// set Service type
		
		agentDescription.addServices(serviceDescription);		// Add the Service to the Agent
		
		try {
			DFService.register(this, agentDescription);			// Register this Agent with the Directory Facilitator
			isRegistered = true;
			System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " registered with the DF");
		}
		catch (FIPAException e)
		{
			System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed to register with the DF");
		}
	}
	
	private void deregister()
	{
		try {
			DFService.deregister(this);
			isRegistered = false;
			System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " deregistered with the DF");
		}
		catch (FIPAException e) {
			System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed to deregister with the DF");
		}
	}
}
