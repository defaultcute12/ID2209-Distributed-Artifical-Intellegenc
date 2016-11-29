import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;


public class ArtistManagerAgent extends Agent
{
	private Object[] args;
	private Location auctionLocation;
	private int ID = 0;
	private boolean isClone = false;
	
	@Override
	protected void setup()
	{
		args = getArguments();

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
					// Make sure it is only performed by the initial auctioneer
					if (!isClone) doClone(here(), (getAID().getLocalName() + "-" + index));
				}
			});
		}
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
			}
		});
	}
	
	@Override
	protected void beforeClone()
	{
		System.out.println(getAID().getLocalName() + " cloning at " + here().getName());
	}
	
	@Override
	protected void afterClone()
	{
		// Only performed by copies
		System.out.println("Clone " + getAID().getLocalName() + " has been created");
		ID = Character.getNumericValue(getAID().getLocalName().charAt(getAID().getLocalName().length()-1));
		isClone = true;
		
		moveToContainer();
	}
	
	
}
