package ua.lokha.autorecipecrushfix;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends JavaPlugin implements Listener {
    private Map<String, Limit> lastAutoRecipe = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                this, PacketType.Play.Client.AUTO_RECIPE
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String playerName = event.getPlayer().getName();
                Limit limit = lastAutoRecipe.computeIfAbsent(playerName, s -> new Limit());

                if (limit.checkLimit()) {
                    Main.this.getLogger().warning("Игрок " + playerName + " " +
                            "слишком часто отправляет AutoRecipe пакеты, отменяем пакет." );
                    event.setCancelled(true);
                }
            }
        });
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        lastAutoRecipe.remove(event.getPlayer().getName());
    }

    /**
     * Token Bucket Algorithm
     */
    public static class Limit {
        private int limit = 16;
        private long lastCheck = System.currentTimeMillis();

        public boolean checkLimit() {
            long current = System.currentTimeMillis();

            long left = current - lastCheck;
            int addLimit = (int) (left / 500);
            if (addLimit > 0) {
                this.limit = Math.min(16, this.limit + addLimit);
                this.lastCheck = current;
            }

            if (this.limit <= 0) {
                return true;
            } else {
                this.limit--;
                return false;
            }
        }
    }
}
