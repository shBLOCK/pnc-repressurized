package me.desht.pneumaticcraft.common.util;

import me.desht.pneumaticcraft.api.drone.ProgWidgetType;
import me.desht.pneumaticcraft.common.progwidgets.IProgWidget;
import me.desht.pneumaticcraft.common.tileentity.TileEntityProgrammer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class to build simple (no jumping) Drone programs, without needing to worry about the X/Y locations of widgets
 *
 * @author MineMaarten
 */
public class DroneProgramBuilder {

    private final List<DroneInstruction> instructions = new ArrayList<>();

    public void add(IProgWidget mainInstruction, IProgWidget... whitelist) {
        instructions.add(new DroneInstruction(mainInstruction, Arrays.asList(whitelist)));
    }

    public List<IProgWidget> build() {
        List<IProgWidget> allWidgets = new ArrayList<>();
        int curY = 0;
        for (DroneInstruction instruction : instructions) {
            instruction.mainInstruction.setX(0);
            instruction.mainInstruction.setY(curY);

            // Add whitelist pieces
            if (!instruction.whitelist.isEmpty()) {
                for (int paramIdx = 0; paramIdx < instruction.mainInstruction.getParameters().size(); paramIdx++) {
                    ProgWidgetType type = instruction.mainInstruction.getParameters().get(paramIdx);
                    List<IProgWidget> whitelist = instruction.whitelist.stream()
                            .filter(w -> type == w.getType())
                            .collect(Collectors.toList());
                    int curX = instruction.mainInstruction.getWidth() / 2;
                    for (IProgWidget whitelistItem : whitelist) {
                        whitelistItem.setX(curX);
                        whitelistItem.setY(curY + paramIdx * 11);
                        curX += whitelistItem.getWidth() / 2;
                    }
                }
            }

            curY += instruction.mainInstruction.getHeight() / 2;
            instruction.addToWidgets(allWidgets);
        }
        TileEntityProgrammer.updatePuzzleConnections(allWidgets);
        return allWidgets;
    }

    private class DroneInstruction {
        final IProgWidget mainInstruction;
        final List<IProgWidget> whitelist;

        DroneInstruction(IProgWidget mainInstruction, List<IProgWidget> whitelist) {
            sanityCheck(mainInstruction, whitelist);
            this.mainInstruction = mainInstruction;
            this.whitelist = whitelist;
        }

        void addToWidgets(List<IProgWidget> widgets) {
            widgets.add(mainInstruction);
            widgets.addAll(whitelist);
        }

        private void sanityCheck(IProgWidget mainInstruction, List<IProgWidget> whitelist) {
//            int nParams = mainInstruction.getParameters().size();
//            Validate.isTrue(whitelist.size() <= nParams, "Must supply at most " + nParams + " parameters for the whitelist!");
//            for (int i = 0; i < whitelist.size(); i++) {
//                Validate.isTrue(whitelist.get(i).getType() == mainInstruction.getParameters().get(i),
//                        String.format("Expected type %s, got %s for param %d",
//                                mainInstruction.getParameters().get(i).getRegistryName(),
//                                whitelist.get(i).getTypeID(), i));
//
//            }
        }
    }
}
