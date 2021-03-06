package com.smartaleq.bukkit.dwarfcraft;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRightClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This watches for broken blocks and reacts
 * 
 */
class DCBlockListener extends BlockListener {
	private final DwarfCraft plugin;

	protected DCBlockListener(final DwarfCraft plugin) {
		this.plugin = plugin;
	}

	/**
	 * Called when a block is destroyed by a player.
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled())
			return;
		Player player = event.getPlayer();
		Dwarf dwarf = plugin.getDataManager().find(player);
		List<Skill> skills = dwarf.getSkills();
		ItemStack tool = player.getItemInHand();
		int toolId = -1;
		short durability = 0;
		if (tool != null) {
			toolId = tool.getTypeId();
			durability = tool.getDurability();
		}
		boolean correctTool = false;
		Location loc = event.getBlock().getLocation();
		int materialId = event.getBlock().getTypeId();
		byte meta = event.getBlock().getData();

		// Logic var
		boolean blockDropChange = false;

		for (Skill s : skills) {
			if (s == null)
				continue;
			for (Effect e : s.getEffects()) {
				if (e == null)
					continue;

				// Check if blockdrop change happens
				if (e.getEffectType() == EffectType.BLOCKDROP
						&& e.getInitiatorId() == materialId) {
					correctTool = false;
					for (int id : e.getTools())
						if (id == toolId)
							correctTool = true;
					// Crops special line:
					if (e.getInitiatorId() == 59)
						if (meta != 7)
							continue;
					if (DwarfCraft.debugMessagesThreshold < 4)
						System.out.println("DC4: Effect:" + e.getId()
								+ " tool: " + toolId + " and toolRequired:"
								+ e.getToolRequired());
					if (correctTool || !e.getToolRequired()) {
						Util.dropBlockEffect(loc, e, e.getEffectAmount(dwarf),
								true, meta);
						blockDropChange = true;
					}
				}
			}
			// Check if durability change happens
			for (Effect e : s.getEffects()) {
				if (e.getEffectType() == EffectType.TOOLDURABILITY
						&& durability != -1) {
					for (int id : e.getTools()) {
						if (id == toolId) {
							double effectAmount = e.getEffectAmount(dwarf);
							if (DwarfCraft.debugMessagesThreshold < 3)
								System.out
										.println("DC2: affected durability of a tool - old:"
												+ durability);
							tool.setDurability((short) (durability + Util
									.randomAmount(effectAmount)));
							// if you use the tool on a non-dropping block it
							// doesn't take special durability damage
							if (DwarfCraft.debugMessagesThreshold < 3)
								System.out
										.println("DC3: affected durability of a tool - new:"
												+ tool.getDurability());
							Util.toolChecker(player);
						}
					}
				}
			}
		}

		if (blockDropChange) {
			event.getBlock().setTypeId(0);
			event.setCancelled(true);
		}
	}

	/**
	 * onBlockDamage used to accelerate how quickly blocks are destroyed.
	 * setDamage() not implemented yet
	 */
	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		if (event.isCancelled())
			return;
		// General information
		Player player = event.getPlayer();
		Dwarf dwarf = plugin.getDataManager().find(player);
		List<Skill> skills = dwarf.getSkills();

		// Effect Specific information
		ItemStack tool = player.getItemInHand();
		int toolId = -1;
		if (tool != null) {
			toolId = tool.getTypeId();
		}
		boolean correctTool = false;
		int materialId = event.getBlock().getTypeId();
		if (DwarfCraft.debugMessagesThreshold < -1)
			System.out
					.println("DC-1: damage level = " + event.getDamageLevel());
		if (event.getDamageLevel() != BlockDamageLevel.STARTED)
			return;
		for (Skill s : skills) {
			for (Effect e : s.getEffects()) {
				if (e.getEffectType() == EffectType.DIGTIME
						&& e.getInitiatorId() == materialId) {
					if (DwarfCraft.debugMessagesThreshold < 2)
						System.out.println("DC2: started instamine check");
					correctTool = false;
					for (int id : e.getTools())
						if (id == toolId)
							correctTool = true;
					if (correctTool || !e.getToolRequired()) {
						if (Util.randomAmount(e.getEffectAmount(dwarf)) == 0)
							return;
						if (DwarfCraft.debugMessagesThreshold < 3)
							System.out
									.println("DC3: Insta-mine occured. Block:"
											+ materialId);
						BlockBreakEvent bbe = new BlockBreakEvent(event.getBlock(), player);
						onBlockBreak(bbe);
					}
				}
			}
		}
		// event.setDamageLevel(event.getDamageLevel() + effectAmount);
		// event.setCancelled(true);
	}

	/**
	 * Called when a player right clicks a block, used for hoe-ing grass.
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onBlockRightClick(BlockRightClickEvent event) {
		Player player = event.getPlayer();
		Dwarf dwarf = plugin.getDataManager().find(player);
		List<Skill> skills = dwarf.getSkills();
		ItemStack tool = player.getItemInHand();
		int toolId = -1;
		short durability = 0;
		if (tool != null) {
			toolId = tool.getTypeId();
			durability = tool.getDurability();
		}
		Block block = event.getBlock();
		Location loc = block.getLocation();
		Material material = event.getBlock().getType();
		for (Skill s : skills) {
			if (s == null)
				continue;
			for (Effect e : s.getEffects()) {
				if (e == null)
					continue;
				if (e.getEffectType() == EffectType.PLOWDURABILITY) {
					for (int id : e.getTools()) {
						if (id == toolId
								&& (material == Material.DIRT || material == Material.GRASS)) {
							double effectAmount = e.getEffectAmount(dwarf);
							if (DwarfCraft.debugMessagesThreshold < 3)
								System.out
										.println("DC2: affected durability of a hoe - old:"
												+ durability);
							tool.setDurability((short) (durability + Util
									.randomAmount(effectAmount)));
							if (DwarfCraft.debugMessagesThreshold < 3)
								System.out
										.println("DC3: affected durability of a hoe - new:"
												+ tool.getDurability());
							Util.toolChecker(player);
							block.setTypeId(60);
						}
					}
				}
				if (e.getEffectType() == EffectType.PLOW) {
					for (int id : e.getTools()) {
						if (id == toolId && material == Material.GRASS) {
							Util.dropBlockEffect(loc, e,
									e.getEffectAmount(dwarf), true, (byte) 0);
							if (DwarfCraft.debugMessagesThreshold < 3)
								System.out.println("DC3: hoed some ground:"
										+ e.getEffectAmount(dwarf));
						}
					}
				}
			}
		}
	}

}
