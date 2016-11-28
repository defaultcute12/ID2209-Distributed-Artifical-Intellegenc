import java.io.IOException;
import java.util.Scanner;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


@SuppressWarnings("serial")
public class QueenAgent extends Agent
{
	public static final int NUM_QUEENS = 4;
	
	@Override
	protected void setup()
	{
		System.out.println(getAID().getLocalName() + " is alive");
		
		addBehaviour(new stepBehaviour());
	}
	
	private class stepBehaviour extends Behaviour
	{
		AID predecessorAID;
		AID successorAID;
		int ID;
		
		GameState gs;
		Position previousPlacedPos;
		
		int step = 0;
		final int END = 10;

		@Override
		public void action()
		{
			switch (step)
			{
			case 0:			// get predecessor & successor
				String name = getAID().getLocalName();
				ID = Character.getNumericValue(name.charAt(name.length()-1));
				
				if (ID == 0)
				{
					predecessorAID = null;
					gs = new GameState(NUM_QUEENS);
					step++;
				}
				else		 predecessorAID = new AID("Q"+(ID-1), AID.ISLOCALNAME);
				
				if (ID == NUM_QUEENS-1) successorAID = null;
				else					successorAID = new AID("Q"+(ID+1), AID.ISLOCALNAME);
				
				step++;
				break;
			case 1:			// wait for predecessor's message
				ACLMessage msg = blockingReceive(MessageTemplate.MatchSender(predecessorAID));
				if (msg != null)
				{
					System.out.println(getAID().getLocalName() + " got msg from predecessor");
					try {
						gs = (GameState)msg.getContentObject();
					} catch (UnreadableException e) {
						System.err.println(getAID().getLocalName() + " failed to cast received item to GameState");
					}
				}
				step++;
				break;
			case 2:			// put queen
				boolean isPlaced = false;
				
				int colStart = 0;
				if (previousPlacedPos != null) colStart = previousPlacedPos.col + 1;
				
				for (int col = colStart; col < NUM_QUEENS; col++)
				{
					if (!gs.isThreatened(ID, col))
					{
						System.out.println(getAID().getLocalName() + " sets queen to (" + ID + ", " + col + ")");
						gs.putQueen(ID, col);
						isPlaced = true;
						break;
					}
				}
				
				if (!isPlaced)			// contact predecessor, GOTO: step 1
				{
					if (predecessorAID == null)
					{
						System.err.println(getAID().getLocalName() + " has no predecessor and cannot place queen");
						doDelete();
					}
					ACLMessage sendMsg = new ACLMessage(ACLMessage.REQUEST);
					sendMsg.addReceiver(predecessorAID);
					send(sendMsg);
					previousPlacedPos = null;	// restart placing queen from leftmost column
					step = 1;					// GOTO: wait for predecessor's message
					break;
				}
				
				step++;
				break;
			case 3:			// send GameState to successor
				if (successorAID != null)
				{
					ACLMessage sendMsg = new ACLMessage(ACLMessage.INFORM);
					sendMsg.addReceiver(successorAID);
					try {
						sendMsg.setContentObject(gs);
						send(sendMsg);
					} catch (IOException e) {
						System.err.println(getAID().getLocalName() + " failed to set GameState as msg content");
					}
				}
				else {		// last queen; print board and wait for possible request to find new solution
					System.out.println(getAID().getLocalName() + " has no successor to send the current GameState to");
					printQueens(gs);
					
					Scanner sc = new Scanner(System.in);
					sc.nextLine();
					step = 2;						// put Queen again; this should "fail" and ask prev. to move
					break;
				}
				step++;
				break;
			case 4:			// Wait for msg from successor asking to move queen
				ACLMessage successorMsg = blockingReceive(MessageTemplate.and(MessageTemplate.MatchSender(successorAID),
																	MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
				if (successorMsg != null)
				{
					System.out.println(getAID().getLocalName() + " got msg from successor");
					previousPlacedPos = gs.removeQueen(ID);
					step = 2;									// Goto placing queen;
				}
				else {
					System.err.println(getAID().getLocalName() + " got empty msg");
					doDelete();
				}
				break;
			}
		}

		@Override
		public boolean done()
		{
			return step == END;
		}
		
	}
	
	private void printQueens(GameState gs)
	{
		for (int i = 0; i < NUM_QUEENS; i++)
		{
			System.out.println("(" + i + ", " + gs.getQueenPosition(i).col + ")");
		}
	}
}



/*

	Get its predecessor
	wait for message from predecessor
	set its queen
		if not possible:	contact predecessor & ask to move queen & GOTO "wait for message"
		else:
	Get its successor
		if successor:	contact successor with GameState
		if not:			print
						if "get different solution" -> GOTO "set it's queen (+1)"
	wait for "moveQueen"

 */







