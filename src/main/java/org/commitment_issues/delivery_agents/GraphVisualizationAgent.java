package org.commitment_issues.delivery_agents;


import com.fxgraph.cells.CellType;
import com.fxgraph.cells.Graph;
import com.fxgraph.cells.Main;
import com.fxgraph.cells.Model;
import com.fxgraph.cells.TextCell;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.yourteamname.agents.BaseAgent;

import com.fxgraph.cells.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class GraphVisualizationAgent extends BaseAgent {
  
  protected Graph graph = new Graph();
  protected Model model = graph.getModel();
  protected Main m;
  protected ArrayList <String> trucksID = new ArrayList<String>();// list of trucks ids for easy tracking
  protected ArrayList <Cell> textNodes = new ArrayList<Cell>(); // list of all text nodes to track them easily
  
  public void setup() {
    
    System.out.println("Hello! Visualization-agent "+getAID().getName()+" is ready each second is one hour.");
    yellowPageRegister();
    
    addBehaviour(new GraphBuilder());
    
    addBehaviour(new TruckTracker());
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
          m = new Main(graph);
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
      ACLMessage recieve = myAgent.receive();
      CellType shape;
      if (recieve != null && recieve.getConversationId().equals("initial-state")) {
        
        JSONObject wholeMsg = new JSONObject(recieve);
        
        JSONArray nodes = wholeMsg.getJSONArray("Nodes");
        JSONArray edges = wholeMsg.getJSONArray("Edges");
        
        for (int i = 0; i < nodes.length(); i++) {
          
          JSONObject node = nodes.getJSONObject(i);
          
          int type = node.getInt("Type");
          // i for bakeries and 0 for customers
          JSONObject location = node.getJSONObject("Location");
          float nodeX = location.getFloat("X")*(float)100.0;
          float nodeY = location.getFloat("Y")*(float)100.0;
          
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
  
  private class TruckTracker extends CyclicBehaviour{

    @Override
    public void action() {
      ACLMessage recieve = myAgent.receive();
      CellType shape = CellType.BALL;
      graph.beginUpdate();
      model.addCell("t", shape);
      graph.endUpdate();
      if (recieve != null && recieve.getConversationId().equals("truck-location")) {
        String content = recieve.getContent();
        
        JSONObject truck = new JSONObject(content);
        String truckID = truck.getString("TruckID");
        String orderID = truck.getString("OrderID");
        float truckXLoc = truck.getFloat("X")*(float)100.0;
        float truckYLoc = truck.getFloat("Y")*(float)100.0;
        float estimatedTime = truck.getFloat("EstimatedTime");
        
        if (trucksID.contains(truckID)) {
          // If it is already there in the graph then search for it by id and relocate it
          int orderIDindex = 0;
          int currTimeIndex = 0;
          int truckIndex = 0;
          int counter = 0;
          for (int i = 0; i < model.getAllCells().size(); i++) {
            if (counter == 3) {
              break;
            }
            if (model.getAllCells().get(i).getCellId().equals(truckID)) {
              truckIndex = i;
            }
            
            if (model.getAllCells().get(i).getCellId().equals(truckID+"Estimated Time: ")) {
              currTimeIndex = i;
            }
            
            if (model.getAllCells().get(i).getCellId().equals(truckID+"order: ")) {
              orderIDindex = i;
            }
          }
          model.getAllCells().get(truckIndex).relocate(truckXLoc, truckYLoc);
          model.getAllCells().get(orderIDindex).relocate(truckXLoc-(float)50, truckYLoc);
          ((TextCell)model.getAllCells().get(orderIDindex)).setContent("order: "+orderID);
          model.getAllCells().get(currTimeIndex).relocate(truckXLoc-(float)75, truckYLoc);
          ((TextCell)model.getAllCells().get(currTimeIndex)).setContent("Estimated Time: "+Float.toString(estimatedTime));
          
        }
        else {
          TextCell currOrderID = new TextCell(truckID+"order: ", truckID, "order: "+ orderID);
          TextCell currTime = new TextCell(truckID+"Estimated Time: ", truckID, "Estimated Time: "+Float.toString(estimatedTime));
          textNodes.add(currOrderID);
          textNodes.add(currTime);
          graph.beginUpdate();
          model.addCell(truckID, shape);
          graph.endUpdate();
          
          model.getAllCells().get(model.getAllCells().size()-1)
          .relocate(truckXLoc, truckYLoc);
          graph.beginUpdate();
          model.addCell(currOrderID);
          graph.endUpdate();
          model.getAllCells().get(model.getAllCells().size()-1)
          .relocate(truckXLoc, truckYLoc-(float)50);
          graph.beginUpdate();
          model.addCell(currOrderID);
          graph.endUpdate();
          model.getAllCells().get(model.getAllCells().size()-1)
          .relocate(truckXLoc, truckYLoc-(float)75);
        }
        
      
      }  
      
    }
    
  }
  
}