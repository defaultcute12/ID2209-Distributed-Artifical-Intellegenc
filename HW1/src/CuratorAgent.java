import com.sun.xml.internal.bind.v2.model.core.ID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREResponder;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
 * Curator Agent monitors the gallery/museum.
 * A gallery/museum contains detailed information of artifacts such as
 * id, name, creator, date of creation, place of creation, genre, etc.
 */

public class CuratorAgent extends Agent {
	
	public static final String AGENTTYPE = "Curator Agent";
	private HashMap<Long,Artifact> artifactCatalogue;


	@Override
	protected void setup() {
        //Curator Agent Started
        System.out.println("Hi am up " + AGENTTYPE + " " + getAID().getName());

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
                //initializing artifact list
                artifactCatalogue = new HashMap<>();
                for(int j=0;j<5;j++)
                {
                    Artifact detailedArtList = new Artifact(j,"FamousFloral","1992/01/12","Delhi","Flowers");
                    artifactCatalogue.put((long) detailedArtList.getID(),detailedArtList);
                }
                System.out.println(artifactCatalogue);
            }
        });

        //Templates for messages coming from Profiler Agent and Curator Agent
        MessageTemplate templateForProfiler = MessageTemplate.MatchContent("DETAIL_ARTIFACT_REQUIRED");
        MessageTemplate templateForTourAgent = MessageTemplate.MatchContent("ARTIFACT_LIST_FOR_TOUR");

        //Serve Tour Agent Request Parallels
        pb.addSubBehaviour(new ServeTourAgentRequest(this,templateForTourAgent,artifactCatalogue));

        //Serve Profiler agent request Parallels
        pb.addSubBehaviour(new ServeProfilerAgent());

        //update of artifacts and serving of requests for Tour Agent and profiler Agent goes sequentially
        sb.addSubBehaviour(pb);

        //adding both behaviours to agent
        addBehaviour(sb);
        addBehaviour(pb);

    }//end of set up


    //Service to Tour Agent begins
    private class ServeTourAgentRequest extends SimpleAchieveREResponder{

        public ServeTourAgentRequest(Agent a, MessageTemplate mt, Map<Long,Artifact> artifactCatalogue) {
            super(a, mt, (DataStore) artifactCatalogue);
        }

        @Override
        protected  ACLMessage prepareResultNotification
                (ACLMessage request, ACLMessage response) throws FailureException {

            System.out.println("Hi Welcome !! I am serving Tour Agent at the moment with virtual tour");
            MessageTemplate templateForTourAgent = MessageTemplate.MatchContent("ARTIFACT_LIST_FOR_TOUR");
            ACLMessage replyToTourAgent;
            replyToTourAgent = null;
            try {
                ACLMessage receivedMsg = myAgent.receive(templateForTourAgent);
                receivedMsg.addReceiver(getAID());
                replyToTourAgent = receivedMsg.createReply();
                replyToTourAgent.setLanguage("English");
                replyToTourAgent.setPerformative(ACLMessage.INFORM);

                ArrayList<Long> myArtifactID = (ArrayList<Long>) replyToTourAgent.getContentObject();
                ArrayList<Artifact> myArtifactReturned = (ArrayList<Artifact>) getArtifactList(myArtifactID);

                replyToTourAgent.setContentObject(myArtifactReturned);

                System.out.println(replyToTourAgent);
            } catch (UnreadableException | IOException ue) {
                ue.printStackTrace();
            } // end of catch block
            return replyToTourAgent;
        }// end of prepareNotifactionResult method

        //Retreive data from Artifact Class
        private ArrayList<Artifact> getArtifactList(ArrayList<Long> myArtifactID) {
            ArrayList<Artifact> resultList = new ArrayList<>();
            for(Long i : myArtifactID){
                Artifact myArtifact = artifactCatalogue.get(i);
            }
            return resultList;
        }
    } // ServeTour Agent Ends

    //Service to profiler Agent begins
    private class ServeProfilerAgent extends CyclicBehaviour {
        @Override
        public void action() {
            System.out.println("Hi Welcome !! I am serving Profiler Agent at the moment with detailed Artifact catalogue");


        }// end of action for serving Profiler

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
