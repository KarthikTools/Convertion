package com.readyapi.converter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a ReadyAPI test step.
 */
public class ReadyApiTestStep {
    private String id;
    private String name;
    private String type;
    private String content;  // Will contain script content for Groovy scripts or request config for REST requests
    private Map<String, String> properties = new HashMap<>();
    private ReadyApiRequest request;  // For REST request test steps
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public void addProperty(String name, String value) {
        this.properties.put(name, value);
    }
    
    public String getProperty(String name) {
        return this.properties.get(name);
    }
    
    public ReadyApiRequest getRequest() {
        return request;
    }
    
    public void setRequest(ReadyApiRequest request) {
        this.request = request;
    }
    
    /**
     * Convert Groovy script to JavaScript for Postman.
     * 
     * @return JavaScript code for Postman
     */
    public String convertGroovyToJavaScript() {
        if (!"groovy".equalsIgnoreCase(type) || content == null || content.isEmpty()) {
            return "";
        }
        
        String scriptType = isPreRequestScript() ? "pre-request" : 
                           isTestScript() ? "test" : "library";
        
        return ScriptConverter.convertToJavaScript(content, scriptType);
    }
    
    /**
     * Determine if this test step should be converted to a Postman pre-request script.
     */
    public boolean isPreRequestScript() {
        return "groovy".equalsIgnoreCase(type) && 
               (name.toLowerCase().contains("setup") || 
                name.toLowerCase().contains("prerequest") ||
                name.toLowerCase().contains("pre-request"));
    }
    
    /**
     * Determine if this test step should be converted to a Postman test script.
     */
    public boolean isTestScript() {
        return "groovy".equalsIgnoreCase(type) && 
               !isPreRequestScript() && 
               (name.toLowerCase().contains("test") || 
                name.toLowerCase().contains("assertion") ||
                name.toLowerCase().contains("validate"));
    }
    
    @Override
    public String toString() {
        return "ReadyApiTestStep{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", properties=" + properties.size() +
                '}';
    }
} 