package org.commitment_issues.delivery_agents;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.yourteamname.agents.BaseAgent;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class LoadingBayAgent extends BaseAgent {
//	private JSONArray orderDetailsArray = new JSONArray();
	private JSONArray orderDetailsArray = null;
	private String readyOrderID = null;
	
	private HashMap<String, HashMap<String, Integer>> productDatabase = 
			new HashMap<>();
	private HashMap<String, JSONArray> boxDatabase = new HashMap<>();
	
	protected void setup() {
		
		super.setup();
		System.out.println("Hello! LoadingBay-agent "+getAID().getName()+" is ready.");
		
		register("loading-bay", "loading-bay");	
		
//		addBehaviour(new OrderDetailsReceiver());
		orderDetailsArray = getDummyOrderData();
		addBehaviour(new ProductDetailsReceiver());
		addBehaviour(new TimeUpdater());
	}
	
	protected void takeDown() {
		deRegister();
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
//	protected void addCustomerOrder (String customerID) {
//		HashMap <String, Integer> temp = new HashMap<String, Integer>();
//		temp.put(product, quantity);
//		productDatabase.put(orderID, temp);
//	}
	
	protected void addCustomerProduct (String orderID, String product, int quantity) {
		HashMap <String, Integer> temp = new HashMap<String, Integer>();
		temp.put(product, quantity);
		productDatabase.put(orderID, temp);
	}
	
	protected void UpdateCustomerProductQuantity (String orderID, String product, int addedQuantity) {
		int oldQuantity = productDatabase.get("order1").get("bread");
		int newQuantity = oldQuantity + addedQuantity;
		productDatabase.get(orderID).replace(product, newQuantity);
	}
	
	protected String getMessageData(String fileName) {
		File fileRelative = new File("src/main/resources/config/"+fileName);
		String data = ""; 
	    try {
			data = new String(Files.readAllBytes(Paths.get(fileRelative.getAbsolutePath())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    return data;
	}
	
	private class TimeUpdater extends CyclicBehaviour {
		public void action() {
		      if (getAllowAction()) {
		        finished();
		      } 
		    }
	}
	
	private class PackagingPhaseMessageSender extends OneShotBehaviour {
		private AID receivingAgent = null;
		
		protected void findReceiver() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            // SERVICE TYPE FOR RECEIVING ORDER CONFIRMATIONS:
            sd.setType("order-aggregator");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                receivingAgent = result[0].getName();
                
                if (receivingAgent == null) {
                	System.out.println("["+getAID().getLocalName()+"]: No OrderAggregator agent found.");
                }
//                else {
//                	receivingAgent = result[0].getName();
//                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
		
		public void action() {
			findReceiver();			
			
//			ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);
//
//			msg1.addReceiver(receivingAgent); 
//			msg1.setContent(getMessageData("LoadingBayMessageExample_1"));
//			msg1.setConversationId("packaged-orders");
//			msg1.setPostTimeStamp(System.currentTimeMillis());
//			
//			myAgent.send(msg1);
//			
//			System.out.println("["+getAID().getLocalName()+"]: Order sent to OrderAggregator:\n"+msg1.toString());
//			
//			try {
//	 			Thread.sleep(3000);
//	 		} catch (InterruptedException e) {
//	 			//e.printStackTrace();
//	 		}
//			
//			ACLMessage msg2 = new ACLMessage(ACLMessage.INFORM);
//			
//			msg2.addReceiver(receivingAgent); 
//			msg2.setContent(getMessageData("LoadingBayMessageExample_2"));
//			msg2.setConversationId("packaged-orders");
//			msg2.setPostTimeStamp(System.currentTimeMillis());
//			
//			myAgent.send(msg2);
//			
//			System.out.println("["+getAID().getLocalName()+"]: Order sent to OrderAggregator:\n"+msg2.toString());
//           
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			
			msg.addReceiver(receivingAgent); 
			msg.setContent(createOrderBoxesJSONMessage(readyOrderID));
			msg.setConversationId("packaged-orders");
			msg.setPostTimeStamp(System.currentTimeMillis());
			
			myAgent.send(msg);
			
			System.out.println("["+getAID().getLocalName()+"]: Order details sent to OrderAggregator:\n"+msg);
		}
	}
	
//	private class OrderDetailsReceiver extends CyclicBehaviour {
//		private MessageTemplate mt;
//		
//		public void action() {
//			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("..........."),
//					MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
//			ACLMessage msg = myAgent.receive(mt);
//			
//			if (msg != null) {
//				orderDetailsArray = new JSONArray(msg.getContent());
//			} else {
//				block();
//			}
//		}
//	}
	
	// Later change to generic behavior with a limiting condition, to avoid having to keep it being
	// called indefinitely (for this test case).
	private class OrderDetailsReceiver extends Behaviour {
		private AID orderProcessor;
		private MessageTemplate mt;
		private int step = 0;
		
		protected void findOrderProcessor() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            // SERVICE TYPE FOR RECEIVING ORDER CONFIRMATIONS:
            sd.setType("order-processor");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                orderProcessor = result[0].getName();
                
                if (orderProcessor == null) {
                	System.out.println("["+getAID().getLocalName()+"]: No OrderProcessor agent found.");
                }
//                else {
//                	orderProcessor = result[0].getName();
//                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }

		public void action() {
			findOrderProcessor();
			
			switch (step) {
			case 0:
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

				cfp.addReceiver(orderProcessor);
				String convID = "loading-bay";
				cfp.setContent(convID);
				cfp.setConversationId(convID);
				cfp.setPostTimeStamp(System.currentTimeMillis());
				
				myAgent.send(cfp);
				
				mt = MessageTemplate.MatchConversationId(convID);
				
				step = 1;
				break;
				
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				
				if (reply != null) {
					orderDetailsArray = new JSONArray(reply.getContent());					
				}
				else {
					block();
				}
				
				step = 2;
				break;
				
			default:
				break;
			}
		}
		
		public boolean done() {
			return (step == 2);
		}
	}
	
	protected JSONArray getDummyOrderData() {
		File fileRelative = new File("src/main/resources/config/small/orderprocessor.json");
		String data = ""; 
	    try {
			data = new String(Files.readAllBytes(Paths.get(fileRelative.getAbsolutePath())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	    JSONArray orderArray = new JSONArray(data);
	    
		return orderArray;
	}
	
	private class ProductDetailsReceiver extends CyclicBehaviour {
		private MessageTemplate mt;
		
		public void action() {
			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("..........."),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null) {
				System.out.println("["+getAID().getLocalName()+"]: Received product boxes from "+msg.getSender().getLocalName());

				// Assumes a json object is sent
				String boxesMessageContent = msg.getContent();
				JSONObject JSONData = new JSONObject(boxesMessageContent);
				String orderID = JSONData.getString("OrderID");
				
				updateBoxDatabase(boxesMessageContent);
				updateProductDatabase(boxesMessageContent);
				
				if (orderProductsReady(orderID)) {
					readyOrderID = orderID;
					addBehaviour(new PackagingPhaseMessageSender());
				}
			} else {
				block();
			}
		}
	}
	
	protected String createOrderBoxesJSONMessage (String orderID) {
		JSONObject message = new JSONObject();
		message.put("OrderID", orderID);
		
		JSONArray boxesArray = new JSONArray();
		
		for (String key : boxDatabase.keySet()) {
			if (key.equals(orderID)) {
				boxesArray.put(boxDatabase.get(key));
			}
		}
		
		message.put("Boxes", boxesArray);
		
		return message.toString();
	}
	
	protected void updateBoxDatabase (String orderBoxesDetails) {
		JSONObject JSONData = new JSONObject(orderBoxesDetails);
		
		String orderID = JSONData.getString("OrderID");
		JSONArray boxes = JSONData.getJSONArray("Boxes");
		
		boxDatabase.put(orderID, boxes);
	}
	
	protected void updateProductDatabase (String orderBoxesDetails) {
		JSONObject JSONData = new JSONObject(orderBoxesDetails);
		
		String orderID = JSONData.getString("OrderID");
		JSONArray boxes = JSONData.getJSONArray("Boxes");
		
		// Check if the database does not contain this order's details
		if (!productDatabase.containsKey(orderID)) {
			for (int i = 0 ; i < boxes.length(); i++) {
				JSONObject boxDetails = boxes.getJSONObject(i);
				addCustomerProduct (orderID, boxDetails.getString("ProductType"), boxDetails.getInt("Quantity"));
			}
		}
		// In the event that it does:
		else {
			// Get the product details currently associated with and stored for this orderID
			HashMap<String, Integer> orderProductDetails = productDatabase.get("orderID");
			
			for (int i = 0 ; i < boxes.length(); i++) {
				JSONObject boxDetails = boxes.getJSONObject(i);
				String productType = boxDetails.getString("ProductType");
				
				// If the order entry in the database already has this product in a certain quantity:
				if (orderProductDetails.containsKey(productType)) {
					// Update that entry with the additional quantity of that product
					UpdateCustomerProductQuantity (orderID, productType, boxDetails.getInt("Quantity"));
				}
				// if it doesn't, simply add it to that order entry's product list:
				else {
					addCustomerProduct (orderID, productType, boxDetails.getInt("Quantity"));
				}
			}
		}
	}
	
	protected boolean orderProductsReady (String orderID) {
		/*
		 * Returns true if the order details (products and their quantities) are
		 * fulfilled in the database for that particular customer order.
		 */
		HashMap<String, Integer> orderProductDetails = productDatabase.get(orderID);
		
		JSONArray productArray = new JSONArray();
		String IDCheckString = null;
		
		for (int i = 0 ; i < orderDetailsArray.length(); i++) {
			JSONObject orderData = orderDetailsArray.getJSONObject(i);
			
			if (orderID.equals(orderData.getString("OrderID"))) {
				IDCheckString = orderData.getString("OrderID");
				productArray = orderData.getJSONArray("Products");
				break;
			}
		}
		
		// Check if order product array was retrieved
//		System.out.println("["+getAID().getLocalName()+"]: Product array found: "+productArray.toString());
		if (IDCheckString.equals(null)) {
			System.out.println("["+getAID().getLocalName()+"]: ERROR: OrderID not found in orderDetailsArray ");
		}
		
		for (int j = 0 ; j < productArray.length() ; j++) {
			String productName = null;;
			JSONObject product = productArray.getJSONObject(j);
			
			// Get product name from key
			for (String key : product.keySet()) {
			    productName = key;
			}
			
			int orderQuantity = product.getInt(productName);
			
			if (orderProductDetails.get(productName).equals(null)) {
				return false;
			}
			else {
				if (orderProductDetails.get(productName) != orderQuantity) {
					return false;
				}
			}
		}
		return true;
	}

}
