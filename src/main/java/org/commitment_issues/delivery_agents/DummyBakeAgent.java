package org.commitment_issues.delivery_agents;

import java.util.ArrayList;

import org.json.JSONArray;
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
  AID targetAgent;
  private ArrayList<ACLMessage> products = new ArrayList<ACLMessage>();
  protected void setup() {
    super.setup();
    System.out.println("Hello! BakingAgent "+ getAID().getName() +" is ready.");

    JSONArray group1 = new JSONArray();
    JSONObject p1 = new JSONObject();
    p1.put("Name", "Bread");
    p1.put("Quantity", 18);
    group1.put(p1);
    JSONObject p2 = new JSONObject();
    p2.put("Name", "Donut");
    p2.put("Quantity", 30);
    group1.put(p1);
    group1.put(p2);
    ACLMessage ms1 = new ACLMessage(ACLMessage.INFORM);
    ms1.setContent(group1.toString());
    products.add(ms1);
    ACLMessage ms2 = new ACLMessage(ACLMessage.INFORM);
    JSONArray group2 = new JSONArray();
    JSONObject p3 = new JSONObject();
    p3.put("Name", "Baguette");
    p3.put("Quantity", 10);
    group2.put(p3);
    JSONObject p4 = new JSONObject();
    p1.put("Name", "Breadstick");
    p1.put("Quantity", 2);
    group2.put(p4);
    JSONObject p5 = new JSONObject();
    p5.put("Name", "Arepa");
    p5.put("Quantity", 5);
    group2.put(p5);
    JSONObject p6 = new JSONObject();
    p6.put("Name", "Rosinenschnecke");
    p6.put("Quantity", 2);
    group2.put(p6);
    ms2.setContent(group2.toString());
    System.out.println("Hello! BakeAgent "+ getAID().getName() +" is ready.");
    findTargetAgent("cooling-rack");
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
  private void findTargetAgent(String service) {
    DFAgentDescription template = new DFAgentDescription();
    ServiceDescription sd = new ServiceDescription();
    sd.setType(service);
    template.addServices(sd);
    try {
      DFAgentDescription[] result = DFService.search(this, template);
      this.targetAgent = result[0].getName();
     
    } catch (Exception fe) {
      //fe.printStackTrace();
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
    int count;
    ACLMessage send;
    public Bake() {
      this.count = 0;
    }
    @Override
    public void action() {
      if (count < products.size()) {
        send = products.get(count);
        send.setConversationId("baked");
        send.addReceiver(targetAgent);
        myAgent.send(send);
        System.out.println("Baking agent has sent orders to the cooling rack");
        count++;
      }
      else {block();}
      
    }
    
  }
}
