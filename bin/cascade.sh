#!/usr/bin/env bash
set -o errexit ; set -o errtrace ; set -o pipefail

curl -q --user ${BAMBOO_USER}:${BAMBOO_PASS} \
  -X POST -d "ARTIFACT1&ExecuteAllStages" \
  https://bamboo.dev.wisetime.com/rest/api/latest/queue/WT-WCA
