package mcheli.aircraft;

import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
public class MCH_HardpointGui extends GuiContainerBase
{
	final MCH_ContainerHardpoint container;
	public MCH_HardpointGui(MCH_ContainerHardpoint container)
	{
		super(container, 178, 192, defaultBackground);
		this.container = container;
		this.ySize = this.container.guiHeight;
	}

	@Override
	public void initElements()
	{
	}

	@Override
	public void setupElements()
	{
	}

	@Override
	public void handleKeyboardInput()
	{
		super.handleKeyboardInput();
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float xSize, int ySize, int backgroundTexture) {

	}

}

