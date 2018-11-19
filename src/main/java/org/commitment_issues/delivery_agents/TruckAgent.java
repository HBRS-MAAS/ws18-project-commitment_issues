package org.commitment_issues.delivery_agents;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
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
import org.yourteamname.agents.*;

import org.json.*;

@SuppressWarnings("serial")
public class TruckAgent extends BaseAgent {
	protected float[] currTruckLocation_;
	protected int numOfBoxes_;
	protected OrderDetails currOrder_;
	protected OrderDetails nextOrder_;
	protected ArrayList<float[]> currPath_;
	protected float pathStartTime_;
	protected TruckState truckState_;
	
	protected void setup() {
		super.setup();
		System.out.println("Hello! TruckAgent "+ getAID().getName() +" is ready.");
		
		currOrder_ = null;
		nextOrder_ = null;
		numOfBoxes_ = 0;
		truckState_ = TruckState.IDLE;
		
		//TODO: Load from json
		currTruckLocation_ = new float[2];
		currTruckLocation_[0] = (float) -0.82;
		currTruckLocation_[1] = (float) 7.19;
		
		register("transport-orders", "transport-orders");
		addBehaviour(new TimeQuotationServer());
		addBehaviour(new TruckScheduleServer());
		addBehaviour(new MoveTruck());
	}

	protected void takeDown() {
		deRegister();
		
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
	protected boolean isTruckIdle() {
		return currOrder_ == null;
	}
	
	protected float[] getCustomerLocation() {
		return currOrder_.customerLocation_;
	}
	
	protected void startNewOrder(OrderDetails order) {
		truckState_ = TruckState.MOVING_TO_BAKERY;
		currOrder_ = order;
		pathStartTime_ = getCurrentHour();
		currPath_ = null;

		addBehaviour(new QueryPath(currOrder_.bakeryLocation_));
	}
	
	protected void updateCurrPath(ArrayList<float[]> path) {
		currPath_ = path;
		currTruckLocation_ = new float[2];
		currTruckLocation_[0] = currPath_.get(0)[0];
		currTruckLocation_[1] = currPath_.get(0)[1];
	}
	
	protected void visualiseStreetNetwork(ACLMessage msg) {
		//TODO
    }
	
	protected AID discoverAgent(String serviceType) {
        // Find the an agent for given service type
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
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
            	System.out.println(getAID().getLocalName() +  ": No agent with Service type (" + serviceType + ") found!");
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        return streetNwAgent;
    }
	
	private enum TruckState {
		IDLE,
		MOVING_TO_BAKERY,
		MOVING_TO_CUSTOMER
	}
	
	private class OrderDetails {
		public String orderID_;
		public float[] bakeryLocation_;
		public float[] customerLocation_;
		public int numOfBoxes_;
		
		// TODO
		public String customerName_ = "No Customer Name Provided";
		public String bakeryName_ = "No Bakery Name Provided";
		public int deliveryDate_;
		public int deliveryTime_;
		
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
	
	private enum TimeQuotationStates {
		WAIT_FOR_QUOTATION_REQUEST,
		QUOTATION_REQUESTED,
		REQUEST_TIME_TO_CUSTOMER,
		WAIT_FOR_TIME_FROM_STREET_NETWORK,
		SEND_QUOTATION,
		SEND_REFUSAL
	}
	
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
        		requestMsg_ = baseAgent.receive(mt);
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
        		baseAgent.addBehaviour(new QueryTime(this));
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
                baseAgent.send(reply);
                resetClassMembers();
        		break;
        	case SEND_REFUSAL:
        		ACLMessage reject = requestMsg_.createReply();
        		reject.setPerformative(ACLMessage.REFUSE);
        		reject.setContent("Busy");
                baseAgent.send(reject);
                resetClassMembers();
        		break;
			default:
				break;
        	}
        }
    }
	
	private class TruckScheduleServer extends CyclicBehaviour {
		public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = baseAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                ACLMessage reply = msg.createReply();
                
                OrderDetails newOrder = new OrderDetails(msg.getContent());
                
                if (currOrder_ == null) {
                	currOrder_ = newOrder;
                	reply.setPerformative(ACLMessage.INFORM);
                	reply.setContent("DeliveryAccepted");
                }
                else if (nextOrder_ == null) {
                	nextOrder_ = newOrder;
                	reply.setPerformative(ACLMessage.INFORM);
                	reply.setContent("DeliveryAccepted");
                }
                else {
                	reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Busy");
                }
                baseAgent.send(reply);
            } else {
                block();
            }
        }
	}
	
	private class MoveTruck extends CyclicBehaviour {
		
		private boolean updateTruckPosition() {
			boolean retval = false;
			if (currPath_ != null) {
				float timeSincePathStart = getCurrentHour() - pathStartTime_;
				int i = 1;			
				while (true) {
					if (timeSincePathStart < currPath_.get(i)[2]) {
						break;
					}
					i++;
				}
				
				currTruckLocation_ = new float[2];
				currTruckLocation_[0] = currPath_.get(i - 1)[0];
				currTruckLocation_[1] = currPath_.get(i - 1)[1];
				retval = true;
			}
			
			return retval;
		}
		
		private boolean reachedEndOfPath() {
			float[] endPos = {currPath_.get(currPath_.size() - 1)[0], currPath_.get(currPath_.size() - 1)[1]};
			return currTruckLocation_ == endPos;
		}
		
		private boolean reachedCutomer() {
			return (truckState_ == TruckState.MOVING_TO_CUSTOMER) && reachedEndOfPath();
		}
		
		private boolean reachedBakery() {
			return (truckState_ == TruckState.MOVING_TO_BAKERY) && reachedEndOfPath();
		}
		
		private String getVisualizationMessage() {			
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("TruckID", baseAgent.getAID().getLocalName());
			jsonObj.put("X", currTruckLocation_[0]);
			jsonObj.put("Y", currTruckLocation_[1]);
			jsonObj.put("OrderID", currOrder_.orderID_);
			jsonObj.put("EstimatedTime", currPath_.get(currPath_.size() - 1)[2] - pathStartTime_);
			return jsonObj.toString();
		}
		
		public void action() {
            if (getAllowAction()) {
            	if ((truckState_ == TruckState.IDLE) && (currOrder_ != null)) {
            		startNewOrder(currOrder_);
            	}
            	else if ((truckState_ != TruckState.IDLE)  && updateTruckPosition()) {
	            	if (reachedBakery()) {
	            		baseAgent.addBehaviour(new RequestBoxes(nextOrder_.orderID_));
	            		truckState_ = TruckState.MOVING_TO_CUSTOMER;
	            		currPath_ = null;
	            	}
	            	else if (reachedCutomer()) {
	            		baseAgent.addBehaviour(new PostDeliveryCompletionMessage(currOrder_));
	            		if (nextOrder_ != null) {
	            			startNewOrder(nextOrder_);
	            			nextOrder_ = null;
	            		}
	            		else {
	            			truckState_ = TruckState.IDLE;
	            		}
	            	}
	            	
	                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
	                msg.addReceiver(discoverAgent("transport-visualization"));
	                msg.setConversationId("truck-location");
	                msg.setContent(getVisualizationMessage());
	                baseAgent.send(msg);
            	}
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
			if (!isTruckIdle()) {
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
				streetNwAgent_ = discoverAgent("street-network");
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
                baseAgent.send(request);
                // Prepare the template to get replies
                mt_ = MessageTemplate.and(MessageTemplate.MatchConversationId(conversationID),
                        MessageTemplate.MatchInReplyTo(request.getReplyWith()));
                state_ = StreetNetworkQueryStates.WAIT_FOR_RESPONSE;
				break;
			case WAIT_FOR_RESPONSE:
				// Receive response from StreetNetworkAgent
                ACLMessage reply = baseAgent.receive(mt_);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        float time = Float.parseFloat(reply.getContent());
                        requester_.handleTimeQueryResponse(time);
                        state_ = StreetNetworkQueryStates.QUERY_COMPLETE;
                    }
                    else {
                    	System.out.println(baseAgent.getAID().getLocalName() + ": Querying time from street network failed!!");
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
		private float[] destination_;
		
		public QueryPath(float[] destination) {
			destination_ = destination;
		}
		
		private String getRequestContent() {
			JSONArray jsonArray = new JSONArray();
			
			// Add source node location
			jsonArray.put(new JSONObject()
					.put("X", currTruckLocation_[0])
          		  	.put("Y", currTruckLocation_[1]));
			
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
				
				if (i > 0) {
					// Add time until previous node
					node[2] += path.get(i - 1)[2];
				}
				
				path.add(node);
			}
			return path;
		}
		
		public void action() {
			switch(state_) {
			case FIND_STREET_NETWORK_AGENTS:
				streetNwAgent_ = discoverAgent("street-network");
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
                baseAgent.send(request);
                // Prepare the template to get replies
                mt_ = MessageTemplate.and(MessageTemplate.MatchConversationId(conversationID),
                        MessageTemplate.MatchInReplyTo(request.getReplyWith()));
                state_ = StreetNetworkQueryStates.WAIT_FOR_RESPONSE;
				break;
			case WAIT_FOR_RESPONSE:
				// Receive response from StreetNetworkAgent
                ACLMessage reply = baseAgent.receive(mt_);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        updateCurrPath(parseJSONPath(reply.getContent()));
                        state_ = StreetNetworkQueryStates.QUERY_COMPLETE;
                    }
                    else {
                    	System.out.println(baseAgent.getAID().getLocalName() + ": Querying path from street network failed!!");
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
	private class RequestBoxes extends Behaviour {
		private String orderID_;
		private int state_ = 0;
		private MessageTemplate mt_;
		
		public RequestBoxes(String orderID) {
			orderID_ = orderID;
		}
		
		private String generateJsonMessage() {			
			JSONObject jsonObj = new JSONObject();						
			jsonObj.put("OrderID", orderID_);
			
			return jsonObj.toString();
		}
		
		private void parseJSONReply(String reply) {
			JSONObject obj = new JSONObject(reply);
			String orderID = obj.getString("OrderID");
			JSONArray boxList = obj.getJSONArray("Boxes");
			
			baseAgent.addBehaviour(new QueryPath(currOrder_.customerLocation_));
			// TODO
		}
		
		public void action() {
			switch(state_) {
			case 0:
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				String convID = "ReadyForPickup" + orderID_;
				msg.addReceiver(discoverAgent("transport-agent")); // TODO fix this services name
				msg.setContent(generateJsonMessage());
				msg.setConversationId(convID);
				msg.setPostTimeStamp(System.currentTimeMillis());
				baseAgent.send(msg);
                // Prepare the template to get replies
                mt_ = MessageTemplate.and(MessageTemplate.MatchConversationId(convID),
                        MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                
                state_ = 1;
				break;
			case 1:
				// Receive response
                ACLMessage reply = baseAgent.receive(mt_);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        parseJSONReply(reply.getContent());
                        state_ = 2;
                    }
                    else {
                    	System.out.println(baseAgent.getAID().getLocalName() + ": Receiving Boxes from TruckAgent Failed!!");
                    	state_ = 3;
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
			return state_ == 2 || state_ == 3;
		}
	}

	@SuppressWarnings("unused")
	private class PostDeliveryCompletionMessage extends OneShotBehaviour {
		private OrderDetails orderInfo_;
		
		public PostDeliveryCompletionMessage(OrderDetails order) {
			orderInfo_ = order;
		}
		
		private String generateJsonMessage() {			
			JSONObject jsonObj = new JSONObject();						
			jsonObj.put("DeliveryStatus", new JSONObject()
						.put("OrderDeliveredTo", orderInfo_.customerName_)
          		  		.put("OrderDeliveredBy", baseAgent.getAID().getLocalName())
          		  		.put("DayOfDelivery", orderInfo_.deliveryDate_)
          		  		.put("TimeOfDelivery", orderInfo_.deliveryTime_)
          		  		.put("NumOfBoxes", orderInfo_.numOfBoxes_)
          		  		.put("ProducedBy", orderInfo_.bakeryName_));
			
			return jsonObj.toString();
		}
		
		
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(discoverAgent("mailbox"));
			msg.setContent(generateJsonMessage());
			msg.setConversationId("DeliveryConfirmation");
			msg.setPostTimeStamp(System.currentTimeMillis());
			baseAgent.send(msg);
		}
	}
	
	// Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	@SuppressWarnings("unused")
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			baseAgent.getContentManager().registerLanguage(codec);
			baseAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(baseAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
				baseAgent.getContentManager().fillContent(shutdownMessage,new Action(baseAgent.getAID(), new ShutdownPlatform()));
				baseAgent.send(shutdownMessage);
			}
			catch (Exception e) {
				//LOGGER.error(e);
			}

		}
	}
}