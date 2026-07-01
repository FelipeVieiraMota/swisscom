#!/usr/bin/env bash

set -euo pipefail

SHORT_CODE="${1:-}"
BASE_URL="${2:-https://localhost}"
TOTAL_CLICKS="${3:-1000}"
CONCURRENCY="${4:-50}"
LOG_FILE="${5:-}"

if [[ -z "${SHORT_CODE}" ]]; then
  echo "Usage: ./deploy/simulate-clicks.sh <short-code> [base-url] [total-clicks] [concurrency] [log-file]" >&2
  echo "Example: ./deploy/simulate-clicks.sh GoThere1 https://localhost 1000 50" >&2
  exit 1
fi

if ! [[ "${TOTAL_CLICKS}" =~ ^[0-9]+$ ]] || (( TOTAL_CLICKS < 1 )); then
  echo "total-clicks must be a positive integer." >&2
  exit 1
fi

if ! [[ "${CONCURRENCY}" =~ ^[0-9]+$ ]] || (( CONCURRENCY < 1 )); then
  echo "concurrency must be a positive integer." >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required." >&2
  exit 1
fi

TARGET_URL="${BASE_URL%/}/r/${SHORT_CODE}"
RUN_ID="$(date -u +"%Y%m%dT%H%M%SZ")"
LOG_DIR="deploy/logs"
if [[ -z "${LOG_FILE}" ]]; then
  LOG_FILE="${LOG_DIR}/clicks-${SHORT_CODE}-${RUN_ID}.tsv"
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT
mkdir -p "$(dirname "${LOG_FILE}")"

echo "Simulating ${TOTAL_CLICKS} clicks on ${TARGET_URL}"
echo "Concurrency: ${CONCURRENCY}"
echo "Log file: ${LOG_FILE}"

printf "request_id\tcorrelation_id\tstarted_at\tstatus\ttime_total\tremote_ip\turl_effective\tredirect_url\n" > "${LOG_FILE}"

seq 1 "${TOTAL_CLICKS}" |
  xargs -P "${CONCURRENCY}" -I {} sh -c '
    request_id="$1"
    target_url="$2"
    tmp_dir="$3"
    correlation_id="click-load-test-${request_id}"
    started_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

    result="$(curl \
      --insecure \
      --silent \
      --output /dev/null \
      --request GET \
      --header "X-Correlation-Id: ${correlation_id}" \
      --write-out "%{http_code}\t%{time_total}\t%{remote_ip}\t%{url_effective}\t%{redirect_url}" \
      --max-time 10 \
      "${target_url}")" || result="000\t0\t\t${target_url}\t"

    printf "%s\t%s\t%s\t%s\n" "${request_id}" "${correlation_id}" "${started_at}" "${result}" > "${tmp_dir}/${request_id}.tsv"
  ' sh {} "${TARGET_URL}" "${TMP_DIR}"

for request_id in $(seq 1 "${TOTAL_CLICKS}"); do
  cat "${TMP_DIR}/${request_id}.tsv" >> "${LOG_FILE}"
done

SUCCESS_COUNT="$(awk -F '\t' 'NR > 1 && $4 == "302" { count++ } END { print count + 0 }' "${LOG_FILE}")"
TOTAL_COUNT="$(awk 'NR > 1 { count++ } END { print count + 0 }' "${LOG_FILE}")"
FAILURE_COUNT="$((TOTAL_COUNT - SUCCESS_COUNT))"

echo
echo "Done."
echo "302 redirects: ${SUCCESS_COUNT}/${TOTAL_COUNT}"
echo "Other responses: ${FAILURE_COUNT}/${TOTAL_COUNT}"

if (( FAILURE_COUNT > 0 )); then
  echo
  echo "Response breakdown:"
  awk -F '\t' 'NR > 1 { print $4 }' "${LOG_FILE}" | sort | uniq -c | sort -nr
  exit 1
fi
