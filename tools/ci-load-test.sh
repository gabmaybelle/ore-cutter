#!/usr/bin/env bash
# CI server load tests. Each one starts a real dedicated server with Ore Cutter and fails unless
# the world loads with no data errors and Ore Cutter's recipes are switched on.
#   tools/ci-load-test.sh fabric   [extra]   the Fabric jar (+ Fabric API)
#   tools/ci-load-test.sh neoforge [extra]   the NeoForge jar
#   tools/ci-load-test.sh vanilla            the data pack zip on plain vanilla
# "extra" also loads the mods from tools/fetch-extra-mods.sh (Cozy Core and JEI).
# Expects dist/ (the two jars and the zip, as the CI build job leaves them).
set -euo pipefail
cd "$(dirname "$0")/.."
LOADER=$1 EXTRA=${2:-none}
MC=26.3 FABRIC_LOADER=0.19.5 FABRIC_INSTALLER=1.1.2
RUN=$(mktemp -d)

modrinth_jar() { # newest Modrinth file of a project for a loader and Minecraft version -> folder
	local url
	url=$(curl -fsS "https://api.modrinth.com/v2/project/$1/version?loaders=%5B%22$2%22%5D&game_versions=%5B%22$MC%22%5D" \
		| jq -r '([.[] | select(.version_type == "release")] + .)[0].files | (map(select(.primary)) + .)[0].url')
	curl -fsSL -o "$3/$(basename "$url")" "$url"
}

server_files() { # eula and an offline server.properties
	echo 'eula=true' > "$RUN/eula.txt"
	printf 'online-mode=false\nspawn-protection=0\nview-distance=4\nsimulation-distance=4\n' > "$RUN/server.properties"
}

run_server() { # <server jar or @args file> <text the data pack list must contain>
	local out
	out=$(node tools/server-test.js "$RUN" java "$1" "datapack list enabled") || { echo "$out"; exit 1; }
	echo "$out"
	echo "$out" | grep -q "$2" || { echo "Ore Cutter's recipes aren't switched on (no \"$2\" in the data pack list)"; exit 1; }
}

case $LOADER in
	fabric)
		mkdir -p "$RUN/mods"
		curl -fsSL -o "$RUN/server.jar" "https://meta.fabricmc.net/v2/versions/loader/$MC/$FABRIC_LOADER/$FABRIC_INSTALLER/server/jar"
		modrinth_jar fabric-api fabric "$RUN/mods"
		cp dist/ore-cutter-fabric-*.jar "$RUN/mods/"
		if [ "$EXTRA" = extra ]; then bash tools/fetch-extra-mods.sh fabric "$RUN/mods" > /dev/null; fi
		server_files
		echo "Fabric $MC: $(ls "$RUN/mods" | tr '\n' ' ')"
		run_server "$RUN/server.jar" "orecutter"
		;;
	neoforge)
		# The newest NeoForge release for this Minecraft version, installed as a server.
		NEOFORGE=$(curl -fsS https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml \
			| grep -o "<version>$MC\.[0-9.]*</version>" | sed 's/<[^>]*>//g' | sort -V | tail -1)
		curl -fsSL -o "$RUN/installer.jar" "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEOFORGE/neoforge-$NEOFORGE-installer.jar"
		(cd "$RUN" && java -jar installer.jar --install-server . > /dev/null)
		mkdir -p "$RUN/mods"
		cp dist/ore-cutter-neoforge-*.jar "$RUN/mods/"
		if [ "$EXTRA" = extra ]; then bash tools/fetch-extra-mods.sh neoforge "$RUN/mods" > /dev/null; fi
		server_files
		echo "NeoForge $NEOFORGE: $(ls "$RUN/mods" | tr '\n' ' ')"
		# NeoForge lists every mod's data as one pack; the game test checks the recipes themselves.
		run_server "@libraries/net/neoforged/neoforge/$NEOFORGE/unix_args.txt" "mod_data"
		;;
	vanilla)
		ZIP=$(ls dist/ore-cutter-*-datapack.zip | head -1)
		mkdir -p "$RUN/world/datapacks"
		url=$(curl -fsS https://piston-meta.mojang.com/mc/game/version_manifest_v2.json | jq -r ".versions[] | select(.id == \"$MC\").url")
		curl -fsSL -o "$RUN/server.jar" "$(curl -fsS "$url" | jq -r '.downloads.server.url')"
		cp "$ZIP" "$RUN/world/datapacks/"
		server_files
		run_server "$RUN/server.jar" "file/$(basename "$ZIP")"
		;;
	*) echo "usage: tools/ci-load-test.sh fabric|neoforge|vanilla [extra]"; exit 2 ;;
esac
