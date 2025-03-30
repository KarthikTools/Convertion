package com.readyapi.converter;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java -jar readyapi-to-openapi-converter.jar <input-file.xml> <output-file.json>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];

        try {
            // Read the ReadyAPI XML file
            String readyApiXml = Files.readString(Paths.get(inputFile));

            // Convert to OpenAPI
            ReadyApiToOpenApiConverter converter = new ReadyApiToOpenApiConverter();
            String openApiJson = converter.convert(readyApiXml);

            // Write the OpenAPI JSON file
            Files.writeString(Paths.get(outputFile), openApiJson);
            System.out.println("Conversion completed successfully!");
            System.out.println("OpenAPI specification written to: " + outputFile);

        } catch (Exception e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 
