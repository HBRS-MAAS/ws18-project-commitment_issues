package org.commitment_issues.delivery_agents;


import com.fxgraph.cells.CellType;
import com.fxgraph.cells.Graph;
import com.fxgraph.cells.Main;
import com.fxgraph.cells.Model;

import java.util.ArrayList;

import com.fxgraph.cells.*;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

@SuppressWarnings("serial")
public class Visualization extends Agent {
  Main m = new Main();
  Graph graph = new Graph();
  Model model = graph.getModel();

  
  public void setup() {
    System.out.println("Hello! Visualization-agent "+getAID().getName()+" is ready each second is one hour.");
    yellowPageRegister();
    addBehaviour(new GraphBuilder());
    addBehaviour(new JFXStart());
    
   }
  
  private void yellowPageRegister() {
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("transport-visualization");
    sd.setName("transport-visualization");
    dfd.addServices(sd);
    try {
      DFService.register(this, dfd);
    }
    catch (FIPAException fe) {
      fe.printStackTrace();
    }
  }
  
  private class JFXStart  extends OneShotBehaviour{

    @Override
    public void action(){
      // Create a new thread to start the application
      Thread thread = new Thread(() -> {
        
        try {
          m.main();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
    });
      thread.start();
    }
    
  }
  private class GraphBuilder extends CyclicBehaviour{

    @Override
    public void action() {
      
      

      
      
      
      
    }
    
  }
}