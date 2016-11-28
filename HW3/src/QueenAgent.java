import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;


@SuppressWarnings("serial")
public class QueenAgent extends Agent
{
	@Override
	protected void setup()
	{
		System.out.println(getAID().getLocalName() + " is alive");
		
		addBehaviour(new CyclicBehaviour() {

			@Override
			public void action()
			{
				if (getAID().getLocalName().equals("Q1"))
				{
					ACLMessage testMsg = new ACLMessage(ACLMessage.PROPOSE);
					AID idQ0 = new AID("Q0", AID.ISLOCALNAME);
					
					testMsg.addReceiver(idQ0);
					
					System.out.println("Q1 will now send msg");
					send(testMsg);
				}
				else { // Q0
					ACLMessage msg = blockingReceive();
					System.out.println("Q0 got msg from " + msg.getSender().getLocalName());
				}
			}
		});
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







