package gd.izno.mc.muon;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * Created by TrinaryLogic on 2016-11-11.
 */
//@IFMLLoadingPlugin.MCVersion("1.10.2")
@IFMLLoadingPlugin.TransformerExclusions({"gd.izno.mc.muon"})
public class MuonPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"gd.izno.mc.muon.MuonClassTransformer"};
    }

    @Override
    public String getModContainerClass() {
        return "gd.izno.mc.muon.MuonModContainer";
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
