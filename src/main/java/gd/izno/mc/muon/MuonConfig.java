package gd.izno.mc.muon;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.FMLLog;

import java.io.File;
import java.util.HashMap;

/**
 * Created by TrinaryLogic on 2016-11-18.
 */
public class MuonConfig {
    public static final String CATEGORY_GENERATION = "terrain_generation";
    public volatile static Configuration config = null;
    public static boolean enabled = true;
    private volatile static HashMap<String, Property> properties = new HashMap<String, Property>();
    private volatile static HashMap<String, Boolean> cached = new HashMap<String, Boolean>();

    public static void loadFile(File file) {
        FMLLog.info("[Muon] %s", "Loading config from "+file.getName());
        // Sets up a new config
        config = new Configuration(file);
        config.load();

        config.setCategoryComment(CATEGORY_GENERATION, "Options controlling generation of new villages.\nThese only take effect as new chunks are generated.");
        properties.put("enable_new_by_default", config.get(CATEGORY_GENERATION, "enable_new_by_default", false, "Enable features of new versions by default"));
        properties.put("fix_buried_doors", config.get(CATEGORY_GENERATION, "fix_buried_doors", true, "Align village buildings with the path only"));
        properties.put("better_paths", config.get(CATEGORY_GENERATION, "better_paths", true, "Use alternate function for creating village paths"));
        properties.put("smooth_village_terrain", config.get(CATEGORY_GENERATION, "smooth_village_terrain", true, "Smooth terrain within village boundaries"));
        properties.put("fix_scattered_features", config.get(CATEGORY_GENERATION, "fix_scattered_features", true, "Ensure scattered features (e.g. Desert temples and Igloos) are accessible."));
        int village_grove_frequency_default = 0;
        if (getBoolean("enable_new_by_default")) {
            if (!config.hasKey(CATEGORY_GENERATION, "village_grove_frequency")) {
                village_grove_frequency_default = 100;
            }
        }
        Property village_grove_frequency = config.get(CATEGORY_GENERATION, "village_grove_frequency", village_grove_frequency_default, "Add stands of trees to villages.");
        village_grove_frequency.setMinValue(0);
        village_grove_frequency.setMaxValue(100);
        properties.put("village_grove_frequency", village_grove_frequency);

        // write out default config if necessary
        config.save();
    }

    public static void save() {
        if (config.hasChanged()) {
            FMLLog.info("[Muon] %s", "Saving settings. ");
            config.save();
            cached = new HashMap<String, Boolean>(); // clear cache
        }
    }

    public static Configuration getConfig() {
        return config;
    }

    public static int getInt(String propertyName) {
        return properties.get(propertyName).getInt();
    }

    public static boolean getBoolean(String propertyName) {
        if (!enabled) {
            return false;
        }
        if (cached.containsKey(propertyName)) {
            return cached.get(propertyName).booleanValue();
        }
        boolean value = false;
        if (propertyName == "using_heightmaps") {
            value = getBoolean("smooth_village_terrain") || getBoolean("fix_scattered_features");
        } else if (propertyName == "terrain_dependent_structures") {
            value = getInt("village_grove_frequency") > 0;// || getBoolean("village_mines");
        } else {
            value = properties.get(propertyName).getBoolean();
        }
        cached.put(propertyName,new Boolean(value));
        return value;
    }

    public static void disableAll(boolean disabled) {
        enabled = !disabled;
    }

//    public static void setBoolean(String propertyName, Boolean value) {
//        properties.get(propertyName).set(value);
//    }
}
