package com.readyapi.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Utility for exporting data files from ReadyAPI projects.
 */
public class DataFileExporter {
    private static final Logger logger = LoggerFactory.getLogger(DataFileExporter.class);
    
    private final ReadyApiProject project;
    private final File outputDir;
    
    public DataFileExporter(ReadyApiProject project, File outputDir) {
        this.project = project;
        this.outputDir = outputDir;
    }
    
    /**
     * Export data files from the ReadyAPI project.
     */
    public void export() {
        logger.info("Exporting data files from ReadyAPI project: {}", project.getName());
        
        // Create a data directory
        File dataDir = new File(outputDir, "data");
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            logger.error("Failed to create data directory: {}", dataDir.getPath());
            return;
        }
        
        // Extract test data as CSV files
        extractTestDataToCSV(dataDir);
        
        logger.info("Exported data files to: {}", dataDir.getPath());
    }
    
    /**
     * Extract test data to CSV files.
     * 
     * @param dataDir Directory to save the CSV files
     */
    private void extractTestDataToCSV(File dataDir) {
        // Extract from test suites
        for (ReadyApiTestSuite testSuite : project.getTestSuites()) {
            // Look for data-driven test cases
            for (ReadyApiTestCase testCase : testSuite.getTestCases()) {
                // Check if the test case has data properties
                String dataSource = testCase.getProperty("DataSource");
                if (dataSource != null && !dataSource.isEmpty()) {
                    try {
                        generateCSVFromDataSource(dataDir, testSuite.getName(), testCase.getName(), dataSource);
                    } catch (IOException e) {
                        logger.error("Error creating CSV file for test case: {}", testCase.getName(), e);
                    }
                }
            }
        }
    }
    
    /**
     * Generate a CSV file from a data source string.
     * 
     * @param dataDir Directory to save the CSV file
     * @param testSuiteName Name of the test suite
     * @param testCaseName Name of the test case
     * @param dataSource Data source string (comma-separated values)
     * @throws IOException If there's an error writing the CSV file
     */
    private void generateCSVFromDataSource(File dataDir, String testSuiteName, String testCaseName, String dataSource) throws IOException {
        String filename = testSuiteName + "_" + testCaseName + ".csv";
        File csvFile = new File(dataDir, filename);
        
        try (FileWriter writer = new FileWriter(csvFile)) {
            // Parse the data source and write to CSV
            String[] lines = dataSource.split("\\r?\\n");
            
            for (String line : lines) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }
        
        logger.info("Created CSV file: {}", csvFile.getPath());
    }
} 