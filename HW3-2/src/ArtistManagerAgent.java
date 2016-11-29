import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;


public class ArtistManagerAgent extends Agent
{
	private Object[] args;
	
	@Override
	protected void setup()
	{
		args = getArguments();

		System.out.println(getAID().getLocalName() + " is alive at " + here().getName());
		
		cloneHandling();
	}
	
	private void cloneHandling()
	{		
		SequentialBehaviour sb = new SequentialBehaviour();
		
		for (int i = 0; i < 2; i++)
		{
			final int index = i;
			
			sb.addSubBehaviour(new OneShotBehaviour()
			{
				@Override
				public void action()
				{
					if (getAID().getLocalName().equals("Auctioneer"))
						doClone(here(), (getAID().getLocalName() + "-" + index));
				}
			});
		}
		addBehaviour(sb);
	}
	
	// only performed by copies
	private void moveHandling()
	{
		final int ID = Character.getNumericValue(getAID().getLocalName().charAt(getAID().getLocalName().length()-1));
		System.out.println(getAID().getLocalName() + " has ID " + ID);
		
		addBehaviour(new OneShotBehaviour()
		{
			@Override
			public void action()
			{
				Location destination = (Location) args[ID+2];
				System.out.println(getAID().getLocalName() + " is a copy, will now move to " + destination.getName());
				doMove(destination);
			}
		});
	}
	
	@Override
	protected void beforeClone()
	{
		System.out.println(getAID().getLocalName() + " cloning locally at " + here().getName());
	}
	
	@Override
	protected void afterClone()
	{
		// Only performed by copies
		System.out.println(getAID().getLocalName() + " is now inside afterClone()");
		
		moveHandling();
	}
	
	
}
