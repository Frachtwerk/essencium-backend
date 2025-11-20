#!/bin/bash

header="# Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe."
fail=0
while IFS= read -r -d '' file; do
  head=$(head -n 1 "$file" | tr -d '\r')
  if [[ "$head" != "$header" ]]; then
    echo "::error file=$file::Licence header missing or incorrect."
    echo "Found:"
    echo "$head"
    fail=1
  fi
done < <(find . -type d -name target -prune -o -type f \( -name "*.yml" -o -name "*.yaml" \) -print0)
if [[ $fail -eq 1 ]]; then
  exit 1
fi

header1="/*"
header2=" * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe."
fail=0
while IFS= read -r -d '' file; do
  line1=$(head -n 1 "$file" | tr -d '\r')
  line2=$(head -n 2 "$file" | tail -n 1 | tr -d '\r')
  if [[ "$line1" != "$header1" || "$line2" != "$header2" ]]; then
    echo "::error file=$file::Licence header missing or incorrect."
    echo "Found:"
    echo "$line1"
    echo "$line2"
    fail=1
  fi
done < <(find . -type d -name target -prune -o -type f -name "*.java" -print0)
if [[ $fail -eq 1 ]]; then
  exit 1
fi
