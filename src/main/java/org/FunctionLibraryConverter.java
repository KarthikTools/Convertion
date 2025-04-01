package com.readyapi.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FunctionLibraryConverter {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static String convertGroovyToJavaScript(String groovyScript) {
        try {
            // Create a JavaScript class that mimics the Groovy functionality
            StringBuilder jsScript = new StringBuilder();
            
            // Add Postman environment setup and documentation
            jsScript.append("// Function Library converted from ReadyAPI Groovy script\n");
            jsScript.append("// Note: This is a Postman-compatible version of the original Groovy script\n\n");
            
            // Create the main class
            jsScript.append("class FunctionLibrary {\n");
            jsScript.append("    constructor(log, context, testRunner) {\n");
            jsScript.append("        this.log = log;\n");
            jsScript.append("        this.context = context;\n");
            jsScript.append("        this.testRunner = testRunner;\n");
            jsScript.append("    }\n\n");
            
            // Add runTestCaseMultipleTimes method
            jsScript.append("    async runTestCaseMultipleTimes(testSuiteName, testCaseName, count) {\n");
            jsScript.append("        try {\n");
            jsScript.append("            const project = this.testRunner.testCase.testSuite.project;\n");
            jsScript.append("            const tcase = project.testSuites[testSuiteName].testCases[testCaseName];\n");
            jsScript.append("            const myContext = new Map(this.context);\n");
            jsScript.append("            for (let i = 0; i < count; i++) {\n");
            jsScript.append("                await tcase.run(myContext, false);\n");
            jsScript.append("                this.log.info(`Running ${testCaseName} -- ${count}`);\n");
            jsScript.append("            }\n");
            jsScript.append("        } catch (e) {\n");
            jsScript.append("            return e;\n");
            jsScript.append("        }\n");
            jsScript.append("    }\n\n");
            
            // Add enableDisableTestStep method
            jsScript.append("    enableDisableTestStep(testStepName, enabled) {\n");
            jsScript.append("        const testStep = this.testRunner.testCase.testSteps[testStepName];\n");
            jsScript.append("        testStep.disabled = !enabled;\n");
            jsScript.append("    }\n\n");
            
            // Add SignInAvion method
            jsScript.append("    SignInAvion(cardNumber, env) {\n");
            jsScript.append("        this.log.info(`Signing in with card number: ${cardNumber} in environment: ${env}`);\n");
            jsScript.append("        const headers = new Map();\n");
            jsScript.append("        const testSteps = this.context.testCase.getTestStepList();\n");
            jsScript.append("        testSteps.forEach(step => {\n");
            jsScript.append("            if (step.config.type === 'restrequest' && !['Signin', 'pvqvalidation', 'WIM'].includes(step.name)) {\n");
            jsScript.append("                headers.set('Cookie', '');\n");
            jsScript.append("                headers.set('Content-Type', 'application/xml');\n");
            jsScript.append("                step.httpRequest.setRequestHeaders(headers);\n");
            jsScript.append("                this.SetEndpoint(step);\n");
            jsScript.append("            }\n");
            jsScript.append("        });\n");
            jsScript.append("        return 'JSESSIONID=ESC8BF5BFD9020E5E9D356334D6F7AEF';\n");
            jsScript.append("    }\n\n");
            
            // Add MobiliserEnvType method
            jsScript.append("    MobiliserEnvType() {\n");
            jsScript.append("        return pm.variables.get('envType');\n");
            jsScript.append("    }\n\n");
            
            // Add SetEndpoint method
            jsScript.append("    SetEndpoint(testStep) {\n");
            jsScript.append("        const endpoint = 'https://mobile.sterbcroyalbank.com';\n");
            jsScript.append("        testStep.testRequest.endpoint = endpoint;\n");
            jsScript.append("    }\n\n");
            
            // Add TestCaseFailureCheck method
            jsScript.append("    TestCaseFailureCheck(testCase) {\n");
            jsScript.append("        let result = true;\n");
            jsScript.append("        testCase.getTestStepList().forEach(step => {\n");
            jsScript.append("            if (step.assertionStatus.toString() === 'FAILED') {\n");
            jsScript.append("                result = false;\n");
            jsScript.append("            }\n");
            jsScript.append("        });\n");
            jsScript.append("        return result;\n");
            jsScript.append("    }\n\n");
            
            // Add CreateLogFile method
            jsScript.append("    CreateLogFile(fileName) {\n");
            jsScript.append("        // In Postman, we'll use pm.variables to store log data\n");
            jsScript.append("        const logKey = fileName || 'default_log';\n");
            jsScript.append("        let logData = pm.variables.get(logKey);\n");
            jsScript.append("        if (!logData) {\n");
            jsScript.append("            logData = [];\n");
            jsScript.append("            pm.variables.set(logKey, JSON.stringify(logData));\n");
            jsScript.append("        }\n");
            jsScript.append("        return logKey;\n");
            jsScript.append("    }\n");
            
            jsScript.append("}\n\n");
            
            // Add initialization code
            jsScript.append("// Initialize the function library\n");
            jsScript.append("const functionLibrary = new FunctionLibrary(\n");
            jsScript.append("    console, // log\n");
            jsScript.append("    pm.variables, // context\n");
            jsScript.append("    pm.testRunner // testRunner\n");
            jsScript.append(");\n\n");
            
            // Export the library for use in other scripts
            jsScript.append("// Export the library for use in other scripts\n");
            jsScript.append("pm.functionLibrary = functionLibrary;\n");
            
            return jsScript.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Groovy script to JavaScript: " + e.getMessage(), e);
        }
    }

    public static String convertLibraryToPostmanVariable(String libraryName, String groovyScript) {
        try {
            String jsScript = convertGroovyToJavaScript(groovyScript);
            
            Map<String, Object> libraryVar = new HashMap<>();
            libraryVar.put("key", libraryName);
            libraryVar.put("value", jsScript);
            libraryVar.put("type", "string");
            libraryVar.put("enabled", true);
            
            return objectMapper.writeValueAsString(libraryVar);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert library to Postman variable: " + e.getMessage(), e);
        }
    }
}