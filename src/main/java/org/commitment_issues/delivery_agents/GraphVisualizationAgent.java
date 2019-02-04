package org.commitment_issues.delivery_agents;


import com.fxgraph.cells.Graph;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

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
<<<<<<< HEAD
=======
import javafx.scene.paint.Color;
>>>>>>> 7b66a30948ad50d7f01da63e21fc6c9e222996a7

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
    register("transport-visualization","transport-visualization");
    
    // Dummy node needed for correct visualization behavior
    addNode(NodeType.SIMPLE, "dummy", 400, 200, ".");

    addBehaviour(new GraphConstructor());
    addBehaviour(new JFXStart());
<<<<<<< HEAD
    
    
    addBehaviour(new PositionUpdater());
    addBehaviour(new TruckPositionUpdater());
=======

>>>>>>> 7b66a30948ad50d7f01da63e21fc6c9e222996a7
    addBehaviour(new TimeUpdater());
   }
  
  private class TimeUpdater extends CyclicBehaviour {
    public void action() {
          if (getAllowAction()) {
            finished();
          } 
        }
  }
  
  private void updateNodePosition(String cellID, double x, double y, double margin) {
	  List<Cell> allCells = model.getAllCells();
	  int cellsUpdated = 0;
	  
	  for (Cell cell: allCells) {
		  if (cell.getCellId().equals(cellID)) {
		      cell.relocate(x, y); 
			  cellsUpdated += 1;
		  }
		  else if (cell.getCellId().equals(cellID + "_label")) {
			  cell.relocate(x, y - margin);
			  cellsUpdated += 1;
		  }
		  
		  if (cellsUpdated >= 2) {
			  break;
		  }
	  }
  }
  
	private enum NodeType {
		INVALID,
		SIMPLE,
		BAKERY,
		CUSTOMER,
		TRANSPORT_COMPANY,
		TRUCK
	}
  
	private void addNode(NodeType type, String id, double posX, double posY, String label) {
		Cell node = null;
		TextCell labelNode = null;
		
		switch (type) {
		case SIMPLE: {
			node = new Ball(id);
			((Ball)node).setColor(Color.BLACK);
			break;
		}
		case BAKERY: {
			node = new RectangleCell(id);
			((RectangleCell)node).setColor(Color.BROWN);
			break;
		}
		case CUSTOMER: {
			node = new RectangleCell(id);
			((RectangleCell)node).setColor(Color.YELLOW);
			break;
		}
		case TRANSPORT_COMPANY: {
			node = new RectangleCell(id);
			((RectangleCell)node).setColor(Color.AQUA);
			break;
		}
		case TRUCK: {
			node = new TriangleCell(id);
			((TriangleCell)node).setColor(Color.RED);
			break;
		}
		default:
			break;
		}
		
		if (label != null && label.length() > 0) {
			labelNode = new TextCell(id+"_label", id, label);
		}
		
	    graph.beginUpdate();
	    if (node != null)
	    	model.addCell(node);
	    if (labelNode != null)
	    	model.addCell(labelNode);
	    graph.endUpdate();		
	    

	    updateNodePosition(id, posX, posY, 25);
	    
	    m.setGraph(graph);
	}
	
	private void addEdge(String cellID1, String cellID2) {
		graph.beginUpdate();
		model.addEdge(cellID1, cellID2);
		graph.endUpdate();
	}
	
	private boolean nodeAlreadyInGraph(String cellID) {
		boolean nodeExists = false;

		List<Cell> allCells = model.getAllCells();

		for (Cell cell : allCells) {
			if (cell.getCellId().equals(cellID)) {
				nodeExists = true;
				break;
			}
		}
		return nodeExists;
	}
  
  private class JFXStart  extends OneShotBehaviour{

    @Override
    public void action(){
      // Create a new thread to start the application
      try {
        Thread.sleep(3000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
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
      addNode(NodeType.BAKERY, "bakery-001", 10, 100, "bakery-001");
      addNode(NodeType.CUSTOMER, "customer-001", 400, 200, "customer-001");
      addNode(NodeType.SIMPLE, "simple-1", 600, 200, null);
      addNode(NodeType.TRUCK, "Truck-001", 800, 200, "Truck-001");
      addNode(NodeType.TRANSPORT_COMPANY, "Transport-Company-001", 1000, 200, "Transport-Company-001");
      addEdge("bakery-001", "customer-001");
      addEdge("Transport-Company-001", "Truck-001");
      
      m.setGraph(graph);
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
            
            if (type.equals("client")) {
              String name = "C-" + node.getString("company").split("-")[1];
              addNode(NodeType.CUSTOMER, nodeID, nodeX, nodeY, name);
            }
            else if (type.equals("delivery")) {
              String name = "DC-" + node.getString("company").split("-")[2];
              addNode(NodeType.TRANSPORT_COMPANY, nodeID, nodeX, nodeY, name);
            }
            else if (type.equals("bakery")) {
              String name = "B-" + node.getString("company").split("-")[1];
              addNode(NodeType.BAKERY, nodeID, nodeX, nodeY, name);
            }
            else {
              addNode(NodeType.SIMPLE, nodeID, nodeX, nodeY, "");
            }
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
      CellType shape = CellType.BALL;
      
      updateNodePosition("bakery-001", counter, 100.0, 25);
      
//      counter += 10.0;
      
      try {
        Thread.sleep(100);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    }
  }
  
	private class TruckPositionUpdater extends CyclicBehaviour {
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("TruckPosUpdate");
			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) {
				System.out.println("++++++++++ Visualization recived msg: \n" + msg.getContent() );
				JSONObject jsonObj = new JSONObject(msg.getContent());
				String truckID = jsonObj.getString("id");
				float x = jsonObj.getFloat("x");
				float y = jsonObj.getFloat("y");
				
				if (nodeAlreadyInGraph(truckID)) {
					System.out.println("Truck node already exists. Updating its position");
					updateNodePosition(truckID, x, y, 25);	
				}
				else {
					System.out.println("Truck node does not exists. Creating a new node");
					addNode(NodeType.TRUCK, truckID, x, y, truckID);	
				}
				
				System.out.println("****************Action Complete..");
			}
		}
	}
  
}