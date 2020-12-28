package me.desht.pneumaticcraft.common.progwidgets;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.drone.ProgWidgetType;
import me.desht.pneumaticcraft.common.ai.DroneAIBlockCondition;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.common.variables.GlobalVariableManager;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

/**
 * Base class for in-world conditions.
 */
public abstract class ProgWidgetCondition extends ProgWidgetInventoryBase implements ICondition, IJump, IVariableSetWidget {
    private DroneAIBlockCondition evaluator;
    private boolean isAndFunction;
    private ICondition.Operator operator = ICondition.Operator.GE;
    private String measureVar = "";

    public ProgWidgetCondition(ProgWidgetType<?> type) {
        super(type);
    }

    @Override
    public Goal getWidgetAI(IDroneBase drone, IProgWidget widget) {
        evaluator = getEvaluator(drone, widget);
        return evaluator;
    }

    protected abstract DroneAIBlockCondition getEvaluator(IDroneBase drone, IProgWidget widget);

    @Override
    public String getMeasureVar() {
        return measureVar;
    }

    @Override
    public void setMeasureVar(String measureVar) {
        this.measureVar = measureVar;
    }

    @Override
    public void getTooltip(List<ITextComponent> curTooltip) {
        super.getTooltip(curTooltip);
        if (!measureVar.isEmpty()) {
            curTooltip.add(xlate("pneumaticcraft.gui.progWidget.condition.measure").appendString(measureVar));
        }
    }

    @Override
    public void addErrors(List<ITextComponent> curInfo, List<IProgWidget> widgets) {
        super.addErrors(curInfo, widgets);
        if (measureVar.isEmpty() && getConnectedParameters()[getParameters().size() - 1] == null && getConnectedParameters()[getParameters().size() * 2 - 1] == null) {
            curInfo.add(xlate("pneumaticcraft.gui.progWidget.condition.error.noFlowControl"));
        }
    }

    @Override
    public IProgWidget getOutputWidget(IDroneBase drone, List<IProgWidget> allWidgets) {
        if (evaluator != null) {
            boolean evaluation = evaluate(drone, this);
            if (evaluation) {
                drone.addDebugEntry("pneumaticcraft.gui.progWidget.condition.evaluatedTrue");
            } else {
                drone.addDebugEntry("pneumaticcraft.gui.progWidget.condition.evaluatedFalse");
            }
            return ProgWidgetJump.jumpToLabel(drone, allWidgets, this, evaluation);
        } else {
            Log.error("Shouldn't be happening! ProgWidgetCondition");
            return super.getOutputWidget(drone, allWidgets);
        }
    }

    @Override
    public boolean evaluate(IDroneBase drone, IProgWidget widget) {
        return evaluator.getResult();
    }

    @Override
    public boolean isAndFunction() {
        return isAndFunction;
    }

    @Override
    public void setAndFunction(boolean isAndFunction) {
        this.isAndFunction = isAndFunction;
    }

    @Override
    public WidgetDifficulty getDifficulty() {
        return WidgetDifficulty.MEDIUM;
    }

    @Override
    public List<String> getPossibleJumpLocations() {
        IProgWidget widget = getConnectedParameters()[getParameters().size() - 1];
        ProgWidgetText textWidget = widget != null ? (ProgWidgetText) widget : null;
        IProgWidget widget2 = getConnectedParameters()[getParameters().size() * 2 - 1];
        ProgWidgetText textWidget2 = widget2 != null ? (ProgWidgetText) widget2 : null;
        List<String> locations = new ArrayList<>();
        if (textWidget != null) locations.add(textWidget.string);
        if (textWidget2 != null) locations.add(textWidget2.string);
        return locations;
    }

    @Override
    public int getRequiredCount() {
        return getCount();
    }

    @Override
    public void setRequiredCount(int count) {
        setCount(count);
    }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putBoolean("isAndFunction", isAndFunction);
        tag.putByte("operator", (byte) operator.ordinal());
        if (!measureVar.isEmpty()) tag.putString("measureVar", measureVar);
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        isAndFunction = tag.getBoolean("isAndFunction");
        operator = ICondition.Operator.values()[tag.getByte("operator")];
        measureVar = tag.getString("measureVar");
    }

    @Override
    public void writeToPacket(PacketBuffer buf) {
        super.writeToPacket(buf);
        buf.writeBoolean(isAndFunction);
        buf.writeByte(operator.ordinal());
        buf.writeString(measureVar, GlobalVariableManager.MAX_VARIABLE_LEN);
    }

    @Override
    public void readFromPacket(PacketBuffer buf) {
        super.readFromPacket(buf);
        isAndFunction = buf.readBoolean();
        operator = Operator.values()[buf.readByte()];
        measureVar = buf.readString(GlobalVariableManager.MAX_VARIABLE_LEN);
    }

    @Override
    protected boolean isUsingSides() {
        return false;
    }

    @Override
    public List<ITextComponent> getExtraStringInfo() {
        IFormattableTextComponent anyAll = xlate(isAndFunction() ? "pneumaticcraft.gui.progWidget.condition.all" : "pneumaticcraft.gui.progWidget.condition.any")
                .appendString(" " + getOperator().toString() + " " + getRequiredCount());
        return measureVar.isEmpty() ? Collections.singletonList(anyAll) : ImmutableList.of(anyAll, varAsTextComponent(measureVar));
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.CYAN;
    }

    @Override
    public void addVariables(Set<String> variables) {
        super.addVariables(variables);

        if (!getMeasureVar().isEmpty()) variables.add(getMeasureVar());
    }

    @Override
    public String getVariable() {
        return getMeasureVar();
    }

    @Override
    public void setVariable(String variable) {
        setMeasureVar(variable);
    }
}
