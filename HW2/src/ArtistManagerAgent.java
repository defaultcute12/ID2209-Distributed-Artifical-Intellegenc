import java.io.IOException;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class ArtistManagerAgent extends Agent {
	
	public static final String AGENTTYPE = "Artist Manager Agent";
	
	private AuctionItem auctionItem;
	private int askingPrice;
	
	@Override
	protected void setup()
	{
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) { }
		
        System.out.println(AGENTTYPE + " " + getAID().getName() + " is created");
        
        auctionItem = new AuctionItem(1, "Mona Lisa", AuctionItem.PAINTING);
        askingPrice = 100;
        
        addBehaviour(new stepBehaviour());
	}
	
	private class stepBehaviour extends Behaviour
	{
		final static int END = 10;
		int step = 0;
		DFAgentDescription[] bidders;
		ACLMessage rejectProposalMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
		ACLMessage acceptProposalMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
		boolean isAccepted = false;
		int receivedProposals;
		MessageTemplate mt;

		@Override
		public void action()
		{
			switch (step)
			{
			case 0:		// fetch bidders from DF & inform of auction start
				DFAgentDescription agentDescription = new DFAgentDescription();		// The "agent" we are looking for
				ServiceDescription serviceDescription = new ServiceDescription();	// The service tied to the "agent"
				serviceDescription.setType(CuratorAgent.SERVICETYPE);				// Should be of type BiddingService
				agentDescription.addServices(serviceDescription);
				
				try
				{
					bidders = DFService.search(myAgent, agentDescription);
					
					System.out.println(AGENTTYPE + " " + getAID().getName() + " found " + bidders.length + " curators");
					
					ACLMessage auctionStartMsg = new ACLMessage(ACLMessage.INFORM);
					auctionStartMsg.setContentObject(auctionItem);
					
					// Add all Curators as receivers
					for (int i = 0; i < bidders.length; i++) auctionStartMsg.addReceiver(bidders[i].getName());
					
					System.out.println(AGENTTYPE + " " + getAID().getName() + " will now inform start of auction");
					send(auctionStartMsg);
					step++;
				}
				catch (FIPAException e)
				{
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed searching for " +
										CuratorAgent.SERVICETYPE + "s");
					doDelete();
				}
				catch (IOException e)
				{
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed serializing auction item");
					doDelete();
				}
				
				break;
			case 1:		// cfp
				ACLMessage cfpMsg = new ACLMessage(ACLMessage.CFP);
				cfpMsg.setConversationId(""+auctionItem.ID);
				cfpMsg.setContent(""+askingPrice);
				cfpMsg.setReplyWith("cfp"+System.currentTimeMillis());		// Unique value
				
				for (int i = 0; i < bidders.length; ++i) cfpMsg.addReceiver(bidders[i].getName());
				
				System.out.println(AGENTTYPE + " " + getAID().getName() + " will now cfp");
				send(cfpMsg);
				
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId(""+auctionItem.ID),
										MessageTemplate.MatchInReplyTo(cfpMsg.getReplyWith()));
				isAccepted = false;
				rejectProposalMsg.clearAllReceiver();
				receivedProposals = 0;
				step++;
				break;
			case 2:		// receive proposals
				ACLMessage proposalMsg = myAgent.receive(mt);
				if (proposalMsg != null)
				{
					receivedProposals++;
					switch (proposalMsg.getPerformative())
					{
					case ACLMessage.PROPOSE:
						if (Integer.parseInt(proposalMsg.getContent()) == askingPrice && !isAccepted)	// winner
						{
							System.out.println(AGENTTYPE + " " + getAID().getName() + " got buyer for " +
												auctionItem.name + "; " + proposalMsg.getSender().getName() +
												" bids " + askingPrice);
							isAccepted = true;
							acceptProposalMsg.addReceiver(proposalMsg.getSender());
						}
						else {
							rejectProposalMsg.addReceiver(proposalMsg.getSender());
						}
						break;
					case ACLMessage.NOT_UNDERSTOOD:
						System.out.println(AGENTTYPE + " " + getAID().getName() + " got NOT_UNDERSTOOD from " +
											proposalMsg.getSender().getName());
						// TODO: remove bidder from list
						break;
					}
					
					if (receivedProposals >= bidders.length)
					{
						rejectProposalMsg.setConversationId(""+auctionItem.ID);
						send(rejectProposalMsg);
						
						if (isAccepted)
						{
							acceptProposalMsg.setConversationId(""+auctionItem.ID);
							send(acceptProposalMsg);
							step++;							// Move to next step
						}
						else {
							askingPrice -= 10;
							// TODO: add/check lowest bound
						}
					}
				}
				break;
			case 3:		// End of auction
				System.out.println(AGENTTYPE + " " + getAID().getName() + " will now close auction");
				ACLMessage eoaMsg = new ACLMessage(ACLMessage.CANCEL);
				eoaMsg.setConversationId(""+auctionItem.ID);
				
				for (int i = 0; i < bidders.length; ++i) eoaMsg.addReceiver(bidders[i].getName());
				send(eoaMsg);
				step = END;
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
