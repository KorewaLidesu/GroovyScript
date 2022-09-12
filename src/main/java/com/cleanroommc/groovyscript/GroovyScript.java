package com.cleanroommc.groovyscript;

import com.cleanroommc.groovyscript.api.IGroovyEnvironmentRegister;
import com.cleanroommc.groovyscript.brackets.BracketHandlerManager;
import com.cleanroommc.groovyscript.command.GSCommand;
import com.cleanroommc.groovyscript.compat.vanilla.VanillaModule;
import com.cleanroommc.groovyscript.event.Events;
import com.cleanroommc.groovyscript.network.NetworkHandler;
import com.cleanroommc.groovyscript.sandbox.GroovyDeobfuscationMapper;
import com.cleanroommc.groovyscript.sandbox.SandboxRunner;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

@Mod(modid = GroovyScript.ID, name = GroovyScript.NAME, version = GroovyScript.VERSION)
@Mod.EventBusSubscriber(modid = GroovyScript.ID)
public class GroovyScript implements IGroovyEnvironmentRegister {

    public static final String ID = "groovyscript";
    public static final String NAME = "GroovyScript";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(ID);

    private static String scriptPath;
    private static File startupPath;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        NetworkHandler.init();
        GroovyDeobfuscationMapper.init();
        scriptPath = Loader.instance().getConfigDir().toPath().getParent().toString() + "/scripts";
        startupPath = new File(getScriptPath() + "/startup");
        SandboxRunner.init();
        Events.init();
        BracketHandlerManager.init();
        VanillaModule.initializeBinding();
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        SandboxRunner.run();
    }

    @Mod.EventHandler
    public void onServerLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new GSCommand());
    }

    @Override
    public Collection<String> getBannedPackages() {
        return Arrays.asList(
                "com.cleanroommc.groovyscript.api",
                "com.cleanroommc.groovyscript.command",
                "com.cleanroommc.groovyscript.core.mixin",
                "com.cleanroommc.groovyscript.registry",
                "com.cleanroommc.groovyscript.sandbox"
        );
    }

    @NotNull
    public static String getScriptPath() {
        if (scriptPath == null) {
            throw new IllegalStateException("GroovyScript is not yet loaded!");
        }
        return scriptPath;
    }

    @NotNull
    public static File getStartupPath() {
        if (startupPath == null) {
            throw new IllegalStateException("GroovyScript is not yet loaded!");
        }
        return startupPath;
    }
}
