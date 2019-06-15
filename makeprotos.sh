#!/bin/bash

ABSPATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
GAME="csgo"

if [[ -d "$ABSPATH/Protobufs" ]]; then
	cd "$ABSPATH/Protobufs"
	git pull
else
	git clone "https://github.com/SteamDatabase/Protobufs" "$ABSPATH/Protobufs"
fi

mkdir -p "$ABSPATH/javaprotos"
find "$ABSPATH/javaprotos" -type f -name "*.java" -exec rm {} \;

protoc --proto_path="$ABSPATH/Protobufs/$GAME" --java_out="$ABSPATH/javaprotos" @"$ABSPATH/doprotos.txt"

find "$ABSPATH/javaprotos" -type f -name "*.java" -print0 | while IFS= read -r -d '' i; do
	mv "$i" "$i.bak"
	printf "package com.valvesoftware.protos.csgo;\n\n" > "$i"
	cat "$i.bak" >> "$i"
	rm "$i.bak"
done

cd "$ABSPATH/javaprotos"
mkdir -p "./build"
javac -cp "$ABSPATH/protobuf-java-3.7.1.jar" -d ./build *.java
cd build
jar cvf "../../${GAME}_protos.jar" *
cd ../
rm -R build
