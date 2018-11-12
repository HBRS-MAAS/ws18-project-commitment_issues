package org.commitment_issues.agents;

import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;



public class Clock extends Agent {
  private static AMSAgentDescription [] agents = null;
  private int hours = 0;
  private int days = 0;
  public void setup() {
    System.out.println("Hello! Clock-agent "+getAID().getName()+" is ready each second is one hour.");
//    try {
//      Thread.sleep(3000);
//    } catch (InterruptedException e) {
//      //e.printStackTrace();
//    }
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
        hours++;
        if (hours == 24){
          days++;
          hours = 0;
        }
        for (int i=0; i<agents.length;i++){
          msg.addReceiver(Clock.agents[i].getName());
     }
        msg.setContent(Clock.showTime(hours, days));
        msg.setConversationId("Time-Update");
        myAgent.send(msg);
      }
    });

  }
  protected static String showTime(int h, int d) {
    String curTime = Integer.toString(h)+"";
    if (curTime.length() == 1) {
      curTime = "0" + curTime;
    }
    curTime = "." + curTime;
    String hh = Integer.toString(d);
    while (hh.length() < 3) {
      hh = "0" + hh;
    }
    curTime = hh + curTime;
    
  return curTime;
  }
}
