# FabriGen

Caches and settings are reloaded when the `Generate Mod` button is pressed, before parsing errors 

Cache location: `~/fabrimodgen/cache` </br>
Settings location: `~/fabrimodgen/settings.json` </br>
Templates location: `~/fabrimodgen/template` </br>

### settings.json
```json5
{
  //boolean value (if false it will not download the template if there is one present)
  "updateTemplate": true,
  //String value [semver] (if not blank it will search the release with the informed tag and download the template on it)
  "templateVersion": ""
}
```

### cache.json
```json5
{
  //String value (caches the last generation location)
  "generationPath": "",
  //String list (Is used to know what licenses are cached)
  "licenses": [],
  //Map<String, String> (Holds the java version for each Minecraft version)
  "javaVersions": {},
  //String value (Holds the latest gradle version [delete to refresh it])
  "gradleVersion": "",
  //String value (Holds what is the version current template being used, used to look for updates)
  "templateVersion": ""
}
```
