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

        public ServeTourAgentRequest(Agent Curator, MessageTemplate templateForTourAgent, Map<Long,Artifact> artifactCatalogue) {
            super(Curator, templateForTourAgent, (DataStore) artifactCatalogue);
        }

        @Override
        protected  ACLMessage prepareResultNotification
                (ACLMessage request, ACLMessage response) throws FailureException {

            System.out.println("Hi Welcome !! I am serving Tour Agent at the moment with virtual tour");
            MessageTemplate templateForTourAgent = MessageTemplate.MatchContent("ARTIFACT_LIST_FOR_TOUR");
            ACLMessage replyToTourAgent = myAgent.receive(templateForTourAgent);
            try {
                MessageTemplate templateForProfiler = MessageTemplate.MatchContent("DETAIL_ARTIFACT_REQUIRED");


                if(replyToTourAgent != null){
                    replyToTourAgent.addReceiver(getAID());
                    replyToTourAgent.setLanguage("English");
                    replyToTourAgent.setOntology("Replying Profiler");
                    replyToTourAgent.setPerformative(ACLMessage.INFORM);
                    ArrayList<Long> myArtifactID = (ArrayList<Long>) replyToTourAgent.getContentObject();
                    String result = getArtifactList(myArtifactID);
                    replyToTourAgent.setContent(result);
                    send(replyToTourAgent);
                }
                else{
                    block();
                }
            }catch(UnreadableException ue)
            {
                ue.printStackTrace();
            } // end of catch block
            return replyToTourAgent;

        }// end of prepareNotifactionResult method

        //Retreive data from Artifact Class
        private String getArtifactList(ArrayList<Long> myArtifactID) {
            String resultList = new String();
            for(Long i : myArtifactID){
                Artifact myArtifact = artifactCatalogue.get(i);
            }
            return resultList;
        }//end of data fetch method
    } // ServeTour Agent Ends

    //Service to profiler Agent begins
    private class ServeProfilerAgent extends CyclicBehaviour {
        @Override
        public void action() {

            System.out.println("Hi Welcome !! I am serving Profiler Agent at the moment with detailed Artifact catalogue");
            try {
                MessageTemplate templateForProfiler = MessageTemplate.MatchContent("DETAIL_ARTIFACT_REQUIRED");
                ACLMessage replyToProfilerAgent = myAgent.receive(templateForProfiler);

                if(replyToProfilerAgent != null){
                replyToProfilerAgent.addReceiver(getAID());
                replyToProfilerAgent.setLanguage("English");
                replyToProfilerAgent.setOntology("Replying Profiler");
                replyToProfilerAgent.setPerformative(ACLMessage.INFORM);
                ArrayList<Long> myArtifactID = (ArrayList<Long>) replyToProfilerAgent.getContentObject();
                String result = getFullArtifactDetail(myArtifactID);
                replyToProfilerAgent.setContent(result);
                send(replyToProfilerAgent);
                }
                else{
                    block();
                }
                }catch(UnreadableException ue)
                {
                 ue.printStackTrace();
                }

        }// end of action for serving Profiler
        private String getFullArtifactDetail (ArrayList < Long > myArtifactID)
        {
            String fullCatalogueDetail = new String();
            for(Long i : myArtifactID){
                Artifact myArtifact = artifactCatalogue.get(i);
            }
            return fullCatalogueDetail;
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
