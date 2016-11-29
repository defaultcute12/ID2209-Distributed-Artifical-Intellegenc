import java.io.IOException;

import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.domain.JADEAgentManagement.QueryAgentsOnLocation;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.Iterator;
import jade.util.leap.List;
import jade.content.lang.sl.SLCodec;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.FIPANames.ContentLanguage;
import jade.domain.FIPANames.InteractionProtocol;
import jade.domain.JADEAgentManagement.JADEManagementOntology;




public class ArtistManagerAgent extends Agent
{
	public static final String AGENTTYPE = "ArtistManagerAgent";
	private AuctionItem auctionItem;
	private int askingPrice;
	private int lowestPrice;

	private Object[] args;
	private Location auctionLocation;
	private Location homeLocation;
	private int ID = 0;
	private boolean isClone = false;
	
	@Override
	protected void setup()
	{
		args = getArguments();

		System.out.println(getAID().getLocalName() + " is alive at " + here().getName());
				
        auctionItem = new AuctionItem(1, "Mona Lisa", AuctionItem.PAINTING);
        askingPrice = 100;
        lowestPrice = 50;
        
        System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will auction \"" +
        					auctionItem.name + "\" starting at " + askingPrice + " (lowest " + lowestPrice + ")");
		
		cloning();
	}
	
	private void cloning()
	{		
		SequentialBehaviour sb = new SequentialBehaviour();
		
		for (int i = 0; i < 2; i++)
		{
			final int index = i+1;
			
			sb.addSubBehaviour(new OneShotBehaviour()
			{
				@Override
				public void action()
				{
					// Make sure it is only performed by the initial auctioneer
					if (!isClone) doClone(here(), (getAID().getLocalName() + "-" + index));
				}
			});
		}
		
		if (!isClone) sb.addSubBehaviour(new mainAuctioneerBehaviour());
		
		addBehaviour(sb);
	}
	
	// only performed by copies
	private void moveToContainer()
	{		
		addBehaviour(new OneShotBehaviour()
		{
			@Override
			public void action()
			{
				auctionLocation = (Location) args[ID+1];
				System.out.println(getAID().getLocalName() + " will now move to " + auctionLocation.getName());
				doMove(auctionLocation);
				
				addBehaviour(new stepBehaviour());
			}
		});
	}
	
	@Override
	protected void beforeClone()
	{
		// Only performed by non-copy
		System.out.println(getAID().getLocalName() + " cloning at " + here().getName());
	}
	
	@Override
	protected void afterClone()
	{
		// Only performed by copy
		System.out.println("Clone " + getAID().getLocalName() + " has been created");
		ID = Character.getNumericValue(getAID().getLocalName().charAt(getAID().getLocalName().length()-1));
		isClone = true;
		
		homeLocation = here();		// remember so we know where to return
		moveToContainer();
	}
	
	// *************************
	
	private class mainAuctioneerBehaviour extends OneShotBehaviour
	{
		@Override
		public void action()
		{
			int receivedProposals = 0;
			AID bestAuctioneer = null;
			int maxBid = 0;
			
			if (isClone) return;
			
			System.out.println(getAID().getLocalName() + " waiting for auctioneer clones to return");
			while (true)				// wait for auctioneers to return back from their containers
			{
				ACLMessage auctionFinishedMsg = myAgent.blockingReceive();
				if (auctionFinishedMsg != null)
				{
					receivedProposals++;
					int bid = Integer.parseInt(auctionFinishedMsg.getContent());
					
					if (bid > maxBid)
					{
						bestAuctioneer = auctionFinishedMsg.getSender();
						maxBid = bid;
					}
				}
				if (receivedProposals >= 2) break;
			}
			if (bestAuctioneer == null)
			{
				System.err.println("Main Auctioneer failed to find a best auctioneer?");
				myAgent.doDelete();
			}
			
			System.out.println(getAID().getLocalName() + " has now got all auctioneer clones checked in");

			ACLMessage winnerMsg = new ACLMessage(ACLMessage.CONFIRM);
			winnerMsg.addReceiver(bestAuctioneer);
			send(winnerMsg);
		}
	}
	
	
	// *************************
	
	private class stepBehaviour extends Behaviour
	{
		final static int END = 10;
		int step = 0;
		AID[] bidders;
		ACLMessage rejectProposalMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
		ACLMessage acceptProposalMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
		boolean isAccepted = false;
		int receivedProposals;
		MessageTemplate mt;
		
		AID mainAuctioneerAID = new AID("Auctioneer", AID.ISLOCALNAME);

		@Override
		public void action()
		{
			switch (step)
			{
			case 0:		// fetch bidders from AMS & inform of auction start
				ACLMessage query = prepareRequestToAMS(here());
				send(query);
				
				ACLMessage response = blockingReceive(MessageTemplate.MatchSender(getAMS()));
				List containerAIDs = parseAMSResponse(response);
				containerAIDs.remove(getAID());						// Remove Auctioneer from list of agents in container
				
				bidders = new AID[containerAIDs.size()];
				Iterator it = containerAIDs.iterator();				// Create iterator on returned List
				for(int i = 0; i < bidders.length; i++) bidders[i] = (AID)it.next();
				
				System.out.println(getAID().getLocalName() + " found " + bidders.length + " bidders");
				
				try
				{					
					ACLMessage auctionStartMsg = new ACLMessage(ACLMessage.INFORM);
					auctionStartMsg.setContentObject(auctionItem);
					
					// Add all Curators as receivers
					for (int i = 0; i < bidders.length; i++) auctionStartMsg.addReceiver(bidders[i]);
					
					System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will now inform start of auction");
					send(auctionStartMsg);
					step++;
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
				
				for (int i = 0; i < bidders.length; ++i) cfpMsg.addReceiver(bidders[i]);
				
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
							acceptProposalMsg.setConversationId(""+auctionItem.ID);
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
							if (isAccepted) System.out.println(AGENTTYPE + " " + getAID().getLocalName() +
																" will now send rejected msg");
							send(rejectProposalMsg);
						}
						
						if (isAccepted) step++;		// Move to next step
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
			case 3:		// Checkin at home container
				System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will now check in at home container");
				doMove(homeLocation);
				
				
				ACLMessage checkinMsg = new ACLMessage(ACLMessage.INFORM);
				checkinMsg.addReceiver(mainAuctioneerAID);
				checkinMsg.setContent(""+askingPrice);
				send(checkinMsg);
				step++;
				break;
			case 4:		// wait for main Auctioneer
				ACLMessage winningAuctionMsg = blockingReceive(MessageTemplate.MatchSender(mainAuctioneerAID));
				
				System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got some message???");
				
				if (winningAuctionMsg != null)
				{
					System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got highest bid; will inform winner");
					doMove(auctionLocation);
					
					System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will now send accepted msg");
					send(acceptProposalMsg);
					
					System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will now close auction");
					ACLMessage eoaMsg = new ACLMessage(ACLMessage.CANCEL);
					eoaMsg.setConversationId(""+auctionItem.ID);
					
					for (int i = 0; i < bidders.length; ++i) eoaMsg.addReceiver(bidders[i]);
					send(eoaMsg);
					step = END;
					break;
				}
				else {
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " got null msg");
					myAgent.doDelete();
				}
			}
		}

		@Override
		public boolean done()
		{
			return step == END;
		}
		
	}
	
	// http://jade.tilab.com/pipermail/jade-develop/2007q4/011369.html
	
	private ACLMessage prepareRequestToAMS(Location where)
	{
		// Prepare Content Manager (used to parse response)
		getContentManager().registerLanguage(new SLCodec(), ContentLanguage.FIPA_SL);
		getContentManager().registerOntology(JADEManagementOntology.getInstance());
		
		// Prepare message
		ACLMessage amsMsg = new ACLMessage(ACLMessage.REQUEST); 
		amsMsg.addReceiver(getAMS()); 
		amsMsg.setOntology(JADEManagementOntology.getInstance().getName());
		amsMsg.setLanguage(ContentLanguage.FIPA_SL);
		amsMsg.setProtocol(InteractionProtocol.FIPA_REQUEST);
		
		// Prepare query to be put in message
		QueryAgentsOnLocation queryAgents = new QueryAgentsOnLocation();
		queryAgents.setLocation(where);
		
		Action queryAction = new Action(getAMS(), queryAgents);

		try {
			getContentManager().fillContent(amsMsg, queryAction);
		} catch (Exception ignore) {
		}
		return amsMsg;
	}

	private List parseAMSResponse(ACLMessage response)
	{
		Result results = null;
		try {
			results = (Result) getContentManager().extractContent(response);
		} catch (Exception ignore) {
		}
		return results.getItems();

	}
	
	
}
