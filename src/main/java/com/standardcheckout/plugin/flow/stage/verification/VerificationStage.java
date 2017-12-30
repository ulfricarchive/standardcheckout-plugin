package com.standardcheckout.plugin.flow.stage.verification;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.standardcheckout.plugin.flow.FlowContext;
import com.standardcheckout.plugin.flow.MutableFlowContext;
import com.standardcheckout.plugin.flow.stage.InventoryStage;
import com.standardcheckout.plugin.flow.stage.Stage;
import com.standardcheckout.plugin.flow.stage.purchase.PurchaseStage;
import com.standardcheckout.plugin.language.Tell;

public class VerificationStage extends InventoryStage {

	private static final String VERIFY_PURCHASE_NAME = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Verify Purchase";

	private Inventory inventory;

	public VerificationStage(FlowContext context) {
		super(context);
	}

	@Override
	public void play() {
		Player player = context.getRequiredPlayer();
		inventory = Bukkit.createInventory(player, InventoryType.DISPENSER, ChatColor.BOLD + "Standard Checkout");
		ItemStack verificationItem = new ItemStack(Material.EMERALD);
		ItemMeta meta = verificationItem.getItemMeta();
		meta.setDisplayName(VERIFY_PURCHASE_NAME);
		meta.setLore(Arrays.asList(
				"",
				ChatColor.YELLOW + "To confirm your purchase",
				ChatColor.YELLOW + "left-click here. To cancel",
				ChatColor.YELLOW + "press the ESC key."));
		verificationItem.setItemMeta(meta);
		inventory.setItem(4, verificationItem);
	}

	@Override
	public Stage next() {
		VerificationContext verification = context.getBean(VerificationContext.class);
		if (verification == null || !verification.getVerified()) {
			return null;
		}

		return new PurchaseStage(context);
	}

	@Override
	public void close() {
		context.getPlayer().ifPresent(player -> Tell.sendMessages(player, ChatColor.YELLOW + "Your purchase was cancelled."));
	}

	@Override
	public void clickInventory(InventoryClickEvent event) {
		if (!isInventoryOpen(event.getWhoClicked())) {
			return;
		}

		event.setCancelled(true);
		event.setResult(Result.DENY);

		ItemStack item = event.getCurrentItem();
		if (item == null) {
			return;
		}

		if (event.getInventory() == null) {
			return;
		}

		if (item.getType() != Material.EMERALD) {
			return;
		}

		if (!item.hasItemMeta()) {
			return;
		}

		ItemMeta meta = item.getItemMeta();
		if (!meta.hasDisplayName() || !meta.getDisplayName().equals(VERIFY_PURCHASE_NAME)) {
			return;
		}

		VerificationContext verification = context.getBean(VerificationContext.class);
		if (verification == null) {
			if (context instanceof MutableFlowContext) {
				MutableFlowContext mutable = (MutableFlowContext) context;
				verification = new VerificationContext();
				mutable.storeBean(verification);
			}
		}
		if (verification != null) {
			verification.setVerified(true);
		}

		context.flow().next();
	}

	@Override
	protected Inventory getInventory() {
		return inventory;
	}

}