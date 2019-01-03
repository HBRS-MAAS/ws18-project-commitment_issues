package org.commitment_issues.packaging_agents;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import com.fasterxml.jackson.core.type.TypeReference;

@SuppressWarnings("serial")
public class EntryAgent extends BaseAgent{
	private PriorityQueue<Order> orderQueue_ = new PriorityQueue<Order>(100000, new OrderComparator());
	protected String scenarioDirectory_;
	Vector<String> bakeryNames_;
	int currBakery_ = 0;
	
	protected void setup() {
		super.setup();
		
		Object args[] = getArguments();
		if (args != null && args.length > 0) {
			scenarioDirectory_ = args[0].toString();
		}
		
		bakeryNames_ = getBakeryNames(scenarioDirectory_);

		register("entryAgent", "entryAgent");
		loadAllOrders(scenarioDirectory_);
		System.out.println("[" + getLocalName() + "]: Number of orders loaded = " + orderQueue_.size());
		
		System.out.println("Hello! EntryAgent " + getAID().getName() + " is ready.");
		addBehaviour(new SendOrders());

	}
	
	protected void takeDown() {
		deRegister();

		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
	private class TimeUpdater extends CyclicBehaviour {
		public void action() {
			if (getAllowAction()) {
//				if (!bakeBehaviourAdded) {
//					addBehaviour(new Bake());
//					bakeBehaviourAdded = true;
//				}
				finished();
			} else {
				block();
			}
		}
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
				System.out.println("[" +
						getAID().getLocalName() + "] : No agent with Service type (" + serviceType + ") found!");
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		return loadingBayAgent;
	}
	
    private Vector<String> getBakeryNames (String scenarioDirectory) {
        String filePath = "config/" + scenarioDirectory + "/bakeries.json";
        String fileString = this.readConfigFile(filePath);
        TypeReference<?> type = new TypeReference<Vector<Bakery>>(){};
        Vector<Bakery> bakeries = JsonConverter.getInstance(fileString, type);
        Vector<String> bakeryNames = new Vector<String> (bakeries.size());
        for (Bakery bakery : bakeries) {
            bakeryNames.add(bakery.getGuid());
        }
        return bakeryNames;
    }
    
    private String readConfigFile (String filePath){
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
		File relativePath = new File("src/main/resources/config/"+ scenarioDirectory + "/clients.json");
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
		public JSONObject products;
		
		public Order(JSONObject object) {
			guid = object.getString("guid");
			customerID = object.getString("customer_id");
			orderDay = object.getJSONObject("order_date").getInt("day");
			orderHour = object.getJSONObject("order_date").getInt("hour");
			orderMinute = object.getJSONObject("order_date").getInt("minute");
			products = new JSONObject();
			products.put("products", object.getJSONObject("products"));
		}
		
		public int getOrderTime() {
			return getGlobalOrderTime(orderDay, orderHour, orderMinute);
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
						String currBakery = bakeryNames_.get(currBakery_);
						currBakery_ = (currBakery_ >= bakeryNames_.size()) ? 0 : (currBakery_+1);
						AID receiver = discoverAgent(currBakery + "-cooling-rack");
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent(orders.get(o).products.toString());
						msg.setConversationId("bake");
						msg.addReceiver(receiver);
						baseAgent.send(msg);
						System.out.println("[" + getLocalName() + "]: Sent orders to " + receiver.getLocalName() + "\n" + msg.getContent());
					}
					
				}
				finished();
			} else {
				block();
			}
		}
		
		protected ArrayList<Order> getOrdersForCurrentTimeStep() {
			ArrayList<Order> orders = new ArrayList<Order>();
			ArrayList<Order> ordersToRemove = new ArrayList<Order>();
			
			int currGlobalOrderTime = getGlobalOrderTime(getCurrentDay(),
														 getCurrentHour(), 
														 getCurrentMinute());
			Iterator<Order> itr = orderQueue_.iterator();
			while (itr.hasNext()) {
				Order order = itr.next();
				System.out.println("Best Order Scheduled for: " + order.orderDay +  "." + order.orderHour + "." + order.orderMinute);
				if (currGlobalOrderTime == order.getOrderTime()) {
					orders.add(order);
					ordersToRemove.add(order);
				}
				else if (currGlobalOrderTime > order.getOrderTime()) {
					System.out.println("[" + getLocalName() + "]: Discarded order " + order.guid +
							" with order time " + order.orderDay + "." + order.orderHour + "." + order.orderMinute);
					ordersToRemove.add(order);
				}
				else {
					// Put the extracted order back in the queue
//					orderQueue_.add(order);
					break;
				}
			}
			
			orderQueue_.removeAll(ordersToRemove);
			
			return orders;
		}
	}
	
	

}
