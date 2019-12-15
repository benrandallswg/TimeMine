package net.indicacorp.timemine.models;

import org.bukkit.Material;

import java.util.ArrayList;

public class Tool {

    private Material tool;
    private ArrayList<Material> mineables;

    public Tool(Material tool, ArrayList<Material> mineables) {
        this.tool = tool;
        this.mineables = mineables;
    }

    public boolean canMine(Material material) {
        return mineables.contains(material);
    }
}
