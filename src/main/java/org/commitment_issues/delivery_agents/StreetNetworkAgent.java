//package org.commitment_issues.deliveryAgents;
package org.commitment_issues.delivery_agents;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.commitment_issues.CustomerOrder;
import org.json.*;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class StreetNetworkAgent extends Agent {
	JSONArray nodesJSONArray = new JSONArray();
	JSONArray linksJSONArray = new JSONArray();
			
	public List<Vertex> nodes = new ArrayList<Vertex>();
	public List<Edge> edges = new ArrayList<Edge>();
	public Graph graph;
    public DijkstraAlgorithm dijkstra;

	protected void setup() {
		System.out.println("Hello! StreetNetwork-agent "+getAID().getName()+" is ready.");
		
		registerInYellowPages();

	}
	
	protected void registerInYellowPages() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("street-network");
        sd.setName("street-network");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    protected void deregisterFromYellowPages() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

	protected void takeDown() {
		deregisterFromYellowPages();
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
	protected void parseStreetNetworkData(String streetNetworkData) {
		JSONObject JSONSNData = new JSONObject(streetNetworkData);
		
		boolean directed = JSONSNData.getBoolean("directed");
		
//		JSONArray nodesJSONArray = JSONSNData.getJSONArray("nodes");
//		JSONArray linksJSONArray = JSONSNData.getJSONArray("links");
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

	protected String findNodeFromLocation(double x, double y) {
//		double roundedX = Math.round(x*100.0) / 100.0;
//		double roundedY = Math.round(y*100.0) / 100.0;
		String roundedX = Double.toString(Math.round(x*100.0) / 100.0);
		String roundedY = Double.toString(Math.round(y*100.0) / 100.0);
//		Vertex node = null;
		String nodeID = null;
		
		int numNodes = nodesJSONArray.length();
		for (int i = 0; i < numNodes; i++) {
			JSONObject nodeInfo = nodesJSONArray.getJSONObject(i);
			
//			double roundedNodeX = Math.round(nodeInfo.getJSONObject("location").getDouble("x")*100.0) / 100.0;
//			double roundedNodeY = Math.round(nodeInfo.getJSONObject("location").getDouble("y")*100.0) / 100.0;
			String roundedNodeX = Double.toString(Math.round(nodeInfo.getJSONObject("location").getDouble("x")*100.0) / 100.0);
			String roundedNodeY = Double.toString(Math.round(nodeInfo.getJSONObject("location").getDouble("y")*100.0) / 100.0);
			
			if (roundedX.equals(roundedNodeX) && roundedY.equals(roundedNodeY)) {
				nodeID = nodeInfo.getString("guid");
			}
		}
		
		return nodeID;
	}


}
