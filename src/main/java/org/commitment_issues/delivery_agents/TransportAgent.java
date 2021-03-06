package org.commitment_issues.delivery_agents;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import org.commitment_issues.agents.CustomerAgent;
import org.json.*;
import org.maas.agents.BaseAgent;


@SuppressWarnings("serial")
public class TransportAgent extends BaseAgent {
  private  ArrayList<Order> orders = new ArrayList<Order>(); //list of all the orders
  private HashMap<String, String> customerInfo = new HashMap<String, String>();
  private AID[] trucks;//list of all the trucks
  
  protected String scenarioDirectory_;

	protected void setup() {
		super.setup();
		System.out.println("Hello! TransportAgent-agent " + getAID().getLocalName() + " is ready.");
		
		Object args[] = getArguments();
		if (args != null && args.length > 0) {
			scenarioDirectory_ = args[0].toString();
		}

		
		register(getBakeryName() + "-transport-agent", getBakeryName() + "-transport-agent");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		trucksFinder();// search for the trucks
		addBehaviour(new OrderParser());
		addBehaviour(new OrderDetailsReceiver());
		addBehaviour(new truckReady());// check if trucks are ready to pick orders
		addBehaviour(new TimeUpdater());
	}
	
	  public String getBakeryName() {
		  return getLocalName().split("_")[0];
	  }

	protected void takeDown() {
		deRegister();
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

	protected float[] getNodePosition(String guid) {
		float location[] = new float[2];
		File relativePath = new File("src/main/resources/config/" + scenarioDirectory_ + "/street-network.json");
		String streetNWContents = CustomerAgent.readFileAsString(relativePath.getAbsolutePath());
		JSONArray nodesArray = new JSONObject(streetNWContents).getJSONArray("nodes");
		for (int i = 0; i < nodesArray.length(); i++) {
			JSONObject node = nodesArray.getJSONObject(i);
			String id = null;
			try {
				id = node.getString("company");
			} catch (JSONException e) {
				continue;
			}

			if (id.equals(guid)) {
				JSONObject position = node.getJSONObject("location");
				location[0] = position.getInt("x");
				location[1] = position.getInt("y");
				return location;
			}
		}
		return location;
	  }
  
  protected void trucksFinder() {
    // search for the trucks by their services 
    DFAgentDescription template = new DFAgentDescription();
    ServiceDescription sd = new ServiceDescription();
		sd.setType("transport-orders");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			trucks = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {
				trucks[i] = result[i].getName();
			}
    } catch (FIPAException fe) {
        fe.printStackTrace();
    }
  }
  
	private class TimeUpdater extends CyclicBehaviour {
		public void action() {
		      if (getAllowAction()) {
		        finished();
		      } 
		    }
	}

  private class OrderParser extends CyclicBehaviour{
    // Periodically updates the pending orders list by the data it takes from order aggregator
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("transport-order");
			ACLMessage msg = baseAgent.receive(mt);
			if (msg != null) {
				JSONArray JSONOrdersBoxes = new JSONArray(msg.getContent());// a list of all the orders with their boxes
				System.out.println(getAID().getLocalName() + " Received transport order");
				for (int i = 0; i < JSONOrdersBoxes.length(); i++) {
					JSONObject wholeOrder = JSONOrdersBoxes.getJSONObject(i);
          String cutID = wholeOrder.getString("CustId");
          float [] custLocation = getNodePosition(cutID);
          String bakID = wholeOrder.getString("BackId");
          float [] bakLocation = getNodePosition(bakID);
					String wholeOrderID = wholeOrder.getString("OrderId");

					Order order = new Order();
					order.setOrderID(wholeOrderID);
					order.setLocation(bakLocation);
					order.setDestination(custLocation);
					JSONArray boxesss = wholeOrder.getJSONArray("boxes");

					for (int k = 0; k < boxesss.length(); k++) {
						JSONObject box = boxesss.getJSONObject(i);
						String boxID = box.getString("BoxID");
						String productType = box.getString("ProductType");
						int quantity = box.getInt("Quantity");
						Box boxObject = new Box(boxID, productType, quantity, 0);
						order.addBoxes(boxObject);
					}
					orders.add(order);

					baseAgent.addBehaviour(new TrucksRequester(order));
				}

			} else {
				block();
			}

		}
	}

	private class TrucksRequester extends Behaviour {
		private Order order;
		private String orderID;
		private float bestTime;
		private AID fastestTruck = null;
		private int rpliesCount;
		private MessageTemplate mt;
		private int step = 0;

		public TrucksRequester(Order order) {
			super();
			this.order = order;
      this.orderID = order.getOrderID();
      bestTime = 9999;
    }
    @Override
    public void action() {        
      // Creating a JSON object to send it to all the trucks
			JSONObject assignmentToTrucks = new JSONObject();
			assignmentToTrucks.put("OrderID", this.order.getOrderID());
			JSONObject source = new JSONObject();
			source.put("X", this.order.getLocation()[0]);
			source.put("Y", this.order.getLocation()[1]);
			JSONObject destination = new JSONObject();
			destination.put("X", this.order.getDestination()[0]);
			destination.put("Y", this.order.getDestination()[1]);
			assignmentToTrucks.put("Source", source);
			assignmentToTrucks.put("Destination", destination);
			assignmentToTrucks.put("NumOfBoxes", this.order.getBoxes().size());
			assignmentToTrucks.put("CustomerId", customerInfo.get(this.order.getOrderID()));

			// Start communicating with the trucks to find the fastest one
      switch (step) {
      case 0:
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        if (trucks != null) {
          for (int i = 0; i < trucks.length; ++i) {
            cfp.addReceiver(trucks[i]);
					}
				}
				cfp.setContent(assignmentToTrucks.toString());
				cfp.setConversationId(this.order.getOrderID());
				cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
				System.out.println("Placed a proposal request from all trucks for order ID: " + this.order.getOrderID());
				baseAgent.sendMessage(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId(this.order.getOrderID()),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				ACLMessage reply = baseAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer
						float time = Float.parseFloat(reply.getContent());
						if (fastestTruck == null || time < bestTime) {
							// This is the best offer at present
							bestTime = time;
							fastestTruck = reply.getSender();
						}
					}
					rpliesCount++;
					if (rpliesCount >= trucks.length) {
						// We received all replies
						if (fastestTruck == null) {
							step = 1;
						} else {
							step = 2;
						}
					}
				} else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the truck with the best time
				ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				confirm.addReceiver(fastestTruck);
				confirm.setContent(assignmentToTrucks.toString());
				confirm.setConversationId(orderID);
				confirm.setReplyWith("order" + System.currentTimeMillis());
				baseAgent.sendMessage(confirm);
				step = 3;
				break;

			default:
				break;
			}

		}

		@Override
		public boolean done() {
			return (step == 3);
		}

	}

	private class truckReady extends CyclicBehaviour {
		private String orderID;
		private ArrayList<Box> boxes = new ArrayList<Box>();

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage truckRequest = baseAgent.receive(mt);

			if (truckRequest != null) {
				ACLMessage reply = truckRequest.createReply();
				JSONObject truckArrived = new JSONObject(truckRequest.getContent());
				orderID = truckArrived.getString("OrderId");
				for (int i = 0; i < orders.size(); i++) {
					if (orderID.equals(orders.get(i).getOrderID())) {
						boxes = orders.get(i).getBoxes();

						break;
					}
				}
				JSONObject assignment = new JSONObject();
				assignment.put("OrderID", orderID);
				JSONArray boxesJSON = new JSONArray();
				for (int k = 0; k < boxes.size(); k++) {
					JSONObject boxJSON = new JSONObject();
					boxJSON.put("BoxID", boxes.get(k).getBoxID());
					boxJSON.put("ProductType", boxes.get(k).getProductType());
					boxJSON.put("Quantity", boxes.get(k).getQuantity());
					boxesJSON.put(boxJSON);

				}
				assignment.put("Boxes", boxesJSON);
				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(assignment.toString());
				baseAgent.sendMessage(reply);
			} else {
				block();
			}

		}

	}
	
	private class OrderDetailsReceiver extends CyclicBehaviour {
		//private String orderProcessorServiceType;
		//private AID orderProcessor = null;
		private MessageTemplate mt;

		/*protected void findOrderProcessor() {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			orderProcessorServiceType = "OrderProcessing";

			sd.setType(orderProcessorServiceType);
			template.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(baseAgent, template);
				if (result.length > 0) {
					orderProcessor = result[0].getName();
				}
			} catch (FIPAException fe) {
				System.out.println("[" + getAID().getLocalName() + "]: No OrderProcessor agent found.");
				fe.printStackTrace();
			}
		}*/

		public void action() {
//			findOrderProcessor();

			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("order"),
          MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msg = baseAgent.receive(mt);

			if (msg != null) {
				JSONObject obj = new JSONObject(msg.getContent());
				String orderID = obj.getString("guid");
				customerInfo.put(orderID, obj.getString("customerId"));
				
			} else {
				block();
			}
		}
	}
}
