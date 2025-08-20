# Deploying to AWS

This doc covers two options:
- Low-cost single EC2 with Docker Compose (cheapest, simplest)
- Managed ECS Fargate with ECR + RDS (more scalable, costs more)

## Option A: Low-cost EC2 + Docker Compose (recommended for budget)

Costs: One small EC2 instance (e.g., t3a.small or t4g.small) + EBS volume. No ALB, no ECS, no RDS. PostgreSQL runs as a container on the same host.

### 1) Launch EC2
- AMI: Amazon Linux 2023 or Ubuntu 22.04
- Instance type: t4g.small (Arm) or t3a.small (x86) for low cost
- Storage: 20–30 GB gp3
- Security group: allow inbound 22 (SSH) from your IP, 80 (HTTP) from 0.0.0.0/0

### 2) SSH and install Docker
```bash
ssh ec2-user@<ec2-public-ip>    # Amazon Linux
# or
ssh ubuntu@<ec2-public-ip>      # Ubuntu

curl -fsSL https://raw.githubusercontent.com/<your_github_user>/<your_repo>/main/scripts/ec2_setup.sh | bash
# If you can't curl from GitHub, scp the file from your local repo
```
Log out and back in if needed to gain docker group permissions.

### 3) Clone repo and prepare env
```bash
# On EC2
sudo yum install -y git || sudo apt-get install -y git

git clone https://github.com/<your_github_user>/<your_repo>.git
cd <your_repo>

cp env.example .env
# Edit .env with strong passwords and secrets
nano .env
```
Set:
- POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD
- JWT_SECRET (min 32 bytes), JWT_EXPIRATION (optional)
- OPENAI_API_KEY (leave empty to use mock parts if applicable)
- JAVA_OPTS if needed

### 4) Deploy with Docker Compose
```bash
./scripts/deploy_compose.sh .env
```
This builds and starts two containers:
- `db` (PostgreSQL + pgvector)
- `app` (Spring Boot)

App will be available at: `http://<ec2-public-ip>/`

### 5) Data persistence and backups
- The database and uploads volumes are persisted on the EC2 EBS volume
- Create EBS snapshots regularly (manual or automated) for backup

### 6) Upgrades
```bash
# Pull latest changes, rebuild, restart
cd <your_repo>
git pull
./scripts/deploy_compose.sh .env
```

---

## Option B: ECS Fargate + ECR + RDS (managed)

Use when you want managed services. See below for summary.

1) Push container to ECR via GitHub Actions
2) RDS PostgreSQL for database
3) ECS Fargate service behind an ALB
4) Configure env vars per the task definition

Refer to the previous section in this document or the comments in `.github/workflows/ecr.yml`.
