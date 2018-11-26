package org.commitment_issues;

import java.util.List;
import java.util.Vector;


public class Start {
    public static void main(String[] args) {
    	List<String> agents = new Vector<>();
    	
    	agents.add("TimeKeeper:org.yourteamname.agents.TimeKeeper");
    	agents.add("bakery-001"+"-TransportAgent:org.commitment_issues.delivery_agents.TransportAgent");
    	agents.add("bakery-001"+"-TruckAgent:org.commitment_issues.delivery_agents.TruckAgent");
    	agents.add("bakery-001"+"-StreetNetworkAgent:org.commitment_issues.delivery_agents.StreetNetworkAgent");
    	agents.add("bakery-001"+"-MailboxAgent:org.commitment_issues.delivery_agents.MailboxAgent");
    	agents.add("customer-001"+":org.commitment_issues.DummyCustomer");
    	
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
