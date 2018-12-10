package org.commitment_issues.delivery_agents;

import java.util.ArrayList;

import org.json.JSONObject;
import org.yourteamname.agents.BaseAgent;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class DummyBakeAgent extends BaseAgent {
  private AID targetAgent;
  private ArrayList<ACLMessage> products = new ArrayList<ACLMessage>();
  protected void setup() {
    super.setup();
    System.out.println("Hello! BakingAgent "+ getAID().getName() +" is ready.");

    JSONObject group1 = new JSONObject();
    group1.put("Bread", 18);
    group1.put("Donut", 30);
    JSONObject g1 = new JSONObject();
    g1.put("products", group1);
    ACLMessage ms1 = new ACLMessage(ACLMessage.INFORM);
    ms1.setContent(g1.toString());
    products.add(ms1);
    ACLMessage ms2 = new ACLMessage(ACLMessage.INFORM);
    JSONObject group2 = new JSONObject();
    group2.put("Baguette", 10);
    group2.put("Breadstick", 2);
    group2.put("Arepa", 5);
    group2.put("Rosinenschnecke", 2);
    JSONObject g2 = new JSONObject();
    g2.put("products", group2);
    ms2.setContent(g2.toString());
    products.add(ms2);
    System.out.println("Hello! BakeAgent "+ getAID().getName() +" is ready.");
    while (findTargetAgent("cooling-rack")) {
      
    }
    addBehaviour(new Bake());
    addBehaviour(new TimeUpdater());
    
  }
  private class TimeUpdater extends CyclicBehaviour {
    public void action() {
      
      if (getAllowAction()) {
        finished();
      } 
    }
  }
  private boolean findTargetAgent(String service) {
    DFAgentDescription template = new DFAgentDescription();
    ServiceDescription sd = new ServiceDescription();
    sd.setType(service);
    template.addServices(sd);
    try {
      DFAgentDescription[] result = DFService.search(this, template);
      this.targetAgent = result[0].getName();
      return false;
     
    } catch (Exception fe) {
      //fe.printStackTrace();
      return true;
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
  private class Bake extends CyclicBehaviour{
    private int count;
    private ACLMessage send;
    public Bake() {
      this.count = 0;
    }
    @Override
    public void action() {
      if (count < products.size()) {
        send = products.get(count);
        send.setConversationId("bake");
        send.addReceiver(targetAgent);
        myAgent.send(send);
        System.out.println("Baking agent has sent orders to the cooling rack");
        count++;
      }
      else {block();}
      
    }
    
  }
}
