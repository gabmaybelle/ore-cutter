#!/usr/bin/env bash
# Downloads the mods Ore Cutter is tested beside into a folder:
#   tools/fetch-extra-mods.sh fabric|neoforge <folder>
# - Cozy Core, which also adds stonecutter recipes (the two sets must sit side by side);
# - JEI, where players look recipes up (with the settings library it needs).
# Prints nothing but the file names. `--ids` instead of a folder prints the mod ids, for -PexpectMods.
set -euo pipefail
LOADER=$1 OUT=$2
MC=26.3 COZY_CORE=0.12.1
if [ "$OUT" = --ids ]; then echo "cozy_core,jei"; exit 0; fi
mkdir -p "$OUT"

modrinth_jar() { # newest Modrinth file of a project for this loader and Minecraft version
	local url
	url=$(curl -fsS "https://api.modrinth.com/v2/project/$1/version?loaders=%5B%22$LOADER%22%5D&game_versions=%5B%22$MC%22%5D" \
		| jq -r '([.[] | select(.version_type == "release")] + .)[0].files | (map(select(.primary)) + .)[0].url')
	curl -fsSL -o "$OUT/$(basename "$url")" "$url"
}

curl -fsSL -o "$OUT/cozy-core-$COZY_CORE-$LOADER.jar" \
	"https://github.com/gabmaybelle/cozy-core/releases/download/v$COZY_CORE/cozy-core-$COZY_CORE-$LOADER.jar"
modrinth_jar jei
modrinth_jar mezzconfig
ls "$OUT"
