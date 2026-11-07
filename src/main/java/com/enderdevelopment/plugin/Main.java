package com.enderdevelopment.plugin;

import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private static final long TWO_DAYS = 172800000L;
    private static final String PREFIX = "[Imperial] ";
    private NamespacedKey crownKey;
    private Location chestLocation, parkourStart, parkourFinish;
    private long unlockAt;
    private boolean eventOpen;

    @Override public void onEnable() {
        saveDefaultConfig(); crownKey = new NamespacedKey(this, "imperial_crown"); loadState();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("imperial") != null) getCommand("imperial").setExecutor((s, c, l, a) -> command(s, c, l, a) != 0);
        Bukkit.getScheduler().runTaskTimer(this, t -> refreshHolder(), 20L, 20L);
        if (chestLocation == null) { World w = Bukkit.getWorlds().get(0); buildEvent(w, w.getSpawnLocation()); }
    }
    @Override public void onDisable() { saveState(); for (Player p : Bukkit.getOnlinePlayers()) clearEffects(p); }

    private void loadState() {
        String name = getConfig().getString("world"); World w = name == null ? null : Bukkit.getWorld(name);
        if (w == null && !Bukkit.getWorlds().isEmpty()) w = Bukkit.getWorlds().get(0); if (w == null) return;
        chestLocation = readLocation("chest", w); parkourStart = readLocation("parkour-start", w); parkourFinish = readLocation("parkour-finish", w);
        unlockAt = getConfig().getLong("unlock-at", System.currentTimeMillis() + TWO_DAYS); eventOpen = getConfig().getBoolean("event-open");
    }
    private Location readLocation(String path, World fallback) { if (!getConfig().contains(path + ".x")) return null; return new Location(fallback, getConfig().getDouble(path + ".x"), getConfig().getDouble(path + ".y"), getConfig().getDouble(path + ".z")); }
    private void saveState() { if (chestLocation == null) return; getConfig().set("world", chestLocation.getWorld().getName()); saveLocation("chest", chestLocation); saveLocation("parkour-start", parkourStart); saveLocation("parkour-finish", parkourFinish); getConfig().set("unlock-at", unlockAt); getConfig().set("event-open", eventOpen); saveConfig(); }
    private void saveLocation(String path, Location l) { if (l != null) { getConfig().set(path + ".x", l.getX()); getConfig().set(path + ".y", l.getY()); getConfig().set(path + ".z", l.getZ()); } }

    private void buildEvent(World w, Location spawn) {
        Location c = spawn.getBlock().getLocation(); chestLocation = c.clone().add(0, 4, 0); parkourStart = c.clone().add(20000, 2, 0); parkourFinish = parkourStart.clone().add(30, 0, 0);
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) { w.getBlockAt(c.getBlockX()+x, c.getBlockY(), c.getBlockZ()+z).setType(Material.POLISHED_DEEPSLATE); if (Math.abs(x)==8 || Math.abs(z)==8) for (int y=1;y<=7;y++) w.getBlockAt(c.getBlockX()+x,c.getBlockY()+y,c.getBlockZ()+z).setType(Material.STONE_BRICKS); }
        for (int[] t : new int[][]{{-8,-8},{-8,8},{8,-8},{8,8}}) for (int y=1;y<=11;y++) for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) w.getBlockAt(c.getBlockX()+t[0]+x,c.getBlockY()+y,c.getBlockZ()+t[1]+z).setType(Material.DEEPSLATE_BRICKS);
        for(int i=0;i<=30;i++){ int y=parkourStart.getBlockY()+i%3; w.getBlockAt(parkourStart.getBlockX()+i,y,parkourStart.getBlockZ()).setType(Material.POLISHED_BLACKSTONE); }
        w.getBlockAt(parkourFinish).setType(Material.EMERALD_BLOCK); unlockAt=System.currentTimeMillis()+TWO_DAYS; eventOpen=false; placeChest(); saveState();
    }
    private void placeChest() { Block b=chestLocation.getBlock(); b.setType(Material.CHEST); Chest c=(Chest)b.getState(); c.getInventory().clear(); if(!eventOpen)c.getInventory().setItem(13,createCrown()); c.update(); }
    private ItemStack createCrown() { ItemStack i=new ItemStack(Material.GOLDEN_HELMET); ItemMeta m=i.getItemMeta(); m.displayName(Component.text("Imperial Crown",NamedTextColor.GOLD)); m.lore(List.of(Component.text("The crown of the realm",NamedTextColor.YELLOW),Component.text("Unbreakable",NamedTextColor.GRAY))); m.setUnbreakable(true); m.setCustomModelData(12001); m.getPersistentDataContainer().set(crownKey,PersistentDataType.BYTE,(byte)1); m.addAttributeModifier(Attribute.ARMOR,new AttributeModifier(UUID.nameUUIDFromBytes("imperial-crown-armor".getBytes()),"imperial_crown_armor",22,AttributeModifier.Operation.ADD_NUMBER)); i.setItemMeta(m); return i; }
    private boolean isCrown(ItemStack i) { return i!=null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(crownKey,PersistentDataType.BYTE); }
    private void refreshHolder() { for(Player p:Bukkit.getOnlinePlayers()){ boolean has=false; for(ItemStack i:p.getInventory().getContents()) if(isCrown(i)){has=true;break;} if(has) applyEffects(p); else clearEffects(p); } }
    private void applyEffects(Player p) { p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,50,1,false,false,true)); p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,50,0,false,false,true)); }
    private void clearEffects(Player p) { p.removePotionEffect(PotionEffectType.STRENGTH); p.removePotionEffect(PotionEffectType.SPEED); }

    private int command(CommandSender s, Command c, String l, String[] a) { if(!s.hasPermission("imperial.admin")){s.sendMessage(PREFIX+"You do not have permission.");return 1;} if(a.length==0||a[0].equalsIgnoreCase("status")){s.sendMessage(PREFIX+"Chest: "+(eventOpen?"opened":"locked")+", remaining seconds: "+Math.max(0,(unlockAt-System.currentTimeMillis())/1000));return 1;} if(a[0].equalsIgnoreCase("open")){unlockAt=0;eventOpen=false;placeChest();saveState();s.sendMessage(PREFIX+"The chest is now openable.");return 1;} if(a[0].equalsIgnoreCase("reset")){eventOpen=false;unlockAt=System.currentTimeMillis()+TWO_DAYS;placeChest();saveState();s.sendMessage(PREFIX+"The two-day countdown restarted.");return 1;} s.sendMessage(PREFIX+"Usage: /imperial <status|open|reset>");return 1; }
    @EventHandler public void onOpen(InventoryOpenEvent e){ if(!(e.getPlayer() instanceof Player p)||!(e.getInventory().getHolder() instanceof Chest c)||!c.getLocation().getBlock().equals(chestLocation.getBlock()))return; if(!eventOpen&&System.currentTimeMillis()<unlockAt){e.setCancelled(true);p.sendMessage(PREFIX+"The crown is still locked.");return;} if(!eventOpen){eventOpen=true;placeChest();p.teleport(parkourStart.clone().add(.5,1,.5));p.sendMessage(PREFIX+"Finish the parkour to claim the crown!");saveState();} }
    @EventHandler public void onMove(PlayerMoveEvent e){if(parkourFinish!=null&&e.getTo()!=null&&e.getTo().getWorld().equals(parkourFinish.getWorld())&&e.getTo().distanceSquared(parkourFinish)<=2.5&&!hasCrown()){e.getPlayer().getInventory().addItem(createCrown());e.getPlayer().sendMessage(PREFIX+"You claimed the Imperial Crown!");}}
    private boolean hasCrown(){for(Player p:Bukkit.getOnlinePlayers())for(ItemStack i:p.getInventory().getContents())if(isCrown(i))return true;return false;}
    private boolean protectedBlock(Location l){return chestLocation!=null&&l.getWorld().equals(chestLocation.getWorld())&&l.distanceSquared(chestLocation)<400;}
    @EventHandler public void onBreak(BlockBreakEvent e){if(protectedBlock(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler public void onEntityExplode(EntityExplodeEvent e){e.blockList().removeIf(b->protectedBlock(b.getLocation()));}
    @EventHandler public void onBlockExplode(BlockExplodeEvent e){e.blockList().removeIf(b->protectedBlock(b.getLocation()));}
    @EventHandler public void onClick(InventoryClickEvent e){if(isCrown(e.getCurrentItem()))Bukkit.getScheduler().runTask(this,this::refreshHolder);}
    @EventHandler public void onDrop(PlayerDropItemEvent e){if(isCrown(e.getItemDrop().getItemStack()))Bukkit.getScheduler().runTask(this,this::refreshHolder);}
    @EventHandler public void onQuit(PlayerQuitEvent e){clearEffects(e.getPlayer());}
    @EventHandler public void onRespawn(PlayerRespawnEvent e){Bukkit.getScheduler().runTaskLater(this,this::refreshHolder,1L);}
    @EventHandler public void onDeath(PlayerDeathEvent e){clearEffects(e.getEntity());}
}
