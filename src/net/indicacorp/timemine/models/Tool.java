package net.indicacorp.timemine.models;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;

public class Tool {
    private Material tool;
    private HashMap<Material, ArrayList<Material>> mineables;

    public Tool(Material material) {
        tool = material;
    }

    public boolean canMine(Material material) {
        HashMap<String, Material> validTools = new HashMap<String, Material>();
        return true;
    }
}

