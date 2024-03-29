package group.zfadah.converter.util;

public class StaticValues {

    public static final double[][][] vertexs = {
        {
            {0.0, 0.0, 0.0}, {1.0, 0.0, 0.0}, {1.0, 0.0, 1.0}, {0.0, 0.0, 1.0}
        }, {
            {0.0, 1.0, 0.0}, {0.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 0.0}
        }, {
            {1.0, 1.0, 0.0}, {1.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 1.0, 0.0}
        }, {
            {0.0, 1.0, 1.0}, {0.0, 0.0, 1.0}, {1.0, 0.0, 1.0}, {1.0, 1.0, 1.0}
        }, {
            {0.0, 1.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 1.0}, {0.0, 1.0, 1.0}
        }, {
            {1.0, 1.0, 1.0}, {1.0, 0.0, 1.0}, {1.0, 0.0, 0.0}, {1.0, 1.0, 0.0}
        }
    };

    public static final float[][] sideNormal = {
        {0.0F, -1.0F, 0.0F},
        {0.0F, 1.0F, 0.0F},
        {0.0F, 0.0F, -1.0F},
        {0.0F, 0.0F, 1.0F},
        {-1.0F, 0.0F, 0.0F},
        {1.0F, 0.0F, 0.0F}
    };
}
