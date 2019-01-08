package org.commitment_issues.packaging_agents;

import java.io.File;
import java.util.ArrayList;

import org.commitment_issues.agents.CustomerAgent;
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
public class GenericItemProcessor extends BaseAgent {
  private ArrayList<Product> allProducts = new ArrayList<Product>();
  private boolean isCoolingRack;
  private AID targetAgent;
  private boolean productsToProcessBehaviorAdded = false;
  protected String scenarioDirectory_;
  protected void setup() {
    super.setup();
    System.out.println("Hello! GenericItemProcessor "+ getAID().getName() +" is ready.");
    
    Object[] args = getArguments();
    if (args != null && args.length > 0) {
    	scenarioDirectory_ = args[0].toString();
    	
    	if (args.length > 1) {
    		isCoolingRack = true;
    }else {
    	    isCoolingRack = false;
    	}
    }
    
    productsStepReader();
    
    if (isCoolingRack) {
      register(getBakeryName() + "-cooling-rack", getBakeryName() + "-cooling-rack");
    }else {
      register(getBakeryName() + "-generic-rack", getBakeryName() + "-generic-rack");
    }
    addBehaviour(new TimeUpdater());
  }
  
  public String getBakeryName() {
	  return getLocalName().split("_")[0];
  }
  private class TimeUpdater extends CyclicBehaviour {
    public void action() {
      //System.out.println(myAgent.getName()+"---------------time update");
      if (getAllowAction()) {
    	  if (!productsToProcessBehaviorAdded) {
    		    addBehaviour(new ProductsToProcess());
    		    productsToProcessBehaviorAdded = true;
    	  }
        finished();
      } 
    }
  }
  private void findTargetAgent(String service) {
    DFAgentDescription template = new DFAgentDescription();
    ServiceDescription sd = new ServiceDescription();
    sd.setType(service);
    template.addServices(sd);
    try {
      DFAgentDescription[] result = DFService.search(this, template);
      if (result.length > 0) {
    	  this.targetAgent = result[0].getName();
      }
      
      if (this.targetAgent == null) {
    	  System.out.println("No agent with service type " + service + " found.");
      }
     
    } catch (FIPAException fe) {
      fe.printStackTrace();
    }
  }
  protected void register(String type, String name){
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType(type);
    sd.setName(name);
    dfd.addServices(sd);
    try {
        DFService.register(this, dfd);
    }
    catch (FIPAException fe) {
        fe.printStackTrace();
    }
}
  protected void takeDown() {
    deRegister();
    
    System.out.println(getAID().getLocalName() + ": Terminating.");
  }
  protected void deRegister() {
    try {
          DFService.deregister(this);
      }
      catch (FIPAException fe) {
          fe.printStackTrace();
      }
  }
  protected void productsStepReader() {
    File relativePath = new File("src/main/resources/config/" + scenarioDirectory_ + "/bakeries.json");
    String read = CustomerAgent.readFileAsString(relativePath.getAbsolutePath());
    JSONArray bakeriesJSON = new JSONArray(read);
    JSONObject bakery = bakeriesJSON.getJSONObject(0);
    JSONArray products = bakery.getJSONArray("products");
    for(int i = 0; i < products.length();i++) {
      Product p = new Product();
      p.setName(products.getJSONObject(i).getString("guid"));
      JSONObject recipieJSON = products.getJSONObject(i).getJSONObject("recipe");
      JSONArray processes = recipieJSON.getJSONArray("steps");
      boolean add = false;
      for (int k = 0; k < processes.length(); k++) {
        if (processes.getJSONObject(k).getString("action").equals("cooling")) {
          add = true;
        }
        if (add) {
          Task t = new Task();
          t.setName(processes.getJSONObject(k).getString("action"));
          t.setDuration(processes.getJSONObject(k).getInt("duration"));
          //System.out.println(p.getName());
          //System.out.println(processes.getJSONObject(k).getString("action"));
          //System.out.println(processes.getJSONObject(k).getInt("duration"));
          p.addProcess(t);
          
        }
      }
      this.allProducts.add(p);
      
    }
  }
  private class ProductsToProcess extends CyclicBehaviour{
    private ACLMessage ordersToPrepare;
    private ArrayList<Product> products = new ArrayList<Product>();
    @Override
    public void action() {
     
      MessageTemplate mt = MessageTemplate.MatchConversationId("bake");
      ordersToPrepare = myAgent.receive(mt);
      if(ordersToPrepare != null) {
    	//System.out.println("[" + getLocalName() + "]: Received products from " + ordersToPrepare.getSender().getLocalName());
    	
        products = new ArrayList<Product>();
        JSONObject productJSON = new JSONObject(ordersToPrepare.getContent());
        JSONObject productsJSON = productJSON.getJSONObject("products");
        int productsSize = productsJSON.keySet().size();
        //System.out.println(productsSize);
        for (int i = 0; i < productsSize; i++) {
          Product p = new Product();
          String key = productsJSON.keys().next();
          //System.out.println(key);
          p.setAmount(productsJSON.getInt(key));
          p.setName(key);
          productsJSON.keySet().remove(key);
          for(int k = 0; k < allProducts.size();k++) {
            if(p.getName().equals(allProducts.get(k).getName())) {
              p.setProcesses(allProducts.get(k).getProcesses());
              break;
            }
          }
          products.add(p);
          //System.out.println(products.size());
        }
        myAgent.addBehaviour(new ProductListHandler(products));
        
        
      }
      else {
        block();
      }
      
    }
    
  }
  
  private class ProductListHandler extends OneShotBehaviour{
    private ArrayList<Product> products = new ArrayList<Product>();
    
    public ProductListHandler(ArrayList<Product> products) {
     this.products = products;
    }

    @Override
    public void action() {
      if (isCoolingRack) {
        for (int i = 0; i < this.products.size();i++) {
          myAgent.addBehaviour(new CoolingTask(products.get(i),24*60*getCurrentDay()+getCurrentHour()*60 + getCurrentMinute()));
          //System.out.println("[" + getLocalName() + "]: Cooling of "+products.get(i).getName()+" started at "+Integer.toString(getCurrentDay())+":"+Integer.toString(getCurrentHour())+":"+Integer.toString(getCurrentMinute())); 

        }
      }
      else {
        for (int i = 0; i < this.products.size();i++) {
          //System.out.println("[" + getLocalName() + "]: " + products.get(i).getProcesses().get(1).getName()+" of "+products.get(i).getName()+" started at "+ Integer.toString(getCurrentDay())+":"+Integer.toString(getCurrentHour())+":"+Integer.toString(getCurrentMinute()));
          myAgent.addBehaviour(new GenericTask(products.get(i), 1));
        }
      }
      
    }
    
  }
  private class CoolingTask extends Behaviour{
    private Product p;
    private int time;
    private int startTime;
    private boolean complete = false;
    public CoolingTask(Product p,int startTime) {
      this.p = p;
      this.time = p.getProcesses().get(0).getDuration();
      this.startTime = startTime;
    }
    @Override
    public void action() {
      findTargetAgent(getBakeryName() + "-generic-rack");
      int timeDiff = (getCurrentMinute()+getCurrentHour()*60+getCurrentDay()*24*60)-this.startTime;
      boolean done = false;
      if (timeDiff >= this.time && !done) {
        done = true;
        ACLMessage msg = new ACLMessage(234);
        msg.addReceiver(targetAgent);
        JSONObject x = new JSONObject();
        JSONObject y = new JSONObject();
        y.put(this.p.getName(),this.p.getAmount());
        x.put("products", y);
        //System.out.println("[" + getLocalName() + "]: Cooling of "+p.getName()+" is done and sent to the next stage"); 

        msg.setContent(x.toString());
        msg.setConversationId("bake");
        myAgent.send(msg);
        complete = true;
      }
      
    }
    @Override
    public boolean done() {
      
      return complete;
    }
    
  }
  
  private class GenericTask extends Behaviour{
    private Product p;
    private int step;
    private int time;
    private int startTime;
    private boolean complete = false;
    public GenericTask(Product product, int step) {
      this.p = product;
      this.step = step;
      this.time = this.p.getProcesses().get(step).getDuration();
      this.startTime = getCurrentMinute()+60*getCurrentHour()+getCurrentDay()*24*60;
    }

    @Override
    public void action() {
      int timeDiff = getCurrentMinute()+60*getCurrentHour()+getCurrentDay()*24*60-this.startTime;
      if (timeDiff >= this.time) {
        
        
        if (this.step == this.p.getProcesses().size()-1) {
          findTargetAgent(getBakeryName() + "-packaging");
          ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
          msg.addReceiver(targetAgent);
          msg.setConversationId("items-to-pack");
          JSONObject y = new JSONObject();
          y.put(this.p.getName(),this.p.getAmount());
          msg.setContent(y.toString());
          myAgent.send(msg);
          //System.out.println("[" + getLocalName() + "]: " + p.getProcesses().get(step).getName()+" of "+p.getName()+" is done and sent to the packaging"); 
          complete = true;
        }else {
          this.step++;
          //System.out.println("[" + getLocalName() + "]: " + p.getProcesses().get(step-1).getName()+" of "+p.getName()+" is done and sent to the " +p.getProcesses().get(step).getName()); 
          myAgent.addBehaviour(new GenericTask(p, step));
          //System.out.println("[" + getLocalName() + "]: " + p.getProcesses().get(step).getName()+" of "+p.getName()+" started at "+ Integer.toString(getCurrentDay())+":"+Integer.toString(getCurrentHour())+":"+Integer.toString(getCurrentMinute()));
          complete = true;
        }
      } 
      
    }

    @Override
    public boolean done() {
      // TODO Auto-generated method stub
      return complete;
    }
    
  }
  
  // A helper class to represent the products with there amounts after recieving them
  private class Product{
    private String name;
    private int amount;
    private ArrayList<Task> processes;
    public ArrayList<Task> getProcesses() {
      return processes;
    }


    public void setProcesses(ArrayList<Task> processes) {
      this.processes = processes;
    }
    public void addProcess(Task t) {
      this.processes.add(t);
    }

    public Product() {
      super();
      this.processes =new ArrayList<Task>();
    }
    
    
    
    public String getName() {
      return name;
    }
    public void setName(String name) {
      this.name = name;
    }
    public int getAmount() {
      return amount;
    }
    public void setAmount(int amount) {
      this.amount = amount;
    }
  }
  private class Task{
    private String name;
    private int duration;
    
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getDuration() {
      return duration;
    }

    public void setDuration(int duration) {
      this.duration = duration;
    }

//    public Task() {
//      super();
//      // TODO Auto-generated constructor stub
//    }

//    public Task(String name, int duration) {
//      super();
//      this.name = name;
//      this.duration = duration;
//    }
    
  }
}
