#!/usr/bin/env bash
# Run this from inside Corridor360/app/src/main/java/com/ashutosh/corridor360/
# Adjust PKG_ROOT if your actual path differs.

set -e

echo "Creating package skeleton..."
mkdir -p capture
mkdir -p data/local/dao
mkdir -p data/local/entity
mkdir -p data/repository
mkdir -p data/sync
mkdir -p stitching
mkdir -p mapping
mkdir -p di

echo "Skeleton created:"
find . -maxdepth 3 -type d | sort

echo ""
echo "---"
echo "Phase 3: move existing files. This script does NOT move files automatically"
echo "since paths depend on your current layout. Run these git mv commands"
echo "(preserves history) after checking paths match your repo:"
echo ""
echo "  git mv Data/NodeEntity.kt data/local/entity/NodeEntity.kt"
echo "  git mv Data/EdgeEntity.kt data/local/entity/EdgeEntity.kt"
echo "  git mv Data/*Dao.kt data/local/dao/"
echo "  git mv Data/CorridorDatabase.kt data/local/CorridorDatabase.kt"
echo "  git mv GitHubSyncManager.kt data/sync/GitHubSyncManager.kt"
echo "  git mv PanoramaStitcher.kt stitching/PanoramaStitcher.kt"
echo "  git mv MappingScreen.kt mapping/MappingScreen.kt"
echo "  git mv MappingViewModel.kt mapping/MappingViewModel.kt"
echo ""
echo "  # remove the now-empty capital-D Data folder once confirmed empty:"
echo "  rmdir Data"
echo ""
echo "After moving, update package declarations at the top of each file to match"
echo "its new path, e.g. NodeEntity.kt should read:"
echo "  package com.ashutosh.corridor360.data.local.entity"
echo ""
echo "Then fix imports across the project (Android Studio: right-click each"
echo "moved file > Refactor > Move, or use 'Optimize Imports' project-wide"
echo "after manual package line fixes)."