package org.commitment_issues.delivery_agents;


import com.fxgraph.cells.CellType;
import com.fxgraph.cells.Graph;
import com.fxgraph.cells.Main;
import com.fxgraph.cells.Model;
import com.fxgraph.cells.TextCell;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.maas.agents.BaseAgent;

import com.fxgraph.cells.*;

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
import javafx.scene.paint.Color;

@SuppressWarnings("serial")
public class GraphVisualizationAgent extends BaseAgent {
  
  protected Graph graph = new Graph();
  protected Main m = new Main(graph);
  protected Model model = graph.getModel();
  protected ArrayList <String> trucksID = new ArrayList<String>();// list of trucks ids for easy tracking
  protected ArrayList <Cell> textNodes = new ArrayList<Cell>(); // list of all text nodes to track them easily
  
  public void setup() {
    super.setup();
    System.out.println("Hello! Visualization-agent "+getAID().getName()+" is ready.");
//    yellowPageRegister();
    register("transport-visualization","transport-visualization");
    
//    addBehaviour(new GraphBuilder());
//    addBehaviour(new TruckTracker());
    addBehaviour(new JFXStart());
    
    
//    addBehaviour(new PositionUpdater());
    addBehaviour(new GraphConstructor());
    addBehaviour(new TimeUpdater());
   }
  
  private class TimeUpdater extends CyclicBehaviour {
    public void action() {
          if (getAllowAction()) {
            finished();
          } 
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
          e.printStackTrace();
        }
      });
      thread.start();
    }
    
  }
  private class GraphBuilder extends OneShotBehaviour {
    
    @Override
    public void action() {
      ACLMessage recieve = myAgent.receive();
      CellType shape;
      ((GraphVisualizationAgent)myAgent).graph.beginUpdate();
      ((GraphVisualizationAgent)myAgent).model.addCell("bakery-001", CellType.RECTANGLE);
      ((GraphVisualizationAgent)myAgent).model.addCell("customer-001", CellType.TRIANGLE);
      TextCell name = new TextCell("Name: bakery-001", "bakery-001", "bakery-001");
      TextCell name2 = new TextCell("Name: customer-001", "customer-001", "customer-001");

      ((GraphVisualizationAgent)myAgent).model.addCell(name);
      ((GraphVisualizationAgent)myAgent).model.addCell(name2);
      //((GraphVisualizationAgent)myAgent).model.addCell("bakery-002", CellType.RECTANGLE);
      ((GraphVisualizationAgent)myAgent).graph.endUpdate();
      ((GraphVisualizationAgent)myAgent).model.getAllCells().get(0).relocate( 10.0 ,100);
      ((GraphVisualizationAgent)myAgent).model.getAllCells().get(1).relocate(400 ,200);

      graph.beginUpdate();
      model.addEdge("bakery-001", "customer-001");
      graph.endUpdate();
      ((GraphVisualizationAgent)myAgent).model.getAllCells().get(2).relocate(10.0 ,100-25);
      ((GraphVisualizationAgent)myAgent).model.getAllCells().get(3).relocate(400.0 ,200-25);
      ((GraphVisualizationAgent)myAgent).m.setGraph(((GraphVisualizationAgent)myAgent).graph);
      
      if (recieve != null && recieve.getConversationId().equals("graph-visualization")) {
        
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
        
        //block();
      
      }

    }
    
  }
  
  
  private class GraphConstructor extends Behaviour {

    private MessageTemplate mt; 
    private boolean receivedDetails = false;
    
    public void action() {
        mt = MessageTemplate.MatchConversationId("graph-visualization");
        ACLMessage msg = myAgent.receive(mt);
        
        if (msg != null) {
          JSONObject wholeMsg = new JSONObject(msg.getContent());
          JSONArray nodes = wholeMsg.getJSONArray("nodes");
          JSONArray edges = wholeMsg.getJSONArray("edges");
          
          
          for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            
            String type = node.getString("type");
            JSONObject location = node.getJSONObject("location");
            float nodeX = location.getFloat("x")*(float)100.0;
            float nodeY = location.getFloat("y")*(float)100.0;
            
            String nodeID = node.getString("guid");
            
            graph.beginUpdate();
            
            if (type.equals("client")) {
              RectangleCell rectangle = new RectangleCell(nodeID);
              model.addCell(rectangle);
            }
            else if (type.equals("delivery")) {
              RectangleCell rectangle = new RectangleCell(nodeID);
              model.addCell(rectangle);
            }
            else if (type.equals("bakery")) {
              RectangleCell rectangle = new RectangleCell(nodeID);
              model.addCell(rectangle);
            }
            else {
              Ball ball = new Ball(nodeID);
              model.addCell(ball);
            }
            
            graph.endUpdate();
            model.getAllCells().get(i).relocate(nodeX, nodeY);
            
          }
          
          for (int k = 0;k < edges.length(); k++) {
            
            JSONObject edge = edges.getJSONObject(k);
            
            String startNode = edge.getString("source");
            String endNode = edge.getString("target");
            
            graph.beginUpdate();
            model.addEdge(startNode, endNode);
            graph.endUpdate();
          
          }
          
          m.setGraph(graph);
          receivedDetails = true;
        }
        else {
            block();
        }
    }
    public boolean done() {
        return receivedDetails;
    }
  }
  
  
  private class PositionUpdater extends CyclicBehaviour {
    double counter = 10.0;
    
    @Override
    public void action() {
      ACLMessage recieve = myAgent.receive();
      CellType shape = CellType.BALL;
      
//      if ((int)counter == 10) {
//        graph.beginUpdate();
//        model.addCell("hello", shape);
//        graph.endUpdate();
//      }
      
      model.getAllCells().get(0).relocate(counter ,100);
      
      counter += 10.0;
      
      try {
        Thread.sleep(100);
    } catch (InterruptedException e) {
        e.printStackTrace();
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
//      if (recieve != null && recieve.getConversationId().equals("truck-location")) {
//        String content = recieve.getContent();
//        
//        JSONObject truck = new JSONObject(content);
//        String truckID = truck.getString("TruckID");
//        String orderID = truck.getString("OrderID");
//        float truckXLoc = truck.getFloat("X")*(float)100.0;
//        float truckYLoc = truck.getFloat("Y")*(float)100.0;
//        float estimatedTime = truck.getFloat("EstimatedTime");
//        
//        if (trucksID.contains(truckID)) {
//          // If it is already there in the graph then search for it by id and relocate it
//          int orderIDindex = 0;
//          int currTimeIndex = 0;
//          int truckIndex = 0;
//          int counter = 0;
//          for (int i = 0; i < model.getAllCells().size(); i++) {
//            if (counter == 3) {
//              break;
//            }
//            if (model.getAllCells().get(i).getCellId().equals(truckID)) {
//              truckIndex = i;
//            }
//            
//            if (model.getAllCells().get(i).getCellId().equals(truckID+"Estimated Time: ")) {
//              currTimeIndex = i;
//            }
//            
//            if (model.getAllCells().get(i).getCellId().equals(truckID+"order: ")) {
//              orderIDindex = i;
//            }
//          }
//          model.getAllCells().get(truckIndex).relocate(truckXLoc, truckYLoc);
//          model.getAllCells().get(orderIDindex).relocate(truckXLoc-(float)50, truckYLoc);
//          ((TextCell)model.getAllCells().get(orderIDindex)).setContent("order: "+orderID);
//          model.getAllCells().get(currTimeIndex).relocate(truckXLoc-(float)75, truckYLoc);
//          ((TextCell)model.getAllCells().get(currTimeIndex)).setContent("Estimated Time: "+Float.toString(estimatedTime));
//          
//        }
//        else {
//          TextCell currOrderID = new TextCell(truckID+"order: ", truckID, "order: "+ orderID);
//          TextCell currTime = new TextCell(truckID+"Estimated Time: ", truckID, "Estimated Time: "+Float.toString(estimatedTime));
//          textNodes.add(currOrderID);
//          textNodes.add(currTime);
//          graph.beginUpdate();
//          model.addCell(truckID, shape);
//          graph.endUpdate();
//          
//          model.getAllCells().get(model.getAllCells().size()-1)
//          .relocate(truckXLoc, truckYLoc);
//          graph.beginUpdate();
//          model.addCell(currOrderID);
//          graph.endUpdate();
//          model.getAllCells().get(model.getAllCells().size()-1)
//          .relocate(truckXLoc, truckYLoc-(float)50);
//          graph.beginUpdate();
//          model.addCell(currOrderID);
//          graph.endUpdate();
//          model.getAllCells().get(model.getAllCells().size()-1)
//          .relocate(truckXLoc, truckYLoc-(float)75);
//        }
        
      
//      }  
      
    }
    
  }
  
}