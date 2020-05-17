ant -f . -Dnb.internal.action.name=build jar
scp dist/SnuviScriptRecoded.jar alpaga:../minecraft/server/mods/libs
