package gd.izno.mc.muon;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Map;

/**
 * Created by TrinaryLogic on 2016-11-12.
 */
public class MuonModContainer extends DummyModContainer {
    static ModMetadata md = null;

    public MuonModContainer() {
        super(getMCModInfo());
    } //Initialiser always uses mcmod.info for version information

    static ModMetadata getMCModInfo() {
        if (md != null) {
            return md;
        }
        // always get version information etc from mcinfo.
        InputStream infostream = new ByteArrayInputStream("[{\"modid\":\"broken\"}]".getBytes());
        try {
            // First we need to do some magic to get the mcmod.info from the right jar file...
            String classLocation = MuonModContainer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            FMLLog.info("[Muon] %s", "Looking for mcmod.info in "+classLocation);
            Path jarPath = FileSystems.getDefault().getPath(classLocation);
            Path modInfoPath = FileSystems.newFileSystem(jarPath, (ClassLoader) null).getPath("/mcmod.info");
            infostream = Files.newInputStream(modInfoPath, StandardOpenOption.READ);
        }
        catch (Exception e) {
            // Umm... something is wrong. very wrong. fail gracefully?
            e.printStackTrace();
        }
        MetadataCollection mc = MetadataCollection.from(infostream, "gd.izno.mc.muon");
        Map<String, Object> dummyinfo = ImmutableMap.<String, Object>builder().put("name", "Muon").put("version", "Broken").build();
        md = mc.getMetadataForId("muon", dummyinfo);
        FMLLog.info("[Muon] %s", ""+md.name+" is version "+md.version);
        return md;
    }

    @Override
    public String getGuiClassName()
    {
        return "gd.izno.mc.muon.MuonGuiFactory";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this); // mod initialisation events
        return true;
    }

    @Subscribe
    public void preInit(FMLPreInitializationEvent event) {
        MuonConfig.loadFile(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(this); // grab configchanged events
    }

    @SubscribeEvent
    public void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if(eventArgs.getModID().equals(md.modId)) {
            MuonConfig.save();
        }
    }
}
