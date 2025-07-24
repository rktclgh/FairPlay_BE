#!/bin/bash

BASE=src/main/java/com/fairplay

# 기능별 폴더
for FEATURE in user event reservation payment admin common
do
    for DIR in controller service dto entity repository
    do
        mkdir -p "$BASE/$FEATURE/$DIR"
        touch "$BASE/$FEATURE/$DIR/.gitkeep"
    done
done

# core는 지정된 하위폴더만
for CORE in config security util
do
    mkdir -p "$BASE/core/$CORE"
    touch "$BASE/core/$CORE/.gitkeep"
done

echo "✅ FairPlay 전체 구조 + .gitkeep 생성 완료!"

