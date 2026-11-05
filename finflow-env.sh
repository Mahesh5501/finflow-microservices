# System JAVA_HOME is often Liberica 8 — FinFlow needs Temurin 21 (Spring Boot 3.5 / --release 21)
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
hash -r
java -version 2>&1 | head -1

# FinFlow local ports (dedicated — avoids common 8080/8081/8082 clashes)
# api-gateway           → 18080
# identity-service      → 18081
# wallet-service        → 18082
# commerce-service      → 18083
# notification-service  → 18084
#
# Shared Docker infra (from /infrastructure):
#   docker compose up -d
#   containers: mysql (:3307 root/root), redis (:6379), kafka (:9092)

