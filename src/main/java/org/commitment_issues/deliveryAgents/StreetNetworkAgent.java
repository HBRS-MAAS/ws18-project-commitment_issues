package org.commitment_issues.deliveryAgents;

import java.util.Hashtable;

import org.commitment_issues.CustomerOrder;
import org.json.*;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class StreetNetworkAgent extends Agent {

	protected void setup() {
		System.out.println("Hello! StreetNetwork-agent "+getAID().getName()+" is ready.");
		
		registerInYellowPages();

	}
	
	protected void registerInYellowPages() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("street-network");
        sd.setName("street-network");
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
	
	protected void parseStreetNetworkData(String streetNetworkData) {
		JSONObject JSONSNData = new JSONObject(streetNetworkData);
		
		boolean directed = JSONSNData.getBoolean("directed");
		
		JSONArray nodesJSONArray = JSONSNData.getJSONArray("nodes");
		JSONArray linksJSONArray = JSONSNData.getJSONArray("links");
		
		
				
	}


}
