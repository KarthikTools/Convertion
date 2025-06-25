/**
 * MCP Server for API Testing Automation
 * 
 * This server provides a Model Context Protocol implementation
 * for storing API context and automating testing workflows.
 * 
 * Features:
 * - Store API context (endpoints, headers, expected responses)
 * - Generate mock responses based on stored context
 * - Create automated test scripts
 * - Trigger testing workflows from Postman
 * - AI-assisted testing with GitHub Copilot
 * - Direct API calls from Copilot Chat
 */

import express from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.static('public'));

// Simple in-memory storage
const apiContexts = new Map();
const apiResults = new Map();

// Basic health check
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    server: 'MCP Server for API Testing',
    version: '1.0.0'
  });
});

// API Documentation
app.get('/api/docs', (req, res) => {
  res.json({
    name: 'MCP Server for API Testing',
    description: 'Simple server to execute API calls from Copilot',
    version: '1.0.0',
    endpoints: {
      'POST /api/execute': 'Execute an API call',
      'GET /api/results/:id': 'Get API call results',
      'GET /api/contexts': 'List stored contexts',
      'POST /api/contexts': 'Store API context',
      'GET /api/health': 'Health check'
    }
  });
});

// Execute API call (main endpoint for Copilot)
app.post('/api/execute', async (req, res) => {
  try {
    const { method, url, headers, body, description } = req.body;
    
    if (!method || !url) {
      return res.status(400).json({
        error: 'Method and URL are required',
        example: {
          method: 'GET',
          url: 'https://api.example.com/users',
          headers: { 'Authorization': 'Bearer token' },
          body: null
        }
      });
    }

    // Generate unique ID for this request
    const requestId = `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    
    // Store request context
    const context = {
      id: requestId,
      method: method.toUpperCase(),
      url,
      headers: headers || {},
      body: body || null,
      description: description || 'API call from Copilot',
      timestamp: new Date().toISOString()
    };
    
    apiContexts.set(requestId, context);

    // Execute the API call
    const fetch = (await import('node-fetch')).default;
    
    const startTime = Date.now();
    const response = await fetch(url, {
      method: method.toUpperCase(),
      headers: {
        'Content-Type': 'application/json',
        ...headers
      },
      body: body ? JSON.stringify(body) : undefined
    });
    const endTime = Date.now();

    // Get response data
    const responseText = await response.text();
    let responseData;
    try {
      responseData = JSON.parse(responseText);
    } catch {
      responseData = responseText;
    }

    // Store results
    const result = {
      requestId,
      status: response.status,
      statusText: response.statusText,
      headers: Object.fromEntries(response.headers.entries()),
      data: responseData,
      duration: endTime - startTime,
      timestamp: new Date().toISOString()
    };
    
    apiResults.set(requestId, result);

    res.json({
      success: true,
      requestId,
      result,
      context
    });

  } catch (error) {
    console.error('API execution error:', error);
    res.status(500).json({
      error: 'Failed to execute API call',
      message: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Get API call results
app.get('/api/results/:id', (req, res) => {
  const { id } = req.params;
  const result = apiResults.get(id);
  const context = apiContexts.get(id);
  
  if (!result) {
    return res.status(404).json({ error: 'Result not found' });
  }
  
  res.json({
    result,
    context
  });
});

// List all contexts
app.get('/api/contexts', (req, res) => {
  const contexts = Array.from(apiContexts.values());
  res.json({
    count: contexts.length,
    contexts: contexts.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
  });
});

// Store API context (for Copilot to save collection info)
app.post('/api/contexts', (req, res) => {
  const { name, description, data } = req.body;
  
  if (!name) {
    return res.status(400).json({ error: 'Name is required' });
  }
  
  const contextId = `ctx_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  const context = {
    id: contextId,
    name,
    description: description || '',
    data: data || {},
    timestamp: new Date().toISOString()
  };
  
  apiContexts.set(contextId, context);
  
  res.json({
    success: true,
    contextId,
    context
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Server error:', err);
  res.status(500).json({
    error: 'Internal server error',
    message: err.message
  });
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({
    error: 'Endpoint not found',
    availableEndpoints: [
      'GET /health',
      'GET /api/docs',
      'POST /api/execute',
      'GET /api/results/:id',
      'GET /api/contexts',
      'POST /api/contexts'
    ]
  });
});

// Start server
app.listen(PORT, () => {
  console.log('🚀 MCP Server for API Testing is running!');
  console.log(`📍 Server: http://localhost:${PORT}`);
  console.log(`📚 API Docs: http://localhost:${PORT}/api/docs`);
  console.log(`🏥 Health Check: http://localhost:${PORT}/health`);
  console.log('');
  console.log('📋 Available endpoints:');
  console.log('   POST /api/execute - Execute API calls from Copilot');
  console.log('   GET  /api/results/:id - Get API call results');
  console.log('   GET  /api/contexts - List stored contexts');
  console.log('   POST /api/contexts - Store API context');
  console.log('');
  console.log('🤖 Ready for Copilot integration!');
  console.log('💡 Copilot reads files → Server executes APIs → Copilot analyzes results');
});

export default app; 