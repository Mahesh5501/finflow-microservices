export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
hash -r

# FinFlow local ports (dedicated — avoids common 8080/8081/8082 clashes)
# api-gateway      → 18080
# identity-service → 18081
# wallet-service   → 18082
# commerce-service → 18083
#
# Shared Docker infra (from /infrastructure):
#   docker compose up -d
#   containers: mysql (:3307 root/root), redis (:6379)

