package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.world.BlockPosition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MachineManager {

    /*
     * Every occupied world cell points to its owning machine.
     *
     * Four cells of one furnace therefore all contain
     * the exact same PrimitiveFurnace reference.
     */
    private final Map<BlockPosition, Machine>
            machineByBlock =
            new HashMap<>();

    /*
     * Actual unique machines.
     */
    private final Set<Machine>
            machines =
            new HashSet<>();

    public boolean canRegister(
            Machine machine
    ) {
        if (machine == null) {
            return false;
        }

        for (
                BlockPosition position :
                machine.getOccupiedBlocks()
        ) {
            if (
                    machineByBlock.containsKey(
                            position
                    )
            ) {
                return false;
            }
        }

        return true;
    }

    public boolean register(
            Machine machine
    ) {
        if (
                !canRegister(
                        machine
                )
        ) {
            return false;
        }

        machines.add(
                machine
        );

        for (
                BlockPosition position :
                machine.getOccupiedBlocks()
        ) {
            machineByBlock.put(
                    position,
                    machine
            );
        }

        return true;
    }

    public Machine getMachineAt(
            int x,
            int y,
            int z
    ) {
        return machineByBlock.get(
                new BlockPosition(
                        x,
                        y,
                        z
                )
        );
    }

    public void remove(
            Machine machine
    ) {
        if (machine == null) {
            return;
        }

        machines.remove(
                machine
        );

        for (
                BlockPosition position :
                machine.getOccupiedBlocks()
        ) {
            machineByBlock.remove(
                    position
            );
        }
    }

    public Iterable<Machine> getMachines() {
        return machines;
    }
}