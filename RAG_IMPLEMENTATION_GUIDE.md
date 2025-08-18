# RAG (Retrieval-Augmented Generation) Implementation Guide

## Overview

This document explains the proper RAG flow implementation in the AiBot application, including vector database integration with PostgreSQL + pgvector.

## Proper RAG Flow

### 1. Document Upload & Processing
```
Document Upload → Text Extraction → Chunking → Embedding Generation → Vector Storage
```

**Steps:**
1. **Document Upload**: User uploads PDF/text file
2. **Text Extraction**: Extract text content from the document
3. **Chunking**: Split document into smaller chunks (1000 characters max)
4. **Embedding Generation**: Generate vector embeddings for each chunk using OpenAI
5. **Vector Storage**: Store chunks and embeddings in PostgreSQL with pgvector

### 2. Question Answering
```
Question → Embedding Generation → Vector Similarity Search → Context Retrieval → LLM Response
```

**Steps:**
1. **Question Input**: User asks a question
2. **Question Embedding**: Generate embedding for the question
3. **Vector Similarity Search**: Find most similar chunks using cosine similarity
4. **Context Retrieval**: Retrieve top-k most relevant chunks
5. **LLM Response**: Send question + context to OpenAI for answer generation

## Vector Database Setup

### Option 1: PostgreSQL + pgvector (Recommended)

1. **Install PostgreSQL**:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install postgresql postgresql-contrib
   
   # macOS
   brew install postgresql
   ```

2. **Install pgvector extension**:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install postgresql-14-pgvector
   
   # Or build from source
   git clone https://github.com/pgvector/pgvector.git
   cd pgvector
   make
   sudo make install
   ```

3. **Create database and enable extension**:
   ```sql
   CREATE DATABASE aibot_db;
   \c aibot_db
   CREATE EXTENSION vector;
   ```

4. **Update application.properties**:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/aibot_db
   spring.datasource.driverClassName=org.postgresql.Driver
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   ```

### Option 2: FAISS (Alternative)

FAISS is Facebook's vector similarity search library. It's faster for large-scale applications but requires more setup.

### Option 3: Pinecone (Cloud Service)

Pinecone is a managed vector database service. Good for production deployments.

## Implementation Details

### VectorRagService

The `VectorRagService` class implements the complete RAG pipeline:

1. **Document Processing**:
   - `processDocument()`: Handles the complete document processing pipeline
   - `chunkDocument()`: Splits documents into manageable chunks
   - `generateEmbedding()`: Creates embeddings using OpenAI API
   - `storeInVectorDatabase()`: Stores in PostgreSQL with pgvector

2. **Question Answering**:
   - `askQuestion()`: Main method for answering questions
   - `findSimilarChunks()`: Performs vector similarity search
   - `generateAnswerWithOpenAI()`: Generates final answer

### Database Schema

```sql
CREATE TABLE document_chunks (
    id SERIAL PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_embedding vector(1536),  -- OpenAI text-embedding-3-small dimension
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for similarity search
CREATE INDEX idx_chunk_embedding ON document_chunks 
USING ivfflat (chunk_embedding vector_cosine_ops);
```

## Cost Considerations

### OpenAI API Costs

1. **Embeddings**: 
   - `text-embedding-3-small`: $0.00002 per 1K tokens
   - Typical document: ~$0.01-0.05 per document

2. **Chat Completions**:
   - `gpt-4o-mini`: $0.00015 per 1K input tokens, $0.0006 per 1K output tokens
   - Typical question: ~$0.01-0.03 per question

### Cost Optimization Strategies

1. **Embedding Caching**: Cache embeddings to avoid regenerating them
2. **Chunk Optimization**: Optimize chunk size for better retrieval
3. **Batch Processing**: Process multiple documents together
4. **Alternative Models**: Use cheaper models for development

## Performance Optimization

1. **Vector Index**: Use appropriate index type (IVFFlat, HNSW)
2. **Chunk Size**: Optimize chunk size (500-1000 characters)
3. **Batch Processing**: Process embeddings in batches
4. **Caching**: Cache frequently accessed embeddings

## Security Considerations

1. **API Key Management**: Store OpenAI API key securely
2. **Data Privacy**: Ensure document content is handled securely
3. **Access Control**: Implement proper user authentication
4. **Rate Limiting**: Implement rate limiting for API calls

## Monitoring and Logging

1. **Embedding Generation**: Log embedding generation times
2. **Similarity Search**: Monitor search performance
3. **API Usage**: Track OpenAI API usage and costs
4. **Error Handling**: Log and monitor errors

## Testing

1. **Unit Tests**: Test individual components
2. **Integration Tests**: Test complete RAG pipeline
3. **Performance Tests**: Test with large documents
4. **Accuracy Tests**: Validate answer quality

## Deployment

1. **Development**: Use H2 database for testing
2. **Staging**: Use PostgreSQL with pgvector
3. **Production**: Use managed PostgreSQL service with pgvector

## Troubleshooting

### Common Issues

1. **pgvector not found**: Ensure pgvector extension is installed
2. **Embedding generation fails**: Check OpenAI API key and quota
3. **Slow similarity search**: Optimize vector index
4. **Memory issues**: Reduce batch size or chunk size

### Debug Commands

```sql
-- Check pgvector installation
SELECT * FROM pg_extension WHERE extname = 'vector';

-- Check table structure
\d document_chunks

-- Test similarity search
SELECT chunk_text, chunk_embedding <=> '[0.1, 0.2, ...]' as distance 
FROM document_chunks 
ORDER BY distance 
LIMIT 5;
```

## Future Enhancements

1. **Hybrid Search**: Combine vector and keyword search
2. **Multi-modal**: Support for images and other file types
3. **Real-time Updates**: Update embeddings when documents change
4. **Federated Search**: Search across multiple document collections
5. **Custom Embeddings**: Train domain-specific embedding models
