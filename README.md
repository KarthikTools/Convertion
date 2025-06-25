# MCP Server for API Testing with GitHub Copilot

A **production-ready Model Context Protocol (MCP) server** that enables GitHub Copilot to execute API calls and automate testing workflows. This server acts as a bridge between Copilot's intelligence and external API execution capabilities.

## 🎯 What This Does

This MCP server provides a **hybrid solution** that combines:
- **GitHub Copilot's intelligence** for reading files, understanding context, and generating test scenarios
- **MCP Server's execution engine** for making actual HTTP API calls and storing results
- **Seamless integration** between natural language requests and API automation

### Core Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   GitHub        │    │   MCP Server    │    │   External      │
│   Copilot       │◄──►│   (This App)    │◄──►│   APIs          │
│                 │    │                 │    │                 │
│ • Reads files   │    │ • Executes HTTP │    │ • REST APIs     │
│ • Understands   │    │ • Stores results│    │ • GraphQL       │
│ • Generates     │    │ • Manages       │    │ • SOAP          │
│   requests      │    │   context       │    │ • Custom APIs   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🚀 Installation Guide for Mac M3 Pro

### Prerequisites

1. **Node.js 18+** (Required for ES modules and modern features)
2. **GitHub Copilot** (VS Code extension)
3. **VS Code** (Latest version)

### Step-by-Step Installation

#### 1. Install Node.js
```bash
# Using Homebrew (recommended)
brew install node

# Verify installation
node --version  # Should be 18.0.0 or higher
npm --version   # Should be 9.0.0 or higher
```

#### 2. Clone and Setup the Project
```bash
# Clone the repository
git clone <your-repo-url>
cd mcpserver-postman

# Install dependencies
npm install

# Verify installation
npm start
```

#### 3. Verify Server is Running
```bash
# Check health endpoint
curl http://localhost:3000/health

# Expected response:
{
  "status": "healthy",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "server": "MCP Server for API Testing",
  "version": "1.0.0"
}
```

#### 4. Configure VS Code with Copilot
1. Open VS Code
2. Install GitHub Copilot extension
3. Sign in with your GitHub account
4. Enable Copilot in your workspace

## 🔧 Technical Architecture

### Server Components

#### Core Server (`src/server.js`)
- **Express.js** server with ES modules
- **CORS** enabled for cross-origin requests
- **JSON body parsing** with 10MB limit
- **Static file serving** for documentation

#### Storage Layer
- **In-memory storage** using JavaScript Maps
- **Context storage** for API configurations
- **Result storage** for API call history
- **Unique ID generation** for request tracking

#### API Execution Engine
- **Dynamic HTTP method support** (GET, POST, PUT, DELETE, PATCH)
- **Header management** with automatic Content-Type handling
- **Body transformation** (JSON, form data, raw text)
- **Response parsing** (JSON, text, binary)
- **Error handling** with detailed error messages

### API Endpoints

| Endpoint | Method | Description | Request Body | Response |
|----------|--------|-------------|--------------|----------|
| `/health` | GET | Server health check | None | Health status |
| `/api/docs` | GET | API documentation | None | Endpoint list |
| `/api/execute` | POST | Execute API call | API request object | Execution result |
| `/api/results/:id` | GET | Get API call result | None | Stored result |
| `/api/contexts` | GET | List all contexts | None | Context list |
| `/api/contexts` | POST | Store API context | Context object | Stored context |

### Request/Response Formats

#### Execute API Call
```json
POST /api/execute
{
  "method": "POST",
  "url": "https://api.example.com/users",
  "headers": {
    "Authorization": "Bearer token123",
    "Content-Type": "application/json"
  },
  "body": {
    "name": "John Doe",
    "email": "john@example.com"
  },
  "description": "Create new user"
}
```

#### Response Format
```json
{
  "success": true,
  "requestId": "req_1234567890_abc123",
  "result": {
    "status": 201,
    "statusText": "Created",
    "headers": {
      "content-type": "application/json"
    },
    "data": {
      "id": "user_123",
      "name": "John Doe"
    },
    "duration": 245,
    "timestamp": "2024-01-01T00:00:00.000Z"
  },
  "context": {
    "id": "req_1234567890_abc123",
    "method": "POST",
    "url": "https://api.example.com/users",
    "headers": {...},
    "body": {...},
    "description": "Create new user",
    "timestamp": "2024-01-01T00:00:00.000Z"
  }
}
```

## 💡 Usage Examples and Prompts

### 1. Basic API Testing

#### Prompt: "Test the user creation API"
**Copilot Action:**
1. Reads your API documentation or Postman collection
2. Generates API call with sample data
3. Executes via MCP server
4. Analyzes response and provides insights

**Example Workflow:**
```bash
# Copilot generates this request
curl -X POST http://localhost:3000/api/execute \
  -H "Content-Type: application/json" \
  -d '{
    "method": "POST",
    "url": "https://api.example.com/users",
    "headers": {"Content-Type": "application/json"},
    "body": {"name": "Test User", "email": "test@example.com"},
    "description": "User creation test"
  }'
```

### 2. Data-Driven Testing

#### Prompt: "Test the API with data from my CSV file"
**Copilot Action:**
1. Reads your CSV file
2. Parses each row into API request data
3. Executes multiple API calls
4. Validates responses against expected patterns

**Example CSV Structure:**
```csv
name,email,age,department
John Doe,john@example.com,30,Engineering
Jane Smith,jane@example.com,25,Marketing
Bob Johnson,bob@example.com,35,Sales
```

**Copilot Processing:**
```javascript
// Copilot reads CSV and creates API calls
const csvData = [
  {name: "John Doe", email: "john@example.com", age: 30, department: "Engineering"},
  {name: "Jane Smith", email: "jane@example.com", age: 25, department: "Marketing"},
  {name: "Bob Johnson", email: "bob@example.com", age: 35, department: "Sales"}
];

// Executes each row as separate API call
csvData.forEach((row, index) => {
  // API call via MCP server
});
```

### 3. Postman Collection Integration

#### Prompt: "Run all GET requests from my Postman collection"
**Copilot Action:**
1. Reads your Postman collection JSON
2. Extracts all GET requests
3. Executes each request via MCP server
4. Compiles results and provides summary

**Example Postman Collection:**
```json
{
  "info": {"name": "My API Collection"},
  "item": [
    {
      "name": "Get Users",
      "request": {
        "method": "GET",
        "url": "https://api.example.com/users"
      }
    },
    {
      "name": "Get User by ID",
      "request": {
        "method": "GET",
        "url": "https://api.example.com/users/1"
      }
    }
  ]
}
```

### 4. Complex Workflow Testing

#### Prompt: "Create a user, then update their profile, then delete them"
**Copilot Action:**
1. Creates user via POST request
2. Extracts user ID from response
3. Updates user profile via PUT request
4. Deletes user via DELETE request
5. Validates each step

**Workflow Example:**
```javascript
// Step 1: Create user
const createResponse = await mcpExecute({
  method: "POST",
  url: "https://api.example.com/users",
  body: {name: "Test User", email: "test@example.com"}
});

// Step 2: Extract user ID and update
const userId = createResponse.data.id;
const updateResponse = await mcpExecute({
  method: "PUT",
  url: `https://api.example.com/users/${userId}`,
  body: {name: "Updated User", email: "updated@example.com"}
});

// Step 3: Delete user
const deleteResponse = await mcpExecute({
  method: "DELETE",
  url: `https://api.example.com/users/${userId}`
});
```

### 5. Authentication Testing

#### Prompt: "Test the API with different authentication methods"
**Copilot Action:**
1. Tests Bearer token authentication
2. Tests Basic authentication
3. Tests API key authentication
4. Validates unauthorized access handling

**Authentication Examples:**
```javascript
// Bearer Token
await mcpExecute({
  method: "GET",
  url: "https://api.example.com/protected",
  headers: {"Authorization": "Bearer your-token-here"}
});

// Basic Auth
await mcpExecute({
  method: "GET",
  url: "https://api.example.com/protected",
  headers: {"Authorization": "Basic dXNlcjpwYXNzd2Q="}
});

// API Key
await mcpExecute({
  method: "GET",
  url: "https://api.example.com/protected",
  headers: {"X-API-Key": "your-api-key-here"}
});
```

### 6. Error Handling and Validation

#### Prompt: "Test error scenarios and validate responses"
**Copilot Action:**
1. Tests invalid requests (400 errors)
2. Tests authentication failures (401 errors)
3. Tests not found scenarios (404 errors)
4. Tests server errors (500 errors)
5. Validates error response formats

**Error Testing Examples:**
```javascript
// Test 400 Bad Request
await mcpExecute({
  method: "POST",
  url: "https://api.example.com/users",
  body: {} // Missing required fields
});

// Test 401 Unauthorized
await mcpExecute({
  method: "GET",
  url: "https://api.example.com/protected",
  headers: {} // No authentication
});

// Test 404 Not Found
await mcpExecute({
  method: "GET",
  url: "https://api.example.com/users/999999"
});
```

## 🔍 Advanced Use Cases

### 1. Performance Testing
**Prompt:** "Test API performance with 100 concurrent requests"

### 2. Data Validation
**Prompt:** "Validate that all user responses contain required fields"

### 3. Schema Testing
**Prompt:** "Test API responses against OpenAPI schema"

### 4. Load Testing
**Prompt:** "Simulate high load with multiple users and transactions"

### 5. Integration Testing
**Prompt:** "Test the complete user registration to login workflow"

## 🛠️ Development and Customization

### Environment Variables
```bash
PORT=3000                    # Server port (default: 3000)
NODE_ENV=production          # Environment mode
LOG_LEVEL=info              # Logging level
```

### Adding Custom Endpoints
```javascript
// Add to src/server.js
app.post('/api/custom', (req, res) => {
  // Your custom logic here
  res.json({ success: true, data: 'Custom endpoint' });
});
```

### Extending Storage
```javascript
// Replace in-memory storage with database
const apiContexts = new Database(); // Your database implementation
const apiResults = new Database();  // Your database implementation
```

### Error Handling
```javascript
// Custom error handling
app.use((err, req, res, next) => {
  console.error('Custom error handler:', err);
  res.status(500).json({
    error: 'Custom error message',
    details: err.message
  });
});
```

## 📊 Monitoring and Debugging

### Health Monitoring
```bash
# Check server health
curl http://localhost:3000/health

# Monitor response times
curl -w "@curl-format.txt" http://localhost:3000/api/docs
```

### Logging
```javascript
// Add custom logging
console.log(`API call executed: ${method} ${url} - ${duration}ms`);
console.log(`Response status: ${status} - ${statusText}`);
```

### Performance Metrics
- **Response times**: Tracked automatically
- **Success rates**: Available in results
- **Error rates**: Captured in error responses
- **Throughput**: Measured in requests/second

## 🔒 Security Considerations

### Input Validation
- URL validation before execution
- Method validation (only HTTP methods allowed)
- Body size limits (10MB max)
- Header sanitization

### Error Information
- Limited error details in production
- No sensitive data in error responses
- Proper HTTP status codes

### CORS Configuration
```javascript
// Configure CORS for your domain
app.use(cors({
  origin: ['https://yourdomain.com'],
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
```

## 🚀 Production Deployment

### Docker Deployment
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY src/ ./src/
EXPOSE 3000
CMD ["npm", "start"]
```

### Environment Setup
```bash
# Production environment
NODE_ENV=production
PORT=3000
LOG_LEVEL=error
```

### Load Balancing
- Use multiple server instances
- Implement health checks
- Configure reverse proxy (nginx)

## 📚 Troubleshooting

### Common Issues

#### 1. Server Won't Start
```bash
# Check Node.js version
node --version  # Should be 18+

# Check port availability
lsof -i :3000

# Check dependencies
npm install
```

#### 2. API Calls Failing
```bash
# Check server health
curl http://localhost:3000/health

# Check CORS settings
# Verify URL format
# Check network connectivity
```

#### 3. Copilot Integration Issues
- Ensure VS Code has Copilot extension
- Check GitHub account authentication
- Verify workspace permissions

### Debug Mode
```bash
# Enable debug logging
DEBUG=* npm start

# Check server logs
tail -f server.log
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Make changes
4. Test thoroughly
5. Submit pull request

## 📄 License

MIT License - see LICENSE file for details

## 🆘 Support

- **Issues**: Create GitHub issue
- **Documentation**: Check this README
- **Examples**: See usage examples above

---

**Ready to automate your API testing with Copilot? Start the server and ask Copilot to read your files and execute API calls!** 