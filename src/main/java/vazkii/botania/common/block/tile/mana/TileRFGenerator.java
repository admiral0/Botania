/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 *
 * File Created @ [Aug 29, 2014, 10:01:32 PM (GMT)]
 */
package vazkii.botania.common.block.tile.mana;

import java.util.EnumMap;
import java.util.Map;

import cofh.api.energy.IEnergyConnection;
import cofh.api.energy.IEnergyReceiver;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Optional;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.tile.TileMod;

@Optional.InterfaceList({
		@Optional.Interface(iface = "cofh.api.energy.IEnergyConnection", modid = "CoFHAPI|energy"),
		@Optional.Interface(iface = "ic2.api.energy.IEnergySource", modid = "IC2")
})
public class TileRFGenerator extends TileMod implements IManaReceiver, IEnergyConnection, IEnergySource {

	private static final int CONVERSION_RATE = 10;
	private static final int IC2_CONVERSION_RATE = 5;
	private static final int MAX_MANA = 1280 * CONVERSION_RATE;

	private static final String TAG_MANA = "mana";
	int mana = 0;

	// Thanks to skyboy for help with this cuz I'm a noob with RF
	private final EnumMap<EnumFacing, IEnergyReceiver> receiverCache = new EnumMap<>(EnumFacing.class);
	private boolean deadCache;

	@Override
	@Optional.Method(modid = "CoFHAPI|energy")
	public void validate() {
		super.validate();
		deadCache = true;
	}

	@Override
	public void update() {
		if(!worldObj.isRemote) {
			if(deadCache)
				if(Botania.rfApiLoaded)
					reCache();
				if(Botania.ic2ApiLoaded)
					ic2TileLoad();
				deadCache = false;

			int transfer = Math.min(mana, 160 * CONVERSION_RATE);
			mana -= transfer;
			mana += transmitEnergy(transfer);
		}
	}

	@Override
	public void invalidate() {
		ic2TileUnload();
		super.invalidate();
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	private final int transmitEnergy(int energy) {
		for(Map.Entry<EnumFacing, IEnergyReceiver> e : receiverCache.entrySet()) {
			IEnergyReceiver tile = e.getValue();
			if (tile == null)
				continue;

			energy -= tile.receiveEnergy(e.getKey().getOpposite(), energy, false);

			if (energy <= 0)
				return 0;
		}

		return energy;
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	private void reCache() {
		if(deadCache) {
			for(EnumFacing dir : EnumFacing.VALUES)
				onNeighborTileChange(pos.offset(dir));
		}
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	public void onNeighborTileChange(BlockPos pos) {
		TileEntity tile = worldObj.getTileEntity(pos);

		BlockPos q = getPos();
		EnumFacing side = EnumFacing.getFacingFromVector(pos.getX() - q.getX(), pos.getY() - q.getY(), pos.getZ() - q.getZ());

		addCache(tile, side);
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	private void addCache(TileEntity tile, EnumFacing side) {
		receiverCache.remove(side);

		if(tile instanceof IEnergyReceiver) {
			receiverCache.put(side, (IEnergyReceiver)tile);
		}
	}



	@Override
	public int getCurrentMana() {
		return mana / CONVERSION_RATE;
	}

	@Override
	public boolean isFull() {
		return mana >= MAX_MANA;
	}

	@Override
	public void recieveMana(int mana) {
		this.mana = Math.min(MAX_MANA, this.mana + mana * CONVERSION_RATE);
	}

	@Override
	public boolean canRecieveManaFromBursts() {
		return true;
	}

	@Override
	public void writePacketNBT(NBTTagCompound cmp) {
		cmp.setInteger(TAG_MANA, mana);
	}

	@Override
	public void readPacketNBT(NBTTagCompound cmp) {
		mana = cmp.getInteger(TAG_MANA);
	}

	@Override
	public boolean canConnectEnergy(EnumFacing from) {
		return true;
	}


	@Optional.Method(modid = "IC2")
	private void ic2TileLoad(){
		MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
	}

	@Optional.Method(modid = "IC2")
	private void ic2TileUnload(){
		MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
	}

	@Optional.Method(modid = "IC2")
	public double getOfferedEnergy() {
		return mana * IC2_CONVERSION_RATE;
	}

	@Optional.Method(modid = "IC2")
	public void drawEnergy(double v) {
		mana -= v * IC2_CONVERSION_RATE;
	}

	@Optional.Method(modid = "IC2")
	public int getSourceTier() {
		return 2;
	}

	@Optional.Method(modid = "IC2")
	public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing enumFacing) {
		return true;
	}
}
