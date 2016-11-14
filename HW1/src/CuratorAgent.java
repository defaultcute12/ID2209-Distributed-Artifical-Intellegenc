import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLCodec;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREResponder;

import java.io.IOException;
import java.io.Serializable;
import java.security.acl.Acl;
import java.text.DateFormat;
import java.util.*;

/*
 * Curator Agent monitors the gallery/museum.
 * A gallery/museum contains detailed information of artifacts such as
 * id, name, creator, date of creation, place of creation, genre, etc.
 */

public class CuratorAgent extends Agent {
	
	public static final String REQUESTONE = "DETAIL_ARTIFACT_REQUIRED";
	public static final String REQUESTALL = "ARTIFACT_LIST_FOR_TOUR";
	public static final String SERVICETYPE = "InquiryService";
	public static final String SERVICENAME = "Curator";
	public static final String AGENTTYPE = "Curator Agent";
	private HashMap<Integer,Artifact> artifactCatalogue;


	@Override
	protected void setup() {
        //Curator Agent Started
        System.out.println("Hi am up " + AGENTTYPE + " " + getAID().getName());
        
        // ---------
		DFAgentDescription agentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();
		
		agentDescription.setName(getAID());						// set Agent name
		serviceDescription.setName(SERVICENAME);				// set Service name
		serviceDescription.setType(SERVICETYPE);				// set Service type
		
		agentDescription.addServices(serviceDescription);		// Add the Service to the Agent
		
		try {
			DFService.register(this, agentDescription);
		} catch (FIPAException e)
		{
			System.err.println(AGENTTYPE + " failed to register with DF");
		}
		
        //behaving to server Tour Agent and Profiler Agent
        SequentialBehaviour sb = new SequentialBehaviour();
        ParallelBehaviour pb = new ParallelBehaviour();

        //update artifact catalogue in one shot for Tour agent
        sb.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                //initializing artifact list
                artifactCatalogue = new HashMap<Integer,Artifact>();
                for(int j=0;j<5;j++)
                {
                    Artifact detailedArtList = new Artifact(j,"FamousFloral", new Date(),"Delhi","flowers");
                    artifactCatalogue.put(detailedArtList.ID,detailedArtList);
                }
                //System.out.println(artifactCatalogue);
            }
        });

        //Templates for messages coming from Profiler Agent and Curator Agent
        MessageTemplate templateForProfiler = MessageTemplate.MatchContent(REQUESTONE);
        MessageTemplate templateForTourAgent = MessageTemplate.MatchContent(REQUESTALL);

        //Serve Tour Agent Request Parallels
        pb.addSubBehaviour(new ServeTourAgentRequest(this, templateForTourAgent));

        //Serve Profiler agent request Parallels
        pb.addSubBehaviour(new ServeProfilerAgent());

        //update of artifacts and serving of requests for Tour Agent and profiler Agent goes sequentially
        sb.addSubBehaviour(pb);

        //adding both behaviours to agent
        addBehaviour(sb);
        //addBehaviour(pb);

    }//end of set up


    //Service to Tour Agent begins
    private class ServeTourAgentRequest extends SimpleAchieveREResponder{

        public ServeTourAgentRequest(Agent Curator, MessageTemplate templateForTourAgent) {
            super(Curator, templateForTourAgent);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request)
        {
            System.out.println("Hi Welcome !! I am serving Tour Agent at the moment with virtual tour");
            ACLMessage reply = null;

                if(request != null)
                {
                	System.out.println("Curator received a non-null msg from TourGuide");
                    reply = request.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    List<Artifact> list = new ArrayList<Artifact>(artifactCatalogue.values());
                    try {
                        reply.setContentObject((Serializable )list);
                        send(reply);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Curator failed to cast");
                    }
                }

                else{
                    block();
                }
            return reply;
        }

        //Retreive data from Artifact Class
        //if i have finished the behaviour am done
        @Override
        public boolean done()
        {
            return false;
        }
    } // ServeTour Agent Ends

    //Service to profiler Agent begins
    private class ServeProfilerAgent extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate templateForProfiler = MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF);
            ACLMessage request = myAgent.receive(templateForProfiler);

            System.out.println("Hi Welcome !! Curator serving Profiler Agent at the moment with detailed Artifact catalogue");
            try {
                if(request != null)
                {
                	System.out.println(AGENTTYPE + " got non-null request");
                	ACLMessage reply =  request.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    Integer artifactID = (Integer)request.getContentObject();
                    System.out.println(AGENTTYPE + " got int " + artifactID);
                    Artifact replyArtifact = artifactCatalogue.get(artifactID);
                    if(replyArtifact == null)
                    {
                    	System.err.println(AGENTTYPE + " could not find that artifact");
                        reply.setPerformative(ACLMessage.FAILURE);
                    }
                    else{
                        reply.setContentObject(replyArtifact);
                    }
                    send(reply);
                }
                else{
                    block();
                }
                }catch(UnreadableException | IOException ue)
                {
                 ue.printStackTrace();
                }

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
