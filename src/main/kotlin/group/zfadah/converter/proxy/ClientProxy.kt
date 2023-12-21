package group.zfadah.converter.proxy

import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import group.zfadah.converter.common.render.RenderBlockEnergyConverter

class ClientProxy : CommonProxy() {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods aswell.

    companion object{
        val renderBlockEnergyConverter = RenderBlockEnergyConverter(RenderingRegistry.getNextAvailableRenderId())
//        val renderTileEnergyConverter = RenderTileEnergyConverter()
    }
    override fun preInit(event: FMLPreInitializationEvent) {

        super.preInit(event)


    }

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods aswell.
    override fun init(event: FMLInitializationEvent?) {
//        ClientRegistry.bindTileEntitySpecialRenderer(TileEnergyConverter::class.java, renderTileEnergyConverter)
        RenderingRegistry.registerBlockHandler(renderBlockEnergyConverter.renderId, renderBlockEnergyConverter)
        super.init(event)
    }


}
