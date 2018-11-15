package org.commitment_issues.deliveryAgents;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
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

import java.util.ArrayList;

import org.json.*;

@SuppressWarnings("serial")
public class TruckAgent extends Agent {
	protected float[] currTruckLocation_;
	protected int numOfBoxes_;
	protected OrderDetails currOrder_;
	protected OrderDetails nextOrder_;
	
	public TruckAgent() {
		currOrder_ = null;
		nextOrder_ = null;
		numOfBoxes_ = 0;
	}
	
	protected void setup() {
		System.out.println("Hello! TruckAgent "+ getAID().getName() +" is ready.");
		
		registerInYellowPages();
		addBehaviour(new TimeQuotationServer());
		addBehaviour(new TruckScheduleServer());
	}
	
	 protected void registerInYellowPages() {
	        // Register the Truck services service in the yellow pages

	        DFAgentDescription dfd = new DFAgentDescription();
	        dfd.setName(getAID());

	        ServiceDescription sd = new ServiceDescription();
	        sd.setType("transport-orders");
	        sd.setName("transport-orders");
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
	
	protected boolean isTruckIdle() {
		boolean isIdle = true;
		//TODO
		
		return isIdle;
	}
	
	protected float[] getCustomerLocation() {
		return currOrder_.customerLocation_;
	}
	
	protected AID discoverStreetNetworkAgents() {
        // Find the street network agent
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("street-network");
        template.addServices(sd);
        
        AID streetNwAgent = null;
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
            	streetNwAgent = result[0].getName();
            }
            else
            {
            	streetNwAgent = null;
            	System.out.println("No agent with Service type (street-network) found!");
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        return streetNwAgent;
    }
	
	@SuppressWarnings("unused")
	private class OrderDetails {
		public String orderID_;
		public float[] bakeryLocation_;
		public float[] customerLocation_;
		public int numOfBoxes_;
		
		public OrderDetails(String jsonMessage) {
			JSONObject jsonObj = new JSONObject(jsonMessage);
			
			orderID_ = jsonObj.getString("OrderID");
			numOfBoxes_ = jsonObj.getInt("NumOfBoxes");
			
			bakeryLocation_ = new float[2];
			bakeryLocation_[0] = jsonObj.getJSONObject("Source").getFloat("X");
			bakeryLocation_[1] = jsonObj.getJSONObject("Source").getFloat("Y");
			
			customerLocation_ = new float[2];
			customerLocation_[0] = jsonObj.getJSONObject("Destination").getFloat("X");
			customerLocation_[1] = jsonObj.getJSONObject("Destination").getFloat("Y");
		}
	}
	
	@SuppressWarnings("unused")
	private enum TimeQuotationStates {
		WAIT_FOR_QUOTATION_REQUEST,
		QUOTATION_REQUESTED,
		REQUEST_TIME_TO_CUSTOMER,
		WAIT_FOR_TIME_FROM_STREET_NETWORK,
		SEND_QUOTATION,
		SEND_REFUSAL
	}
	
	@SuppressWarnings("unused")
    private class TimeQuotationServer extends CyclicBehaviour {

		private TimeQuotationStates state_ = TimeQuotationStates.WAIT_FOR_QUOTATION_REQUEST;
		private ACLMessage requestMsg_ = null;
		private float timeQuote_ = 0;
		private boolean responseReceivedFromStreetNW_ = false;	
		public OrderDetails orderDetails_;
		
		private void resetClassMembers() {
			state_ = TimeQuotationStates.WAIT_FOR_QUOTATION_REQUEST;
			requestMsg_ = null;
			timeQuote_ = 0;
			responseReceivedFromStreetNW_ = false;	
			orderDetails_ = null;
		}
		
		protected void handleTimeQueryResponse(float time) {
			timeQuote_ = time;
			responseReceivedFromStreetNW_ = true;
		}
		
        public void action() {
        	switch (state_) {
        	case WAIT_FOR_QUOTATION_REQUEST:
        		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
        		requestMsg_ = myAgent.receive(mt);
                if (requestMsg_ != null) {
                	state_ = TimeQuotationStates.QUOTATION_REQUESTED;
                }
                else {
                	block();
                }
        		break;
        	case QUOTATION_REQUESTED:
        		if (nextOrder_ == null) {
	        		orderDetails_ = new OrderDetails(requestMsg_.getContent());
	        		state_ = TimeQuotationStates.REQUEST_TIME_TO_CUSTOMER;
        		} 
        		else {
        			state_ = TimeQuotationStates.SEND_REFUSAL;
        		}
        		break;
        	case REQUEST_TIME_TO_CUSTOMER:
        		myAgent.addBehaviour(new QueryTime(this));
        		state_ = TimeQuotationStates.WAIT_FOR_TIME_FROM_STREET_NETWORK;
        		break;
        	case WAIT_FOR_TIME_FROM_STREET_NETWORK:
        		if (responseReceivedFromStreetNW_) {
        			state_ = TimeQuotationStates.SEND_QUOTATION;
        			responseReceivedFromStreetNW_ = false;
        		}
        		else {
        			block();
        		}
        		break;
        	case SEND_QUOTATION:
        		ACLMessage reply = requestMsg_.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(Float.toString(timeQuote_));
                myAgent.send(reply);
                resetClassMembers();
        		break;
        	case SEND_REFUSAL:
        		ACLMessage reject = requestMsg_.createReply();
        		reject.setPerformative(ACLMessage.REFUSE);
        		reject.setContent("Busy");
                myAgent.send(reject);
                resetClassMembers();
        		break;
			default:
				break;
        	}
        }
    }
	
	@SuppressWarnings("unused")
	private class TruckScheduleServer extends CyclicBehaviour {
		public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                ACLMessage reply = msg.createReply();
                
                if (nextOrder_ == null) {
                	nextOrder_ = new OrderDetails(msg.getContent());
                	reply.setPerformative(ACLMessage.INFORM);
                	reply.setContent("DeliveryAccepted");
                }
                else {
                	reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Busy");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
	}
	
	private enum StreetNetworkQueryStates{
		FIND_STREET_NETWORK_AGENTS,
		REQUEST_STREET_NETWORK,
		WAIT_FOR_RESPONSE,
		QUERY_COMPLETE,
		QUERY_FAILED
	}
	
	@SuppressWarnings("unused")
	private class QueryTime extends Behaviour {
		private AID streetNwAgent_;
		private StreetNetworkQueryStates state_ = StreetNetworkQueryStates.FIND_STREET_NETWORK_AGENTS;
		private MessageTemplate mt_;
		private TimeQuotationServer requester_;
		
		public QueryTime(TimeQuotationServer requester) {
			requester_ = requester;
		}
		
		private String getRequestContent() {
			JSONArray jsonArray = new JSONArray();
			
			// Add source node location
			jsonArray.put(new JSONObject()
					.put("X", currTruckLocation_[0])
          		  	.put("Y", currTruckLocation_[1]));
			
			// If truck is not idle add the location of current customer
			if (currOrder_.customerLocation_ != null) {
				jsonArray.put(new JSONObject()
						.put("X", currOrder_.customerLocation_[0])
	          		  	.put("Y", currOrder_.customerLocation_[1]));
			}
			
			// Add location of bakery
			jsonArray.put(new JSONObject()
					.put("X", requester_.orderDetails_.bakeryLocation_[0])
          		  	.put("Y", requester_.orderDetails_.bakeryLocation_[1]));
			
			// Add location of new customer
			jsonArray.put(new JSONObject()
					.put("X", requester_.orderDetails_.customerLocation_[0])
			        .put("Y", requester_.orderDetails_.customerLocation_[1]));
			
			return jsonArray.toString();
		}
		
		public void action() {
			switch(state_) {
			case FIND_STREET_NETWORK_AGENTS:
				streetNwAgent_ = discoverStreetNetworkAgents();
				if (streetNwAgent_ != null) {
					state_ = StreetNetworkQueryStates.REQUEST_STREET_NETWORK;
				}
				break;
			case REQUEST_STREET_NETWORK:
				// Send the request to the street network
				String conversationID = "TimeQuery";
                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(streetNwAgent_);
                request.setContent(getRequestContent());
                request.setConversationId(conversationID);
                request.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                myAgent.send(request);
                // Prepare the template to get replies
                mt_ = MessageTemplate.and(MessageTemplate.MatchConversationId(conversationID),
                        MessageTemplate.MatchInReplyTo(request.getReplyWith()));
                state_ = StreetNetworkQueryStates.WAIT_FOR_RESPONSE;
				break;
			case WAIT_FOR_RESPONSE:
				// Receive response from StreetNetworkAgent
                ACLMessage reply = myAgent.receive(mt_);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        float time = Float.parseFloat(reply.getContent());
                        requester_.handleTimeQueryResponse(time);
                        state_ = StreetNetworkQueryStates.QUERY_COMPLETE;
                    }
                    else {
                    	System.out.println("TruckAgent: Querying time from street network failed!!");
                    	state_ = StreetNetworkQueryStates.QUERY_FAILED;
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
			return state_ == StreetNetworkQueryStates.QUERY_COMPLETE ||
					state_ == StreetNetworkQueryStates.QUERY_FAILED;
		}
	}
	
	@SuppressWarnings("unused")
	private class QueryPath extends Behaviour {
		private AID streetNwAgent_;
		private StreetNetworkQueryStates state_ = StreetNetworkQueryStates.FIND_STREET_NETWORK_AGENTS;
		private MessageTemplate mt_;
		private TimeQuotationServer requester_;
		private float[] source_;
		private float[] destination_;
		
		public QueryPath(TimeQuotationServer requester, float[] source, float[] destination) {
			requester_ = requester;
			source_ = source;
			destination_ = destination;
		}
		
		private String getRequestContent() {
			JSONArray jsonArray = new JSONArray();
			
			// Add source node location
			jsonArray.put(new JSONObject()
					.put("X", source_[0])
          		  	.put("Y", source_[1]));
			
			// Add location of new customer
			jsonArray.put(new JSONObject()
					.put("X", destination_[0])
			        .put("Y", destination_[1]));
			
			return jsonArray.toString();
		}
		
		private ArrayList<float[]> parseJSONPath(String jsonString) {
			ArrayList<float[]> path = new ArrayList<float[]>();
			JSONArray jsonArr = new JSONArray(jsonString);
			
			for (int i = 0; i < jsonArr.length(); i++) {
				float[] node = new float[3];
				node[0] = jsonArr.getJSONObject(i).getFloat("X");
				node[1] = jsonArr.getJSONObject(i).getFloat("Y");
				node[2] = jsonArr.getJSONObject(i).getFloat("time");
				path.add(node);
			}
			return path;
		}
		
		public void action() {
			switch(state_) {
			case FIND_STREET_NETWORK_AGENTS:
				streetNwAgent_ = discoverStreetNetworkAgents();
				if (streetNwAgent_ != null) {
					state_ = StreetNetworkQueryStates.REQUEST_STREET_NETWORK;
				}
				break;
			case REQUEST_STREET_NETWORK:
				// Send the request to the street network
				String conversationID = "PathQuery";
                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(streetNwAgent_);
                request.setContent(getRequestContent());
                request.setConversationId(conversationID);
                request.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                myAgent.send(request);
                // Prepare the template to get replies
                mt_ = MessageTemplate.and(MessageTemplate.MatchConversationId(conversationID),
                        MessageTemplate.MatchInReplyTo(request.getReplyWith()));
                state_ = StreetNetworkQueryStates.WAIT_FOR_RESPONSE;
				break;
			case WAIT_FOR_RESPONSE:
				// Receive response from StreetNetworkAgent
                ACLMessage reply = myAgent.receive(mt_);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        ArrayList<float[]> path = parseJSONPath(reply.getContent());
                        //TODO store the path for use.
                        state_ = StreetNetworkQueryStates.QUERY_COMPLETE;
                    }
                    else {
                    	System.out.println("TruckAgent: Querying path from street network failed!!");
                    	state_ = StreetNetworkQueryStates.QUERY_FAILED;
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
			return state_ == StreetNetworkQueryStates.QUERY_COMPLETE ||
					state_ == StreetNetworkQueryStates.QUERY_FAILED;
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
