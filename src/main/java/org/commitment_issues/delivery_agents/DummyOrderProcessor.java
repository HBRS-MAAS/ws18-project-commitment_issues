package org.commitment_issues.delivery_agents;

import java.io.File;


import org.commitment_issues.agents.CustomerAgent;
import org.json.JSONArray;
import org.maas.agents.BaseAgent;

import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class DummyOrderProcessor extends BaseAgent {
  
  private JSONArray orders;
  
  protected void setup() {
	super.setup();
    System.out.println("Hello! DummyOrderProcessor-agent "+getAID().getName()+" is ready.");
    orders = parseOrders();
    register("order-processor", "order-processor");
    addBehaviour(new OrderProcessorServer());
    addBehaviour(new TimeUpdater());

  }
  private class TimeUpdater extends CyclicBehaviour {
    public void action() {
      if (getAllowAction()) {
        finished();
      } 
    }
  }
  protected  JSONArray parseOrders() {
    File relativePath = new File("src/main/resources/config/small/orderprocessor.json");
    String read = CustomerAgent.readFileAsString(relativePath.getAbsolutePath());
    JSONArray ordersJSON = new JSONArray(read);
    return ordersJSON;

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
  
  private class OrderProcessorServer extends CyclicBehaviour{

    @Override
    public void action() {
     MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
     ACLMessage request = myAgent.receive(mt);
     if (request != null) {
       String orderID = request.getContent();
       ACLMessage reply = request.createReply();
       for (int i = 0; i < orders.length();i++) {
         if (orders.getJSONObject(i).getString("OrderID").equals(orderID)) {
           reply.setContent(orders.getJSONObject(i).toString());
           System.out.println("Order processor has sent order"+orders.getJSONObject(i).toString());
           break;
         }
       }
       myAgent.send(reply);
     }
     else {
       block();
     }
      
    }
    
  }
}
