package org.commitment_issues.delivery_agents;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.*;

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
	
	protected void parseTruckConfirmationMessage(String truckMessage) {
		String orderDeliveredTo;
		String orderDeliveredBy;
		int dayOfDelivery;
		int timeOfDelivery;
		int numOfBoxes;
		String producedBy;
		
		JSONObject truckMessageData = new JSONObject(truckMessage);
		JSONObject deliveryStatus = truckMessageData.getJSONObject("DeliveryStatus");
		
		orderDeliveredTo = deliveryStatus.getString("OrderDeliveredTo");
		orderDeliveredBy = deliveryStatus.getString("OrderDeliveredBy");
		dayOfDelivery = deliveryStatus.getInt("DayOfDelivery");
		timeOfDelivery = deliveryStatus.getInt("TimeOfDelivery");
		numOfBoxes = deliveryStatus.getInt("NumOfBoxes");
		
		producedBy = deliveryStatus.getString("ProducedBy");
		
	}
}
