#!/bin/bash

# Setze Standard-Wert falls VERSION_DIVIDER nicht definiert ist (für lokale Ausführung)
VERSION_DIVIDER="${VERSION_DIVIDER:-## Version }"

awk -v version_divider="$VERSION_DIVIDER" '
$0 ~ version_divider {
    if (count == 0) {
        count++;
        next
    } else if (count == 1) {
        exit
    }
}
count == 1 {
    print
}' ./CHANGELOG.md > RELEASE_NOTES.md
