import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

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
		// fake user
		LinkedList<String> userInterests = new LinkedList<String>();
		userInterests.add("flowers");
		userInterests.add("KTH");
		UserProfile = new Profile("Alice", 25, Profile.FEMALE, "student", userInterests);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Setting up " + AGENTTYPE + " " + getAID().getName());
		addBehaviours();
	}
	
	private void addBehaviours()
	{
		DFAgentDescription agentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();
		serviceDescription.setType(TourGuideAgent.SERVICETYPE);				// get all registered TG Agents
		agentDescription.addServices(serviceDescription);
		
		addBehaviour(new TickerBehaviour(this, 10*SECONDS)
		{
			@Override
			protected void onTick()
			{
				try {
					System.out.println(AGENTTYPE + " is looking for TourGuideAgents");
					DFAgentDescription[] result = DFService.search(myAgent, agentDescription);
					
					System.out.println("ProfilerAgent found " + result.length + " TourGuideAgents");
										
					// -----
					ACLMessage tourRequest = new ACLMessage(ACLMessage.REQUEST);
					tourRequest.setContentObject(UserProfile);
					
					// Add all TourAgents as receivers for the tour request
					for (int i = 0; i < result.length; i++) tourRequest.addReceiver(result[i].getName());
					
					System.out.println("ProfilerAgent will now send request for items to TourAgent");
					send(tourRequest);
					
					System.out.println("ProfilerAgent now waiting for answer from TourAgent");
					ACLMessage tourMsg = myAgent.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
					System.out.println("ProfilerAgent got answer from TourAgent");
					
						Tour t = (Tour)tourMsg.getContentObject();
						
						/*t.fromBeginning();
						for (int i = 0; i < t.length(); i++)
						{
							System.out.println("ID is " + t.getNext());
						}*/
						
						// -------
						
						DFAgentDescription agentDescription = new DFAgentDescription();		// The "agent" we are looking for
						ServiceDescription serviceDescription = new ServiceDescription();	// The service tied to the "agent"
						serviceDescription.setType(CuratorAgent.SERVICETYPE);				// Should be of type InquiryService
						agentDescription.addServices(serviceDescription);
						
							DFAgentDescription[] curatorResult = DFService.search(myAgent, agentDescription);
							
							System.out.println(AGENTTYPE + " found " + curatorResult.length + " number of curators");
							
							ACLMessage artifactRequest = new ACLMessage(ACLMessage.QUERY_REF);
							
							t.fromBeginning();
							int requestingID = (int)t.getNext();
							System.out.println(AGENTTYPE + " will request info with id " + requestingID);
							
							artifactRequest.setContentObject(requestingID);		// Request all artifacts
							
							System.out.println(AGENTTYPE + " will now add curators to sending list");
							
							// Add all Curators as receivers for the artifact request
							for (int i = 0; i < curatorResult.length; i++)
							{
								artifactRequest.addReceiver(curatorResult[i].getName());
							}
							
							
							System.out.println(AGENTTYPE + " will now send request for items to Curator");
							send(artifactRequest);
							
							System.out.println(AGENTTYPE + " now waiting for answer from Curator");
							ACLMessage curatorMsg = myAgent.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
							System.out.println(AGENTTYPE + " got answer from curator");
							
							Artifact artInfo = (Artifact)curatorMsg.getContentObject();
							System.out.println("First object on tour is:" + artInfo.name + " with ID " + artInfo.ID);
						
						// ------
					
				} 
				catch (FIPAException | IOException e)
				{
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed searching for " +
										TourGuideAgent.SERVICETYPE + "s");
				} catch (UnreadableException e)
				{
					System.err.println(AGENTTYPE + " failed reading Tour");
				}
			}
		});
	}

}
