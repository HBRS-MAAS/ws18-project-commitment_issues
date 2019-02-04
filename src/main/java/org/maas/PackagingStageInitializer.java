package org.maas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.maas.Initializer;

public class PackagingStageInitializer extends Initializer {
    @Override
    public String initialize(String scenarioDirectory) {
        Vector<String> agents = new Vector<>();


        agents.add("dummy:org.right_brothers.agents.PackagingStageTester");
        agents.add("postBakingProcessor:org.right_brothers.agents.PreLoadingProcessor");
        agents.add("packaging-agent:org.right_brothers.agents.LoadingBayAgent");

        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
    }
}
