package org.commitment_issues.delivery_agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.maas.agents.BaseAgent;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class OrderAggregatorAgent extends BaseAgent {
  private ArrayList<Order> orders = new ArrayList<Order>(); //list of all the orders
  public HashMap<String, OrderInfo> pendingOrderInfo = new HashMap<String, OrderInfo>();

  private AID transportAgent = null;
  protected void setup() {
	super.setup();
	System.out.println("Hello! OrderAggregatorAgent " + getAID().getLocalName() + " is ready.");
    this.register(getBakeryName() + "-order-aggregator",getBakeryName() + "-order-aggregator");
    while(this.transportAgent == null) {
      findTransportAgent();
    }
    addBehaviour(new OrderDetailsReceiver());
    addBehaviour(new LoadingBayParser());
    addBehaviour(new TimeUpdater());
  }
  
  public String getBakeryName() {
	  return getLocalName().split("_")[0];
  }

  
  protected void takeDown() {
    deRegister();
    
    System.out.println(getAID().getLocalName() + ": Terminating.");
  }
  
  private void findTransportAgent() {
    DFAgentDescription template = new DFAgentDescription();
    ServiceDescription sd = new ServiceDescription();
    sd.setType(getBakeryName() + "-transport-agent");
    template.addServices(sd);
    try {
      DFAgentDescription[] result = DFService.search(this, template);
      this.transportAgent = result[0].getName();
     
  } catch (Exception fe) {
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
  
  private class OrderInfo {
	public String custID;
	public ArrayList<String> products;
  }
  
	private class LoadingBayParser extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("packaged-orders");
			ACLMessage msg = baseAgent.receive(mt);

			if (msg != null) {
				JSONObject recieved = new JSONObject(msg.getContent());
				JSONArray boxesJSON = recieved.getJSONArray("Boxes");
				String orderID = recieved.getString("OrderID");

				Order order = null;
				for (int k = 0; k < ((OrderAggregatorAgent) baseAgent).orders.size(); k++) {
					if ((((OrderAggregatorAgent) baseAgent).orders.get(k).getOrderID()).equals(orderID)) {
						order = orders.get(k);
						orders.remove(order);
						break;
					}
				}

				if (order == null) {
					order = new Order();
					order.setOrderID(orderID);
					order.setBakID(getBakeryName());
					order.setCustID(pendingOrderInfo.get(orderID).custID);
				}

				for (int i = 0; i < boxesJSON.length(); i++) {
					JSONObject boxJSON = boxesJSON.getJSONObject(i);
					Box box = new Box();
					String productType = boxJSON.getString("ProductType");
					box.setBoxID(boxJSON.getString("BoxID"));
					box.setProductType(productType);
					box.setQuantity(boxJSON.getInt("Quantity"));
					order.addBoxes(box);

					pendingOrderInfo.get(orderID).products.remove(productType);
				}

				if (pendingOrderInfo.get(orderID).products.size() <= 0) {
					baseAgent.addBehaviour(new SendOrderToTransport(order));
					pendingOrderInfo.remove(orderID);
				} else {
					((OrderAggregatorAgent) baseAgent).orders.add(order);
				}
				System.out.println(getAID().getName() + " recieved an order");
			} else {
				block();
			}

		}

	}
  
  private class SendOrderToTransport extends OneShotBehaviour {
    private Order order;
    private ACLMessage finalOrder = new ACLMessage(ACLMessage.INFORM);
    public SendOrderToTransport(Order fullOrder) {
    this.order = fullOrder;
  }

    @Override
    public void action() {
      finalOrder.setConversationId("transport-order");
      finalOrder.addReceiver(transportAgent);
      JSONArray msgJSON = new JSONArray();
      JSONArray boxesJSON = new JSONArray();
      JSONObject orderJSON = new JSONObject();
      orderJSON.put("OrderId",order.getOrderID());
      orderJSON.put("CustId", order.getCustID());
      orderJSON.put("BackId", order.getBakID());
      for (int i = 0; i < order.getBoxes().size(); i++) {
        JSONObject box = new JSONObject();
        box.put("BoxID", order.getBoxes().get(i).getBoxID());
        box.put("ProductType", order.getBoxes().get(i).getProductType());
        box.put("Quantity", order.getBoxes().get(i).getQuantity());
        boxesJSON.put(box);
      }
      orderJSON.put("boxes", boxesJSON);
      msgJSON.put(orderJSON);
      finalOrder.setContent(msgJSON.toString());
      baseAgent.sendMessage(finalOrder);
      System.out.println(getAID().getName()+" sent order to transport agent");

    }
    
  }
  
	private class OrderDetailsReceiver extends CyclicBehaviour {
		private String orderProcessorServiceType;
		private AID orderProcessor = null;
		private MessageTemplate mt;

		protected void findOrderProcessor() {
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
		}

		public void action() {
//			findOrderProcessor();

			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("order"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msg = baseAgent.receive(mt);

			if (msg != null) {
				JSONObject obj = new JSONObject(msg.getContent());
				String orderID = obj.getString("guid");
				
				ArrayList<String> products = new ArrayList<String>();
				Iterator<String> keyItr = obj.getJSONObject("products").keys();
				while (keyItr.hasNext()) {
					products.add(keyItr.next());
				}
				
				OrderInfo orderInfo = new OrderInfo();
				orderInfo.custID = obj.getString("customerId");
				orderInfo.products = products;
				
				pendingOrderInfo.put(orderID, orderInfo);
				
			} else {
				block();
			}
		}
	}
  
}
