package group.zfadah.converter.common.render

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import group.zfadah.converter.common.block.BlockEnergyConverter
import group.zfadah.converter.common.tile.TileEnergyConverter
import group.zfadah.converter.util.StaticValues
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.Tessellator
import net.minecraft.util.ResourceLocation
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.util.ForgeDirection

class RenderBlockEnergyConverter(val renderID:Int) : ISimpleBlockRenderingHandler {
    companion object {
        val vertexs: Array<Array<DoubleArray>> = StaticValues.vertexs
        val sideNormal: Array<FloatArray> = StaticValues.sideNormal
        val faceNoneArray: Array<ResourceLocation> = arrayOf(ResourceLocation("converter:textures/blocks/energyconverter/EnergyConverter.png"))

    }

    override fun renderInventoryBlock(block: Block?, metadata: Int, modelId: Int, renderer: RenderBlocks?) {
        this.renderBlock()
    }

    override fun renderWorldBlock(
        world: IBlockAccess?,
        x: Int,
        y: Int,
        z: Int,
        block: Block?,
        modelId: Int,
        renderer: RenderBlocks?
    ): Boolean {

        val tileEntity = world?.getTileEntity(x, y, z)
        if (tileEntity !is TileEnergyConverter || renderer == null){
            return false
        }
        if (block !is BlockEnergyConverter){
            return false
        }
        renderer.renderStandardBlock(block,x,y,z)

        return true
    }

    override fun shouldRender3DInInventory(modelId: Int): Boolean {
        return true
    }

    override fun getRenderId(): Int {
        return renderID
    }

    private fun renderBlock(){
        val tessellator = Tessellator.instance
        renderSideWithNormal(ForgeDirection.DOWN, faceNoneArray,tessellator, sideNormal[0])
        renderSideWithNormal(ForgeDirection.UP, faceNoneArray,tessellator, sideNormal[1])
        renderSideWithNormal(ForgeDirection.NORTH, faceNoneArray,tessellator,sideNormal[2])
        renderSideWithNormal(ForgeDirection.SOUTH, faceNoneArray,tessellator,sideNormal[3])
        renderSideWithNormal(ForgeDirection.WEST, faceNoneArray,tessellator,sideNormal[4])
        renderSideWithNormal(ForgeDirection.EAST, faceNoneArray,tessellator,sideNormal[5])
    }

    private fun renderSideWithNormal(sides: ForgeDirection, resource: Array<ResourceLocation>, tessellator: Tessellator, normalArray:FloatArray){
        val vertex = vertexs[sides.ordinal]
        for (resourceLocation in resource) {
            tessellator.startDrawingQuads()
            Minecraft.getMinecraft().renderEngine.bindTexture(resourceLocation)
            tessellator.setNormal(normalArray[0], normalArray[1], normalArray[2])
            tessellator.addVertexWithUV(vertex[0][0], vertex[0][1], vertex[0][2], 0.0, 0.0)
            tessellator.addVertexWithUV(vertex[1][0], vertex[1][1], vertex[1][2], 0.0, 1.0)
            tessellator.addVertexWithUV(vertex[2][0], vertex[2][1], vertex[2][2], 1.0, 1.0)
            tessellator.addVertexWithUV(vertex[3][0], vertex[3][1], vertex[3][2], 1.0, 0.0)
            tessellator.draw()
        }
    }
}
