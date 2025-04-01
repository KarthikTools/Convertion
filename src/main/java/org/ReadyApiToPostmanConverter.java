package com.readyapi.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class for converting ReadyAPI projects to Postman collections.
 */
public class ReadyApiToPostmanConverter {
    private static final Logger logger = LoggerFactory.getLogger(ReadyApiToPostmanConverter.class);
    
    // List to track items that couldn't be converted
    private final List<String> conversionIssues = new ArrayList<>();
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar readyapi-to-postman-converter.jar <readyapi_project_file.xml> [output_directory]");
            System.exit(1);
        }
        
        String inputFilePath = args[0];
        String outputDirectory = args.length > 1 ? args[1] : ".";
        
        ReadyApiToPostmanConverter converter = new ReadyApiToPostmanConverter();
        converter.convert(inputFilePath, outputDirectory);
    }
    
    /**
     * Convert a ReadyAPI project to Postman collection
     * 
     * @param readyApiFile Path to the ReadyAPI project file
     * @param outputDirectory Directory to save the output files
     */
    public void convert(String readyApiFile, String outputDirectory) {
        logger.info("Starting conversion of ReadyAPI project: {}", readyApiFile);
        
        try {
            // Create output directory if it doesn't exist
            File outputDir = new File(outputDirectory);
            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    logger.error("Failed to create output directory: {}", outputDirectory);
                    return;
                }
            }
            
            // Parse the ReadyAPI project
            logger.info("Parsing ReadyAPI project...");
            ReadyApiProject project = new ReadyApiProjectParser().parse(readyApiFile);
            
            // Create Postman collection
            logger.info("Creating Postman collection...");
            PostmanCollectionBuilder collectionBuilder = new PostmanCollectionBuilder(project);
            PostmanCollection collection = collectionBuilder.build();
            collection.setConversionIssues(collectionBuilder.getConversionIssues());
            
            // Create Postman environment
            logger.info("Creating Postman environment...");
            PostmanEnvironment environment = new PostmanEnvironmentBuilder(project).build();
            
            // Save Postman collection and environment
            String projectName = project.getName();
            String collectionFile = outputDir.getPath() + File.separator + projectName + ".postman_collection.json";
            String environmentFile = outputDir.getPath() + File.separator + projectName + ".postman_environment.json";
            String issuesFile = outputDir.getPath() + File.separator + projectName + "_conversion_issues.txt";
            
            logger.info("Saving Postman collection to: {}", collectionFile);
            collection.saveToFile(collectionFile);
            
            logger.info("Saving Postman environment to: {}", environmentFile);
            environment.saveToFile(environmentFile);
            
            // Save any CSV data files
            logger.info("Saving data files...");
            new DataFileExporter(project, outputDir).export();
            
            // Save conversion issues if any
            if (!collection.getConversionIssues().isEmpty()) {
                logger.info("Saving conversion issues to: {}", issuesFile);
                ConversionIssueReporter.saveIssues(collection.getConversionIssues(), issuesFile);
            }
            
            logger.info("Validating Postman collection...");
            boolean isValid = new PostmanCollectionValidator().validate(collectionFile);
            if (isValid) {
                logger.info("Postman collection validation successful!");
            } else {
                logger.warn("Postman collection validation failed. See logs for details.");
            }
            
            logger.info("Conversion completed successfully!");
            
        } catch (Exception e) {
            logger.error("Error during conversion: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get the list of conversion issues
     * 
     * @return List of conversion issues
     */
    public List<String> getConversionIssues() {
        return conversionIssues;
    }
} 