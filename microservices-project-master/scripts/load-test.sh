#!/usr/bin/env bash
#
# Simple end-to-end load test against the API gateway.
# Creates COUNT payments, then exercises status updates and refunds on a sample,
# and prints latency/throughput stats.
#
# Usage: GW=http://localhost:18090 COUNT=100 ./scripts/load-test.sh
#
set -uo pipefail

# Note: LOGIN_USER (not USERNAME) - Windows sets USERNAME in the environment.
GW="${GW:-http://localhost:18090}"
LOGIN_USER="${LOGIN_USER:-admin}"
LOGIN_PASS="${LOGIN_PASS:-admin123}"
COUNT="${COUNT:-100}"
UPDATES="${UPDATES:-25}"
REFUNDS="${REFUNDS:-15}"

echo "==> Logging in as $LOGIN_USER"
TOKEN=$(curl -s -X POST "$GW/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"username\":\"$LOGIN_USER\",\"password\":\"$LOGIN_PASS\"}" \
  | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then echo "Login failed"; exit 1; fi
AUTH="Authorization: Bearer $TOKEN"

echo "==> Creating $COUNT payments"
ok=0; fail=0; rl=0
total_time=0; min=99; max=0
ids=()
run_id="$(date +%s)"
start=$(date +%s%3N)
for i in $(seq 1 "$COUNT"); do
  resp=$(curl -s -w "\n%{http_code} %{time_total}" -X POST "$GW/api/payments/create" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"orderId\":\"LT-$run_id-$i\",\"amount\":$((RANDOM % 9000 + 100)).50,\"currency\":\"USD\",\"paymentMethod\":\"CREDIT_CARD\"}")
  body=$(echo "$resp" | head -n -1)
  meta=$(echo "$resp" | tail -n1)
  code=$(echo "$meta" | cut -d' ' -f1)
  t=$(echo "$meta" | cut -d' ' -f2)
  if [ "$code" = "201" ] || [ "$code" = "200" ]; then
    ok=$((ok+1))
    id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
    [ -n "$id" ] && ids+=("$id")
    total_time=$(awk "BEGIN{print $total_time+$t}")
    min=$(awk "BEGIN{print ($t<$min)?$t:$min}")
    max=$(awk "BEGIN{print ($t>$max)?$t:$max}")
  elif [ "$code" = "429" ]; then rl=$((rl+1)); else fail=$((fail+1)); fi
done
end=$(date +%s%3N)
elapsed=$(awk "BEGIN{print ($end-$start)/1000}")

echo "==> Updating status (SUCCESS) on $UPDATES payments"
upok=0
for id in "${ids[@]:0:$UPDATES}"; do
  c=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$GW/api/payments/$id/status" \
    -H "$AUTH" -H "Content-Type: application/json" -d '{"status":"SUCCESS"}')
  [ "$c" = "200" ] && upok=$((upok+1))
done

echo "==> Refunding $REFUNDS payments"
rfok=0
for id in "${ids[@]:0:$REFUNDS}"; do
  c=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GW/api/payments/$id/refund" \
    -H "$AUTH" -H "Content-Type: application/json" -d '{"refundAmount":10.00,"refundReason":"load-test"}')
  [ "$c" = "200" ] && rfok=$((rfok+1))
done

avg=$(awk "BEGIN{ if($ok>0) printf \"%.3f\", $total_time/$ok; else print 0 }")
tput=$(awk "BEGIN{ if($elapsed>0) printf \"%.1f\", $COUNT/$elapsed; else print 0 }")

echo
echo "================ LOAD TEST SUMMARY ================"
echo " Creates attempted : $COUNT"
echo " Creates succeeded : $ok"
echo " Rate-limited (429): $rl"
echo " Other failures    : $fail"
echo " Status updates ok : $upok / $UPDATES"
echo " Refunds ok        : $rfok / $REFUNDS"
echo " Wall time         : ${elapsed}s   (~${tput} creates/s incl. curl overhead)"
echo " Create latency    : min=${min}s avg=${avg}s max=${max}s"
echo "=================================================="
