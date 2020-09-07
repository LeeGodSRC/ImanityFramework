package org.imanity.framework.bukkit.visual;

import lombok.Getter;
import org.imanity.framework.bukkit.util.BlockPosition;
import org.imanity.framework.bukkit.visual.type.VisualType;

@Getter
public class VisualPosition extends BlockPosition {

    private VisualType type;

    public VisualPosition(int x, int y, int z, String world, VisualType type) {
        super(x, y ,z, world);
        this.type = type;
    }
}