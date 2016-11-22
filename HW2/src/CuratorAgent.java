
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


@SuppressWarnings("serial")
public class CuratorAgent extends Agent {
	
	public static final String AGENTTYPE = "Curator Agent";
	public static final String SERVICETYPE = "BiddingService";
	public static final String SERVICENAME = "CuratorBidder";
	
	private AuctionItem item;
	private int maxPrice = 50;

	
	public CuratorAgent() { }
	
	public CuratorAgent(int maxPrice)
	{
		this.maxPrice = maxPrice;
	}

	
	@Override
	protected void setup()
	{
        System.out.println(AGENTTYPE + " " + getAID().getName() + " is created");
        addBehaviours();
	}
	
	private void addBehaviours()
	{
		// Register with DF
		addBehaviour(new OneShotBehaviour()
		{
			@Override
			public void action()
			{
				DFAgentDescription agentDescription = new DFAgentDescription();
				ServiceDescription serviceDescription = new ServiceDescription();
				
				agentDescription.setName(getAID());						// set Agent name
				serviceDescription.setName(SERVICENAME);				// set Service name
				serviceDescription.setType(SERVICETYPE);				// set Service type
				
				agentDescription.addServices(serviceDescription);		// Add the Service to the Agent
				
				try {
					DFService.register(myAgent, agentDescription);
					
					// Handle auction communication
					addBehaviour(new stepBehaviour());
				}
				catch (FIPAException e)
				{
					System.err.println(AGENTTYPE + " " + getAID().getName() + " failed to register with DF");
				}
			}
		});
	}
	
	private ACLMessage getProposal(ACLMessage cfp)
	{
		ACLMessage proposal = cfp.createReply();
		cfp.setPerformative(ACLMessage.PROPOSE);
		int biddingPrice = Integer.parseInt(cfp.getContent());
		
		if (item.type == AuctionItem.PAINTING && biddingPrice <= maxPrice) proposal.setContent(""+biddingPrice);
		else proposal.setContent(""+0);
		
		return proposal;
	}
	
	private class stepBehaviour extends Behaviour
	{
		int step = 0;
		final static int END = 10;
		
		@Override
		public void action()
		{
			switch (step)
			{
			case 0:		// inform of new auction
				ACLMessage newAuctionMsg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				System.out.println(AGENTTYPE + " " + getAID().getName() + " got notification of new auction");
				
				if (newAuctionMsg != null)
				{
					try {
						item = (AuctionItem)newAuctionMsg.getContentObject();
						step++;
					}
					catch (UnreadableException e)
					{
						System.err.println(AGENTTYPE + " " + getAID().getName() + " failed casting received msg");
						step = END;
					}
				}
				break;
			case 1:		// call for proposals or auction termination
				ACLMessage cfpMsg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
				switch (cfpMsg.getPerformative())
				{
				case ACLMessage.CFP:
					ACLMessage proposalMsg = getProposal(cfpMsg);
					send(proposalMsg);
					step++;
				case ACLMessage.CANCEL:
					System.err.println(AGENTTYPE + " " + getAID().getName() + " got notification of auction ending");
					item = null;
					step = 0;		// Go back waiting for new auction to begin
				}
				break;
			case 2:		// status of sent proposal
				ACLMessage statusMsg = myAgent.receive();
				if (statusMsg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
				{
					System.out.println(AGENTTYPE + " " + getAID().getName() + " won auction item " + item.name);
				}
				else if (statusMsg.getPerformative() == ACLMessage.REJECT_PROPOSAL)
				{
					System.out.println(AGENTTYPE + " " + getAID().getName() + " lost auction item " + item.name);
				}
				step++;
				break;
			}
		}

		@Override
		public boolean done()
		{
			return step == END;
		}
		
	}
}
