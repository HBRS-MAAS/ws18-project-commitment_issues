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
import java.util.Arrays;

import org.maas.agents.*;

import org.json.*;

@SuppressWarnings("serial")
public class TruckAgent extends TimedAgent {
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
		
		ArrayList<String> services = new ArrayList<String>();
		services.add("transport-orders");
		register(services, "transport-orders");
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
		currPath_ = null;

		addBehaviour(new QueryPath(currOrder_.bakeryLocation_));
		System.out.println(timedAgent.getAID().getLocalName() + " Started executing new order: " + currOrder_.orderID_);
		
		//TODO: Find a proper place to put the below finished command
		finished();
	}
	
	protected void updateCurrPath(ArrayList<float[]> path) {
		currPath_ = path;
		currTruckLocation_ = new float[2];
		currTruckLocation_[0] = currPath_.get(0)[0];
		currTruckLocation_[1] = currPath_.get(0)[1];
		pathStartTime_ = (getCurrentDay() * 24) + getCurrentHour();
		System.out.println(timedAgent.getAID().getLocalName() + " Truck started with new path at " + getCurrentHour() + " hrs");
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
	
	private String getPosAsString(float[] pos) {
		StringBuilder sb = new StringBuilder();
		sb.append("( " + pos[0]);
		sb.append(", ");
		sb.append(pos[1] + ")");
		return sb.toString();
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
		
		public void print() {
			System.out.println("******** Order Details ********");
			System.out.println("Order ID: " + orderID_);
			System.out.println("Bakery Name/Location: " + bakeryName_ + getPosAsString(bakeryLocation_));
			System.out.println("Customer Name/Location: " + customerName_ + getPosAsString(customerLocation_));
			System.out.println("NumOfBoxes: " + numOfBoxes_);
			System.out.println("Delivery date/time: " + deliveryDate_ + "." + deliveryTime_);
			System.out.println("*******************************");
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
        		requestMsg_ = timedAgent.receive(mt);
                if (requestMsg_ != null) {
                	state_ = TimeQuotationStates.QUOTATION_REQUESTED;
                	System.out.println(timedAgent.getAID().getLocalName() + " Received Quotation message: " + requestMsg_.getContent());
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
        		timedAgent.addBehaviour(new QueryTime(this));
        		state_ = TimeQuotationStates.WAIT_FOR_TIME_FROM_STREET_NETWORK;
        		System.out.println(timedAgent.getAID().getLocalName() + " Query for total time placed");
        		break;
        	case WAIT_FOR_TIME_FROM_STREET_NETWORK:
        		if (responseReceivedFromStreetNW_) {
        			state_ = TimeQuotationStates.SEND_QUOTATION;
        			responseReceivedFromStreetNW_ = false;
        			System.out.println(timedAgent.getAID().getLocalName() + " Query response for time received");
        		}
        		break;
        	case SEND_QUOTATION:
        		ACLMessage reply = requestMsg_.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(Float.toString(timeQuote_));
                timedAgent.send(reply);
                System.out.println(timedAgent.getAID().getLocalName() + " Sent time to deliver quote" + timeQuote_);
                resetClassMembers();
        		break;
        	case SEND_REFUSAL:
        		ACLMessage reject = requestMsg_.createReply();
        		reject.setPerformative(ACLMessage.REFUSE);
        		reject.setContent("Busy");
                timedAgent.send(reject);
                System.out.println(timedAgent.getAID().getLocalName() + " Rejected Quotation request");
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
            ACLMessage msg = timedAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                ACLMessage reply = msg.createReply();
                
                OrderDetails newOrder = new OrderDetails(msg.getContent());
                
                if (currOrder_ == null) {
                	currOrder_ = newOrder;
                	reply.setPerformative(ACLMessage.INFORM);
                	reply.setContent("DeliveryAccepted");
                	System.out.println(timedAgent.getAID().getLocalName() + " Accepted new order as CURRENT order:");
                	currOrder_.print();
                }
                else if (nextOrder_ == null) {
                	nextOrder_ = newOrder;
                	reply.setPerformative(ACLMessage.INFORM);
                	reply.setContent("DeliveryAccepted");
                	System.out.println(timedAgent.getAID().getLocalName() + " Accepted new order as NEXT order:");
                	nextOrder_.print();
                }
                else {
                	reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Busy");
                    System.out.println(timedAgent.getAID().getLocalName() + " Failed to accept new order");
                }
                timedAgent.send(reply);
                System.out.println(timedAgent.getAID().getLocalName() + " Responded to ACCEPT_PROPOSAL with : " + reply.getContent());
            } else {
                block();
            }
        }
	}
	
	private class MoveTruck extends CyclicBehaviour {
		
		private boolean updateTruckPosition() {
			boolean retval = false;
			if (currPath_ != null && getAllowAction()) {
				float timeSincePathStart = getTime() - pathStartTime_;
				int i = 1;			
				while (true && (i < currPath_.size())) {
					if (timeSincePathStart < currPath_.get(i)[2]) {
						break;
					}
					i++;
				}
				
				if (currTruckLocation_[0] != currPath_.get(i - 1)[0] ||
					currTruckLocation_[1] != currPath_.get(i - 1)[1])
				{
					float[] oldPos = currTruckLocation_;
					currTruckLocation_ = new float[2];
					currTruckLocation_[0] = currPath_.get(i - 1)[0];
					currTruckLocation_[1] = currPath_.get(i - 1)[1];
					retval = true;
					System.out.println("Truck moved at " + getCurrentHour() + " hrs from " + getPosAsString(oldPos) + " to " + getPosAsString(currTruckLocation_));

				}
			}
			finished();
			return retval;
		}
		
		private int getTime() {
			return (getCurrentDay() * 24) + getCurrentHour();
		}
		
		private boolean reachedEndOfPath() {
			float[] endPos = {currPath_.get(currPath_.size() - 1)[0], currPath_.get(currPath_.size() - 1)[1]};
			return Arrays.equals(currTruckLocation_, endPos);
		}
		
		private boolean reachedCutomer() {
			return (truckState_ == TruckState.MOVING_TO_CUSTOMER) && reachedEndOfPath();
		}
		
		private boolean reachedBakery() {
			return (truckState_ == TruckState.MOVING_TO_BAKERY) && reachedEndOfPath();
		}
		
		@SuppressWarnings("unused")
		private String getVisualizationMessage() {			
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("TruckID", timedAgent.getAID().getLocalName());
			jsonObj.put("X", currTruckLocation_[0]);
			jsonObj.put("Y", currTruckLocation_[1]);
			jsonObj.put("OrderID", currOrder_.orderID_);
			jsonObj.put("EstimatedTime", currPath_.get(currPath_.size() - 1)[2] - pathStartTime_);
			return jsonObj.toString();
		}
		
		public void action() {
        	if ((truckState_ == TruckState.IDLE) && (currOrder_ != null)) {
        		startNewOrder(currOrder_);
        	}
        	else if ((truckState_ != TruckState.IDLE)  && updateTruckPosition()) {
            	if (reachedBakery()) {
            		timedAgent.addBehaviour(new RequestBoxes(currOrder_.orderID_));
            		truckState_ = TruckState.MOVING_TO_CUSTOMER;
            		currPath_ = null;
            		System.out.println(timedAgent.getAID().getLocalName() + " Reached bakery. Requested boxes from transport agent");
            	}
            	else if (reachedCutomer()) {
            		timedAgent.addBehaviour(new PostDeliveryCompletionMessage(currOrder_));
            		if (nextOrder_ != null) {
            			startNewOrder(nextOrder_);
            			nextOrder_ = null;
            			System.out.println(timedAgent.getAID().getLocalName() + " Reached customer. Starting with next request");
            		}
            		else {
            			currOrder_ = null;
            			truckState_ = TruckState.IDLE;
            			System.out.println(timedAgent.getAID().getLocalName() + " Reached customer. Truck is Idle as there is no next order");
            		}
            	}
            	
//                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
//                msg.addReceiver(discoverAgent("transport-visualization"));
//                msg.setConversationId("truck-location");
//                msg.setContent(getVisualizationMessage());
//                timedAgent.send(msg);
//                System.out.println(timedAgent.getAID().getLocalName() + " Message sent to Visualization agent: " + msg.getContent());
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
                timedAgent.send(request);
                // Prepare the template to get replies
                mt_ = MessageTemplate.and(MessageTemplate.MatchConversationId(conversationID),
                        MessageTemplate.MatchInReplyTo(request.getReplyWith()));
                state_ = StreetNetworkQueryStates.WAIT_FOR_RESPONSE;
                System.out.println(timedAgent.getAID().getLocalName() + " Placed time query with SN for: " + request.getContent());
				break;
			case WAIT_FOR_RESPONSE:
				// Receive response from StreetNetworkAgent
                ACLMessage reply = timedAgent.receive(mt_);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        float time = Float.parseFloat(reply.getContent());
                        requester_.handleTimeQueryResponse(time);
                        state_ = StreetNetworkQueryStates.QUERY_COMPLETE;
                        System.out.println(timedAgent.getAID().getLocalName() + " Response for time received from SN: " + time);
                    }
                    else {
                    	System.out.println(timedAgent.getAID().getLocalName() + ": Querying time from street network failed!!");
                    	state_ = StreetNetworkQueryStates.QUERY_FAILED;
                    	System.out.println(timedAgent.getAID().getLocalName() + " Time query from SN failed: ");
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
				
//				if (i > 0) {
//					// Add time until previous node
//					node[2] += path.get(i - 1)[2];
//				}
				
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
                timedAgent.send(request);
                // Prepare the template to get replies
                mt_ = MessageTemplate.and(MessageTemplate.MatchConversationId(conversationID),
                        MessageTemplate.MatchInReplyTo(request.getReplyWith()));
                state_ = StreetNetworkQueryStates.WAIT_FOR_RESPONSE;
                System.out.println(timedAgent.getAID().getLocalName() + " Query for path placed with SN: " + request.getContent());
				break;
			case WAIT_FOR_RESPONSE:
				// Receive response from StreetNetworkAgent
                ACLMessage reply = timedAgent.receive(mt_);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                    	System.out.println(timedAgent.getAID().getLocalName() + " Response for path received from SN: " + reply.getContent());
                        updateCurrPath(parseJSONPath(reply.getContent()));
                        state_ = StreetNetworkQueryStates.QUERY_COMPLETE;
                    }
                    else {
                    	System.out.println(timedAgent.getAID().getLocalName() + ": Querying path from street network failed!!");
                    	state_ = StreetNetworkQueryStates.QUERY_FAILED;
                    	System.out.println(timedAgent.getAID().getLocalName() + " Query for path failed from SN");
                    }
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
			jsonObj.put("OrderId", orderID_);
			
			return jsonObj.toString();
		}
		
		private void parseJSONReply(String reply) {
			JSONObject obj = new JSONObject(reply);
			String orderID = obj.getString("OrderID");
			JSONArray boxList = obj.getJSONArray("Boxes");
			
			timedAgent.addBehaviour(new QueryPath(currOrder_.customerLocation_));
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
				msg.setReplyWith("req" + System.currentTimeMillis());
				timedAgent.send(msg);
                // Prepare the template to get replies
                mt_ = MessageTemplate.and(MessageTemplate.MatchConversationId(convID),
                        MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                System.out.println(timedAgent.getAID().getLocalName() + " Requested transportAgent for boxes: " + msg.getContent());
                state_ = 1;
				break;
			case 1:
				// Receive response
                ACLMessage reply = timedAgent.receive(mt_);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        parseJSONReply(reply.getContent());
                        System.out.println(timedAgent.getAID().getLocalName() + " Received boxes from transportAgent: " + reply.getContent());
                        state_ = 2;
                    }
                    else {
                    	System.out.println(timedAgent.getAID().getLocalName() + ": Receiving Boxes from TruckAgent Failed!!");
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
          		  		.put("OrderDeliveredBy", timedAgent.getAID().getLocalName())
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
			timedAgent.send(msg);
			System.out.println(timedAgent.getAID().getLocalName() + " Posted message to mailbox: " + msg.getContent());
		}
	}
	
	// Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	@SuppressWarnings("unused")
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			timedAgent.getContentManager().registerLanguage(codec);
			timedAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(timedAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
				timedAgent.getContentManager().fillContent(shutdownMessage,new Action(timedAgent.getAID(), new ShutdownPlatform()));
				timedAgent.send(shutdownMessage);
			}
			catch (Exception e) {
				//LOGGER.error(e);
			}

		}
	}
}
