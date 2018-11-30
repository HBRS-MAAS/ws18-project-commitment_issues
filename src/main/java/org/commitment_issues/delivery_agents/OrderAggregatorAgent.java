package org.commitment_issues.delivery_agents;

import java.util.ArrayList;


import org.json.JSONArray;
import org.json.JSONObject;
import org.yourteamname.agents.BaseAgent;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
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

  private AID transportAgent = null;
  protected void setup() {
    
    this.register("order-aggregator","order-aggregator");
    while(this.transportAgent == null) {
      findTransportAgent();
    }
    addBehaviour(new LoadingBayParser());
    addBehaviour(new TimeUpdater());

  }
//  protected  JSONArray parseOrders() {
//    File relativePath = new File("src/main/resources/config/small/bakeries.json");
//    String read = CustomerAgent.readFileAsString(relativePath.getAbsolutePath());
//    JSONArray ordersJSON = new JSONArray(read);
//    for (int i = 0; i < ordersJSON.length();i ++) {
//      
//    }
//
//  }
  protected void register(String type, String name){
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType(type);
    sd.setName(name);
    dfd.addServices(sd);
    try {
        DFService.register(this, dfd);
    }
    catch (FIPAException fe) {
        fe.printStackTrace();
    }
}
  protected void takeDown() {
    deRegister();
    
    System.out.println(getAID().getLocalName() + ": Terminating.");
  }
  protected void deRegister() {
    try {
          DFService.deregister(this);
      }
      catch (FIPAException fe) {
          fe.printStackTrace();
      }
  }
  private void findTransportAgent() {
    DFAgentDescription template = new DFAgentDescription();
    ServiceDescription sd = new ServiceDescription();
    sd.setType("transport-agent");
    template.addServices(sd);
    try {
      DFAgentDescription[] result = DFService.search(this, template);
      this.transportAgent = result[0].getName();
     
  } catch (FIPAException fe) {
      fe.printStackTrace();
  }
    
    
  }
  private class TimeUpdater extends CyclicBehaviour {
    public void action() {
      MessageTemplate mt = MessageTemplate.MatchPerformative(55);
      ACLMessage msg = baseAgent.receive(mt);
      if (msg != null) {
        finished();
      } else {
        block();
      }
    }
  }
  private class LoadingBayParser extends CyclicBehaviour{

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.MatchConversationId("packaged-orders");
      ACLMessage msg = myAgent.receive(mt);
      
      
      
      if (msg != null) {
        JSONObject recieved = new JSONObject(msg.getContent());
        JSONArray boxesJSON = recieved.getJSONArray("Boxes");
        Order order = new Order();
        
        if (((OrderAggregatorAgent)myAgent).orders.size()==0) {
          order.setOrderID(recieved.getString("OrderID"));
          System.out.println(order.getOrderID());
          
        }for (int k = 0; k < ((OrderAggregatorAgent)myAgent).orders.size();k++) {
          if (((OrderAggregatorAgent)myAgent).orders.size()==0) {
            order.setOrderID(recieved.getString("OrderID"));
            System.out.println(order.getOrderID());
            break;
          }
          else {
            System.out.println((((OrderAggregatorAgent)myAgent).orders.get(k).getOrderID()));
          if ((((OrderAggregatorAgent)myAgent).orders.get(k).getOrderID()).equals(recieved.getString("OrderID"))) {
            order = orders.get(k);
            
            break;
          }
          else {
            order.setOrderID(recieved.getString("OrderID"));
            
          }}
        }
        
        
        for (int i = 0; i < boxesJSON.length(); i++) {
          JSONObject boxJSON = boxesJSON.getJSONObject(i);
          Box box = new Box();
          box.setBoxID(boxJSON.getString("BoxID"));
          box.setProductType(boxJSON.getString("ProductType"));
          box.setQuantity(boxJSON.getInt("Quantity"));
          order.addBoxes(box);
        }
        
        
        ((OrderAggregatorAgent)myAgent).orders.add(order);
        System.out.println(getAID().getName()+"recieved an order");
        myAgent.addBehaviour(new CheckOrder(order));
        
      }
      
    }
    
  }
  
  private class CheckOrder extends Behaviour {
    private int state = 0;
    ACLMessage orderDetails = null;
    private MessageTemplate mt;
    Order testOrder;
    public CheckOrder(Order checkOrder) {
      this.testOrder = checkOrder;
    }
    @Override
    public void action() {
      switch(state) {
    case 0://Search for the order processor and send it a request
      DFAgentDescription template = new DFAgentDescription();
      ServiceDescription sd = new ServiceDescription();
      sd.setType("order-processor");// assuming that the order processor will provide a service with this name
      template.addServices(sd);
      
      try {
        DFAgentDescription[] result = DFService.search(myAgent, template);
        AID orderProcessor = result[0].getName();
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.setConversationId("order-details"+testOrder.getOrderID());
        request.setContent(testOrder.getOrderID());
        request.addReceiver(orderProcessor);
        myAgent.send(request);
        state++;
        mt = MessageTemplate.MatchConversationId("order-details"+testOrder.getOrderID());

    } catch (FIPAException fe) {
        fe.printStackTrace();
    }
    case 1:// receive the order details from the order processor
      if(orderDetails == null) {
        orderDetails = myAgent.receive(mt);
      }
      
      if (orderDetails != null) {
        
        ArrayList<String> productTypes = new ArrayList<String>();
        JSONObject orderDetailsJSON = new JSONObject(orderDetails.getContent());
        JSONArray productsJSON = orderDetailsJSON.getJSONArray("Products");
        int productCount = 0;
        for (int i = 0; i < productsJSON.length(); i++) {
          if (((JSONObject)productsJSON.getJSONObject(i)).getInt(((JSONObject)productsJSON.getJSONObject(i)).names().getString(0)) > 0) {
            productCount++;
          }
        }
      
        for (int k = 0; k < this.testOrder.getBoxes().size(); k++) {
          if(k == 0) {
            productTypes.add(this.testOrder.getBoxes().get(k).getProductType());
          }
          else {
            if (!productTypes.contains(this.testOrder.getBoxes().get(k).getProductType())) {
              productTypes.add(this.testOrder.getBoxes().get(k).getProductType());

            }
          }
        }
        if (productCount == productTypes.size()) {
          myAgent.addBehaviour(new SendOrderToTransport(testOrder));
        }
        state++;
      }
    default: break;
    }
      
    }
    @Override
    public boolean done() {
      
      if (this.state == 2) {
        return true;
      }
      else {
        return false;
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
      orderJSON.put("CustId", "customer-001");
      orderJSON.put("BackId", "bakery-001");
      orderJSON.put("OrderId",order.getOrderID());
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
      myAgent.send(finalOrder);
      System.out.println(getAID().getName()+"sent order to transport agent");

    }
    
  }
  
}
