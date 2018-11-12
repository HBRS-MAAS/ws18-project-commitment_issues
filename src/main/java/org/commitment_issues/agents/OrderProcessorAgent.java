package org.commitment_issues.agents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class OrderProcessorAgent extends Agent {
	protected void setup() {
		System.out.println("Hello! OrderProcessor-agent "+getAID().getName()+" is ready.");
		
		registerInYellowPages();
		
		addBehaviour(new OrderServer());

	}
	
	 protected void registerInYellowPages() {
	        // Register the order-processing service in the yellow pages

	        DFAgentDescription dfd = new DFAgentDescription();
	        dfd.setName(getAID());

	        ServiceDescription sd = new ServiceDescription();
	        sd.setType("order-processor");
	        sd.setName("order-processing");
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

	private class OrderServer extends CyclicBehaviour {
		public void action() {
			ACLMessage msg = myAgent.receive();
			
			if (msg != null) {
				String orderDetails = msg.getContent();
				ACLMessage reply = msg.createReply();
				
				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent("order-received");
				
				System.out.println("["+getAID().getLocalName()+"]: Order received: "+orderDetails);

				myAgent.send(reply);
			}
			
			else {
				block();
			}
		}
	}
	
	protected void parseOrder(String clientData) {
		JSONObject JSONClientData = new JSONObject(clientData.substring(1, clientData.length() - 1));
		
		String customerName = JSONClientData.getString("name");
		String customerId = JSONClientData.getString("guid");
		int customerType = JSONClientData.getInt("type");
		float customerLocationX = JSONClientData.getJSONObject("location").getFloat("x");
		float customerLocationY = JSONClientData.getJSONObject("location").getFloat("y");
				
		JSONArray orderJsonArray = JSONClientData.getJSONArray("orders");
				
		JSONObject order = orderJsonArray.getJSONObject(0);
		String orderID = order.getString("guid");
		
		JSONObject orderDate = order.getJSONObject("orderDate");
		int orderDay = orderDate.getInt("day");
		int orderTime = orderDate.getInt("hour");
		
		JSONObject deliveryDate = order.getJSONObject("deliveryDate");
		int deliveryDay = deliveryDate.getInt("day");
		int deliveryTime = deliveryDate.getInt("hour");
		
		JSONObject orderProducts = order.getJSONObject("products");
		
	}
	
	public static String readFileAsString(String fileName) { 
	    String data = ""; 
	    try {
			data = new String(Files.readAllBytes(Paths.get(fileName)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	    return data; 
	 }

}
