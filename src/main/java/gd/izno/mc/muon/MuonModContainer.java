package gd.izno.mc.muon;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Level;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.Map;

/**
 * Created by TrinaryLogic on 2016-11-12.
 */
public class MuonModContainer extends DummyModContainer {
    static ModMetadata md = null;
    boolean updateMsgDone = false;

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
            // Windows ends up with a leading slash on drive letters, for some reason.
            if (classLocation.matches("^/[A-Z]:/.*")) {
                classLocation = classLocation.substring(1); // remove leading slash
            }
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

    public URL getUpdateUrl() {
        String jsonURL = md.updateJSON;
        FMLLog.info("[Muon] %s", "Returning JSON url "+jsonURL);
        if (jsonURL != null && jsonURL != "")
        {
            jsonURL += "?version="+md.version+"&mcversion="+ForgeVersion.mcVersion+"&launcher="+Minecraft.getMinecraft().getVersion();
            try
            {
                return new URL(jsonURL);
            }
            catch (MalformedURLException e)
            {
                FMLLog.log(getModId(), Level.DEBUG, "Specified json URL invalid: %s", jsonURL);
            }
        }
        return null;
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

    @Subscribe
    public void postInit(FMLPostInitializationEvent event) {
        if (Loader.isModLoaded("movillages")) {
            // ... disable hooks that conflict with movillages
        }
    }

    @SubscribeEvent
    public void playerJoin(final EntityJoinWorldEvent event) {
        if (!updateMsgDone && !event.isCanceled() && event.getWorld().isRemote && (event.getEntity() instanceof EntityPlayer)) {
            final EntityPlayer player = (EntityPlayer) event.getEntity();
            ForgeVersion.CheckResult versioncheck = ForgeVersion.getResult(this);
            if (player != null && !player.isDead && versioncheck != null && (versioncheck.status == ForgeVersion.Status.OUTDATED || versioncheck.status == ForgeVersion.Status.BETA_OUTDATED)) {
                String msg = "Version "+versioncheck.target+" of "+getMCModInfo().name+" is available";
                player.sendMessage(new TextComponentString(msg));
            }
            updateMsgDone = true;
        }
    }

//    // I really want to implement this but it doesn't do anythign yet...
//    // could we monkey-patch something to make it work?
//    public Disableable canBeDisabled() {
//        return Disableable.YES;
//    }
//
//    public void setEnabledState(boolean enabled) {
//        FMLLog.info("[Muon] %s", (enabled?"enabling":"disabling")+md.name+" "+md.version);
//        MuonConfig.disableAll(!enabled);
//    }

    @SubscribeEvent
    public void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if(eventArgs.getModID().equals(md.modId)) {
            MuonConfig.save();
        }
    }
}
