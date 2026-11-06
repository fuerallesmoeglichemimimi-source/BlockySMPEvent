package de.citypvpworld.lobbyevent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbyEventPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private static final String PREFIX = ChatColor.DARK_PURPLE + "[LobbyEvent] " + ChatColor.GRAY;

    /** False = protected lobby; true = event is running and normal gameplay is enabled. */
    private boolean eventStarted;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        eventStarted = getConfig().getBoolean("event.started", false);

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("event") != null) {
            getCommand("event").setExecutor(this);
        } else {
            getLogger().severe("Der Befehl /event fehlt in der plugin.yml!");
        }

        // Den PvP-Zustand passend zum gespeicherten Event-Status setzen.
        applyWorldPvpState();

        // Bei einem Neustart während der Lobby-Phase stehen alle Online-Spieler
        // wieder am gespeicherten Lobby-Punkt.
        if (!eventStarted && getConfig().getBoolean("teleport-on-join", true)) {
            Location lobby = getLobbyLocation();
            if (lobby != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.teleport(lobby.clone());
                }
            }
        }

        getLogger().info("LobbyEvent aktiviert. Event gestartet: " + eventStarted);
    }

    /** Lobby-Schutz: Abbau ist vor dem Eventstart fuer alle gesperrt, auch fuer OPs. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventStarted) {
            event.setCancelled(true);
        }
    }

    /** Lobby-Schutz: Platzieren ist vor dem Eventstart fuer alle gesperrt, auch fuer OPs. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!eventStarted) {
            event.setCancelled(true);
        }
    }

    /** PvP wird nur in der Lobby-Phase blockiert; waehrend des Events greift normales PvP. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (eventStarted || !(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getDamager() instanceof Player) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player) {
            event.setCancelled(true);
        }
    }

    /** Neu eingeloggte Spieler kommen nur waehrend der Lobby-Phase in die Lobby. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (eventStarted || !getConfig().getBoolean("teleport-on-join", true)) {
            return;
        }

        Location lobby = getLobbyLocation();
        if (lobby == null) {
            return;
        }

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> {
            // Status erneut pruefen, falls das Event im selben Tick gestartet wurde.
            if (player.isOnline() && !eventStarted) {
                player.teleport(lobby.clone());
            }
        });
    }

    /** Auch Welten, die spaeter geladen werden, erhalten den aktuellen PvP-Status. */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        event.getWorld().setPVP(eventStarted);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Diesen Befehl kann nur ein Spieler ausfuehren.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(PREFIX + ChatColor.RED + "Dafuer musst du OP sein.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("setlobby")) {
            saveLobby(player.getLocation());
            player.sendMessage(PREFIX + ChatColor.GREEN + "Lobby-Punkt wurde an deiner Position gespeichert.");
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (args.length < 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Benutzung: /event start <text>");
                return true;
            }

            String message = ChatColor.translateAlternateColorCodes('&', String.join(" ",
                    java.util.Arrays.copyOfRange(args, 1, args.length)).trim());
            if (message.isBlank()) {
                player.sendMessage(PREFIX + ChatColor.RED + "Bitte gib einen Text an.");
                return true;
            }

            startEvent(player, message);
            return true;
        }

        if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("stop")) {
            resetEvent();
            Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW
                    + "Das Event wurde beendet. Die geschuetzte Lobby ist wieder aktiv.");
            return true;
        }

        sendHelp(player);
        return true;
    }

    private void startEvent(Player starter, String message) {
        // Erst den Status umschalten: ab jetzt sind PvP, Abbauen und Platzieren frei.
        eventStarted = true;
        getConfig().set("event.started", true);
        saveConfig();
        applyWorldPvpState();

        // Alle aktuell Online-Spieler zum ausfuehrenden OP teleportieren und Titel anzeigen.
        Location target = starter.getLocation().clone();
        int teleported = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.teleport(target.clone());
            online.sendTitle(message, ChatColor.GRAY + "Das Event beginnt!", 10, 80, 20);
            teleported++;
        }

        Bukkit.broadcastMessage(PREFIX + ChatColor.GREEN + "Event gestartet von "
                + ChatColor.WHITE + starter.getName() + ChatColor.GRAY
                + ". PvP und Bauen sind jetzt aktiviert. (" + teleported + " Spieler teleportiert)");
    }

    /** Setzt den Server wieder in die geschuetzte Lobby-Phase zurueck. */
    private void resetEvent() {
        eventStarted = false;
        getConfig().set("event.started", false);
        saveConfig();
        applyWorldPvpState();

        Location lobby = getLobbyLocation();
        if (lobby != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.teleport(lobby.clone());
            }
        }
    }

    private void applyWorldPvpState() {
        for (World world : Bukkit.getWorlds()) {
            world.setPVP(eventStarted);
        }
    }

    private void saveLobby(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        getConfig().set("lobby.world", world.getName());
        getConfig().set("lobby.x", location.getX());
        getConfig().set("lobby.y", location.getY());
        getConfig().set("lobby.z", location.getZ());
        getConfig().set("lobby.yaw", location.getYaw());
        getConfig().set("lobby.pitch", location.getPitch());
        saveConfig();
    }

    private Location getLobbyLocation() {
        String worldName = getConfig().getString("lobby.world", "");
        if (worldName == null || worldName.isBlank()) {
            if (Bukkit.getWorlds().isEmpty()) {
                return null;
            }
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().warning("Die gespeicherte Lobby-Welt '" + worldName + "' wurde nicht gefunden.");
            return null;
        }

        return new Location(
                world,
                getConfig().getDouble("lobby.x"),
                getConfig().getDouble("lobby.y"),
                getConfig().getDouble("lobby.z"),
                (float) getConfig().getDouble("lobby.yaw"),
                (float) getConfig().getDouble("lobby.pitch")
        );
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.DARK_PURPLE + "----- LobbyEvent -----");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/event setlobby "
                + ChatColor.GRAY + "- setzt den Lobby-Punkt");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/event start <text> "
                + ChatColor.GRAY + "- startet das Event, zeigt den Titel und aktiviert PvP/Bauen");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/event reset "
                + ChatColor.GRAY + "- aktiviert den Lobby-Schutz erneut");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/event help "
                + ChatColor.GRAY + "- zeigt diese Hilfe");
    }
}
