import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.Hashtable;

/*
 * Curator Agent monitors the gallery/museum.
 * A gallery/museum contains detailed information of artifacts such as
 * id, name, creator, date of creation, place of creation, genre, etc.
 */
public class CuratorAgent extends Agent {
	
	public static final String AGENTTYPE = "Curator Agent";
	private Hashtable artifactCatalogue;

	@Override
	protected void setup() {
        System.out.println("Hi am up " + AGENTTYPE + " " + getAID().getName());
        artifactCatalogue = new Hashtable();

        //Register my service
        DFAgentDescription dfad = new DFAgentDescription();
        dfad.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ARTIFACT-INFORMATION");
        sd.setName("MUSEUM-ARTIFACT-CATALOGUE");
        dfad.addServices(sd);
        try{
            DFService.register(this,dfad);
        } catch(FIPAException fe){
            fe.printStackTrace();
        }


        //  respond to tour agent for build tour-----
           addBehaviour(new ServeTourAgentRequest());

        //respond to profiler giving detailed information about artifacts-----
           addBehaviour(new ServeProfilerAgent());
    }


        //update artifact catalogue list
    public void updateArtifactCatalogue(final String artifactName, final int artifactId) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                artifactCatalogue.put(artifactName, artifactId);
            }
        });
    }


    //Complete tour agent service
    private class ServeTourAgentRequest extends ParallelBehaviour {

    }
    //Complete profiler agent service
    private class ServeProfilerAgent extends Behaviour {
        @Override
        public void action() {
        }
        @Override
        public boolean done() {
            return false;
        }
    }

    //agent terminating
    protected void takeDown()
    {
        System.out.println("CuratorAgent Terminating"+getAID().getName());
    }
}
