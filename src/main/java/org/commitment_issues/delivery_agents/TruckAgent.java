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
public class TruckAgent extends BaseAgent {
	protected float[] currTruckLocation_;
	protected int numOfBoxes_;
	protected OrderDetails currOrder_;
	protected OrderDetails nextOrder_;
	protected ArrayList<float[]> currPath_;
	protected float pathStartTime_;
	protected TruckState truckState_;
	protected boolean autoFinish = true;
	protected int capacity_;

	protected void setup() {
		super.setup();

		currOrder_ = null;
		nextOrder_ = null;
		numOfBoxes_ = 0;
		truckState_ = TruckState.IDLE;
		capacity_ = 0;

		Object args[] = getArguments();
		if (args != null && args.length > 0) {
			capacity_ = Integer.parseInt(args[0].toString());
		}
		System.out.println("Hello! TruckAgent " + getAID().getName() + " with capacity " + capacity_ + " is ready.");

		register("transport-orders", "transport-orders");
		addBehaviour(new TimeQuotationServer());
		addBehaviour(new TruckScheduleServer());
		addBehaviour(new MoveTruck());

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		addBehaviour(new QueryNodePosition(getLocalName().split("_")[0]));
		addBehaviour(new TimeUpdater());

	}

	protected void takeDown() {
		deRegister();

		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

	public String getTruckName() {
		return getLocalName().split("_")[1];
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
		System.out.println(baseAgent.getAID().getLocalName() + " Started executing new order: " + currOrder_.orderID_
				+ " of bakery " + currOrder_.bakeryName_);
	}

	protected void updateCurrPath(ArrayList<float[]> path) {
		currPath_ = path;
		currTruckLocation_ = new float[2];
		currTruckLocation_[0] = currPath_.get(0)[0];
		currTruckLocation_[1] = currPath_.get(0)[1];

		pathStartTime_ = (getCurrentDay() * 24 * 60) + (getCurrentHour() * 60) + getCurrentMinute();
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
			} else {
				streetNwAgent = null;
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
		IDLE, MOVING_TO_BAKERY, MOVING_TO_CUSTOMER
	}

	private class OrderDetails {
		public String orderID_;
		public float[] bakeryLocation_;
		public float[] customerLocation_;
		public int numOfBoxes_;
		public String customerName_ = "No Customer Name Provided";
		public String bakeryName_ = "No Bakery Name Provided";
		public int deliveryDate_;
		public int deliveryHour_;
		public int deliveryMinutes_;

		public OrderDetails(String jsonMessage) {
			JSONObject jsonObj = new JSONObject(jsonMessage);

			orderID_ = jsonObj.getString("OrderID");
			numOfBoxes_ = jsonObj.getInt("NumOfBoxes");
			customerName_ = jsonObj.getString("CustomerId");

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
			System.out.println(
					"Delivery date/time/minutes: " + deliveryDate_ + "." + deliveryHour_ + "." + deliveryMinutes_);
			System.out.println("*******************************");
		}
	}

	private class TimeUpdater extends CyclicBehaviour {
		public void action() {
			if (getAllowAction() && truckState_.equals(TruckState.IDLE)) {
				finished();
			}
		}
	}

	private enum TimeQuotationStates {
		WAIT_FOR_QUOTATION_REQUEST, QUOTATION_REQUESTED, REQUEST_TIME_TO_CUSTOMER, WAIT_FOR_TIME_FROM_STREET_NETWORK, SEND_QUOTATION, SEND_REFUSAL
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
				} else {
					block();
				}
				break;
			case QUOTATION_REQUESTED:
				if (nextOrder_ == null) {
					orderDetails_ = new OrderDetails(requestMsg_.getContent());
					state_ = TimeQuotationStates.REQUEST_TIME_TO_CUSTOMER;
				} else {
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
				break;
			case SEND_QUOTATION:
				ACLMessage reply = requestMsg_.createReply();
				reply.setPerformative(ACLMessage.PROPOSE);
				reply.setContent(Float.toString(timeQuote_));
				baseAgent.sendMessage(reply);
				System.out.println(baseAgent.getAID().getLocalName() + " Sent time to deliver quote: " + timeQuote_);
				resetClassMembers();
				break;
			case SEND_REFUSAL:
				ACLMessage reject = requestMsg_.createReply();
				reject.setPerformative(ACLMessage.REFUSE);
				reject.setContent("Busy");
				baseAgent.sendMessage(reject);
				System.out.println(baseAgent.getAID().getLocalName() + " Rejected Quotation request");
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
				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent("DeliveryAccepted");

				OrderDetails newOrder = new OrderDetails(msg.getContent());
				newOrder.bakeryName_ = msg.getSender().getLocalName().split("_")[0];

				if (currOrder_ == null) {
					currOrder_ = newOrder;
					System.out.println(baseAgent.getAID().getLocalName() + " Accepted new order as CURRENT order:");
				} else if (nextOrder_ == null) {
					nextOrder_ = newOrder;
					System.out.println(baseAgent.getAID().getLocalName() + " Accepted new order as NEXT order:");
				} else {
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("Busy");
					System.out.println(baseAgent.getAID().getLocalName() + " Failed to accept new order");
				}
				baseAgent.sendMessage(reply);
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

				if (currTruckLocation_[0] != currPath_.get(i - 1)[0]
						|| currTruckLocation_[1] != currPath_.get(i - 1)[1]) {
					currTruckLocation_ = new float[2];
					currTruckLocation_[0] = currPath_.get(i - 1)[0];
					currTruckLocation_[1] = currPath_.get(i - 1)[1];
					retval = true;
				}
			}
			if (getAllowAction()) {
				finished();
			}

			return retval;
		}

		private int getTime() {
			return (getCurrentDay() * 24 * 60) + (getCurrentHour() * 60) + getCurrentMinute();
		}

		private boolean reachedEndOfPath() {
			float[] endPos = { currPath_.get(currPath_.size() - 1)[0], currPath_.get(currPath_.size() - 1)[1] };
			return Arrays.equals(currTruckLocation_, endPos);
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
			if ((truckState_ == TruckState.IDLE) && (currOrder_ != null)) {
				startNewOrder(currOrder_);
			} else if ((truckState_ != TruckState.IDLE) && updateTruckPosition()) {
				if (reachedBakery()) {
					baseAgent.addBehaviour(new RequestBoxes(currOrder_.orderID_));
					truckState_ = TruckState.MOVING_TO_CUSTOMER;
					currPath_ = null;
					System.out.println(baseAgent.getAID().getLocalName()
							+ " Reached bakery. Requested boxes from transport agent");
				} else if (reachedCutomer()) {
					currOrder_.deliveryDate_ = getCurrentDay();
					currOrder_.deliveryHour_ = getCurrentHour();
					currOrder_.deliveryMinutes_ = getCurrentMinute();
					baseAgent.addBehaviour(new PostDeliveryCompletionMessage(currOrder_));
					if (nextOrder_ != null) {
						startNewOrder(nextOrder_);
						nextOrder_ = null;
						System.out.println(
								baseAgent.getAID().getLocalName() + " Reached customer. Starting with next request");
					} else {
						currOrder_ = null;
						truckState_ = TruckState.IDLE;
						System.out.println(baseAgent.getAID().getLocalName()
								+ " Reached customer. Truck is Idle as there is no next order");
					}
				}

				baseAgent.addBehaviour(new SendTruckPosForVisualiaztion());
			}
		}
	}

	private enum StreetNetworkQueryStates {
		FIND_STREET_NETWORK_AGENTS, REQUEST_STREET_NETWORK, WAIT_FOR_RESPONSE, QUERY_COMPLETE, QUERY_FAILED
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
			jsonArray.put(new JSONObject().put("X", currTruckLocation_[0]).put("Y", currTruckLocation_[1]));

			// If truck is not idle add the location of current customer
			if (!isTruckIdle()) {
				jsonArray.put(new JSONObject().put("X", currOrder_.customerLocation_[0]).put("Y",
						currOrder_.customerLocation_[1]));
			}

			// Add location of bakery
			jsonArray.put(new JSONObject().put("X", requester_.orderDetails_.bakeryLocation_[0]).put("Y",
					requester_.orderDetails_.bakeryLocation_[1]));

			// Add location of new customer
			jsonArray.put(new JSONObject().put("X", requester_.orderDetails_.customerLocation_[0]).put("Y",
					requester_.orderDetails_.customerLocation_[1]));

			return jsonArray.toString();
		}

		public void action() {
			switch (state_) {
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
				baseAgent.sendMessage(request);
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
					} else {
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
			return state_ == StreetNetworkQueryStates.QUERY_COMPLETE || state_ == StreetNetworkQueryStates.QUERY_FAILED;
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
			jsonArray.put(new JSONObject().put("X", currTruckLocation_[0]).put("Y", currTruckLocation_[1]));

			// Add location of new customer
			jsonArray.put(new JSONObject().put("X", destination_[0]).put("Y", destination_[1]));

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
			switch (state_) {
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
				baseAgent.sendMessage(request);
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

					} else {
						state_ = StreetNetworkQueryStates.QUERY_FAILED;
					}
				}
				break;
			default:
				break;
			}
		}

		public boolean done() {
			return state_ == StreetNetworkQueryStates.QUERY_COMPLETE || state_ == StreetNetworkQueryStates.QUERY_FAILED;
		}
	}

	@SuppressWarnings("unused")
	private class QueryNodePosition extends Behaviour {
		private AID streetNwAgent_;
		private StreetNetworkQueryStates state_ = StreetNetworkQueryStates.FIND_STREET_NETWORK_AGENTS;
		private MessageTemplate mt_;
		private String nodeID_;

		public QueryNodePosition(String nodeID) {
			nodeID_ = nodeID;
		}

		private float[] parseNodePosition(String jsonString) {
			float[] position = new float[2];
			JSONObject jsonObj = new JSONObject(jsonString);
			position[0] = jsonObj.getFloat("x");
			position[1] = jsonObj.getFloat("y");
			return position;
		}

		public void action() {
			switch (state_) {
			case FIND_STREET_NETWORK_AGENTS:
				streetNwAgent_ = discoverAgent("street-network");
				if (streetNwAgent_ != null) {
					state_ = StreetNetworkQueryStates.REQUEST_STREET_NETWORK;
				}
				break;
			case REQUEST_STREET_NETWORK:
				// Send the request to the street network
				String conversationID = "LocationQuery";
				ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
				request.addReceiver(streetNwAgent_);
				request.setContent(nodeID_);
				request.setConversationId(conversationID);
				request.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
				baseAgent.sendMessage(request);
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
						currTruckLocation_ = parseNodePosition(reply.getContent());
						state_ = StreetNetworkQueryStates.QUERY_COMPLETE;

					} else {
						state_ = StreetNetworkQueryStates.QUERY_FAILED;
					}

					baseAgent.addBehaviour(new SendTruckPosForVisualiaztion());
				}
				break;
			default:
				break;
			}
		}

		public boolean done() {
			return state_ == StreetNetworkQueryStates.QUERY_COMPLETE || state_ == StreetNetworkQueryStates.QUERY_FAILED;
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

			baseAgent.addBehaviour(new QueryPath(currOrder_.customerLocation_));
			// TODO
		}

		public void action() {
			switch (state_) {
			case 0:
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				String convID = "ReadyForPickup" + orderID_;
				msg.addReceiver(discoverAgent(currOrder_.bakeryName_ + "-transport-agent")); // TODO fix this services
																								// name
				msg.setContent(generateJsonMessage());
				msg.setConversationId(convID);
				msg.setReplyWith("req" + System.currentTimeMillis());
				baseAgent.sendMessage(msg);
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
					} else {
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
			jsonObj.put("DeliveryStatus", new JSONObject().put("OrderDeliveredTo", orderInfo_.customerName_)
					.put("OrderID", orderInfo_.orderID_).put("OrderDeliveredBy", baseAgent.getAID().getLocalName())
					.put("DayOfDelivery", orderInfo_.deliveryDate_).put("HourOfDelivery", orderInfo_.deliveryHour_)
					.put("MinuteOfDelivery", orderInfo_.deliveryMinutes_).put("NumOfBoxes", orderInfo_.numOfBoxes_)
					.put("ProducedBy", orderInfo_.bakeryName_));

			return jsonObj.toString();
		}

		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(discoverAgent("mailbox"));
			msg.setContent(generateJsonMessage());
			msg.setConversationId("DeliveryConfirmation");
			msg.setPostTimeStamp(System.currentTimeMillis());
			baseAgent.sendMessage(msg);
			System.out.println(baseAgent.getAID().getLocalName() + " Posted message to mailbox" + msg.getContent());
		}
	}

	@SuppressWarnings("unused")
	private class SendTruckPosForVisualiaztion extends OneShotBehaviour {
		private String generateJsonMessage() {
			float eta = 0.0f;
			if (currPath_ != null) {
				float timeSincePathStart = getTime() - pathStartTime_;
				eta = currPath_.get(currPath_.size() - 1)[2] - timeSincePathStart;
			}

			JSONObject jsonObj = new JSONObject();
			jsonObj.put("id", getTruckName());
			jsonObj.put("x", currTruckLocation_[0]);
			jsonObj.put("y", currTruckLocation_[1]);
			jsonObj.put("state", truckState_);
			jsonObj.put("eta", (int) eta);
			return jsonObj.toString();
		}

		private int getTime() {
			return (getCurrentDay() * 24 * 60) + (getCurrentHour() * 60) + getCurrentMinute();
		}

		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(discoverAgent("transport-visualization"));
			msg.setContent(generateJsonMessage());
			msg.setConversationId("TruckPosUpdate");
			msg.setPostTimeStamp(System.currentTimeMillis());
			baseAgent.sendMessage(msg);
		}
	}

	// Taken from
	// http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	@SuppressWarnings("unused")
	private class shutdown extends OneShotBehaviour {
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			baseAgent.getContentManager().registerLanguage(codec);
			baseAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(baseAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
				baseAgent.getContentManager().fillContent(shutdownMessage,
						new Action(baseAgent.getAID(), new ShutdownPlatform()));
				baseAgent.sendMessage(shutdownMessage);
			} catch (Exception e) {
				// LOGGER.error(e);
			}

		}
	}
}
