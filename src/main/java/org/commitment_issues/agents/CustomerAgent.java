package org.commitment_issues.agents;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


@SuppressWarnings("serial")
public class CustomerAgent extends Agent {
	protected void setup() {
		System.out.println("Hello! Customer-agent "+getAID().getName()+" is ready.");

        try {
 			Thread.sleep(3000);
 		} catch (InterruptedException e) {
 			//e.printStackTrace();
 		}
        
        addBehaviour(new OrderGenerator());

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
//		private int day;
//		private int hour;
		private AID[] orderProcessorAgents;
		private int step = 0;
		
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
        }
		
		public void action() {
			switch(step) {
			case 0:
				discoverProcessors();
				ACLMessage order = new ACLMessage(ACLMessage.INFORM);
				
				for (int i = 0; i < orderProcessorAgents.length; ++i) {
					order.addReceiver(orderProcessorAgents[i]);
				}
				
				String orderDetails = "<005.10> Bagels:5; Bread:10; Cookies:20";
				order.setContent(orderDetails);
				order.setConversationId("bakery-order");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bakery-order"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				
				step = 1;
				break;
				
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.INFORM) {
						orderProcessor = reply.getSender();
						System.out.println("["+getAID().getLocalName()+"]: Order reception confirmed by "+ orderProcessor.getLocalName());
					}
					
					step = 2;
				}

				break;
				
			default:
				break;
					
			}
		}
		
		public boolean done() {
			if (step == 2) {
				addBehaviour(new shutdown());
			}
			return (step == 2);
		}
	}

}