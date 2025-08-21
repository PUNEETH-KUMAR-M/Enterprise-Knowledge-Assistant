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

## 📋 Prerequisites

- Java 21 or higher
- PostgreSQL 15+ with pgvector extension (in production)
- OpenAI API key (for full RAG)
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

See `DEPLOY_AWS.md` for the lowest-cost single-EC2 deployment using Docker Compose.

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

## 🐛 Troubleshooting

- **JWT Token Issues**: Ensure JWT secret length (≥ 32 bytes)
- **Database Connection**: Ensure PostgreSQL (container) is running and accessible
- **pgvector Extension**: Enabled by the container image on first run
- **OpenAI API**: Check API key validity and quota
- **File Upload**: Ensure uploads directory exists and has write permissions
