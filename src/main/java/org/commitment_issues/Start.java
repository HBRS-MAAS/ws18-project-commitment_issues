package org.commitment_issues;

import java.io.File;
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
    	File clientFileRelative = new File("src/main/resources/config/sample/clients.json");
    	String clientFilePath = clientFileRelative.getAbsolutePath();
    	String clientFileData = "";
    	try {
			clientFileData = new String(Files.readAllBytes(Paths.get(clientFilePath)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	JSONArray clientDetailsJSONArray = new JSONArray(clientFileData);
    	
    	for (int i = 0 ; i < clientDetailsJSONArray.length(); i++) {
    		agents.add(clientDetailsJSONArray.getJSONObject(i).getString("guid")+":org.commitment_issues.agents.CustomerAgent");
		}
    	
    	// Initializing bakery agents based on bakeries.json
    	File bakeriesFileRelative = new File("src/main/resources/config/sample/bakeries.json");
    	
    	String bakeriesFilePath = bakeriesFileRelative.getAbsolutePath();
    	String bakeriesFileData = "";
    	
    	try {
			bakeriesFileData = new String(Files.readAllBytes(Paths.get(bakeriesFilePath)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	JSONArray bakeriesDetailsJSONArray = new JSONArray(bakeriesFileData);
    	
    	for (int i = 0 ; i < bakeriesDetailsJSONArray.length(); i++) {
    		agents.add(bakeriesDetailsJSONArray.getJSONObject(i).getString("guid")+"-SchedulerAgent:org.commitment_issues.agents.SchedulerAgent");
    		agents.add(bakeriesDetailsJSONArray.getJSONObject(i).getString("guid")+"-orderprocessor:org.commitment_issues.agents.OrderProcessorAgent");
        	agents.add(bakeriesDetailsJSONArray.getJSONObject(i).getString("guid")+"-ProoferAgent:org.commitment_issues.agents.ProoferAgent");
        	agents.add(bakeriesDetailsJSONArray.getJSONObject(i).getString("guid")+"-CoolingRacksAgent:org.commitment_issues.agents.CoolingRacksAgent");
        	agents.add(bakeriesDetailsJSONArray.getJSONObject(i).getString("guid")+"-LoadingBayAgentAgent:org.commitment_issues.agents.LoadingBayAgent");
        	agents.add(bakeriesDetailsJSONArray.getJSONObject(i).getString("guid")+"-MailboxAgent:org.commitment_issues.agents.MailboxAgent");
		}

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
