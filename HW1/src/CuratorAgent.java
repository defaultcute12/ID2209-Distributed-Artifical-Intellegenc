import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SimpleAchieveREResponder;
import java.util.Hashtable;

/*
 * Curator Agent monitors the gallery/museum.
 * A gallery/museum contains detailed information of artifacts such as
 * id, name, creator, date of creation, place of creation, genre, etc.
 */
public class CuratorAgent extends Agent {
	
	public static final String AGENTTYPE = "Curator Agent";
	private Hashtable artifactCatalogue;
    private Hashtable artifactDetails;

	@Override
	protected void setup() {
        System.out.println("Hi am up " + AGENTTYPE + " " + getAID().getName());
        artifactCatalogue = new Hashtable();
        artifactDetails = new Hashtable();

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

        //update artifact catalogue in one shot for Tour agent
        sb.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
            System.out.println(artifactCatalogue);
            }
        });

        //update artifact catalogue in one shot for Profiler agent
        sb.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
              System.out.println("Deatils are : "+artifactCatalogue +artifactDetails);
            }
        });

        //Serve Tour Agent Request Parallels
        MessageTemplate tourAgentRequest = null;
        pb.addSubBehaviour(new ServeTourAgentRequest());

        //Serve Profiler agent request Parallels
        pb.addSubBehaviour(new ServeProfilerAgent());

        addBehaviour(sb);
        addBehaviour(pb);

    }//end of set up

    //update artifact details
    public Hashtable detailedList(String creationPlace, int date) {
        artifactDetails.put(creationPlace,date);
        detailedList("egypt",1991-01-11);
        detailedList("india",1990-01-11);
        detailedList("stockholm",1995-01-11);
        return artifactDetails;
    }


    //update artifact catalogue list

    public Hashtable  listCatalogue(final String artifactName, final int artifactId) {
        artifactCatalogue.put(artifactName, artifactId);
        listCatalogue("Flowers",1);
        listCatalogue("Nature",2);
        listCatalogue("Mummy",3);
        return artifactCatalogue;
    }

    //Service to Tour Agent begins
    private class ServeTourAgentRequest extends CyclicBehaviour {

        @Override
        public void action() {

        }
    } // ServeTour Agent Ends

    //Service to profiler Agent begins
    private class ServeProfilerAgent extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate tmpProfilerAgent = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage acm = myAgent.receive(tmpProfilerAgent);
            if (acm != null)
            {
                System.out.println("I got message from TourAgent.. Processing ....");
            }
            else{
                System.out.println("Got no message");
                block();
            }
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
