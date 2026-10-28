package de.zelda;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import java.util.*;

public final class ZeldaPort implements ModInitializer {
 private static final String MOD = "zelda";
 public static final Map<String,Item> ITEMS = new LinkedHashMap<>();
 private static final Map<UUID,State> STATE = new HashMap<>();
 private static int ticks=0;
 static final class State {int rupees=0, cap=99, hearts=3, pieces=0, note=0; boolean navi=false; Set<String> unlocked=new HashSet<>(); List<String> notes=new ArrayList<>();}
 private static State state(ServerPlayer p){return STATE.computeIfAbsent(p.getUUID(),k->new State());}
 private static void say(ServerPlayer p,String m){p.sendSystemMessage(Component.literal(m));}
 private static void cmd(ServerPlayer p,String s){
  var server=p.level().getServer();
  server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(p.level()).withPosition(p.position()).withEntity(p).withSuppressedOutput(),s);
 }
 private static void consume(ServerPlayer p, InteractionHand hand){if(!p.getAbilities().instabuild)p.getItemInHand(hand).shrink(1);}
 private static void register(String id){
  Identifier location=Identifier.fromNamespaceAndPath(MOD,id);
  ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,location);
  Item item=Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key)));
  ITEMS.put(id,item);
 }
 @Override public void onInitialize(){
  for(String id: IDS)register(id);
  UseItemCallback.EVENT.register((player,level,hand)->{
   if(level.isClientSide() || !(player instanceof ServerPlayer p)) return InteractionResult.PASS;
   Identifier id=BuiltInRegistries.ITEM.getKey(player.getItemInHand(hand).getItem());
   if(!MOD.equals(id.getNamespace()))return InteractionResult.PASS;
   use(p,hand,id.getPath());return InteractionResult.SUCCESS;
  });
  ServerTickEvents.END_SERVER_TICK.register(server->{
   if(++ticks%20!=0)return;
   for(ServerPlayer p:server.getPlayerList().getPlayers()){
    State s=state(p);
    // Preview status display; persistent save and max-health attributes still need the full port.
    p.sendOverlayMessage(Component.literal("§c♥ "+s.hearts+"/12  §6♦ "+s.rupees+"/"+s.cap+(s.navi?"  §b✦ Navi":"")));
    if(s.navi && ticks%3600==0)say(p,"§bNavi: §fSchau dich gut um. Geheimnisse können überall sein!");
   }
  });
  CommandRegistrationCallback.EVENT.register((dispatcher,registryAccess,environment)->dispatcher.register(
   Commands.literal("zelda")
    .then(Commands.literal("shop").executes(ctx->{shop(ctx.getSource().getPlayerOrException());return 1;}))
    .then(Commands.literal("status").executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException(); State s=state(p);say(p,"Rubine "+s.rupees+"/"+s.cap+", Herzen "+s.hearts+", Herzteile "+s.pieces+"/4");return 1;}))
    .then(Commands.literal("buy").then(Commands.argument("option",IntegerArgumentType.integer(1,3)).executes(ctx->{buy(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"option"));return 1;})))
    .then(Commands.literal("note").then(Commands.argument("key",IntegerArgumentType.integer(1,5)).executes(ctx->{note(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"key")-1);return 1;})))
  ));
 }
 private static final String[] IDS = {"big_rupee_purse", "bottle_empty", "bottle_fairy", "bottle_green", "bottle_purple", "bottle_red", "bottle_yellow", "deku_seed", "deku_slingshot", "deku_stick", "disk_epona", "disk_lullaby", "disk_storms", "disk_sun", "disk_time", "giant_rupee_purse", "heart", "heart_container", "heart_piece", "navi_item", "ocarina", "royal_rupee_purse", "rupee_blue", "rupee_gold", "rupee_green", "rupee_purple", "rupee_red", "rupee_silver", "small_rupee_purse"};
 private static void use(ServerPlayer p,InteractionHand hand,String id){
  State s=state(p);
  switch(id){
   case "heart" -> {cmd(p,"effect give @s minecraft:instant_health 1 1 true");consume(p,hand);say(p,"§c♥ Ein Herz verwendet.");}
   case "heart_container" -> {consume(p,hand); if(++s.pieces>=4){s.pieces=0; s.hearts=Math.min(12,s.hearts+1);say(p,"§c♥ Herzcontainer freigeschaltet: "+s.hearts+"/12");}else say(p,"§c♥ Herzteil "+s.pieces+"/4");}
   case "heart_piece" -> {if(s.hearts<12){s.hearts++;consume(p,hand);say(p,"§c♥ Maximale Herzen: "+s.hearts);}else say(p,"§eMaximale Herzen erreicht.");}
   case "rupee_green","rupee_blue","rupee_red","rupee_purple","rupee_gold","rupee_silver" -> {
    int value=switch(id){case "rupee_blue"->5;case "rupee_red"->20;case "rupee_purple"->50;case "rupee_gold"->100;case "rupee_silver"->200;default->1;};
    if(s.rupees==s.cap){say(p,"§eRubintasche ist voll!");return;}
    s.rupees=Math.min(s.cap,s.rupees+value);consume(p,hand);say(p,"§6♦ "+s.rupees+"/"+s.cap);
   }
   case "small_rupee_purse","big_rupee_purse","giant_rupee_purse","royal_rupee_purse" -> {
    int cap=switch(id){case "royal_rupee_purse"->500;case "giant_rupee_purse"->300;case "big_rupee_purse"->200;default->99;};
    if(s.cap>=cap){say(p,"§eDu hast schon eine gleichwertige oder bessere Tasche.");return;}
    s.cap=cap;consume(p,hand);say(p,"§6♦ Tasche erweitert: "+cap);
   }
   case "ocarina" -> say(p,"§dOkarina: /zelda note 1-5 (A, C↑, C↓, C←, C→). Gespielte Noten: "+s.notes);
   case "disk_storms","disk_sun","disk_lullaby","disk_time","disk_epona" -> {s.unlocked.add(id.substring(5));say(p,"§d♫ Lied freigeschaltet: "+id.substring(5));}
   case "navi_item" -> {s.navi=!s.navi;say(p,s.navi?"§bNavi: Hey! Listen!":"§7Navi verabschiedet sich.");if(s.navi)cmd(p,"playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 1 1.4");}
   case "deku_slingshot" -> {if(!removeAmmo(p)){say(p,"§cDu brauchst Deku-Kerne!");return;}cmd(p,"execute at @s run summon minecraft:snowball ^ ^1.5 ^1");say(p,"§aDeku-Schleuder ausgelöst (Projektil-Physik noch nicht fertig).");}
   default -> say(p,"§7"+id+" – noch keine spezielle Nutzung portiert.");
  }
 }
 private static boolean removeAmmo(ServerPlayer p){
  Item item=ITEMS.get("deku_seed");
  for(int i=0;i<p.getInventory().getContainerSize();i++){
   ItemStack st=p.getInventory().getItem(i);if(st.is(item)){st.shrink(1);return true;}
  }return false;
 }
 private static final String[] SONG_NAMES={"storms","sun","lullaby","time"};
 private static final String[][] SONGS={{"A","DOWN","LEFT","A","UP","RIGHT"},{"RIGHT","UP","A","LEFT","DOWN","A"},{"LEFT","A","UP","RIGHT","DOWN","LEFT"},{"DOWN","RIGHT","A","UP","LEFT","A"}};
 private static final String[] KEYS={"A","UP","DOWN","LEFT","RIGHT"};
 private static void note(ServerPlayer p,int index){
  State s=state(p);s.notes.add(KEYS[index]);if(s.notes.size()>6)s.notes.remove(0);
  cmd(p,"playsound minecraft:block.note_block.harp player @s ~ ~ ~ 1 1");
  for(int i=0;i<SONGS.length;i++) if(s.notes.equals(Arrays.asList(SONGS[i]))){
   s.notes.clear();String song=SONG_NAMES[i];if(!s.unlocked.contains(song)){say(p,"§cDieses Lied ist noch nicht freigeschaltet: "+song);return;}
   say(p,"§d♫ "+song);
   switch(song){case "storms"->cmd(p,"weather rain 300");case "sun"->cmd(p,"time set day");case "time"->{cmd(p,"time set day");cmd(p,"weather clear 300");}default->say(p,"§dWiegen-Motiv gespielt.");}
   return;
  }
  boolean possible=false;for(String[] sequence:SONGS){boolean prefix=true;for(int i=0;i<s.notes.size();i++)if(!s.notes.get(i).equals(sequence[i])){prefix=false;break;}if(prefix)possible=true;}
  if(!possible){s.notes.clear();s.notes.add(KEYS[index]);}
  say(p,"§d♫ "+s.notes);
 }
 private static void shop(ServerPlayer p){State s=state(p);say(p,"§6♦ Händler – "+s.rupees+" Rubine | /zelda buy 1 = +2 Herzen (10), 2 = Goldapfel (8), 3 = Bogen (15)");}
 private static void buy(ServerPlayer p,int option){State s=state(p);int cost=option==1?10:option==2?8:15;
  if(s.rupees<cost){say(p,"§cNicht genug Rubine: "+cost+" benötigt.");return;}
  if(option==1)cmd(p,"effect give @s minecraft:instant_health 1 1 true");
  else if(option==2)cmd(p,"give @s minecraft:golden_apple 1");else cmd(p,"give @s minecraft:bow 1");
  s.rupees-=cost;say(p,"§aKauf abgeschlossen. Rest: "+s.rupees);
 }
}
