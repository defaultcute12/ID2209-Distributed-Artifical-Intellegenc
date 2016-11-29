import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;


public class CuratorAgent extends Agent
{
	private int ID = 0;
	private boolean isClone = false;
	
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
	}

}
