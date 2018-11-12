package org.commitment_issues;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;

import org.json.*;

public class Start {
    public static void main(String[] args) {
    	List<String> agents = new Vector<>();
    	
    	// Initializing customer agents based on clients.json
    	String filePath = "/home/ahmed/Desktop/H-BRS/Semester 2/Multiagent and Agent Systems/ws18-project-commitment_issues/src/main/resources/config/sample/clients.json";
    	String clientFileData = "";
    	try {
			clientFileData = new String(Files.readAllBytes(Paths.get(filePath)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	JSONArray clientDetailsJSONArray = new JSONArray(clientFileData);
    	
    	for (int i = 0 ; i < clientDetailsJSONArray.length(); i++) {
    		agents.add(clientDetailsJSONArray.getJSONObject(i).getString("guid")+":org.commitment_issues.agents.CustomerAgent");
		}

    	agents.add("SchedulerAgent1:org.commitment_issues.agents.SchedulerAgent");
    	agents.add("orderprocessor1:org.commitment_issues.agents.OrderProcessorAgent");
    	agents.add("ProoferAgent1:org.commitment_issues.agents.ProoferAgent");
    	agents.add("CoolingRacksAgent1:org.commitment_issues.agents.CoolingRacksAgent");
    	agents.add("LoadingBayAgentAgent1:org.commitment_issues.agents.LoadingBayAgent");
    	agents.add("MailboxAgent1:org.commitment_issues.agents.MailboxAgent");

    	List<String> cmd = new Vector<>();
    	cmd.add("-agents");
    	StringBuilder sb = new StringBuilder();
    	for (String a : agents) {
    		sb.append(a);
    		sb.append(";");
    	}
    	cmd.add(sb.toString());
        jade.Boot.main(cmd.toArray(new String[cmd.size()]));
    }
}
