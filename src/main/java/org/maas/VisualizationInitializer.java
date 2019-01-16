package org.maas;

import java.util.Vector;

public class VisualizationInitializer extends Initializer {
    @Override
    public String initialize(String scenarioDirectory) {
        Vector<String> agents = new Vector<>();        
<<<<<<< HEAD
        agents.add("GraphVisualizationAgent:org.maas.agents.GraphVisualizationAgent");
=======
        agents.add("GraphVisualizationAgent:org.commitment_issues.delivery_agents.GraphVisualizationAgent");
>>>>>>> b2b49e9768e03647a2daac108c79292bfaae920d
        
        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
    }
}
