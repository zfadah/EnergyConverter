package group.zfadah.converter.common.tile

import cofh.api.energy.EnergyStorage
import cofh.api.energy.IEnergyHandler
import gregtech.api.enums.GT_Values
import gregtech.api.interfaces.tileentity.IBasicEnergyContainer
import gregtech.api.interfaces.tileentity.IEnergyConnected.Util
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.BaseMetaPipeEntity
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_Cable
import gregtech.api.net.GT_Packet_Block_Event
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.inventory.IInventory
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.EnumSkyBlock
import net.minecraft.world.World
import net.minecraft.world.biome.BiomeGenBase
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.IFluidHandler
import java.util.concurrent.ThreadLocalRandom
import kotlin.experimental.and

open class TileEnergyConverter : TileEntity(), IEnergyHandler, IBasicEnergyContainer {

    private var sidesArray: ByteArray
    private var energyStorage: EnergyStorage = EnergyStorage(2097152 * 4 * 200)
    private var currentInputAmperage: LongArray = longArrayOf(0, 0, 0, 0, 0, 0)
    private var currentInputVoltage: LongArray = longArrayOf(0, 0, 0, 0, 0, 0)

    private var currentOutputAmperage: LongArray = longArrayOf(0, 0, 0, 0, 0, 0)
    private var currentOutputVoltage: LongArray = longArrayOf(0, 0, 0, 0, 0, 0)

    private var currentReceiving: Int = 0

    init {
        sidesArray = byteArrayOf(0,0,0,0,0,0)
        MinecraftForge.EVENT_BUS.register(this)
    }

    //RF Part
    override fun canConnectEnergy(p0: ForgeDirection?): Boolean {
        return if (p0 == null) {
            false
        } else {
            sidesArray[p0.ordinal].toInt() != 0
        }
    }

    override fun extractEnergy(p0: ForgeDirection?, p1: Int, p2: Boolean): Int {
        return if (p0 == null) {
            0
        } else {
            if (sidesArray[p0.ordinal].toInt() == 2) {
                energyStorage.extractEnergy(p1, p2)
            } else {
                0
            }
        }
    }

    override fun receiveEnergy(p0: ForgeDirection?, p1: Int, p2: Boolean): Int {
        return if (p0 == null) {
            0
        } else {
            if (sidesArray[p0.ordinal].toInt() == 1) {
                energyStorage.receiveEnergy(p1, p2)
            } else {
                0
            }
        }
    }

    override fun getEnergyStored(p0: ForgeDirection?): Int {
        return energyStorage.energyStored
    }

    override fun getMaxEnergyStored(p0: ForgeDirection?): Int {
        return energyStorage.maxEnergyStored
    }

    //GT Part
    override fun injectEnergyUnits(aSide: ForgeDirection, aVoltage: Long, aAmperage: Long): Long {
        if (aVoltage <= 0 || aAmperage <= 0 || aSide.ordinal == 6) { //Refuse to the side is not spec
            return 0
        }
        if (sidesArray[aSide.ordinal].toInt() != 1) { //Not Input Side
            return 0
        }

        val acceptableAmperage = (energyStorage.maxReceive - currentReceiving) / 4 / aVoltage

        val requireAmperage = if (acceptableAmperage < aAmperage) {
            acceptableAmperage
        } else {
            aAmperage
        }

        currentReceiving = (requireAmperage * aVoltage).toInt()

        currentInputVoltage[aSide.ordinal] = aVoltage
        currentInputAmperage[aSide.ordinal] = requireAmperage

        increaseStoredEnergyUnits(aVoltage*requireAmperage,true)

        return requireAmperage
    }

    override fun drainEnergyUnits(aSide: ForgeDirection, aVoltage: Long, aAmperage: Long): Boolean {
        if (!this.outputsEnergyTo(aSide) || aVoltage <= 0 || aAmperage <= 0L || aSide.ordinal == 6) {
            return false
        }
        val needs = aVoltage * aAmperage
        return if (needs * 4 > energyStorage.maxExtract) {
            false
        } else {
            currentOutputVoltage[aSide.ordinal] = aVoltage
            currentOutputAmperage[aSide.ordinal] = aAmperage
            return decreaseStoredEnergyUnits(aVoltage*aAmperage,false)
        }

    }

    override fun decreaseStoredEnergyUnits(aEnergy: Long, aIgnoreTooLessEnergy: Boolean): Boolean {
        return if (aEnergy * 4 < Int.MAX_VALUE) {
            val aEnergyInt = aEnergy.toInt() * 4
            if (energyStorage.energyStored < aEnergyInt) {
                false
            } else {
                energyStorage.extractEnergy(aEnergyInt, false) >= aEnergyInt
            }
        } else {
            false
        }
    }

    override fun increaseStoredEnergyUnits(aEnergy: Long, aIgnoreTooMuchEnergy: Boolean): Boolean {
        if (energyStorage.energyStored == energyStorage.maxEnergyStored) {
            return false
        }
        energyStorage.receiveEnergy(aEnergy.toInt() * 4, false)
        return true
    }

    private fun outputEnergys() {
        for ((index, byte) in sidesArray.withIndex()) {
            if (byte.toInt() == 2) {
                val tileEntityAtSide = getTileEntityAtSide(ForgeDirection.getOrientation(index)) ?: continue
                if (tileEntityAtSide is BaseMetaPipeEntity) {
                    val metaTileEntity = tileEntityAtSide.metaTileEntity
                    if (metaTileEntity is GT_MetaPipeEntity_Cable) {
                        val mAmperage = metaTileEntity.mAmperage
                        val mVoltage = metaTileEntity.mVoltage
                        val mUsedAmperage = Util.emitEnergyToNetwork(mVoltage,mAmperage,this)
                        drainEnergyUnits(ForgeDirection.getOrientation(index),mVoltage,mUsedAmperage)
                    }

                }
            }
        }
    }

    override fun updateEntity() {
        super.updateEntity()
        if (!worldObj.isRemote){
            outputEnergys()
        }
    }


    override fun inputEnergyFrom(side: ForgeDirection): Boolean {
        return sidesArray[side.ordinal].toInt() == 1
    }

    override fun outputsEnergyTo(aSide: ForgeDirection): Boolean {
        return sidesArray[aSide.ordinal].toInt() == 2
    }

    override fun isUniversalEnergyStored(aEnergyAmount: Long): Boolean {
        return if (aEnergyAmount > Int.MAX_VALUE) {
            false
        } else {
            aEnergyAmount >= universalEnergyStored
        }
    }

    override fun getUniversalEnergyStored(): Long {
        return (energyStorage.energyStored / 4).toLong()
    }

    override fun getUniversalEnergyCapacity(): Long {
        return (energyStorage.maxEnergyStored / 4).toLong()
    }

    override fun getOutputAmperage(): Long {
        return 0L
    }

    override fun getOutputVoltage(): Long {
        return 0L
    }

    override fun getInputAmperage(): Long {
        return 0L
    }

    override fun getInputVoltage(): Long {
        return 0L
    }

    override fun getAverageElectricInput(): Long {
        return 0L
    }

    override fun getAverageElectricOutput(): Long {
        return 0L
    }

    override fun getStoredEU(): Long {
        return (energyStorage.energyStored / 4).toLong()
    }

    override fun getEUCapacity(): Long {
        return (energyStorage.maxEnergyStored / 4).toLong()
    }


    override fun readFromNBT(p_145839_1_: NBTTagCompound?) {
        super.readFromNBT(p_145839_1_)
        if (p_145839_1_ != null) {
            sidesArray = p_145839_1_.getByteArray("Sides")
            energyStorage.energyStored = p_145839_1_.getInteger("StoredEnergy")
        }
    }

    override fun writeToNBT(p_145841_1_: NBTTagCompound?) {
        super.writeToNBT(p_145841_1_)
        p_145841_1_?.setByteArray("Sides", sidesArray)
        p_145841_1_?.setInteger("StoredEnergy",energyStorage.energyStored)
    }

    open fun getSideConfig(side: ForgeDirection): Byte {
        return sidesArray[side.ordinal]
    }

//    @SubscribeEvent
//    fun onInteract(e: PlayerInteractEvent) {
//        if (e.entityPlayer == null || e.entityPlayer.worldObj == null || e.action == null || e.world.provider == null) {
//            return
//        }
//        val entityPlayer = e.entityPlayer
//        if (e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
//            if (e.world.getTileEntity(e.x,e.y,e.z) != this){
//                return
//            }
//            val item = entityPlayer.inventory.getCurrentItem()?.item
//            if (item is IToolWrench) {
//                when (item.getDamage(entityPlayer.inventory.getCurrentItem())) {
//                    15 -> {}
//                    16 -> {}
//                    else -> { return }
//                }
//                if (!entityPlayer.worldObj.isRemote) {
//                    if ((e.face >= 0) and (e.face <= 5)) {
//                        val i = e.face
//                        if (!entityPlayer.isSneaking) {
//                            if (sidesArray[i] < 2) {
//                                sidesArray[i]++
//                            } else {
//                                sidesArray[i] = 0
//                            }
//                        } else {
//                            if (sidesArray[i] > 0) {
//                                sidesArray[i]--
//                            } else {
//                                sidesArray[i] = 2
//                            }
//                        }
//                        this.markDirty()
//                        e.world.markBlockForUpdate(e.x,e.y,e.z)
//                    }
//                }
//            }
//        }
//    }

    override fun getDescriptionPacket(): Packet {
        val nbtTagCompound = NBTTagCompound()
        writeToNBT(nbtTagCompound)
        return S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbtTagCompound)
    }

    override fun onDataPacket(net: NetworkManager?, pkt: S35PacketUpdateTileEntity?) {
        if (pkt != null) {
            readFromNBT(pkt.func_148857_g())
            this.worldObj.markBlockForUpdate(xCoord,yCoord,zCoord)
        }
    }


    override fun getColorization(): Byte {
        return -1
    }

    override fun setColorization(aColor: Byte): Byte {
        return -1
    }

    override fun getWorld(): World {
        return worldObj
    }

    override fun getXCoord(): Int {
        return xCoord
    }

    override fun getYCoord(): Short {
        return yCoord.toShort()
    }

    override fun getZCoord(): Int {
        return zCoord
    }

    override fun isServerSide(): Boolean {
        return !worldObj.isRemote
    }

    override fun isClientSide(): Boolean {
        return worldObj.isRemote
    }

    override fun getRandomNumber(aRange: Int): Int {
        return ThreadLocalRandom.current().nextInt(aRange)
    }

    override fun getTileEntity(aX: Int, aY: Int, aZ: Int): TileEntity? {
        return worldObj.getTileEntity(aX, aY, aZ)
    }

    override fun getTileEntityOffset(aX: Int, aY: Int, aZ: Int): TileEntity? {
        return getTileEntity(xCoord + aX, yCoord + aY, zCoord + aZ)
    }

    private fun getXYZ(aSide: Byte, aDistance: Int): Triple<Int, Int, Int> {
        var tempX = xCoord
        var tempY = yCoord
        var tempZ = zCoord

        when (aSide.toInt()) {
            0 -> tempY -= aDistance
            1 -> tempY += aDistance
            2 -> tempZ -= aDistance
            3 -> tempZ += aDistance
            4 -> tempX -= aDistance
            5 -> tempX += aDistance
        }
        return Triple(tempX, tempY, tempZ)
    }

    override fun getTileEntityAtSide(aSide: ForgeDirection): TileEntity? {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), 1)
        return worldObj.getTileEntity(tempX, tempY, tempZ)
    }

    override fun getTileEntityAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): TileEntity? {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), aDistance)
        return worldObj.getTileEntity(tempX, tempY, tempZ)
    }


    override fun getIInventory(aX: Int, aY: Int, aZ: Int): IInventory? {
        val tileEntity = getTileEntity(aX, aY, aZ)
        if (tileEntity is IInventory) {
            return tileEntity
        }
        return null
    }

    override fun getIInventoryOffset(aX: Int, aY: Int, aZ: Int): IInventory? {
        val tileEntityOffset = getTileEntityOffset(aX, aY, aZ)
        if (tileEntityOffset is IInventory) {
            return tileEntityOffset
        }
        return null
    }

    override fun getIInventoryAtSide(aSide: ForgeDirection): IInventory? {
        val tileEntityAtSide = getTileEntityAtSide(aSide)
        if (tileEntityAtSide is IInventory) {
            return tileEntityAtSide
        }
        return null
    }

    override fun getIInventoryAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): IInventory? {
        val tileEntityAtSideAndDistance = getTileEntityAtSideAndDistance(aSide, aDistance)
        if (tileEntityAtSideAndDistance is IInventory) {
            return tileEntityAtSideAndDistance
        }
        return null
    }

    override fun getITankContainer(aX: Int, aY: Int, aZ: Int): IFluidHandler? {
        val tileEntity = getTileEntity(aX, aY, aZ)
        if (tileEntity is IFluidHandler) {
            return tileEntity
        }
        return null
    }

    override fun getITankContainerOffset(aX: Int, aY: Int, aZ: Int): IFluidHandler? {
        val tileEntityOffset = getTileEntityOffset(aX, aY, aZ)
        if (tileEntityOffset is IFluidHandler) {
            return tileEntityOffset
        }
        return null
    }

    override fun getITankContainerAtSide(aSide: ForgeDirection): IFluidHandler? {
        val tileEntityAtSide = getTileEntityAtSide(aSide)
        if (tileEntityAtSide is IFluidHandler) {
            return tileEntityAtSide
        }
        return null
    }

    override fun getITankContainerAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): IFluidHandler? {
        val tileEntityAtSideAndDistance = getTileEntityAtSideAndDistance(aSide, aDistance)
        if (tileEntityAtSideAndDistance is IFluidHandler) {
            return tileEntityAtSideAndDistance
        }
        return null
    }

    override fun getIGregTechTileEntity(aX: Int, aY: Int, aZ: Int): IGregTechTileEntity? {
        val tileEntity = getTileEntity(aX, aY, aZ)
        if (tileEntity is IGregTechTileEntity) {
            return tileEntity
        }
        return null
    }

    override fun getIGregTechTileEntityOffset(aX: Int, aY: Int, aZ: Int): IGregTechTileEntity? {
        val tileEntityOffset = getTileEntityOffset(aX, aY, aZ)
        if (tileEntityOffset is IGregTechTileEntity) {
            return tileEntityOffset
        }
        return null
    }

    override fun getIGregTechTileEntityAtSide(aSide: ForgeDirection): IGregTechTileEntity? {
        val tileEntityAtSide = getTileEntityAtSide(aSide)
        if (tileEntityAtSide is IGregTechTileEntity) {
            return tileEntityAtSide
        }
        return null
    }

    override fun getIGregTechTileEntityAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): IGregTechTileEntity? {
        val tileEntityAtSideAndDistance = getTileEntityAtSideAndDistance(aSide, aDistance)
        if (tileEntityAtSideAndDistance is IGregTechTileEntity) {
            return tileEntityAtSideAndDistance
        }
        return null
    }

    override fun getBlock(aX: Int, aY: Int, aZ: Int): Block {
        return if (worldObj.blockExists(aX, aY, aZ)) {
            worldObj.getBlock(aX, aY, aZ)
        } else {
            Blocks.air
        }

    }

    override fun getBlockOffset(aX: Int, aY: Int, aZ: Int): Block {
        return getBlock(xCoord + aX, yCoord + aY, zCoord + aZ)
    }

    override fun getBlockAtSide(aSide: ForgeDirection): Block {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), 1)
        return getBlock(tempX, tempY, tempZ)
    }

    override fun getBlockAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): Block {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), aDistance)
        return getBlock(tempX, tempY, tempZ)
    }

    override fun getMetaID(aX: Int, aY: Int, aZ: Int): Byte {
        return if (worldObj.blockExists(aX, aY, aZ)) {
            worldObj.getBlockMetadata(aX, aY, aZ).toByte()
        } else {
            0
        }
    }

    override fun getMetaIDOffset(aX: Int, aY: Int, aZ: Int): Byte {
        return getMetaID(xCoord + aX, yCoord + aY, zCoord + aZ)
    }

    override fun getMetaIDAtSide(aSide: ForgeDirection): Byte {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), 1)
        return getMetaID(tempX, tempY, tempZ)
    }

    override fun getMetaIDAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): Byte {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), aDistance)
        return getMetaID(tempX, tempY, tempZ)
    }

    override fun getLightLevel(aX: Int, aY: Int, aZ: Int): Byte {
        return if (worldObj.blockExists(aX, aY, aZ)) {
            return (worldObj.getLightBrightness(aX, aY, aZ) * 15).toInt().toByte()
        } else {
            0
        }
    }

    override fun getLightLevelOffset(aX: Int, aY: Int, aZ: Int): Byte {
        return getLightLevel(xCoord + aX, yCoord + aY, zCoord + aZ)
    }

    override fun getLightLevelAtSide(aSide: ForgeDirection): Byte {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), 1)
        return getLightLevel(tempX, tempY, tempZ)
    }

    override fun getLightLevelAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): Byte {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), aDistance)
        return getLightLevel(tempX, tempY, tempZ)
    }

    override fun getOpacity(aX: Int, aY: Int, aZ: Int): Boolean {
        return if (worldObj.blockExists(aX, aY, aZ)) {
            getBlock(aX, aY, aZ).isOpaqueCube
        } else {
            false
        }
    }

    override fun getOpacityOffset(aX: Int, aY: Int, aZ: Int): Boolean {
        return getOpacity(xCoord + aX, yCoord + aY, zCoord + aZ)
    }

    override fun getOpacityAtSide(aSide: ForgeDirection): Boolean {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), 1)
        return getOpacity(tempX, tempY, tempZ)
    }

    override fun getOpacityAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): Boolean {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), aDistance)
        return getOpacity(tempX, tempY, tempZ)
    }

    override fun getSky(aX: Int, aY: Int, aZ: Int): Boolean {
        return if (worldObj.blockExists(aX, aY, aZ)) {
            worldObj.canBlockSeeTheSky(aX, aY, aZ)
        } else {
            false
        }
    }

    override fun getSkyOffset(aX: Int, aY: Int, aZ: Int): Boolean {
        return getSky(xCoord + aX, yCoord + aY, zCoord + aZ)
    }

    override fun getSkyAtSide(aSide: ForgeDirection): Boolean {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), 1)
        return getSky(tempX, tempY, tempZ)
    }

    override fun getSkyAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): Boolean {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), aDistance)
        return getSky(tempX, tempY, tempZ)
    }

    override fun getAir(aX: Int, aY: Int, aZ: Int): Boolean {
        return worldObj.isAirBlock(aX, aY, aZ)
    }

    override fun getAirOffset(aX: Int, aY: Int, aZ: Int): Boolean {
        return getAir(xCoord + aX, yCoord + aY, zCoord + aZ)
    }

    override fun getAirAtSide(aSide: ForgeDirection): Boolean {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), 1)
        return getAir(tempX, tempY, tempZ)
    }

    override fun getAirAtSideAndDistance(aSide: ForgeDirection, aDistance: Int): Boolean {
        val (tempX, tempY, tempZ) = getXYZ(aSide.ordinal.toByte(), aDistance)
        return getAir(tempX, tempY, tempZ)
    }

    override fun getBiome(): BiomeGenBase {
        return getBiome(xCoord, zCoord)
    }

    override fun getBiome(aX: Int, aZ: Int): BiomeGenBase {
        return worldObj.getBiomeGenForCoordsBody(aX, aZ)
    }

    override fun getOffsetX(aSide: ForgeDirection, aMultiplier: Int): Int {
        return xCoord + aSide.offsetX * aMultiplier
    }

    override fun getOffsetY(aSide: ForgeDirection, aMultiplier: Int): Short {
        return (yCoord + aSide.offsetY * aMultiplier).toShort()
    }

    override fun getOffsetZ(aSide: ForgeDirection, aMultiplier: Int): Int {
        return zCoord + aSide.offsetZ * aMultiplier
    }

    override fun isDead(): Boolean {
        return isInvalid
    }

    override fun sendBlockEvent(aID: Byte, aValue: Byte) {
        GT_Values.NW.sendPacketToAllPlayersInRange(
            worldObj,
            GT_Packet_Block_Event(xCoord, yCoord.toShort(), zCoord, aID, aValue),
            xCoord,
            zCoord
        )
    }

    override fun getTimer(): Long {
        return 0L
    }

    override fun setLightValue(aLightValue: Byte) {
        worldObj.setLightValue(EnumSkyBlock.Block, xCoord, yCoord, zCoord, (aLightValue and 15).toInt())
    }


    override fun isInvalidTileEntity(): Boolean {
        return isInvalid
    }

}
