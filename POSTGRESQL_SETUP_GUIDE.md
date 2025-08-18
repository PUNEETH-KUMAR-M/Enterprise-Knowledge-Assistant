# PostgreSQL + pgvector Setup Guide

## Prerequisites
- PostgreSQL installed (version 12 or higher)
- pgAdmin installed
- pgvector extension

## Step 1: Install pgvector Extension

### Windows (using pgAdmin):
1. Download pgvector from: https://github.com/pgvector/pgvector/releases
2. Extract the files
3. Copy the `vector.dll` to your PostgreSQL `lib` directory
4. Copy the `vector.control` and SQL files to your PostgreSQL `share/extension` directory

### Alternative: Using Docker (Recommended)
```bash
docker run -d \
  --name postgres-vector \
  -e POSTGRES_DB=aibot_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg15
```

## Step 2: Create Database and Enable Extension

### Using pgAdmin:
1. Open pgAdmin
2. Connect to your PostgreSQL server
3. Right-click on "Databases" → "Create" → "Database"
4. Name: `aibot_db`
5. Click "Save"

### Using SQL:
```sql
-- Connect to PostgreSQL
CREATE DATABASE aibot_db;
\c aibot_db

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
```

## Step 3: Verify Setup

Run this query in pgAdmin to verify pgvector is working:
```sql
-- Test vector operations
SELECT '[1,2,3]'::vector;
SELECT '[1,2,3]'::vector <=> '[4,5,6]'::vector as distance;
```

## Step 4: Update Application Configuration

The application is already configured to use PostgreSQL. If you need to change credentials:

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/aibot_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Step 5: Test the Setup

1. Start the application: `mvn spring-boot:run`
2. Upload a document
3. Ask a question
4. Check console logs for vector processing

## Troubleshooting

### "pgvector extension not found"
- Ensure pgvector is properly installed
- Check PostgreSQL version compatibility
- Verify extension files are in correct directories

### "Connection refused"
- Check if PostgreSQL is running
- Verify port 5432 is accessible
- Check firewall settings

### "Authentication failed"
- Verify username/password in application.properties
- Check PostgreSQL authentication settings

## Expected Behavior

With PostgreSQL + pgvector + OpenAI:
- Documents will be chunked into smaller pieces
- Each chunk gets converted to a vector embedding
- Vectors are stored in PostgreSQL with pgvector
- Questions are converted to vectors
- Similar chunks are found using vector similarity search
- OpenAI generates precise answers based on relevant context

## Performance Benefits

- **Better Search**: Vector similarity is much more accurate than keyword search
- **Semantic Understanding**: Finds related concepts, not just exact words
- **Scalability**: Can handle large documents efficiently
- **Real-time**: Fast similarity search with proper indexing
