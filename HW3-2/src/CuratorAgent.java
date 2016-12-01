import java.util.Random;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class CuratorAgent extends Agent
{
	public static final String AGENTTYPE = "CuratorAgent";
	public static final String SERVICENAME = "CuratorBidder";
	
	private int ID = 0;
	private boolean isClone = false;
	
	private AuctionItem item;
	private int maxPrice = 50;
	
	@Override
	protected void setup()
	{
		System.out.println(getAID().getLocalName() + " is alive at " + here().getName());
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
					// Make sure it is only performed by the initial curator
					if (!isClone) doClone(here(), (getAID().getLocalName() + "-" + index));
				}
			});
		}
		sb.addSubBehaviour(new stepBehaviour());
		addBehaviour(sb);
	}
	
	@Override
	protected void beforeClone()
	{
		System.out.println(getAID().getLocalName() + " cloning at " + here().getName());
	}
	
	@Override
	protected void afterClone()
	{
		// Only performed by copy
		System.out.println("Clone " + getAID().getLocalName() + " has been created");
		ID = Character.getNumericValue(getAID().getLocalName().charAt(getAID().getLocalName().length()-1));
		isClone = true;
		
		/*
		Random rnd = new Random();
		maxPrice = rnd.nextInt(100);
		*/
	}
	
	
	
	// **************
	
	private ACLMessage getProposal(ACLMessage cfp)
	{
		ACLMessage proposal = cfp.createReply();
		proposal.setPerformative(ACLMessage.PROPOSE);
		int biddingPrice = Integer.parseInt(cfp.getContent());
		
		if (biddingPrice <= maxPrice) proposal.setContent(""+biddingPrice);
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
			case 0:		// get inform of new auction
				System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " waiting for notification of new auction");
				ACLMessage newAuctionMsg = myAgent.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				
				if (newAuctionMsg != null)
				{
					System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got notification of new auction");
					try {
						item = (AuctionItem)newAuctionMsg.getContentObject();
					}
					catch (UnreadableException e)
					{
						System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " failed casting received msg");
						step = END;
					}
				}
				else {
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " got empty msg");
				}
				step++;
				break;
			case 1:		// call for proposals or auction termination
				ACLMessage cfpMsg = myAgent.blockingReceive();
				if (cfpMsg != null)
				{
					switch (cfpMsg.getPerformative())
					{
					case ACLMessage.CFP:
						System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got cfp");
						ACLMessage proposalMsg = getProposal(cfpMsg);
						if (Integer.parseInt(proposalMsg.getContent()) > 0)
							System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " will propose " + 
											Integer.parseInt(proposalMsg.getContent()));
						send(proposalMsg);
						step++;
						break;
					case ACLMessage.CANCEL:
						System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got notification of auction end");
						item = null;
						step = 0;		// Go back waiting for new auction to begin
						break;
					}
				}
				else {
					System.err.println(AGENTTYPE + " " + getAID().getLocalName() + " got empty msg (2)");
				}
				break;
			case 2:		// status of sent proposal
				//System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " waiting for proposal reply");
				ACLMessage statusMsg = myAgent.blockingReceive();
				if (statusMsg != null)
				{
					if (statusMsg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
					{
						System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " won auction of " + item.name + "!");
					}
					else if (statusMsg.getPerformative() == ACLMessage.REJECT_PROPOSAL)
					{
						//System.out.println(AGENTTYPE + " " + getAID().getLocalName() + " got proposal reply");
					}
				}
				step--;
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
