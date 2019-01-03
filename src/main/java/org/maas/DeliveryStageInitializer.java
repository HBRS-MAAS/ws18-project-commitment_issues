package org.maas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

public class DeliveryStageInitializer extends Initializer {
    @Override
    public String initialize(String scenarioDirectory) {
        Vector<String> agents = new Vector<>();
        
        agents = addTransportAndTruckAgents(agents);
		agents.add("StreetNetworkAgent:org.commitment_issues.delivery_agents.StreetNetworkAgent");
		agents.add("MailboxAgent:org.commitment_issues.delivery_agents.MailboxAgent");
		agents.add("OrderAggregatorAgent:org.commitment_issues.delivery_agents.OrderAggregatorAgent");
		agents.add("customer-001" + ":org.commitment_issues.DummyCustomer");

        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
    }
    
	protected static Vector<String> addTransportAndTruckAgents(Vector<String> agents) {
		File fileRelative = new File("src/main/resources/config/small/delivery.json");
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
					agents.add(companyName + ":org.commitment_issues.delivery_agents.TransportAgent");
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
}
