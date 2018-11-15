package org.commitment_issues.deliveryAgents;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.json.*;

@SuppressWarnings("serial")
public class TruckAgent extends Agent {
	protected void setup() {
		System.out.println("Hello! TruckAgent "+ getAID().getName() +" is ready.");
		
		registerInYellowPages();
		
//		addBehaviour(new BoxToDeliverServer());
//		addBehaviour(new BoxToDeliverInformer());

	}
	
	 protected void registerInYellowPages() {
	        // Register the Truck services service in the yellow pages

	        DFAgentDescription dfd = new DFAgentDescription();
	        dfd.setName(getAID());

	        ServiceDescription sd = new ServiceDescription();
	        sd.setType("Transport-orders");
	        sd.setName("Transport-orders");
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
	
	protected void handleTimeQueryResponse(int queryID, float time) {
		//TODO
	}
	
	private enum TimeQueryStates{
		FIND_STREET_NETWORK_AGENTS,
		REQUEST_STREET_NETWORK,
		WAIT_FOR_RESPONSE,
		QUERY_COMPLETE,
		QUERY_FAILED
	}
	
	@SuppressWarnings("unused")
	private class QueryTime extends Behaviour {
		private AID streetNwAgent_;
		private TimeQueryStates state_ = TimeQueryStates.FIND_STREET_NETWORK_AGENTS;
		private MessageTemplate mt_;
		private float[] source_;
		private float[] destination_;
		private int queryID_;
		
		public QueryTime(int queryID, float[] source, float[] destination) {
			queryID_ = queryID;
			source_ = source;
			destination_ = destination;
		}
		
		protected void discoverProcessors() {
            // Find the street network agent
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("street-network");
            template.addServices(sd);
            
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                	streetNwAgent_ = result[0].getName();
                	state_ = TimeQueryStates.REQUEST_STREET_NETWORK;
                }
                else
                {
                	streetNwAgent_ = null;
                	System.out.println("No agent with Service type (street-network) found!");
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
		
		private String getRequestContent() {
			String jsonString = new JSONObject()
	                  .put("Source", new JSONObject()
	                		  .put("X", source_[0])
	                		  .put("Y", source_[1]))
	                  .put("Destination", new JSONObject()
	                		  .put("X", destination_[0])
	                		  .put("Y", destination_[1])).toString();
			return jsonString;
		}
		
		private String getConversationID() {
			return "TimeQuery-" + Integer.toString(queryID_);
		}
		
		private void reportTimeBetweenNodes(float time) {
			((TruckAgent) myAgent).handleTimeQueryResponse(queryID_, time);
		}
		
		public void action() {
			switch(state_) {
			case FIND_STREET_NETWORK_AGENTS:
				discoverProcessors();
				break;
			case REQUEST_STREET_NETWORK:
				// Send the request to the street network
				String conversationID = getConversationID();
				String content = getRequestContent();
                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(streetNwAgent_);
                request.setContent(content);
                request.setConversationId(conversationID);
                request.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                myAgent.send(request);
                // Prepare the template to get replies
                mt_ = MessageTemplate.and(MessageTemplate.MatchConversationId(conversationID),
                        MessageTemplate.MatchInReplyTo(request.getReplyWith()));
                state_ = TimeQueryStates.WAIT_FOR_RESPONSE;
				break;
			case WAIT_FOR_RESPONSE:
				// Receive response from StreetNetworkAgent
                ACLMessage reply = myAgent.receive(mt_);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        float time = Float.parseFloat(reply.getContent());
                        reportTimeBetweenNodes(time);
                        state_ = TimeQueryStates.QUERY_COMPLETE;
                    }
                    else {
                    	System.out.println("TruckAgent: Querying time from street network failed!!");
                    	state_ = TimeQueryStates.QUERY_FAILED;
                    }
                } else {
                    block();
                }
				break;
			default:
				break;
			}
		}
		
		public boolean done() {
			return state_ == TimeQueryStates.QUERY_COMPLETE ||
					state_ == TimeQueryStates.QUERY_FAILED;
		}
	}
	
	// Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	@SuppressWarnings("unused")
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			myAgent.getContentManager().registerLanguage(codec);
			myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(myAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
				myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
				myAgent.send(shutdownMessage);
			}
			catch (Exception e) {
				//LOGGER.error(e);
			}

		}
	}
}
