package org.commitment_issues.agents;

import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;



public class Clock extends Agent {
  private static AMSAgentDescription [] agents = null;
  public void setup() {
    System.out.println("Hello! Clock-agent "+getAID().getName()+" is ready each second is one hour.");
    
    try {
      SearchConstraints c = new SearchConstraints();
      c.setMaxResults ( new Long(-1) );
      agents = AMSService.search( this, new AMSAgentDescription (), c );
  }
  catch (Exception e) {
    
  }
    addBehaviour(new TickerBehaviour(this, 1000) {
      protected void onTick() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        
        for (int i=0; i<agents.length;i++){
          msg.addReceiver(Clock.agents[i].getName());
     }
        msg.setContent("Hour_has_passed");
        myAgent.send(msg);
      }
    });

  }
}
