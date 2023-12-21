package group.zfadah.converter.common.block

import buildcraft.api.tools.IToolWrench
import cpw.mods.fml.relauncher.ReflectionHelper
import group.zfadah.converter.common.tile.TileEnergyConverter
import group.zfadah.converter.proxy.ClientProxy
import net.minecraft.block.Block
import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class BlockEnergyConverter : Block(Material.redstoneLight), ITileEntityProvider {

    lateinit var iconBlocked: IIcon
    lateinit var iconInput: IIcon
    lateinit var iconOutput: IIcon

    init {
        this.setHardness(10F)
        ReflectionHelper.setPrivateValue(Block::class.java, this, true, "isTileProvider")
    }

    override fun createNewTileEntity(p_149915_1_: World?, p_149915_2_: Int): TileEntity {
        return TileEnergyConverter::class.java.newInstance()
    }

    override fun registerBlockIcons(p_149651_1_: IIconRegister?) {
        if (p_149651_1_ == null) {
            return
        }
        blockIcon = p_149651_1_.registerIcon("converter:energyconverter/EnergyConverter")
        iconBlocked = p_149651_1_.registerIcon("converter:energyconverter/EnergyConverter")
        iconInput = p_149651_1_.registerIcon("converter:energyconverter/EC_IN")
        iconOutput = p_149651_1_.registerIcon("converter:energyconverter/EC_OUT")

    }

    override fun renderAsNormalBlock(): Boolean {
        return false
    }

    override fun getRenderType(): Int {
        return ClientProxy.renderBlockEnergyConverter.renderId
    }

    override fun isOpaqueCube(): Boolean {
        return true
    }

    override fun hasTileEntity(): Boolean {
        return true
    }

    override fun getIcon(
        worldObj: IBlockAccess?,
        x: Int,
        y: Int,
        z: Int,
        metadata: Int
    ): IIcon {
        if (worldObj == null) {
            return blockIcon
        }
        val tileEntity = worldObj.getTileEntity(x, y, z)
        if (tileEntity !is TileEnergyConverter) {
            return blockIcon
        }
        val sideConfig = tileEntity.getSideConfig(ForgeDirection.getOrientation(metadata))
        return when (sideConfig.toInt()) {
            0 -> blockIcon
            1 -> iconInput
            2 -> iconOutput
            else -> blockIcon
        }
    }

    override fun onBlockActivated(
        world: World?,
        x: Int,
        y: Int,
        z: Int,
        entityPlayer: EntityPlayer?,
        side: Int,
        clickPosX: Float,
        clickPosY: Float,
        clickPosZ: Float
    ): Boolean {
        if (world == null || entityPlayer == null) {
            return true
        }
        if (world.isRemote) {
            return true
        }

        val itemStack = entityPlayer.inventory.getCurrentItem()
        if (itemStack == null){
            return true
        }
        val itemInUse = itemStack.item
        val tileEntity = world.getTileEntity(x, y, z)
        if (itemInUse is IToolWrench) {
            if (itemStack.unlocalizedName.contains("metatool")) {
                when (itemInUse.getDamage(itemStack)) {
                    15 -> {}
                    16 -> {}
                    else -> return true
                }
            }
            val nbtTagCompound = NBTTagCompound()

            tileEntity.writeToNBT(nbtTagCompound)

            if (nbtTagCompound.hasKey("Sides")){
                var sidesArray = nbtTagCompound.getByteArray("Sides")
                if (!entityPlayer.isSneaking) {
                    if (sidesArray[side] < 2) {
                        sidesArray[side]++
                    } else {
                        sidesArray[side] = 0
                    }
                } else {
                    if (sidesArray[side] > 0) {
                        sidesArray[side]--
                    } else {
                        sidesArray[side] = 2
                    }
                }
                tileEntity.readFromNBT(nbtTagCompound)
                world.markBlockForUpdate(x,y,z)
            }

        }
        return true
    }
}
