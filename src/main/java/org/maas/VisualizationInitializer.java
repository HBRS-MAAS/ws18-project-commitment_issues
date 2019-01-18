package org.maas;

import java.util.Vector;

public class VisualizationInitializer extends Initializer {
    @Override
    public String initialize(String scenarioDirectory) {
        Vector<String> agents = new Vector<>();        
        agents.add("GraphVisualizationAgent:org.maas.agents.GraphVisualizationAgent");
        
        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
    }
}
