package com.readyapi.converter;

/**
 * Simple Main class to test the converter.
 */
public class Main {
    public static void main(String[] args) {
        // Test with the example ReadyAPI project
        String inputFile = "clean_xml/ready_api_project.xml";
        String outputDir = "output";
        
        ReadyApiToPostmanConverter converter = new ReadyApiToPostmanConverter();
        converter.convert(inputFile, outputDir);
    }
} 