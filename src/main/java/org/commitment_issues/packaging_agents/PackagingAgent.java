package org.commitment_issues.packaging_agents;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import org.commitment_issues.delivery_agents.Box;
import org.json.JSONArray;
import org.json.JSONObject;
import org.maas.agents.BaseAgent;

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

@SuppressWarnings("serial")
public class PackagingAgent extends BaseAgent {
	private PriorityQueue<OrderInfo> orderQueue_ = new PriorityQueue<OrderInfo>(1000, new OrderComparator());
	private HashMap<String, Integer> itemsPerBox_ = new HashMap<String, Integer>();
	private String bakeryName_;
	private int boxCount_ = 0;
	protected String scenarioDirectory_;

	protected void setup() {
		super.setup();

		Object args[] = getArguments();
		if (args != null && args.length > 0) {
			bakeryName_ = args[0].toString();

			if (args.length > 1) {
				scenarioDirectory_ = args[1].toString();
			}
		}

		register(getBakeryName() + "-packaging", getBakeryName() + "-packaging");

		determineItemsPerBox();
		addBehaviour(new OrderDetailsReceiver());

		addBehaviour(new ProductsReceiver());
		addBehaviour(new TimeUpdater());

		System.out.println("Hello! PackagingAgent " + getAID().getLocalName() + " is ready.");
	}

	public String getBakeryName() {
		return getLocalName().split("_")[0];
	}

	protected void takeDown() {
		deRegister();

		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

	private String loadFile(String fileName) {
		File fileRelative = new File(fileName);
		String data = null;
		try {
			data = new String(Files.readAllBytes(Paths.get(fileRelative.getAbsolutePath())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return data;
	}

	private void determineItemsPerBox() {
		String data = loadFile("src/main/resources/config/" + scenarioDirectory_ + "/bakeries.json");
		JSONArray bakeries = new JSONArray(data);
		for (int b = 0; b < bakeries.length(); b++) {
			JSONObject bakeryDetails = bakeries.getJSONObject(b);
			if (bakeryDetails.get("guid").equals(bakeryName_)) {
				JSONArray productList = bakeryDetails.getJSONArray("products");
				for (int p = 0; p < productList.length(); p++) {
					String productName = productList.getJSONObject(p).getString("guid");
					int itemsPerBox = productList.getJSONObject(p).getJSONObject("packaging").getInt("breadsPerBox");
					itemsPerBox_.put(productName, itemsPerBox);
				}
			}
		}
	}

	private class OrderInfo {
		public int deliveryTime_;
		public HashMap<String, Integer> pendingProducts_ = new HashMap<String, Integer>();
		public HashMap<String, ArrayList<Box>> boxes_ = new HashMap<String, ArrayList<Box>>();
		public String orderID_;

		public OrderInfo(String jsonString) {
			JSONObject obj = new JSONObject(jsonString);
			orderID_ = obj.getString("guid");
			deliveryTime_ = obj.getJSONObject("deliveryDate").getInt("hour");

			JSONObject products = obj.getJSONObject("products");
			Iterator<String> keyItr = products.keys();
			while (keyItr.hasNext()) {
				String product = keyItr.next();
				int quantity = products.getInt(product);
				pendingProducts_.put(product, quantity);
			}
		}

		public void printOrderInfo() {
			System.out.println("*********" + orderID_ + "*********");
			System.out.println("Delivery Time: " + deliveryTime_);
			Iterator<String> it = pendingProducts_.keySet().iterator();
			while (it.hasNext()) {
				String product = it.next();
				System.out.println("Product: " + product + " Pending Quantity: " + pendingProducts_.get(product));
			}
			System.out.println("**********************************");
		}

		public boolean requiresProduct(String productID) {
			return pendingProducts_.containsKey(productID) && (pendingProducts_.get(productID) > 0);
		}

		public int takeAsRequired(String productID, int quantity) {
			if (pendingProducts_.containsKey(productID)) {
				int requirement = pendingProducts_.get(productID);
				int acceptedItemCount = 0;
				if (requirement >= quantity) {
					acceptedItemCount = quantity;
					requirement -= quantity;
					quantity = 0;
				} else {
					acceptedItemCount = requirement;
					quantity = quantity - requirement;
					requirement = 0;
				}
				pendingProducts_.put(productID, requirement);

				while (acceptedItemCount > 0) {
					Box box = getFreeBox(productID);
					acceptedItemCount = box.addItems(acceptedItemCount);
				}
			}
			return quantity;
		}

		private Box getFreeBox(String productID) {
			ArrayList<Box> boxList = boxes_.get(productID);

			if (boxList == null || boxList.size() == 0) {
				boxList = new ArrayList<Box>();
				boxList.add(getNewBox(productID));
				boxes_.put(productID, boxList);
			} else if (boxList.get(boxList.size() - 1).getFreeSpace() <= 0) {
				boxList.add(getNewBox(productID));
			}

			return boxList.get(boxList.size() - 1);

		}

		private Box getNewBox(String productID) {
			String boxID = bakeryName_ + "_Box_" + Integer.toString(boxCount_);
			boxCount_ += 1;
			return new Box(boxID, productID, 0, itemsPerBox_.get(productID));
		}

		public ArrayList<Box> extractBoxes() {
			ArrayList<Box> boxList = new ArrayList<Box>();
			Iterator<String> it = boxes_.keySet().iterator();
			while (it.hasNext()) {
				String productID = it.next();
				boxList.addAll(boxes_.get(productID));
				boxes_.get(productID).clear();
			}
			return boxList;
		}
	}

	private class OrderComparator implements Comparator<OrderInfo> {
		@Override
		public int compare(OrderInfo x, OrderInfo y) {
			if (x.deliveryTime_ < y.deliveryTime_) {
				return -1;
			}
			if (x.deliveryTime_ > y.deliveryTime_) {
				return 1;
			}
			return 0;
		}
	}

	private class TimeUpdater extends CyclicBehaviour {
		public void action() {
			if (getAllowAction()) {
				finished();
			}
		}
	}

	private class ProductsReceiver extends CyclicBehaviour {

		protected void addProductsToOrders(String jsonString) {
			JSONObject obj = new JSONObject(jsonString);
			Iterator<String> keyItr = obj.keys();
			while (keyItr.hasNext()) {
				String product = keyItr.next();
				int quantity = obj.getInt(product);

				while (quantity > 0) {
					Iterator<OrderInfo> itr = orderQueue_.iterator();
					while (itr.hasNext()) {
						OrderInfo order = itr.next();
						if (order.requiresProduct(product)) {
							quantity = order.takeAsRequired(product, quantity);
							if (quantity <= 0) {
								break;
							}
						}
					}
				}
			}
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("items-to-pack");
			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) {
				addProductsToOrders(msg.getContent());
				baseAgent.addBehaviour(new SendBoxes());
			} else {
				block();
			}
		}
	}

	private class SendBoxes extends OneShotBehaviour {

		protected ArrayList<String> generateMessages() {
			ArrayList<String> messages = new ArrayList<String>();

			Iterator<OrderInfo> itr = orderQueue_.iterator();
			while (itr.hasNext()) {
				OrderInfo order = itr.next();
				ArrayList<Box> boxes = order.extractBoxes();
				if (!boxes.isEmpty()) {
					JSONArray boxArray = new JSONArray();
					for (Box box : boxes) {
						boxArray.put(box.getAsJSONObject());
					}

					JSONObject msgObject = new JSONObject();
					msgObject.put("OrderID", order.orderID_);
					msgObject.put("Boxes", boxArray);
					messages.add(msgObject.toString());
				}
			}

			return messages;
		}

		protected AID discoverAgent(String serviceType) {
			// Find the an agent for given service type
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(serviceType);
			template.addServices(sd);

			AID loadingBayAgent = null;

			while (loadingBayAgent == null) {
				try {
					DFAgentDescription[] result = DFService.search(baseAgent, template);
					if (result.length > 0) {
						loadingBayAgent = result[0].getName();
					} else {
						loadingBayAgent = null;
						System.out.println(
								getAID().getLocalName() + ": No agent with Service type (" + serviceType + ") found!");
					}
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}
			}

			return loadingBayAgent;
		}

		public void action() {
			ArrayList<String> messageList = generateMessages();
			for (int i = 0; i < messageList.size(); i++) {
				String message = messageList.get(i);
				if ((message != null) && !message.isEmpty()) {
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.addReceiver(discoverAgent(getBakeryName() + "-loading-bay"));
					msg.setContent(message);
					msg.setConversationId("boxes-ready");
					msg.setPostTimeStamp(System.currentTimeMillis());
					baseAgent.sendMessage(msg);
					System.out.println(baseAgent.getAID().getLocalName() + " Sent boxes to loading bay");
				}
			}
		}

	}

	private class OrderDetailsReceiver extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("order"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) {
				OrderInfo newOrder = new OrderInfo(msg.getContent());
				orderQueue_.add(newOrder);
			} else {
				block();
			}
		}
	}
}
