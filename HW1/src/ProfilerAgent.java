import java.util.LinkedList;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;

/*
 * Maintains the profile of the user.
 * Travels around the network and looks for interesting information about
 * art and culture from online museums or art galleries on the internet.
 * 
 * The Profiler Agent interacts directly with Tour Guide Agent to get a personalized virtual tour.
 * 
 * The Profile Agent interacts with Curator Agent to obtain detailed information about each of the
 * items stated in the virtual tour.
 */
public class ProfilerAgent extends Agent {
	
	private static final int SECONDS = 1000;
		
	private static final String AGENTTYPE = "Profiler Agent";
	private static final String SERVICETYPE = "ProfilerService";
	private final String SERVICENAME = "JadeProfilerService";
	
	private Profile UserProfile;
	private LinkedList<Tour> relevantTours;
		
	public ProfilerAgent() { }
	
	public ProfilerAgent(String AgentName)
	{
		getAID().setLocalName(AgentName);
	}
	
	public ProfilerAgent(String AgentName, Profile user)
	{
		getAID().setLocalName(AgentName);
		UserProfile = user;
	}
	
	public boolean setProfile(Profile user)
	{
		if (UserProfile == null)
		{
			UserProfile = user;
			return true;
		}
		
		System.err.println("Assigning user \"" + user.name + "\" to " + AGENTTYPE + " " + getAID().getName() + "failed,"
							+ " agent already busy");
		return false;
	}
	
	public boolean isRegistered()
	{
		return false;
	}
	
	public boolean isAssigned()
	{
		if (UserProfile == null) return false;
		return true;
	}
	
	@Override
	protected void setup()
	{
		System.out.println("Setting up " + AGENTTYPE + " " + getAID().getName());
		addBehaviours();
	}
	
	private void addBehaviours()
	{
		DFAgentDescription agentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();
		serviceDescription.setType(TourGuideAgent.SERVICETYPE);				// get all registered TG Agents
		agentDescription.addServices(serviceDescription);
		
		addBehaviour(new TickerBehaviour(this, 60*SECONDS)
		{
			@Override
			protected void onTick()
			{
				try {
					DFAgentDescription[] result = DFService.search(myAgent, agentDescription);
					filterInteresting(result);
				} 
				catch (FIPAException e)
				{
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed searching for " +
										TourGuideAgent.SERVICETYPE + "s");
				}
			}
		});
	}
	
	private void filterInteresting(DFAgentDescription[] result)
	{
		// TODO: Contact each Tour Agent, request personal tour by sending User Profile
		// TODO: Save all tours that are relevant
		// relevantTours = ...
	}

}
