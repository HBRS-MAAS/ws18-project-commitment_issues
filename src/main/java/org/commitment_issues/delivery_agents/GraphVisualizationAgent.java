package org.commitment_issues.delivery_agents;


import com.fxgraph.cells.CellType;
import com.fxgraph.cells.Graph;
import com.fxgraph.cells.Main;
import com.fxgraph.cells.Model;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fxgraph.cells.*;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class GraphVisualizationAgent extends Agent {
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
    CellType shape;
    @Override
    public void action() {
      ACLMessage recieve = myAgent.receive();
      
      if (recieve != null && recieve.getConversationId().equals("initial-state")) {
        
        JSONObject wholeMsg = new JSONObject(recieve);
        
        JSONArray nodes = wholeMsg.getJSONArray("Nodes");
        JSONArray edges = wholeMsg.getJSONArray("Edges");
        
        for (int i = 0; i < nodes.length(); i++) {
          
          JSONObject node = nodes.getJSONObject(i);
          
          int type = node.getInt("Type");
          // i for bakeries and 0 for customers
          JSONObject location = node.getJSONObject("Location");
          float nodeX = location.getFloat("X")*(float)10.0;
          float nodeY = location.getFloat("Y")*(float)10.0;
          
          String nodeID = node.getString("NodeID");
          
          if (type == 0) {
            shape = CellType.RECTANGLE;
          }
          else{
            shape = CellType.TRIANGLE;
          }
          
          graph.beginUpdate();
          model.addCell(nodeID, shape);
          graph.endUpdate();
          
          model.getAllCells().get(i).relocate(nodeX, nodeY);
        }
        
        for (int k = 0;k < edges.length(); k++) {
          
          JSONObject edge = edges.getJSONObject(k);
          
          String startNode = edge.getString("startNodeID");
          String endNode = edge.getString("endNodeID");
          
          graph.beginUpdate();
          model.addEdge(startNode, endNode);
          graph.endUpdate();
        
        }
        
      m.setGraph(graph);
      }
      else {
        
        block();
      
      }

    }
    
  }
}