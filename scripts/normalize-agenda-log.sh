#!/usr/bin/env bash

#
# Normalizes the log output from the rule engine so different runs can be effectively compared.
#

# 1. Remove timestamp and thread name at beginning
# 2. Replace UUID
# 3. Java object hash appended to Object.toString()
# 4. Activation fact ids
sed -E \
  -e 's/^\s*[0-9:. ]+\[[A-Za-z0-9 ]*\]\s*//' \
  -e 's/[0-9a-f-]{36}/NNN/g' \
  -e 's/@[0-9a-z]{8}/@NNN/g' \
  -e 's/\[[0-9, ]+\]/\[NNN\]/g' \
  -e 's/factId: [0-9]+/factId: NNN/g'
