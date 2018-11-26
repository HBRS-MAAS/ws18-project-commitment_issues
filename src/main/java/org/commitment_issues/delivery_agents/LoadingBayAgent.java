package org.commitment_issues.delivery_agents;

import org.json.*;
import org.yourteamname.agents.BaseAgent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class LoadingBayAgent extends BaseAgent {
	
	protected void setup() {
		System.out.println("Hello! LoadingBay-agent "+getAID().getName()+" is ready.");
		
		register("loading-bay", "loading-bay");		
		
	}
	
	protected void takeDown() {
		deRegister();
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
}
