package org.commitment_issues.agents;


import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class SchedulerAgent extends Agent {
	protected void setup() {
		System.out.println("Hello! SchedulerAgent "+ getAID().getName() +" is ready.");
		
		registerInYellowPages();
		
//		addBehaviour(new ItemsPreparedServer());
//		addBehaviour(new ReadyToBakeInformer());

	}
	
	 protected void registerInYellowPages() {
	        // Register the proofing service in the yellow pages

	        DFAgentDescription dfd = new DFAgentDescription();
	        dfd.setName(getAID());

	        ServiceDescription sd = new ServiceDescription();
	        sd.setType("scheduling");
	        sd.setName("scheduling");
	        dfd.addServices(sd);

	        try {
	            DFService.register(this, dfd);
	        } catch (FIPAException fe) {
	            fe.printStackTrace();
	        }
	    }

	    protected void deregisterFromYellowPages() {
	        // Deregister from the yellow pages
	        try {
	            DFService.deregister(this);
	        } catch (FIPAException fe) {
	            fe.printStackTrace();
	        }
	    }

	protected void takeDown() {
		deregisterFromYellowPages();
		
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
	@SuppressWarnings("unused")
	private class PendingOrderRequester extends CyclicBehaviour {
		public void action() {
			//TODO
		}
	} 
	
	@SuppressWarnings("unused")
	private class ProductInfoRequester extends Behaviour {
		public void action() {
			// TODO
		}
		
		public boolean done() {
			//TODO
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	private class DeliveryTimeRequester extends Behaviour {
		public void action() {
			// TODO
		}
		
		public boolean done() {
			//TODO
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	private class KneadingRequester extends Behaviour {
		public void action() {
			// TODO
		}
		
		public boolean done() {
			//TODO
			return false;
		}
	}
	
	// Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	@SuppressWarnings("unused")
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
