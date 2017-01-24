package gd.izno.mc.muon;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;

/**
 * Created by TrinaryLogic on 2016-11-17.
 */
public class MuonGuiConfig extends GuiConfig {
    public MuonGuiConfig(GuiScreen parent)
    {
        super(parent,
                new ConfigElement(MuonConfig.getConfig().getCategory(MuonConfig.CATEGORY_GENERATION)).getChildElements(),
                "muon",
                false,
                false,
                "Muon "+MuonConfig.config.getCategory(MuonConfig.CATEGORY_GENERATION).getName(),
                MuonConfig.config.getConfigFile().getName());
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    public void initGui() {
        super.initGui();
    }

    // Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);
    }

    // Draws the screen and all the components in it.
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
