//package org.commitment_issues.deliveryAgents;
package org.commitment_issues.delivery_agents;

import org.yourteamname.agents.BaseAgent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.*;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class StreetNetworkAgent extends Agent {
	public JSONArray nodesJSONArray = new JSONArray();
	public JSONArray linksJSONArray = new JSONArray();
			
	public List<Vertex> nodes = new ArrayList<Vertex>();
	public List<Edge> edges = new ArrayList<Edge>();
	public Graph graph;
    public DijkstraAlgorithm dijkstra;

	protected void setup() {
		System.out.println("Hello! StreetNetwork-agent "+getAID().getName()+" is ready.");
		
		register("street-network", "street-network");
		
		parseStreetNetworkData(getStreetNetworkData());
		
		// Uncomment this behavior for graph visualization integration
//		addBehaviour(new GraphVisualizerServer());
		addBehaviour(new TimeToDeliveryServer());
		addBehaviour(new PathServer());
	}

	  /* This function registers the agent to yellow pages
	   * Call this in `setup()` function
	   */
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
	  
	  /* This function removes the agent from yellow pages
	   * Call this in `doDelete()` function
	   */
	  protected void deRegister() {
	  	try {
	          DFService.deregister(this);
	      }
	      catch (FIPAException fe) {
	          fe.printStackTrace();
	      }
	  }
	  
		protected void takeDown() {
			deRegister();
			System.out.println(getAID().getLocalName() + ": Terminating.");
		}
		
	protected String getStreetNetworkData() {
		File fileRelative = new File("src/main/resources/config/sample/street-network.json");
		String data = ""; 
	    try {
			data = new String(Files.readAllBytes(Paths.get(fileRelative.getAbsolutePath())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return data;
	}
	
	// TODO: This behavior still requires the identity of the visualization agent
	private class GraphVisualizerServer extends CyclicBehaviour {
		private MessageTemplate mt;

		public void action() {			
			ACLMessage SNVisualizationInfo = new ACLMessage(ACLMessage.INFORM);
			// TODO:
			AID receivingAgent = null;
			String messageContent = createVisualizerMessage();
			
			SNVisualizationInfo.addReceiver(receivingAgent);
			SNVisualizationInfo.setContent(messageContent);
			SNVisualizationInfo.setConversationId("graph-visualization");
			
			myAgent.send(SNVisualizationInfo);
		}
	}
	
	private class TimeToDeliveryServer extends CyclicBehaviour {
		private MessageTemplate mt;

		public void action() {
			
//			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
//			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("TimeQuery"),
//					MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("TimeQuery"),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			ACLMessage msg = myAgent.receive(mt);
								
			
			if (msg != null) {
				// +++
				System.out.println("["+getAID().getLocalName()+"]: Received time request from "+msg.getSender().getLocalName());
				
				String truckMessageContent = msg.getContent();
				ACLMessage reply = msg.createReply();
				
				double time = getPathTime(getShortestPath(truckMessageContent));


				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(String.valueOf(time));
				myAgent.send(reply);
				
				// +++
				System.out.println("["+getAID().getLocalName()+"]: Returned journey time for "+msg.getSender().getLocalName()+" is "+time);
				
			}

			else {
//				System.out.println("["+getAID().getLocalName()+"]: Waiting for time request messages.");
				block();
			}
		}
	}
	
	
	private class PathServer extends CyclicBehaviour {
		private MessageTemplate mt;

		public void action() {
			
//			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
//			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("PathQuery"),
//					MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
//			msg = myAgent.receive(mt);
			
			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("PathQuery"),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			ACLMessage msg = myAgent.receive(mt);
			
					
			if (msg != null) {
				// +++
				System.out.println("["+getAID().getLocalName()+"]: Received path request from "+msg.getSender().getLocalName());
				
				String truckMessageContent = msg.getContent();
				ACLMessage reply = msg.createReply();
				
				String JSONPath = getJSONPath(getShortestPath(truckMessageContent));


				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(String.valueOf(JSONPath));
				myAgent.send(reply);
				
				// +++
				System.out.println("["+getAID().getLocalName()+"]: Returned journey path for "+msg.getSender().getLocalName()+":\n"+JSONPath);
				
			}

			else {
				// +++
//				System.out.println("["+getAID().getLocalName()+"]: Waiting for path requests.");
				block();
			}
		}
	}
	
	protected String createVisualizerMessage() {
		JSONObject JSONVisData = new JSONObject();
		JSONArray JSONVisNodes = new JSONArray();
		JSONArray JSONVisLinks = new JSONArray();
		
		for (int i = 0; i < nodesJSONArray.length(); i++) {
			JSONObject JSONVisNodeInfo = new JSONObject();
			JSONVisNodeInfo.put("guid", nodesJSONArray.getJSONObject(i).getString("guid"));
			int type = 0;
			if (nodesJSONArray.getJSONObject(i).getString("type").equals("bakery")) {
				type = 1;
			}
			JSONVisNodeInfo.put("type",type);
			JSONVisNodeInfo.put("location", nodesJSONArray.getJSONObject(i).getJSONObject("location"));
			
			JSONVisNodes.put(JSONVisNodeInfo);
		}
		
		for (int i = 0; i < linksJSONArray.length(); i++) {
			JSONObject JSONVisLinkInfo = new JSONObject();
			JSONVisLinkInfo.put("source", linksJSONArray.getJSONObject(i).getString("source"));
			JSONVisLinkInfo.put("target", linksJSONArray.getJSONObject(i).getString("target"));
			
			JSONVisLinks.put(JSONVisLinkInfo);
		}
		
		JSONVisData.put("nodes", JSONVisNodes);		
		JSONVisData.put("edges", JSONVisLinks);
		
		return JSONVisData.toString();
	}
	
	protected void parseStreetNetworkData(String streetNetworkData) {
		JSONObject JSONSNData = new JSONObject(streetNetworkData);
		
		nodesJSONArray = JSONSNData.getJSONArray("nodes");
		linksJSONArray = JSONSNData.getJSONArray("links");
		
		int numNodes = nodesJSONArray.length();
		int numLinks = linksJSONArray.length();
		
		for (int i = 0; i < numNodes; i++) {
			JSONObject nodeInfo = nodesJSONArray.getJSONObject(i);
//			String nodeName = nodeInfo.getString("name");
			String nodeID = nodeInfo.getString("guid");
            Vertex location = new Vertex(nodeID, nodeID);
            nodes.add(location);
        }
		
		for (int i = 0; i < numLinks; i++) {
			JSONObject linkInfo = linksJSONArray.getJSONObject(i);
			String sourceNodeID = linkInfo.getString("source");
			String targetNodeID = linkInfo.getString("target");
			Vertex sourceVertex = new Vertex(sourceNodeID, sourceNodeID);
			Vertex targetVertex = new Vertex(targetNodeID, targetNodeID);
			
			addLink(linkInfo.getString("guid"), nodes.indexOf(sourceVertex), nodes.indexOf(targetVertex), linkInfo.getFloat("dist"));
		}
		
		graph = new Graph(nodes, edges);
        dijkstra = new DijkstraAlgorithm(graph);
				
	}
	
	protected void addLink(String laneId, int sourceLocNo, int destLocNo,
            float distance) {
        Edge lane = new Edge(laneId,nodes.get(sourceLocNo), nodes.get(destLocNo), distance );
        edges.add(lane);
    }
	
	
	protected LinkedList<Vertex> getShortestPath(String truckMessageData) {
		Vertex sourceNode = null;
		Vertex targetNode = null;
		LinkedList<Vertex> fullPath = new LinkedList<Vertex>();
		
		JSONArray JSONTruckMessage = new JSONArray(truckMessageData);
		
		for (int i = 0; i < JSONTruckMessage.length()-1; i++) {
			JSONObject sourceInfo = JSONTruckMessage.getJSONObject(i);
			JSONObject nextInfo = JSONTruckMessage.getJSONObject(i+1);
			String sourceID = findNodeFromLocation(sourceInfo.getInt("X"), sourceInfo.getInt("Y"));
			String targetID = findNodeFromLocation(nextInfo.getInt("X"), nextInfo.getInt("Y"));
			
			for (int j = 0; j < nodes.size(); j++) {
				if (nodes.get(j).getId().equals(sourceID)) {
					sourceNode = nodes.get(j);
				}
				if (nodes.get(j).getId().equals(targetID)) {
					targetNode = nodes.get(j);
				}
			}
			
			// ++++
			if (sourceNode == null) {
				System.out.println("["+getAID().getLocalName()+"]: Source location invalid (not found in graph network) ");
			}
			if (targetNode == null) {
				System.out.println("["+getAID().getLocalName()+"]: target location invalid (not found in graph network) ");
			}
			
//			graph = new Graph(nodes, edges);
//	        dijkstra = new DijkstraAlgorithm(graph);
			
			dijkstra.execute(sourceNode);
	        LinkedList<Vertex> path = dijkstra.getPath(targetNode);
	        
	     // ++++
	     if (path.size() == 0) {
	     	System.out.println("["+getAID().getLocalName()+"]: No valid path found!! ");
	     }
	        
	        for (int k = 0; k < path.size(); k++) {
	        	fullPath.add(path.get(k));
	        }
	        
		}
		return fullPath;		
    }
	

	protected String findNodeFromLocation(int x, int y) {
//		protected String findNodeFromLocation(double x, double y) {
//			String roundedX = Double.toString(Math.round(x*100.0) / 100.0);
//			String roundedY = Double.toString(Math.round(y*100.0) / 100.0);
		String roundedX = Integer.toString(x);
		String roundedY = Integer.toString(y);
		String nodeID = null;
		
		int numNodes = nodesJSONArray.length();
		for (int i = 0; i < numNodes; i++) {
			JSONObject nodeInfo = nodesJSONArray.getJSONObject(i);

			String roundedNodeX = Integer.toString(nodeInfo.getJSONObject("location").getInt("x"));
			String roundedNodeY = Integer.toString(nodeInfo.getJSONObject("location").getInt("y"));
			
			if (roundedX.equals(roundedNodeX) && roundedY.equals(roundedNodeY)) {
				nodeID = nodeInfo.getString("guid");
			}
		}
		
		return nodeID;
	}
	
	protected double getPathTime(LinkedList<Vertex> fullPath) {
		double time = 0.0;
		double speedFactor = 1.0;
		double totalDistance = 0.0;
		
		for (int i = 0; i < fullPath.size()-1; i++) {
			String graphSourceID = fullPath.get(i).getId();
			String graphTargetID = fullPath.get(i+1).getId();
			
			for (int j = 0; j < linksJSONArray.length(); j++) {
				String edgeSourceID = linksJSONArray.getJSONObject(j).getString("source");
				String edgeTargetID = linksJSONArray.getJSONObject(j).getString("target");
				
				if (edgeSourceID.equals(graphSourceID) && edgeTargetID.equals(graphTargetID)) {
					totalDistance = totalDistance + linksJSONArray.getJSONObject(j).getDouble("dist");
					
				}
			}
		}
		
		time = totalDistance / speedFactor;
		
		return time;
	}
	
	protected String getJSONPath(LinkedList<Vertex> fullPath) {
		double time = 0.0;
		double edgeTime = 0.0;
		double speedFactor = 1.0;
		double x = 0.0;
		double y = 0.0;
		
		JSONArray pathInfoArray = new JSONArray();
		
//		String graphSourceID = fullPath.get(0).getId();
//		String graphTargetID = fullPath.get(1).getId();
//		
//		for (int k = 0; k < nodesJSONArray.length(); k++) {
//			String nodeID = nodesJSONArray.getJSONObject(k).getString("guid");
//			if (nodeID.equals(graphSourceID)) {
//				x = nodesJSONArray.getJSONObject(k).getJSONObject("location").getDouble("x");
//				y = nodesJSONArray.getJSONObject(k).getJSONObject("location").getDouble("y");
//			}
//		}
//		
//		JSONObject nodeInfo = new JSONObject();
//		nodeInfo.put("X", x);
//		nodeInfo.put("Y", y);
		
		for (int i = 0; i < fullPath.size(); i++) {
			JSONObject nodeInfo = new JSONObject();
			
			if (i == 0) {
				String graphSourceID = fullPath.get(i).getId();
				
				for (int k = 0; k < nodesJSONArray.length(); k++) {
					String nodeID = nodesJSONArray.getJSONObject(k).getString("guid");
					if (nodeID.equals(graphSourceID)) {
						x = nodesJSONArray.getJSONObject(k).getJSONObject("location").getDouble("x");
						y = nodesJSONArray.getJSONObject(k).getJSONObject("location").getDouble("y");
					}
				}
				
				nodeInfo.put("X", x);
				nodeInfo.put("Y", y);
				
				nodeInfo.put("time", time);
			}
			
			else {
				String graphSourceID = fullPath.get(i-1).getId();
				String graphTargetID = fullPath.get(i).getId();
				
				for (int k = 0; k < nodesJSONArray.length(); k++) {
					String nodeID = nodesJSONArray.getJSONObject(k).getString("guid");
					if (nodeID.equals(graphSourceID)) {
						x = nodesJSONArray.getJSONObject(k).getJSONObject("location").getDouble("x");
						y = nodesJSONArray.getJSONObject(k).getJSONObject("location").getDouble("y");
					}
				}
				
//				JSONObject nodeInfo = new JSONObject();
				nodeInfo.put("X", x);
				nodeInfo.put("Y", y);
				
				for (int j = 0; j < linksJSONArray.length(); j++) {
					String edgeSourceID = linksJSONArray.getJSONObject(j).getString("source");
					String edgeTargetID = linksJSONArray.getJSONObject(j).getString("target");
					
					if (edgeSourceID.equals(graphSourceID) && edgeTargetID.equals(graphTargetID)) {
						edgeTime = linksJSONArray.getJSONObject(j).getDouble("dist") / speedFactor;
						time = time + edgeTime;
						
						nodeInfo.put("time", time);
					}
				}
			}
			pathInfoArray.put(nodeInfo);
		}
		return pathInfoArray.toString();    	
    }
}
