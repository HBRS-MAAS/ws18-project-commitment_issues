package org.maas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

public class PackagingStageInitializer extends Initializer {
    @Override
    public String initialize() {
        Vector<String> agents = new Vector<>();
        
        agents.add("OrderProcessor:org.commitment_issues.delivery_agents.DummyOrderProcessor");
        agents.add("BakingAgent:org.commitment_issues.delivery_agents.DummyBakeAgent");
        agents.add("CoolingRack:org.commitment_issues.packaging_agents.GenericItemProcessor(cooling)");
        agents.add("ItemsProcessor:org.commitment_issues.packaging_agents.GenericItemProcessor");
        agents.add("PackagingAgent:org.commitment_issues.packaging_agents.PackagingAgent(bakery-001)");
		agents.add("LoadingBayAgent:org.commitment_issues.delivery_agents.LoadingBayAgent");

        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
    }
}
