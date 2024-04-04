package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;


public class PostmanToKarate {
    public static String strFolderPath = "/Users/kargee/Documents/karthik/Karthik_Private_Java/Resources/";
    public static void main(String[] args) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get("/Users/kargee/Documents/karthik/Karthik_Private_Java/Resources/weather.json")));

        JSONObject postmanCollection = new JSONObject(content);
        JSONArray requests = postmanCollection.getJSONArray("requests");

        for (int i = 0; i < requests.length(); i++) {
            JSONObject request = requests.getJSONObject(i);
            String name = request.getString("name");
            String method = request.getString("method");
            String url = request.getString("url");

            // Generate Karate feature file
            generateKarateFeature(name, method, url);
        }
    }

    private static void generateKarateFeature(String name, String method, String url) {
        // Define the Karate feature file content
        String featureContent = "Feature: " + name + "\n\n" +
                "Background:\n" +
                "  * url '" + url + "'\n\n" +
                "Scenario: " + name + "\n" +
                "  Given method " + method + "\n" +
                "  When path '/'\n" +
                "  Then status 200\n\n";

        // Write content to the feature file
        try {
            FileWriter writer = new FileWriter(strFolderPath+name + ".feature");
            writer.write(featureContent);
            writer.close();
            System.out.println("Karate feature file generated for: " + name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
