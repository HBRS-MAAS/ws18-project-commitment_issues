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
import org.json.*;

@SuppressWarnings("serial")
public class TruckAgent extends Agent {
	protected float[] currentLocation_;
	protected float[] customerLocation_;
	protected int numOfBoxes_;
	
	public TruckAgent() {
		currentLocation_ = null; //TODO
		customerLocation_ = null;
		numOfBoxes_ = 0;
	}
	
	protected void setup() {
		System.out.println("Hello! TruckAgent "+ getAID().getName() +" is ready.");
		
		registerInYellowPages();
		addBehaviour(new TimeQuotationServer());
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
	
	protected float[] getTruckLocation() {
		return currentLocation_;
	}
	
	protected float[] getCustomerLocation() {
		return customerLocation_;
	}
	
	@SuppressWarnings("unused")
	private enum TimeQuotationStates {
		WAIT_FOR_QUOTATION_REQUEST,
		QUOTATION_REQUESTED,
		REQUEST_TIME_TO_CUSTOMER,
		WAIT_FOR_TIME_FROM_STREET_NETWORK,
		SEND_QUOTATION
	}
	
	@SuppressWarnings("unused")
    private class TimeQuotationServer extends CyclicBehaviour {
		private TimeQuotationStates state_ = TimeQuotationStates.WAIT_FOR_QUOTATION_REQUEST;
		private ACLMessage requestMsg_ = null;
		private String orderID_ = null;
		public float[] bakeryLocation_;
		public float[] newCustomerLocation_;
		public float[] currCustomerLocation_;
		public float[] truckLocation_;
		private float timeQuote_ = 0;
		private boolean responseReceivedFromStreetNW_ = false;		
		
		public float[] getCurrCustomerLoc() {
			return ((TruckAgent)myAgent).getCustomerLocation();
		}
		
		protected void decodeRequestMessage() {
			//TODO
			bakeryLocation_ = null;
			newCustomerLocation_ = null;
		}
		
		protected void handleTimeQueryResponse(String queryID, float time) {
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
        		decodeRequestMessage();
        		truckLocation_ = ((TruckAgent)myAgent).getTruckLocation();
        		currCustomerLocation_ = ((TruckAgent)myAgent).getCustomerLocation();
        		state_ = TimeQuotationStates.REQUEST_TIME_TO_CUSTOMER;
        		break;
        	case REQUEST_TIME_TO_CUSTOMER:
        		myAgent.addBehaviour(new QueryTime(this));
        		state_ = TimeQuotationStates.WAIT_FOR_TIME_FROM_STREET_NETWORK;
        		break;
        	case WAIT_FOR_TIME_FROM_STREET_NETWORK:
        		if (responseReceivedFromStreetNW_) {
        			state_ = TimeQuotationStates.SEND_QUOTATION;
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
                state_ = TimeQuotationStates.WAIT_FOR_QUOTATION_REQUEST;
        		break;
			default:
				break;
        	}
        }
    } // End of inner class OfferRequestsServer
	
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
		private String queryID_;
		private TimeQuotationServer requester_;
		
		public QueryTime(TimeQuotationServer requester) {
			requester_ = requester;
			queryID_ = Long.toString(System.currentTimeMillis());
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
			JSONArray jsonArray = new JSONArray();
			
			// Add source node location
			jsonArray.put(new JSONObject()
					.put("X", requester_.truckLocation_[0])
          		  	.put("Y", requester_.truckLocation_[1]));
			
			// If truck is not idle add the location of current customer
			if (requester_.currCustomerLocation_ != null) {
				jsonArray.put(new JSONObject()
						.put("X", requester_.currCustomerLocation_[0])
	          		  	.put("Y", requester_.currCustomerLocation_[1]));
			}
			
			// Add location of bakery
			jsonArray.put(new JSONObject()
					.put("X", requester_.bakeryLocation_[0])
          		  	.put("Y", requester_.bakeryLocation_[1]));
			
			// Add location of new customer
			jsonArray.put(new JSONObject()
					.put("X", requester_.newCustomerLocation_[0])
			        .put("Y", requester_.newCustomerLocation_[1]));
			
			return jsonArray.toString();
		}
		
		private void reportTimeBetweenNodes(float time) {
			requester_.handleTimeQueryResponse(queryID_, time);
		}
		
		public void action() {
			switch(state_) {
			case FIND_STREET_NETWORK_AGENTS:
				discoverProcessors();
				break;
			case REQUEST_STREET_NETWORK:
				// Send the request to the street network
				String conversationID = "TimeQuery-" + queryID_;
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
