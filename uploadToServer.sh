ant -f . -Dnb.internal.action.name=build jar
scp dist/SnuviScriptRecoded.jar poro:/home/minecraft/server/mods/libs
