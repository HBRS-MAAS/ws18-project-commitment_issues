package org.maas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.maas.data.models.Bakery;
import org.maas.utils.JsonConverter;
import com.fasterxml.jackson.core.type.TypeReference;

public class DeliveryStageInitializer extends Initializer {
    @Override
    public String initialize() {
        Vector<String> agents = new Vector<>();
        agents = addTruckAgents(agents, scenarioDirectory);
        
        Vector<String> bakeryNames = this.getBakeryNames(scenarioDirectory);
        for (String bakeryName : bakeryNames) {
            agents.add(bakeryName + "-TransportAgent:org.commitment_issues.delivery_agents.TransportAgent");
    		agents.add(bakeryName + "-StreetNetworkAgent:org.commitment_issues.delivery_agents.StreetNetworkAgent");
    		agents.add(bakeryName + "-OrderAggregatorAgent:org.commitment_issues.delivery_agents.OrderAggregatorAgent");
        }
        agents.add("MailboxAgent:org.commitment_issues.delivery_agents.MailboxAgent");
		agents.add("customer-001" + ":org.commitment_issues.DummyCustomer");

        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
    }
    
	protected static Vector<String> addTruckAgents(Vector<String> agents,
															   String scenarioDirectory) {
		File fileRelative = new File("src/main/resources/config/"+scenarioDirectory+"/delivery.json");
		String data = null;
		try {
			data = new String(Files.readAllBytes(Paths.get(fileRelative.getAbsolutePath())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (data != null) {
			JSONArray deliveryCompanies = new JSONArray(data);
			for (int i = 0; i < deliveryCompanies.length(); i++) {
				JSONObject company = deliveryCompanies.getJSONObject(i);
				String companyName = company.getString("guid");
				JSONArray truckList = company.getJSONArray("trucks");
				if (truckList.length() > 0) {
//					agents.add(companyName + ":org.commitment_issues.delivery_agents.TransportAgent");
					for (int t = 0; t < truckList.length(); t++) {
						String truckID = truckList.getJSONObject(t).getString("guid");
						int capacity = (int)truckList.getJSONObject(t).getFloat("load_capacity");
						agents.add(companyName + "_" + truckID + ":org.commitment_issues.delivery_agents.TruckAgent(" + capacity + ")");
//						break;
					}
				}
				else {
					System.out.println("Skipped Transport Company " + companyName + " as it has no trucks");
				}
			}
		}
		return agents;
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
}
