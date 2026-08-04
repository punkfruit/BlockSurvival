package com.daniel.blocksurvival.inventory;

public interface ItemCollector {

    /*
     * Returns how many items could not be accepted.
     *
     * 0 means everything fit.
     */
    int collect(
            ItemDefinition item,
            int quantity
    );
}