package org.commitment_issues.delivery_agents;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class OrderAggregatorAgent extends Agent {
  private ArrayList<Order> orders = new ArrayList<Order>(); //list of all the orders
  private AID transportAgent = null;
  protected void setup() {
    addBehaviour(new LoadingBayParser());
    this.register("order-aggregator","order-aggregator");
    while(this.transportAgent == null) {
      findTransportAgent();
    }
  }
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
  private class LoadingBayParser extends CyclicBehaviour{

    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.MatchConversationId("packaged-orders");
      ACLMessage msg = myAgent.receive(mt);
      if (msg != null) {
        JSONObject recieved = new JSONObject(msg.getContent());
        JSONArray boxesJSON = recieved.getJSONArray("Boxes");
        Order order = new Order();
        for (int k = 0; k < orders.size();k++) {
          if (orders.get(k).getOrderID().equals(recieved.getString("OrderID"))) {
            order = orders.get(k);
            break;
          }
          else {
            order.setOrderID(recieved.getString("OrderID"));
            ((OrderAggregatorAgent)myAgent).orders.add(order);
          }
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
        addBehaviour(new CheckOrderComplete(order));
      }
      
    }
    
  }
  
  private class CheckOrderComplete extends OneShotBehaviour {
    // This class is now implemented this way for the purpose of simulating the scenario
    // The actual implementation is partially done in the commented class below this class
    // It is not fully implemented because the order processor is not known yet
    private Order order; 
    private Order fullOrder;
    public CheckOrderComplete(Order order) {
      this.order = order;
    }

    @Override
    public void action() {
      if(((OrderAggregatorAgent)myAgent).orders.size()==2) {
       for(int i = 0; i< ((OrderAggregatorAgent)myAgent).orders.size();i++) {
         Order partialOrder = ((OrderAggregatorAgent)myAgent).orders.get(i);
         for (int k = 0; k < partialOrder.getBoxes().size(); k++) {
           fullOrder.addBoxes(partialOrder.getBoxes().get(k));
         }
       }
       fullOrder.setOrderID(order.getOrderID());
       fullOrder.setDestination(order.getDestination());
       fullOrder.setLocation(order.getLocation());
       myAgent.addBehaviour(new SendOrderToTransport(fullOrder));
    } 
    
  }
  
//  private class CheckOrderComplete extends CyclicBehaviour {
//    private int state = 0;
//    private Order order;
//    //private Order receivedOrder;
//    ACLMessage orderDetails = null;
//    private MessageTemplate mt;
//    public CheckOrderComplete(Order order) {
//      this.order = order;
//    }
//    public void action() {
//      //Searching for the order processor to ask it about the order
//      
//      switch(state) {
//      case 0://Search for the order processor and send it a request
//        DFAgentDescription template = new DFAgentDescription();
//        ServiceDescription sd = new ServiceDescription();
//        sd.setType("order-processor");// assuming that the order processor will provide a service with this name
//        template.addServices(sd);
//        
//        try {
//          DFAgentDescription[] result = DFService.search(myAgent, template);
//          AID orderProcessor = result[0].getName();
//          ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
//          request.setConversationId("order-details"+order.getOrderID());
//          request.setContent(order.getOrderID());
//          request.addReceiver(orderProcessor);
//          myAgent.send(request);
//          state++;
//          mt = MessageTemplate.MatchConversationId("order-details"+order.getOrderID());
//
//      } catch (FIPAException fe) {
//          fe.printStackTrace();
//      }
//      case 1:// receive the order details from the order processor
//        if(orderDetails == null) {
//          orderDetails = myAgent.receive(mt);
//          // assume it is an array of boxes
//          if (orderDetails != null) {
//           //JSONArray boxesOfOriginal =  new JSONArray(orderDetails.getContent());
//           
//          }
//        }
//        else {
//          state++;
//        }
//      }
//      
//      
//    }
//    
//  }
  
  private class SendOrderToTransport extends OneShotBehaviour {
    private Order order;
    ACLMessage finalOrder = new ACLMessage(ACLMessage.INFORM);
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
    }
    
  }
  }
}