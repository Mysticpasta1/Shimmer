package com.lowdragmc.shimmer.client.light;

import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;
import java.nio.FloatBuffer;

/**
 * @author KilaBash
 * @date 2022/05/04
 * @implNote ColorPointLight
 */
@OnlyIn(Dist.CLIENT)
public class ColorPointLight {
    public static final int STRUCT_SIZE = (4 + 3 + 1);
    public float r, g, b, a;
    public float x, y, z;
    public float radius;
    LightManager lightManager;
    int offset;

    protected ColorPointLight(BlockPos pos , Template template) {
        a = template.a;
        r = template.r;
        g = template.g;
        b = template.b;
        radius = template.radius;
        x = pos.getX() + 0.5f;
        y = pos.getY() + 0.5f;
        z = pos.getZ() + 0.5f;
    }

    protected ColorPointLight(LightManager lightManager, Vector3f pos, int color, float radius, int offset) {
        x = pos.x();
        y = pos.y();
        z = pos.z();
        setColor(color);
        this.lightManager = lightManager;
        this.radius = radius;
        this.offset = offset;
    }

    public void setColor(int color) {
        a = (((color >> 24) & 0xff) / 255f);
        r = (((color >> 16) & 0xff) / 255f);
        g = (((color >> 8) & 0xff) / 255f);
        b = (((color) & 0xff) / 255f);
    }

    public int getColor(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF));
    }

    public void setColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public void setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean isRemoved() {
        return lightManager == null;
    }

    public void remove() {
        if (lightManager != null) {
            lightManager.removeLight(this);
            lightManager = null;
        }
    }

    public void update() {
        if (lightManager != null) {
            Minecraft.getInstance().execute(() -> lightManager.lightUBO.bufferSubData(offset, getData()));
        }
    }

    protected float[] getData() {
        return new float[]{r,g,b,a,x,y,z,radius};
    }

    public void uploadBuffer(FloatBuffer buffer) {
        buffer.put(getData());
    }

    public static class Template {
        public float r, g, b, a;
        public float radius;

        public Template(float radius, int color) {
            setColor(color);
            this.radius = radius;
        }

        public Template(float radius, float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.radius = radius;
        }

        public void setColor(int color) {
            a = (((color >> 24) & 0xff) / 255f);
            r = (((color >> 16) & 0xff) / 255f);
            g = (((color >> 8) & 0xff) / 255f);
            b = (((color) & 0xff) / 255f);
        }
    }

}
