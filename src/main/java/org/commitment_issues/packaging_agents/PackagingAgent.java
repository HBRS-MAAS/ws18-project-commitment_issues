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
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.yourteamname.agents.BaseAgent;

import jade.core.AID;
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
	private AID loadingBayAgent_ = null;
	private HashMap<String, Integer> itemsPerBox_ = new HashMap<String, Integer>();
	private String bakeryName_;

	protected void setup() {
		super.setup();

		Object args[] = getArguments();
		if (args != null && args.length > 0) {
			bakeryName_ = args[0].toString();
		}

		register("packaging", "packaging");

		while (this.loadingBayAgent_ == null) {
			findLoadingBayAgent();
		}

		determineItemsPerBox();
		loadOrderInfoFromFile();

		System.out.println("All Loaded orders:");
		Iterator<OrderInfo> itr = orderQueue_.iterator();
		while (itr.hasNext()) {
			OrderInfo order = itr.next();
			order.printOrderInfo();
		}

		addBehaviour(new TimeUpdater());
	}

	protected void takeDown() {
		deRegister();

		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

	private void findLoadingBayAgent() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("loading-bay");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			this.loadingBayAgent_ = result[0].getName();

		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

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
		String data = loadFile("src/main/resources/config/small/bakeries.json");
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

		System.out.println("Initialized Packaging agent with " + itemsPerBox_.size() + " product types");
	}

	private void loadOrderInfoFromFile() {
		String data = loadFile("src/main/resources/config/small/orderprocessor.json");

		JSONArray orderList = new JSONArray(data);
		for (int o = 0; o < orderList.length(); o++) {
			OrderInfo newOrder = new OrderInfo(orderList.getJSONObject(o).toString());
			orderQueue_.add(newOrder);
		}
	}

	private class OrderInfo {
		public int deliveryTime_;
		public HashMap<String, Integer> pendingProducts_ = new HashMap<String, Integer>();
		public String orderID_;

		public OrderInfo(String jsonString) {
			JSONObject obj = new JSONObject(jsonString);
			orderID_ = obj.getString("OrderID");
			deliveryTime_ = obj.getJSONObject("delivery_date").getInt("hour");

			JSONArray products = obj.getJSONArray("Products");
			for (int p = 0; p < products.length(); p++) {
				Iterator<String> key = products.getJSONObject(p).keys();
				String productName = key.next();
				int quantity = products.getJSONObject(p).getInt(productName);
				pendingProducts_.put(productName, quantity);
			}
		}

		public void printOrderInfo() {
			System.out.println("*********" + orderID_ + "*********");
			System.out.println("Delivery Time: " + deliveryTime_);
			Iterator<String> it = pendingProducts_.keySet().iterator();
			while (it.hasNext()) {
				String product = it.next();
				System.out.println("Product: " + product + " Quantity: " + pendingProducts_.get(product));
			}
			System.out.println("**********************************");

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
			MessageTemplate mt = MessageTemplate.MatchPerformative(55);
			ACLMessage msg = baseAgent.receive(mt);
			if (msg != null) {
				finished();
			} else {
				block();
			}
		}
	}

}