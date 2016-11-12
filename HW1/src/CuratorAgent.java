import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
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
        SequentialBehaviour sb = new SequentialBehaviour();
        ParallelBehaviour pb = new ParallelBehaviour();

        //update artifact catalogue in one shot
        sb.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                updateArtifactCatalogue("Flowers",1);
                updateArtifactCatalogue("Nature",2);
                updateArtifactCatalogue("Mummy",3);
                System.out.println(artifactCatalogue);
            }
        });
        //Serve Tour Agent Request
        pb.addSubBehaviour(new ServeTourAgentRequest());

        //Serve Profiler agent request
        pb.addSubBehaviour(new ServeProfilerAgent());

        addBehaviour(sb);
        addBehaviour(pb);

    }//end of set up



    //update artifact catalogue list
    public void updateArtifactCatalogue(final String artifactName, final int artifactId) {
        artifactCatalogue.put(artifactName, artifactId);
    }

    //Service to Tour Agent begins
    private class ServeTourAgentRequest extends Behaviour {
        @Override
        public void action() {

        }

        @Override
        public boolean done() {
            return false;
        }
    } // ServeTour Agent Ends

    //Service to profiler Agent begins
    private class ServeProfilerAgent extends Behaviour {
        @Override
        public void action() {

        }

        @Override
        public boolean done() {
            return false;
        }
    }// Serve Profiler Agent ends

    //agent terminating
    protected void takeDown()
    {   //deregister yellow pages
        try {
            DFService.deregister(this);
        }catch (FIPAException fe){
            fe.printStackTrace();
        }
        System.out.println("CuratorAgent Terminating"+getAID().getName());
    }

}//end of Main curator agent class
