package org.commitment_issues.packaging_agents;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Vector;

import org.commitment_issues.agents.CustomerAgent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.maas.agents.BaseAgent;
import org.maas.data.models.Bakery;
import org.maas.utils.JsonConverter;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
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

import com.fasterxml.jackson.core.type.TypeReference;

@SuppressWarnings("serial")
public class EntryAgent extends BaseAgent {
	private PriorityQueue<Order> orderQueue_ = new PriorityQueue<Order>(100000, new OrderComparator());
	protected String scenarioDirectory_;
	protected Vector<String> bakeryNames_ = new Vector<String>();
	protected int currBakery_ = 0;

	protected void setup() {
		super.setup();

		Object args[] = getArguments();
		if (args != null && args.length > 0) {
			scenarioDirectory_ = args[0].toString();
		}

//		bakeryNames_ = getBakeryNames(scenarioDirectory_);
		bakeryNames_.add(getBakeryNames(scenarioDirectory_).get(0));

		// This agent also acts as a dummy order processor. Therefore this name.
		register("OrderProcessing", "OrderProcessing");
		loadAllOrders(scenarioDirectory_);
		System.out.println("[" + getLocalName() + "]: Number of orders loaded = " + orderQueue_.size());

		addBehaviour(new SendOrders());
		System.out.println("Hello! EntryAgent " + getAID().getName() + " is ready.");
	}

	protected void takeDown() {
		deRegister();

		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

	protected AID discoverAgent(String serviceType) {
		// Find the an agent for given service type
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(serviceType);
		template.addServices(sd);

		AID loadingBayAgent = null;

		try {
			DFAgentDescription[] result = DFService.search(baseAgent, template);
			if (result.length > 0) {
				loadingBayAgent = result[0].getName();
			} else {
				loadingBayAgent = null;
				System.out.println(
						"[" + getAID().getLocalName() + "] : No agent with Service type (" + serviceType + ") found!");
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		return loadingBayAgent;
	}

	private Vector<String> getBakeryNames(String scenarioDirectory) {
		String filePath = "config/" + scenarioDirectory + "/bakeries.json";
		String fileString = this.readConfigFile(filePath);
		TypeReference<?> type = new TypeReference<Vector<Bakery>>() {
		};
		Vector<Bakery> bakeries = JsonConverter.getInstance(fileString, type);
		Vector<String> bakeryNames = new Vector<String>(bakeries.size());
		for (Bakery bakery : bakeries) {
			bakeryNames.add(bakery.getGuid());
		}
		return bakeryNames;
	}

	private String readConfigFile(String filePath) {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(filePath).getFile());
		String fileString = "";
		try (Scanner sc = new Scanner(file)) {
			sc.useDelimiter("\\Z");
			fileString = sc.next();
			sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileString;
	}

	private void loadAllOrders(String scenarioDirectory) {
		File relativePath = new File("src/main/resources/config/" + scenarioDirectory + "/clients.json");
		String read = CustomerAgent.readFileAsString(relativePath.getAbsolutePath());
		JSONArray clientsArray = new JSONArray(read);

		for (int c = 0; c < clientsArray.length(); c++) {
			JSONArray orders = clientsArray.getJSONObject(c).getJSONArray("orders");
			for (int o = 0; o < orders.length(); o++) {
				Order newOrder = new Order(orders.getJSONObject(o));
				orderQueue_.add(newOrder);
			}
		}
	}

	protected int getGlobalOrderTime(int day, int hour, int minute) {
		return minute + (hour * 60) + (day * 24 * 60);
	}

	private class Order {
		public String guid;
		public String customerID;
		public int orderDay;
		public int orderHour;
		public int orderMinute;
		public int deliveryDay;
		public int deliveryHour;
		public int deliveryMinute;
		public JSONObject products;

		public Order(JSONObject object) {
			guid = object.getString("guid");
			customerID = object.getString("customer_id");
			orderDay = object.getJSONObject("order_date").getInt("day");
			orderHour = object.getJSONObject("order_date").getInt("hour");
//			orderMinute = object.getJSONObject("order_date").getInt("minute");
			deliveryDay = object.getJSONObject("delivery_date").getInt("day");
			deliveryHour = object.getJSONObject("delivery_date").getInt("hour");
//			deliveryMinute = object.getJSONObject("delivery_date").getInt("minute");
			products = new JSONObject();
			products.put("products", object.getJSONObject("products"));
		}

		public int getOrderTime() {
			return getGlobalOrderTime(orderDay, orderHour, 0);
		}
	}

	private class OrderComparator implements Comparator<Order> {
		@Override
		public int compare(Order x, Order y) {
			if (x.getOrderTime() < y.getOrderTime()) {
				return -1;
			}
			if (x.getOrderTime() > y.getOrderTime()) {
				return 1;
			}
			return 0;
		}
	}

	private class SendOrders extends CyclicBehaviour {
		public void action() {
			if (getAllowAction()) {
				ArrayList<Order> orders = getOrdersForCurrentTimeStep();
				if (orders.size() > 0) {
					for (int o = 0; o < orders.size(); o++) {
						baseAgent.addBehaviour(new BroadcastOrder(orders.get(o)));
						String currBakery = bakeryNames_.get(currBakery_);
						currBakery_ = (currBakery_ >= bakeryNames_.size()-1) ? 0 : (currBakery_ + 1);
						AID receiver = discoverAgent(currBakery + "-cooling-rack");
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent(orders.get(o).products.toString());
						msg.setConversationId("bake");
						msg.addReceiver(receiver);
						baseAgent.sendMessage(msg);
						System.out.println("[" + getLocalName() + "]: Sent orders to " + receiver.getLocalName() + "\n"
								+ msg.getContent());
					}

				}
				finished();
			}
		}

		protected ArrayList<Order> getOrdersForCurrentTimeStep() {
			ArrayList<Order> orders = new ArrayList<Order>();
			ArrayList<Order> ordersToRemove = new ArrayList<Order>();

			int currGlobalOrderTime = getGlobalOrderTime(getCurrentDay(), getCurrentHour(), 0);
			Iterator<Order> itr = orderQueue_.iterator();
			while (itr.hasNext()) {
				Order order = itr.next();
				System.out.println("Best Order Scheduled for: " + order.orderDay + "." + order.orderHour + "."
						+ order.orderMinute);
				if (currGlobalOrderTime == order.getOrderTime()) {
					orders.add(order);
					ordersToRemove.add(order);
				} else if (currGlobalOrderTime > order.getOrderTime()) {
					System.out.println("[" + getLocalName() + "]: Discarded order " + order.guid + " with order time "
							+ order.orderDay + "." + order.orderHour + "." + order.orderMinute);
					ordersToRemove.add(order);
				} else {
					break;
				}
			}

			orderQueue_.removeAll(ordersToRemove);
			if (orderQueue_.size() <= 0) {
				baseAgent.addBehaviour(new shutdown());
			}

			return orders;
		}
	}

	private AID[] findAllAgents() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		template.addServices(sd);
		AID[] allAgents = null;
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			allAgents = new AID[result.length];
			int counter = 0;
			for (DFAgentDescription ad : result) {
				allAgents[counter] = ad.getName();
				counter++;
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
			allAgents = new AID[0];
		}

		return allAgents;
	}

	private class BroadcastOrder extends OneShotBehaviour {
		protected JSONObject orderObj;

		public BroadcastOrder(Order order) {
			super();
			this.orderObj = getJSONObject(order);
		}

		protected JSONObject getJSONObject(Order order) {
			JSONObject object = new JSONObject();

			JSONObject orderDate = new JSONObject();
			orderDate.put("day", order.orderDay);
			orderDate.put("hour", order.orderHour);

			JSONObject deliveryDate = new JSONObject();
			deliveryDate.put("day", order.deliveryDay);
			deliveryDate.put("hour", order.deliveryHour);

			object.put("customerId", order.customerID);
			object.put("guid", order.guid);
			object.put("orderDate", orderDate);
			object.put("deliveryDate", deliveryDate);
			object.put("products", order.products.getJSONObject("products"));
			return object;
		}

		@Override
		public void action() {
			AID[] allAgents = findAllAgents();
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent(orderObj.toString());
			for (AID agent : allAgents) {
				msg.addReceiver(agent);
			}
			sendMessage(msg);
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
				baseAgent.sendMessage(shutdownMessage);
			}
			catch (Exception e) {
				//LOGGER.error(e);
			}

		}
	}
}
