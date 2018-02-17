package gd.izno.mc.muon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

/**
 * Created by TrinaryLogic on 2016-11-17.
 */
public class MuonGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {

    }

    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return MuonGuiConfig.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }


	@Override
	public boolean hasConfigGui() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen) {
		// TODO Auto-generated method stub
		return null;
	}
}
