import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
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
	
	public static final int SECONDS = 1000;
	public static final String AGENTTYPE = "Tour Guide Agent";
	public static final String SERVICETYPE = "TourGuide";
	private final String SERVICENAME = "JadeTourGuide";
	
	private boolean isRegistered = false;
	
	private List<Artifact> artifacts;
	
	@Override
	protected void setup()
	{	
		System.out.println("Setting up " + AGENTTYPE + " " + getAID().getName());
		addBehaviours();
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
		addBehaviour(new WakerBehaviour(this, 1*SECONDS)
		{
			@Override
			public void handleElapsedTimeout()
			{
				DFAgentDescription agentDescription = new DFAgentDescription();		// The "agent" we are looking for
				ServiceDescription serviceDescription = new ServiceDescription();	// The service tied to the "agent"
				serviceDescription.setType(CuratorAgent.SERVICETYPE);				// Should be of type InquiryService
				agentDescription.addServices(serviceDescription);
				
				try
				{
					DFAgentDescription[] result = DFService.search(myAgent, agentDescription);
					
					System.out.println("TourGuide found " + result.length + " number of curators");
					
					ACLMessage artifactRequest = new ACLMessage(ACLMessage.REQUEST);
					artifactRequest.setContent(CuratorAgent.REQUESTALL);			// Request all artifacts
					
					// Add all Curators as receivers for the artifact request
					for (int i = 0; i < result.length; i++) artifactRequest.addReceiver(result[i].getName());
					
					System.out.println("TourGuide will now send request for items to Curator");
					send(artifactRequest);
					
					System.out.println("TourGUide now waiting for answer from Curator");
					ACLMessage curatorMsg = myAgent.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
					System.out.println("TourGuide got answer from curator");
					try {
						List<Artifact> list = (List<Artifact>) curatorMsg.getContentObject();
						artifacts = list;
						
						/*System.out.println("Got a list of artifacts:");
						for (int i = 0; i < list.size(); i++)
						{
							System.out.println(list.get(i).ID + ": " + list.get(i).name);
						}*/
						
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
						
						System.out.println(AGENTTYPE + " finished registering. Will not wait for ProfilerAgents");
					}
					catch (UnreadableException e)
					{
						System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed to cast Artifact list");
						doDelete();
					}
				}
				catch (FIPAException e1)
				{
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed searching for " +
										CuratorAgent.SERVICETYPE + "s");
					doDelete();
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
				System.out.println(AGENTTYPE + " inside CyclicBehaviour");
				
				// Wait until received message (request by ProfilerAgent) of type REQUEST
				ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
				
				if (msg != null)
				{
					System.out.println(AGENTTYPE + " got a non-null message, sent from " + msg.getSender().getLocalName());

					try {
						Profile userProfile = (Profile)msg.getContentObject();	// get Profile sent from ProfilerAgent
						
						ACLMessage reply = msg.createReply();
						
						System.out.println(AGENTTYPE + " will now create personal tour");
						Tour personalTour = getPersonalTour(userProfile);		// Create personal tour
						
						System.out.println("PersonalTour is " + personalTour.length());
						
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
						
						System.out.println(AGENTTYPE + " will now send tour");
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
			System.out.println("PersonalTour: working with artifact number " + art.ID + " of type " + art.genre);
			if (userProfile.interests.contains(art.genre))
			{
				System.out.println("User has interest in this");
				artifactIDs.add(art.ID);		// user also interested; add to tour
			}
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
