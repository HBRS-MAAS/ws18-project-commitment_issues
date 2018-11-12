package org.commitment_issues;

import java.util.List;
import java.util.Vector;

public class Start {
    public static void main(String[] args) {
    	List<String> agents = new Vector<>();
    	agents.add("customer1:org.commitment_issues.agents.CustomerAgent");
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
