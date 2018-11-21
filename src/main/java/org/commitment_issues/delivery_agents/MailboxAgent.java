package org.commitment_issues.delivery_agents;

import org.yourteamname.agents.BaseAgent;

import org.json.*;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class MailboxAgent extends BaseAgent {

	protected void setup() {
		super.setup();
		System.out.println("Hello! Mailbox-agent "+getAID().getName()+" is ready.");
		
		register("mailbox", "mailbox");
		
		addBehaviour(new truckDeliveryCompletionProcessor());
		
	}

	protected void takeDown() {
		deRegister();
		System.out.println(getAID().getLocalName() + ": Terminating.");
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
				
				// At the moment, this list includes all customers as well.
				// TODO: Send the message only to the specific customer
				findReceivers();
				ACLMessage orderConfirmation = new ACLMessage(ACLMessage.INFORM);
				
				for (int i = 0; i < receiverAgents.length; ++i) {
					orderConfirmation.addReceiver(receiverAgents[i]);
				}
				
				orderConfirmation.setContent(truckMessageContent);
				orderConfirmation.setConversationId("order-confirmation");
				orderConfirmation.setPostTimeStamp(System.currentTimeMillis());
				myAgent.send(orderConfirmation);
			}

			else {
				block();
			}
			
			// This is called unconditionally since the agent does not depend on time
			finished();
		}
	}
}