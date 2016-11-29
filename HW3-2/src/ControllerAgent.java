
import java.util.*;
import java.io.*;

import jade.lang.acl.*;
import jade.content.*;
import jade.content.onto.basic.*;
import jade.content.lang.*;
import jade.content.lang.sl.*;
import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.mobility.*;
import jade.domain.JADEAgentManagement.*;
import jade.gui.*;


public class ControllerAgent extends GuiAgent {
// --------------------------------------------

   private jade.wrapper.AgentContainer home;
   private jade.wrapper.AgentContainer HM;
   private jade.wrapper.AgentContainer G;

   private Map locations = new HashMap();
   private Vector agents = new Vector();
   private int agentCnt = 0;
   private int command;
   transient protected ControllerAgentGui myGui;

   public static final int QUIT = 0;
   public static final int NEW_BIDDER = 1;
   public static final int NEW_AUCTIONEER = 2;
   public static final int MOVE_AGENT = 3;
   public static final int CLONE_AGENT = 4;
   public static final int KILL_AGENT = 5;

   // Get a JADE Runtime instance
   jade.core.Runtime runtime = jade.core.Runtime.instance();

   protected void setup() {
// ------------------------

	  // Register language and ontology
	  getContentManager().registerLanguage(new SLCodec());
	  getContentManager().registerOntology(MobilityOntology.getInstance());

      try {
          home = runtime.createAgentContainer(new ProfileImpl());

         // Create the container objects
         HM = runtime.createAgentContainer(new ProfileImpl());
         
         String agentName = "HM-Bidder";
         jade.wrapper.AgentController agent = HM.createNewAgent(agentName, CuratorAgent.class.getName(), null);
         agent.start();
         agents.add(agentName);
         
         G = runtime.createAgentContainer(new ProfileImpl());
         
         agentName = "G-Bidder";
         agent = G.createNewAgent(agentName, CuratorAgent.class.getName(), null);
         agent.start();
         agents.add(agentName);
         
         // ----------------------
        
         //TODO: myGui.updateList(agents);
	     doWait(2000);

	     // Get available locations with AMS
	     sendRequest(new Action(getAMS(), new QueryPlatformLocationsAction()));

	     //Receive response from AMS
         MessageTemplate mt = MessageTemplate.and(
			                  MessageTemplate.MatchSender(getAMS()),
			                  MessageTemplate.MatchPerformative(ACLMessage.INFORM));
         ACLMessage resp = blockingReceive(mt);
         ContentElement ce = getContentManager().extractContent(resp);
         Result result = (Result) ce;
         jade.util.leap.Iterator it = result.getItems().iterator();
         while (it.hasNext())
         {
            Location loc = (Location)it.next();
            locations.put(loc.getName(), loc);
		 }
         
         agentName = "Auctioneer";
         agent = home.createNewAgent(agentName, ArtistManagerAgent.class.getName(), locations.values().toArray());
         agent.start();
         agents.add(agentName);
         
	  }
	  catch (Exception e) { e.printStackTrace(); }


	  // Create and show the gui
      myGui = new ControllerAgentGui(this, locations.keySet());
      myGui.setVisible(true);
   }


   protected void onGuiEvent(GuiEvent ev) {
// ----------------------------------------

	  command = ev.getType();

	  if (command == QUIT) {
	     try {
		    home.kill();
		    HM.kill();
		    G.kill();
	     }
	     catch (Exception e) { e.printStackTrace(); }
	     myGui.setVisible(false);
	     myGui.dispose();
		 doDelete();
		 System.exit(0);
      }
	  if (command == NEW_BIDDER) {

	     jade.wrapper.AgentController a = null;
         try {
            Object[] args = new Object[2];
            args[0] = getAID();
            String name = "Agent" + (agentCnt++) + "-Bidder";
            a = home.createNewAgent(name, CuratorAgent.class.getName(), args);
	        a.start();
	        agents.add(name);
	        myGui.updateList(agents);
	     }
         catch (Exception ex) {
		    System.out.println("Problem creating new agent");
	     }
         return;
	  }
	  if (command == NEW_AUCTIONEER) {

		     jade.wrapper.AgentController a = null;
	         try {
	            Object[] args = new Object[2];
	            args[0] = getAID();
	            String name = "Agent" + (agentCnt++) + "-Auctioneer";
	            a = home.createNewAgent(name, MobileAgent.class.getName(), args);
		        a.start();
		        agents.add(name);
		        myGui.updateList(agents);
		     }
	         catch (Exception ex) {
			    System.out.println("Problem creating new agent");
		     }
	         return;
		  }
      String agentName = (String)ev.getParameter(0);
      AID aid = new AID(agentName, AID.ISLOCALNAME);

	  if (command == MOVE_AGENT) {

         String destName = (String)ev.getParameter(1);
         Location dest = (Location)locations.get(destName);
         MobileAgentDescription mad = new MobileAgentDescription();
         mad.setName(aid);
         mad.setDestination(dest);
         MoveAction ma = new MoveAction();
         ma.setMobileAgentDescription(mad);
         sendRequest(new Action(aid, ma));
	  }
      else if (command == CLONE_AGENT) {

         String destName = (String)ev.getParameter(1);
         Location dest = (Location)locations.get(destName);
         MobileAgentDescription mad = new MobileAgentDescription();
         mad.setName(aid);
         mad.setDestination(dest);
         String newName = "Clone-"+agentName;
         CloneAction ca = new CloneAction();
         ca.setNewName(newName);
         ca.setMobileAgentDescription(mad);
         sendRequest(new Action(aid, ca));
         
         agents.add(newName);
         myGui.updateList(agents);
	  }
      else if (command == KILL_AGENT) {

         KillAgent ka = new KillAgent();
         ka.setAgent(aid);
         sendRequest(new Action(aid, ka));
	     agents.remove(agentName);
		 myGui.updateList(agents);
	  }
   }


   void sendRequest(Action action) {
// ---------------------------------

      ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
      request.setLanguage(new SLCodec().getName());
      request.setOntology(MobilityOntology.getInstance().getName());
      try {
	     getContentManager().fillContent(request, action);
	     request.addReceiver(action.getActor());
	     send(request);
	  }
	  catch (Exception ex) { ex.printStackTrace(); }
   }

}//class Controller
