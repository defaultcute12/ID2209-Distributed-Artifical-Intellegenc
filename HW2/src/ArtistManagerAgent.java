
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
	
	public static final String AGENTTYPE = "ArtistManagerAgent";
	
	private AuctionItem auctionItem;
	private int askingPrice;
	private int lowestPrice;
	
	public ArtistManagerAgent()
	{
		System.out.println("EMPTY CONSTRUCTOR");
	}
	public ArtistManagerAgent(String k)
	{
		System.out.println("STRING CONSTRUCTOR");
	}
	@Override
	protected void setup()
	{
		Object[] args = getArguments();
		askingPrice = Integer.parseInt(args[0].toString());
		lowestPrice = Integer.parseInt(args[1].toString());
        
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) { }
		        
        auctionItem = new AuctionItem(1, "Mona Lisa", AuctionItem.PAINTING);
        askingPrice = 100;
        
        System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " created; will auction \"" +
        					auctionItem.name + "\" starting at " + askingPrice + " (lowest " + lowestPrice + ")");
        
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
					
					System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " found " + bidders.length + " curators");
					
					ACLMessage auctionStartMsg = new ACLMessage(ACLMessage.INFORM);
					auctionStartMsg.setContentObject(auctionItem);
					
					// Add all Curators as receivers
					for (int i = 0; i < bidders.length; i++) auctionStartMsg.addReceiver(bidders[i].getName());
					
					System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will now inform start of auction");
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
				
				System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will now cfp");
				send(cfpMsg);
				
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId(""+auctionItem.ID),
										MessageTemplate.MatchInReplyTo(cfpMsg.getReplyWith()));
				isAccepted = false;
				rejectProposalMsg.clearAllReceiver();
				receivedProposals = 0;
				step++;
				break;
			case 2:		// receive proposals
				ACLMessage proposalMsg = myAgent.blockingReceive(mt);
				if (proposalMsg != null)
				{
					receivedProposals++;
					switch (proposalMsg.getPerformative())
					{
					case ACLMessage.PROPOSE:
						if (Integer.parseInt(proposalMsg.getContent()) == askingPrice && !isAccepted)	// winner
						{
							System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got buyer for " +
												auctionItem.name + "; " + proposalMsg.getSender().getLocalName() +
												" bids " + askingPrice);
							isAccepted = true;
							acceptProposalMsg.addReceiver(proposalMsg.getSender());
						}
						else {
							rejectProposalMsg.addReceiver(proposalMsg.getSender());
						}
						break;
					case ACLMessage.NOT_UNDERSTOOD:
						System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got NOT_UNDERSTOOD from " +
											proposalMsg.getSender().getLocalName());
						// TODO: remove bidder from list
						break;
					}
					if (receivedProposals >= bidders.length)
					{
						System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " all bidders checked in");
						rejectProposalMsg.setConversationId(""+auctionItem.ID);
						
						if (rejectProposalMsg.getAllReceiver().hasNext())	// there are bidders who need rejection
						{
							if (isAccepted) System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will now send rejected msg");
							send(rejectProposalMsg);
						}
						
						if (isAccepted)
						{
							acceptProposalMsg.setConversationId(""+auctionItem.ID);
							System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will now send accepted msg");
							send(acceptProposalMsg);
							step++;							// Move to next step
						}
						else {
							askingPrice -= 10;
							
							if (askingPrice < lowestPrice)
							{
								System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got no good bids, " +
										"price is now lower than minimum accepted");
								step++;
							}
							else {
								System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got no good bids, " + 
										"lowered price by 10. New price: " + askingPrice);
								step--;
							}
						}
					}
				}
				break;
			case 3:		// End of auction
				System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will now close auction");
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
