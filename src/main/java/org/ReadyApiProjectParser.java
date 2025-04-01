package com.readyapi.converter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Parser for ReadyAPI project XML files.
 */
public class ReadyApiProjectParser {
    private static final Logger logger = LoggerFactory.getLogger(ReadyApiProjectParser.class);
    
    /**
     * Parse a ReadyAPI project XML file.
     * 
     * @param filePath Path to the ReadyAPI project XML file
     * @return A ReadyApiProject object with parsed project data
     * @throws DocumentException If there's an error parsing the XML
     */
    public ReadyApiProject parse(String filePath) throws DocumentException {
        logger.info("Parsing ReadyAPI project file: {}", filePath);
        
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(filePath));
        Element rootElement = document.getRootElement();
        
        ReadyApiProject project = new ReadyApiProject();
        project.setId(rootElement.attributeValue("id"));
        project.setName(rootElement.attributeValue("name"));
        
        // Parse project properties
        parseProperties(rootElement, project);
        
        // Parse interfaces
        parseInterfaces(rootElement, project);
        
        // Parse test suites
        parseTestSuites(rootElement, project);
        
        // Parse script libraries
        parseScriptLibraries(rootElement, project);
        
        logger.info("Parsed ReadyAPI project: {}", project);
        return project;
    }
    
    /**
     * Parse project properties.
     * 
     * @param rootElement The XML root element
     * @param project The project to populate
     */
    private void parseProperties(Element rootElement, ReadyApiProject project) {
        Element propertiesElement = rootElement.element("properties");
        if (propertiesElement != null) {
            List<Element> propertyElements = propertiesElement.elements("property");
            for (Element propertyElement : propertyElements) {
                String name = propertyElement.elementText("name");
                String value = propertyElement.elementText("value");
                if (name != null && value != null) {
                    project.addProperty(name, value);
                }
            }
        }
    }
    
    /**
     * Parse interfaces.
     * 
     * @param rootElement The XML root element
     * @param project The project to populate
     */
    private void parseInterfaces(Element rootElement, ReadyApiProject project) {
        List<Element> interfaceElements = rootElement.elements("interface");
        for (Element interfaceElement : interfaceElements) {
            ReadyApiInterface apiInterface = new ReadyApiInterface();
            apiInterface.setId(interfaceElement.attributeValue("id"));
            apiInterface.setName(interfaceElement.attributeValue("name"));
            apiInterface.setType(interfaceElement.attributeValue("type"));
            
            // Parse endpoints
            Element endpointsElement = interfaceElement.element("endpoints");
            if (endpointsElement != null) {
                List<Element> endpointElements = endpointsElement.elements("endpoint");
                for (Element endpointElement : endpointElements) {
                    apiInterface.addEndpoint(endpointElement.getTextTrim());
                }
            }
            
            // Parse resources
            List<Element> resourceElements = interfaceElement.elements("resource");
            for (Element resourceElement : resourceElements) {
                ReadyApiResource resource = new ReadyApiResource();
                resource.setId(resourceElement.attributeValue("id"));
                resource.setName(resourceElement.attributeValue("name"));
                resource.setPath(resourceElement.attributeValue("path"));
                
                // Parse methods
                List<Element> methodElements = resourceElement.elements("method");
                for (Element methodElement : methodElements) {
                    ReadyApiMethod method = new ReadyApiMethod();
                    method.setId(methodElement.attributeValue("id"));
                    method.setName(methodElement.attributeValue("name"));
                    method.setHttpMethod(methodElement.attributeValue("method"));
                    
                    // Parse requests
                    List<Element> requestElements = methodElement.elements("request");
                    for (Element requestElement : requestElements) {
                        ReadyApiRequest request = new ReadyApiRequest();
                        request.setId(requestElement.attributeValue("id"));
                        request.setName(requestElement.attributeValue("name"));
                        request.setMediaType(requestElement.attributeValue("mediaType"));
                        
                        // Parse request settings (headers, etc.)
                        Element settingsElement = requestElement.element("settings");
                        if (settingsElement != null) {
                            List<Element> settingElements = settingsElement.elements("setting");
                            for (Element settingElement : settingElements) {
                                if ("request-headers".equals(settingElement.attributeValue("id"))) {
                                    // TODO: Parse request headers
                                }
                            }
                        }
                        
                        // Set endpoint
                        Element endpointElement = requestElement.element("endpoint");
                        if (endpointElement != null) {
                            request.setEndpoint(endpointElement.getTextTrim());
                        }
                        
                        // Set request body
                        Element requestBodyElement = requestElement.element("request");
                        if (requestBodyElement != null) {
                            request.setRequestBody(requestBodyElement.getTextTrim());
                        }
                        
                        // Parse assertions
                        List<Element> assertionElements = requestElement.elements("assertion");
                        for (Element assertionElement : assertionElements) {
                            ReadyApiAssertion assertion = new ReadyApiAssertion();
                            assertion.setId(assertionElement.attributeValue("id"));
                            assertion.setName(assertionElement.attributeValue("name"));
                            assertion.setType(assertionElement.attributeValue("type"));
                            
                            // Parse assertion configuration
                            Element configElement = assertionElement.element("configuration");
                            if (configElement != null) {
                                List<Element> configChildElements = configElement.elements();
                                for (Element configChild : configChildElements) {
                                    assertion.addConfigurationProperty(configChild.getName(), configChild.getTextTrim());
                                }
                            }
                            
                            request.addAssertion(assertion);
                        }
                        
                        method.addRequest(request);
                    }
                    
                    resource.addMethod(method);
                }
                
                apiInterface.addResource(resource);
            }
            
            project.addInterface(apiInterface);
        }
    }
    
    /**
     * Parse test suites.
     * 
     * @param rootElement The XML root element
     * @param project The project to populate
     */
    private void parseTestSuites(Element rootElement, ReadyApiProject project) {
        List<Element> testSuiteElements = rootElement.elements("testSuite");
        for (Element testSuiteElement : testSuiteElements) {
            ReadyApiTestSuite testSuite = new ReadyApiTestSuite();
            testSuite.setId(testSuiteElement.attributeValue("id"));
            testSuite.setName(testSuiteElement.attributeValue("name"));
            testSuite.setRunType(testSuiteElement.attributeValue("runType"));
            
            // Parse test suite properties
            Element propertiesElement = testSuiteElement.element("properties");
            if (propertiesElement != null) {
                List<Element> propertyElements = propertiesElement.elements("property");
                for (Element propertyElement : propertyElements) {
                    String name = propertyElement.elementText("name");
                    String value = propertyElement.elementText("value");
                    if (name != null && value != null) {
                        testSuite.addProperty(name, value);
                    }
                }
            }
            
            // Parse test cases
            List<Element> testCaseElements = testSuiteElement.elements("testCase");
            for (Element testCaseElement : testCaseElements) {
                ReadyApiTestCase testCase = new ReadyApiTestCase();
                testCase.setId(testCaseElement.attributeValue("id"));
                testCase.setName(testCaseElement.attributeValue("name"));
                
                // Parse test case properties
                Element testCasePropertiesElement = testCaseElement.element("properties");
                if (testCasePropertiesElement != null) {
                    List<Element> testCasePropertyElements = testCasePropertiesElement.elements("property");
                    for (Element propertyElement : testCasePropertyElements) {
                        String name = propertyElement.elementText("name");
                        String value = propertyElement.elementText("value");
                        if (name != null && value != null) {
                            testCase.addProperty(name, value);
                        }
                    }
                }
                
                // Parse test steps
                List<Element> testStepElements = testCaseElement.elements("testStep");
                for (Element testStepElement : testStepElements) {
                    ReadyApiTestStep testStep = new ReadyApiTestStep();
                    testStep.setId(testStepElement.attributeValue("id"));
                    testStep.setName(testStepElement.attributeValue("name"));
                    testStep.setType(testStepElement.attributeValue("type"));
                    
                    // Parse test step configuration
                    Element configElement = testStepElement.element("config");
                    if (configElement != null) {
                        if ("groovy".equals(testStep.getType())) {
                            // Parse Groovy script
                            Element scriptElement = configElement.element("script");
                            if (scriptElement != null) {
                                testStep.setContent(scriptElement.getTextTrim());
                            }
                        } else if ("restrequest".equals(testStep.getType())) {
                            // Parse REST request
                            Element restRequestElement = configElement.element("restRequest");
                            if (restRequestElement != null) {
                                ReadyApiRequest request = new ReadyApiRequest();
                                request.setId(restRequestElement.attributeValue("id"));
                                request.setName(restRequestElement.attributeValue("name"));
                                request.setMediaType(restRequestElement.attributeValue("mediaType"));
                                
                                // Parse request settings (headers, etc.)
                                Element settingsElement = restRequestElement.element("settings");
                                if (settingsElement != null) {
                                    List<Element> settingElements = settingsElement.elements("setting");
                                    for (Element settingElement : settingElements) {
                                        if ("request-headers".equals(settingElement.attributeValue("id"))) {
                                            // TODO: Parse request headers
                                        }
                                    }
                                }
                                
                                // Set endpoint
                                Element endpointElement = restRequestElement.element("endpoint");
                                if (endpointElement != null) {
                                    request.setEndpoint(endpointElement.getTextTrim());
                                }
                                
                                // Set request body
                                Element requestBodyElement = restRequestElement.element("request");
                                if (requestBodyElement != null) {
                                    request.setRequestBody(requestBodyElement.getTextTrim());
                                }
                                
                                // Parse assertions
                                List<Element> assertionElements = restRequestElement.elements("assertion");
                                for (Element assertionElement : assertionElements) {
                                    ReadyApiAssertion assertion = new ReadyApiAssertion();
                                    assertion.setId(assertionElement.attributeValue("id"));
                                    assertion.setName(assertionElement.attributeValue("name"));
                                    assertion.setType(assertionElement.attributeValue("type"));
                                    
                                    // Parse assertion configuration
                                    Element assertionConfigElement = assertionElement.element("configuration");
                                    if (assertionConfigElement != null) {
                                        List<Element> configChildElements = assertionConfigElement.elements();
                                        for (Element configChild : configChildElements) {
                                            assertion.addConfigurationProperty(configChild.getName(), configChild.getTextTrim());
                                        }
                                    }
                                    
                                    request.addAssertion(assertion);
                                }
                                
                                testStep.setRequest(request);
                            }
                        }
                    }
                    
                    testCase.addTestStep(testStep);
                }
                
                testSuite.addTestCase(testCase);
            }
            
            project.addTestSuite(testSuite);
        }
    }
    
    /**
     * Parse script libraries.
     * 
     * @param rootElement The XML root element
     * @param project The project to populate
     */
    private void parseScriptLibraries(Element rootElement, ReadyApiProject project) {
        Element scriptLibraryElement = rootElement.element("scriptLibrary");
        if (scriptLibraryElement != null) {
            List<Element> libraryConfigElements = scriptLibraryElement.elements("libraryConfig");
            for (Element libraryConfigElement : libraryConfigElements) {
                ReadyApiScriptLibrary scriptLibrary = new ReadyApiScriptLibrary();
                scriptLibrary.setId(libraryConfigElement.attributeValue("id"));
                scriptLibrary.setName(libraryConfigElement.attributeValue("name"));
                
                // Parse script content
                Element groovyScriptElement = libraryConfigElement.element("groovyScript");
                if (groovyScriptElement != null) {
                    scriptLibrary.setContent(groovyScriptElement.getTextTrim());
                }
                
                project.addScriptLibrary(scriptLibrary);
            }
        }
    }
} 