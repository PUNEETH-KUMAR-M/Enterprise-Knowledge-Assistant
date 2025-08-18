# Enterprise Knowledge Assistant

A Spring Boot application with a complete RAG (Retrieval-Augmented Generation) pipeline that provides document management and AI-powered Q&A capabilities with JWT authentication.

## 🚀 Features

- **JWT Authentication**: Secure user authentication with role-based access control
- **Document Management**: Upload, store, and manage documents with local file storage
- **RAG Pipeline**: Complete Retrieval-Augmented Generation implementation
  - Document chunking and embedding generation
  - Vector database storage (PostgreSQL with pgvector)
  - Semantic search for relevant document chunks
  - AI-powered question answering with context
- **Modern Frontend**: Beautiful, responsive web interface
- **Query Logging**: Track all user queries and responses for analytics
- **Role-Based Access**: ADMIN and EMPLOYEE roles with different permissions

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Spring Boot   │    │   Vector DB     │
│   (HTML/JS)     │◄──►│   Backend       │◄──►│   (PostgreSQL)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   OpenAI API    │
                       │   (Embeddings)  │
                       └─────────────────┘
```

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.5.4
- **Database**: PostgreSQL 15+ with pgvector extension
- **Vector Database**: pgvector for storing embeddings
- **Authentication**: JWT (JSON Web Tokens)
- **Security**: Spring Security
- **File Storage**: Local file system (uploads/ directory)
- **AI Integration**: OpenAI API (embeddings + completions)
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla)

## 📋 Prerequisites

- Java 21 or higher
- PostgreSQL 15+ with pgvector extension
- OpenAI API key
- Maven 3.6 or higher

## 🚀 Quick Start (Development Mode)

For testing without OpenAI API key:

1. **Clone and navigate to project:**
   ```bash
   cd AiBot
   ```

2. **Set up environment variables:**
   ```bash
   # Copy the example environment file
   cp .env.example .env
   
   # Edit .env and add your OpenAI API key
   OPENAI_API_KEY=your-actual-openai-api-key-here
   ```

3. **Run in development mode:**
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=dev
   ```

4. **Access the application:**
   - Frontend: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console

## 🔧 Production Setup

### 1. Database Setup

1. **Install PostgreSQL with pgvector:**
   ```bash
   # Ubuntu/Debian
   sudo apt-get install postgresql-15
   
   # Enable pgvector extension
   sudo -u postgres psql
   CREATE EXTENSION vector;
   ```

2. **Create database and user:**
   ```sql
   CREATE DATABASE aibot_vector_db;
   CREATE USER aibot_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE aibot_vector_db TO aibot_user;
   ```

3. **Update application.properties:**
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/aibot_vector_db
   spring.datasource.username=aibot_user
   spring.datasource.password=your_password
   ```

### 2. OpenAI API Setup

1. **Get OpenAI API key:**
   - Visit https://platform.openai.com/api-keys
   - Create a new API key

2. **Update application.properties:**
   ```properties
   spring.ai.openai.api-key=your-actual-openai-api-key
   ```

### 3. Application Setup

1. **Create uploads directory:**
   ```bash
   mkdir uploads
   ```

2. **Build and run:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## 🎯 Usage Guide

### 1. Authentication
- Register as ADMIN or EMPLOYEE
- Login to get JWT token
- Token is automatically used for subsequent requests

### 2. Document Upload (Admin Only)
- Select document file (TXT, MD, PDF, DOC, DOCX)
- Enter uploader name
- Document is automatically processed through RAG pipeline

### 3. Ask Questions
- Select a document from the dropdown
- Enter your question
- Get AI-powered answers based on document content

### 4. View History
- See all your previous questions and answers
- Track document usage

## 🔍 RAG Pipeline Details

### Document Processing
1. **Upload**: Document is saved locally and metadata stored in database
2. **Chunking**: Document is split into smaller chunks (paragraphs/sentences)
3. **Embedding**: Each chunk is converted to vector embeddings using OpenAI
4. **Storage**: Embeddings stored in PostgreSQL with pgvector extension

### Question Answering
1. **Question Embedding**: User question is converted to vector
2. **Semantic Search**: Find most similar document chunks using cosine similarity
3. **Context Building**: Relevant chunks are combined with the question
4. **AI Generation**: OpenAI GPT generates answer based on context

## 📁 Project Structure

```
src/main/java/AiBot/example/AiBot/
├── config/
│   ├── JwtUtil.java              # JWT token utilities
│   ├── JwtAuthFilter.java        # JWT authentication filter
│   └── SecurityConfig.java       # Spring Security configuration
├── controller/
│   ├── AuthController.java       # Authentication endpoints
│   ├── DocumentController.java   # Document management endpoints
│   └── QueryController.java      # Q&A endpoints
├── model/
│   ├── User.java                 # User entity
│   ├── Document.java             # Document entity
│   └── QueryLog.java             # Query log entity
├── repository/
│   ├── UserRepository.java       # User data access
│   ├── DocumentRepository.java   # Document data access
│   └── QueryLogRepository.java   # Query log data access
└── service/
    ├── AuthService.java          # Authentication business logic
    ├── DocumentService.java      # Document management logic
    ├── QueryService.java         # Q&A business logic
    ├── RagService.java           # RAG pipeline implementation
    ├── MockRagService.java       # Mock RAG for development
    ├── FileUtil.java             # File handling utilities
    └── ChatService.java          # AI integration (mock)
```

## 🔧 Configuration

### Development Mode (No OpenAI Required)
```properties
# Use H2 database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=password

# Mock OpenAI services
spring.ai.openai.api-key=mock-key-for-development
spring.ai.vectorstore.pgvector.enabled=false
```

### Production Mode
```properties
# PostgreSQL with pgvector
spring.datasource.url=jdbc:postgresql://localhost:5432/aibot_vector_db
spring.datasource.username=aibot_user
spring.datasource.password=your_password

# OpenAI API
spring.ai.openai.api-key=your-actual-openai-api-key
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.embedding.options.model=text-embedding-3-small

# Vector store
spring.ai.vectorstore.pgvector.dimensions=1536
spring.ai.vectorstore.pgvector.distance-type=cosine
```

## 🧪 Testing

### API Testing with Postman
1. Import the provided Postman collection
2. Set environment variables:
   - `base_url`: http://localhost:8080
   - `admin_token`: JWT token from login

### Frontend Testing
1. Open http://localhost:8080
2. Register as admin user
3. Upload test document
4. Ask questions about the document

## 🚀 Deployment

### Local Development
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Production
```bash
mvn clean package
java -jar target/AiBot-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker (Future Enhancement)
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/AiBot-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 🔮 Future Enhancements

1. **Advanced Document Processing**: PDF parsing, OCR, table extraction
2. **Multi-Modal Support**: Images, audio, video processing
3. **Advanced Chunking**: Semantic chunking, overlap strategies
4. **Hybrid Search**: Combine semantic and keyword search
5. **Caching**: Redis for embedding and response caching
6. **Analytics**: Query analytics, document usage metrics
7. **User Management**: User profiles, permissions, groups
8. **API Rate Limiting**: Protect against abuse
9. **Monitoring**: Health checks, metrics, logging
10. **AWS Integration**: S3 for file storage, RDS for database

## 🐛 Troubleshooting

### Common Issues

1. **JWT Token Issues**: Check JWT secret length (must be ≥256 bits)
2. **Database Connection**: Ensure PostgreSQL is running and accessible
3. **pgvector Extension**: Verify pgvector is installed and enabled
4. **OpenAI API**: Check API key validity and quota
5. **File Upload**: Ensure uploads directory exists and has write permissions

### Development Mode
- Uses H2 database (no setup required)
- Mock RAG service (no OpenAI API required)
- Perfect for testing and development

### Production Mode
- Requires PostgreSQL with pgvector
- Requires valid OpenAI API key
- Full RAG pipeline functionality

## 📄 License

This project is for educational purposes.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

---

**Note**: For production use, ensure you have proper security measures, monitoring, and backup strategies in place.
