import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

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
	
	private LinkedList<Artifact> artifacts;
	
	@Override
	protected void setup()
	{
		System.out.println("Setting up " + AGENTTYPE + " " + getAID().getName());
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
	
	
	private void addBehaviours()
	{
		/*
		 * Behaviour for requesting artifacts and registering with the DF
		 */
		addBehaviour(new OneShotBehaviour()
		{
			@Override
			public void action()
			{
				DFAgentDescription agentDescription = new DFAgentDescription();		// The "agent" we are looking for
				ServiceDescription serviceDescription = new ServiceDescription();	// The service tied to the "agent"
				serviceDescription.setType(CuratorAgent.SERVICETYPE);				// Should be of type InquiryService
				agentDescription.addServices(serviceDescription);
				
				try
				{
					DFAgentDescription[] result = DFService.search(myAgent, agentDescription);
					
					ACLMessage artifactRequest = new ACLMessage(ACLMessage.REQUEST);
					artifactRequest.setContent(CuratorAgent.REQUESTALL);			// Request all artifacts
					
					// Add all Curators as receivers for the artifact request
					for (int i = 0; i < result.length; i++) artifactRequest.addReceiver(result[i].getName());
					
					send(artifactRequest);
				}
				catch (FIPAException e1)
				{
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed searching for " +
										CuratorAgent.SERVICETYPE + "s");
					doDelete();
				}

				
				// REGISTER WITH DF --------------------------------------------
				agentDescription = new DFAgentDescription();
				serviceDescription = new ServiceDescription();
				
				agentDescription.setName(getAID());						// set Agent name
				serviceDescription.setName(SERVICENAME);				// set Service name
				serviceDescription.setType(SERVICETYPE);				// set Service type
				
				agentDescription.addServices(serviceDescription);		// Add the Service to the Agent
				
				try {
					DFService.register(myAgent, agentDescription);		// Register this Agent with the Directory Facilitator
					isRegistered = true;
					System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " registered with the DF");
				}
				catch (FIPAException e)
				{
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed to register with the DF");
					doDelete();											// Kill Agent
				}
			}
		});
		
		/*
		 * Behaviour for receiving requests (Profile) from ProfilerAgent
		 */
		addBehaviour(new CyclicBehaviour()
		{
			@Override
			public void action()
			{
				// TODO: Capture all receives for artifact request
				// TODO: Change OneShot to Waker to add initial delay (to give Curator time to set up)
				
				// Wait until received message (request by ProfilerAgent) of type REQUEST
				ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
				
				if (msg != null)
				{
					try {
						Profile userProfile = (Profile)msg.getContentObject();	// get Profile sent from ProfilerAgent
						
						ACLMessage reply = msg.createReply();
						
						Tour personalTour = getPersonalTour(userProfile);		// Create personal tour
						
						if (personalTour.length() > 0)							// interesting tour can be offered
						{
							reply.setPerformative(ACLMessage.PROPOSE);
							try
							{
								reply.setContentObject(personalTour);
							}
							catch (IOException e)
							{
								System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed sending Tour");
							}
						}
						else {
							reply.setPerformative(ACLMessage.FAILURE);
						}
						
						myAgent.send(reply);
					} 
					catch (UnreadableException e)
					{
						System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed interpeting received msg");
					}
				}
				block();
			}
		});
	}
	
	private Tour getPersonalTour(Profile userProfile)
	{
		LinkedList<Integer> artifactIDs = new LinkedList<Integer>();
		
		ListIterator<Artifact> li = artifacts.listIterator();
		Artifact art;
		
		while (li.hasNext())			// for every Artifact that this TG is interested in
		{
			art = li.next();
			if (userProfile.interests.contains(art.genre)) artifactIDs.add(art.ID);		// user also interested; add to tour
		}
		
		return new Tour(artifactIDs);
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
