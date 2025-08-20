# Deploying to AWS (ECS Fargate + ECR + RDS)

This guide explains how to deploy this Spring Boot app to AWS using:
- Amazon ECR for the Docker image
- Amazon ECS Fargate for compute (managed containers)
- Amazon RDS (PostgreSQL) for the database
- Application Load Balancer for public access

## 1) Prerequisites
- AWS account and permissions to create ECR/ECS/IAM/RDS/ALB resources
- GitHub repository for this code
- OpenAI API key (if using the real RAG flow)

## 2) Configure GitHub for ECR push
The repo includes `.github/workflows/ecr.yml` which builds and pushes the image to ECR on every push to `main`.

You have two options to authenticate GitHub to AWS:

- Recommended: OIDC role
  - Create an IAM role with a trust policy for GitHub OIDC
  - Grant it permissions for ECR (ecr:*)
  - Add a GitHub secret `AWS_ROLE_TO_ASSUME` with the role ARN

- Alternate: Access keys
  - Add GitHub secrets: `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
  - Replace the `configure-aws-credentials` step to use these keys instead of a role

Update these in the workflow if needed:
- `AWS_REGION`: e.g. `ap-south-1`
- `ECR_REPOSITORY`: e.g. `aibot`

Push to `main` and the image will be available in ECR as:
`<aws_account_id>.dkr.ecr.<region>.amazonaws.com/<repo>:<sha>` and `:latest`.

## 3) Provision RDS PostgreSQL
- Create an RDS PostgreSQL instance (e.g., t4g.micro) in the same VPC as ECS
- Ensure security groups allow ECS tasks to connect on port 5432
- Note the endpoint, db name, username, password
- Enable `pgvector` extension if required by your usage

## 4) Create ECS Fargate service
1. Create an ECS cluster (Fargate)
2. Create a Task Definition:
   - Container image: ECR image URI from step 2
   - CPU/Memory: e.g., 0.5 vCPU / 1GB
   - Port mappings: 8080 TCP
   - Env vars:
     - `SPRING_PROFILES_ACTIVE=prod`
     - `SPRING_DATASOURCE_URL=jdbc:postgresql://<rds-endpoint>:5432/<db>`
     - `SPRING_DATASOURCE_USERNAME=<db_user>`
     - `SPRING_DATASOURCE_PASSWORD=<db_password>`
     - `JWT_SECRET=<long_random_secret_>=32bytes`
     - `JWT_EXPIRATION=3600000`
     - `OPENAI_API_KEY=<your_openai_key>`
     - `FILE_UPLOAD_DIR=/data/uploads` (see note below)
   - Storage: at least 1–2GB ephemeral, or mount EFS if you need persistence
3. Create a Service:
   - Desired count: 1+
   - Launch type: Fargate
   - Networking: Public subnets + Security group allowing 80/443 from internet
   - Load balancer: Application Load Balancer, listener 80/443 → Target group on port 8080
   - Health check path: `/` (or `/index.html`)

## 5) Important notes on file storage
This project currently uses local disk (`file.upload-dir`). In containers, local storage is ephemeral.

- For production durability, prefer S3 or EFS. S3 integration would require small code changes in `FileUtil` to write/read from S3 instead of local disk.
- If you keep local storage temporarily, mount EFS to `/data/uploads` and set `FILE_UPLOAD_DIR=/data/uploads`.

## 6) Environment variables recap
The app reads these in `application-prod.properties`:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION` (default 3600000)
- `OPENAI_API_KEY`
- `FILE_UPLOAD_DIR` (default `/data/uploads`)
- `PORT` (container port, default 8080)

## 7) Local test (optional)
```bash
# Build image
docker build -t aibot:local .

# Run with env vars
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/aibot_db' \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  -e JWT_SECRET='replace_with_long_secret' \
  -e OPENAI_API_KEY='sk-...' \
  -v $(pwd)/uploads:/data/uploads \
  aibot:local
```

## 8) Troubleshooting
- 502/timeout: Verify ALB target health and security groups
- DB connection errors: Check RDS SG inbound rules and credentials
- WebSocket issues: Ensure ALB target group uses HTTP/1.1 and sticky sessions are disabled
- Memory pressure: Lower `JAVA_OPTS` heap or increase task memory
