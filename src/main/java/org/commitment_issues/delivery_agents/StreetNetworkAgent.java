//package org.commitment_issues.deliveryAgents;
package org.commitment_issues.delivery_agents;

import org.yourteamname.agents.BaseAgent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.*;

import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class StreetNetworkAgent extends BaseAgent {
	JSONArray nodesJSONArray = new JSONArray();
	JSONArray linksJSONArray = new JSONArray();
			
	public List<Vertex> nodes = new ArrayList<Vertex>();
	public List<Edge> edges = new ArrayList<Edge>();
	public Graph graph;
    public DijkstraAlgorithm dijkstra;

	protected void setup() {
		System.out.println("Hello! StreetNetwork-agent "+getAID().getName()+" is ready.");
		
		register("street-network", "street-network");
		
		addBehaviour(new TimeToDeliveryServer());
		addBehaviour(new PathServer());
	}

	protected void takeDown() {
		deRegister();
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
	private class TimeToDeliveryServer extends CyclicBehaviour {
		private MessageTemplate mt;

		public void action() {
			
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("TimeQuery"),
					MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
			msg = myAgent.receive(mt);
			
			if (msg != null) {
				String truckMessageContent = msg.getContent();
				ACLMessage reply = msg.createReply();
				
				double time = getPathTime(getShortestPath(truckMessageContent));


				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(String.valueOf(time));
				myAgent.send(reply);
			}

			else {
				block();
			}
		}
	}
	
	
	private class PathServer extends CyclicBehaviour {
		private MessageTemplate mt;

		public void action() {
			
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("PathQuery"),
					MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
			msg = myAgent.receive(mt);
			
			if (msg != null) {
				String truckMessageContent = msg.getContent();
				ACLMessage reply = msg.createReply();
				
				String JSONPath = getJSONPath(getShortestPath(truckMessageContent));


				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(String.valueOf(JSONPath));
				myAgent.send(reply);
			}

			else {
				block();
			}
		}
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
		
//		graph = new Graph(nodes, edges);
//        dijkstra = new DijkstraAlgorithm(graph);
				
	}
	
	protected void addLink(String laneId, int sourceLocNo, int destLocNo,
            float distance) {
        Edge lane = new Edge(laneId,nodes.get(sourceLocNo), nodes.get(destLocNo), distance );
        edges.add(lane);
    }
	
	
	protected LinkedList<Vertex> getShortestPath(String truckMessageData) {
		Vertex sourceNode = null;
		Vertex targetNode = null;
		LinkedList<Vertex> fullPath = null;
		
		JSONArray JSONTruckMessage = new JSONArray(truckMessageData);
		
		for (int i = 0; i < JSONTruckMessage.length()-1; i++) {
			JSONObject sourceInfo = JSONTruckMessage.getJSONObject(i);
			JSONObject nextInfo = JSONTruckMessage.getJSONObject(i+1);
			String sourceID = findNodeFromLocation(sourceInfo.getDouble("X"), sourceInfo.getDouble("Y"));
			String targetID = findNodeFromLocation(nextInfo.getDouble("X"), nextInfo.getDouble("Y"));
			
			for (int j = 0; j < nodes.size(); j++) {
				if (nodes.get(j).getId().equals(sourceID)) {
					sourceNode = nodes.get(j);
				}
				if (nodes.get(j).getId().equals(targetID)) {
					targetNode = nodes.get(j);
				}
			}
			
			dijkstra.execute(sourceNode);
	        LinkedList<Vertex> path = dijkstra.getPath(targetNode);
	        
	        for (int k = 0; k < path.size(); k++) {
	        	fullPath.add(path.get(k));
	        }
	        
		}
		
		return fullPath;		
		
    }
	

	protected String findNodeFromLocation(double x, double y) {
		String roundedX = Double.toString(Math.round(x*100.0) / 100.0);
		String roundedY = Double.toString(Math.round(y*100.0) / 100.0);
		String nodeID = null;
		
		int numNodes = nodesJSONArray.length();
		for (int i = 0; i < numNodes; i++) {
			JSONObject nodeInfo = nodesJSONArray.getJSONObject(i);

			String roundedNodeX = Double.toString(Math.round(nodeInfo.getJSONObject("location").getDouble("x")*100.0) / 100.0);
			String roundedNodeY = Double.toString(Math.round(nodeInfo.getJSONObject("location").getDouble("y")*100.0) / 100.0);
			
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
		double speedFactor = 1.0;
		double x = 0.0;
		double y = 0.0;
		JSONArray pathInfoArray = new JSONArray();
		
		for (int i = 0; i < fullPath.size()-1; i++) {
			String graphSourceID = fullPath.get(i).getId();
			String graphTargetID = fullPath.get(i+1).getId();
			
			for (int k = 0; k < nodesJSONArray.length(); k++) {
				String nodeID = nodesJSONArray.getJSONObject(k).getString("guid");
				if (nodeID.equals(graphSourceID)) {
					x = nodesJSONArray.getJSONObject(k).getJSONObject("location").getDouble("x");
					y = nodesJSONArray.getJSONObject(k).getJSONObject("location").getDouble("y");
				}
			}
			
			JSONObject nodeInfo = new JSONObject();
			nodeInfo.put("X", x);
			nodeInfo.put("Y", y);
			
			for (int j = 0; j < linksJSONArray.length(); j++) {
				String edgeSourceID = linksJSONArray.getJSONObject(j).getString("source");
				String edgeTargetID = linksJSONArray.getJSONObject(j).getString("target");
				
				if (edgeSourceID.equals(graphSourceID) && edgeTargetID.equals(graphTargetID)) {
					time = linksJSONArray.getJSONObject(j).getDouble("dist") / speedFactor;
					nodeInfo.put("time", time);
					
				}
			}
			
			pathInfoArray.put(nodeInfo);
			
		}
		
		return pathInfoArray.toString();
    	
    }


}
