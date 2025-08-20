#!/usr/bin/env bash
set -euo pipefail

# Detect OS (Amazon Linux 2023 or Ubuntu) and install Docker + Compose plugin
if [ -f /etc/os-release ]; then
  . /etc/os-release
fi

if [[ "${ID:-}" == "amzn" ]]; then
  echo "Detected Amazon Linux. Installing docker..."
  sudo dnf update -y
  sudo dnf install -y docker
  sudo systemctl enable docker
  sudo systemctl start docker
  sudo usermod -aG docker "$USER" || true
  echo "Installing docker-compose-plugin..."
  sudo dnf install -y docker-compose-plugin
elif [[ "${ID:-}" == "ubuntu" ]]; then
  echo "Detected Ubuntu. Installing docker..."
  sudo apt-get update -y
  sudo apt-get install -y ca-certificates curl gnupg lsb-release
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(lsb_release -cs) stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt-get update -y
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo usermod -aG docker "$USER" || true
  sudo systemctl enable docker
  sudo systemctl start docker
else
  echo "Unsupported OS. Install Docker manually."
  exit 1
fi

echo "Docker installed. You may need to log out and back in for group changes to take effect."
