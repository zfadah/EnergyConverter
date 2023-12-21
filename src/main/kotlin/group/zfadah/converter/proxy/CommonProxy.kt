package group.zfadah.converter.proxy

import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.event.FMLServerStartingEvent
import cpw.mods.fml.common.registry.GameRegistry
import group.zfadah.converter.Config
import group.zfadah.converter.common.block.BlockEnergyConverter
import group.zfadah.converter.common.tile.TileEnergyConverter
import mcp.mobius.waila.api.IWailaRegistrar
import mcp.mobius.waila.api.impl.ModuleRegistrar
import net.minecraft.block.Block

open class CommonProxy {


    companion object{
        val blockEnergyConverter : Block = BlockEnergyConverter().setBlockName("energy_converter")
    }

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    open fun preInit(event: FMLPreInitializationEvent) {
        Config.synchronizeConfiguration(event.suggestedConfigurationFile)
        GameRegistry.registerBlock(blockEnergyConverter,"energy_converter");
        GameRegistry.registerTileEntity(TileEnergyConverter::class.java,"energy_converter")
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    open fun init(event: FMLInitializationEvent?) {}

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    open fun postInit(event: FMLPostInitializationEvent?) {
    }

    // register server commands in this event handler (Remove if not needed)
    open fun serverStarting(event: FMLServerStartingEvent?) {}
}

