package org.commitment_issues.delivery_agents;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.yourteamname.agents.BaseAgent;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class LoadingBayAgent extends BaseAgent {
	
	protected void setup() {
		System.out.println("Hello! LoadingBay-agent "+getAID().getName()+" is ready.");
		
		register("loading-bay", "loading-bay");	
		
		addBehaviour(new PackagingPhaseMessageSender());	
		addBehaviour(new TimeUpdater());
	}
	
	protected void takeDown() {
		deRegister();
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
	protected String getMessageData(String fileName) {
		File fileRelative = new File("src/main/resources/config/"+fileName);
		String data = ""; 
	    try {
			data = new String(Files.readAllBytes(Paths.get(fileRelative.getAbsolutePath())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    return data;
	}
	
	private class TimeUpdater extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(55);
			ACLMessage msg = baseAgent.receive(mt);
			if (msg != null) {
				finished();
			} else {
				block();
			}
		}
	}
	
	private class PackagingPhaseMessageSender extends OneShotBehaviour {
		private AID receivingAgent = null;
		
		protected void findReceiver() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            // SERVICE TYPE FOR RECEIVING ORDER CONFIRMATIONS:
            sd.setType("order-aggregator");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                receivingAgent = result[0].getName();
                
                if (receivingAgent == null) {
                	System.out.println("["+getAID().getLocalName()+"]: No OrderAggregator agent found.");
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
		
		public void action() {
			findReceiver();			
			
			ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);

			msg1.addReceiver(receivingAgent); 
			msg1.setContent(getMessageData("LoadingBayMessageExample_1"));
			msg1.setConversationId("packaged-orders");
			msg1.setPostTimeStamp(System.currentTimeMillis());
			
			myAgent.send(msg1);
			
			System.out.println("["+getAID().getLocalName()+"]: Order sent to OrderAggregator:\n"+msg1.toString());
			
			try {
	 			Thread.sleep(3000);
	 		} catch (InterruptedException e) {
	 			//e.printStackTrace();
	 		}
			
			ACLMessage msg2 = new ACLMessage(ACLMessage.INFORM);
			
			msg2.addReceiver(receivingAgent); 
			msg2.setContent(getMessageData("LoadingBayMessageExample_2"));
			msg2.setConversationId("packaged-orders");
			msg2.setPostTimeStamp(System.currentTimeMillis());
			
			myAgent.send(msg2);
			
			System.out.println("["+getAID().getLocalName()+"]: Order sent to OrderAggregator:\n"+msg2.toString());
           
		}
	}

}