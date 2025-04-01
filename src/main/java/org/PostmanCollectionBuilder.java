package com.readyapi.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * Builder for creating Postman collections from ReadyAPI projects.
 */
public class PostmanCollectionBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PostmanCollectionBuilder.class);
    
    private final ReadyApiProject project;
    private final List<String> conversionIssues = new ArrayList<>();
    private final ObjectMapper objectMapper;
    
    public PostmanCollectionBuilder(ReadyApiProject project) {
        this.project = project;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Build a Postman collection from the ReadyAPI project.
     * 
     * @return A PostmanCollection
     */
    public PostmanCollection build() {
        logger.info("Building Postman collection from ReadyAPI project: {}", project.getName());
        
        PostmanCollection collection = new PostmanCollection();
        PostmanCollection.PostmanInfo info = new PostmanCollection.PostmanInfo();
        info.setName(project.getName());
        collection.setInfo(info);
        
        // Create main folder structure
        PostmanItem interfacesFolder = new PostmanItem();
        interfacesFolder.setName("Interfaces");
        
        PostmanItem testSuitesFolder = new PostmanItem();
        testSuitesFolder.setName("Test Suites");
        
        // Add interfaces
        addInterfaces(interfacesFolder);
        
        // Add test suites
        addTestSuites(testSuitesFolder);
        
        // Add variables
        addVariables(collection);
        
        // Add main folders to collection
        if (interfacesFolder.getItem() != null && !interfacesFolder.getItem().isEmpty()) {
            collection.addItem(interfacesFolder);
        }
        
        if (testSuitesFolder.getItem() != null && !testSuitesFolder.getItem().isEmpty()) {
            collection.addItem(testSuitesFolder);
        }
        
        logger.info("Built Postman collection with {} interfaces and {} test suites", 
                project.getInterfaces().size(), 
                project.getTestSuites().size());
        
        return collection;
    }
    
    /**
     * Add interfaces to the Postman collection.
     * 
     * @param interfacesFolder The interfaces folder item
     */
    private void addInterfaces(PostmanItem interfacesFolder) {
        for (ReadyApiInterface apiInterface : project.getInterfaces()) {
            PostmanItem interfaceFolder = new PostmanItem();
            interfaceFolder.setName(apiInterface.getName());
            
            for (ReadyApiResource resource : apiInterface.getResources()) {
                // For each resource, add a request for each method
                for (ReadyApiMethod method : resource.getMethods()) {
                    for (ReadyApiRequest request : method.getRequests()) {
                        PostmanItem requestItem = new PostmanItem();
                        requestItem.setName(resource.getName());
                        
                        // Create Postman request
                        PostmanRequest postmanRequest = new PostmanRequest();
                        postmanRequest.setMethod(method.getHttpMethod());
                        
                        // Set URL
                        String endpoint = request.getEndpoint();
                        if (endpoint == null || endpoint.isEmpty()) {
                            endpoint = apiInterface.getDefaultEndpoint();
                        }
                        
                        String urlString = endpoint + resource.getPath();
                        postmanRequest.setUrl(PostmanRequest.PostmanUrl.parse(urlString));
                        
                        // Set headers
                        for (Map.Entry<String, String> header : request.getRequestHeaders().entrySet()) {
                            postmanRequest.addHeader(header.getKey(), header.getValue());
                        }
                        
                        // Set body
                        if (request.getRequestBody() != null && !request.getRequestBody().isEmpty()) {
                            PostmanRequest.PostmanBody body = new PostmanRequest.PostmanBody();
                            body.setMode("raw");
                            body.setRaw(request.getRequestBody());
                            
                            // Set body options based on media type
                            PostmanRequest.PostmanBody.PostmanBodyOptions options = 
                                    new PostmanRequest.PostmanBody.PostmanBodyOptions();
                            
                            PostmanRequest.PostmanBody.PostmanBodyOptions.PostmanRawOptions rawOptions = 
                                    new PostmanRequest.PostmanBody.PostmanBodyOptions.PostmanRawOptions();
                            
                            if (request.getMediaType() != null) {
                                if (request.getMediaType().contains("json")) {
                                    rawOptions.setLanguage("json");
                                } else if (request.getMediaType().contains("xml")) {
                                    rawOptions.setLanguage("xml");
                                } else {
                                    rawOptions.setLanguage("text");
                                }
                            } else {
                                rawOptions.setLanguage("text");
                            }
                            
                            options.setRaw(rawOptions);
                            body.setOptions(options);
                            
                            postmanRequest.setBody(body);
                        }
                        
                        // Add assertions as test scripts
                        if (!request.getAssertions().isEmpty()) {
                            StringBuilder testScript = new StringBuilder();
                            
                            // Add each assertion
                            for (ReadyApiAssertion assertion : request.getAssertions()) {
                                String assertionScript = assertion.toPostmanTest();
                                testScript.append(assertionScript).append("\n");
                            }
                            
                            PostmanEvent testEvent = PostmanEvent.createTest(testScript.toString());
                            requestItem.addEvent(testEvent);
                        }
                        
                        requestItem.setRequest(postmanRequest);
                        interfaceFolder.addItem(requestItem);
                    }
                }
            }
            
            if (interfaceFolder.getItem() != null && !interfaceFolder.getItem().isEmpty()) {
                interfacesFolder.addItem(interfaceFolder);
            }
        }
    }
    
    /**
     * Add test suites to the Postman collection.
     * 
     * @param testSuitesFolder The test suites folder item
     */
    private void addTestSuites(PostmanItem testSuitesFolder) {
        Map<String, String> scriptLibraryMap = new HashMap<>();
        
        // First, convert script libraries to JavaScript
        for (ReadyApiScriptLibrary scriptLibrary : project.getScriptLibraries()) {
            String jsLibrary = scriptLibrary.convertToJavaScript();
            scriptLibraryMap.put(scriptLibrary.getName(), jsLibrary);
        }
        
        // Convert test suites
        for (ReadyApiTestSuite testSuite : project.getTestSuites()) {
            PostmanItem testSuiteFolder = new PostmanItem();
            testSuiteFolder.setName(testSuite.getName());
            
            for (ReadyApiTestCase testCase : testSuite.getTestCases()) {
                PostmanItem testCaseFolder = new PostmanItem();
                testCaseFolder.setName(testCase.getName());
                
                // Add setup script with library imports
                StringBuilder setupScript = new StringBuilder();
                setupScript.append("// Import script libraries\n");
                
                for (Map.Entry<String, String> libraryEntry : scriptLibraryMap.entrySet()) {
                    setupScript.append("// Include ").append(libraryEntry.getKey()).append("\n");
                    setupScript.append("let ").append(libraryEntry.getKey()).append(" = pm.collectionVariables.get(\"")
                            .append(libraryEntry.getKey()).append("\");\n");
                    setupScript.append("if (").append(libraryEntry.getKey()).append(" !== null) {\n");
                    setupScript.append("    ").append(libraryEntry.getKey()).append(" = JSON.parse(")
                            .append(libraryEntry.getKey()).append(");\n");
                    setupScript.append("}\n\n");
                }
                
                // Process test steps
                List<ReadyApiTestStep> restRequestSteps = new ArrayList<>();
                List<ReadyApiTestStep> preRequestScriptSteps = new ArrayList<>();
                List<ReadyApiTestStep> testScriptSteps = new ArrayList<>();
                
                // Sort test steps by type
                for (ReadyApiTestStep testStep : testCase.getTestSteps()) {
                    if ("restrequest".equalsIgnoreCase(testStep.getType())) {
                        restRequestSteps.add(testStep);
                    } else if (testStep.isPreRequestScript()) {
                        preRequestScriptSteps.add(testStep);
                    } else if (testStep.isTestScript()) {
                        testScriptSteps.add(testStep);
                    } else {
                        conversionIssues.add("Unsupported test step type: " + testStep.getType() + 
                                " for step: " + testStep.getName() + " in test case: " + testCase.getName());
                    }
                }
                
                // Process pre-request scripts
                for (ReadyApiTestStep scriptStep : preRequestScriptSteps) {
                    setupScript.append("// From test step: ").append(scriptStep.getName()).append("\n");
                    setupScript.append(scriptStep.convertGroovyToJavaScript()).append("\n\n");
                }
                
                // Add test steps to test case folder
                for (ReadyApiTestStep restStep : restRequestSteps) {
                    if (restStep.getRequest() == null) {
                        conversionIssues.add("REST request step without request: " + restStep.getName() + 
                                " in test case: " + testCase.getName());
                        continue;
                    }
                    
                    PostmanItem requestItem = new PostmanItem();
                    requestItem.setName(restStep.getName());
                    
                    // Create Postman request
                    ReadyApiRequest readyRequest = restStep.getRequest();
                    
                    PostmanRequest postmanRequest = new PostmanRequest();
                    
                    // Determine method
                    if (readyRequest.getEndpoint() != null && readyRequest.getEndpoint().contains("service")) {
                        // Typically a POST endpoint
                        postmanRequest.setMethod("POST");
                    } else {
                        // Default to GET
                        postmanRequest.setMethod("GET");
                    }
                    
                    // Set URL
                    String endpoint = readyRequest.getEndpoint();
                    
                    postmanRequest.setUrl(PostmanRequest.PostmanUrl.parse(endpoint));
                    
                    // Set headers
                    for (Map.Entry<String, String> header : readyRequest.getRequestHeaders().entrySet()) {
                        postmanRequest.addHeader(header.getKey(), header.getValue());
                    }
                    
                    // Set body
                    if (readyRequest.getRequestBody() != null && !readyRequest.getRequestBody().isEmpty()) {
                        PostmanRequest.PostmanBody body = new PostmanRequest.PostmanBody();
                        body.setMode("raw");
                        body.setRaw(readyRequest.getRequestBody());
                        
                        // Set body options based on media type
                        PostmanRequest.PostmanBody.PostmanBodyOptions options = 
                                new PostmanRequest.PostmanBody.PostmanBodyOptions();
                        
                        PostmanRequest.PostmanBody.PostmanBodyOptions.PostmanRawOptions rawOptions = 
                                new PostmanRequest.PostmanBody.PostmanBodyOptions.PostmanRawOptions();
                        
                        if (readyRequest.getMediaType() != null) {
                            if (readyRequest.getMediaType().contains("json")) {
                                rawOptions.setLanguage("json");
                            } else if (readyRequest.getMediaType().contains("xml")) {
                                rawOptions.setLanguage("xml");
                            } else {
                                rawOptions.setLanguage("text");
                            }
                        } else {
                            rawOptions.setLanguage("text");
                        }
                        
                        options.setRaw(rawOptions);
                        body.setOptions(options);
                        
                        postmanRequest.setBody(body);
                    }
                    
                    // Add pre-request script
                    PostmanEvent preRequestEvent = PostmanEvent.createPreRequestScript(setupScript.toString());
                    requestItem.addEvent(preRequestEvent);
                    
                    // Add assertions as test scripts
                    StringBuilder testScript = new StringBuilder();
                    
                    // Add each assertion
                    for (ReadyApiAssertion assertion : readyRequest.getAssertions()) {
                        String assertionScript = assertion.toPostmanTest();
                        testScript.append(assertionScript).append("\n");
                    }
                    
                    // Add test scripts
                    for (ReadyApiTestStep scriptStep : testScriptSteps) {
                        testScript.append("// From test step: ").append(scriptStep.getName()).append("\n");
                        testScript.append(scriptStep.convertGroovyToJavaScript()).append("\n\n");
                    }
                    
                    if (testScript.length() > 0) {
                        PostmanEvent testEvent = PostmanEvent.createTest(testScript.toString());
                        requestItem.addEvent(testEvent);
                    }
                    
                    requestItem.setRequest(postmanRequest);
                    testCaseFolder.addItem(requestItem);
                }
                
                if (testCaseFolder.getItem() != null && !testCaseFolder.getItem().isEmpty()) {
                    testSuiteFolder.addItem(testCaseFolder);
                } else {
                    conversionIssues.add("No REST requests found in test case: " + testCase.getName());
                }
            }
            
            if (testSuiteFolder.getItem() != null && !testSuiteFolder.getItem().isEmpty()) {
                testSuitesFolder.addItem(testSuiteFolder);
            }
        }
    }
    
    /**
     * Add variables to the Postman collection.
     * 
     * @param collection The Postman collection
     */
    private void addVariables(PostmanCollection collection) {
        // Add project properties as variables
        for (Map.Entry<String, String> property : project.getProperties().entrySet()) {
            PostmanVariable variable = new PostmanVariable(property.getKey(), property.getValue());
            collection.addVariable(variable);
        }
        
        // Add script libraries as variables
        for (ReadyApiScriptLibrary scriptLibrary : project.getScriptLibraries()) {
            PostmanVariable variable = new PostmanVariable(scriptLibrary.getName(), 
                    scriptLibrary.convertToJavaScript(), "string");
            collection.addVariable(variable);
        }
    }
    
    /**
     * Get the list of conversion issues.
     * 
     * @return List of conversion issues
     */
    public List<String> getConversionIssues() {
        return conversionIssues;
    }
} 