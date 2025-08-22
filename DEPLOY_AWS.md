# Deploying to AWS (Single EC2, Lowest Cost)

This guide deploys everything on one EC2 instance using Docker Compose (app + PostgreSQL/pgvector). No ALB, no ECS, no RDS.

## 1) Launch EC2
- AMI: Amazon Linux 2023 or Ubuntu 22.04
- Instance type: t4g.small (Arm) or t3a.small (x86)
- Storage: 20–30 GB gp3
- Security Group: inbound 22 (SSH) from your IP, 80 (HTTP) from 0.0.0.0/0

## 2) SSH and install Docker
```bash
ssh ec2-user@<ec2-public-ip>    # Amazon Linux
# or
ssh ubuntu@<ec2-public-ip>      # Ubuntu

curl -fsSL https://raw.githubusercontent.com/<your_github_user>/<your_repo>/main/scripts/ec2_setup.sh | bash
# If curl fails, scp the file and run: bash scripts/ec2_setup.sh
```
Log out and back in if needed to gain docker group permissions.

## 3) Clone repo and prepare env
```bash
# On EC2
sudo yum install -y git || sudo apt-get install -y git

git clone https://github.com/<your_github_user>/<your_repo>.git
cd <your_repo>

cp env.example .env
nano .env
```
Set these in `.env`:
- POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD
- JWT_SECRET (min 32 bytes), JWT_EXPIRATION (optional)
- OPENAI_API_KEY (optional)
- JAVA_OPTS (optional)

## 4) Deploy with Docker Compose
```bash
./scripts/deploy_compose.sh .env

## Enable HTTPS with Caddy (Let's Encrypt)

Prerequisites:
- Buy or use a domain and create an A record pointing to your EC2 public IP.
- Open inbound ports 80 and 443 in the EC2 security group.

Steps:
1. Set environment variables on EC2 (replace with your values):
   ```bash
   echo "DOMAIN=your-domain.example" | sudo tee -a /etc/environment
   echo "EMAIL=your-email@example.com" | sudo tee -a /etc/environment
   export DOMAIN=your-domain.example
   export EMAIL=your-email@example.com
   ```
2. Update and run Docker Compose:
   - App now listens on host port 8080.
   - Caddy proxy listens on 80/443 and terminates TLS automatically.
   ```bash
   docker compose up -d --build
   ```
3. Verify:
   - Visit https://$DOMAIN
   - First issuance may take ~30-60 seconds.

Notes:
- Certificates are stored in the `caddy_data` volume.
- If using ECR, log in prior to `docker compose up`.
```
This builds and starts two containers:
- `db` (PostgreSQL + pgvector)
- `app` (Spring Boot on port 80)

Open: `http://<ec2-public-ip>/`

## 5) Operate
- Status: `docker compose ps`
- Logs: `docker compose logs -f app`
- Stop: `docker compose down`
- Update to latest code: `git pull && ./scripts/deploy_compose.sh .env`

## 6) Data and backups
- DB and uploads are persisted on EC2’s EBS via Docker volumes
- Create EBS snapshots periodically for backups

## 7) Troubleshooting
- Merge conflicts: reset to the clean branch you deploy from
- Port busy: ensure nothing else listens on port 80 (`sudo lsof -i :80`)
- Low disk: `docker system prune -af`
- OOM: reduce `JAVA_OPTS` heap or choose a larger instance
