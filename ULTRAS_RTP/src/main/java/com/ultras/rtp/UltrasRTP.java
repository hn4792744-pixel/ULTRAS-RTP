package com.ultras.rtp;

import com.ultras.rtp.commands.RtpCommand;
import com.ultras.rtp.configuration.ConfigManager;
import com.ultras.rtp.gui.GuiManager;
import com.ultras.rtp.language.LanguageManager;
import com.ultras.rtp.listeners.CommandConflictListener;
import com.ultras.rtp.listeners.GuiListener;
import com.ultras.rtp.listeners.PlayerListener;
import com.ultras.rtp.sounds.SoundManager;
import com.ultras.rtp.storage.PlayerDataStorage;
import com.ultras.rtp.teleport.RtpManager;
import com.ultras.rtp.world.WorldManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** ULTRAS_RTP • Developer: UC_Hussein */
public final class UltrasRTP extends JavaPlugin {
    private ConfigManager configManager;
    private PlayerDataStorage storage;
    private LanguageManager languageManager;
    private SoundManager soundManager;
    private WorldManager worldManager;
    private GuiManager guiManager;
    private RtpManager rtpManager;
    private RtpCommand rtpCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        storage = new PlayerDataStorage(this);
        languageManager = new LanguageManager(this);
        soundManager = new SoundManager(this);
        worldManager = new WorldManager(this);
        guiManager = new GuiManager(this);
        rtpManager = new RtpManager(this);
        rtpCommand = new RtpCommand(this);

        reloadEverything();

        PluginCommand command = getCommand("rtp");
        if (command != null) {
            command.setExecutor(rtpCommand);
            command.setTabCompleter(rtpCommand);
        }
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandConflictListener(this), this);
        getLogger().info("ULTRAS_RTP enabled • Developer: UC_Hussein");
    }

    @Override
    public void onDisable() {
        if (rtpManager != null) {
            rtpManager.shutdown();
        }
        if (guiManager != null) {
            guiManager.closeAll();
        }
        if (storage != null) {
            storage.close();
        }
    }

    /** config.yml, languages, sounds, worlds and GUIs. No restart needed; running countdowns keep working. */
    public void reloadEverything() {
        guiManager.closeAll();
        configManager.reload();
        languageManager.reload();
        soundManager.reload();
        worldManager.reload();
        guiManager.reload();
    }

    public ConfigManager config() {
        return configManager;
    }

    public PlayerDataStorage storage() {
        return storage;
    }

    public LanguageManager lang() {
        return languageManager;
    }

    public SoundManager sounds() {
        return soundManager;
    }

    public WorldManager worlds() {
        return worldManager;
    }

    public GuiManager gui() {
        return guiManager;
    }

    public RtpManager rtp() {
        return rtpManager;
    }

    public RtpCommand command() {
        return rtpCommand;
    }
}
