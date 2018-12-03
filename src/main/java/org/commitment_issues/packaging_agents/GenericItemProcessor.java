package org.commitment_issues.packaging_agents;

import java.io.File;
import java.util.ArrayList;

import org.commitment_issues.agents.CustomerAgent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yourteamname.agents.BaseAgent;
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
  protected void setup() {
    super.setup();
    System.out.println("Hello! GenericItemProcessor "+ getAID().getName() +" is ready.");
    productsStepReader();
    Object[] args = getArguments();
    if (args != null && args.length > 0) {
      isCoolingRack = true;
    }else {
      isCoolingRack = false;
    }
    if (isCoolingRack) {
      register("cooling-rack", "cooling-rack");
    }else {
      register("generic-rack", "generic-rack");
    }
    addBehaviour(new ProductsToProcess());
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
    File relativePath = new File("src/main/resources/config/small/bakeries.json");
    String read = CustomerAgent.readFileAsString(relativePath.getAbsolutePath());
    JSONArray bakeriesJSON = new JSONArray(read);
    JSONObject bakery = bakeriesJSON.getJSONObject(0);
    JSONArray products = bakery.getJSONArray("products");
    for(int i = 0; i < products.length();i++) {
      Product p = new Product();
      p.setName(products.getJSONObject(i).getString("guide"));
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
          p.addProcess(t);
          
        }
      }
      this.allProducts.add(p);
      
    }
  }
  private class ProductsToProcess extends CyclicBehaviour{
    ACLMessage ordersToPrepare;
    private ArrayList<Product> products = new ArrayList<Product>();
    @Override
    public void action() {
      MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

      ordersToPrepare = myAgent.receive(mt);
      if(ordersToPrepare != null) {
        JSONArray productsJSON = new JSONArray(ordersToPrepare.getContent());
        for (int i = 0; i < productsJSON.length(); i++) {
          JSONObject product = productsJSON.getJSONObject(i);
          Product p = new Product();
          p.setAmount(product.getInt("Quantity"));
          p.setName(product.getString("Name"));
          for(int k = 0; k < allProducts.size();k++) {
            if(p.getName().equals(allProducts.get(k).getName())) {
              p.setProcesses(allProducts.get(k).getProcesses());
            }
          }
          products.add(p);
        }
        myAgent.addBehaviour(new ProductListHandler(products));
        
      }
      else {
        block();
      }
      
    }
    
  }
  
  private class ProductListHandler extends OneShotBehaviour{
    ArrayList<Product> products = new ArrayList<Product>();
    
    public ProductListHandler(ArrayList<Product> products) {
     this.products = products;
    }

    @Override
    public void action() {
      if (isCoolingRack) {
        
      }
      else {
        
      }
      
    }
    
  }
  
  // A helper class to represent the products with there amounts after recieving them
  private class Product{
    String name;
    int amount;
    ArrayList<Task> processes;
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
    }
    
    
    public Product(String name, int amount) {
      this.name = name;
      this.amount = amount;
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
    String name;
    int duration;
    
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

    public Task() {
      super();
      // TODO Auto-generated constructor stub
    }

    public Task(String name, int duration) {
      super();
      this.name = name;
      this.duration = duration;
    }
    
  }
}
