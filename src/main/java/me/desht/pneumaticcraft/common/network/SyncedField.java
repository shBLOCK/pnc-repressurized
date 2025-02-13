/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.common.network;

import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;

public abstract class SyncedField<T> {
    private final Field field;
    private final Object syncableObject;
    private final FieldType fieldType;
    private T lastValue;
    private int arrayIndex = -1;
    private boolean isLazy;

    SyncedField(Object syncableObject, Field field, FieldType fieldType) {
        this.syncableObject = syncableObject;
        this.field = field;
        field.setAccessible(true);
        this.fieldType = fieldType;
    }

    SyncedField<T> setArrayIndex(int arrayIndex) {
        this.arrayIndex = arrayIndex;
        return this;
    }

    public SyncedField<T> setLazy(boolean lazy) {
        this.isLazy = lazy;
        return this;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    @Override
    public String toString() {
        return arrayIndex == -1 ?
                "[" + syncableObject + "/" + field.getName() + "=" + getValue() + "]" :
                "[" + syncableObject + "/" + field.getName() + "[" + arrayIndex + "]=" + getValue() + "]";
    }

    /**
     * Called server-side: retrieve the latest value of this field from the syncable object and return true if
     * it's changed since the last time update() was called (provided this is not a @LazySynced field).
     *
     * @return true if the field has changed and is non-lazy so needs to be sync'd to clients, false otherwise
     */
    public boolean update() {
        try {
            T value = arrayIndex >= 0 ? getValueForArray(field.get(syncableObject), arrayIndex) : retrieveValue(field, syncableObject);
            if (lastValue == null && value != null || lastValue != null && !equals(lastValue, value)) {
                lastValue = value == null ? null : copyWhenNecessary(value);
                return !isLazy;
            }
        } catch (Throwable e) {
            Log.error("A problem occurred when trying to sync the field of {}. Field: {}, error: {}",
                    syncableObject.toString(), field.toString(), e.getMessage());
        }
        return false;
    }

    protected boolean equals(T oldValue, T newValue) {
        return oldValue.equals(newValue);
    }

    protected T copyWhenNecessary(T oldValue) {
        return oldValue;
    }

    protected T retrieveValue(Field field, Object te) throws Exception {
        //noinspection unchecked
        return (T) field.get(te);
    }

    protected void injectValue(Field field, Object te, T value) throws Exception {
        field.set(te, value);
    }

    protected abstract T getValueForArray(Object array, int index);

    protected abstract void setValueForArray(Object array, int index, T value);

    public T getValue() {
        return lastValue;
    }

    private void setValueInternal(T value) {
        try {
            if (arrayIndex >= 0) {
                setValueForArray(field.get(syncableObject), arrayIndex, value);
            } else {
                injectValue(field, syncableObject, value);
            }
        } catch (Exception e) {
            Log.error("A problem occurred when trying to sync the field of {}. Field: {}, error: {}",
                    syncableObject.toString(), field.toString(), e.getMessage());
        }
    }

    public void setValue(Object value) {
        //noinspection unchecked
        setValueInternal((T) value);
    }

    public static class SyncedInt extends SyncedField<Integer> {

        public SyncedInt(Object te, Field field) {
            super(te, field, FieldType.SYNCED_INT);
        }

        @Override
        protected Integer getValueForArray(Object array, int index) {
            return ((int[]) array)[index];
        }

        @Override
        protected void setValueForArray(Object array, int index, Integer value) {
            ((int[]) array)[index] = value;
        }

    }

    public static class SyncedFloat extends SyncedField<Float> {

        SyncedFloat(Object te, Field field) {
            super(te, field, FieldType.SYNCED_FLOAT);
        }

        @Override
        protected Float getValueForArray(Object array, int index) {
            return ((float[]) array)[index];
        }

        @Override
        protected void setValueForArray(Object array, int index, Float value) {
            ((float[]) array)[index] = value;
        }

    }

    public static class SyncedDouble extends SyncedField<Double> {

        SyncedDouble(Object te, Field field) {
            super(te, field, FieldType.SYNCED_DOUBLE);
        }

        @Override
        protected Double getValueForArray(Object array, int index) {
            return ((double[]) array)[index];
        }

        @Override
        protected void setValueForArray(Object array, int index, Double value) {
            ((double[]) array)[index] = value;
        }

    }

    public static class SyncedBoolean extends SyncedField<Boolean> {

        SyncedBoolean(Object te, Field field) {
            super(te, field, FieldType.SYNCED_BOOL);
        }

        @Override
        protected Boolean getValueForArray(Object array, int index) {
            return ((boolean[]) array)[index];
        }

        @Override
        protected void setValueForArray(Object array, int index, Boolean value) {
            ((boolean[]) array)[index] = value;
        }

    }

    public static class SyncedString extends SyncedField<String> {

        SyncedString(Object te, Field field) {
            super(te, field, FieldType.SYNCED_STRING);
        }

        @Override
        protected String getValueForArray(Object array, int index) {
            return ((String[]) array)[index];
        }

        @Override
        protected void setValueForArray(Object array, int index, String value) {
            ((String[]) array)[index] = value;
        }

    }

    /**
     * FIXME: sync'ing an array of enum does not work
     */
    public static class SyncedEnum extends SyncedField<Byte> {

        SyncedEnum(Object te, Field field) {
            super(te, field, FieldType.SYNCED_ENUM);
        }

        @Override
        protected Byte getValueForArray(Object array, int index) {
            return ((byte[]) array)[index];
        }

        @Override
        protected void setValueForArray(Object array, int index, Byte value) {
            ((byte[]) array)[index] = value;
        }

        @Override
        protected Byte retrieveValue(Field field, Object te) throws Exception {
            Object[] enumTypes = field.getType().getEnumConstants();
            // this will be INDEX_NOT_FOUND if the enum field is null, which we can check for in injectValue()
            return (byte) ArrayUtils.indexOf(enumTypes, field.get(te));
        }

        @Override
        protected void injectValue(Field field, Object te, Byte value) throws Exception {
            if (value == ArrayUtils.INDEX_NOT_FOUND) {
                field.set(te, null);
            } else {
                Object enumType = field.getType().getEnumConstants()[value];
                field.set(te, enumType);
            }
        }
    }

    public static class SyncedItemStack extends SyncedField<ItemStack> {
        SyncedItemStack(Object te, Field field) {
            super(te, field, FieldType.SYNCED_ITEMSTACK);
        }

        @Override
        protected ItemStack getValueForArray(Object array, int index) {
            return ((ItemStack[]) array)[index];
        }

        @Override
        protected void setValueForArray(Object array, int index, ItemStack value) {
            ((ItemStack[]) array)[index] = value;
        }

        @Override
        protected boolean equals(ItemStack oldValue, ItemStack newValue) {
            // Note: ItemStack doesn't override equals()
            return ItemStack.matches(oldValue, newValue);
        }

        @Override
        protected ItemStack copyWhenNecessary(ItemStack oldValue) {
            return oldValue.copy();
        }
    }

    public static class SyncedFluidStack extends SyncedField<FluidStack> {
        SyncedFluidStack(Object te, Field field) {
            super(te, field, FieldType.SYNCED_FLUIDSTACK);
        }

        @Override
        protected FluidStack getValueForArray(Object array, int index) {
            return ((FluidStack[]) array)[index];
        }

        @Override
        protected void setValueForArray(Object array, int index, FluidStack value) {
            ((FluidStack[]) array)[index] = value;
        }

        @Override
        protected boolean equals(FluidStack oldValue, FluidStack newValue) {
            // Note: FluidStack#equals() implementation only checks the fluid, not the amount
            return FluidStack.matches(oldValue, newValue);
        }

        @Override
        protected FluidStack copyWhenNecessary(FluidStack oldValue) {
            return oldValue.copy();
        }
    }

    public static class SyncedItemHandler extends SyncedField<IItemHandlerModifiable> {
        SyncedItemHandler(Object te, Field field) {
            super(te, field, FieldType.SYNCED_ITEM_HANDLER);
        }

        @Override
        protected ItemStackHandler getValueForArray(Object array, int index) {
            return ((ItemStackHandler[]) array)[index];
        }

        @Override
        protected void setValueForArray(Object array, int index, IItemHandlerModifiable value) {
            ((IItemHandlerModifiable[]) array)[index] = value;
        }

        @Override
        protected IItemHandlerModifiable retrieveValue(Field field, Object te) throws Exception {
            return (IItemHandlerModifiable) field.get(te);
        }

        @Override
        protected void injectValue(Field field, Object te, IItemHandlerModifiable value) throws Exception {
            IItemHandlerModifiable handler = (IItemHandlerModifiable) field.get(te);
            for (int i = 0; i < value.getSlots(); i++) {
                handler.setStackInSlot(i, value.getStackInSlot(i));
            }
        }

        @Override
        protected boolean equals(IItemHandlerModifiable oldValue, IItemHandlerModifiable newValue) {
            if (oldValue.getSlots() != newValue.getSlots()) return false;
            for (int i = 0; i < oldValue.getSlots(); i++) {
                if (!ItemStack.matches(oldValue.getStackInSlot(i), newValue.getStackInSlot(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected IItemHandlerModifiable copyWhenNecessary(IItemHandlerModifiable oldValue) {
            ItemStackHandler result = new ItemStackHandler(oldValue.getSlots());
            for (int i = 0; i < oldValue.getSlots(); i++) {
                ItemStack stack = oldValue.getStackInSlot(i);
                result.setStackInSlot(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
            return result;
        }
    }

    /*************** Utility Methods ***************************/

    static Object fromBytes(RegistryFriendlyByteBuf buf, FieldType type) {
        return switch (type) {
            case SYNCED_INT -> buf.readInt();
            case SYNCED_FLOAT -> buf.readFloat();
            case SYNCED_DOUBLE -> buf.readDouble();
            case SYNCED_BOOL -> buf.readBoolean();
            case SYNCED_STRING -> buf.readUtf();
            case SYNCED_ENUM -> buf.readByte();
            case SYNCED_ITEMSTACK -> ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            case SYNCED_FLUIDSTACK -> FluidStack.OPTIONAL_STREAM_CODEC.decode(buf);
            case SYNCED_ITEM_HANDLER -> {
                int len = buf.readVarInt();
                ItemStackHandler handler = new ItemStackHandler(len);
                for (int i = 0; i < len; i++) {
                    handler.setStackInSlot(buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                yield handler;
            }
        };
    }

    static void toBytes(RegistryFriendlyByteBuf buf, Object value, FieldType type) {
        switch (type) {
            case SYNCED_INT -> buf.writeInt((Integer) value);
            case SYNCED_FLOAT -> buf.writeFloat((Float) value);
            case SYNCED_DOUBLE -> buf.writeDouble((Double) value);
            case SYNCED_BOOL -> buf.writeBoolean((Boolean) value);
            case SYNCED_STRING -> buf.writeUtf((String) value);
            case SYNCED_ENUM -> buf.writeByte((Byte) value);
            case SYNCED_ITEMSTACK -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, value == null ? ItemStack.EMPTY : (ItemStack) value);
            case SYNCED_FLUIDSTACK -> FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, ((FluidStack) value));
            case SYNCED_ITEM_HANDLER -> {
                ItemStackHandler h = (ItemStackHandler) value;
                buf.writeVarInt(h.getSlots());
                for (int i = 0; i < h.getSlots(); i++) {
                    buf.writeVarInt(i);
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, h.getStackInSlot(i));
                }
            }
        }
    }


    public enum FieldType {
        SYNCED_INT,
        SYNCED_FLOAT,
        SYNCED_DOUBLE,
        SYNCED_BOOL,
        SYNCED_STRING,
        SYNCED_ENUM,
        SYNCED_ITEMSTACK,
        SYNCED_FLUIDSTACK,
        SYNCED_ITEM_HANDLER;
    }
}
