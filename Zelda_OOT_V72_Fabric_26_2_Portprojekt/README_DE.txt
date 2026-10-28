ZELDA OOT V72 - Fabric 26.2 Java SOURCE PREVIEW

STATUS: Am 21.10.2026 mit JDK 25 und Gradle 9.5.1 erfolgreich kompiliert.
Fabric Loader 0.19.3 erkennt den Mod beim Serverstart; die EULA-Pruefung
beendet den Test vor der Mod-Initialisierung. Gameplay ist nicht getestet.
Dies ist ein funktionaler Portierungsversuch mit Item-Registrierung, Texturen,
Rechtsklick-Mechaniken, Rubin-Wallet, Herzteilen, Okarina-Noten,
Lied-Freischaltungen, einem Shop-Kommando und einfacher Navi-Chatfunktion.

INSTALLATION/BUILD:
- JDK 25 und Gradle 9.5.1 verwenden.
- gradle build ausfuehren (oder einen Gradle-9.5.1-Wrapper verwenden).
- build/libs/zelda-oot-v72-fabric-0.2.0-source-preview.jar zusammen mit
  Fabric API in den mods-Ordner einer Fabric-26.2-Installation kopieren.

SPIEL: /give @s zelda:ocarina; /give @s zelda:rupee_green
/zelda status, /zelda shop, /zelda buy 1-3, /zelda note 1-5

NOCH NICHT PORTIERT: persistente Spielerdaten/Welt-Saves, echtes Navi-Entity
und Händler-Entity mit GUI, voll funktionsfähige Schleuder-Projektilphysik,
Maximal-Lebensattribute und maßgefertigtes HUD, Bedrock Welt-Events und
weitere Skriptfunktionen. Das ist ausdrücklich KEINE vollständige V72-Portierung.
Die Original-Bedrock-Referenzen bleiben erhalten.
