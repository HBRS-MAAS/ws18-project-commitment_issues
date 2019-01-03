package org.maas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.maas.data.models.Bakery;
import org.maas.utils.JsonConverter;
import com.fasterxml.jackson.core.type.TypeReference;

public class PackagingStageInitializer extends Initializer {
    @Override
    public String initialize(String scenarioDirectory) {
        Vector<String> agents = new Vector<>();
        Vector<String> bakeryNames = this.getBakeryNames(scenarioDirectory);
        
        for (String bakeryName : bakeryNames)
        {
        	agents.add(bakeryName + "_OrderProcessor:org.commitment_issues.delivery_agents.DummyOrderProcessor");
            //agents.add(bakeryName + "_BakingAgent:org.commitment_issues.delivery_agents.DummyBakeAgent");
            agents.add(bakeryName + "_CoolingRack:org.commitment_issues.packaging_agents.GenericItemProcessor("+scenarioDirectory+",cooling)");
            agents.add(bakeryName + "_ItemsProcessor:org.commitment_issues.packaging_agents.GenericItemProcessor(" + scenarioDirectory + ")");
            agents.add(bakeryName + "_PackagingAgent:org.commitment_issues.packaging_agents.PackagingAgent(bakery-001,"+ scenarioDirectory +")");
    		agents.add(bakeryName + "_LoadingBayAgent:org.commitment_issues.delivery_agents.LoadingBayAgent");
        }
        
        agents.add("EntryAgent:org.commitment_issues.packaging_agents.EntryAgent(" + scenarioDirectory + ")");
        
        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
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
