package org.commitment_issues.delivery_agents;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.*;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class MailboxAgent extends Agent {

	protected void setup() {
		System.out.println("Hello! Mailbox-agent "+getAID().getName()+" is ready.");
		
		registerInYellowPages();
		
		addBehaviour(new truckDeliveryCompletionProcessor());
		
	}
	
	protected void registerInYellowPages() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("mailbox");
        sd.setName("mailbox");
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
	
	private class truckDeliveryCompletionProcessor extends CyclicBehaviour {
		private MessageTemplate mt;
		private AID[] receiverAgents;
		
		protected void findReceivers() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            // SERVICE TYPE FOR RECEIVING ORDER CONFIRMATIONS:
            sd.setType("order-confirmation");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                receiverAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                	receiverAgents[i] = result[i].getName();
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }

		public void action() {
			
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("DeliveryConfirmation"),
					MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
			msg = myAgent.receive(mt);
			
			if (msg != null) {
				String truckMessageContent = msg.getContent();
//				DeliveryStatus confirmedStatus = parseTruckConfirmationMessage(truckMessageContent);

//				ACLMessage reply = msg.createReply();
//				...
//
//				reply.setPerformative(ACLMessage.INFORM);
//				reply.setContent(String.valueOf(time));
//				myAgent.send(reply);
				
				findReceivers();
				ACLMessage orderConfirmation = new ACLMessage(ACLMessage.INFORM);
				
				for (int i = 0; i < receiverAgents.length; ++i) {
					orderConfirmation.addReceiver(receiverAgents[i]);
				}
				
				orderConfirmation.setContent(truckMessageContent);
				orderConfirmation.setConversationId("order-confirmation");
				orderConfirmation.setReplyWith("Order Confirmation"+System.currentTimeMillis());
				myAgent.send(orderConfirmation);
			}

			else {
				block();
			}
		}
	}
	
	protected DeliveryStatus parseTruckConfirmationMessage(String truckMessage) {		
		DeliveryStatus status = new DeliveryStatus();
		
		JSONObject truckMessageData = new JSONObject(truckMessage);
		JSONObject deliveryStatus = truckMessageData.getJSONObject("DeliveryStatus");
		
		status.orderDeliveredTo = deliveryStatus.getString("OrderDeliveredTo");
		status.orderDeliveredBy = deliveryStatus.getString("OrderDeliveredBy");
		status.dayOfDelivery = deliveryStatus.getInt("DayOfDelivery");
		status.timeOfDelivery = deliveryStatus.getInt("TimeOfDelivery");
		status.numOfBoxes = deliveryStatus.getInt("NumOfBoxes");
		status.producedBy = deliveryStatus.getString("ProducedBy");
		
		return status;		
	}
}
