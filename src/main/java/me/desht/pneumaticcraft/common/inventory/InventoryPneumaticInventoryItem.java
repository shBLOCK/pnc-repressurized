// Code used from the Backpack Mod,
// https://github.com/Eydamos/Minecraft-Backpack-Mod

package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.tileentity.TileEntityChargingStation;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class InventoryPneumaticInventoryItem extends InventoryBasic {
    // the title of the backpack
    protected String inventoryTitle;
    // the original ItemStack to compare with the player inventory
    private ItemStack armorStack;
    private final TileEntityChargingStation te;

    // if class is reading from NBT tag
    private boolean reading = false;

    public InventoryPneumaticInventoryItem(TileEntityChargingStation te) {
        super("", false, getInventorySize(te.getPrimaryInventory().getStackInSlot(TileEntityChargingStation.CHARGE_INVENTORY_INDEX)));
        this.te = te;
        armorStack = te.getPrimaryInventory().getStackInSlot(TileEntityChargingStation.CHARGE_INVENTORY_INDEX);

        // check if inventory exists if not create one
        if (!hasInventory()) {
            createInventory();
        }
        loadInventory();
    }

    /**
     * Is called whenever something is changed in the inventory.
     */
    @Override
    public void markDirty() {
        super.markDirty();
        // if reading from NBT don't write
        if (!reading) {
            saveInventory();
        }
    }

    /**
     * This method is called when the chest opens the inventory. It loads the
     * content of the inventory and its title.
     */
    @Override
    public void openInventory(EntityPlayer player) {
        loadInventory();
    }

    /**
     * This method is called when the chest closes the inventory. It then throws
     * out every backpack which is inside the backpack and saves the inventory.
     */
    @Override
    public void closeInventory(EntityPlayer player) {
        saveInventory();
    }

    /**
     * Returns the name of the inventory.
     */
    @Override
    public String getName() {
        return "Pneumatic Helmet";
    }

    protected static int getInventorySize(ItemStack is) {
        return 9;
    }

    /**
     * Returns if an Inventory is saved in the NBT.
     *
     * @return True when the NBT is not null and the NBT has key "UpgradeInventory"
     * otherwise false.
     */
    protected boolean hasInventory() {
        return NBTUtil.hasTag(armorStack, "UpgradeInventory");
    }

    /**
     * Creates the Inventory Tag in the NBT with an empty inventory.
     */
    protected void createInventory() {
        writeToNBT();
    }

    /**
     * If there is no inventory create one. Then load the content and title of
     * the inventory from the NBT
     */
    public void loadInventory() {
        readFromNBT();
    }

    /**
     * Saves the actual content of the inventory to the NBT.
     */
    public void saveInventory() {
        writeToNBT();
        //   updateToChargingStation();
    }

    private void updateToChargingStation() {
        te.getPrimaryInventory().setStackInSlot(TileEntityChargingStation.CHARGE_INVENTORY_INDEX, armorStack);
    }

    /**
     * Writes a NBT Node with inventory.
     */
    public void writeToNBT() {
        NBTTagList itemList = new NBTTagList();
        for (int i = 0; i < getSizeInventory(); i++) {
            if (!getStackInSlot(i).isEmpty()) {
                NBTTagCompound slotEntry = new NBTTagCompound();
                slotEntry.setByte("Slot", (byte) i);
                getStackInSlot(i).writeToNBT(slotEntry);
                itemList.appendTag(slotEntry);
            }
        }
        // save content in Inventory->Items
        NBTTagCompound inventory = new NBTTagCompound();
        inventory.setTag("Items", itemList);
        NBTUtil.setCompoundTag(armorStack, "UpgradeInventory", inventory);
        NBTUtil.removeTag(armorStack, "Inventory");
    }

    /**
     * Reads the inventory from a NBT Node.
     */
    protected void readFromNBT() {
        reading = true;
        if (NBTUtil.hasTag(armorStack, "Inventory") && armorStack.getTagCompound().getTag("Inventory") instanceof NBTTagCompound) {
            Log.info("Converting 'Inventory' tag to 'UpgradeInventory' in Pneumatic items");
            armorStack.getTagCompound().setTag("UpgradeInventory", armorStack.getTagCompound().getTag("Inventory"));
            armorStack.getTagCompound().removeTag("Inventory");
        }
        NBTTagList itemList = NBTUtil.getCompoundTag(armorStack, "UpgradeInventory").getTagList("Items", 10);
        for (int i = 0; i < itemList.tagCount(); i++) {
            NBTTagCompound slotEntry = itemList.getCompoundTagAt(i);
            int j = slotEntry.getByte("Slot");

            if (j >= 0 && j < getSizeInventory()) {
                setInventorySlotContents(j, new ItemStack(slotEntry));
            }
        }
        reading = false;
    }

}
