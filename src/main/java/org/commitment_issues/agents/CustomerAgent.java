package org.commitment_issues.agents;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


@SuppressWarnings("serial")
public class CustomerAgent extends Agent {
	protected void setup() {
	// Printout a welcome message
		System.out.println("Hello! Customer-agent "+getAID().getName()+" is ready.");

        try {
 			Thread.sleep(3000);
 		} catch (InterruptedException e) {
 			//e.printStackTrace();
 		}
        
        addBehaviour(new OrderGenerator());
        
//		addBehaviour(new shutdown());

	}
	
	protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
	// Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
		private class shutdown extends OneShotBehaviour{
			public void action() {
				ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
				Codec codec = new SLCodec();
				myAgent.getContentManager().registerLanguage(codec);
				myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
				shutdownMessage.addReceiver(myAgent.getAMS());
				shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
				shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
				try {
				    myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
				    myAgent.send(shutdownMessage);
				}
				catch (Exception e) {
				    //LOGGER.error(e);
				}

			}
		}
    
	private class OrderGenerator extends Behaviour {
		private AID orderProcessor;
		private int day;
		private int hour;
		private AID[] orderProcessorAgents;
		
		private MessageTemplate mt;
		
		protected void discoverProcessors() {
            // Update the list of order processor agents
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("order-processor");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                orderProcessorAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    orderProcessorAgents[i] = result[i].getName();
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            // for (AID a : orderProcessorAgents) {
            // System.out.println("Found order processor: " + a.getLocalName());
            // }
        }
		
		public void action() {
			ACLMessage order = new ACLMessage(ACLMessage.INFORM);
		}
		
		public boolean done() {
			return true;
		}
	}

}