package org.maas.agents;

import java.util.ArrayList;

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
public class DummyAgent extends TimedAgent {
	protected void setup() {
	// Printout a welcome message
		System.out.println("Hello! Dummy-agent "+getAID().getName()+" is ready.");

        try {
 			Thread.sleep(3000);
 		} catch (InterruptedException e) {
 			//e.printStackTrace();
 		}
//		addBehaviour(new shutdown());
        addBehaviour(new ReceiveOrderConfirmation());
		ArrayList<String> services = new ArrayList<String>();
		services.add("order-confirmation");
		register(services, "order-confirmation");

	}
	protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
	private class ReceiveOrderConfirmation extends CyclicBehaviour {
		private MessageTemplate mt;

		public void action() {
			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("order-confirmation"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null) {
				System.out.println("["+getAID().getLocalName()+"]: Received order completion message from "+msg.getSender().getLocalName()+":\n"+msg.getContent());
				System.out.println("Triggering System Shutdown");
				myAgent.addBehaviour(new shutdown());
			}

			else {
				block();
			}
		}
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
}
