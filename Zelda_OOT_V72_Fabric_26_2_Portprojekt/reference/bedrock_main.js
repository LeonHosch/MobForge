import { world, system, EquipmentSlot, ItemStack } from "@minecraft/server";
import { ActionFormData, ModalFormData } from "@minecraft/server-ui";

const NOTES = [
  { id: "A", label: "§e●  A", pitch: 1.00 },
  { id: "UP", label: "§b▲  C↑", pitch: 1.26 },
  { id: "DOWN", label: "§d▼  C↓", pitch: 0.79 },
  { id: "LEFT", label: "§a◀  C←", pitch: 0.89 },
  { id: "RIGHT", label: "§6▶  C→", pitch: 1.12 }
];
const SONGS = [
  // Original fan-made note sequences with an Ocarina-of-Time-style control layout.
  // These intentionally are not transcriptions of the copyrighted game melodies.
  { id: "storms", disk: "zelda:disk_storms", tag: "zelda_song_storms", name: "Sturm-Motiv", color: "§b", notes: ["A","DOWN","LEFT","A","UP","RIGHT"] },
  { id: "sun", disk: "zelda:disk_sun", tag: "zelda_song_sun", name: "Sonnen-Motiv", color: "§e", notes: ["RIGHT","UP","A","LEFT","DOWN","A"] },
  { id: "lullaby", disk: "zelda:disk_lullaby", tag: "zelda_song_lullaby", name: "Wiegen-Motiv", color: "§d", notes: ["LEFT","A","UP","RIGHT","DOWN","LEFT"] },
  { id: "time", disk: "zelda:disk_time", tag: "zelda_song_time", name: "Zeit-Motiv", color: "§a", notes: ["DOWN","RIGHT","A","UP","LEFT","A"] },
  { id: "epona", disk: "zelda:disk_epona", tag: "zelda_song_epona", name: "Pferde-Motiv", color: "§7", notes: ["UP","RIGHT","DOWN","A","LEFT","UP"], later: true }
];
const sequences = new Map();
const naviByPlayer = new Map();
const naviAlertUntil = new Map();
const naviDialogueIndex = new Map();
const NAVI_DIALOGUES = [
  "§bNavi: §fSchau dich gut um. Geheimnisse verstecken sich manchmal direkt vor deiner Nase!",
  "§bNavi: §fVier Herzteile ergeben zusammen einen Herzcontainer.",
  "§bNavi: §fDie kleine grüne Rubintasche fasst 99 Rubine.",
  "§bNavi: §fDie mittlere blaue Rubintasche fasst 200 Rubine.",
  "§bNavi: §fDie große rote Rubintasche fasst 300 Rubine.",
  "§bNavi: §fDie königliche lila Rubintasche fasst bis zu 500 Rubine!",
  "§bNavi: §fHeb dir deine Rubine gut auf. Im Händlerladen findest du nützliche Sachen.",
  "§bNavi: §fWenn du dich verirrst, schau dich in Ruhe um und such nach auffälligen Wegen.",
  "§bNavi: §fDeine Ocarina kann besondere Effekte auslösen. Probier deine freigeschalteten Lieder!",
  "§bNavi: §fHöhlen sind spannend, aber nimm dich vor Monstern in Acht.",
  "§bNavi: §fManchmal lohnt es sich, einen Weg noch einmal abzusuchen.",
  "§bNavi: §fBehalte deine Herzen im Auge. Ein sicherer Rückzug ist manchmal klug.",
  "§bNavi: §fTruhen und versteckte Ecken könnten wertvolle Überraschungen bergen.",
  "§bNavi: §fNicht jeder Weg führt geradeaus. Erkunde auch die Seitenpfade!",
  "§bNavi: §fWenn du ein Herzteil findest, bewahre es gut auf.",
  "§bNavi: §fEin voller Rubinbeutel ist toll — aber ein paar Rubine auszugeben kann sich lohnen.",
  "§bNavi: §fHör auf die Geräusche deiner Umgebung. Sie können dir Hinweise geben.",
  "§bNavi: §fMach ruhig eine Pause und genieß die Aussicht. Hyrule läuft nicht weg!",
  "§bNavi: §fWenn du neue Ausrüstung findest, prüfe, ob sie dir auf dem Abenteuer hilft.",
  "§bNavi: §fVergiss nicht: Du musst nicht jeden Kampf annehmen.",
  "§bNavi: §fEin ungewöhnlicher Block oder eine einsame Ecke kann ein Hinweis sein.",
  "§bNavi: §fSammle verschiedenfarbige Rubine — sie haben unterschiedliche Werte.",
  "§bNavi: §fDer Händler bleibt an seinem Platz. Sprich ihn an, wenn du etwas kaufen möchtest.",
  "§bNavi: §fWenn dein Beutel voll ist, könnte eine größere Rubintasche helfen.",
  "§bNavi: §fDie Welt ist groß. Such dir ein Ziel und erkunde Schritt für Schritt.",
  "§bNavi: §fDu kannst deine Ocarina öffnen und die verfügbaren Noten ansehen.",
  "§bNavi: §fManche Musiknoten schalten erst ein Lied frei, wenn du sie gefunden hast.",
  "§bNavi: §fDer Morgen ist ein guter Zeitpunkt, um einen neuen Weg auszuprobieren.",
  "§bNavi: §fIn der Nacht ist es besonders wichtig, aufmerksam zu bleiben.",
  "§bNavi: §fWenn du eine Truhe siehst, schau nach, ob du sie öffnen kannst.",
  "§bNavi: §fEine gute Vorbereitung macht jedes Abenteuer leichter.",
  "§bNavi: §fDenk daran, dass Herzcontainer deine Zelda-Herzen dauerhaft erweitern.",
  "§bNavi: §fDie ersten drei Zelda-Herzen sind frei. Weitere schaltest du mit Herzcontainern frei.",
  "§bNavi: §fWenn etwas gefährlich aussieht, nähere dich vorsichtig.",
  "§bNavi: §fVielleicht gibt es in der Nähe einen Händler oder einen interessanten Ort.",
  "§bNavi: §fEin kleiner Umweg kann zu einer großen Entdeckung führen.",
  "§bNavi: §fAchte auf dein Inventar. Platz für Beute ist immer praktisch.",
  "§bNavi: §fDu bist weiter gekommen, als du denkst. Bleib neugierig!",
  "§bNavi: §fWenn du Rubine ausgibst, überleg, was dir gerade am meisten hilft.",
  "§bNavi: §fProbier deine Ausrüstung aus, wenn du an einem sicheren Ort bist.",
  "§bNavi: §fEin Herzteil ist nicht dasselbe wie ein Herzcontainer — vier Teile ergeben einen Container.",
  "§bNavi: §fAchte auf deine Umgebung, statt immer nur geradeaus zu laufen.",
  "§bNavi: §fEs ist okay, einen schwierigen Ort später noch einmal zu besuchen.",
  "§bNavi: §fVielleicht findest du in einer Höhle Rubine oder andere Schätze.",
  "§bNavi: §fWenn du einen neuen Bereich betrittst, merk dir den Rückweg.",
  "§bNavi: §fDie Ocarina ist mehr als ein hübsches Instrument — sie reagiert auf deine Notenfolge.",
  "§bNavi: §fIch bleibe an deiner Seite, solange du mein Navi-Item bei dir hältst.",
  "§bNavi: §fWenn du mich brauchst, benutze das Navi-Item noch einmal: Hey! Listen!",
  "§bNavi: §fGib nicht auf, wenn du ein Rätsel nicht sofort löst.",
  "§bNavi: §fHyrule steckt voller kleiner Überraschungen. Lass dir Zeit beim Erkunden.",
  "§bNavi: §fAbenteuer macht mehr Spaß, wenn du vorbereitet bist. Viel Glück!"
];
const NAVI_EVENT_DIALOGUES = [
  "§bNavi: §fHey! Schön, dass du da bist! Los geht’s auf Abenteuerreise! ✨",
  "§bNavi: §fHör mal! In deiner Umgebung könnten sich Geheimnisse verstecken.",
  "§bNavi: §fSammle Rubine! Vielleicht findest du bald eine bessere Tasche.",
  "§bNavi: §fVier Herzteile ergeben einen Herzcontainer. Bleib aufmerksam!",
  "§bNavi: §fDeine Ocarina kann besondere Effekte auslösen. Probier deine Melodien aus!",
  "§bNavi: §fWir schaffen das zusammen! Schritt für Schritt durch Hyrule!",
  "§bNavi: §fSchau in Truhen und erkunde Höhlen. Man weiß nie, was man findet!",
  "§bNavi: §fVergiss nicht, auf deine Herzen aufzupassen. Sicherheit geht vor!"
];
const naviDialogueCooldown = new Map();
const naviLastLowHealth = new Map();
function naviSay(player, message) {
  try { player.sendMessage(message); player.playSound("random.orb", { volume: 0.45, pitch: 1.35 }); } catch {}
}
function naviRandomLine(player) {
  const now = system.currentTick;
  const last = naviDialogueCooldown.get(player.id) || -10000;
  if (now - last < 100) {
    player.sendMessage("§8Navi braucht kurz einen Moment, bevor sie wieder etwas sagt.");
    return;
  }
  naviDialogueCooldown.set(player.id, now);
  const line = NAVI_DIALOGUES[Math.floor(Math.random() * NAVI_DIALOGUES.length)];
  naviSay(player, line);
}
const stormRainOn = new Map();
function sequenceText(seq) {
  if (!seq || seq.length === 0) return "§8— — — — — —";
  return seq.map(id => { const n = NOTES.find(item => item.id === id); return n ? n.label : id; }).join(" §7· ");
}
function playNote(player, note) {
  try { player.playSound("note.harp", { volume: 1, pitch: note.pitch }); } catch {}
  let seq = sequences.get(player.id) || [];
  seq.push(note.id); if (seq.length > 6) seq = seq.slice(-6); sequences.set(player.id, seq);
  const song = SONGS.filter(s => !s.later).find(s => s.notes.length === seq.length && s.notes.every((n, i) => seq[i] === n));
  if (song) {
    sequences.set(player.id, []);
    if (!player.hasTag(song.tag)) {
      player.sendMessage(`§c${song.name} ist noch gesperrt! §7Finde zuerst die passende farbige Musiknote.`);
      try { player.playSound("random.click", { volume: 0.45, pitch: 0.65 }); } catch {}
      return;
    }
    runSongEffect(player, song);
    return;
  }
  const possible = SONGS.filter(s => !s.later).some(s => seq.every((n, i) => s.notes[i] === n));
  if (!possible) { sequences.set(player.id, [note.id]); player.sendMessage("§8Die Melodie beginnt mit der letzten Note neu."); }
}
function runSongEffect(player, song) {
  player.sendMessage(`§d♫ §f${song.name}!`);
  try {
    if (song.id === "storms") {
      const key = player.dimension.id; const isRainingFromSong = stormRainOn.get(key) === true;
      if (isRainingFromSong) { player.dimension.runCommand("weather clear 6000"); stormRainOn.set(key, false); player.sendMessage("§b☀ §7Der Sturm endet – der Regen hört auf!"); }
      else { player.dimension.runCommand("weather rain 6000"); stormRainOn.set(key, true); player.sendMessage("§b☔ §7Die Hymne des Sturms ruft den Regen!"); }
    } else if (song.id === "sun") {
      const t = world.getTimeOfDay(); const goingToDay = t >= 13000 && t < 23000;
      world.setTimeOfDay(goingToDay ? 1000 : 14000); player.sendMessage(goingToDay ? "§e☀ §7Es wird Tag!" : "§9☾ §7Es wird Nacht!");
    } else if (song.id === "lullaby") {
      // Deliberately no music playback: avoid starting looping/background Minecraft music.
      player.sendMessage("§d♪ §7Zeldas Wiegenlied wurde gespielt. (Ohne Hintergrundmusik)");
    } else if (song.id === "time") {
      world.setTimeOfDay(1000); player.dimension.runCommand("weather clear 6000"); player.sendMessage("§e⌛ §7Die Zeit wird auf Morgen gesetzt und der Himmel klart auf!");
    } else if (song.id === "epona") {
      player.sendMessage("§7Eponas Lied wird in einer späteren Version aktiviert.");
    }
  } catch { player.sendMessage("§cDie Lied-Aktion ist fehlgeschlagen. Prüfe die Script-API und die Welt-Einstellungen."); }
}
function unlockSong(player, song) {
  if (song.later) { player.sendMessage("§7Eponas Lied wird später freigeschaltet."); return; }
  if (player.hasTag(song.tag)) { player.sendMessage(`${song.color}${song.name} §7ist bereits freigeschaltet!`); return; }
  player.addTag(song.tag);
  player.sendMessage(`${song.color}♫ ${song.name} §aist jetzt freigeschaltet! §7Öffne die Ocarina und wähle „Lied spielen“.`);
  try { player.playSound("random.levelup", { volume: 0.7, pitch: 1.2 }); } catch {}
}
async function showOcarina(player) {
  const seq = sequences.get(player.id) || [];
  const unlocked = SONGS.filter(s => !s.later && player.hasTag(s.tag));
  const songLines = SONGS.map(s => {
    const status = s.later ? "§8[SPÄTER]" : player.hasTag(s.tag) ? "§a[OFFEN]" : "§c[GESPERRT]";
    return `${s.color}${s.name}§r ${status}\n§7${s.notes.map(n => n === "A" ? "A" : ({UP:"C↑",DOWN:"C↓",LEFT:"C←",RIGHT:"C→"}[n])).join(" → ")}`;
  }).join("\n\n");
  const form = new ActionFormData().title("§1§lOCARINA §r§8• §7Musiknoten")
    .body("§b✦ §fOcarina-Tasten wie im Zelda-Stil\n§8A · C↑ · C↓ · C← · C→. Eigene Retro-Melodien; Effekte nach dem Freischalten.\n\n§fDEINE MELODIE\n" + sequenceText(seq) +
      "\n\n§fLIEDER & NOTENFOLGEN\n" + songLines +
      "\n\n§8Benutze die passende farbige Musiknote, um ein Lied freizuschalten.");
  for (const note of NOTES) form.button(note.label);
  form.button("§c↺  Melodie zurücksetzen");
  for (const song of unlocked) form.button(`${song.color}♫ §fLied spielen: ${song.name}`);
  form.button("§8Menü schließen");
  try {
    const result = await form.show(player); if (result.canceled) return;
    if (result.selection < NOTES.length) { playNote(player, NOTES[result.selection]); system.runTimeout(() => showOcarina(player), 1); return; }
    if (result.selection === NOTES.length) { sequences.set(player.id, []); try { player.playSound("random.click", { volume: 0.5, pitch: 0.8 }); } catch {} system.runTimeout(() => showOcarina(player), 1); return; }
    const songIndex = result.selection - NOTES.length - 1;
    if (songIndex >= 0 && songIndex < unlocked.length) { runSongEffect(player, unlocked[songIndex]); system.runTimeout(() => showOcarina(player), 1); }
  } catch { player.sendMessage("§cOcarina-Menü konnte nicht geöffnet werden. Prüfe das aktivierte Verhaltenspaket."); }
}
// Heart-system safety clamp: runs for every player, even when Navi is not summoned.
 // A new counter key starts this corrected version at the intended 3 unlocked hearts.
system.runInterval(() => {
  for (const player of world.getAllPlayers()) {
    try {
      let count = player.getDynamicProperty("zelda:heart_container_count_v2");
      if (typeof count !== "number" || !Number.isFinite(count)) {
        count = 0;
        player.setDynamicProperty("zelda:heart_container_count_v2", 0);
      }
      count = Math.max(0, Math.min(9, Math.floor(count)));
      const maxUnlockedHealth = (3 + count) * 2;
      const health = player.getComponent("minecraft:health");
      if (health && health.currentValue > maxUnlockedHealth) {
        health.setCurrentValue(maxUnlockedHealth);
      }
    } catch {}
  }
}, 3);

// Navi follows only while the Navi item is held in the main hand or offhand.
function hasNaviEquipped(player) {
  try {
    const inv = player.getComponent("minecraft:inventory");
    const container = inv && inv.container;
    if (container && typeof player.selectedSlotIndex === "number") {
      const held = container.getItem(player.selectedSlotIndex);
      if (held && held.typeId === "zelda:navi_item") return true;
    }
  } catch {}
  try {
    const eq = player.getComponent("minecraft:equippable");
    const offhand = eq && eq.getEquipment(EquipmentSlot.Offhand);
    if (offhand && offhand.typeId === "zelda:navi_item") return true;
  } catch {}
  return false;
}
system.runInterval(() => {
  for (const player of world.getAllPlayers()) {
    let naviId = naviByPlayer.get(player.id);
    // Equipping Navi in either hand summons her automatically, including the offhand.
    if (!naviId && hasNaviEquipped(player)) {
      try {
        const p = player.location;
        const spawned = player.dimension.spawnEntity("zelda:navi", { x: p.x + 0.8, y: p.y + 1.7, z: p.z + 0.2 });
        spawned.nameTag = "§bNavi";
        naviByPlayer.set(player.id, spawned.id);
        naviId = spawned.id;
      } catch {}
    }
    if (!naviId) continue;
    // If Navi is no longer held, remove the fairy.
    if (!hasNaviEquipped(player)) {
      try { const oldNavi = world.getEntity(naviId); if (oldNavi) oldNavi.remove(); } catch {}
      naviByPlayer.delete(player.id);
      naviAlertUntil.delete(player.id);
      continue;
    }
    let navi;
    try { navi = world.getEntity(naviId); } catch {}
    if (!navi) { naviByPlayer.delete(player.id); continue; }
    const p = player.location;
    let view = { x: 0, y: 0, z: 1 };
    let rotation = { x: 0, y: 0 };
    try { view = player.getViewDirection(); } catch {}
    try { rotation = player.getRotation(); } catch {}
    const sideX = -view.z * 0.65;
    const sideZ = view.x * 0.65;
    const target = {
      x: p.x + view.x * 0.75 + sideX,
      y: p.y + 1.45 + Math.sin(system.currentTick / 8) * 0.08,
      z: p.z + view.z * 0.75 + sideZ
    };
    try { navi.teleport(target, { dimension: player.dimension, rotation }); } catch {}
    const alert = naviAlertUntil.get(player.id) || 0;
    try { navi.nameTag = system.currentTick < alert ? "§bNavi §e!" : "§bNavi"; } catch {}
  }
}, 3);
// Zelda merchant NPC: remains still and opens the existing rupee shop when interacted with.
try {
  world.afterEvents.playerInteractWithEntity.subscribe(event => {
    try {
      if (event.target && event.target.typeId === "zelda:merchant") {
        system.run(() => showZeldaMenu(event.player));
      } else if (event.target && event.target.typeId === "zelda:navi") {
        system.run(() => naviRandomLine(event.player));
      }
    } catch {}
  });
} catch {}

world.afterEvents.itemUse.subscribe(event => {
  const player = event.source;
  if (!player || !player.typeId || player.typeId !== "minecraft:player") return;
  const typeId = event.itemStack.typeId;
  // Deku-Schleuder: verbraucht einen Deku-Kern und verschießt ein leichtes Schneeball-Projektil.
  if (typeId === "zelda:deku_slingshot") {
    system.run(() => {
      try {
        const inv = player.getComponent("minecraft:inventory")?.container;
        if (!inv) return;
        let seedSlot = -1;
        for (let i = 0; i < inv.size; i++) {
          const stack = inv.getItem(i);
          if (stack && stack.typeId === "zelda:deku_seed") { seedSlot = i; break; }
        }
        if (seedSlot < 0) { player.sendMessage("§aDeku-Schleuder: §fDu brauchst einen Deku-Kern!"); return; }
        const ammo = inv.getItem(seedSlot);
        if (ammo.amount > 1) { ammo.amount -= 1; inv.setItem(seedSlot, ammo); } else { inv.setItem(seedSlot, undefined); }
        const head = player.getHeadLocation();
        const dir = player.getViewDirection();
        const start = { x: head.x + dir.x * 0.8, y: head.y + dir.y * 0.8, z: head.z + dir.z * 0.8 };
        const shot = player.dimension.spawnEntity("minecraft:snowball", start);
        shot.applyImpulse({ x: dir.x * 1.8, y: dir.y * 1.8, z: dir.z * 1.8 });
        try { player.playSound("random.bow", { volume: 0.6, pitch: 1.5 }); } catch {}
      } catch { try { player.sendMessage("§cDie Deku-Schleuder konnte nicht abgefeuert werden."); } catch {} }
    });
    return;
  }
  if (typeId === "zelda:navi_item") {
    system.run(() => {
      const existingId = naviByPlayer.get(player.id);
      let existing;
      if (existingId) { try { existing = world.getEntity(existingId); } catch {} }
      if (!existing) {
        const p = player.location;
        try {
          const navi = player.dimension.spawnEntity("zelda:navi", {x:p.x+0.8,y:p.y+1.7,z:p.z+0.2});
          navi.nameTag = "§bNavi"; naviByPlayer.set(player.id, navi.id);
          player.sendMessage("§b✦ Navi ist bei dir! Benutze das Item erneut für „Hey! Listen!“.");
        } catch { player.sendMessage("§cNavi konnte nicht erscheinen. Prüfe, ob beide Add-on-Pakete aktiviert sind."); }
      } else {
        naviAlertUntil.set(player.id, system.currentTick + 50);
        try { existing.nameTag = "§bNavi §e!"; } catch {}
        player.sendMessage("§b✦ Navi: §fHey! Listen! §e❗");
        try { player.playSound("zelda.navi_hey_listen", {volume:1.0,pitch:1.0}); } catch {}
        // Command playback is a second route for Bedrock versions that ignore custom sound IDs in playSound.
        try { player.runCommand("playsound zelda.navi_hey_listen @s ~ ~ ~ 1 1"); } catch {}
      }
    });
    return;
  }
  const song = SONGS.find(s => s.disk === typeId);
  if (song) { system.run(() => unlockSong(player, song)); return; }
  if (typeId === "zelda:ocarina") system.run(() => showOcarina(player));
});


// Zelda OOT Ultimate V18: default wallet holds 99; big wallet raises capacity to 200.
function objective() {
  try { return world.scoreboard.getObjective("zelda_rubies") || world.scoreboard.addObjective("zelda_rubies", "Rubine"); } catch { return undefined; }
}
function rubyCapacity(player) {
  try { const cap=Number(player.getDynamicProperty("zelda:rupee_capacity") || 99); return cap >= 500 ? 500 : cap >= 300 ? 300 : cap >= 200 ? 200 : 99; } catch { return 99; }
}
function rubies(player) {
  const cap = rubyCapacity(player);
  try { const v = player.getDynamicProperty("zelda_rubies"); if (typeof v === "number") return Math.min(cap, Math.max(0, Math.floor(v))); } catch {}
  try { const o=objective(); return o && player.scoreboardIdentity ? Math.min(cap, Math.max(0, o.getScore(player.scoreboardIdentity) || 0)) : 0; } catch { return 0; }
}
function setRubies(player, value) {
  const v = Math.min(rubyCapacity(player), Math.max(0, Math.floor(value)));
  try { player.setDynamicProperty("zelda_rubies", v); } catch {}
  try { const o=objective(); if(o && player.scoreboardIdentity) o.setScore(player.scoreboardIdentity, v); } catch {}
}
async function showZeldaMenu(player) {
  const health=player.getComponent("minecraft:health");
  const current=health ? Math.ceil(health.currentValue/2) : 0;
  const max=health ? Math.ceil(health.effectiveMax/2) : 10;
  const r=rubies(player); const cap=rubyCapacity(player);
  const form=new ActionFormData().title("§2ZELDA OOT §6• Rubin-Shop")
    .body(`§c♥ §fHerzen: ${current}/${max}\n§6♦ §fDeine Rubine: ${r}/${cap}\n\n§7Wähle einen Gegenstand. Rubine werden nur abgezogen, wenn der Kauf gelingt.\n§8Rubine: grün +1, blau +5, rot +20, violett +50.`)
    .button("§a♥ Herzen auffüllen §8(10 Rubine)")
    .button("§eGoldener Apfel §8(8 Rubine)")
    .button("§bBogen §8(15 Rubine)")
    .button("§6Shop schließen");
  try {
    const res=await form.show(player); if(res.canceled || res.selection===3) return;
    // Re-read the balance after the form closes, so an outdated menu cannot overspend.
    const balance=rubies(player);
    if(res.selection===0){
      if(balance<10){player.sendMessage("§cDu brauchst 10 Rubine.");return;}
      const h=player.getComponent("minecraft:health");
      if(!h || h.currentValue>=h.effectiveMax){player.sendMessage("§eDeine Herzen sind bereits voll!");return;}
      setRubies(player,balance-10);
      h.setCurrentValue(Math.min(h.effectiveMax,h.currentValue+4));
      player.sendMessage(`§c♥ §aHerzen aufgefüllt! §6(-10 Rubine, übrig: ${rubies(player)})`);
      return;
    }
    const cost=res.selection===1 ? 8 : 15;
    const type=res.selection===1 ? "minecraft:golden_apple" : "minecraft:bow";
    const label=res.selection===1 ? "Goldener Apfel" : "Bogen";
    if(balance<cost){player.sendMessage(`§cDu brauchst ${cost} Rubine für ${label}.`);return;}
    const inv=player.getComponent("minecraft:inventory")?.container;
    if(!inv){player.sendMessage("§cInventar nicht erreichbar – es wurden keine Rubine abgezogen.");return;}
    // Add the item first. If inventory is full, addItem returns the leftover stack.
    const leftover=inv.addItem(new ItemStack(type,1));
    if(leftover){player.sendMessage("§cDein Inventar ist voll! Es wurden keine Rubine abgezogen.");return;}
    setRubies(player,balance-cost);
    player.sendMessage(`§a${label} gekauft! §6-${cost} Rubine §7(übrig: ${rubies(player)})`);
  } catch (e) { player.sendMessage("§cDer Shop konnte nicht abgeschlossen werden. Bitte erneut versuchen."); }
}
// V42 HUD-Fix: Die normale Minecraft-Herzleiste bleibt sichtbar.
world.afterEvents.itemUse.subscribe(event=>{
 const p=event.source; if(!p || p.typeId!=="minecraft:player") return;
 const id=event.itemStack.typeId;
 if(id==="zelda:heart"){ system.run(()=>{ try { const h=p.getComponent("minecraft:health"); if(!h){ p.sendMessage("§cDein Herz konnte nicht verwendet werden."); return; } const unlocked=Math.min(12,3+Math.max(0,Math.floor(Number(p.getDynamicProperty("zelda:heart_container_count_v2")||0)))); const maxUnlockedHealth=unlocked*2; const before=Math.min(h.currentValue,maxUnlockedHealth); if(h.currentValue>maxUnlockedHealth) h.setCurrentValue(maxUnlockedHealth); if(before>=maxUnlockedHealth){ p.sendMessage("§c♥ Deine freigeschalteten Herzen sind bereits voll. Weitere Herzen musst du zuerst mit Herzteilen freischalten."); return; } p.runCommand("clear @s zelda:heart 0 1"); const after=Math.min(maxUnlockedHealth,before+4); h.setCurrentValue(after); const restored=Math.max(0,after-before); p.sendMessage(`§c♥ Herz benutzt! §f${restored} Lebenspunkte wiederhergestellt.`); } catch { p.sendMessage("§cHerz konnte nicht verwendet werden."); } }); }
 // The two heart items are visually/name-swapped in V49, so their use behavior follows the new labels.
 if(id==="zelda:heart_container"){ system.run(()=>{ try{ p.runCommand("clear @s zelda:heart_container 0 1"); const count=(p.getDynamicProperty("zelda:heart_piece_count")||0)+1; if(count>=4){p.setDynamicProperty("zelda:heart_piece_count",0);p.runCommand("give @s zelda:heart_piece 1");p.sendMessage("§c♥ Vier Herzteile! Du erhältst einen Herzcontainer.");}else{p.setDynamicProperty("zelda:heart_piece_count",count);p.sendMessage(`§c♥ Herzteil ${count}/4 gesammelt.`);} }catch{p.sendMessage("§cHerzteil konnte nicht gesammelt werden.");} }); }
 if(id==="zelda:heart_piece"){ system.run(()=>{ try{
   const oldCount=Math.max(0,Math.floor(Number(p.getDynamicProperty("zelda:heart_container_count_v2")||0)));
   if(oldCount>=9){ p.sendMessage("§c♥ Du hast bereits alle 12 Herzen freigeschaltet!"); return; }
   const count=oldCount+1; p.setDynamicProperty("zelda:heart_container_count_v2",count);
   try { p.runCommand("clear @s zelda:heart_piece 0 1"); } catch {}
   // Native Bedrock HUD has 12 heart slots; a container unlocks one more and fills it.
   const h=p.getComponent("minecraft:health"); if(h)h.setCurrentValue(Math.min(h.effectiveMax,h.currentValue+2));
   p.sendMessage(`§c♥ Herzcontainer benutzt! ${Math.min(12,3+count)}/12 Herzen freigeschaltet.`);
 }catch{p.sendMessage("§cHerzcontainer konnte nicht verwendet werden.");} }); }
 if(id==="zelda:rupee_green" || id==="zelda:rupee_blue" || id==="zelda:rupee_red" || id==="zelda:rupee_purple"){ system.run(()=>{const n= id==="zelda:rupee_blue" ? 5 : id==="zelda:rupee_red" ? 20 : id==="zelda:rupee_purple" ? 50 : 1;const before=rubies(p);setRubies(p,before+n);const after=rubies(p);try{p.runCommand(`clear @s ${id} 0 1`);}catch{} if(before>=rubyCapacity(p) || after===before){p.sendMessage(`§a♦ Deine Rubintasche ist voll! Maximum: ${rubyCapacity(p)} Rubine.`);}else if(before+n>rubyCapacity(p)){p.sendMessage(`§a♦ Du hast ${after-before} Rubine erhalten. Deine Rubintasche ist jetzt voll (${rubyCapacity(p)}/${rubyCapacity(p)})!`);}else{p.sendMessage(`§6♦ +${after-before} Rubin${after-before!==1?"e":""}!`);}}); }
 if(id==="zelda:small_rupee_purse"){ system.run(()=>{ try {
   const oldCap=rubyCapacity(p);
   if(oldCap>=99){ p.sendMessage("§a♦ Die kleine Rubintasche ist bereits aktiv (Limit: 99 Rubine)."); return; }
   p.runCommand("clear @s zelda:small_rupee_purse 0 1");
   p.setDynamicProperty("zelda:rupee_capacity",99);
   p.sendMessage("§a♦ Kleine Rubintasche (grün) aktiviert! Dein Limit ist 99 Rubine.");
 } catch { p.sendMessage("§cDie kleine Rubintasche konnte nicht aktiviert werden."); } }); }
 if(id==="zelda:big_rupee_purse"){ system.run(()=>{ try {
   const oldCap=rubyCapacity(p);
   if(oldCap>=200){ p.sendMessage("§b♦ Du besitzt bereits mindestens die mittlere Rubintasche."); return; }
   p.runCommand("clear @s zelda:big_rupee_purse 0 1");
   p.setDynamicProperty("zelda:rupee_capacity",200); setRubies(p, rubies(p));
   p.sendMessage("§b♦ Mittlere Rubintasche (blau) erhalten! Dein neues Limit ist 200 Rubine.");
   try { p.playSound("random.levelup",{volume:0.8,pitch:1.2}); } catch {}
 } catch { p.sendMessage("§cDie große Rubintasche konnte nicht aktiviert werden."); } }); }
 if(id==="zelda:giant_rupee_purse"){ system.run(()=>{ try {
   const oldCap=rubyCapacity(p);
   if(oldCap>=300){ p.sendMessage("§c♦ Du besitzt bereits mindestens die große Rubintasche."); return; }
   p.runCommand("clear @s zelda:giant_rupee_purse 0 1");
   p.setDynamicProperty("zelda:rupee_capacity",300); setRubies(p, rubies(p));
   p.sendMessage("§c♦ Große Rubintasche (rot) erhalten! Dein neues Limit ist 300 Rubine.");
   try { p.playSound("random.levelup",{volume:0.8,pitch:1.25}); } catch {}
 } catch { p.sendMessage("§cDie riesige Rubintasche konnte nicht aktiviert werden."); } }); }
 if(id==="zelda:royal_rupee_purse"){ system.run(()=>{ try {
   const oldCap=rubyCapacity(p);
   if(oldCap>=500){ p.sendMessage("§a♦ Du besitzt bereits die königliche Rubintasche (500 Rubine)."); return; }
   p.runCommand("clear @s zelda:royal_rupee_purse 0 1");
   p.setDynamicProperty("zelda:rupee_capacity",500); setRubies(p, rubies(p));
   p.sendMessage("§d♦ Königliche Rubintasche (lila) erhalten! Dein neues Limit ist 500 Rubine.");
   try { p.playSound("random.levelup",{volume:0.8,pitch:1.3}); } catch {}
 } catch { p.sendMessage("§cDie königliche Rubintasche konnte nicht aktiviert werden."); } }); }
});


// V48: Zelda hearts HUD. The action bar shows 12 slots (3 unlocked initially);
// the native player health is capped to 3 hearts where the Bedrock attribute command is supported.
const healthCapApplied = new Set();
function applyZeldaStartingHealth(p) {
  try {
    // Bedrock's attribute command is not available on every mobile build.
    // Health Boost adds two native heart slots (12 total); current health is still
    // capped by the Zelda unlock counter below, starting at 3 hearts.
    try { p.runCommand("effect @s health_boost 999999 0 true"); } catch {}
    const h = p.getComponent("minecraft:health");
    if (h && h.currentValue > 6) h.setCurrentValue(6);
    healthCapApplied.add(p.id);
  } catch {}
}
world.afterEvents.playerSpawn.subscribe(ev => {
  system.runTimeout(() => applyZeldaStartingHealth(ev.player), 2);
});
system.runInterval(() => {
  for (const p of world.getAllPlayers()) {
    try {
      if (!healthCapApplied.has(p.id)) applyZeldaStartingHealth(p);
      // Navi compares health with the player's unlocked Zelda hearts, not the 12-slot HUD.
      // At the starting maximum of 3 full hearts, she must not warn the player to heal.
      const health = p.getComponent("minecraft:health");
      const unlockedForNavi = Math.min(12, 3 + Math.max(0, Math.floor(Number(p.getDynamicProperty("zelda:heart_container_count_v2") || 0))));
      const maxUnlockedHealthForNavi = unlockedForNavi * 2;
      const nowTick = system.currentTick;
      const lastWarn = naviLastLowHealth.get(p.id) || 0;
      if (health && health.currentValue < maxUnlockedHealthForNavi && health.currentValue <= 6 && naviByPlayer.has(p.id) && nowTick - lastWarn >= 600) {
        naviLastLowHealth.set(p.id, nowTick);
        naviSay(p, "§bNavi: §fHey, pass auf! Deine freigeschalteten Herzen sind nicht voll. Such dir einen sicheren Platz! ♥");
      }
      // Navi occasionally shares a short adventure hint while following; never more than once every 3 minutes.
      if (naviByPlayer.has(p.id)) {
        const lastHint = naviDialogueCooldown.get("hint:" + p.id) || 0;
        if (nowTick - lastHint >= 3600 && Math.random() < 0.025) {
          naviDialogueCooldown.set("hint:" + p.id, nowTick);
          naviSay(p, NAVI_DIALOGUES[Math.floor(Math.random() * NAVI_DIALOGUES.length)]);
        }
      }
      const r = rubies(p);
      // Show green only when the player reaches the current purse capacity (99, 200, 300, or 500). After upgrading, the old threshold is no longer green.
      const cap = rubyCapacity(p);
      const atCurrentLimit = r === cap;
      const rubyColor = atCurrentLimit ? "§a" : "§f";
      const gemColor = cap >= 500 ? "§d" : cap >= 300 ? "§c" : cap >= 200 ? "§b" : "§a";
      const unlockedHearts = Math.min(12, 3 + Math.max(0, Math.floor(Number(p.getDynamicProperty("zelda:heart_container_count_v2") || 0))));
      const openHearts = "§c" + "♥".repeat(unlockedHearts);
      const lockedHearts = "§8" + "♥".repeat(12 - unlockedHearts);
      p.onScreenDisplay.setActionBar(`${openHearts}${lockedHearts} §r${gemColor}♦ ${rubyColor}${r}§7/${cap}`);
    } catch {}
  }
}, 10);


// V50 Navi dialogue: friendly greeting when a player enters the world.
world.afterEvents.playerSpawn.subscribe(ev => {
  if (ev.initialSpawn) system.runTimeout(() => {
    try { naviSay(ev.player, "§bNavi: §fHey! Ich bin Navi! Ich begleite dich auf deinem Abenteuer. Hör mal! ✨"); } catch {}
  }, 40);
});


// V52: Navi reacts to actual world events, but only while she is summoned for that player.
// Per-player cooldown avoids flooding chat when many events happen quickly.
const naviEventCooldown = new Map();
function naviReact(player, key, message, cooldownTicks = 160) {
  try {
    if (!player || player.typeId !== "minecraft:player" || !naviByPlayer.has(player.id)) return;
    const now = system.currentTick;
    const cooldownKey = player.id + ":" + key;
    const last = naviEventCooldown.get(cooldownKey) || -100000;
    if (now - last < cooldownTicks) return;
    naviEventCooldown.set(cooldownKey, now);
    naviSay(player, "§bNavi: §f" + message);
  } catch {}
}

// Player takes damage: Navi notices the real hurt event.
try {
  world.afterEvents.entityHurt.subscribe(ev => {
    try {
      const victim = ev.hurtEntity;
      if (victim && victim.typeId === "minecraft:player") {
        const hp = victim.getComponent("minecraft:health");
        if (hp && hp.currentValue <= 6) {
          naviReact(victim, "danger", "Autsch! Du hast nur noch wenig Leben. Geh in Deckung! ♥", 200);
        } else {
          naviReact(victim, "hurt", "Pass auf! Du hast gerade Schaden bekommen.", 260);
        }
      }
      const attacker = ev.damageSource && ev.damageSource.damagingEntity;
      if (attacker && attacker.typeId === "minecraft:player" && victim && victim.typeId !== "minecraft:player") {
        naviReact(attacker, "combat", "Da ist ein Gegner! Bleib aufmerksam und schau auf deine Herzen.", 260);
      }
    } catch {}
  });
} catch {}

// Player defeats a mob.
try {
  world.afterEvents.entityDie.subscribe(ev => {
    try {
      const killer = ev.damageSource && ev.damageSource.damagingEntity;
      if (killer && killer.typeId === "minecraft:player") {
        naviReact(killer, "victory", "Gut gemacht! Der Gegner ist besiegt. Schau dich um, ob noch Gefahr droht.", 240);
      }
    } catch {}
  });
} catch {}

// Real block-break event: short reactions to mining and exploring.
try {
  world.afterEvents.playerBreakBlock.subscribe(ev => {
    try {
      const p = ev.player;
      const id = ev.brokenBlockPermutation && ev.brokenBlockPermutation.type.id;
      if (!p || !id) return;
      // V53: Grass can rarely hide a green Zelda rupee. The item is a real
      // zelda:rupee_green drop and can be collected like other world items.
      const isGrass = id === "minecraft:short_grass" || id === "minecraft:tall_grass" ||
        id === "minecraft:grass" || id === "minecraft:grass_block" ||
        id === "minecraft:fern" || id === "minecraft:large_fern";
      if (isGrass && Math.random() < 0.06) {
        try {
          const loc = ev.block.location;
          const dropAt = { x: loc.x + 0.5, y: loc.y + 0.55, z: loc.z + 0.5 };
          p.dimension.spawnItem(new ItemStack("zelda:rupee_green", 1), dropAt);
          try { p.playSound("random.orb", { volume: 0.45, pitch: 1.2 }); } catch {}
          naviReact(p, "grass_rupee", "Hey! Da war ein grüner Rubin im Gras! Sammle ihn ein! ♦", 400);
        } catch {}
      }
      if (id.includes("diamond_ore") || id.includes("emerald_ore") || id.includes("ancient_debris")) {
        naviReact(p, "rare_ore", "Wow! Das sieht wertvoll aus. Sammle es sicher ein! ✨", 500);
      } else if (id.includes("chest") || id.includes("barrel")) {
        naviReact(p, "container", "Hier war etwas versteckt. Schau nach, ob du alles Wichtige eingesammelt hast!", 500);
      } else if (!isGrass && Math.random() < 0.025) {
        naviReact(p, "mining", "Du gräbst dich tiefer hinein. Vergiss nicht, den Weg zurück im Blick zu behalten.", 600);
      }
    } catch {}
  });
} catch {}

// Pickups: Navi comments on Zelda heart items and rupees when the API reports them.
try {
  world.afterEvents.entityPickupItem.subscribe(ev => {
    try {
      const p = ev.entity;
      const stack = ev.itemStack;
      if (!p || p.typeId !== "minecraft:player" || !stack) return;
      if (stack.typeId === "zelda:heart_piece") {
        naviReact(p, "heart_piece", "Du hast ein Herzteil gefunden! Vier Herzteile ergeben einen Herzcontainer. ♥", 100);
      } else if (stack.typeId === "zelda:heart_container") {
        naviReact(p, "heart_container", "Ein Herzcontainer! Dein Zelda-Leben wird dauerhaft erweitert! ♥", 100);
      } else if (stack.typeId.startsWith("zelda:rupee_")) {
        naviReact(p, "rupee_pickup", "Ein Rubin! Heb ihn auf – vielleicht findest du beim Händler etwas Nützliches.", 240);
      }
    } catch {}
  });
} catch {}

// World time/weather are sampled rather than guessed from player position.
const naviWorldState = new Map();
system.runInterval(() => {
  for (const p of world.getAllPlayers()) {
    try {
      if (!naviByPlayer.has(p.id)) continue;
      const dim = p.dimension;
      const time = world.getTimeOfDay();
      const phase = time >= 12500 && time < 23500 ? "night" : "day";
      const old = naviWorldState.get(p.id) || {};
      if (old.phase && old.phase !== phase) {
        naviReact(p, "daynight", phase === "night" ? "Es wird dunkel. Halte Fackeln bereit und achte auf Monster! 🌙" : "Die Sonne geht auf! Ein neuer Tag für dein Abenteuer. ☀", 1200);
      }
      let raining = false;
      try { raining = dim.getWeather() === "Rain" || dim.getWeather() === "Thunder"; } catch {}
      if (old.raining === false && raining) naviReact(p, "rain", "Es fängt an zu regnen. Bleib aufmerksam, das Wetter kann sich schnell ändern.", 1200);
      naviWorldState.set(p.id, {phase, raining});
    } catch {}
  }
}, 100);
